# === BẢN VÁ LỖI CHO PYTHON 3.10+ (BẮT BUỘC ĐỂ LOAD FILE .PKL CŨ) ===
import collections
import collections.abc
collections.MutableSequence = collections.abc.MutableSequence
collections.Sequence = collections.abc.Sequence
collections.Mapping = collections.abc.Mapping
collections.MutableMapping = collections.abc.MutableMapping
# =========================================================

import os
import re
import sys
import glob
import json
import time
import uuid
import shutil
import logging
import hashlib
import warnings
import threading
import urllib.parse
import unicodedata
from contextlib import contextmanager
from pathlib import Path

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# --- BẢN VÁ ÉP NHẬN DIỆN FFMPEG ---
current_dir = os.path.dirname(os.path.abspath(__file__))
os.environ["PATH"] = f"{current_dir};{os.environ.get('PATH', '')}"
os.environ.setdefault("CUDA_MODULE_LOADING", "LAZY")
# ----------------------------------

import mysql.connector
from flask import Flask, request, jsonify, send_file, after_this_request
from werkzeug.utils import secure_filename

import librosa
from pydub import AudioSegment
import numpy as np
import joblib
import yt_dlp
from youtube_transcript_api import YouTubeTranscriptApi

# --- FIX LỖI THIẾU DLL CUDA TRÊN WINDOWS ---
try:
    import torch

    torch_lib_path = os.path.join(os.path.dirname(torch.__file__), "lib")
    os.environ["PATH"] = f"{torch_lib_path};{os.environ.get('PATH', '')}"
    if hasattr(os, "add_dll_directory"):
        os.add_dll_directory(torch_lib_path)
    print("✅ Đã mượn thành công kho DLL CUDA từ PyTorch!")
except Exception as e:
    print(f"⚠️ Cảnh báo tìm DLL CUDA: {e}")

# --- IMPORT THƯ VIỆN GPU & AI ---
import torch
import torchaudio
from playwright.sync_api import sync_playwright

# ==============================================================================
# KHÓA TÁCH BIỆT: GPU, PLAYWRIGHT VÀ TASK
# ==============================================================================
# Không giữ khóa GPU trong lúc mở trình duyệt hoặc tải mạng.
gpu_lock = threading.Lock()
browser_lock = threading.Lock()
analysis_lock = threading.Lock()
task_lock = threading.RLock()

# ==============================================================================
# CẤU HÌNH TỐI ƯU CÓ THỂ ĐIỀU CHỈNH BẰNG BIẾN MÔI TRƯỜNG
# ==============================================================================
def _env_int(name, default, minimum=None, maximum=None):
    try:
        value = int(os.getenv(name, str(default)))
    except (TypeError, ValueError):
        value = default
    if minimum is not None:
        value = max(minimum, value)
    if maximum is not None:
        value = min(maximum, value)
    return value


def _env_float(name, default, minimum=None, maximum=None):
    try:
        value = float(os.getenv(name, str(default)))
    except (TypeError, ValueError):
        value = default
    if minimum is not None:
        value = max(minimum, value)
    if maximum is not None:
        value = min(maximum, value)
    return value


MDX_OVERLAP = _env_float("MDX_OVERLAP", 0.10, 0.001, 0.999)
MDX_SEGMENT_SIZE = _env_int("MDX_SEGMENT_SIZE", 256, 32, 4096)
MDX_BATCH_SIZE = _env_int("MDX_BATCH_SIZE", 1, 1, 16)
ZING_MAX_SONGS = _env_int("ZING_MAX_SONGS", 3, 1, 5)
ZING_WAIT_ROUNDS = _env_int("ZING_WAIT_ROUNDS", 12, 2, 40)
ZING_WAIT_INTERVAL_MS = _env_int("ZING_WAIT_INTERVAL_MS", 500, 100, 2000)
SHOW_AI_PROGRESS_CONSOLE = os.getenv("SHOW_AI_PROGRESS_CONSOLE", "0") == "1"
ENABLE_WHISPER = os.getenv("ENABLE_WHISPER", "0") == "1"
MDX_USE_SOUNDFILE = os.getenv("MDX_USE_SOUNDFILE", "0") == "1"
TASK_TTL_SECONDS = _env_int("TASK_TTL_SECONDS", 21600, 600, 86400)

MDX_TEMP_DIR = os.path.join(current_dir, "MDX_TEMP")
os.makedirs(MDX_TEMP_DIR, exist_ok=True)
os.makedirs("YOUTUBE_CACHE", exist_ok=True)
os.makedirs("ZING_AUDIO", exist_ok=True)
os.makedirs("ZING_JSON", exist_ok=True)

# ==============================================================================
# PHẦN 1: [CORE] CẤU HÌNH HỆ THỐNG & TẢI MÔ HÌNH AI
# ==============================================================================
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
if torch.cuda.is_available():
    torch.backends.cudnn.benchmark = True
    torch.set_grad_enabled(False)

print("=" * 50)
print(
    "🚀 HỆ THỐNG ĐÃ KẾT NỐI VỚI LÕI: "
    + (torch.cuda.get_device_name(0) if torch.cuda.is_available() else "CPU")
)
print("=" * 50)

# Faster-Whisper trong file gốc chưa được gọi ở bất kỳ endpoint nào.
# Mặc định không nạp để giải phóng VRAM cho MDX. Bật lại bằng ENABLE_WHISPER=1.
whisper_model = None
if ENABLE_WHISPER:
    print("⏳ Đang nạp Faster-Whisper theo cấu hình ENABLE_WHISPER=1...")
    from faster_whisper import WhisperModel

    whisper_compute_type = "float32" if DEVICE == "cuda" else "int8"
    whisper_model = WhisperModel(
        "small",
        device=DEVICE,
        compute_type=whisper_compute_type,
    )
else:
    print("ℹ️ Bỏ qua Faster-Whisper để dành VRAM cho MDX-Net.")

print(
    "⏳ Đang nạp MDX-Net Kim_Vocal_2 "
    f"(segment={MDX_SEGMENT_SIZE}, overlap={MDX_OVERLAP}, batch={MDX_BATCH_SIZE})..."
)
from audio_separator.separator import Separator

global_separator = Separator(
    log_level=logging.INFO,
    output_dir=MDX_TEMP_DIR,
    output_format="WAV",
    sample_rate=44100,
    use_soundfile=MDX_USE_SOUNDFILE,
    use_autocast=False,
    mdx_params={
        "hop_length": 1024,
        "segment_size": MDX_SEGMENT_SIZE,
        "overlap": MDX_OVERLAP,
        "batch_size": MDX_BATCH_SIZE,
        "enable_denoise": False,
    },
)
global_separator.load_model(model_filename="Kim_Vocal_2.onnx")
print("✅ Đã nạp thành công bộ não MDX-Net!")

warnings.filterwarnings("ignore")
app = Flask(__name__)

# --- BỘ NHỚ ĐỆM QUẢN LÝ CÁC TIẾN TRÌNH AI CHẠY NGẦM ---
active_tasks = {}
task_updated_at = {}
inflight_tasks = {}


def set_task(task_id, payload):
    with task_lock:
        active_tasks[task_id] = dict(payload)
        task_updated_at[task_id] = time.time()


def update_task(task_id, **fields):
    with task_lock:
        task = active_tasks.setdefault(task_id, {"status": "processing"})
        task.update(fields)
        task_updated_at[task_id] = time.time()


def get_task_copy(task_id):
    with task_lock:
        task = active_tasks.get(task_id)
        return dict(task) if task else None


def cleanup_expired_tasks():
    cutoff = time.time() - TASK_TTL_SECONDS
    with task_lock:
        expired = [
            task_id
            for task_id, updated_at in task_updated_at.items()
            if updated_at < cutoff
            and active_tasks.get(task_id, {}).get("status") != "processing"
        ]
        for task_id in expired:
            active_tasks.pop(task_id, None)
            task_updated_at.pop(task_id, None)

        stale_keys = [
            key
            for key, task_id in inflight_tasks.items()
            if task_id not in active_tasks
            or active_tasks.get(task_id, {}).get("status") != "processing"
        ]
        for key in stale_keys:
            inflight_tasks.pop(key, None)


class ThreadSafeProgressInterceptor:
    def __init__(self):
        self.original_stderr = sys.stderr
        self.pattern = re.compile(r"(\d+)\s*%")
        self.thread_tasks = {}
        self.last_percent = {}
        self.is_phase_2 = {}
        self._lock = threading.RLock()

    def register_thread(self, task_id, base_progress, max_progress):
        tid = threading.get_ident()
        with self._lock:
            self.thread_tasks[tid] = (
                task_id,
                float(base_progress),
                float(max_progress) - float(base_progress),
            )
            self.last_percent.pop(tid, None)
            self.is_phase_2[tid] = False

    def unregister_thread(self):
        tid = threading.get_ident()
        with self._lock:
            self.thread_tasks.pop(tid, None)
            self.last_percent.pop(tid, None)
            self.is_phase_2.pop(tid, None)

    def write(self, buf):
        match = self.pattern.search(buf)

        # Giữ lại lỗi/cảnh báo thông thường; ẩn thanh tqdm mặc định để giảm I/O console.
        if SHOW_AI_PROGRESS_CONSOLE or match is None:
            self.original_stderr.write(buf)

        if match is None:
            return

        tid = threading.get_ident()
        with self._lock:
            task_config = self.thread_tasks.get(tid)
            if task_config is None:
                return

            try:
                percent_ai = int(match.group(1))
                if percent_ai < 0 or percent_ai > 100:
                    return

                previous = self.last_percent.get(tid)
                if previous == percent_ai:
                    return

                if previous is not None and percent_ai < previous - 25:
                    self.is_phase_2[tid] = True

                self.last_percent[tid] = percent_ai
                task_id, base_progress, progress_range = task_config

                if self.is_phase_2.get(tid, False):
                    overall_ai_percent = 70.0 + percent_ai * 0.30
                else:
                    overall_ai_percent = percent_ai * 0.70

                mapped_progress = (
                    base_progress
                    + overall_ai_percent * progress_range / 100.0
                )
                update_task(
                    task_id,
                    progress=round(mapped_progress, 2),
                    message=f"AI đang tách giọng ca sĩ ({int(mapped_progress)}%)...",
                )
            except Exception:
                return

    def flush(self):
        self.original_stderr.flush()


progress_interceptor = ThreadSafeProgressInterceptor()
sys.stderr = progress_interceptor


@app.route("/api/check_status/<task_id>", methods=["GET"])
def check_status(task_id):
    cleanup_expired_tasks()
    task = get_task_copy(task_id)
    if not task:
        return jsonify({"status": "error", "message": "Không tìm thấy tiến trình"}), 404
    return jsonify(task)


# ==============================================================================
# HELPER MẠNG, FILE, CACHE VÀ MDX
# ==============================================================================
_http_local = threading.local()


def get_http_session():
    session = getattr(_http_local, "session", None)
    if session is None:
        retry = Retry(
            total=2,
            connect=2,
            read=1,
            status=2,
            backoff_factor=0.35,
            status_forcelist=(429, 500, 502, 503, 504),
            allowed_methods=frozenset({"GET"}),
            raise_on_status=False,
        )
        adapter = HTTPAdapter(
            max_retries=retry,
            pool_connections=8,
            pool_maxsize=8,
        )
        session = requests.Session()
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/120.0.0.0 Safari/537.36"
                )
            }
        )
        _http_local.session = session
    return session


def safe_remove(path):
    if not path:
        return
    try:
        if os.path.isfile(path):
            os.remove(path)
    except OSError as error:
        print(f"⚠️ Không thể xóa file {path}: {error}")


def make_safe_filename(text):
    cleaned = re.sub(r'[\\/*?:"<>|]', "", text or "").strip()
    return cleaned or "Khong_ten"


def normalize_lookup_text(text):
    normalized = unicodedata.normalize("NFD", text or "")
    ascii_text = normalized.encode("ascii", "ignore").decode("utf-8").lower()
    return re.sub(r"[^a-z0-9]", "", ascii_text)


def audio_mimetype(path):
    extension = Path(path).suffix.lower()
    return {
        ".ogg": "audio/ogg",
        ".mp3": "audio/mpeg",
        ".wav": "audio/wav",
        ".m4a": "audio/mp4",
        ".aac": "audio/aac",
        ".flac": "audio/flac",
    }.get(extension, "application/octet-stream")


def find_first_existing(paths):
    for path in paths:
        if path and os.path.exists(path):
            return path
    return None


def resolve_output_path(path):
    if not path:
        return None
    if os.path.exists(path):
        return path
    candidate = os.path.join(MDX_TEMP_DIR, os.path.basename(path))
    return candidate if os.path.exists(candidate) else path


def find_stem_path(output_files, stem):
    stem = stem.lower()
    resolved = [resolve_output_path(path) for path in output_files or []]

    if stem == "vocals":
        for path in resolved:
            name = os.path.basename(path).lower()
            if "vocals" in name and "no_vocals" not in name:
                return path
    elif stem == "instrumental":
        for path in resolved:
            name = os.path.basename(path).lower()
            if "instrumental" in name or "no_vocals" in name:
                return path

    return resolved[0] if len(resolved) == 1 else None


def convert_audio_to_ogg(source_path, destination_path, bitrate="192k"):
    if not source_path or not os.path.exists(source_path):
        raise FileNotFoundError(f"Không tìm thấy nguồn audio: {source_path}")

    os.makedirs(os.path.dirname(destination_path) or ".", exist_ok=True)
    temp_output = destination_path + f".{uuid.uuid4().hex}.tmp.ogg"
    try:
        AudioSegment.from_file(source_path).export(
            temp_output,
            format="ogg",
            bitrate=bitrate,
        )
        os.replace(temp_output, destination_path)
    finally:
        safe_remove(temp_output)


def download_to_file(url, destination_path):
    os.makedirs(os.path.dirname(destination_path) or ".", exist_ok=True)
    part_path = destination_path + f".{uuid.uuid4().hex}.part"
    session = get_http_session()
    try:
        with session.get(
            url,
            stream=True,
            allow_redirects=True,
            timeout=(10, 90),
            headers={"Referer": "https://zingmp3.vn/"},
        ) as response:
            response.raise_for_status()
            with open(part_path, "wb") as output:
                for chunk in response.iter_content(chunk_size=1024 * 512):
                    if chunk:
                        output.write(chunk)
        if os.path.getsize(part_path) < 100 * 1024:
            raise RuntimeError("File nhạc tải về quá nhỏ hoặc không hợp lệ.")
        os.replace(part_path, destination_path)
    finally:
        safe_remove(part_path)


@contextmanager
def temporary_single_stem(stem_name):
    separator_old = getattr(global_separator, "output_single_stem", None)
    model_instance = getattr(global_separator, "model_instance", None)
    model_old = (
        getattr(model_instance, "output_single_stem", None)
        if model_instance is not None
        else None
    )

    try:
        if hasattr(global_separator, "output_single_stem"):
            global_separator.output_single_stem = stem_name
        if model_instance is not None and hasattr(model_instance, "output_single_stem"):
            model_instance.output_single_stem = stem_name
        yield
    finally:
        if hasattr(global_separator, "output_single_stem"):
            global_separator.output_single_stem = separator_old
        if model_instance is not None and hasattr(model_instance, "output_single_stem"):
            model_instance.output_single_stem = model_old


def separate_with_mdx(
    input_path,
    single_stem=None,
    task_id=None,
    progress_start=25,
    progress_end=90,
):
    if task_id:
        update_task(
            task_id,
            status="processing",
            progress=progress_start,
            message="Đang chờ GPU xử lý...",
        )

    queue_started = time.perf_counter()
    with gpu_lock:
        queue_wait = time.perf_counter() - queue_started
        if queue_wait > 0.5:
            print(f"⏳ Đã chờ GPU {queue_wait:.2f} giây.")

        if task_id:
            update_task(
                task_id,
                status="processing",
                progress=progress_start,
                message="GPU bắt đầu tách giọng ca sĩ...",
            )
            progress_interceptor.register_thread(
                task_id,
                progress_start,
                progress_end,
            )

        started = time.perf_counter()
        try:
            with temporary_single_stem(single_stem):
                output_files = global_separator.separate(input_path)
        finally:
            if task_id:
                progress_interceptor.unregister_thread()

        elapsed = time.perf_counter() - started
        print(f"✅ MDX hoàn tất trong {elapsed:.2f} giây.")
        return [resolve_output_path(path) for path in output_files]


def cleanup_separator_outputs(output_files, keep_paths=None):
    keep = {os.path.abspath(path) for path in (keep_paths or []) if path}
    for path in output_files or []:
        resolved = resolve_output_path(path)
        if resolved and os.path.abspath(resolved) not in keep:
            safe_remove(resolved)


def find_karaoke_cache_by_id(song_id):
    if not song_id or not os.path.isdir("ZING_JSON"):
        return None
    for filename in os.listdir("ZING_JSON"):
        if filename == f"{song_id}.json" or (
            filename.startswith(f"{song_id}_") and filename.endswith(".json")
        ):
            return os.path.join("ZING_JSON", filename)
    return None


def find_karaoke_cache_by_title(title, singer):
    wanted_title = normalize_lookup_text(title)
    wanted_singer = normalize_lookup_text(singer)
    if not wanted_title or not os.path.isdir("ZING_JSON"):
        return None

    for filename in os.listdir("ZING_JSON"):
        if not filename.endswith(".json"):
            continue
        stem = filename[:-5]
        parts = stem.split("_", 2)
        if len(parts) < 2:
            continue
        file_title = normalize_lookup_text(parts[1])
        file_singer = normalize_lookup_text(parts[2] if len(parts) >= 3 else "")
        title_matches = wanted_title in file_title or file_title in wanted_title
        singer_matches = (
            not wanted_singer
            or wanted_singer in file_singer
            or file_singer in wanted_singer
        )
        if title_matches and singer_matches:
            return os.path.join("ZING_JSON", filename)
    return None


def song_id_from_cache_path(path):
    if not path:
        return ""
    return os.path.basename(path).split("_", 1)[0].replace(".json", "")


def load_karaoke_cache(path, expected_sentence=""):
    if not path or not os.path.exists(path):
        return None
    try:
        with open(path, "r", encoding="utf-8") as file:
            data = json.load(file)
        if expected_sentence and not check_lyrics_match(data, expected_sentence):
            print(f"⚠️ JSON cache không khớp câu đầu: {os.path.basename(path)}")
            return None
        return data
    except (OSError, json.JSONDecodeError) as error:
        print(f"⚠️ Không đọc được cache JSON {path}: {error}")
        return None


def sanitize_karaoke_data(karaoke_data):
    if not isinstance(karaoke_data, dict):
        return karaoke_data
    data_block = karaoke_data.get("data")
    if isinstance(data_block, dict):
        for key in ("defaultIBGUrls", "file", "enabledVideoBG", "BGMode"):
            data_block.pop(key, None)
    return karaoke_data


def save_karaoke_cache(song_id, title, singer, karaoke_data):
    if not song_id or not karaoke_data:
        return None
    sanitize_karaoke_data(karaoke_data)
    filename = (
        f"{song_id}_{make_safe_filename(title)}_"
        f"{make_safe_filename(singer)}.json"
    )
    destination = os.path.join("ZING_JSON", filename)
    temp_path = destination + f".{uuid.uuid4().hex}.tmp"
    try:
        with open(temp_path, "w", encoding="utf-8") as file:
            json.dump(karaoke_data, file, ensure_ascii=False)
        os.replace(temp_path, destination)
        return destination
    finally:
        safe_remove(temp_path)


def find_zing_audio_cache(song_id):
    if not song_id:
        return None
    return find_first_existing(
        [
            os.path.join("ZING_AUDIO", f"zing_acapella_{song_id}.ogg"),
            os.path.join("ZING_AUDIO", f"zing_acapella_{song_id}.mp3"),
            os.path.join("ZING_AUDIO", f"zing_acapella_{song_id}.wav"),
        ]
    )


def task_key_for_zing(song_id, title, singer, mode):
    stable = "|".join(
        [
            song_id or "",
            normalize_lookup_text(title),
            normalize_lookup_text(singer),
            mode or "",
        ]
    )
    return hashlib.sha1(stable.encode("utf-8")).hexdigest()


def save_ai_meta(video_id, title, singer):
    if not video_id or not title:
        return
    cache_dir = "YOUTUBE_CACHE"
    os.makedirs(cache_dir, exist_ok=True)
    meta_file = os.path.join(cache_dir, f"meta_{video_id}.json")
    
    with open(meta_file, "w", encoding="utf-8") as f:
        json.dump({
            "id": video_id,
            "title": title,
            "singer": singer if singer else "Không rõ ca sĩ"
        }, f, ensure_ascii=False)
        print(f"📝 [META] Đã lưu thông tin bài hát: {title} ({video_id})")

print("⏳ Đang nạp hệ thống (Chế độ Hybrid: GPU STFT + Ma trận Vectơ hóa)...")
MODEL_PATH = 'nhan_dien_dieu_nhac.pkl'
if os.path.exists(MODEL_PATH):
    model = joblib.load(MODEL_PATH)
    print("✅ Đã nạp thành công bộ não Điệu nhạc: ", MODEL_PATH)
else:
    model = None
    print("⚠️ CẢNH BÁO: Không tìm thấy file nhan_dien_dieu_nhac.pkl!")

CHORDS = ['C', 'C#', 'D', 'Eb', 'E', 'F', 'F#', 'G', 'Ab', 'A', 'Bb', 'B']
templates = []
chord_labels = []

for i in range(12):
    maj_t = np.zeros(12); maj_t[[i, (i+4)%12, (i+7)%12]] = 1
    templates.append(maj_t); chord_labels.append(CHORDS[i])
    min_t = np.zeros(12); min_t[[i, (i+3)%12, (i+7)%12]] = 1
    templates.append(min_t); chord_labels.append(CHORDS[i] + 'm')
templates = np.array(templates)

maj_profile = [6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88]
min_profile = [6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17]

RHYTHM_DATABASE = [
    {"name": "Slow Rock", "div": 6, "g": [0.00, 0.00, 1.00, 0.05, 0.11, 0.00], "k": [0.19, 0.02, 0.46, 0.02, 1.00, 0.00], "s": [0.72, 0.00, 1.00, 0.15, 0.77, 0.00]},
    {"name": "Slow Rock", "div": 8, "g": [0.00, 0.19, 0.16, 0.17, 1.00, 0.00, 0.11, 0.10], "k": [0.53, 0.59, 0.00, 0.33, 0.08, 0.24, 1.00, 0.37], "s": [0.00, 0.10, 1.00, 0.01, 0.96, 0.51, 0.12, 0.87]},
    {"name": "Slow Surf / Pop", "div": 8, "g": [0.50, 0.19, 0.87, 0.00, 1.00, 0.00, 0.07, 0.00], "k": [0.07, 0.00, 0.18, 0.66, 0.12, 1.00, 0.81, 0.16], "s": [0.20, 0.06, 0.00, 0.47, 0.42, 1.00, 0.74, 0.04]},
    {"name": "Valse", "div": 6, "g": [0.85, 0.00, 0.03, 0.07, 0.48, 1.00], "k": [0.15, 0.58, 0.79, 1.00, 0.00, 0.04], "s": [0.69, 1.00, 0.02, 0.09, 0.00, 0.20]},
    {"name": "Valse Nhanh", "div": 6, "g": [0.03, 0.00, 0.07, 0.72, 1.00, 0.50], "k": [0.27, 0.00, 0.02, 0.07, 0.06, 1.00], "s": [0.55, 0.00, 0.20, 0.90, 1.00, 0.35]},
    {"name": "Valse", "div": 6, "g": [0.00, 1.00, 0.63, 0.76, 0.94, 0.04], "k": [1.00, 0.00, 0.04, 0.17, 0.00, 0.02], "s": [0.14, 1.00, 0.00, 0.46, 0.43, 0.00]},
    {"name": "Bolero / Rumba", "div": 8, "g": [0.88, 0.34, 0.80, 0.96, 0.80, 1.00, 0.00, 0.47], "k": [0.51, 0.02, 0.00, 0.22, 0.00, 0.09, 0.12, 1.00], "s": [0.00, 0.63, 0.03, 0.11, 1.00, 0.23, 0.71, 0.02]},
    {"name": "Bolero / Rumba", "div": 6, "g": [0.83, 0.00, 1.00, 0.01, 0.67, 0.00], "k": [0.00, 0.88, 0.00, 0.77, 0.00, 1.00], "s": [0.08, 0.45, 0.00, 0.07, 0.15, 1.00]},
    {"name": "Bolero / Rumba", "div": 6, "g": [0.00, 0.83, 0.00, 1.00, 0.00, 0.98], "k": [0.00, 1.00, 0.00, 0.72, 0.00, 0.82], "s": [0.06, 0.27, 0.00, 0.57, 0.05, 1.00]},
    {"name": "Bolero / Rumba", "div": 8, "g": [1.00, 0.03, 0.52, 0.32, 0.05, 0.14, 0.00, 0.00], "k": [0.00, 0.13, 0.18, 1.00, 0.04, 0.52, 0.43, 0.19], "s": [0.47, 0.12, 0.08, 0.06, 0.00, 1.00, 0.02, 0.00]},
    {"name": "Ballad", "div": 8, "g": [0.87, 0.23, 0.69, 1.00, 0.22, 0.39, 0.00, 0.21], "k": [0.54, 0.52, 1.00, 0.49, 0.01, 0.17, 0.00, 0.94], "s": [0.32, 0.38, 0.17, 1.00, 0.16, 0.07, 0.30, 0.00]},
    {"name": "Ballad", "div": 8, "g": [1.00, 0.20, 0.00, 0.38, 0.59, 0.28, 0.44, 0.81], "k": [0.33, 0.54, 0.57, 0.61, 1.00, 0.31, 0.00, 0.00], "s": [0.14, 0.00, 0.00, 0.00, 0.34, 1.00, 0.44, 0.11]},
    {"name": "Ballad", "div": 8, "g": [0.88, 0.39, 0.41, 0.29, 0.03, 0.77, 1.00, 0.00], "k": [0.00, 0.60, 0.66, 1.00, 0.43, 0.14, 0.41, 0.03], "s": [0.19, 1.00, 0.17, 0.66, 0.68, 0.00, 0.42, 0.17]},
    {"name": "Ballad", "div": 8, "g": [0.68, 0.82, 0.00, 0.15, 0.00, 1.00, 0.14, 0.12], "k": [0.75, 0.47, 0.00, 0.04, 0.03, 0.79, 0.04, 1.00], "s": [0.95, 0.41, 0.42, 0.14, 0.00, 1.00, 0.23, 0.85]},
    {"name": "Ballad", "div": 8, "g": [0.66, 0.00, 1.00, 0.25, 0.51, 0.19, 0.47, 0.17], "k": [0.78, 0.67, 0.31, 0.00, 0.03, 0.29, 0.81, 1.00], "s": [0.01, 0.11, 0.27, 0.52, 1.00, 0.34, 0.00, 0.38]}
]


# ==============================================================================
# PHẦN 2: [CHỨC NĂNG 1] CHỈ CHẠY CHỮ (LYRICS ONLY)
# ==============================================================================

def check_lyrics_match(json_data, expected_sentence):
    if not expected_sentence:
        return True
    try:
        sentences = json_data.get("data", {}).get("sentences", [])
        if not sentences: return False
        zing_text = ""
        for i in range(min(4, len(sentences))):
            for word_obj in sentences[i].get("words", []):
                zing_text += word_obj.get("data", "") + " "
                
        import unicodedata
        import re
        
        def normalize_text(text):
            # 1. Cắt bỏ số thứ tự đứng ở đầu chuỗi (vd: "1. ", "2) ", "10-")
            text = re.sub(r'^\s*[0-9]+[.,)\-]?\s*', '', text)
            
            # 2. Xóa các hợp âm trong ngoặc vuông (vd: "[Em]", "[G]")
            text = re.sub(r'\[.*?\]', '', text)
            
            # 3. Tẩy dấu Tiếng Việt và đưa về chữ thường
            t = unicodedata.normalize('NFD', text).encode('ascii', 'ignore').decode('utf-8').lower()
            
            # 4. Loại bỏ các ký tự đặc biệt, chỉ giữ lại chữ và số tự nhiên
            return re.sub(r'[^a-z0-9]', '', t)
            
        norm_expected = normalize_text(expected_sentence)
        norm_zing = normalize_text(zing_text)
        
        # Kiểm tra chéo: Dữ liệu của App nằm trong Zing hoặc Zing nằm trong App
        return (norm_expected in norm_zing) or (norm_zing in norm_expected)
    except Exception:
        return False

def fetch_zing_api_smart(title, singer):
    norm_title = normalize_lookup_text(title)
    norm_singer = normalize_lookup_text(singer)
    queries = [f"{title} {singer}".strip(), (title or "").strip()]
    session = get_http_session()

    for query in dict.fromkeys(item for item in queries if item):
        api_url = (
            "https://ac.zingmp3.vn/v1/web/search?"
            f"type=song&num=5&query={urllib.parse.quote(query)}"
        )
        try:
            response = session.get(
                api_url,
                headers={"Accept": "application/json, text/plain, */*"},
                timeout=(5, 10),
            )
            if response.status_code != 200:
                continue
            payload = response.json()
            groups = payload.get("data") or []
            if not payload.get("result") or not groups:
                continue
            songs = groups[0].get("song", [])

            if norm_singer:
                for song in songs:
                    api_title = normalize_lookup_text(song.get("title", ""))
                    api_singer = normalize_lookup_text(song.get("artistsNames", ""))
                    if norm_title in api_title and norm_singer in api_singer:
                        return song

            for song in songs:
                api_title = normalize_lookup_text(song.get("title", ""))
                if norm_title and (norm_title in api_title or api_title in norm_title):
                    return song
        except Exception as error:
            print(f"⚠️ Zing search API lỗi với '{query}': {error}")
    return None


ZING_CLICK_SONG_SCRIPT = r"""(params) => {
    const targetId = params.targetId || '';
    const avoidId = params.avoidId || '';
    const isFirstScenario = Boolean(params.isFirstScenario);
    const songIndex = Number(params.songIndex || 0);

    function isSafe(el) {
        const inPlayer = el.closest('.player-controls')
            || el.closest('.now-playing')
            || el.closest('.bottom-player');
        return !inPlayer;
    }

    const buttons = [];
    function addButton(button) {
        if (button && !buttons.includes(button) && isSafe(button)) {
            buttons.push(button);
        }
    }

    if (isFirstScenario && targetId) {
        const exactRows = document.querySelectorAll(
            '[data-id="' + targetId + '"], a[href*="' + targetId + '"]'
        );
        for (const el of exactRows) {
            const row = el.closest('.media') || el.closest('.song-item') || el.closest('li');
            if (!row || row.classList.contains('active')) continue;
            addButton(
                row.querySelector('.action-play')
                || row.querySelector('button[title="Phát"]')
                || row.querySelector('.zm-btn')
            );
        }
    }

    if (!buttons.length) {
        const links = document.querySelectorAll('a[href*="/bai-hat/"]');
        for (const link of links) {
            if (!isSafe(link)) continue;
            if (avoidId && link.href.includes(avoidId)) continue;
            const row = link.closest('.media') || link.closest('.song-item') || link.closest('li');
            if (!row) continue;
            addButton(
                row.querySelector('.action-play')
                || row.querySelector('button[title="Phát"]')
                || row.querySelector('.zm-btn')
            );
        }
    }

    if (songIndex < buttons.length) {
        buttons[songIndex].click();
        return true;
    }

    if (songIndex === 0) {
        const bottomButton = document.querySelector('.player-controls .btn-toggle-play');
        if (bottomButton) {
            bottomButton.click();
            return true;
        }
    }
    return false;
}"""

ZING_CLICK_KARAOKE_SCRIPT = r"""() => {
    const direct = document.querySelector(
        '.player-controls-right .ic-karaoke, '
        + '.player-controls-right [title*="lời bài hát"], '
        + '.player-controls-right [title*="Lời bài hát"]'
    );
    if (direct) {
        direct.click();
        return true;
    }

    for (const el of document.querySelectorAll('button, i')) {
        const className = String(el.className || '').toLowerCase();
        const title = String(el.title || '').toLowerCase();
        if (className.includes('ic-karaoke') || title.includes('lời bài hát')) {
            el.click();
            return true;
        }
    }
    return false;
}"""


def extract_zing_id(url):
    match = re.search(r"[?&]id=([A-Za-z0-9]+)", url or "")
    return match.group(1).upper() if match else ""


def build_zing_scenarios(result_url, query, title, initial_song_id):
    scenarios = []
    seen_urls = set()

    def add(name, url, avoid_id=""):
        if not url or url in seen_urls:
            return
        seen_urls.add(url)
        scenarios.append({"name": name, "url": url, "avoid_id": avoid_id})

    add("Kịch bản trực tiếp", result_url)
    if query:
        add(
            "Tìm đúng tên và ca sĩ",
            "https://zingmp3.vn/tim-kiem/bai-hat?q="
            + urllib.parse.quote(query),
            initial_song_id or "",
        )
    if title and normalize_lookup_text(title) != normalize_lookup_text(query):
        add(
            "Tìm mở rộng theo tên bài",
            "https://zingmp3.vn/tim-kiem/bai-hat?q="
            + urllib.parse.quote(title),
        )
    return scenarios


def scrape_zing_sources(
    *,
    result_url,
    query,
    title,
    initial_song_id="",
    expected_sentence="",
    cached_karaoke=None,
    need_lyrics=True,
    need_audio=False,
    strict_song_id="",
):
    lyrics_by_id = {}
    audio_by_id = {}
    seen_ids = []
    last_target_url = result_url or ""

    if cached_karaoke and initial_song_id:
        lyrics_by_id[initial_song_id] = cached_karaoke
        seen_ids.append(initial_song_id)

    def remember_id(song_id):
        if song_id and song_id not in seen_ids:
            seen_ids.append(song_id)

    def select_candidate():
        if strict_song_id:
            candidates = [strict_song_id]
        else:
            candidates = []
            if initial_song_id:
                candidates.append(initial_song_id)
            candidates.extend(song_id for song_id in seen_ids if song_id not in candidates)
            candidates.extend(
                song_id
                for song_id in list(lyrics_by_id) + list(audio_by_id)
                if song_id not in candidates
            )

        for song_id in candidates:
            has_lyrics = song_id in lyrics_by_id
            has_audio = song_id in audio_by_id
            if need_lyrics and not has_lyrics:
                continue
            if need_audio and not has_audio:
                continue
            return (
                lyrics_by_id.get(song_id),
                audio_by_id.get(song_id),
                song_id,
            )
        return None

    scenarios = build_zing_scenarios(
        result_url,
        query,
        title,
        initial_song_id,
    )
    if not scenarios:
        return None, None, initial_song_id, last_target_url

    with browser_lock:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(
                headless=True,
                args=["--disable-blink-features=AutomationControlled"],
            )
            try:
                context = browser.new_context(
                    user_agent=(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "Chrome/120.0.0.0 Safari/537.36"
                    )
                )
                context.add_init_script(
                    "Object.defineProperty(navigator, 'webdriver', "
                    "{get: () => undefined})"
                )
                page = context.new_page()

                def handle_response(response):
                    response_id = extract_zing_id(response.url)

                    if need_lyrics and "/api/v2/lyric/get/lyric" in response.url:
                        try:
                            data = response.json()
                            valid = (
                                data.get("err") == 0
                                and data.get("data")
                                and "sentences" in data.get("data", {})
                                and check_lyrics_match(data, expected_sentence)
                            )
                            if valid:
                                song_id = response_id or initial_song_id
                                if song_id:
                                    lyrics_by_id[song_id] = data
                                    remember_id(song_id)
                                    print(f"✅ [ZING] Đã nhận JSON lời đúng (ID: {song_id})")
                            else:
                                print("❌ [ZING] JSON lời không khớp câu mẫu, bỏ qua.")
                        except Exception:
                            pass

                    if need_audio and "/api/v2/song/get/streaming" in response.url:
                        try:
                            data = response.json()
                            if data.get("err") == 0:
                                audio_url = (
                                    data.get("data", {}).get("128")
                                    or data.get("data", {}).get("default")
                                )
                                song_id = response_id or initial_song_id
                                if audio_url and song_id:
                                    audio_by_id[song_id] = audio_url
                                    remember_id(song_id)
                                    print(f"✅ [ZING] Đã nhận URL audio (ID: {song_id})")
                        except Exception:
                            pass

                page.on("response", handle_response)

                for scenario_index, scenario in enumerate(scenarios):
                    selected = select_candidate()
                    if selected is not None:
                        return (*selected, last_target_url)

                    last_target_url = scenario["url"]
                    print(f"👉 [ZING] {scenario['name']}: {last_target_url}")
                    try:
                        page.goto(last_target_url, timeout=30000, wait_until="domcontentloaded")
                        page.wait_for_timeout(1500)
                    except Exception as error:
                        print(f"⚠️ Không mở được trang Zing: {error}")
                        continue

                    selected = select_candidate()
                    if selected is not None:
                        return (*selected, last_target_url)

                    for song_index in range(ZING_MAX_SONGS):
                        clicked = page.evaluate(
                            ZING_CLICK_SONG_SCRIPT,
                            {
                                "targetId": initial_song_id or "",
                                "avoidId": scenario.get("avoid_id", ""),
                                "isFirstScenario": scenario_index == 0,
                                "songIndex": song_index,
                            },
                        )
                        if not clicked:
                            break

                        print(f"   ┣ 🎵 Đang thử bài thứ {song_index + 1}...")
                        page.wait_for_timeout(1200)

                        selected = select_candidate()
                        if selected is not None:
                            return (*selected, last_target_url)

                        # Chỉ mở karaoke khi chưa có cặp dữ liệu phù hợp.
                        if need_lyrics:
                            page.evaluate(ZING_CLICK_KARAOKE_SCRIPT)

                        for _ in range(ZING_WAIT_ROUNDS):
                            selected = select_candidate()
                            if selected is not None:
                                return (*selected, last_target_url)
                            page.wait_for_timeout(ZING_WAIT_INTERVAL_MS)

                selected = select_candidate()
                if selected is not None:
                    return (*selected, last_target_url)
                return None, None, initial_song_id, last_target_url
            finally:
                browser.close()


@app.route("/api/search_zing_mp3", methods=["POST"])
def search_zing_mp3():
    zing_id = request.form.get("zing_id", "").strip()
    title = request.form.get("title", "").strip()
    singer = request.form.get("singer", "").strip()
    first_sentence = request.form.get("first_sentence", "").strip()
    query = f"{title} {singer}".strip()

    print("\n" + "=" * 50)
    print(f"🔍 [ZING LYRICS] '{query}' (ID: {zing_id})")

    if not query and not zing_id:
        return jsonify({"success": False, "error": "Thiếu từ khóa tìm kiếm"}), 400

    started = time.perf_counter()
    cache_path = find_karaoke_cache_by_id(zing_id)
    if not cache_path:
        cache_path = find_karaoke_cache_by_title(title, singer)

    cached_data = load_karaoke_cache(cache_path, first_sentence)
    if cached_data is not None:
        cached_id = song_id_from_cache_path(cache_path) or zing_id
        print(
            f"⚡ [CACHE HIT] {os.path.basename(cache_path)} "
            f"trong {time.perf_counter() - started:.3f}s"
        )
        return jsonify(
            {
                "success": True,
                "url": f"https://zingmp3.vn/bai-hat/Zing/{cached_id}.html",
                "id": cached_id,
                "karaoke_data": cached_data,
            }
        )

    song_id = zing_id
    result_url = (
        f"https://zingmp3.vn/bai-hat/Zing/{song_id}.html"
        if song_id
        else ""
    )

    if not song_id:
        target_song = fetch_zing_api_smart(title, singer)
        if target_song:
            song_id = target_song.get("id", "")
            result_url = f"https://zingmp3.vn{target_song.get('link', '')}"

    karaoke_data, _, selected_id, result_url = scrape_zing_sources(
        result_url=result_url,
        query=query,
        title=title,
        initial_song_id=song_id,
        expected_sentence=first_sentence,
        need_lyrics=True,
        need_audio=False,
    )

    if karaoke_data is None:
        print("❌ Không tìm thấy dữ liệu lời bài hát phù hợp.")
        return jsonify(
            {
                "success": False,
                "error": "Không tìm thấy dữ liệu lời bài hát",
                "fallback_url": result_url,
            }
        )

    selected_id = selected_id or song_id
    cache_path = save_karaoke_cache(
        selected_id,
        title,
        singer,
        karaoke_data,
    )
    print(
        f"🎉 Hoàn tất lấy lời trong {time.perf_counter() - started:.2f}s; "
        f"cache={cache_path}"
    )
    return jsonify(
        {
            "success": True,
            "url": result_url,
            "id": selected_id,
            "karaoke_data": karaoke_data,
        }
    )


# ==============================================================================
# PHẦN 3: [CHỨC NĂNG 2] TÁCH VOCAL / INSTRUMENTAL TỪ FILE ANDROID
# ==============================================================================
def youtube_cache_candidates(video_id, requested_stem):
    if requested_stem == "Vocals":
        base_names = [
            f"acapella_{video_id}.ogg",
            f"acapella_{video_id}.mp3",
            f"acapella_{video_id}.wav",
        ]
    else:
        base_names = [
            f"beat_karaoke_{video_id}.ogg",
            f"beat_karaoke_{video_id}.wav",
            f"beat_karaoke_{video_id}.mp3",
        ]
    return [os.path.join("YOUTUBE_CACHE", name) for name in base_names]


def handle_uploaded_separation(requested_stem):
    video_id = request.form.get("video_id", "").strip()
    title = request.form.get("title", "").strip()
    singer = request.form.get("singer", "").strip()

    if video_id and title:
        save_ai_meta(video_id, title, singer)

    if video_id:
        cached = find_first_existing(youtube_cache_candidates(video_id, requested_stem))
        if cached:
            print(f"⚡ [CACHE HIT] {requested_stem}: {cached}")
            return send_file(
                cached,
                mimetype=audio_mimetype(cached),
                as_attachment=True,
                download_name=(
                    "vocals_only" if requested_stem == "Vocals" else "beat"
                )
                + Path(cached).suffix,
                conditional=True,
            )

    if "file" not in request.files:
        return jsonify({"error": "Không tìm thấy file âm thanh."}), 400

    uploaded_file = request.files["file"]
    original_name = secure_filename(uploaded_file.filename or "")
    if not original_name:
        original_name = "audio_input.bin"

    actual_file = os.path.join(
        "YOUTUBE_CACHE",
        f"temp_{uuid.uuid4().hex}_{original_name}",
    )
    uploaded_file.save(actual_file)
    output_files = []
    temporary_response_file = None

    try:
        file_size_mb = os.path.getsize(actual_file) / (1024 * 1024)
        print(f"📦 File nhận được: {file_size_mb:.2f} MB")
        if file_size_mb < 0.1:
            raise RuntimeError("File âm thanh gửi lên quá nhỏ.")

        # Nếu có video_id, một lần AI sẽ lưu luôn cả Vocal và Beat để endpoint còn lại
        # không phải chạy MDX lần thứ hai.
        output_files = separate_with_mdx(
            actual_file,
            single_stem=None if video_id else requested_stem,
        )
        vocal_path = find_stem_path(output_files, "Vocals")
        instrumental_path = find_stem_path(output_files, "Instrumental")

        if requested_stem == "Vocals" and not vocal_path:
            raise RuntimeError("MDX không trả về stem Vocals.")
        if requested_stem == "Instrumental" and not instrumental_path:
            raise RuntimeError("MDX không trả về stem Instrumental.")

        if video_id:
            vocal_cache = os.path.join(
                "YOUTUBE_CACHE",
                f"acapella_{video_id}.ogg",
            )
            beat_cache = os.path.join(
                "YOUTUBE_CACHE",
                f"beat_karaoke_{video_id}.ogg",
            )
            if vocal_path and not os.path.exists(vocal_cache):
                convert_audio_to_ogg(vocal_path, vocal_cache)
            if instrumental_path and not os.path.exists(beat_cache):
                convert_audio_to_ogg(instrumental_path, beat_cache)

            response_file = vocal_cache if requested_stem == "Vocals" else beat_cache
        else:
            source = vocal_path if requested_stem == "Vocals" else instrumental_path
            temporary_response_file = os.path.join(
                "YOUTUBE_CACHE",
                f"response_{uuid.uuid4().hex}.ogg",
            )
            convert_audio_to_ogg(source, temporary_response_file)
            response_file = temporary_response_file

        cleanup_separator_outputs(output_files)
        output_files = []
        safe_remove(actual_file)

        if temporary_response_file:
            @after_this_request
            def remove_temp_response(response):
                safe_remove(temporary_response_file)
                return response

        return send_file(
            response_file,
            mimetype="audio/ogg",
            as_attachment=True,
            download_name=(
                "vocals_only.ogg"
                if requested_stem == "Vocals"
                else "beat.ogg"
            ),
            conditional=True,
        )
    except Exception as error:
        print(f"❌ Lỗi tách {requested_stem}: {error}")
        return jsonify({"error": str(error)}), 500
    finally:
        safe_remove(actual_file)
        cleanup_separator_outputs(output_files)


@app.route("/api/get_vocals_only", methods=["POST"])
def get_vocals_only():
    print("\n" + "=" * 50)
    print(f"🎤 YÊU CẦU LẤY GIỌNG CA SĨ TỪ: {request.remote_addr}")
    return handle_uploaded_separation("Vocals")


@app.route("/api/separate_vocals", methods=["POST"])
def separate_vocals():
    print("\n" + "=" * 50)
    print(f"🎙️ YÊU CẦU LẤY NHẠC KARAOKE TỪ: {request.remote_addr}")
    return handle_uploaded_separation("Instrumental")


# ==============================================================================
# PHẦN 4: [CHỨC NĂNG 3] GIỌNG HÁT + CHẠY CHỮ (KARAOKE CHUẨN)
# ==============================================================================

@app.route('/api/get_direct_link', methods=['POST'])
def get_direct_link():
    data = request.get_json()
    if not data or 'url' not in data:
        return jsonify({"error": "Không tìm thấy URL."}), 400

    youtube_url = data['url']
    print(f"\n🔗 Đang bóc tách lấy link gốc cho: {youtube_url}")

    # BẢN VÁ: Đổi client sang Web và Tv để lách luật PO Token mới của YouTube
    ydl_opts = {
        'format': 'bestaudio[ext=m4a]/bestaudio/best',
        'quiet': True,
        'extractor_args': {
            'youtube': {
                'player_client': ['web', 'tv', 'default'],
                'player_skip': ['js']
            }
        }
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
            direct_url = info['url']
            print("✅ Đã lấy được link stream trực tiếp (Bypass PO Token)!")
            return jsonify({"url": direct_url})
    except Exception as e:
        print(f"❌ Lỗi bóc tách: {str(e)}")
        return jsonify({"error": f"Lỗi bóc tách link: {str(e)}"}), 500


# ==============================================================================
# PHẦN 5: [CHỨC NĂNG 4] PHÂN TÍCH HỢP ÂM & NHỊP ĐIỆU ĐA CHIỀU
# ==============================================================================

@app.route('/api/analyze_file', methods=['POST']) 
def analyze_audio_file():
    t_total_start = time.time()  

    print("\n" + "="*50)
    print(f"🔥 [THÔNG BÁO] YÊU CẦU PHÂN TÍCH HỢP ÂM MỚI TỪ: {request.remote_addr}")
    
    if 'file' not in request.files:
        return jsonify({"error": "Không tìm thấy file âm thanh."}), 400

    uploaded_file = request.files['file']
    if uploaded_file.filename == '':
        return jsonify({"error": "Tên file không hợp lệ."}), 400

    timestamp = int(time.time())
    chord_dir = "CHORD_AI"
    os.makedirs(chord_dir, exist_ok=True)
    safe_upload_name = secure_filename(uploaded_file.filename) or "audio_input.bin"
    output_wav = os.path.join(
        chord_dir,
        f"temp_upload_{timestamp}_{uuid.uuid4().hex}_{safe_upload_name}",
    )
    uploaded_file.save(output_wav)
    
    file_size_mb = round(os.path.getsize(output_wav) / (1024 * 1024), 2)
    print(f"✅ [THÀNH CÔNG] Đã nhận file từ Android! ({file_size_mb} MB)")
    print("="*50 + "\n")

    try:
        # Mỗi lần chỉ chạy một job phân tích; chỉ khóa GPU ở đúng đoạn STFT.
        with analysis_lock:
            print("⚙️ Đang phân tích (BPM, Tone, Hợp âm, Điệu nhạc)...")
            SAMPLE_RATE = 22050
            
            t_load_start = time.time()
            try:
                waveform, sr_orig = torchaudio.load(output_wav)
                if sr_orig != SAMPLE_RATE:
                    resampler = torchaudio.transforms.Resample(orig_freq=sr_orig, new_freq=SAMPLE_RATE)
                    waveform = resampler(waveform)
                if waveform.shape[0] > 1:
                    waveform = torch.mean(waveform, dim=0, keepdim=True)
                y = waveform[0].numpy()
            except Exception:
                y, sr_orig = librosa.load(output_wav, sr=SAMPLE_RATE)
            
            sr = SAMPLE_RATE
            duration_ms = int((len(y) / sr) * 1000)
            t_load_end = time.time()
            print(f"   ┣ ⏱️ [1/7] Đọc file audio (Torchaudio Siêu Tốc): {round(t_load_end - t_load_start, 3)}s")

            t_hpss_start = time.time()
            with gpu_lock:
                y_tensor = torch.from_numpy(y).to(DEVICE)
                window = torch.hann_window(2048).to(DEVICE)
                S_complex = torch.stft(
                    y_tensor,
                    n_fft=2048,
                    hop_length=512,
                    window=window,
                    return_complex=True,
                )
                S = torch.abs(S_complex).cpu().numpy()
                del y_tensor, window, S_complex

            chromagram = librosa.feature.chroma_stft(S=S, sr=sr)
            t_hpss_end = time.time()
            print(f"   ┣ ⏱️ [2/7] Ma trận GPU & Chroma: {round(t_hpss_end - t_hpss_start, 3)}s")
            
            t_beat_start = time.time()
            onset_env = librosa.onset.onset_strength(S=S, sr=sr)
            tempo, beat_frames = librosa.beat.beat_track(onset_envelope=onset_env, sr=sr)
            raw_bpm = int(tempo[0]) if isinstance(tempo, np.ndarray) else int(tempo)
            beats_sec = librosa.frames_to_time(beat_frames, sr=sr)
            
            mean_onset = float(np.mean(onset_env))
            bpm = raw_bpm
            while bpm < 55: bpm *= 2
            while bpm > 140: bpm = int(bpm / 2)
            if raw_bpm > 130 and mean_onset < 2.2:
                bpm = int(raw_bpm / 2)
            t_beat_end = time.time()
            print(f"   ┣ ⏱️ [3/7] Dò Nhịp độ (Tái sử dụng S chống nghẽn): {round(t_beat_end - t_beat_start, 3)}s")

            t_tone_start = time.time()
            chroma_sum = np.sum(chromagram, axis=1)
            max_score = -float('inf')
            detected_tone = "C"
            for i in range(12):
                maj_score = float(np.corrcoef(np.roll(maj_profile, i), chroma_sum)[0, 1])
                min_score = float(np.corrcoef(np.roll(min_profile, i), chroma_sum)[0, 1])
                if maj_score > max_score: max_score = maj_score; detected_tone = CHORDS[i]
                if min_score > max_score: max_score = min_score; detected_tone = CHORDS[i] + "m"
            t_tone_end = time.time()
            print(f"   ┣ ⏱️ [4/7] Dò Tone (Giọng chuẩn): {round(t_tone_end - t_tone_start, 3)}s")

            t_ai_start = time.time()
            ai_best_genre = "Tự do"
            if model:
                mfcc_full = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
                contrast_full = librosa.feature.spectral_contrast(S=S, sr=sr)
                centroid_full = librosa.feature.spectral_centroid(S=S, sr=sr)
                bw_full = librosa.feature.spectral_bandwidth(S=S, sr=sr)
                rolloff_full = librosa.feature.spectral_rolloff(S=S, sr=sr)
                zcr_full = librosa.feature.zero_crossing_rate(y=y)
                
                SEGMENT_DURATION = 3
                frames_per_segment = librosa.time_to_frames(SEGMENT_DURATION, sr=sr)
                num_segments = chromagram.shape[1] // frames_per_segment
                
                if num_segments > 0:
                    predictions = []
                    for i in range(num_segments):
                        start_f = i * frames_per_segment
                        end_f = start_f + frames_per_segment
                        
                        mfcc = np.mean(mfcc_full[:, start_f:end_f], axis=1)
                        chroma = np.mean(chromagram[:, start_f:end_f], axis=1)
                        contrast = np.mean(contrast_full[:, start_f:end_f], axis=1)
                        centroid = np.mean(centroid_full[:, start_f:end_f], axis=1)
                        bandwidth = np.mean(bw_full[:, start_f:end_f], axis=1)
                        rolloff = np.mean(rolloff_full[:, start_f:end_f], axis=1)
                        zcr = np.mean(zcr_full[:, start_f:end_f], axis=1)
                        
                        features_43 = np.hstack([mfcc, chroma, contrast, centroid, bandwidth, rolloff, zcr])
                        predictions.append(model.predict(features_43.reshape(1, -1))[0])
                        
                    unique_genres, counts = np.unique(predictions, return_counts=True)
                    ai_best_genre = str(unique_genres[np.argmax(counts)]).title()
            t_ai_end = time.time()
            print(f"   ┣ ⏱️ [5/7] Ma thuật Vectơ hóa AI (Khắc phục hoàn toàn sập RAM): {round(t_ai_end - t_ai_start, 3)}s")

            t_math_start = time.time()
            freqs = librosa.fft_frequencies(sr=sr)
            kick_idx = np.where((freqs >= 40) & (freqs <= 120))[0]
            snare_idx = np.where((freqs >= 200) & (freqs <= 400))[0]
            
            kick_energy = np.sum(S[kick_idx, :], axis=0)
            snare_energy = np.sum(S[snare_idx, :], axis=0)
            times = librosa.frames_to_time(np.arange(S.shape[1]), sr=sr)
            
            def create_normalized_grids(divisions, beats_per_measure, bpm_val):
                measure_duration_sec = (60.0 / bpm_val) * beats_per_measure
                groove_grid = np.zeros(divisions)
                kick_grid = np.zeros(divisions)
                snare_grid = np.zeros(divisions)
                
                for i, t in enumerate(times):
                    time_in_measure = t % measure_duration_sec
                    grid_index = int((time_in_measure / measure_duration_sec) * divisions)
                    if grid_index >= divisions: grid_index = divisions - 1
                    
                    groove_grid[grid_index] += onset_env[i]
                    kick_grid[grid_index] += kick_energy[i]
                    snare_grid[grid_index] += snare_energy[i]
                    
                def normalize(grid):
                    max_val = np.max(grid) if np.max(grid) > 0 else 1.0
                    min_val = np.min(grid)
                    rnge = max(max_val - min_val, 0.0001)
                    return [float(((v - min_val) / rnge)**2) for v in grid]
                    
                return normalize(groove_grid), normalize(kick_grid), normalize(snare_grid)
                
            g6, k6, s6 = create_normalized_grids(6, 3, bpm)
            g8, k8, s8 = create_normalized_grids(8, 4, bpm)
            
            math_best_match = "Tự do"
            min_error = float('inf')
            matched_div = 8
            error_list = []
            
            for template in RHYTHM_DATABASE:
                div = template['div']
                g, k, s = (g6, k6, s6) if div == 6 else (g8, k8, s8)
                error = 0.0
                for i in range(div):
                    g_diff = abs(g[i] - template['g'][i])
                    k_diff = abs(k[i] - template['k'][i])
                    s_diff = abs(s[i] - template['s'][i])
                    weight = 1.2 if i % 2 == 0 else 0.8
                    error += (g_diff * 1.0 + k_diff * 1.2 + s_diff * 1.0) * weight
                error /= div
                error_list.append((template['name'], div, error))
                if error < min_error:
                    min_error = error
                    math_best_match = template['name']
                    matched_div = div
            error_list.sort(key=lambda x: x[2])

            if min_error <= 1.0:
                if matched_div == 6:
                    is_false_valse = "valse" in math_best_match.lower() and ai_best_genre.lower() in ["bolero", "ballad", "pop"]
                    is_false_44_in_grid_6 = math_best_match in ["Bolero / Rumba", "Ballad", "Slow Surf / Pop"] and ai_best_genre.lower() in ["ballad", "slow", "pop"]
                    
                    if (is_false_valse or is_false_44_in_grid_6) and min_error > 0.7:
                        fallback_genre_8 = None
                        for idx in range(min(3, len(error_list))):
                            if error_list[idx][1] == 8 and error_list[idx][2] <= 1.2:
                                fallback_genre_8 = error_list[idx][0]
                                break
                        if fallback_genre_8:
                            best_genre = fallback_genre_8 if (ai_best_genre.lower() in fallback_genre_8.lower() or fallback_genre_8.lower() in ai_best_genre.lower()) else ai_best_genre
                            time_sig = "4/4"
                        else:
                            best_genre = ai_best_genre
                            time_sig = "4/4"
                    elif math_best_match == "Slow Rock" or ai_best_genre.lower() == "slow" or bpm < 75:
                        best_genre = "Slow Rock"
                        time_sig = "6/8"
                    else:
                        best_genre = math_best_match
                        genre_lower_math = best_genre.lower()
                        if "slow rock" in genre_lower_math: time_sig = "6/8"
                        elif "valse" in genre_lower_math or "boston" in genre_lower_math: time_sig = "3/4"
                        else: time_sig = "3/4" 
                elif matched_div == 8:
                    bolero_is_close = any("Bolero" in error_list[idx][0] and error_list[idx][2] <= 1.05 for idx in range(min(2, len(error_list))))
                    if math_best_match.lower() == ai_best_genre.lower():
                        best_genre = math_best_match
                        time_sig = "4/4"
                    elif bolero_is_close:
                        best_genre = "Bolero / Rumba"
                        time_sig = "4/4"
                    elif math_best_match in ["Ballad", "Slow Surf / Pop"] and ai_best_genre.lower() == "slow":
                        best_genre = "Slow"
                        time_sig = "4/4"
                    else:
                        best_genre = math_best_match
                        time_sig = "4/4" if "slow rock" not in best_genre.lower() else "4/4"
            else:
                best_genre = ai_best_genre
                genre_lower = best_genre.lower()
                if "valse" in genre_lower or "boston" in genre_lower: time_sig = "3/4"
                elif "slow rock" in genre_lower or "slow" in genre_lower: time_sig = "4/4" 
                else: time_sig = "4/4"

            t_math_end = time.time()
            print(f"   ┣ ⏱️ [6/7] So khớp từ điển Toán học MAE & Hybrid Fusion: {round(t_math_end - t_math_start, 3)}s")

            t_chord_start = time.time()
            raw_chords = []
            if len(beats_sec) > 0 and beats_sec[0] < 0.5: beats_sec = beats_sec[1:]

            for i in range(len(beats_sec)):
                b_time = beats_sec[i]
                next_time = beats_sec[i+1] if i + 1 < len(beats_sec) else b_time + (60.0 / bpm)
                start_frame, end_frame = librosa.time_to_frames(b_time, sr=sr), librosa.time_to_frames(next_time, sr=sr)
                if start_frame == end_frame: end_frame += 1
                
                chroma_slice = np.mean(chromagram[:, start_frame:end_frame], axis=1)
                scores = np.dot(templates, chroma_slice)
                raw_chords.append(chord_labels[np.argmax(scores)])

            filtered_chords = []
            for i in range(len(raw_chords)):
                if 0 < i < len(raw_chords) - 1:
                    prev_c, curr_c, next_c = raw_chords[i-1], raw_chords[i], raw_chords[i+1]
                    if curr_c != prev_c and prev_c == next_c: filtered_chords.append(prev_c)
                    elif curr_c != prev_c and curr_c != next_c: filtered_chords.append(prev_c)
                    else: filtered_chords.append(curr_c)
                else:
                    filtered_chords.append(raw_chords[i])

            chords_timeline, last_chord = [], ""
            for i in range(len(beats_sec)):
                chord_name = filtered_chords[i]
                if chord_name != last_chord:
                    chords_timeline.append({"time": float(beats_sec[i]), "chord": chord_name})
                    last_chord = chord_name

            rms = librosa.feature.rms(y=y)[0]
            times_rms = librosa.frames_to_time(np.arange(len(rms)), sr=sr)
            waveform_list = [{"time": int(times_rms[i] * 1000), "energy": float(rms[i] * 5.0)} for i in range(0, len(rms), 10)]
            t_chord_end = time.time()
            print(f"   ┣ ⏱️ [7/7] Quét Hợp âm & Đóng gói Waveform JSON: {round(t_chord_end - t_chord_start, 3)}s")

            end_time = time.time()
            processing_time_sec = round(end_time - t_total_start, 2)
            print(f"   ┗ 🏁 TỔNG THỜI GIAN PHÂN TÍCH: {processing_time_sec}s")

            final_result = {
                "meta": {"tone": detected_tone, "bpm": bpm, "nhip": time_sig, "dieu": best_genre},
                "chords": chords_timeline,
                "duration": float(duration_ms / 1000.0),
                "waveform": waveform_list
            }

            if os.path.exists(output_wav): os.remove(output_wav)
            print(f"🎯 Trả kết quả JSON về App! (Tone: {detected_tone}, Nhịp: {time_sig}, Điệu: {best_genre})")
            return jsonify(final_result)

    except Exception as e:
        if os.path.exists(output_wav): os.remove(output_wav)
        print(f"❌ LỖI TRONG QUÁ TRÌNH PHÂN TÍCH: {str(e)}")
        return jsonify({"error": f"Lỗi máy chủ: {str(e)}"}), 500


# ==============================================================================
# PHẦN 6: [TIỆN ÍCH] KHO NHẠC CACHE & TRẠNG THÁI MÁY CHỦ
# ==============================================================================

@app.route('/api/get_cached_songs', methods=['GET'])
def get_cached_songs():
    songs_list = []
    
    zing_folder = "ZING_JSON"
    zing_audio_folder = "ZING_AUDIO"
    yt_folder = "YOUTUBE_CACHE"
    
    # === TỐI ƯU HÓA: Đọc trước danh sách file vào RAM (Set) để tra cứu O(1) ===
    zing_audio_files = set(os.listdir(zing_audio_folder)) if os.path.exists(zing_audio_folder) else set()
    yt_files = set(os.listdir(yt_folder)) if os.path.exists(yt_folder) else set()
    
    # 1. QUÉT BÀI HÁT TỪ ZING MP3
    if os.path.exists(zing_folder):
        for fname in os.listdir(zing_folder):
            if fname.endswith(".json"):
                parts = fname.replace(".json", "").split("_")
                if len(parts) >= 3:
                    song_id = parts[0]
                    title = parts[1]
                    singer = "_".join(parts[2:]) 
                    
                    # So sánh trực tiếp trên RAM thay vì gọi ổ cứng os.path.exists
                    has_audio = (
                        f"zing_acapella_{song_id}.mp3" in zing_audio_files or 
                        f"zing_acapella_{song_id}.ogg" in zing_audio_files
                    )

                    songs_list.append({
                        "id": song_id,
                        "title": title,
                        "singer": singer,
                        "source": "zing",
                        "has_lyrics_cache": True,
                        "has_audio_cache": has_audio 
                    })
                    
    # 2. QUÉT BÀI HÁT TỪ YOUTUBE
    if os.path.exists(yt_folder):
        # Duyệt trực tiếp mảng yt_files đã load vào RAM
        for fname in yt_files:
            if fname.startswith("meta_") and fname.endswith(".json"):
                meta_path = os.path.join(yt_folder, fname)
                try:
                    with open(meta_path, "r", encoding="utf-8") as f:
                        meta_data = json.load(f)
                        video_id = meta_data.get("id", "")
                        
                        if video_id:
                            # So sánh trực tiếp trên RAM thay vì gọi ổ cứng os.path.exists
                            has_audio = (
                                f"acapella_{video_id}.ogg" in yt_files or 
                                f"beat_karaoke_{video_id}.ogg" in yt_files or
                                f"acapella_{video_id}.wav" in yt_files or 
                                f"beat_karaoke_{video_id}.wav" in yt_files or
                                f"acapella_{video_id}.mp3" in yt_files or      
                                f"beat_karaoke_{video_id}.mp3" in yt_files
                            )
                            has_lyrics = f"lyrics_{video_id}.json" in yt_files
                            
                            songs_list.append({
                                "id": video_id,
                                "title": meta_data.get("title", "Không tên"),
                                "singer": meta_data.get("singer", "Không rõ ca sĩ"),
                                "source": "youtube",
                                "has_lyrics_cache": has_lyrics,
                                "has_audio_cache": has_audio
                            })
                except Exception as e:
                    pass
                    
    # Tùy chọn: Bạn có thể đưa hàm sắp xếp bằng Python vào ngay đây nếu muốn (Sắp xếp theo tên hoặc ID)
    # Tuy nhiên kết hợp với đoạn code Kotlin (sắp xếp theo viewCount) ở tin nhắn trước là chuẩn nhất.
    return jsonify({"songs": songs_list})

import glob # Import thêm thư viện glob để tìm kiếm file

@app.route('/api/delete_cache', methods=['POST'])
def delete_cache():
    video_id = request.form.get("video_id", "")
    if not video_id:
        return jsonify({"success": False, "message": "Thiếu video_id"}), 400
    
    deleted_files = []
    
    # 1. XÓA FILE VẬT LÝ
    folders_to_clean = ["YOUTUBE_CACHE", "ZING_AUDIO", "ZING_JSON"]
    for folder in folders_to_clean:
        if os.path.exists(folder):
            search_pattern = os.path.join(folder, f"*{video_id}*.*")
            files = glob.glob(search_pattern)
            for file_path in files:
                try:
                    os.remove(file_path)
                    deleted_files.append(file_path)
                except Exception as e:
                    print(f"Lỗi khi xóa {file_path}: {e}")
    
    # 2. ĐỒNG BỘ: XÓA ID TRONG CƠ SỞ DỮ LIỆU MYSQL
    db_msg = ""
    try:
        conn = mysql.connector.connect(
            host='127.0.0.1', user='root', password='123456', database='tuner', port=3306
        )
        cursor = conn.cursor()
        
        # Chỉ làm rỗng id_zing_mp3 của bài hát có mã video_id tương ứng đang bị xóa
        sql = "UPDATE songs SET id_zing_mp3 = NULL WHERE id_zing_mp3 = %s"
        cursor.execute(sql, (video_id,))
        conn.commit()
        
        if cursor.rowcount > 0:
            db_msg = f" và làm rỗng CSDL cho ID {video_id}"
            
        cursor.close()
        conn.close()
    except Exception as e:
        print(f"❌ Lỗi MySQL khi xóa cache: {e}")

    print(f"🗑️ Đã dọn dẹp {len(deleted_files)} file liên quan đến ID: {video_id}{db_msg}")
    
    return jsonify({
        "success": True, 
        "message": f"Đã xóa {len(deleted_files)} files{db_msg}.",
        "deleted": deleted_files
    })

@app.route('/api/clear_zing_id_db', methods=['POST'])
def clear_zing_id_db():
    print("\n" + "="*50)
    print(f"🗄️ YÊU CẦU XÓA DỮ LIỆU CỘT id_zing_mp3 TỪ CSDL")
    
    try:
        # 1. Khởi tạo kết nối MySQL
        conn = mysql.connector.connect(
            host='127.0.0.1', 
            user='root', 
            password='123456', 
            database='tuner', 
            port=3306
        )
        cursor = conn.cursor()
        
        # 2. Câu lệnh SQL với tên bảng 'songs'
        table_name = "songs"
        
        # Làm rỗng dữ liệu cột id_zing_mp3
        sql = f"UPDATE {table_name} SET id_zing_mp3 = NULL"
        
        # 3. Thực thi
        cursor.execute(sql)
        conn.commit()
        affected_rows = cursor.rowcount
        
        # 4. Đóng kết nối
        cursor.close()
        conn.close()
        
        print(f"✅ Đã làm rỗng thành công cột id_zing_mp3 cho {affected_rows} dòng!")
        print("="*50 + "\n")
        
        return jsonify({
            "success": True, 
            "message": f"Đã xóa dữ liệu cột id_zing_mp3 thành công cho {affected_rows} bài hát!"
        })
        
    except mysql.connector.Error as err:
        print(f"❌ Lỗi MySQL: {str(err)}")
        print("="*50 + "\n")
        return jsonify({"success": False, "error": f"Lỗi CSDL: {str(err)}"}), 500
    except Exception as e:
        print(f"❌ Lỗi hệ thống: {str(e)}")
        print("="*50 + "\n")
        return jsonify({"success": False, "error": f"Lỗi máy chủ: {str(e)}"}), 500

@app.route('/', methods=['GET'])
def health_check():
    return jsonify({"status": "Máy chủ đang chờ nhận file từ thiết bị Android!"})


# ==============================================================================
# PHẦN 7: [CHỨC NĂNG 5] GIỌNG HÁT + CHẠY CHỮ (ZING MP3 ALL-IN-ONE ĐỒNG BỘ 100%)
# ==============================================================================

@app.route("/api/zing_acapella_sync", methods=["POST"])
def zing_acapella_sync():
    zing_id = request.form.get("zing_id", "").strip()
    title = request.form.get("title", "").strip()
    singer = request.form.get("singer", "").strip()
    first_sentence = request.form.get("first_sentence", "").strip()
    mode = request.form.get("mode", "ZING_ACAPELLA_SYNC").strip()
    requires_lyrics = mode != "ZING_ACAPELLA_NO_LYRICS"
    query = f"{title} {singer}".strip()

    print("\n" + "=" * 50)
    print(f"🔥 [ZING ALL-IN-ONE] '{query}' (ID: {zing_id} | Mode: {mode})")

    if not query and not zing_id:
        return jsonify({"success": False, "error": "Thiếu thông tin tìm kiếm"}), 400

    cleanup_expired_tasks()

    # 1) Xác định ID nhanh bằng dữ liệu Android, cache JSON hoặc search API.
    fast_song_id = zing_id
    cached_json_path = find_karaoke_cache_by_id(fast_song_id)
    if not cached_json_path:
        cached_json_path = find_karaoke_cache_by_title(title, singer)
        # Cache theo tiêu đề đã được đối chiếu câu đầu; ưu tiên đúng ID của cache
        # thay vì tiếp tục dùng một ID cũ có thể trỏ sang bản thu khác.
        if cached_json_path:
            fast_song_id = song_id_from_cache_path(cached_json_path)

    target_song = None
    if not fast_song_id:
        target_song = fetch_zing_api_smart(title, singer)
        if target_song:
            fast_song_id = target_song.get("id", "")

    cached_karaoke = (
        load_karaoke_cache(cached_json_path, first_sentence)
        if requires_lyrics
        else None
    )
    cached_audio = find_zing_audio_cache(fast_song_id)

    if cached_audio and (not requires_lyrics or cached_karaoke is not None):
        print(f"⚡ [CACHE HIT TRỰC TIẾP] ID: {fast_song_id}")
        return jsonify(
            {
                "success": True,
                "id": fast_song_id,
                "karaoke_data": cached_karaoke,
                "acapella_filename": os.path.basename(cached_audio),
            }
        )

    # 2) Chống tạo hai job giống nhau khi Android gửi lặp yêu cầu.
    request_key = task_key_for_zing(fast_song_id, title, singer, mode)
    with task_lock:
        existing_task_id = inflight_tasks.get(request_key)
        if (
            existing_task_id
            and active_tasks.get(existing_task_id, {}).get("status") == "processing"
        ):
            return jsonify(
                {
                    "success": True,
                    "task_id": existing_task_id,
                    "status": "processing",
                    "deduplicated": True,
                }
            )

        task_id = str(uuid.uuid4())
        inflight_tasks[request_key] = task_id
        set_task(
            task_id,
            {
                "status": "processing",
                "progress": 1,
                "message": "Đang chuẩn bị xử lý...",
            },
        )

    def background_process():
        temp_audio_path = None
        output_files = []
        try:
            song_id = fast_song_id
            karaoke_data = cached_karaoke
            audio_cache = cached_audio
            audio_url = None

            if target_song:
                result_url = f"https://zingmp3.vn{target_song.get('link', '')}"
            elif song_id:
                result_url = f"https://zingmp3.vn/bai-hat/Zing/{song_id}.html"
            else:
                result_url = (
                    "https://zingmp3.vn/tim-kiem/bai-hat?q="
                    + urllib.parse.quote(title or query)
                )

            need_audio_search = audio_cache is None
            need_lyrics_search = requires_lyrics and karaoke_data is None

            if need_audio_search or need_lyrics_search:
                update_task(
                    task_id,
                    status="processing",
                    progress=8,
                    message="Đang lấy nguồn nhạc và lời bài hát...",
                )

                strict_id = song_id if (audio_cache or karaoke_data) else ""
                scraped_lyrics, scraped_audio_url, selected_id, _ = scrape_zing_sources(
                    result_url=result_url,
                    query=query,
                    title=title,
                    initial_song_id=song_id,
                    expected_sentence=first_sentence,
                    cached_karaoke=karaoke_data,
                    need_lyrics=need_lyrics_search,
                    need_audio=need_audio_search,
                    strict_song_id=strict_id,
                )

                if selected_id:
                    song_id = selected_id
                if need_lyrics_search:
                    karaoke_data = scraped_lyrics
                if need_audio_search:
                    audio_url = scraped_audio_url

            if requires_lyrics and karaoke_data is None:
                raise RuntimeError(
                    "Không tìm thấy lời bài hát khớp. Đã hủy để tránh xử lý nhầm bài."
                )

            # Sau khi biết ID cuối, kiểm tra lại cache vì một job khác có thể vừa hoàn tất.
            audio_cache = find_zing_audio_cache(song_id) or audio_cache

            if audio_cache is None:
                if not audio_url:
                    raise RuntimeError("Không lấy được URL âm thanh gốc từ Zing.")

                if not song_id:
                    digest = hashlib.sha1(query.encode("utf-8")).hexdigest()[:12].upper()
                    song_id = f"LOCAL{digest}"

                update_task(
                    task_id,
                    status="processing",
                    progress=18,
                    message="Đang tải âm thanh về máy chủ...",
                )
                temp_audio_path = os.path.join(
                    "ZING_AUDIO",
                    f"temp_zing_{song_id}_{uuid.uuid4().hex}.mp3",
                )
                download_to_file(audio_url, temp_audio_path)

                update_task(
                    task_id,
                    status="processing",
                    progress=25,
                    message="Chuẩn bị mô hình MDX-Net...",
                )
                print("🧠 MDX-Net đang tách giọng ca sĩ...")
                progress_end = 100 if not requires_lyrics else 92
                output_files = separate_with_mdx(
                    temp_audio_path,
                    single_stem="Vocals",
                    task_id=task_id,
                    progress_start=25,
                    progress_end=progress_end,
                )

                vocals_path = find_stem_path(output_files, "Vocals")
                if not vocals_path or not os.path.exists(vocals_path):
                    raise RuntimeError("MDX không trả về file giọng ca sĩ.")

                final_audio_name = f"zing_acapella_{song_id}.ogg"
                final_audio_path = os.path.join("ZING_AUDIO", final_audio_name)
                update_task(
                    task_id,
                    status="processing",
                    progress=94 if requires_lyrics else 98,
                    message="Đang mã hóa OGG và lưu cache...",
                )
                convert_audio_to_ogg(vocals_path, final_audio_path)
                audio_cache = final_audio_path
            else:
                final_audio_name = os.path.basename(audio_cache)
                update_task(
                    task_id,
                    status="processing",
                    progress=90,
                    message="Đã dùng lại audio cache, đang hoàn tất dữ liệu...",
                )

            if karaoke_data is not None:
                save_karaoke_cache(song_id, title, singer, karaoke_data)

            set_task(
                task_id,
                {
                    "status": "completed",
                    "progress": 100,
                    "message": "Hoàn tất xử lý.",
                    "data": {
                        "success": True,
                        "id": song_id,
                        "karaoke_data": karaoke_data,
                        "acapella_filename": final_audio_name,
                    },
                },
            )
        except Exception as error:
            print(f"❌ Zing all-in-one lỗi: {error}")
            set_task(
                task_id,
                {
                    "status": "error",
                    "message": str(error),
                },
            )
        finally:
            safe_remove(temp_audio_path)
            cleanup_separator_outputs(output_files)
            with task_lock:
                if inflight_tasks.get(request_key) == task_id:
                    inflight_tasks.pop(request_key, None)

    thread = threading.Thread(
        target=background_process,
        name=f"zing-task-{task_id[:8]}",
        daemon=True,
    )
    thread.start()

    return jsonify(
        {
            "success": True,
            "task_id": task_id,
            "status": "processing",
        }
    )


# API ĐỂ ANDROID LOAD NHẠC TRỰC TIẾP TỪ KHO
@app.route("/api/stream_cache/<path:filename>", methods=["GET"])
def stream_cache(filename):
    safe_name = os.path.basename(filename)
    if safe_name != filename:
        return "Invalid filename", 400

    if "zing" in safe_name.lower():
        file_path = os.path.join("ZING_AUDIO", safe_name)
    else:
        file_path = os.path.join("YOUTUBE_CACHE", safe_name)

    if not os.path.exists(file_path):
        alternate = os.path.join("ai_cache", safe_name)
        if os.path.exists(alternate):
            file_path = alternate

    if not os.path.exists(file_path):
        return "File not found", 404

    return send_file(
        file_path,
        mimetype=audio_mimetype(file_path),
        conditional=True,
        max_age=3600,
    )


# ==============================================================================
# KHỞI ĐỘNG MÁY CHỦ FLASK
# ==============================================================================
if __name__ == "__main__":
    print("🚀 MÁY CHỦ API ĐÃ KHỞI ĐỘNG TẠI CỔNG 5000!")
    try:
        from waitress import serve

        serve(
            app,
            host="0.0.0.0",
            port=5000,
            threads=_env_int("WAITRESS_THREADS", 8, 2, 32),
            channel_timeout=300,
        )
    except ImportError:
        print("⚠️ Chưa cài waitress; tạm chạy Flask threaded server.")
        app.run(host="0.0.0.0", port=5000, debug=False, threaded=True)

