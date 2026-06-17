#!/usr/bin/env python3
"""Frame real device screenshots for RuStore (phone 1080x1920 + tablet 1920x1200)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from generate_assets import CREAM, INK, OUTLINE, WHITE, generate_icon, load_font, rounded_rect

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "raw"
TABLET = ROOT / "tablet"
OUT = ROOT

PHONE_W, PHONE_H = 1080, 1920
TABLET_W, TABLET_H = 1920, 1200
FRAME_X, FRAME_Y = 120, 180
FRAME_W = 840
FRAME_H = round(FRAME_W * PHONE_H / PHONE_W)  # keep 9:16, no horizontal squash


def resize_cover(img: Image.Image, target_w: int, target_h: int) -> Image.Image:
    src_w, src_h = img.size
    scale = max(target_w / src_w, target_h / src_h)
    resized_w = round(src_w * scale)
    resized_h = round(src_h * scale)
    resized = img.resize((resized_w, resized_h), Image.Resampling.LANCZOS)
    left = (resized_w - target_w) // 2
    top = (resized_h - target_h) // 2
    return resized.crop((left, top, left + target_w, top + target_h))


def crop_phone_shot(path: Path) -> Image.Image:
    """Normalize device capture to 9:16 without stretching."""
    img = Image.open(path).convert("RGB")
    return resize_cover(img, PHONE_W, PHONE_H)


def draw_phone_marketing(title: str, shot: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", (PHONE_W, PHONE_H), CREAM)
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(52, bold=True)
    bbox = draw.textbbox((0, 0), title, font=title_font)
    tw = bbox[2] - bbox[0]
    draw.text(((PHONE_W - tw) // 2, 72), title, fill=INK, font=title_font)

    rounded_rect(draw, (FRAME_X - 8, FRAME_Y - 8, FRAME_X + FRAME_W + 8, FRAME_Y + FRAME_H + 8), 48, OUTLINE)
    rounded_rect(draw, (FRAME_X, FRAME_Y, FRAME_X + FRAME_W, FRAME_Y + FRAME_H), 40, WHITE)

    inner = resize_cover(shot, FRAME_W, FRAME_H)
    canvas.paste(inner, (FRAME_X, FRAME_Y))
    return canvas


def draw_tablet_marketing(title: str, shot: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", (TABLET_W, TABLET_H), CREAM)
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(56, bold=True)
    bbox = draw.textbbox((0, 0), title, font=title_font)
    tw = bbox[2] - bbox[0]
    draw.text(((TABLET_W - tw) // 2, 56), title, fill=INK, font=title_font)

    tw, th = 720, 1280
    tx = (TABLET_W - tw) // 2
    ty = (TABLET_H - th) // 2 + 24
    rounded_rect(draw, (tx - 8, ty - 8, tx + tw + 8, ty + th + 8), 40, OUTLINE)
    rounded_rect(draw, (tx, ty, tx + tw, ty + th), 32, WHITE)
    inner = resize_cover(shot, tw, th)
    canvas.paste(inner, (tx, ty))
    return canvas


SHOTS = [
    ("01-diary", "01-diary.png", "Дневник питания за день"),
    ("02-result", "02-result.png", "Калории по фото за секунды"),
    ("03-scan", "03-camera.png", "Сфотографируй еду"),
    ("04-free", "05-profile.png", "3 бесплатных скана каждый день"),
    ("05-limit", "04-journal.png", "Статистика и дневник за неделю"),
]


def main() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    TABLET.mkdir(parents=True, exist_ok=True)
    icon = generate_icon()
    print(icon)
    for out_name, raw_name, title in SHOTS:
        raw_path = RAW / raw_name
        if not raw_path.exists():
            raise FileNotFoundError(raw_path)
        shot = crop_phone_shot(raw_path)
        phone = draw_phone_marketing(title, shot)
        phone_path = OUT / f"screenshot-{out_name}.png"
        phone.save(phone_path, "PNG", optimize=True)
        tablet = draw_tablet_marketing(title, shot)
        tablet_path = TABLET / f"screenshot-{out_name}-landscape.png"
        tablet.save(tablet_path, "PNG", optimize=True)
        print(phone_path)
        print(tablet_path)


if __name__ == "__main__":
    main()
