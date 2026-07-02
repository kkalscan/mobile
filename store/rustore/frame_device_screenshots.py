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
    LEMON,
    MANGO,
    MANGO_LIGHT,
    MINT,
    OUTLINE,
    PRO,
    WHITE,
    draw_text_centered,
    load_font,
    rounded_rect,
)

ROOT = Path(__file__).resolve().parent
SCRIPTS = ROOT.parent.parent / "scripts"
RAW = ROOT / "raw"
PUBLISHED = ROOT / "published"
TABLET = ROOT / "tablet"
UPLOAD = ROOT / "upload"
OUT = ROOT

PHONE_W, PHONE_H = 1080, 1920
TABLET_W, TABLET_H = 1920, 1200
STATUS_BAR_H = 120

OverlayFn = Callable[[Image.Image], None]

ShotSpec = tuple[str, str, str, str, tuple[str, ...]]

SHOTS: list[ShotSpec] = [
    (
        "01-diary",
        "screenshot-01-diary.png",
        "Дневник с клетчаткой",
        "Ккал, БЖУ и клетчатка за день — плюс поиск продуктов",
        ("клетчатка", "БЖУ", "поиск"),
    ),
    (
        "02-result",
        "screenshot-02-result.png",
        "Калории по фото за секунды",
        "Проверь порцию и добавь результат в дневник",
        ("по фото", "клетчатка", "порции"),
    ),
    (
        "03-scan",
        "screenshot-03-scan.png",
        "Поиск продуктов",
        "Добавьте блюдо в день без фото — из каталога",
        ("каталог", "быстро", "дневник"),
    ),
    (
        "04-free",
        "screenshot-04-free.png",
        "3 бесплатных скана каждый день",
        "Можно попробовать приложение без регистрации",
        ("без регистрации", "каждый день"),
    ),
    (
        "05-limit",
        "screenshot-05-limit.png",
        "Клетчатка за неделю",
        "Графики калорий, БЖУ и клетчатки в дневнике",
        ("неделя", "клетчатка", "прогресс"),
    ),
]

LEGACY_RAW_BACKUPS = {
    "01-diary.png": OUT / "screenshot-01-diary.png",
    "02-result.png": OUT / "screenshot-02-result.png",
    "04-journal.png": OUT / "screenshot-05-limit.png",
    "05-profile.png": OUT / "screenshot-04-free.png",
}


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


def draw_cta_button(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    fill: tuple[int, int, int],
    text_fill: tuple[int, int, int],
    radius: int = 28,
) -> None:
    x0, y0, x1, y1 = box
    rounded_rect(draw, (x0 + 2, y0 + 5, x1 + 2, y1 + 5), radius, OUTLINE)
    rounded_rect(draw, box, radius, fill)
    font = load_font(30, bold=True)
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text((x0 + (x1 - x0 - tw) // 2, y0 + (y1 - y0 - th) // 2 - 3), text, fill=text_fill, font=font)


def ui_scan_store() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    draw = ImageDraw.Draw(ui)
    draw.text((40, 48), "Скан еды", fill=INK, font=load_font(40, bold=True))
    draw.text((40, 110), "AI распознает блюда и считает калории", fill=INK_MUTED, font=load_font(26))
    rounded_rect(draw, (40, 172, 320, 232), 20, (255, 248, 214))
    draw.text((70, 190), "Осталось 3 бесплатных скана", fill=INK, font=load_font(22, bold=True))
    rounded_rect(draw, (40, 300, 800, 920), 32, MANGO_LIGHT)
    draw.line((125, 380, 225, 380), fill=MINT, width=12)
    draw.line((125, 380, 125, 480), fill=MINT, width=12)
    draw.line((715, 380, 615, 380), fill=MINT, width=12)
    draw.line((715, 380, 715, 480), fill=MINT, width=12)
    draw.line((125, 840, 225, 840), fill=MINT, width=12)
    draw.line((125, 840, 125, 740), fill=MINT, width=12)
    draw.line((715, 840, 615, 840), fill=MINT, width=12)
    draw.line((715, 840, 715, 740), fill=MINT, width=12)
    draw.ellipse((322, 510, 518, 706), outline=WHITE, width=8)
    draw_cta_button(draw, (90, 990, 750, 1082), "Выбрать фото", MANGO, WHITE)
    return ui


def ui_free_store() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    draw = ImageDraw.Draw(ui)
    draw.text((40, 48), "Скан еды", fill=INK, font=load_font(40, bold=True))
    rounded_rect(draw, (120, 420, 720, 560), 30, (255, 248, 214))
    draw.text((170, 470), "3 бесплатных скана каждый день", fill=INK, font=load_font(34, bold=True))
    draw.text((120, 620), "Хватит на завтрак, обед и ужин", fill=INK_MUTED, font=load_font(28))
    rounded_rect(draw, (40, 760, 800, 980), 28, WHITE)
    draw.text((70, 810), "Без регистрации", fill=INK, font=load_font(36, bold=True))
    draw.text((70, 870), "Открыли приложение — сразу сканируете", fill=INK_MUTED, font=load_font(26))
    return ui


def render_in_app_scan_screenshot() -> Image.Image:
    """In-app scan screen (not the system camera)."""
    canvas = Image.new("RGB", (PHONE_W, PHONE_H), CREAM)
    ui = ui_scan_store()
    ui = ui.resize((PHONE_W, PHONE_H - 168), Image.Resampling.LANCZOS)
    canvas.paste(ui, (0, 0))
    draw = ImageDraw.Draw(canvas)
    draw_bottom_nav(draw, selected="Today")
    return canvas


def render_generated_raw(ui: Image.Image, selected: str = "Today") -> Image.Image:
    canvas = Image.new("RGB", (PHONE_W, PHONE_H), CREAM)
    ui = ui.resize((PHONE_W, PHONE_H - 168), Image.Resampling.LANCZOS)
    canvas.paste(ui, (0, 0))
    draw_bottom_nav(ImageDraw.Draw(canvas), selected=selected)
    return canvas


def ui_progress() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    draw = ImageDraw.Draw(ui)
    title = load_font(40, bold=True)
    h2 = load_font(32, bold=True)
    body = load_font(26)
    small = load_font(22)

    draw.text((40, 48), "Дневник", fill=INK, font=title)
    draw.text((300, 130), "15.06 – 21.06", fill=INK, font=body)
    rounded_rect(draw, (40, 190, 800, 400), 28, WHITE)
    draw.text((70, 230), "СРЕДНЕЕ ЗА НЕДЕЛЮ", fill=INK_MUTED, font=small)
    draw.text((70, 275), "840", fill=MANGO, font=load_font(78, bold=True))
    draw.text((245, 315), "ккал", fill=INK_MUTED, font=body)
    draw.text((70, 355), "7 дней с данными · всего 5880 ккал", fill=INK_MUTED, font=small)

    rounded_rect(draw, (40, 470, 800, 1100), 28, WHITE)
    draw.text((70, 515), "Калории по дням", fill=INK, font=h2)
    draw.text((70, 562), "Среднее 840 ккал/день", fill=INK_MUTED, font=body)
    chart_left, chart_right = 105, 750
    chart_top, chart_bottom = 660, 1005
    for y, label in [(chart_top, "1200"), ((chart_top + chart_bottom) // 2, "600"), (chart_bottom, "0")]:
        draw.line((chart_left, y, chart_right, y), fill=OUTLINE, width=1)
        draw.text((70, y - 14), label, fill=INK_MUTED, font=load_font(18))
    values = [760, 910, 840, 1020, 730, 880, 740]
    labels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]
    bar_w = 46
    gap = 43
    for idx, value in enumerate(values):
        x = chart_left + idx * (bar_w + gap) + 10
        height = int((chart_bottom - chart_top) * value / 1200)
        y = chart_bottom - height
        rounded_rect(draw, (x, y, x + bar_w, chart_bottom), 16, MANGO if idx == 3 else MANGO_LIGHT)
        draw.text((x + 2, chart_bottom + 24), labels[idx], fill=INK_MUTED, font=load_font(20, bold=True))

    rounded_rect(draw, (40, 1160, 800, 1260), 24, MANGO_LIGHT)
    draw.text((70, 1192), "Смотри неделю целиком, не только один прием пищи", fill=INK, font=small)
    return ui


def ui_more_scans() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    draw = ImageDraw.Draw(ui)
    title = load_font(40, bold=True)
    h2 = load_font(32, bold=True)
    body = load_font(26)

    draw.text((40, 48), "Сканы закончились", fill=INK, font=title)
    draw.text((40, 110), "Посмотрите короткую рекламу или оформите Pro", fill=INK_MUTED, font=body)
    rounded_rect(draw, (40, 200, 800, 420), 28, MANGO_LIGHT)
    draw.text((70, 250), "На сегодня хватит", fill=INK, font=load_font(44, bold=True))
    draw.text((70, 320), "Завтра снова 3 бесплатных скана", fill=INK_MUTED, font=load_font(28))
    rounded_rect(draw, (40, 480, 800, 620), 24, (255, 248, 214))
    draw.text((70, 530), "+2 скана за рекламу", fill=INK, font=h2)
    rounded_rect(draw, (40, 660, 800, 800), 24, (240, 230, 255))
    draw.text((70, 710), "KkalScan Pro — без лимитов", fill=PRO, font=h2)
    draw_cta_button(draw, (80, 890, 760, 982), "Смотреть рекламу (+2)", LEMON, INK)
    draw_cta_button(draw, (80, 1020, 760, 1112), "KkalScan Pro — 199 руб/мес", PRO, WHITE)
    return ui


def ensure_scan_raw() -> Path:
    path = RAW / "03-scan-app.png"
    if not path.exists():
        render_in_app_scan_screenshot().save(path, "PNG", optimize=True)
    return path


def ensure_generated_raw(raw_name: str) -> Path:
    path = PUBLISHED / raw_name
    if path.exists():
        return path
    raise FileNotFoundError(path)


def is_probably_blank(path: Path) -> bool:
    if not path.exists():
        return False
    image = Image.open(path).convert("L")
    min_luma, max_luma = image.getextrema()
    return min_luma > 180 and max_luma - min_luma < 80


def repair_blank_web_raws() -> None:
    for raw_name, backup_path in LEGACY_RAW_BACKUPS.items():
        raw_path = RAW / raw_name
        if is_probably_blank(raw_path) and backup_path.exists():
            Image.open(backup_path).save(raw_path, "PNG", optimize=True)


def wrapped_lines(text: str, font, max_width: int) -> list[str]:
    draw = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = f"{current} {word}".strip()
        bbox = draw.textbbox((0, 0), candidate, font=font)
        if bbox[2] - bbox[0] <= max_width or not current:
            current = candidate
            continue
        lines.append(current)
        current = word
    if current:
        lines.append(current)
    return lines


def draw_centered_lines(
    draw: ImageDraw.ImageDraw,
    lines: list[str],
    y: int,
    font,
    fill: tuple[int, int, int],
    canvas_w: int,
    line_gap: int,
) -> int:
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        tw = bbox[2] - bbox[0]
        draw.text(((canvas_w - tw) // 2, y), line, fill=fill, font=font)
        y += bbox[3] - bbox[1] + line_gap
    return y


def draw_badges(draw: ImageDraw.ImageDraw, badges: tuple[str, ...], y: int, canvas_w: int) -> None:
    if not badges:
        return
    font = load_font(25, bold=True)
    badge_sizes: list[tuple[str, int]] = []
    for badge in badges:
        bbox = draw.textbbox((0, 0), badge, font=font)
        badge_sizes.append((badge, bbox[2] - bbox[0] + 44))
    total_w = sum(width for _, width in badge_sizes) + 16 * (len(badge_sizes) - 1)
    x = (canvas_w - total_w) // 2
    colors = [MANGO_LIGHT, (212, 250, 238), (255, 248, 214), (240, 230, 255)]
    for idx, (badge, width) in enumerate(badge_sizes):
        fill = colors[idx % len(colors)]
        text_color = PRO if fill == (240, 230, 255) else INK
        rounded_rect(draw, (x, y, x + width, y + 56), 18, fill)
        draw.text((x + 22, y + 14), badge, fill=text_color, font=font)
        x += width + 16


def draw_device_frame(canvas: Image.Image, shot: Image.Image, box: tuple[int, int, int, int], radius: int) -> None:
    draw = ImageDraw.Draw(canvas)
    x0, y0, x1, y1 = box
    rounded_rect(draw, (x0 - 8, y0 - 8, x1 + 8, y1 + 8), radius + 8, OUTLINE)
    rounded_rect(draw, (x0, y0, x1, y1), radius, WHITE)
    inner = fit_rustore_screenshot(shot).resize((x1 - x0, y1 - y0), Image.Resampling.LANCZOS)
    canvas.paste(inner, (x0, y0))


def draw_phone_marketing(title: str, subtitle: str, badges: tuple[str, ...], shot: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", (PHONE_W, PHONE_H), CREAM)
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(54, bold=True)
    subtitle_font = load_font(28)

    y = 56
    y = draw_centered_lines(draw, wrapped_lines(title, title_font, 940), y, title_font, INK, PHONE_W, 8)
    y = draw_centered_lines(draw, wrapped_lines(subtitle, subtitle_font, 900), y + 10, subtitle_font, INK_MUTED, PHONE_W, 4)
    draw_badges(draw, badges, y + 18, PHONE_W)

    draw_device_frame(canvas, shot, (120, 360, 960, 1860), 46)
    return canvas


def draw_tablet_marketing(title: str, subtitle: str, badges: tuple[str, ...], shot: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", (TABLET_W, TABLET_H), CREAM)
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(48, bold=True)
    draw_text_centered(draw, title, 34, title_font, INK, TABLET_W)
    draw_text_centered(draw, subtitle, 94, load_font(25), INK_MUTED, TABLET_W)
    draw_badges(draw, badges, 140, TABLET_W)

    tw, th = 640, 1040
    tx = (TABLET_W - tw) // 2
    ty = 146
    draw_device_frame(canvas, shot, (tx, ty, tx + tw, ty + th), 26)
    return canvas


def clean_generated_screenshots() -> None:
    for folder in (OUT, UPLOAD, TABLET):
        if not folder.exists():
            continue
        for path in folder.glob("screenshot-*.png"):
            path.unlink()


def main() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    PUBLISHED.mkdir(parents=True, exist_ok=True)
    TABLET.mkdir(parents=True, exist_ok=True)
    UPLOAD.mkdir(parents=True, exist_ok=True)
    clean_generated_screenshots()
    icon = load_store_icon()
    print(icon)

    for out_name, raw_name, title, subtitle, badges in SHOTS:
        raw_path = ensure_generated_raw(raw_name)
        raw_img = Image.open(raw_path)
        upload_img = draw_phone_marketing(title, subtitle, badges, raw_img)

        upload_path = UPLOAD / f"screenshot-{out_name}.png"
        upload_img.save(upload_path, "PNG", optimize=True)
        print(upload_path)

        phone_path = OUT / f"screenshot-{out_name}.png"
        upload_img.save(phone_path, "PNG", optimize=True)
        print(phone_path)

        tablet_path = TABLET / f"screenshot-{out_name}-landscape.png"
        draw_tablet_marketing(title, subtitle, badges, raw_img).save(tablet_path, "PNG", optimize=True)
        print(tablet_path)


if __name__ == "__main__":
    main()
