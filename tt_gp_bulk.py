#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tuner Tools - bulk Guitar Pro uploader to Google My Drive.

Key properties:
- ALWAYS starts from a new, empty SQLite catalog; old test catalogs are rejected.
- Scans locally without copying or moving GP files.
- Splits new Drive uploads into 256 subfolders (00..ff).
- Writes Google file IDs back to the same schema used by api_gp_drive.php.
- Records in-flight attempts to recover files if the process is interrupted.
- Never deletes source files or files already on Google Drive.

Usage: python tt_gp_bulk.py --help
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import random
import sqlite3
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

EXTENSIONS = {'.gp', '.gp3', '.gp4', '.gp5', '.gp6', '.gp7', '.gp8', '.gpx'}
DRIVE_FOLDER_MIMETYPE = 'application/vnd.google-apps.folder'
NEW_OAUTH_SCOPE = ['https://www.googleapis.com/auth/drive.file']
LOCAL_ID_PREFIX = 'local-'
_thread_local = threading.local()
_rate_lock = threading.Lock()
_next_write_at = 0.0
FRESH_MARKER = 'tt_fresh_gp_1600000_v1'


def pace_write(max_per_second: float) -> None:
    """Cap sustained writes to Drive (default 2.5/sec/account)."""
    global _next_write_at
    if max_per_second <= 0:
        return
    with _rate_lock:
        now = time.monotonic()
        planned = max(now, _next_write_at)
        _next_write_at = planned + 1.0 / max_per_second
    if planned > now:
        time.sleep(planned - now)


def ensure_fresh_run(con: sqlite3.Connection) -> None:
    if meta_get(con, 'catalog_generation') != FRESH_MARKER:
        raise SystemExit('DB khong phai catalog moi do bo cong cu Fresh tao ra. '
                         'Khong nap DB 5.000 bai. Hay chon ten DB moi, vi du gp_fresh_1600000.db.')


def now_utc() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def connect_db(path: Path) -> sqlite3.Connection:
    con = sqlite3.connect(str(path), timeout=60)
    con.execute('PRAGMA journal_mode=WAL')
    con.execute('PRAGMA busy_timeout=60000')
    con.execute('PRAGMA synchronous=NORMAL')
    con.executescript('''
      CREATE TABLE IF NOT EXISTS songs (
        id TEXT PRIMARY KEY, title TEXT, artist TEXT, file_path TEXT,
        format TEXT, drive_file_id TEXT, local_test_path TEXT
      );
      CREATE UNIQUE INDEX IF NOT EXISTS idx_bulk_songs_file_path ON songs(file_path);
      CREATE TABLE IF NOT EXISTS bulk_meta (
        key TEXT PRIMARY KEY, value TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS bulk_upload_journal (
        song_id TEXT PRIMARY KEY, state TEXT NOT NULL,
        last_error TEXT, attempts INTEGER NOT NULL DEFAULT 0,
        updated_utc TEXT NOT NULL
      );
    ''')
    con.commit()
    return con


def meta_get(con: sqlite3.Connection, key: str) -> str | None:
    row = con.execute('SELECT value FROM bulk_meta WHERE key=?', (key,)).fetchone()
    return row[0] if row else None


def meta_set(con: sqlite3.Connection, key: str, value: str) -> None:
    con.execute('INSERT INTO bulk_meta(key,value) VALUES (?,?) '
                'ON CONFLICT(key) DO UPDATE SET value=excluded.value', (key, value))
    con.commit()


def progress_stats(con: sqlite3.Connection) -> None:
    total, ready = con.execute('''
      SELECT COUNT(*), SUM(CASE WHEN drive_file_id IS NOT NULL AND TRIM(drive_file_id)<>''
                                   THEN 1 ELSE 0 END) FROM songs
    ''').fetchone()
    ready = ready or 0
    print(f'Tong ban ghi: {total:,} | Da co Drive ID: {ready:,} | Con lai: {total-ready:,}')
    for state, count in con.execute('SELECT state, COUNT(*) FROM bulk_upload_journal GROUP BY state'):
        print(f'  Nhat ky {state}: {count:,}')
    print(f'  Root Drive ID: {meta_get(con, "drive_root_id") or "(chua tao)"}')


def scan_files(args: argparse.Namespace) -> None:
    source = Path(args.source).expanduser().resolve()
    db_path = Path(args.db).expanduser().resolve()
    if not source.is_dir():
        raise SystemExit(f'Khong tim thay thu muc: {source}')
    if db_path.is_relative_to(source):
        raise SystemExit('KHONG luu database moi trong thu muc dang quet. Dat DB tai C:\\TunerToolsGP_Fresh.')
    if source == db_path.parent:
        print('Luu y: nen de CSDL o thu muc khac noi chua 1.6 trieu file.')
    db_exists = db_path.exists()
    con = connect_db(db_path)
    if db_exists:
        ensure_fresh_run(con)
    else:
        meta_set(con, 'catalog_generation', FRESH_MARKER)
        print(f'Da tao DATABASE TRONG, khong nhap 5.000 bai cu: {db_path}', flush=True)
    saved_source = meta_get(con, 'local_source')
    if saved_source and saved_source != str(source):
        raise SystemExit('DB nay da gan voi nguon khac. '
                         f'Nguon hien tai: {saved_source}\nHay su dung dung thu muc cu hoac DB moi.')
    meta_set(con, 'local_source', str(source))
    prefix = args.prefix or source.name
    saved_prefix = meta_get(con, 'relative_prefix')
    if saved_prefix and saved_prefix != prefix:
        raise SystemExit(f'Prefix trong DB la {saved_prefix!r}, khong phai {prefix!r}.')
    meta_set(con, 'relative_prefix', prefix)

    checked = added = existed = errors = size_bytes = 0
    directory_count = 0
    batch = 0
    started = last_report = time.monotonic()

    def show_progress(stage: str = 'QUET') -> None:
        elapsed = max(0.1, time.monotonic() - started)
        print(f'[{stage}] Thu muc {directory_count:,} | File GP {checked:,} | '
              f'Da co trong DB {existed:,} | Them moi {added:,} | '
              f'Loi {errors:,} | {elapsed:.0f}s', flush=True)

    print(f'Quet file tu: {source}\nDuong dan luu DB bat dau bang: {prefix}/', flush=True)
    print('Dang doi chieu tat ca file GP voi database Songsterr... '
          'Hien thi tien do moi 20.000 file GP hoac 10 giay.', flush=True)
    try:
        try:
            for current, dirnames, filenames in os.walk(source, followlinks=False):
                directory_count += 1
                # Avoid following junctions / symlink directories on Windows.
                dirnames[:] = [d for d in dirnames if not (Path(current) / d).is_symlink()]
                for filename in filenames:
                    path = Path(current) / filename
                    ext = path.suffix.lower()
                    if ext not in EXTENSIONS or path.is_symlink():
                        continue
                    checked += 1
                    rel = path.relative_to(source).as_posix()
                    file_path = prefix + '/' + rel
                    # Important: DO NOT 'continue' on an existing file here.
                    # The old code skipped progress printing for all matched rows.
                    if con.execute('SELECT 1 FROM songs WHERE file_path=? LIMIT 1',
                                   (file_path,)).fetchone():
                        existed += 1
                    else:
                        try:
                            size_bytes += path.stat().st_size
                            basename = path.stem
                            artist = basename.split(' - ', 1)[0] if ' - ' in basename else ''
                            local_id = LOCAL_ID_PREFIX + hashlib.sha256(
                                file_path.encode('utf-8', 'surrogatepass')
                            ).hexdigest()[:32]
                            cursor = con.execute('''
                              INSERT OR IGNORE INTO songs
                              (id,title,artist,file_path,format,drive_file_id,local_test_path)
                              VALUES (?,?,?,?,?,NULL,?)
                            ''', (local_id, basename, artist, file_path,
                                  ext.lstrip('.'), str(path)))
                            added += cursor.rowcount
                            batch += cursor.rowcount
                            if batch >= 2000:
                                con.commit()
                                batch = 0
                        except (OSError, sqlite3.Error) as exc:
                            errors += 1
                            print(f'Loi doc file {path}: {exc}', file=sys.stderr, flush=True)
                    now = time.monotonic()
                    if checked == 1000 or checked % 20000 == 0 or now - last_report >= 10:
                        con.commit()
                        show_progress()
                        last_report = now
                # A folder may have no GP files: still report if directory walking is slow.
                now = time.monotonic()
                if now - last_report >= 10:
                    con.commit()
                    show_progress()
                    last_report = now
            con.commit()
            show_progress('QUET XONG')
        except KeyboardInterrupt:
            # Do not lose the most recent small insert batch if Ctrl+C is pressed.
            con.commit()
            show_progress('DA DUNG')
            print('Da dung quet. Database da nhap van duoc giu. Chay lai scan khi can.',
                  flush=True)
            raise

        # Build the FTS search index. SQLite's FTS5 rebuild is a single long SQL
        # statement; a lightweight heartbeat prevents it from looking frozen.
        try:
            con.execute('''CREATE VIRTUAL TABLE IF NOT EXISTS songs_fts USING fts5(
                    title, artist, file_path, format, content='songs',
                    content_rowid='rowid', tokenize='unicode61 remove_diacritics 2'
            )''')
            print('Dang tao lai chi muc tim kiem FTS5 cho toan bo database...', flush=True)
            index_start = time.monotonic()
            index_done = threading.Event()

            def index_heartbeat() -> None:
                while not index_done.wait(15):
                    print('  FTS5 van dang xu ly, da qua '
                          f'{time.monotonic() - index_start:.0f}s (chua upload Drive).',
                          flush=True)

            monitor = threading.Thread(target=index_heartbeat, daemon=True)
            monitor.start()
            try:
                con.execute("INSERT INTO songs_fts(songs_fts) VALUES ('rebuild')")
                con.commit()
            finally:
                index_done.set()
                monitor.join(timeout=1)
            print(f'Da tao FTS5 trong {time.monotonic() - index_start:.1f}s.', flush=True)
        except sqlite3.Error as exc:
            print('CANH BAO: Khong tao duoc chi muc FTS5; PHP se thu LIKE fallback.',
                  exc, flush=True)
        print(f'Hoan tat: tim thay {checked:,} file GP | them {added:,} | '
              f'da ton tai {existed:,} | loi {errors:,}', flush=True)
        print(f'Dung luong cac file moi duoc thong ke: {size_bytes/1_000_000_000:.2f} GB',
              flush=True)
        progress_stats(con)
    finally:
        con.close()

def load_credentials(token_file: Path, credentials_file: Path | None = None):
    try:
        from google.oauth2.credentials import Credentials
        from google.auth.transport.requests import Request
    except ImportError as exc:
        raise SystemExit('Chua cai thu vien. Chay: py -m pip install -r requirements.txt') from exc
    if token_file.is_file():
        # Use the existing PHP OAuth token (as a PRIVATE local copy) where possible.
        # Do not force a different OAuth scope on a previously authorized token.
        creds = Credentials.from_authorized_user_file(str(token_file))
        if not creds.valid or creds.expiry is None:
            if not creds.refresh_token:
                raise SystemExit('Token khong co refresh_token. Can dang nhap OAuth lai.')
            creds.refresh(Request())
            token_file.write_text(creds.to_json(), encoding='utf-8')
        return creds
    if credentials_file is None or not credentials_file.is_file():
        raise SystemExit('Thieu drive_token.json va credentials.json. '
                         'Lay ban SAO token tu PHP, hoac tao OAuth Desktop credentials.json.')
    try:
        from google_auth_oauthlib.flow import InstalledAppFlow
    except ImportError as exc:
        raise SystemExit('Cai google-auth-oauthlib trong requirements.txt') from exc
    print('Mo trinh duyet de dang nhap tai khoan Drive se luu file.')
    creds = InstalledAppFlow.from_client_secrets_file(
        str(credentials_file), NEW_OAUTH_SCOPE
    ).run_local_server(port=0)
    token_file.write_text(creds.to_json(), encoding='utf-8')
    return creds


def new_service(creds):
    try:
        from googleapiclient.discovery import build
    except ImportError as exc:
        raise SystemExit('Chua cai google-api-python-client') from exc
    return build('drive', 'v3', credentials=creds, cache_discovery=False)


def get_worker_service(token_file: Path):
    key = str(token_file)
    if getattr(_thread_local, 'auth_key', None) != key:
        _thread_local.service = new_service(load_credentials(token_file))
        _thread_local.auth_key = key
    return _thread_local.service


def auth_check(args: argparse.Namespace) -> None:
    token = Path(args.token).expanduser().resolve()
    client = Path(args.client_secret).expanduser().resolve() if args.client_secret else None
    creds = load_credentials(token, client)
    service = new_service(creds)
    about = service.about().get(fields='user(emailAddress),storageQuota(limit,usage)').execute()
    print('Google Drive API dang hoat dong.')
    print('Tai khoan:', (about.get('user') or {}).get('emailAddress', '(khong ro)'))
    q = about.get('storageQuota') or {}
    if q.get('limit'):
        print('Dung luong cho phep (GB):', round(int(q['limit'])/1_000_000_000, 2))
    if q.get('usage'):
        print('Dung luong da dung (GB):', round(int(q['usage'])/1_000_000_000, 2))
    print('KHONG chia se drive_token.json, credentials.json len chat, web hay Git.')


def esc_drive_search(s: str) -> str:
    return s.replace('\\', '\\\\').replace("'", "\\'")


def folder_find_or_create(service, name: str, parent_id: str) -> str:
    q = (f"name = '{esc_drive_search(name)}' and "
         f"mimeType = '{DRIVE_FOLDER_MIMETYPE}' and "
         f"'{esc_drive_search(parent_id)}' in parents and trashed = false")
    files = service.files().list(q=q, pageSize=100, fields='files(id,name),nextPageToken').execute()
    if files.get('files'):
        return files['files'][0]['id']
    file = service.files().create(
        body={'name': name, 'mimeType': DRIVE_FOLDER_MIMETYPE, 'parents': [parent_id]},
        fields='id', supportsAllDrives=False
    ).execute()
    return file['id']


def prepare_root(service, con: sqlite3.Connection, folder_name: str) -> str:
    root_id = meta_get(con, 'drive_root_id')
    if root_id:
        info = service.files().get(fileId=root_id, fields='id,name,trashed').execute()
        if info.get('trashed'):
            raise RuntimeError('Thu muc Drive goc da bi xoa. Phuc hoi truoc khi upload.')
        print(f'Su dung thu muc da tao: {info.get("name")} ({root_id})')
        return root_id
    # Always create a fresh root folder. Never reuse the 5,000-file pilot folder.
    actual_name = f'{folder_name}_{datetime.now().strftime("%Y%m%d_%H%M%S")}'
    drive_folder = service.files().create(
        body={'name': actual_name, 'mimeType': DRIVE_FOLDER_MIMETYPE, 'parents': ['root']},
        fields='id,name', supportsAllDrives=False
    ).execute()
    root_id = drive_folder['id']
    meta_set(con, 'drive_root_id', root_id)
    meta_set(con, 'drive_root_name', actual_name)
    print(f'Da tao thu muc Drive MOI: {actual_name} ({root_id})')
    return root_id


def get_shard_folder(service, con: sqlite3.Connection, root_id: str,
                     shard: str, cache: dict[str, str]) -> str:
    if shard in cache:
        return cache[shard]
    key = 'drive_shard_' + shard
    folder_id = meta_get(con, key)
    if not folder_id:
        folder_id = folder_find_or_create(service, shard, root_id)
        meta_set(con, key, folder_id)
    cache[shard] = folder_id
    return folder_id


def reason_from_http_error(err) -> tuple[int, str, str]:
    status = int(getattr(getattr(err, 'resp', None), 'status', 0) or 0)
    reason = ''
    message = str(err)
    content = getattr(err, 'content', None)
    if content:
        try:
            data = json.loads(content.decode('utf-8') if isinstance(content, bytes) else content)
            obj = data.get('error') or {}
            message = str(obj.get('message', message))
            entries = obj.get('errors') or obj.get('details') or []
            if entries:
                reason = str(entries[0].get('reason', ''))
        except Exception:
            pass
    return status, reason, message


def find_existing_file(service, song_id: str) -> str | None:
    # Searchable private appProperty supports recovery after interrupted writes.
    s = esc_drive_search(song_id)
    q = f"appProperties has {{ key='tt_song_id' and value='{s}' }} and trashed=false"
    results = service.files().list(q=q, fields='files(id),nextPageToken', pageSize=10).execute()
    return results['files'][0]['id'] if results.get('files') else None


def upload_one(token_file: Path, file_path: Path, song_id: str,
               parent_id: str, inspect_existing: bool, write_rate: float) -> tuple[str, str | None, str | None]:
    """Returns (state, drive_id, error). State: ok | stop | error."""
    try:
        from googleapiclient.errors import HttpError
        from googleapiclient.http import MediaFileUpload
    except ImportError:
        return ('stop', None, 'Thieu google-api-python-client')
    try:
        if not file_path.is_file():
            return ('error', None, f'Thieu file dia phuong: {file_path}')
        service = get_worker_service(token_file)
        if inspect_existing:
            old_id = find_existing_file(service, song_id)
            if old_id:
                return ('ok', old_id, None)
        for attempt in range(7):
            try:
                size = file_path.stat().st_size
                media = MediaFileUpload(
                    str(file_path), mimetype='application/octet-stream',
                    resumable=size > (8 * 1024 * 1024), chunksize=8 * 1024 * 1024
                )
                body = {
                    'name': file_path.name,
                    'parents': [parent_id],
                    'appProperties': {'tt_song_id': song_id},
                    'description': 'Tuner Tools GP catalog file',
                }
                pace_write(write_rate)
                req = service.files().create(body=body, media_body=media, fields='id')
                if media.resumable():
                    response = None
                    while response is None:
                        _, response = req.next_chunk(num_retries=2)
                else:
                    response = req.execute(num_retries=2)
                if not response or not response.get('id'):
                    raise RuntimeError('Drive khong tra ve file ID')
                return ('ok', response['id'], None)
            except HttpError as exc:
                status, reason, message = reason_from_http_error(exc)
                low_reason, low_message = reason.lower(), message.lower()
                fatal = {'storagequotaexceeded', 'dailylimitexceeded',
                         'insufficientpermissions', 'accessnotconfigured',
                         'activeitemcreationlimitexceeded', 'uploadlimitexceeded'}
                if (low_reason in fatal or status in {401, 404} or
                    (status == 403 and low_reason not in {'ratelimitexceeded', 'userratelimitexceeded', 'backenderror'}) or
                    ('750' in low_message and 'limit' in low_message)):
                    return ('stop', None, f'HTTP {status}: {reason} {message}')
                can_retry = status in {429, 500, 502, 503, 504} or (
                    status == 403 and low_reason in {'ratelimitexceeded',
                    'userratelimitexceeded', 'backenderror'})
                if not can_retry:
                    return ('error', None, f'HTTP {status}: {reason} {message}')
                if attempt == 6:
                    # Stop an entire bulk job instead of hammering a quota-limited API.
                    return ('stop', None, f'Limit/retry exhausted HTTP {status}: {reason} {message}')
                # If a previous upload succeeded but its response was lost, don't duplicate it.
                try:
                    old_id = find_existing_file(service, song_id)
                    if old_id:
                        return ('ok', old_id, None)
                except Exception:
                    pass
                time.sleep(min(60, 2 ** attempt) + random.uniform(0, 1))
            except (OSError, RuntimeError) as exc:
                return ('error', None, str(exc))
        return ('error', None, 'Da het lan retry')
    except Exception as exc:
        return ('error', None, f'{type(exc).__name__}: {exc}')


def resolve_local_path(source: Path, relative_prefix: str, db_file_path: str) -> Path:
    if not db_file_path.startswith(relative_prefix + '/'):
        raise ValueError(f'Duong dan {db_file_path!r} khong nam trong prefix {relative_prefix!r}')
    relative = db_file_path[len(relative_prefix) + 1:]
    parts = Path(relative.replace('\\', '/')).parts
    if not parts or '..' in parts or Path(relative).is_absolute():
        raise ValueError(f'Duong dan khong an toan: {db_file_path!r}')
    return source.joinpath(*parts)



def inspect_samefile_aliases(con: sqlite3.Connection, source: Path, prefix: str):
    """Find case-insensitive path aliases with bounded memory and confirm samefile.

    Only a small mapping of true aliases is kept; 16-byte fingerprints are
    used in a first pass and full paths are compared in the second pass.
    Different physical files are NEVER merged merely because names casefold.
    """
    print('[DEDUP] Dang tim duong dan trung (khong upload)...', flush=True)
    t0 = time.monotonic()
    seen: set[bytes] = set()
    candidates: set[bytes] = set()
    total = 0

    def path_key(value: str) -> str:
        return str(value or '').replace('\\', '/').casefold()

    def digest(value: str) -> bytes:
        return hashlib.blake2b(value.encode('utf-8', 'surrogatepass'), digest_size=16).digest()

    for (fp,) in con.execute('SELECT file_path FROM songs'):
        total += 1
        key = digest(path_key(fp))
        if key in seen:
            candidates.add(key)
        else:
            seen.add(key)
        if total % 500000 == 0:
            print(f'  Kiem tra {total:,} ban ghi...', flush=True)
    del seen
    if not candidates:
        print(f'[DEDUP] Khong co duong dan trung ({total:,} ban ghi).', flush=True)
        return {}, {}, [], 0, 0

    # A matching digest only narrows the candidate group. Full normalized paths
    # must also match, and os.path.samefile then verifies the SAME physical file.
    by_path: dict[str, list[tuple[str, str, str | None]]] = {}
    for sid, fp, fid in con.execute('SELECT id,file_path,drive_file_id FROM songs'):
        norm = path_key(fp)
        if digest(norm) in candidates:
            by_path.setdefault(norm, []).append((sid, fp, fid))

    alias_to_primary: dict[str, str] = {}
    primary_to_aliases: dict[str, list[str]] = {}
    link_existing: list[tuple[str, str]] = []  # (drive_id, alias_song_id)
    groups = multiple_remote = unsafe = 0
    for rows in by_path.values():
        if len(rows) <= 1:
            continue  # fingerprint collision only
        physical: list[tuple[Path, list[tuple[str, str, str | None]]]] = []
        for row in rows:
            try:
                resolved = resolve_local_path(source, prefix, row[1])
                if not resolved.is_file():
                    unsafe += 1
                    continue
                match = False
                for representative, members in physical:
                    if os.path.samefile(resolved, representative):
                        members.append(row)
                        match = True
                        break
                if not match:
                    physical.append((resolved, [row]))
            except (OSError, ValueError):
                unsafe += 1
        for _, members in physical:
            if len(members) <= 1:
                continue
            groups += 1
            uploaded = [r for r in members if r[2] and str(r[2]).strip()]
            primary = uploaded[0] if uploaded else members[0]
            primary_id, _, primary_drive_id = primary
            if len({r[2] for r in uploaded}) > 1:
                multiple_remote += 1
            for sid, _, fid in members:
                if sid == primary_id:
                    continue
                alias_to_primary[sid] = primary_id
                primary_to_aliases.setdefault(primary_id, []).append(sid)
                if (not fid or not fid.strip()) and primary_drive_id:
                    link_existing.append((primary_drive_id, sid))
    print(f'[DEDUP] Nhom cung file: {groups:,} | Ban ghi alias: {len(alias_to_primary):,} | '
          f'Co the dung lai Drive ID: {len(link_existing):,} | '
          f'Nhom da co >1 Drive ID: {multiple_remote:,} | Khong xac minh duoc: {unsafe:,} | '
          f'{time.monotonic() - t0:.1f}s', flush=True)
    if multiple_remote:
        print('[DEDUP] Canh bao: mot so nhom da co nhieu Drive ID rieng. '
              'KHONG xoa hoac ghi de Drive ID da co.', flush=True)
    return alias_to_primary, primary_to_aliases, link_existing, groups, unsafe


def dedup_check(args: argparse.Namespace) -> None:
    """Read-only alias inspection; no new Drive files and no SQLite edits."""
    source = Path(args.source).expanduser().resolve()
    db_path = Path(args.db).expanduser().resolve()
    if not source.is_dir() or not db_path.is_file():
        raise SystemExit('Khong tim thay thu muc nguon hoac database.')
    con = sqlite3.connect(db_path.as_uri() + '?mode=ro', uri=True)
    try:
        ensure_fresh_run(con)
        saved = meta_get(con, 'local_source')
        if saved and saved.casefold() != str(source).casefold():
            raise SystemExit(f'Sai thu muc nguon: {saved}')
        inspect_samefile_aliases(con, source, meta_get(con, 'relative_prefix') or source.name)
        print('Da kiem tra xong: KHONG sua DB, KHONG upload Drive.', flush=True)
    finally:
        con.close()

def journal_mark(con: sqlite3.Connection, song_id: str, state: str,
                 last_error: str | None = None) -> None:
    con.execute('''
      INSERT INTO bulk_upload_journal(song_id,state,last_error,attempts,updated_utc)
      VALUES(?,?,?,?,?)
      ON CONFLICT(song_id) DO UPDATE SET state=excluded.state,
        last_error=excluded.last_error, attempts=bulk_upload_journal.attempts+1,
        updated_utc=excluded.updated_utc
    ''', (song_id, state, last_error, 1, now_utc()))


def upload_files(args: argparse.Namespace) -> None:
    source = Path(args.source).expanduser().resolve()
    db_path = Path(args.db).expanduser().resolve()
    token = Path(args.token).expanduser().resolve()
    if not source.is_dir() or not db_path.is_file():
        raise SystemExit('Thieu thu muc nguon hoac DB. Hay chay scan truoc.')
    con = connect_db(db_path)
    ensure_fresh_run(con)
    source_saved = meta_get(con, 'local_source')
    if source_saved and str(source) != source_saved:
        raise SystemExit(f'Dang dung sai thu muc: {source_saved}')
    prefix = meta_get(con, 'relative_prefix') or source.name
    # Authenticate before doing any remote writes.
    creds = load_credentials(token)
    service = new_service(creds)
    about = service.about().get(fields='user(emailAddress)').execute()
    print('Dang tai len Drive:', (about.get('user') or {}).get('emailAddress', '(unknown)'))
    if not args.yes:
        raise SystemExit('Chay lai voi --yes sau khi kiem tra tai khoan va duong dan.')
    # Match aliases BEFORE starting new uploads. This also preserves the 100
    # previously uploaded files by choosing an already-uploaded row as primary.
    alias_to_primary, primary_to_aliases, link_existing, _, _ = (
        inspect_samefile_aliases(con, source, prefix)
    )
    if link_existing:
        con.executemany('''
            UPDATE songs SET drive_file_id=? WHERE id=?
              AND (drive_file_id IS NULL OR TRIM(drive_file_id)='')
        ''', link_existing)
        con.commit()
        print(f'[DEDUP] Da gan lai {len(link_existing):,} Drive ID cu cho alias.', flush=True)
    root_id = prepare_root(service, con, args.folder)
    cache: dict[str, str] = {}
    workers = min(12, max(1, args.workers))
    processed = succeeded = failed = skipped_aliases = 0
    last_rowid = 0
    stop_error = None
    started_at = time.monotonic()
    try:
        with ThreadPoolExecutor(max_workers=workers) as pool:
            while (args.limit <= 0 or processed < args.limit) and not stop_error:
                remaining = (args.limit-processed) if args.limit > 0 else workers * 3
                count = min(workers * 3, remaining)
                if count <= 0:
                    break
                rows = con.execute('''
                    SELECT rowid,id,file_path FROM songs
                    WHERE rowid > ? AND
                      (drive_file_id IS NULL OR TRIM(drive_file_id)='')
                    ORDER BY rowid LIMIT ?
                ''', (last_rowid, count)).fetchall()
                if not rows:
                    break
                last_rowid = rows[-1][0]
                futures = {}
                for _, sid, relpath in rows:
                    if sid in alias_to_primary:
                        # Its verified physical file is owned by one primary song.
                        # The primary's upload success will update this row too.
                        skipped_aliases += 1
                        continue
                    shard = hashlib.sha256(sid.encode('utf-8')).hexdigest()[:2]
                    folder_id = get_shard_folder(service, con, root_id, shard, cache)
                    prev = con.execute('SELECT state FROM bulk_upload_journal WHERE song_id=?',
                                       (sid,)).fetchone()
                    # For jobs previously begun, check whether Drive already has the file.
                    inspect = prev is not None
                    journal_mark(con, sid, 'started')
                    try:
                        local_path = resolve_local_path(source, prefix, relpath)
                        fut = pool.submit(upload_one, token, local_path, sid, folder_id, inspect, args.rate)
                        futures[fut] = sid
                    except Exception as exc:
                        journal_mark(con, sid, 'error', str(exc))
                        failed += 1
                        processed += 1
                con.commit()
                for fut in as_completed(futures):
                    sid = futures[fut]
                    state, fid, err = fut.result()
                    if state == 'ok' and fid:
                        con.execute('UPDATE songs SET drive_file_id=? WHERE id=?', (fid, sid))
                        aliases = primary_to_aliases.get(sid, [])
                        if aliases:
                            con.executemany('''
                                UPDATE songs SET drive_file_id=? WHERE id=?
                                  AND (drive_file_id IS NULL OR TRIM(drive_file_id)='')
                            ''', ((fid, alias_id) for alias_id in aliases))
                        # Completed entries can be removed. Only ambiguous or failed ones need recovery.
                        con.execute('DELETE FROM bulk_upload_journal WHERE song_id=?', (sid,))
                        succeeded += 1
                    else:
                        journal_mark(con, sid, 'error', err)
                        failed += 1
                        if state == 'stop':
                            stop_error = err
                        elif failed >= args.max_errors:
                            stop_error = f'Qua {args.max_errors} loi. Kiem tra nhat ky va file nguon truoc khi tiep tuc.'
                    processed += 1
                    if processed % 100 == 0:
                        con.commit()
                        elapsed = max(0.1, time.monotonic()-started_at)
                        print(f'Xu ly {processed:,} | Thanh cong {succeeded:,} | Loi {failed:,} '
                              f'| {processed/elapsed:.2f} file/s', flush=True)
                con.commit()
        print('KET QUA dot upload:')
        print(f'  Thanh cong {succeeded:,} | Loi {failed:,} | Tong xu ly {processed:,}')
        if alias_to_primary:
            print(f'  Alias bo qua upload rieng: {skipped_aliases:,} | '
                  f'Gop bang Drive ID: {len(alias_to_primary):,} alias toi da')
        progress_stats(con)
        if stop_error:
            print('DA DUNG DO LOI/GIOI HAN DRIVE:', stop_error)
            raise SystemExit(2)
    finally:
        con.commit()
        con.close()


def verify_sample(args: argparse.Namespace) -> None:
    source = Path(args.source).expanduser().resolve()
    db_path = Path(args.db).expanduser().resolve()
    token = Path(args.token).expanduser().resolve()
    con = connect_db(db_path)
    ensure_fresh_run(con)
    prefix = meta_get(con, 'relative_prefix') or source.name
    service = new_service(load_credentials(token))
    rows = con.execute('''
      SELECT id,file_path,drive_file_id FROM songs
      WHERE drive_file_id IS NOT NULL AND TRIM(drive_file_id)<>''
      ORDER BY RANDOM() LIMIT ?
    ''', (max(1, min(args.sample, 200)),)).fetchall()
    if not rows:
        print('Chua co file moi duoc upload de kiem tra.')
        return
    good = bad = 0
    for sid, relative, fid in rows:
        try:
            path = resolve_local_path(source, prefix, relative)
            md5_local = hashlib.md5()
            with path.open('rb') as fh:
                for chunk in iter(lambda: fh.read(1024 * 1024), b''):
                    md5_local.update(chunk)
            remote = service.files().get(fileId=fid, fields='id,size,md5Checksum,trashed').execute()
            if (not remote.get('trashed') and
                int(remote.get('size', -1)) == path.stat().st_size and
                remote.get('md5Checksum') == md5_local.hexdigest()):
                good += 1
            else:
                bad += 1
                print(f'SAI FILE: {relative} | Drive ID={fid}')
        except Exception as exc:
            bad += 1
            print(f'LOI KIEM TRA: {relative} | {exc}')
    print(f'Doi chieu MD5 mau: OK={good}, NG={bad}, da kiem tra={len(rows)}')
    con.close()


def main() -> None:
    parser = argparse.ArgumentParser(description='TunerTools upload GP hang loat len Google My Drive')
    subs = parser.add_subparsers(dest='command', required=True)
    ps = subs.add_parser('scan', help='Quet file, tao DB day du, khong upload')
    ps.add_argument('--source', required=True, help='Thu muc chua file GP tren Windows')
    ps.add_argument('--db', default='gp_fresh_1600000.db')
    ps.add_argument('--prefix', help='Ten folder dung trong DB; mac dinh source folder name')
    pa = subs.add_parser('auth', help='Xac thuc token va kiem tra dung luong Drive')
    pa.add_argument('--token', default='drive_token.json')
    pa.add_argument('--client-secret', default='credentials.json')
    pu = subs.add_parser('upload', help='Upload resume; luu Drive ID moi file')
    pu.add_argument('--source', required=True)
    pu.add_argument('--db', default='gp_fresh_1600000.db')
    pu.add_argument('--token', default='drive_token.json')
    pu.add_argument('--folder', default='TunerTools_GP_FRESH_1M6')
    pu.add_argument('--workers', type=int, default=4)
    pu.add_argument('--rate', type=float, default=2.5,
                    help='Gioi han upload ban ghi moi/giay; mac dinh 2.5 de tranh rate limit')
    pu.add_argument('--limit', type=int, default=100)
    pu.add_argument('--max-errors', type=int, default=30,
                    help='Tu dung neu phat sinh qua nhieu loi trong mot dot upload')
    pu.add_argument('--yes', action='store_true', help='Xac nhan upload len tai khoan Drive dang dang nhap')
    pd = subs.add_parser('dedup', help='Kiem tra file cung duong dan vat ly (chi doc)')
    pd.add_argument('--source', required=True)
    pd.add_argument('--db', default='gp_fresh_1600000.db')
    pst = subs.add_parser('status', help='Xem so file da tai len Drive')
    pst.add_argument('--db', default='gp_fresh_1600000.db')
    pv = subs.add_parser('verify', help='Doi chieu mau MD5 voi Drive')
    pv.add_argument('--source', required=True)
    pv.add_argument('--db', default='gp_fresh_1600000.db')
    pv.add_argument('--token', default='drive_token.json')
    pv.add_argument('--sample', type=int, default=20)
    args = parser.parse_args()
    try:
        if args.command == 'scan':
            scan_files(args)
        elif args.command == 'auth':
            auth_check(args)
        elif args.command == 'upload':
            upload_files(args)
        elif args.command == 'dedup':
            dedup_check(args)
        elif args.command == 'status':
            if not Path(args.db).is_file():
                raise SystemExit('Khong tim thay database. Hay giai nen ZIP hoac chay scan truoc.')
            con = connect_db(Path(args.db))
            ensure_fresh_run(con)
            progress_stats(con)
            con.close()
        elif args.command == 'verify':
            verify_sample(args)
    except KeyboardInterrupt:
        if args.command == 'scan':
            print('\nDa dung scan. Khong xoa database; co the quet lai.', flush=True)
        else:
            print('\nDa dung. Chay lai lenh upload de tiep tuc, khong xoa du lieu.', flush=True)
        raise SystemExit(130)


if __name__ == '__main__':
    main()
