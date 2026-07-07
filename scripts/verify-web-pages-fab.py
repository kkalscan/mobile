#!/usr/bin/env python3
"""Verify deployed GitHub Pages web build: FAB expand + screenshot."""

from __future__ import annotations

import os
import sys
import time
from pathlib import Path

DEFAULT_URL = "https://kkalscan.github.io/mobile/?fake=1"
SCREENSHOT = Path(os.environ.get("FAB_SCREENSHOT", "fab-expanded-pages.png"))


def wait_hook(page, text: str, timeout_ms: int = 120_000) -> None:
    page.wait_for_function(
        f"() => document.getElementById('maestro-hook')?.textContent?.includes('{text}')",
        timeout=timeout_ms,
    )


def main() -> int:
    url = os.environ.get("WEB_BASE_URL", DEFAULT_URL)
    if "?" in url:
        url = f"{url}&t={time.time()}"
    else:
        url = f"{url}?fake=1&t={time.time()}"

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("Install playwright: pip install playwright && playwright install chromium")
        return 1

    failures: list[str] = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 430, "height": 900})
        page.goto(url, wait_until="networkidle", timeout=180_000)
        wait_hook(page, "diary-screen")

        fab_main = page.locator("#maestro-fab-main-hook").inner_text()
        if "diary-fab-main-count-1" not in fab_main:
            failures.append(f"expected single main FAB, got {fab_main}")

        page.locator("#maestro-tap-main-fab").click(force=True)
        page.wait_for_function(
            "() => document.getElementById('maestro-fab-hook')?.textContent === 'diary-fab-expanded'",
            timeout=15_000,
        )
        actions = page.locator("#maestro-fab-actions-hook").inner_text()
        if actions != "diary-fab-actions-3":
            failures.append(f"expected 3 FAB actions, got {actions}")

        page.screenshot(path=str(SCREENSHOT), full_page=False)
        print(f"Screenshot saved: {SCREENSHOT}")

        browser.close()

    if failures:
        for item in failures:
            print(f"FAIL {item}")
        return 1

    print(f"OK web FAB verified at {url}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
