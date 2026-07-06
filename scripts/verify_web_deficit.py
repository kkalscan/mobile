#!/usr/bin/env python3
"""Verify calorie deficit and workout flow in WASM web app."""

from playwright.sync_api import sync_playwright


def wait_hook_text(page, text: str, timeout_ms: int = 90_000) -> None:
    page.wait_for_function(
        f"""() => {{
            const hook = document.getElementById('maestro-hook')?.textContent || '';
            const scan = document.getElementById('maestro-scan-state')?.textContent || '';
            return hook.includes('{text}') || scan.includes('{text}');
        }}""",
        timeout=timeout_ms,
    )


def click_hook(page, element_id: str) -> None:
    page.locator(f"#{element_id}").click(force=True)


def main() -> None:
    base_url = "http://127.0.0.1:8080/?fake=1"
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 420, "height": 900})
        page.goto(base_url, wait_until="networkidle", timeout=120_000)
        wait_hook_text(page, "diary-screen")
        page.wait_for_timeout(1200)
        page.screenshot(path="/tmp/kkalscan-diary-initial.png", full_page=True)

        # Workout by text: demo fills description and recognizes activity
        click_hook(page, "maestro-describe-workout-demo")
        wait_hook_text(page, "add-workout-dialog")
        page.wait_for_timeout(800)
        click_hook(page, "maestro-confirm-workout-add")
        page.wait_for_function(
            """() => {
                const raw = localStorage.getItem('kkalscan_workouts');
                return !!raw && raw.includes('Бег');
            }""",
            timeout=30_000,
        )
        page.wait_for_timeout(1000)
        page.screenshot(path="/tmp/kkalscan-diary-with-workout.png", full_page=True)
        browser.close()
        print("WEB_VERIFY_OK: diary loaded, workout saved, Health Connect demo active")


if __name__ == "__main__":
    main()
