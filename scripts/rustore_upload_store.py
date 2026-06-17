#!/usr/bin/env python3
"""Upload RuStore store assets (icon, screenshots, AAB) via Public API."""

from __future__ import annotations

import argparse
import base64
import datetime
import json
import os
import random
import sys
import urllib.error
import urllib.request
from pathlib import Path

from Crypto.Hash import SHA512
from Crypto.PublicKey import RSA
from Crypto.Signature import pkcs1_15

API_BASE = os.environ.get("RUSTORE_API_BASE", "https://public-api.rustore.ru").rstrip("/")
PACKAGE = os.environ.get("RUSTORE_PACKAGE_NAME", "ru.kkalscan.app")
STORE_DIR = Path(__file__).resolve().parents[1] / "store" / "rustore"


def env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        print(f"Missing env: {name}", file=sys.stderr)
        sys.exit(1)
    return value


def fetch_token(key_id: str, private_key_b64: str) -> str:
    private_key = RSA.import_key(base64.b64decode(private_key_b64))
    now = datetime.datetime.now(datetime.timezone.utc)
    ms = f"{now.microsecond // 1000:03d}"
    timestamp = now.strftime("%Y-%m-%dT%H:%M:%S.") + ms + "+00:00"
    message = (key_id + timestamp).encode("utf-8")
    signature = base64.b64encode(pkcs1_15.new(private_key).sign(SHA512.new(message))).decode("utf-8")
    payload = {"keyId": key_id, "timestamp": timestamp, "signature": signature}
    req = urllib.request.Request(
        f"{API_BASE}/public/auth/",
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    if data.get("code") != "OK":
        raise RuntimeError(f"Auth failed: {data}")
    return data["body"]["jwe"]


def api_json(method: str, path: str, token: str, body: dict | None = None) -> dict:
    req = urllib.request.Request(
        f"{API_BASE}{path}",
        data=None if body is None else json.dumps(body).encode("utf-8"),
        method=method,
        headers={
            "Public-Token": token,
            "Accept": "application/json",
            **({"Content-Type": "application/json"} if body is not None else {}),
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"{method} {path} -> {e.code}: {e.read().decode()}") from e


def api_multipart(path: str, token: str, file_path: Path) -> dict:
    boundary = f"----KkalScanStore{random.randint(10**8, 10**9 - 1)}"
    file_bytes = file_path.read_bytes()
    filename = file_path.name
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
        f"Content-Type: application/octet-stream\r\n\r\n"
    ).encode("utf-8") + file_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")
    req = urllib.request.Request(
        f"{API_BASE}{path}",
        data=body,
        method="POST",
        headers={
            "Public-Token": token,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"POST {path} -> {e.code}: {e.read().decode()}") from e


def find_draft_version_id(token: str) -> int | None:
    data = api_json("GET", f"/public/v1/application/{PACKAGE}/version?versionStatuses=DRAFT&page=0&size=5", token)
    content = data.get("body", {}).get("content") or []
    if content:
        vid = content[0].get("versionId") or content[0].get("id")
        if vid is not None:
            return int(vid)
    return None


def ensure_draft(token: str, whats_new: str) -> int:
    existing = find_draft_version_id(token)
    if existing is not None:
        print(f"Reuse draft versionId={existing}")
        return existing
    data = api_json(
        "POST",
        f"/public/v1/application/{PACKAGE}/version",
        token,
        {"appType": "MAIN", "publishType": "INSTANTLY", "whatsNew": whats_new[:5000]},
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"Create draft failed: {data}")
    version_id = int(data["body"])
    print(f"Created draft versionId={version_id}")
    return version_id


def upload_icon(token: str, version_id: int, icon_path: Path) -> None:
    data = api_multipart(
        f"/public/v1/application/{PACKAGE}/version/{version_id}/image/icon",
        token,
        icon_path,
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"Icon upload failed: {data}")
    print(f"Icon uploaded: {icon_path.name}")


def upload_screenshot(token: str, version_id: int, ordinal: int, shot_path: Path) -> None:
    data = api_multipart(
        f"/public/v1/application/{PACKAGE}/version/{version_id}/image/screenshot/PORTRAIT/{ordinal}",
        token,
        shot_path,
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"Screenshot {ordinal} failed: {data}")
    print(f"Screenshot {ordinal} uploaded: {shot_path.name}")


def upload_aab(token: str, version_id: int, aab_path: Path) -> None:
    data = api_multipart(
        f"/public/v1/application/{PACKAGE}/version/{version_id}/aab",
        token,
        aab_path,
    )
    if data.get("code") != "OK":
        raise RuntimeError(f"AAB upload failed: {data}")
    print(f"AAB uploaded ({aab_path.stat().st_size // 1024} KB)")


def send_moderation(token: str, version_id: int) -> None:
    data = api_json("POST", f"/public/v1/application/{PACKAGE}/version/{version_id}/commit", token)
    if data.get("code") != "OK":
        raise RuntimeError(f"Moderation commit failed: {data}")
    print("Sent to moderation")


def main() -> None:
    parser = argparse.ArgumentParser(description="Upload KkalScan RuStore store assets")
    parser.add_argument("--aab", type=Path, help="Path to signed AAB")
    parser.add_argument("--skip-moderation", action="store_true")
    parser.add_argument(
        "--whats-new",
        default="Первая публикация KkalScan: скан еды по фото, дневник питания, 3 бесплатных скана в день.",
    )
    args = parser.parse_args()

    key_id = env("RUSTORE_KEY_ID")
    private_key = env("RUSTORE_PRIVATE_KEY")
    token = fetch_token(key_id, private_key)
    print("Auth OK")

    version_id = ensure_draft(token, args.whats_new)

    icon = STORE_DIR / "icon-512.png"
    if not icon.is_file():
        print(f"Missing {icon}. Run store/rustore/generate_assets.py first.", file=sys.stderr)
        sys.exit(1)
    upload_icon(token, version_id, icon)

    shots = sorted(STORE_DIR.glob("screenshot-*.png"))
    if not shots:
        print("No screenshots found", file=sys.stderr)
        sys.exit(1)
    for ordinal, shot in enumerate(shots[:10]):
        upload_screenshot(token, version_id, ordinal, shot)

    aab = args.aab
    if aab is None:
        default_aab = Path(__file__).resolve().parents[1] / "composeApp/build/outputs/bundle/release/composeApp-release.aab"
        aab = default_aab if default_aab.is_file() else None
    if aab and aab.is_file():
        upload_aab(token, version_id, aab)
    else:
        print("AAB not found — skip binary upload")

    if not args.skip_moderation and aab and aab.is_file():
        send_moderation(token, version_id)
    else:
        print(f"Draft ready: versionId={version_id}. Paste texts from store/rustore/copy.md in Console, then commit.")

    print(f"Done. versionId={version_id}")


if __name__ == "__main__":
    main()
