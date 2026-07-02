#!/usr/bin/env python3
"""Browser smoke test: feature search + deeplink navigation on local WASM."""

from __future__ import annotations

import sys
import time

URL = "http://localhost:8081/?fake=1"


def wait_hook(page, text: str, timeout_ms: int = 60_000) -> None:
    page.wait_for_function(
        f"() => document.getElementById('maestro-hook')?.textContent?.includes('{text}')",
        timeout=timeout_ms,
    )


def click(page, element_id: str) -> None:
    page.locator(f"#{element_id}").click(force=True)


def main() -> int:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("Install playwright in store/rustore/.venv")
        return 1

    failures: list[str] = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1080, "height": 2400})
        page.goto(f"{URL}&t={time.time()}", wait_until="networkidle", timeout=120_000)
        wait_hook(page, "diary-screen")
        hook = page.locator("#maestro-hook").inner_text()
        if "feature-search-bar" not in hook:
            failures.append(f"expected feature-search-bar on diary, got {hook}")
        else:
            print("OK feature search bar visible")
        print("OK diary-screen")

        click(page, "maestro-deeplink-profile")
        page.wait_for_timeout(800)
        hook = page.locator("#maestro-hook").inner_text()
        if "profile-screen" not in hook:
            failures.append(f"deeplink profile expected profile-screen, got {hook}")
        else:
            print("OK deeplink profile -> profile-screen")

        click(page, "maestro-deeplink-journal")
        page.wait_for_timeout(800)
        hook = page.locator("#maestro-hook").inner_text()
        if "journal-screen" not in hook:
            failures.append(f"deeplink journal expected journal-screen, got {hook}")
        else:
            print("OK deeplink journal -> journal-screen")

        click(page, "maestro-deeplink-diary")
        page.wait_for_timeout(800)
        hook = page.locator("#maestro-hook").inner_text()
        if "diary-screen" not in hook:
            failures.append(f"deeplink diary expected diary-screen, got {hook}")
        else:
            print("OK deeplink diary -> diary-screen")

        click(page, "maestro-food-search-demo")
        page.wait_for_timeout(1200)
        hook = page.locator("#maestro-hook").inner_text()
        if "feature-search-bar" not in hook:
            failures.append(f"feature search demo expected bar, got {hook}")
        else:
            print("OK feature search demo query")

        click(page, "maestro-feature-search-first")
        page.wait_for_timeout(1000)
        hook = page.locator("#maestro-hook").inner_text()
        if "profile-screen" not in hook:
            failures.append(f"feature search first result expected profile-screen, got {hook}")
        else:
            print("OK feature search -> deeplink -> profile-screen")

        browser.close()

    if failures:
        for msg in failures:
            print("FAIL", msg)
        return 1

    print("All E2E checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
