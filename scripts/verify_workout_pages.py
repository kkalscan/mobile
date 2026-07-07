#!/usr/bin/env python3
"""Verify AI workout describe flow on GitHub Pages with Playwright."""

from __future__ import annotations

import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCREENSHOT_DIR = ROOT
URL = "https://kkalscan.github.io/mobile/?fake=1"


def wait_for_hook(page, text: str, timeout_ms: int = 120_000) -> None:
    page.wait_for_function(
        f"() => document.getElementById('maestro-hook')?.textContent?.includes('{text}')",
        timeout=timeout_ms,
    )


def main() -> int:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("Install playwright: pip install playwright && playwright install chromium", file=sys.stderr)
        return 1

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1080, "height": 2400})
        page.goto(f"{URL}&t={time.time()}", wait_until="networkidle", timeout=120_000)
        wait_for_hook(page, "diary-screen")
        page.wait_for_timeout(800)

        shot1 = SCREENSHOT_DIR / "pages-workout-01-diary.png"
        page.locator("#ComposeTarget").screenshot(path=str(shot1))
        print(f"screenshot: {shot1}")

        page.locator("#maestro-tap-main-fab").click(force=True)
        page.wait_for_function(
            "() => document.getElementById('maestro-fab-hook')?.textContent === 'diary-fab-expanded'",
            timeout=15_000,
        )
        shot2 = SCREENSHOT_DIR / "pages-workout-02-fab-expanded.png"
        page.locator("#ComposeTarget").screenshot(path=str(shot2))
        print(f"screenshot: {shot2}")

        # Open workout via maestro hook (maps to middle FAB action)
        if page.locator("#maestro-open-workout").count() == 0:
            print("WARN: maestro-open-workout hook missing — Pages may not be redeployed yet")
            browser.close()
            return 2

        page.locator("#maestro-open-workout").click(force=True)
        page.wait_for_function(
            "() => document.getElementById('maestro-hook')?.textContent?.includes('quick-workout-dialog')",
            timeout=15_000,
        )
        shot3 = SCREENSHOT_DIR / "pages-workout-03-dialog.png"
        page.locator("#ComposeTarget").screenshot(path=str(shot3))
        print(f"screenshot: {shot3}")

        page.locator("#maestro-workout-demo").click(force=True)
        page.wait_for_function(
            "() => document.getElementById('maestro-hook')?.textContent?.includes('quick-workout-preview')",
            timeout=20_000,
        )
        shot4 = SCREENSHOT_DIR / "pages-workout-04-preview.png"
        page.locator("#ComposeTarget").screenshot(path=str(shot4))
        print(f"screenshot: {shot4}")
        print("OK: workout preview visible (Бег / 300 kcal expected from fake API)")

        browser.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
