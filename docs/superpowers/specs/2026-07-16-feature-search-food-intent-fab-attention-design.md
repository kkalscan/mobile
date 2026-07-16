# Feature search food intent + FAB attention

**Date:** 2026-07-16  
**Status:** Approved for planning  
**Repos:** mobile + backend

## Problem

1. Users type food names (e.g. «борщ») into **feature search**, which is meant for app features. They expect product results; instead they get unrelated features or popular fallback.
2. New users who have never logged food or a workout often miss the diary FAB (+). We need a light, recurring attention cue until first successful add.

## Goals

- When the user **submits** a search that looks like a **food dish/product intent**, open the diary and tell them they can describe the dish in text or take a photo via the FAB.
- Detect food intent via a **dedicated REST + LLM** call (same OpenRouter stack as describe-food), not a hardcoded food dictionary.
- Attract attention to the diary FAB with a short **star / sparkle animation**, at most **once per minute**, only until the user has logged food or a workout at least once (local flag).

## Non-goals

- Turning feature search into a full food catalog search.
- Auto-calling the LLM on every keystroke / debounce.
- Counting intent classification against scan quota.
- Changing FAB action set (describe / workout / scan stay as today).

## Decisions (confirmed)

| Topic | Choice |
|-------|--------|
| Food redirect UX | Open diary immediately with a hint snackbar (no describe/scan chooser in search) |
| When to call LLM | Only after **Search/IME submit**; and only if local feature match is empty or popular-fallback |
| Min query | ≥ 3 characters after trim |
| FAB gate | Local persistent flag after first successful food **or** workout add |
| FAB cadence | Attention animation at most once per 60 seconds while eligible |
| Implementation | TDD (failing tests first) |

---

## 1. Backend: food-intent classification

### Endpoint

`POST /api/v1/feature-search/intent`

**Request**

```json
{ "query": "борщ" }
```

**Response**

```json
{ "isFoodIntent": true }
```

Optional later (not required for v1): `confidence`, `reason`. Keep v1 to a boolean.

### Behavior

1. Validate: trim query; reject blank / `< 3` / over max length (align with describe limits where sensible, e.g. 200 chars for a search query).
2. Call Vision/LLM client with a **short classification prompt** (text-only), same OpenRouter path as `analyzeDescription`, but **must not** create a scan session or decrement scans.
3. Parse model output into `isFoodIntent: Boolean`.
4. Stub provider returns deterministic fixtures for tests (e.g. known food queries → true, feature-like → false).
5. On LLM failure: return `isFoodIntent: false` (or 503 — prefer **false + log** so search UX degrades gracefully to “nothing found”).
6. Vision monthly budget: charge a **small** cost (same or lower than describe) so runaway usage is capped; document the constant.

### Prompt intent (summary)

Ask the model: given a short search string from an app feature search, is the user trying to find a **dish/product/meal** to log, vs navigating an app feature (diary, profile, scan, macros, etc.)? Answer JSON `{ "isFoodIntent": boolean }` only.

Examples expected:

| Query | isFoodIntent |
|-------|--------------|
| борщ | true |
| творог 200г | true |
| профиль | false |
| дневник | false |
| xyzunknown | false |

### Auth / routing

Same device-id / auth pattern as other `/api/v1/*` routes. Add to `RouteCatalog`.

---

## 2. Mobile: feature search submit flow

### Input behavior

- Keep local feature catalog filtering on typing **optional** for preview; **LLM intent must not run on debounce**.
- Wire IME **Search** action (and/or explicit search affordance) as **submit**.
- On submit:
  1. Normalize query (trim, lowercase for local match only).
  2. Run local `FeatureSearchCatalog.query`.
  3. If there is a **real match** (`popularFallback == false` and items non-empty) → show results as today.
  4. Else if query length ≥ 3 → call `POST .../feature-search/intent`.
  5. If `isFoodIntent == true` → clear search, navigate to diary (`kkalscan://diary` / existing tab), show snackbar:  
     **«Опишите блюдо текстом или сфотографируйте — кнопка +»**
  6. Else → keep current empty/popular UI.

### Analytics

- `feature_search_food_intent` with params: `query_length`, `is_food` (boolean). Do **not** send raw query text if privacy-sensitive; length + boolean is enough for v1.
- Existing `feature_search_query` remains for local/executed searches as today (document whether submit-only changes firing).

### API client

- Add `classifyFeatureSearchIntent(query: String): FeatureSearchIntentResult` on `IKkalScanApi` + real/fake implementations.
- Fake: map a few fixtures for unit tests.

---

## 3. Mobile: FAB attention animation

### Visual

- Around `diary-main-fab`: short sparkle/star particles + subtle pulse on the FAB itself.
- Duration: short (~1–1.5s), non-blocking, does not steal clicks.
- Must not run while FAB is expanded or `actionLoading`.

### Gating

- Local persistent flag `hasLoggedAnything` (name TBD in plan), default `false`.
- Set `true` after first successful:
  - add food to diary, **or**
  - add workout.
- While `false`, schedule attention show at most every **60 seconds** while Today tab is visible and FAB is shown.
- Once `true`, never show again (until app data wipe).

### Analytics

- `fab_attention_shown` when an attention cycle plays.

---

## 4. Testing (TDD)

### Backend

- Route/service tests: food query → true; feature query → false; short query → 400; stub LLM paths.
- Budget / no scan-session side effects.

### Mobile shared

- ViewModel/flow: submit + food intent → diary callback + hint; submit + feature match → no intent API; submit + not food → empty/popular.
- Flag storage: first add flips flag; attention eligibility false after.

### UI (as needed)

- Instrumented: FAB attention not required for every visual frame; prefer unit tests for scheduling/gating; one UI smoke if cheap.

---

## 5. Architecture sketch

```
User presses Search
        │
        ▼
Local FeatureSearchCatalog
        │
   match? ──yes──► show feature results
        │ no / popular only
        ▼
POST /feature-search/intent  (LLM)
        │
   food? ──yes──► Diary + snackbar (text/photo via +)
        │ no
        ▼
Empty / popular fallback
```

FAB attention is independent: Today + `!hasLoggedAnything` + 60s interval → sparkles.

---

## 6. Risks / open edges (resolved defaults)

| Risk | Default |
|------|---------|
| LLM latency on submit | Show brief searching state; timeout → treat as not food |
| Cost | Text-only classify; budget line item; only on submit after no feature match |
| False positives | Prefer false on error; prompt emphasizes app-feature negatives |
| Search without keyboard Search key | Provide IME `ImeAction.Search`; ensure hardware/soft keyboard both work |

---

## Success criteria

- Searching «борщ» (submit, no strong feature hit) opens diary with the snackbar.
- Searching «профиль» still opens/shows profile feature result without diary redirect.
- FAB sparkles at most once/minute until first food or workout log, then never.
- All new behavior covered by failing-then-passing tests (TDD).
