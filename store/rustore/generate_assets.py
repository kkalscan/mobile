#!/usr/bin/env python3
"""Generate RuStore store assets for KkalScan (icon + marketing screenshots)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
OUT = ROOT

# Citrus Scan palette
MANGO = (255, 122, 47)
MANGO_PINK = (255, 77, 141)
MINT = (30, 201, 149)
LEMON = (255, 229, 102)
CREAM = (255, 252, 248)
INK = (28, 20, 40)
INK_MUTED = (107, 94, 114)
WHITE = (255, 255, 255)
PRO = (155, 92, 255)
SURFACE = (255, 255, 255)
OUTLINE = (232, 223, 212)
MANGO_LIGHT = (255, 228, 209)
MINT_LIGHT = (212, 250, 238)
PROTEIN = (91, 141, 239)
FAT = (255, 159, 67)
CARBS = MINT


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial Unicode.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def gradient(size: int) -> Image.Image:
    img = Image.new("RGB", (size, size))
    draw = ImageDraw.Draw(img)
    for y in range(size):
        t = y / max(size - 1, 1)
        r = int(MANGO[0] + (MANGO_PINK[0] - MANGO[0]) * t)
        g = int(MANGO[1] + (MANGO_PINK[1] - MANGO[1]) * t)
        b = int(MANGO[2] + (MANGO_PINK[2] - MANGO[2]) * t)
        draw.line([(0, y), (size, y)], fill=(r, g, b))
    return img


def draw_viewfinder(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int], width: int = 10) -> None:
    x0, y0, x1, y1 = box
    arm = min(x1 - x0, y1 - y0) // 5
    for x, y, dx, dy in [
        (x0, y0, 1, 1),
        (x1, y0, -1, 1),
        (x0, y1, 1, -1),
        (x1, y1, -1, -1),
    ]:
        draw.line([(x, y), (x + dx * arm, y)], fill=color, width=width)
        draw.line([(x, y), (x, y + dy * arm)], fill=color, width=width)


def generate_icon() -> Path:
    import importlib.util

    spec = importlib.util.spec_from_file_location(
        "generate_icons",
        ROOT.parent.parent / "scripts" / "generate-icons.py",
    )
    if spec is None or spec.loader is None:
        raise RuntimeError("Cannot load generate-icons.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    path = OUT / "icon-512.png"
    module.draw_icon(512).save(path, "PNG", optimize=True)
    return path


def rounded_rect(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], radius: int, fill: tuple[int, int, int]) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def draw_text_centered(
    draw: ImageDraw.ImageDraw,
    text: str,
    y: int,
    font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    fill: tuple[int, int, int],
    canvas_w: int,
) -> None:
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    draw.text(((canvas_w - tw) // 2, y), text, fill=fill, font=font)


def draw_arc_ring(
    draw: ImageDraw.ImageDraw,
    cx: int,
    cy: int,
    radius: int,
    thickness: int,
    fraction: float,
    color: tuple[int, int, int],
    track: tuple[int, int, int],
) -> None:
    bbox = (cx - radius, cy - radius, cx + radius, cy + radius)
    draw.arc(bbox, start=90, end=450, fill=track, width=thickness)
    if fraction > 0:
        sweep = max(8, int(360 * min(fraction, 1.0)))
        draw.arc(bbox, start=90, end=90 - sweep, fill=color, width=thickness)


def draw_floating_card(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
) -> None:
    x0, y0, x1, y1 = box
    rounded_rect(draw, (x0 + 3, y0 + 5, x1 + 3, y1 + 5), 22, OUTLINE)
    rounded_rect(draw, box, 22, WHITE)


def overlay_calorie_ring_card(img: Image.Image, eaten: int, goal: int, x: int | None = None, y: int | None = None) -> None:
    draw = ImageDraw.Draw(img)
    x = 48 if x is None else x
    y = 1580 if y is None else y
    w, h = 220, 230
    draw_floating_card(draw, (x, y, x + w, y + h))
    cx, cy = x + w // 2, y + 92
    draw_arc_ring(draw, cx, cy, 58, 14, eaten / max(goal, 1), MANGO, OUTLINE)
    val_font = load_font(38, bold=True)
    small = load_font(20)
    label = str(eaten)
    bbox = draw.textbbox((0, 0), label, font=val_font)
    tw = bbox[2] - bbox[0]
    draw.text((cx - tw // 2, cy - 22), label, fill=MANGO, font=val_font)
    draw.text((cx - 20, cy + 14), "ккал", fill=INK_MUTED, font=small)
    draw.text((x + 36, y + 178), f"цель {goal}", fill=INK_MUTED, font=small)


def overlay_macro_donut_card(
    img: Image.Image,
    protein: float,
    fat: float,
    carbs: float,
    x: int | None = None,
    y: int | None = None,
) -> None:
    draw = ImageDraw.Draw(img)
    x = 300 if x is None else x
    y = 1580 if y is None else y
    w, h = 250, 230
    draw_floating_card(draw, (x, y, x + w, y + h))
    total = max(protein + fat + carbs, 1.0)
    cx, cy, r = x + w // 2, y + 98, 64
    thickness = 15
    segments = [(protein, PROTEIN), (fat, FAT), (carbs, CARBS)]
    bbox = (cx - r, cy - r, cx + r, cy + r)
    draw.arc(bbox, start=0, end=360, fill=OUTLINE, width=thickness)
    start = 90
    for value, color in segments:
        sweep = max(6, int(360 * value / total))
        draw.arc(bbox, start=start, end=start - sweep, fill=color, width=thickness)
        start -= sweep

    title = load_font(22, bold=True)
    small = load_font(18)
    draw.text((x + 24, y + 18), "БЖУ", fill=INK, font=title)
    labels = [("Б", protein, PROTEIN), ("Ж", fat, FAT), ("У", carbs, CARBS)]
    lx = x + 20
    for letter, grams, color in labels:
        rounded_rect(draw, (lx, y + 178, lx + 68, y + 214), 12, SURFACE)
        draw.text((lx + 8, y + 184), f"{letter} {int(grams)}г", fill=color, font=small)
        lx += 74


def overlay_weekly_bars_card(
    img: Image.Image,
    values: list[int],
    labels: list[str],
    x: int | None = None,
    y: int | None = None,
) -> None:
    draw = ImageDraw.Draw(img)
    x = 580 if x is None else x
    y = 1580 if y is None else y
    w, h = 450, 230
    draw_floating_card(draw, (x, y, x + w, y + h))
    draw.text((x + 20, y + 16), "Калории за неделю", fill=INK, font=load_font(22, bold=True))
    chart_top, chart_bottom = y + 52, y + h - 44
    chart_left, chart_right = x + 24, x + w - 20
    max_val = max(max(values, default=0), 300)
    bar_w = (chart_right - chart_left) // max(len(values), 1) - 8
    for idx, value in enumerate(values):
        bx = chart_left + idx * (bar_w + 8)
        bh = int((chart_bottom - chart_top) * value / max_val)
        by = chart_bottom - bh
        color = MANGO if value > 0 else OUTLINE
        rounded_rect(draw, (bx, by, bx + bar_w, chart_bottom), 8, color)
        if value > 0:
            draw.text((bx + 4, by - 24), str(value), fill=MANGO, font=load_font(16, bold=True))
        label = labels[idx] if idx < len(labels) else ""
        bbox = draw.textbbox((0, 0), label, font=load_font(16))
        tw = bbox[2] - bbox[0]
        draw.text((bx + (bar_w - tw) // 2, chart_bottom + 8), label, fill=INK_MUTED, font=load_font(16))


def draw_daily_chart_strip(canvas: Image.Image) -> None:
    overlay_calorie_ring_card(canvas, eaten=110, goal=2000, x=48, y=1660)
    overlay_macro_donut_card(canvas, protein=1, fat=0, carbs=28, x=310, y=1660)
    overlay_weekly_bars_card(
        canvas,
        values=[0, 0, 110, 0, 0, 0, 0],
        labels=["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"],
        x=590, y=1660,
    )


def draw_macro_chart_strip(canvas: Image.Image) -> None:
    overlay_macro_donut_card(canvas, protein=12, fat=8, carbs=52, x=120, y=1660)
    overlay_calorie_ring_card(canvas, eaten=420, goal=2000, x=430, y=1660)


def draw_phone_canvas(title: str) -> Image.Image:
    w, h = 1080, 1920
    canvas = Image.new("RGB", (w, h), CREAM)
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(52, bold=True)
    bbox = draw.textbbox((0, 0), title, font=title_font)
    tw = bbox[2] - bbox[0]
    draw.text(((w - tw) // 2, 72), title, fill=INK, font=title_font)

    px, py, pw, ph = 120, 180, 840, 1560
    rounded_rect(draw, (px - 8, py - 8, px + pw + 8, py + ph + 8), 48, OUTLINE)
    rounded_rect(draw, (px, py, px + pw, py + ph), 40, WHITE)
    return canvas


def paste_ui(canvas: Image.Image, ui: Image.Image) -> None:
    px, py, pw, ph = 120, 180, 840, 1560
    ui = ui.resize((pw, ph), Image.Resampling.LANCZOS)
    canvas.paste(ui, (px, py))


def ui_diary() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    d = ImageDraw.Draw(ui)
    h1 = load_font(40, bold=True)
    h2 = load_font(34, bold=True)
    body = load_font(28)
    small = load_font(24)
    d.text((40, 48), "Сегодня · 17.06.2026", fill=INK, font=h1)
    rounded_rect(d, (40, 130, 800, 320), 28, MANGO_LIGHT)
    d.text((70, 160), "Съедено сегодня", fill=INK_MUTED, font=body)
    d.text((70, 210), "1840", fill=MANGO, font=load_font(72, bold=True))
    d.text((250, 245), "ккал", fill=INK_MUTED, font=body)
    meals = [
        ("Борщ с говядиной", "320", MINT_LIGHT),
        ("Гречка с курицей", "420", MANGO_LIGHT),
        ("Салат оливье", "280", LEMON if False else (255, 248, 214)),
    ]
    y = 360
    for name, kcal, bg in meals:
        rounded_rect(d, (40, y, 800, y + 150), 24, SURFACE)
        d.rectangle((40, y, 120, y + 150), fill=bg)
        d.text((140, y + 28), name, fill=INK, font=h2)
        d.text((140, y + 82), f"{kcal} ккал", fill=MANGO, font=load_font(44, bold=True))
        y += 170
    rounded_rect(d, (320, 1380, 520, 1480), 999, MANGO)
    d.text((365, 1415), "+", fill=WHITE, font=load_font(48, bold=True))
    return ui


def macro_chip(d: ImageDraw.ImageDraw, x: int, y: int, label: str, color: tuple[int, int, int], bg: tuple[int, int, int]) -> None:
    rounded_rect(d, (x, y, x + 110, y + 52), 16, bg)
    d.text((x + 18, y + 12), label, fill=color, font=load_font(24, bold=True))


def ui_result() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    d = ImageDraw.Draw(ui)
    d.text((40, 48), "Результат скана", fill=INK, font=load_font(40, bold=True))
    rounded_rect(d, (40, 120, 800, 430), 28, MANGO_LIGHT)
    d.text((70, 150), "ИТОГО", fill=INK_MUTED, font=load_font(28))
    d.text((70, 200), "420", fill=MANGO, font=load_font(84, bold=True))
    d.text((230, 235), "ккал", fill=INK_MUTED, font=load_font(30))
    d.text((70, 300), "Борщ с говядиной, сметана", fill=INK, font=load_font(26))
    macro_chip(d, 70, 350, "Б 12", PROTEIN, (232, 240, 255))
    macro_chip(d, 200, 350, "Ж 8", FAT, (255, 240, 224))
    macro_chip(d, 330, 350, "У 52", CARBS, MINT_LIGHT)
    rounded_rect(d, (40, 470, 800, 620), 24, SURFACE)
    d.text((60, 500), "Борщ с говядиной", fill=INK, font=load_font(32, bold=True))
    d.text((60, 555), "320 г · 420 ккал", fill=INK_MUTED, font=load_font(26))
    rounded_rect(d, (40, 1320, 800, 1420), 999, MANGO)
    d.text((260, 1355), "Добавить в дневник", fill=WHITE, font=load_font(30, bold=True))
    return ui


def ui_scan() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    d = ImageDraw.Draw(ui)
    d.text((40, 48), "Скан еды", fill=INK, font=load_font(40, bold=True))
    d.text((40, 110), "AI распознаёт блюда и считает калории", fill=INK_MUTED, font=load_font(26))
    rounded_rect(d, (40, 170, 280, 230), 999, (255, 248, 214))
    d.text((70, 188), "Осталось 3 бесплатных скана", fill=INK, font=load_font(22, bold=True))
    rounded_rect(d, (40, 280, 800, 920), 32, (255, 228, 209))
    draw_viewfinder(d, (120, 360, 720, 840), MINT, 12)
    d.ellipse((320, 500, 520, 700), outline=WHITE, width=8)
    rounded_rect(d, (40, 980, 800, 1080), 999, MANGO)
    d.text((250, 1015), "Выбрать фото", fill=WHITE, font=load_font(34, bold=True))
    return ui


def ui_free_badge() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    d = ImageDraw.Draw(ui)
    d.text((40, 48), "Скан еды", fill=INK, font=load_font(40, bold=True))
    rounded_rect(d, (120, 420, 720, 560), 32, (255, 248, 214))
    d.text((170, 470), "3 бесплатных скана каждый день", fill=INK, font=load_font(34, bold=True))
    d.text((120, 620), "Хватит на завтрак, обед и ужин", fill=INK_MUTED, font=load_font(28))
    rounded_rect(d, (40, 760, 800, 980), 28, SURFACE)
    d.text((70, 810), "Без регистрации", fill=INK, font=load_font(36, bold=True))
    d.text((70, 870), "Открыли приложение — сразу сканируете", fill=INK_MUTED, font=load_font(26))
    return ui


def ui_limit() -> Image.Image:
    ui = Image.new("RGB", (840, 1560), CREAM)
    d = ImageDraw.Draw(ui)
    d.text((40, 48), "Сканы закончились", fill=INK, font=load_font(40, bold=True))
    d.text((40, 110), "Посмотрите короткую рекламу или оформите Pro", fill=INK_MUTED, font=load_font(26))
    rounded_rect(d, (40, 200, 800, 420), 28, MANGO_LIGHT)
    d.text((70, 250), "На сегодня хватит", fill=INK, font=load_font(44, bold=True))
    d.text((70, 320), "Завтра снова 3 бесплатных скана", fill=INK_MUTED, font=load_font(28))
    rounded_rect(d, (40, 480, 800, 620), 24, (255, 248, 214))
    d.text((70, 530), "+2 скана за рекламу", fill=INK, font=load_font(32, bold=True))
    rounded_rect(d, (40, 660, 800, 800), 24, (240, 230, 255))
    d.text((70, 710), "KkalScan Pro — без лимитов", fill=PRO, font=load_font(32, bold=True))
    rounded_rect(d, (40, 880, 800, 980), 999, LEMON)
    d.text((210, 915), "Смотреть рекламу (+2)", fill=INK, font=load_font(30, bold=True))
    rounded_rect(d, (40, 1010, 800, 1110), 999, PRO)
    d.text((220, 1045), "KkalScan Pro — 199 ₽/мес", fill=WHITE, font=load_font(30, bold=True))
    return ui


def generate_screenshot(name: str, title: str, ui_factory) -> Path:
    canvas = draw_phone_canvas(title)
    paste_ui(canvas, ui_factory())
    path = OUT / f"screenshot-{name}.png"
    canvas.save(path, "PNG", optimize=True)
    return path


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    icon = generate_icon()
    shots = [
        generate_screenshot("01-diary", "Дневник питания за день", ui_diary),
        generate_screenshot("02-result", "Калории по фото за секунды", ui_result),
        generate_screenshot("03-scan", "Сфотографируй еду", ui_scan),
        generate_screenshot("04-free", "3 бесплатных скана каждый день", ui_free_badge),
        generate_screenshot("05-limit", "Лимит на сегодня — завтра снова 3", ui_limit),
    ]
    print("Generated:")
    print(icon)
    for s in shots:
        print(s)


if __name__ == "__main__":
    main()
