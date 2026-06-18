#!/usr/bin/env python3
"""Generate KkalScan launcher PNGs and favicons (Citrus Scan icon)."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
WASM_RES = ROOT / "composeApp/src/wasmJsMain/resources"
ANDROID_RES = ROOT / "composeApp/src/androidMain/res"
STORE_ICON = ROOT / "store" / "rustore" / "icon-512.png"

MANGO = (255, 122, 47)
MANGO_PINK = (255, 77, 141)
WHITE = (255, 255, 255)


def load_font(size: int, bold: bool = True) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial Unicode.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def gradient_scan_hero(size: int) -> Image.Image:
    """scanHero: #FF7A2F → #FF4D8D at 135° (top-left → bottom-right)."""
    img = Image.new("RGB", (size, size))
    draw = ImageDraw.Draw(img)
    span = max(size - 1, 1)
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * span)
            r = int(MANGO[0] + (MANGO_PINK[0] - MANGO[0]) * t)
            g = int(MANGO[1] + (MANGO_PINK[1] - MANGO[1]) * t)
            b = int(MANGO[2] + (MANGO_PINK[2] - MANGO[2]) * t)
            draw.point((x, y), fill=(r, g, b))
    return img


def add_highlight(img: Image.Image) -> None:
    """Soft top-left glow for depth."""
    size = img.size[0]
    overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    cx, cy = int(size * 0.22), int(size * 0.18)
    radius = int(size * 0.42)
    for r in range(radius, 0, -2):
        alpha = int(28 * (1 - r / radius) ** 1.6)
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(255, 255, 255, alpha))
    base = img.convert("RGBA")
    img.paste(Image.alpha_composite(base, overlay).convert("RGB"))


def draw_round_cap_line(
    draw: ImageDraw.ImageDraw,
    start: tuple[float, float],
    end: tuple[float, float],
    color: tuple[int, int, int],
    width: int,
) -> None:
    draw.line([start, end], fill=color, width=width)
    cap_r = width // 2
    for point in (start, end):
        x, y = point
        draw.ellipse((x - cap_r, y - cap_r, x + cap_r, y + cap_r), fill=color)


def draw_viewfinder(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    color: tuple[int, int, int],
    width: int,
    arm_ratio: float = 0.24,
) -> None:
    x0, y0, x1, y1 = box
    arm = int(min(x1 - x0, y1 - y0) * arm_ratio)
    corners = [
        ((x0, y0), (x0 + arm, y0), (x0, y0 + arm)),
        ((x1, y0), (x1 - arm, y0), (x1, y0 + arm)),
        ((x0, y1), (x0 + arm, y1), (x0, y1 - arm)),
        ((x1, y1), (x1 - arm, y1), (x1, y1 - arm)),
    ]
    for center, h_end, v_end in corners:
        draw_round_cap_line(draw, center, h_end, color, width)
        draw_round_cap_line(draw, center, v_end, color, width)


def draw_symbol(draw: ImageDraw.ImageDraw, size: int) -> None:
    inset = int(size * 0.2)
    box = (inset, inset, size - inset, size - inset)
    stroke = max(3, int(size * 0.042))
    draw_viewfinder(draw, box, WHITE, stroke)

    font = load_font(int(size * 0.19))
    label = "ккал"
    bbox = draw.textbbox((0, 0), label, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = (size - tw) // 2
    ty = (size - th) // 2 - int(size * 0.01)
    draw.text((tx, ty), label, fill=WHITE, font=font)


def draw_icon(size: int) -> Image.Image:
    img = gradient_scan_hero(size)
    add_highlight(img)
    draw = ImageDraw.Draw(img)
    draw_symbol(draw, size)
    return img


def draw_foreground_layer(size: int) -> Image.Image:
    """Transparent layer for Android adaptive icon foreground."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_symbol(draw, size)
    return img


def save_png(path: Path, size: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    draw_icon(size).save(path, format="PNG")


def save_svg(path: Path) -> None:
    path.write_text(
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" role="img" aria-label="KkalScan">
  <defs>
    <linearGradient id="scanHero" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FF7A2F"/>
      <stop offset="100%" stop-color="#FF4D8D"/>
    </linearGradient>
  </defs>
  <rect width="32" height="32" rx="6" fill="url(#scanHero)"/>
  <g fill="none" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round">
    <path d="M8.5 8.5h4.2"/><path d="M8.5 8.5v4.2"/>
    <path d="M23.5 8.5h-4.2"/><path d="M23.5 8.5v4.2"/>
    <path d="M8.5 23.5h4.2"/><path d="M8.5 23.5v-4.2"/>
    <path d="M23.5 23.5h-4.2"/><path d="M23.5 23.5v-4.2"/>
  </g>
  <text x="16" y="18.2" text-anchor="middle" fill="#FFFFFF" font-family="system-ui,-apple-system,sans-serif" font-size="5.8" font-weight="700">ккал</text>
</svg>
""",
        encoding="utf-8",
    )


def main() -> None:
    save_svg(WASM_RES / "favicon.svg")
    for name, size in (
        ("favicon-32.png", 32),
        ("apple-touch-icon.png", 180),
        ("icon-192.png", 192),
    ):
        save_png(WASM_RES / name, size)

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in densities.items():
        out = ANDROID_RES / folder
        save_png(out / "ic_launcher.png", size)
        save_png(out / "ic_launcher_round.png", size)

    foreground_dir = ANDROID_RES / "drawable-nodpi"
    foreground_dir.mkdir(parents=True, exist_ok=True)
    draw_foreground_layer(432).save(foreground_dir / "ic_launcher_foreground_image.png", format="PNG")

    save_png(STORE_ICON, 512)
    preview = ROOT.parent.parent / "fixaverse" / "assets" / "kkalscan-icon-512.png"
    if preview.parent.exists():
        save_png(preview, 512)

    print("Generated KkalScan icons")
    print(STORE_ICON)


if __name__ == "__main__":
    main()
