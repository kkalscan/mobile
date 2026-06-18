#!/usr/bin/env python3
"""Frame real device screenshots for RuStore (phone 1080x1920 + tablet 1920x1200)."""

from __future__ import annotations

import importlib.util
from collections.abc import Callable
from pathlib import Path

from PIL import Image, ImageDraw

from generate_assets import (
    CREAM,
    INK,
    INK_MUTED,
    MANGO,
    OUTLINE,
    WHITE,
    draw_text_centered,
    load_font,
    rounded_rect,
    ui_scan,
)

ROOT = Path(__file__).resolve().parent
SCRIPTS = ROOT.parent.parent / "scripts"
RAW = ROOT / "raw"
TABLET = ROOT / "tablet"
UPLOAD = ROOT / "upload"
OUT = ROOT

PHONE_W, PHONE_H = 1080, 1920
TABLET_W, TABLET_H = 1920, 1200
STATUS_BAR_H = 120

OverlayFn = Callable[[Image.Image], None]

SHOTS: list[tuple[str, str, str, OverlayFn | None, bool]] = [
    ("01-diary", "01-diary.png", "Дневник питания за день", None, False),
    ("02-result", "02-result.png", "Калории по фото за секунды", None, False),
    ("03-scan", "03-scan-app.png", "Сфотографируй еду", None, False),
    ("04-free", "05-profile.png", "3 бесплатных скана каждый день", None, False),
    ("05-limit", "04-journal.png", "Статистика и дневник за неделю", None, False),
]


def load_store_icon() -> Path:
    spec = importlib.util.spec_from_file_location("generate_icons", SCRIPTS / "generate-icons.py")
    if spec is None or spec.loader is None:
        raise RuntimeError("Cannot load generate-icons.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    path = OUT / "icon-512.png"
    module.draw_icon(512).save(path, "PNG", optimize=True)
    return path


def fit_rustore_screenshot(img: Image.Image) -> Image.Image:
    """1080x2400 device capture → 1080x1920, keep bottom navigation visible."""
    img = img.convert("RGB")
    w, h = img.size
    top = min(STATUS_BAR_H, max(0, h - PHONE_H))
    img = img.crop((0, top, w, h))
    w, h = img.size

    if w != PHONE_W:
        scale = PHONE_W / w
        img = img.resize((PHONE_W, round(h * scale)), Image.Resampling.LANCZOS)
        w, h = img.size

    if h > PHONE_H:
        img = img.crop((0, h - PHONE_H, w, h))
    elif h < PHONE_H:
        canvas = Image.new("RGB", (PHONE_W, PHONE_H), CREAM)
        canvas.paste(img, (0, PHONE_H - h))
        img = canvas
    return img


def draw_bottom_nav(draw: ImageDraw.ImageDraw, selected: str) -> None:
    y0 = PHONE_H - 168
    rounded_rect(draw, (0, y0, PHONE_W, PHONE_H), 0, WHITE)
    tabs = [("Сегодня", "Today"), ("Дневник", "Journal"), ("Профиль", "Profile")]
    slot_w = PHONE_W // 3
    label_font = load_font(24)
    for idx, (label, key) in enumerate(tabs):
        cx = idx * slot_w + slot_w // 2
        color = MANGO if key == selected else INK_MUTED
        dot_r = 10
        draw.ellipse((cx - dot_r, y0 + 28, cx + dot_r, y0 + 48), fill=color)
        bbox = draw.textbbox((0, 0), label, font=label_font)
        tw = bbox[2] - bbox[0]
        draw.text((cx - tw // 2, y0 + 58), label, fill=color, font=label_font)
    fab_r = 44
    fab_cx, fab_cy = PHONE_W - 72, y0 - 12
    draw.ellipse((fab_cx - fab_r, fab_cy - fab_r, fab_cx + fab_r, fab_cy + fab_r), fill=MANGO)
    plus = load_font(40, bold=True)
    draw.text((fab_cx, fab_cy - 2), "+", fill=WHITE, font=plus, anchor="mm")


def render_in_app_scan_screenshot() -> Image.Image:
    """In-app scan screen (not the system camera)."""
    canvas = Image.new("RGB", (PHONE_W, PHONE_H), CREAM)
    ui = ui_scan()
    ui = ui.resize((PHONE_W, PHONE_H - 168), Image.Resampling.LANCZOS)
    canvas.paste(ui, (0, 0))
    draw = ImageDraw.Draw(canvas)
    draw_bottom_nav(draw, selected="Today")
    return canvas


def ensure_scan_raw() -> Path:
    path = RAW / "03-scan-app.png"
    if not path.exists():
        render_in_app_scan_screenshot().save(path, "PNG", optimize=True)
    return path


def draw_tablet_marketing(title: str, shot: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", (TABLET_W, TABLET_H), CREAM)
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(48, bold=True)
    draw_text_centered(draw, title, 40, title_font, INK, TABLET_W)

    tw, th = 640, 1040
    tx = (TABLET_W - tw) // 2
    ty = 108
    rounded_rect(draw, (tx - 6, ty - 6, tx + tw + 6, ty + th + 6), 32, OUTLINE)
    rounded_rect(draw, (tx, ty, tx + tw, ty + th), 26, WHITE)
    inner = fit_rustore_screenshot(shot)
    inner = inner.resize((tw, th), Image.Resampling.LANCZOS)
    canvas.paste(inner, (tx, ty))
    return canvas


def main() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    TABLET.mkdir(parents=True, exist_ok=True)
    UPLOAD.mkdir(parents=True, exist_ok=True)
    icon = load_store_icon()
    print(icon)

    for out_name, raw_name, title, overlay_fn, _with_charts in SHOTS:
        raw_path = RAW / raw_name
        if raw_name == "03-scan-app.png":
            raw_path = ensure_scan_raw()

        if not raw_path.exists():
            raise FileNotFoundError(raw_path)

        if raw_name == "03-scan-app.png":
            upload_img = render_in_app_scan_screenshot()
        else:
            upload_img = fit_rustore_screenshot(Image.open(raw_path))

        upload_path = UPLOAD / f"screenshot-{out_name}.png"
        upload_img.save(upload_path, "PNG", optimize=True)
        print(upload_path)

        phone_path = OUT / f"screenshot-{out_name}.png"
        upload_img.save(phone_path, "PNG", optimize=True)
        print(phone_path)

        tablet_path = TABLET / f"screenshot-{out_name}-landscape.png"
        draw_tablet_marketing(title, upload_img).save(tablet_path, "PNG", optimize=True)
        print(tablet_path)


if __name__ == "__main__":
    main()
