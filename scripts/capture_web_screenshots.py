#!/usr/bin/env python3
"""Capture RuStore raw screenshots from the local Compose WASM dev server."""

from __future__ import annotations

import argparse
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PUBLISHED = ROOT / "store" / "rustore" / "published"
DEFAULT_URL = "http://localhost:8081/?fake=1"


def wait_for_hook(page, text: str, timeout_ms: int = 60_000) -> None:
    page.wait_for_function(
        f"() => document.getElementById('maestro-hook')?.textContent?.includes('{text}')",
        timeout=timeout_ms,
    )


def wait_until_hook_not(page, text: str, timeout_ms: int = 60_000) -> None:
    page.wait_for_function(
        f"() => !document.getElementById('maestro-hook')?.textContent?.includes('{text}')",
        timeout=timeout_ms,
    )


def click_hook(page, element_id: str) -> None:
    page.locator(f"#{element_id}").click(force=True)


def scroll_canvas(page, steps: int = 8, delta: int = 500) -> None:
    canvas = page.locator("#ComposeTarget")
    box = canvas.bounding_box()
    if not box:
        return
    x = box["x"] + box["width"] / 2
    y = box["y"] + box["height"] / 2
    for _ in range(steps):
        page.mouse.move(x, y)
        page.mouse.wheel(0, delta)
        page.wait_for_timeout(180)


def capture(page, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    page.locator("#ComposeTarget").screenshot(path=str(path))
    print(path)


def open_app(playwright, base_url: str):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context(
        viewport={"width": 1080, "height": 2400},
        device_scale_factor=1,
        locale="ru-RU",
    )
    page = context.new_page()
    page.goto(f"{base_url}&t={time.time()}", wait_until="networkidle", timeout=120_000)
    wait_for_hook(page, "diary-screen")
    page.wait_for_timeout(800)
    return browser, page


def save_generated_diary_raw() -> None:
    """Fallback raw for diary when WASM automation cannot add entries."""
    sys.path.insert(0, str(ROOT / "store" / "rustore"))
    from frame_device_screenshots import render_generated_raw
    from generate_assets import ui_diary

    path = PUBLISHED / "screenshot-01-diary.png"
    render_generated_raw(ui_diary(), selected="Today").save(path, "PNG", optimize=True)
    print(path)


def run_capture(base_url: str) -> None:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError as exc:
        raise SystemExit(
            "Install playwright: cd mobile/store/rustore && .venv/bin/pip install playwright && .venv/bin/playwright install chromium"
        ) from exc

    with sync_playwright() as playwright:
        browser, page = open_app(playwright, base_url)
        capture(page, PUBLISHED / "screenshot-04-free.png")

        click_hook(page, "maestro-stub-scan")
        wait_for_hook(page, "scan-result")
        page.wait_for_timeout(1200)
        capture(page, PUBLISHED / "screenshot-02-result.png")
        browser.close()

        browser, page = open_app(playwright, base_url)
        click_hook(page, "maestro-food-search-demo")
        wait_for_hook(page, "food-search")
        page.wait_for_timeout(1200)
        capture(page, PUBLISHED / "screenshot-03-scan.png")
        browser.close()

        save_generated_diary_raw()

        browser, page = open_app(playwright, base_url)
        click_hook(page, "maestro-open-journal")
        wait_for_hook(page, "journal-screen")
        page.wait_for_timeout(1000)
        scroll_canvas(page, steps=10, delta=600)
        page.wait_for_timeout(600)
        capture(page, PUBLISHED / "screenshot-05-limit.png")
        browser.close()


def frame_screenshots() -> None:
    script = ROOT / "store" / "rustore" / "frame_device_screenshots.py"
    venv_python = ROOT / "store" / "rustore" / ".venv" / "bin" / "python"
    python = venv_python if venv_python.exists() else Path(sys.executable)
    subprocess.run([str(python), str(script)], check=True, cwd=ROOT / "store" / "rustore")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default=DEFAULT_URL, help="WASM dev server URL")
    parser.add_argument("--frame-only", action="store_true", help="Only run framing step")
    args = parser.parse_args()

    if not args.frame_only:
        run_capture(args.url)
    frame_screenshots()


if __name__ == "__main__":
    main()
