"""Build-time only: download the pinned hand model, verify it, bundle for offline use."""
from pathlib import Path
import hashlib
import urllib.request

URL = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
SHA256 = "fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1"
path = Path(__file__).resolve().parents[1] / "app/src/main/assets/hand_landmarker.task"
if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != SHA256:
    path.parent.mkdir(parents=True, exist_ok=True)
    data = urllib.request.urlopen(URL, timeout=60).read()
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit("Model checksum mismatch; refusing to package it.")
    path.write_bytes(data)
print("Verified hand model:", path)
