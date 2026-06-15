#!/usr/bin/env python3
"""Generate KkalScan launcher PNGs and favicons from brand colors."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
WASM_RES = ROOT / "composeApp/src/wasmJsMain/resources"
ANDROID_RES = ROOT / "composeApp/src/androidMain/res"

ORANGE = (255, 122, 47, 255)
MINT_TOP = (232, 244, 240, 255)
MINT_BOTTOM = (244, 247, 251, 255)
WHITE = (255, 255, 255, 255)


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    radius = max(4, size // 6)

    draw.rounded_rectangle((0, 0, size, size), radius=radius, fill=MINT_TOP)
    draw.rectangle((0, int(size * 0.62), size, size), fill=MINT_BOTTOM)

    inset = size // 5
    stroke = max(2, size // 28)
    corner = size // 7
    for x0, y0, x1, y1 in (
        (inset, inset, inset + corner, inset),
        (inset, inset, inset, inset + corner),
        (size - inset - corner, inset, size - inset, inset),
        (size - inset, inset, size - inset, inset + corner),
        (inset, size - inset, inset + corner, size - inset),
        (inset, size - inset - corner, inset, size - inset),
        (size - inset - corner, size - inset, size - inset, size - inset),
        (size - inset, size - inset - corner, size - inset, size - inset),
    ):
        draw.line((x0, y0, x1, y1), fill=ORANGE, width=stroke)

    badge_r = int(size * 0.22)
    cx = cy = size // 2
    draw.ellipse(
        (cx - badge_r, cy - badge_r, cx + badge_r, cy + badge_r),
        fill=ORANGE,
    )

    font = load_font(int(size * 0.34))
    draw.text((cx, cy - max(1, size // 40)), "K", fill=WHITE, font=font, anchor="mm")
    return img


def save_png(path: Path, size: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    draw_icon(size).save(path, format="PNG")


def save_svg(path: Path) -> None:
    path.write_text(
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" role="img" aria-label="KkalScan">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#E8F4F0"/>
      <stop offset="100%" stop-color="#F4F7FB"/>
    </linearGradient>
  </defs>
  <rect width="32" height="32" rx="6" fill="url(#bg)"/>
  <path d="M7 9h3M7 9v3" stroke="#FF7A2F" stroke-width="1.4" stroke-linecap="round"/>
  <path d="M25 9h-3M25 9v3" stroke="#FF7A2F" stroke-width="1.4" stroke-linecap="round"/>
  <path d="M7 23h3M7 23v-3" stroke="#FF7A2F" stroke-width="1.4" stroke-linecap="round"/>
  <path d="M25 23h-3M25 23v-3" stroke="#FF7A2F" stroke-width="1.4" stroke-linecap="round"/>
  <circle cx="16" cy="16" r="7" fill="#FF7A2F"/>
  <text x="16" y="19.5" text-anchor="middle" fill="#FFFFFF" font-family="system-ui,sans-serif" font-size="11" font-weight="700">K</text>
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

    print("Generated KkalScan icons")


if __name__ == "__main__":
    main()
