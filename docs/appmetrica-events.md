# AppMetrica Events

KkalScan uses AppMetrica in release Android builds to understand where users get value, where they get blocked, and which missing product pieces are worth building next.

## Core Funnels

### Scan Funnel

```text
scan_open -> photo_selected -> photo_scan -> scan_success -> add_to_diary
```

### Describe Food Funnel

```text
describe_food_open -> describe_text_scan -> scan_success -> describe_food_recognized -> add_to_diary
```

Text description reuses the same `scan_success`, `limit_hit`, and `add_to_diary` events as photo scan.

Read this funnel as:

| Drop-off | Likely signal |
|----------|---------------|
| `scan_open` high, `photo_selected` low | Camera/gallery flow is unclear, permission was denied, or the user backed out. Check `photo_picker_cancel`. |
| `photo_scan` high, `scan_success` low | Recognition, network, backend, or image quality issue. Check `scan_error`. |
| `scan_success` high, `add_to_diary` low | The result screen or diary action is unclear, or users do not trust the result. |
| `add_to_diary_failed` present | Diary persistence/API failure after a successful scan. |

### Monetization Funnel

```text
third_scan_success -> limit_hit -> paywall_shown -> pro_click
```

Read this funnel as:

| Drop-off | Likely signal |
|----------|---------------|
| `third_scan_success` high, `limit_hit` low | Limit may not be reached often enough to test monetization. |
| `limit_hit` high, `paywall_shown` low | Navigation or paywall display bug. |
| `paywall_shown` high, `pro_click` low | Value proposition, price, or paywall copy is weak. |
| `paywall_back` high | User saw the monetization prompt and intentionally left. |

### Rewarded Ad Funnel

```text
ad_offer_shown -> ad_bonus_click -> ad_watch_complete
```

Read this funnel as:

| Drop-off | Likely signal |
|----------|---------------|
| `ad_offer_shown` high, `ad_bonus_click` low | Reward is not compelling or UI is unclear. |
| `ad_bonus_click` high, `ad_watch_complete` low | Ad provider, network, or bonus grant failure. Check `ad_bonus_failed`. |

## Event Catalog

| Event | When it fires | Important attributes |
|-------|---------------|----------------------|
| `app_launch` | AppMetrica initializes in release build | none |
| `feature_open` | Root screen changes | `feature` |
| `scan_open` | User opens camera/photo scan entry point | none |
| `photo_selected` | Photo bytes are returned by the picker | none |
| `photo_picker_cancel` | Picker returns no photo. This includes user cancel, denied camera permission, or camera URI creation failure in the current picker API. | none |
| `photo_scan` | Photo is submitted to backend recognition | none |
| `describe_food_open` | User opens text description sheet | none |
| `describe_text_scan` | Text description is submitted to backend | none |
| `describe_food_recognized` | AI returned a result for text description | none |
| `scan_success` | Backend returns a scan result | none |
| `scan_error` | Scan finishes without result and without limit-hit state | `reason` |
| `first_scan_success` | Scan success leaves 3 free scans | none |
| `second_scan_success` | Scan success leaves 2 free scans | none |
| `third_scan_success` | Scan success leaves 1 free scan | none |
| `add_to_diary` | User confirms saving scan result to diary | none |
| `add_to_diary_failed` | Diary save fails after confirmation | `reason` |
| `limit_hit` | Scan attempt reaches free scan limit | `scans_left` |
| `paywall_shown` | Paywall screen opens | none |
| `paywall_back` | User backs out of paywall | none |
| `pro_click` | User taps Pro payment CTA | none |
| `ad_offer_shown` | Paywall/ad offer screen opens | none |
| `ad_bonus_click` | User taps rewarded ad CTA | none |
| `ad_watch_complete` | Ad bonus grant succeeds | `scans_left` |
| `ad_bonus_failed` | Ad bonus grant fails or leaves user blocked | `reason` |
| `bug_report_open` | User opens bug report dialog | none |
| `bug_report_submit` | User submits bug report | none |
| `dietitian_insight_click` | User requests AI dietitian insight | none |
| `feature_search_open` | Feature search opened | none |
| `feature_search_query` | Feature search query executed | `query`, `query_length`, `results`, `empty_query` |
| `feature_search_food_intent` | Feature search classified as food (or not) after no feature match | `query_length`, `is_food` (no raw query) |
| `fab_attention_shown` | Diary FAB sparkle/pulse attention animation played | none |
| `deeplink_open` | Deeplink navigation | `link` |
| `subscription_start` | Pro subscription activated | none |
| `day_1_return` | User returns on day 1 after install | none |
| `day_7_return` | User returns on day 7 after install | none |
| `dev_stub_scan` | Maestro/dev bridge triggers a stub scan | none |

## Device Segments

Always compare funnels by:

- `ym:ce:mobileDeviceBranding`
- `ym:ce:mobileDeviceModel`
- app version
- OS version

If one model has lower `scan_success / photo_scan`, look for camera, image compression, memory, or network problems specific to that device class.

## Queries To Ask

- Which device models reach `photo_scan` but not `scan_success`?
- Which users reach `scan_success` but do not send `add_to_diary`?
- How many users hit `limit_hit` and then leave via `paywall_back`?
- Does `pro_click / paywall_shown` improve after changing paywall copy?
- Are `scan_error` reasons clustered around network, backend API, or recognition quality?

## Current Limitation

The photo picker currently reports a single `photo_picker_cancel` event for user cancel, denied camera permission, and camera URI creation failure. Split this into separate reason values if picker-level diagnosis becomes important.

`install` is tracked by AppMetrica automatically — no custom event required. Use AppMetrica installs metric in funnel denominators.

`subscription_cancel` is not implemented yet.
