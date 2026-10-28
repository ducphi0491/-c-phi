#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Kiem tra TOAN BO duong dan file GP trong catalog moi.

Chi doc SQLite va he thong file. KHONG sua database, KHONG upload Google Drive.
Vi du:
  py kiem_tra_file_truoc_upload.py --source "C:\\inetpub\\wwwroot\\tab" --db "gp_fresh_1600000.db"
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import sqlite3
import stat
import sys
import time
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description='Kiem tra file GP goc truoc khi upload Drive (READ ONLY)')
    parser.add_argument('--source', required=True, help='Thu muc GP: C:\\inetpub\\wwwroot\\tab')
    parser.add_argument('--db', default='gp_fresh_1600000.db', help='Database moi')
    parser.add_argument('--report', default='gp_kiem_tra_thieu_file.csv', help='CSV file loi (tao rieng)')
    parser.add_argument('--skip-alias-check', action='store_true', help='Tiet kiem RAM: bo kiem tra path khac chu hoa/thuong')
    parser.add_argument('--limit', type=int, default=0, help='Chi kiem tra N dong (0 = toan bo), dung cho test')
    args = parser.parse_args()

    source = Path(args.source).expanduser().resolve()
    db = Path(args.db).expanduser().resolve()
    report = Path(args.report).expanduser().resolve()
    if not source.is_dir():
        print(f'LOI: Thu muc nguon khong ton tai: {source}', flush=True)
        return 2
    if not db.is_file():
        print(f'LOI: Khong tim thay DB: {db}', flush=True)
        return 2
    if report == db or report == source:
        print('LOI: Duong dan bao cao khong duoc trung voi database/thu muc nguon.', flush=True)
        return 2
    con = sqlite3.connect(db.as_uri() + '?mode=ro', uri=True)
    con.execute('PRAGMA query_only=ON')
    try:
        metadata = dict(con.execute('SELECT key,value FROM bulk_meta WHERE key IN ("local_source", "relative_prefix", "catalog_generation")'))
        original_source = metadata.get('local_source')
        if original_source and original_source.casefold() != str(source).casefold():
            print('LOI: Thu muc nguon khac voi duong dan da ghi trong DB:', original_source, flush=True)
            return 2
        prefix = metadata.get('relative_prefix') or source.name
        print('=== KIEM TRA FILE TRUOC UPLOAD (READ ONLY) ===', flush=True)
        print('Source:', source, flush=True)
        print('Database:', db, flush=True)
        print('Prefix:', prefix + '/', flush=True)
        count = int(con.execute('SELECT COUNT(*) FROM songs').fetchone()[0])
        print('Tong ban ghi SQLite:', format(count, ','), flush=True)
        print('Dang kiem tra file that... Khong sua SQLite, khong upload.', flush=True)

        scanned = present = missing = unsafe = aliases = bytes_sum = 0
        seen_hashes: set[bytes] = set()
        start = time.monotonic()
        with report.open('w', encoding='utf-8-sig', newline='') as rf:
            writer = csv.writer(rf)
            writer.writerow(['reason', 'song_id', 'file_path', 'resolved_path'])
            cursor = con.execute('SELECT id,file_path FROM songs')
            for sid, raw in cursor:
                scanned += 1
                value = str(raw or '').replace('\\', '/')
                parts = value.split('/')
                if not parts or parts[0].casefold() != prefix.casefold() or len(parts) < 2 or any(p in ('', '.', '..') for p in parts[1:]) or ':' in value or '\x00' in value:
                    unsafe += 1
                    writer.writerow(['invalid_path', sid, value, ''])
                else:
                    rel = '/'.join(parts[1:])
                    if not args.skip_alias_check:
                        # Doi chieu duong dan khong phan biet hoa/thuong nhu Windows.
                        path_digest = hashlib.blake2b(rel.casefold().encode('utf-8', 'surrogatepass'), digest_size=16).digest()
                        if path_digest in seen_hashes:
                            aliases += 1
                        else:
                            seen_hashes.add(path_digest)
                    local = source.joinpath(*parts[1:])
                    try:
                        info = local.stat()
                        if not stat.S_ISREG(info.st_mode):
                            raise OSError('not_a_regular_file')
                        present += 1
                        bytes_sum += info.st_size
                    except (OSError, ValueError) as exc:
                        missing += 1
                        writer.writerow(['not_found', sid, value, str(local) + ' | ' + str(exc)])
                if scanned % 50000 == 0 or scanned == count:
                    rate = scanned / max(0.1, time.monotonic() - start)
                    print(f'  Checked {scanned:,}/{count:,} | OK {present:,} | Missing {missing:,} | '
                          f'Invalid {unsafe:,} | Path aliases {aliases:,} | {rate:.0f} rows/s', flush=True)
                if args.limit > 0 and scanned >= args.limit:
                    break
        print('=== KET QUA KIEM TRA ===', flush=True)
        print(f'Ban ghi da kiem tra: {scanned:,}', flush=True)
        print(f'File doc duoc: {present:,}', flush=True)
        print(f'File khong tim thay: {missing:,}', flush=True)
        print(f'Duong dan khong hop le: {unsafe:,}', flush=True)
        if args.skip_alias_check:
            print('Duong dan trung khac chu hoa/thuong: CHUA KIEM TRA', flush=True)
        else:
            print(f'Duong dan trung (khong phan biet hoa/thuong): {aliases:,}', flush=True)
        print(f'Tong dung luong tinh theo ban ghi: {bytes_sum / (1024 ** 3):.2f} GiB', flush=True)
        print('Luu y: dung luong tren co the tinh lap lai neu co nhieu ban ghi tro cung mot file.', flush=True)
        print('Bao cao file thieu / duong dan loi:', report, flush=True)
        if args.limit > 0 and scanned < count:
            print('MOI KIEM TRA MOT PHAN DO --limit. De kiem tra het, khong dung --limit.', flush=True)
        print('No database changes; no Google Drive uploads.', flush=True)
        return 0
    finally:
        con.close()


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        sys.exit('\nDa dung. SQLite goc van duoc giu nguyen.')
