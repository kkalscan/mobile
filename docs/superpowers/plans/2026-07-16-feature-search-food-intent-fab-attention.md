# Feature search food intent + FAB attention — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On Search submit, if the query is not a real app-feature hit, ask the LLM whether the user is looking for food; if yes, open the diary with a hint to describe/photo via FAB. Separately, sparkle-animate the diary FAB once per minute until the user has logged food or a workout once.

**Architecture:** Backend adds `POST /api/v1/feature-search/intent` using the existing OpenRouter `VisionClient` stack with a new classify method (no scan quota). Mobile feature search becomes submit-driven for intent; local/API feature results still run on submit. FAB attention is a Compose overlay gated by a small expect/actual prefs flag mirrored after successful diary add / workout add.

**Tech Stack:** Ktor backend (Kotlin), OpenRouter text chat, KMP shared ViewModels + Compose Multiplatform UI, Kotest/`kotlin.test`, SharedPreferences / in-memory prefs.

**Repos:** `backend/` and `mobile/` are **separate git repos**. Commit backend work in `backend/`, mobile work in `mobile/`. Spec: `mobile/docs/superpowers/specs/2026-07-16-feature-search-food-intent-fab-attention-design.md`.

## Global Constraints

- LLM intent runs **only on Search/IME submit**, never on debounce keystrokes.
- Call intent only when feature search has **no real match** (empty or `popularFallback == true`), query trim length ≥ 3.
- Intent must **not** consume scan quota; on LLM failure treat as `isFoodIntent = false`.
- Snackbar copy (exact): `Опишите блюдо текстом или сфотографируйте — кнопка +`
- FAB attention: ≤ once / 60s; only while `hasLoggedAnything == false`; skip when FAB expanded or loading.
- TDD: no production code without a failing test first.
- Do not send raw search query strings in analytics for food-intent (only `query_length` + `is_food`).

## File map

### Backend (`backend/`)

| File | Role |
|------|------|
| `src/main/kotlin/ru/kkalscan/domain/port/Services.kt` | Add `classifySearchIntent` to `VisionClient` |
| `src/main/kotlin/ru/kkalscan/integrations/openrouter/OpenRouterModels.kt` | `FeatureSearchIntentPrompt` + `buildFeatureSearchIntent` + parser |
| `src/main/kotlin/ru/kkalscan/integrations/openrouter/OpenRouterVisionClient.kt` | Implement classify |
| `src/main/kotlin/ru/kkalscan/integrations/Stubs.kt` | Stub classify heuristics |
| `src/main/kotlin/ru/kkalscan/domain/port/FeatureSearchService.kt` | Add `classifyIntent` |
| `src/main/kotlin/ru/kkalscan/domain/service/FeatureSearchServiceImpl.kt` | Validation + budget + vision call |
| `src/main/kotlin/ru/kkalscan/api/dto/Dtos.kt` | Request/response DTOs |
| `src/main/kotlin/ru/kkalscan/routes/ApiRoutes.kt` | `POST /feature-search/intent` |
| `src/main/kotlin/ru/kkalscan/routes/RouteCatalog.kt` | Document route |
| `src/main/kotlin/ru/kkalscan/AppConfig.kt` | Optional `visionCostPerIntentRub` (default 1) |
| Tests under `src/test/kotlin/...` | Service + route + prompt parse |

### Mobile (`mobile/`)

| File | Role |
|------|------|
| `shared/.../domain/model/Models.kt` (or features models) | `FeatureSearchIntentResult` |
| `shared/.../data/api/IKkalScanApi.kt` + `KkalScanApi` + `FakeKkalScanApi` | `classifyFeatureSearchIntent` |
| `shared/.../data/repository/FeatureSearchRepository.kt` | `classifyIntent` |
| `shared/.../presentation/features/FeatureSearchViewModel.kt` | Submit flow + food-intent effect |
| `shared/.../domain/features/FeatureSearchCatalog.kt` | Remove dish-name keywords from `food_search` |
| `backend` catalog mirror if present | Same keyword cleanup |
| `shared/.../analytics/AnalyticsEvents.kt` + docs | New events |
| `shared/.../onboarding/HasLoggedAnythingStorage*.kt` | expect/actual flag |
| `composeApp/.../ui/features/FeatureSearchBar.kt` | `ImeAction.Search` → `onSubmit` |
| `composeApp/.../navigation/AppRootContent.kt` | Handle food-intent → diary + hint |
| `composeApp/.../components/KkalComponents.kt` | FAB sparkles + gate |
| `composeApp/.../ui/diary/DiaryScreen.kt` or root | Transient hint banner |
| Tests under `shared/src/commonTest` (+ UI if needed) | TDD |

**Collision fix (required):** today `борщ` / `творог` are keywords for `food_search`, so submit would never reach LLM. Remove product-name keywords from `food_search`; keep navigation words (`продукт`, `каталог`, `найти`, `добавить еду`).

---

### Task 1: Backend — parse food-intent LLM JSON (pure)

**Files:**
- Create: `backend/src/test/kotlin/ru/kkalscan/integrations/openrouter/FeatureSearchIntentParserTest.kt`
- Modify: `backend/src/main/kotlin/ru/kkalscan/integrations/openrouter/OpenRouterModels.kt`

**Interfaces:**
- Produces: `FeatureSearchIntentParser.parse(content: String): Boolean`

- [ ] **Step 1: Write the failing test**

```kotlin
package ru.kkalscan.integrations.openrouter

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureSearchIntentParserTest {
    @Test
    fun parse_trueJson() {
        assertTrue(FeatureSearchIntentParser.parse("""{"isFoodIntent":true}"""))
    }

    @Test
    fun parse_falseJson() {
        assertFalse(FeatureSearchIntentParser.parse("""{"isFoodIntent":false}"""))
    }

    @Test
    fun parse_fencedMarkdown() {
        assertTrue(FeatureSearchIntentParser.parse("```json\n{\"isFoodIntent\": true}\n```"))
    }

    @Test
    fun parse_garbage_defaultsFalse() {
        assertFalse(FeatureSearchIntentParser.parse("not json"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run (from `backend/`):
```bash
./gradlew test --tests ru.kkalscan.integrations.openrouter.FeatureSearchIntentParserTest
```
Expected: FAIL — `FeatureSearchIntentParser` unresolved.

- [ ] **Step 3: Minimal implementation**

In `OpenRouterModels.kt` add:

```kotlin
@Serializable
internal data class FeatureSearchIntentEnvelope(val isFoodIntent: Boolean)

internal object FeatureSearchIntentParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(content: String): Boolean {
        val payload = VisionResponseParser.extractJsonPayloadForIntent(content.trim())
            // Prefer extracting extractJsonPayload by making it internal shared,
            // or duplicate the small extract helper used by VisionResponseParser.
        return runCatching {
            json.decodeFromString<FeatureSearchIntentEnvelope>(payload).isFoodIntent
        }.getOrDefault(false)
    }
}
```

Refactor: expose `VisionResponseParser`'s private `extractJsonPayload` as `internal fun extractJsonPayload` and reuse it (no behavior change for food/workout parsers).

Also add prompt + builder stubs used later:

```kotlin
internal object FeatureSearchIntentPrompt {
    val SYSTEM = """
        Ты классификатор запросов поиска в приложении подсчёта калорий.
        Пользователь вводит строку в поиск функций приложения (дневник, профиль, скан…).
        Верни ТОЛЬКО JSON: {"isFoodIntent":true} если человек ищет блюдо/продукт/еду чтобы записать калории,
        или {"isFoodIntent":false} если это навигация по функциям приложения, опечатка, мусор или не еда.
    """.trimIndent()

    fun userMessage(query: String): String = "Запрос поиска:\n${query.trim()}"
}

// Inside OpenRouterRequestBuilder:
fun buildFeatureSearchIntent(model: String, query: String): JsonObject = /* same shape as buildText with FeatureSearchIntentPrompt */
```

- [ ] **Step 4: Run tests — PASS**

- [ ] **Step 5: Commit (backend)**

```bash
git add src/test/kotlin/ru/kkalscan/integrations/openrouter/FeatureSearchIntentParserTest.kt \
  src/main/kotlin/ru/kkalscan/integrations/openrouter/OpenRouterModels.kt
git commit -m "feat: parse feature-search food-intent LLM JSON"
```

---

### Task 2: Backend — VisionClient.classifySearchIntent + stub

**Files:**
- Modify: `backend/src/main/kotlin/ru/kkalscan/domain/port/Services.kt` (`VisionClient`)
- Modify: `backend/src/main/kotlin/ru/kkalscan/integrations/Stubs.kt`
- Modify: `backend/src/main/kotlin/ru/kkalscan/integrations/openrouter/OpenRouterVisionClient.kt`
- Create: `backend/src/test/kotlin/ru/kkalscan/integrations/StubVisionClientIntentTest.kt`

**Interfaces:**
- Consumes: `FeatureSearchIntentParser`, `OpenRouterRequestBuilder.buildFeatureSearchIntent`
- Produces: `VisionClient.classifySearchIntent(query: String): Boolean`

- [ ] **Step 1: Failing stub test**

```kotlin
class StubVisionClientIntentTest {
    private val client = StubVisionClient()

    @Test
    fun borscht_isFood() = runBlocking {
        assertTrue(client.classifySearchIntent("борщ"))
    }

    @Test
    fun profile_isNotFood() = runBlocking {
        assertFalse(client.classifySearchIntent("профиль"))
    }

    @Test
    fun garbage_isNotFood() = runBlocking {
        assertFalse(client.classifySearchIntent("xyzunknown123"))
    }
}
```

- [ ] **Step 2: Run — FAIL** (method missing on `VisionClient`)

- [ ] **Step 3: Implement**

Add to `VisionClient`:
```kotlin
suspend fun classifySearchIntent(query: String): Boolean
```

`StubVisionClient`:
```kotlin
override suspend fun classifySearchIntent(query: String): Boolean {
    delay(STUB_LATENCY_MS)
    val n = query.trim().lowercase()
    if (n.length < 3) return false
    val featureWords = setOf("профиль", "дневник", "скан", "бжу", "клетчатка", "подписка", "pro")
    if (featureWords.any { n.contains(it) }) return false
    // Heuristic for stub: Cyrillic word-ish → food; else false
    return n.any { it in 'а'..'я' || it == 'ё' }
}
```

`OpenRouterVisionClient`: mirror `analyzeDescription` but call `buildFeatureSearchIntent` and `FeatureSearchIntentParser.parse`; on parse/HTTP failure throw `VisionUnavailableException` (service layer maps to false).

- [ ] **Step 4: Tests PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: VisionClient.classifySearchIntent stub and OpenRouter"
```

---

### Task 3: Backend — FeatureSearchService.classifyIntent + route

**Files:**
- Modify: `FeatureSearchService.kt`, `FeatureSearchServiceImpl.kt`, `Dtos.kt`, `ApiRoutes.kt`, `RouteCatalog.kt`, `AppModule.kt` (wire if needed), `AppConfig.kt`
- Create: `backend/src/test/kotlin/ru/kkalscan/domain/service/FeatureSearchIntentServiceTest.kt`
- Modify: `backend/src/test/kotlin/ru/kkalscan/routes/ApiRoutesTest.kt`

**Interfaces:**
- Produces:
  - `FeatureSearchService.classifyIntent(deviceId: UUID, query: String): FeatureSearchIntentResult`
  - `data class FeatureSearchIntentResult(val query: String, val isFoodIntent: Boolean)`
  - DTOs: `FeatureSearchIntentRequest(query: String)`, `FeatureSearchIntentResponse(query, isFoodIntent)`
  - Route: `POST /api/v1/feature-search/intent` with `device_id` header as other routes

- [ ] **Step 1: Failing service tests**

```kotlin
class FeatureSearchIntentServiceTest {
    @Test
    fun shortQuery_badRequest() = runBlocking {
        val svc = FeatureSearchServiceImpl(/* memory repos + StubVisionClient */)
        assertFailsWith<BadRequestException> {
            svc.classifyIntent(UUID.randomUUID(), "ab")
        }
    }

    @Test
    fun foodQuery_true() = runBlocking {
        val result = svc.classifyIntent(UUID.randomUUID(), "борщ")
        assertTrue(result.isFoodIntent)
    }

    @Test
    fun featureQuery_false() = runBlocking {
        assertFalse(svc.classifyIntent(UUID.randomUUID(), "профиль").isFoodIntent)
    }
}
```

- [ ] **Step 2: Run — FAIL**

- [ ] **Step 3: Implement service**

```kotlin
override suspend fun classifyIntent(deviceId: UUID, query: String): FeatureSearchIntentResult {
    val trimmed = query.trim()
    if (trimmed.length < 3) throw BadRequestException("Запрос слишком короткий")
    if (trimmed.length > 200) throw BadRequestException("Запрос слишком длинный")
    val month = YearMonth.now()
    if (visionBudgetRepository.getMonthCost(month) >= AppConfig.visionMonthlyBudgetRub) {
        return FeatureSearchIntentResult(trimmed, isFoodIntent = false)
    }
    val isFood = try {
        visionClient.classifySearchIntent(trimmed)
    } catch (e: Exception) {
        log.warn("feature_search_intent failed: {}", e.message)
        false
    }
    visionBudgetRepository.addCost(month, AppConfig.visionCostPerIntentRub) // only on successful LLM call; skip add on catch
    return FeatureSearchIntentResult(trimmed, isFood)
}
```

Charge budget **only when** OpenRouter/stub call succeeds (not on early false from budget exceeded).

Route:
```kotlin
post("/feature-search/intent") {
    val deviceId = call.parseDeviceId() ?: throw BadRequestException("device_id обязателен")
    val body = call.receive<FeatureSearchIntentRequest>()
    val result = module.featureSearchService.classifyIntent(deviceId, body.query)
    call.respond(FeatureSearchIntentResponse(query = result.query, isFoodIntent = result.isFoodIntent))
}
```

ApiRoutesTest: POST борщ → true; POST профиль → false; POST `ab` → 400.

- [ ] **Step 4: Tests PASS**

```bash
./gradlew test --tests ru.kkalscan.domain.service.FeatureSearchIntentServiceTest \
  --tests 'ru.kkalscan.routes.ApiRoutesTest.feature search intent*'
```

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: POST /feature-search/intent food classification"
```

---

### Task 4: Mobile — remove dish keywords from food_search + catalog tests

**Files:**
- Modify: `mobile/shared/src/commonMain/kotlin/ru/kkalscan/domain/features/FeatureSearchCatalog.kt`
- Modify: `backend/src/main/kotlin/ru/kkalscan/domain/features/FeatureSearchCatalog.kt` (keep catalogs aligned)
- Modify/extend: `mobile/shared/src/commonTest/kotlin/ru/kkalscan/domain/features/FeatureSearchCatalogTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
@Test
fun borscht_doesNotMatchFoodSearchFeature() {
    val result = FeatureSearchCatalog.query("борщ", limit = 10)
    // Either popularFallback or empty of food_search id — assert no food_search hit as real match
    assertTrue(result.popularFallback || result.items.none { it.id == "food_search" })
}
```

- [ ] **Step 2: Run — FAIL** (currently matches)

- [ ] **Step 3: Change keywords**

```kotlin
"food_search" to listOf("продукт", "каталог", "найти", "добавить еду"),
```

Remove `борщ`, `творог`. Mirror on backend catalog keywords string.

- [ ] **Step 4: PASS + commit both repos if both changed**

```bash
# mobile
git commit -m "fix: stop matching dish names to food_search feature"

# backend
git commit -m "fix: align feature search keywords without dish names"
```

---

### Task 5: Mobile API — classifyFeatureSearchIntent

**Files:**
- Modify: `IKkalScanApi.kt`, `KkalScanApi.kt`, `FakeKkalScanApi.kt`, `StatefulDiaryApi.kt` (if implements API)
- Modify: `FeatureSearchRepository.kt`
- Create/extend model: `FeatureSearchIntentResult(query: String, isFoodIntent: Boolean)` in `domain/model`
- Test: extend Fake usage in repository test or ViewModel tests in Task 6

- [ ] **Step 1: Failing compile via repository test**

```kotlin
@Test
fun classifyIntent_delegatesToApi() = runTest {
    val api = FakeKkalScanApi()
    val storage = InMemoryDeviceIdStorage().apply { setDeviceId("d1") }
    val repo = FeatureSearchRepository(api, storage)
    val result = repo.classifyIntent("борщ")
    result.isFoodIntent shouldBe true
}
```

Fake rules (mirror stub):
- length < 3 → could throw or return false (client validates before call)
- contains feature words → false
- Cyrillic → true

- [ ] **Step 2: FAIL**

- [ ] **Step 3: Implement API methods**

```kotlin
// IKkalScanApi
suspend fun classifyFeatureSearchIntent(deviceId: String, query: String): FeatureSearchIntentResult

// KkalScanApi — POST "$base/feature-search/intent" with device_id header + JSON body
```

Repository:
```kotlin
suspend fun classifyIntent(query: String): FeatureSearchIntentResult {
    val deviceId = deviceIdStorage.getDeviceId()
    return api.classifyFeatureSearchIntent(deviceId, query)
}
```

Update `IFeatureSearchRepository` accordingly.

- [ ] **Step 4: PASS + commit**

```bash
git commit -m "feat: client API for feature-search food intent"
```

---

### Task 6: Mobile — FeatureSearchViewModel submit + food intent effect

**Files:**
- Modify: `FeatureSearchViewModel.kt`
- Modify: `FeatureSearchViewModelTest.kt`
- Modify: `App.kt` analytics wiring
- Modify: `AnalyticsEvents.kt`, `mobile/docs/appmetrica-events.md`, `AnalyticsEventsTest.kt`

**Interfaces:**
- Produces:
  - `fun onSubmit()`
  - `FeatureSearchUiState.foodIntentRedirect: Boolean` (one-shot) **or** callback `onFoodIntent: (query: String) -> Unit`
  - Prefer callback injected like `onSearchCompleted` to avoid sticky state:

```kotlin
typealias FeatureSearchFoodIntentListener = (queryLength: Int) -> Unit

class FeatureSearchViewModel(
    ...,
    private val onFoodIntent: FeatureSearchFoodIntentListener = {},
    private val onFoodIntentAnalytics: (queryLength: Int, isFood: Boolean) -> Unit = { _, _ -> },
)
```

Behavior:
- `onQueryChange`: update `query` only; cancel pending jobs; **do not** search (breaking change vs today — update existing tests to call `onSubmit()`).
- `onSubmit`:
  1. trim; if blank → clear results
  2. set `isSearching = true`
  3. `repo.search(trimmed)`
  4. if `!popularFallback && items.isNotEmpty()` → show results; analytics query; done
  5. else if length ≥ 3 → `repo.classifyIntent(trimmed)`; analytics food intent; if true → `onFoodIntent(length)` + clear query; else show popular/empty as search returned
  6. on classify error → show popular/empty, analytics `is_food=false`

- [ ] **Step 1: Rewrite/add failing tests**

```kotlin
@Test
fun typing_doesNotSearchUntilSubmit() = runTest {
    val vm = createViewModel(this)
    vm.onQueryChange("профиль")
    advanceUntilIdle()
    vm.state.value.results shouldBe emptyList()
}

@Test
fun submitProfile_showsFeatureResults() = runTest {
    val vm = createViewModel(this)
    vm.onQueryChange("профиль")
    vm.onSubmit()
    advanceUntilIdle()
    vm.state.value.showPopular shouldBe false
    vm.state.value.results.shouldNotBeEmpty()
}

@Test
fun submitBorscht_triggersFoodIntentCallback() = runTest {
    var foodCalls = 0
    val vm = createViewModel(this, onFoodIntent = { foodCalls++ })
    vm.onQueryChange("борщ")
    vm.onSubmit()
    advanceUntilIdle()
    foodCalls shouldBe 1
    vm.state.value.query shouldBe ""
}

@Test
fun submitUnknown_showsPopularWithoutFoodIntent() = runTest {
    var foodCalls = 0
    val vm = createViewModel(this, onFoodIntent = { foodCalls++ })
    vm.onQueryChange("xyzunknown123")
    vm.onSubmit()
    advanceUntilIdle()
    foodCalls shouldBe 0
    assertTrue(vm.state.value.showPopular)
}
```

Note: Fake must return `isFoodIntent=false` for `xyzunknown123` and `true` for `борщ`.

- [ ] **Step 2: FAIL**

- [ ] **Step 3: Implement ViewModel + update all old tests to use `onSubmit`**

Analytics in `App.kt`:
```kotlin
onFoodIntentAnalytics = { len, isFood ->
    KkalAnalytics.reportAction(
        AnalyticsEvents.FEATURE_SEARCH_FOOD_INTENT,
        mapOf("query_length" to len.toString(), "is_food" to isFood.toString()),
    )
}
```

Add `FEATURE_SEARCH_FOOD_INTENT = "feature_search_food_intent"` to events set + docs.

- [ ] **Step 4: PASS**

```bash
cd mobile && ./gradlew :shared:cleanTestDebugUnitTest :shared:testDebugUnitTest --tests 'ru.kkalscan.presentation.features.FeatureSearchViewModelTest'
```
(Adjust Gradle task name to the project’s usual shared test target.)

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: feature search submit classifies food intent"
```

---

### Task 7: Mobile UI — IME Search + diary hint on food intent

**Files:**
- Modify: `FeatureSearchBar.kt`
- Modify: `AppRootContent.kt` (and/or `DiaryScreen.kt`)
- Create optional: small `KkalInfoBanner` / reuse Surface text for hint
- Test: update `DiaryFabUiTest` only if needed; prefer ViewModel coverage already done. Add compose test only if project already tests FeatureSearchBar.

- [ ] **Step 1: Failing UI/unit contract** — if no compose test harness for search, assert via a tiny pure helper or skip to implementation after ViewModel green; otherwise:

Wire:
```kotlin
OutlinedTextField(
    ...
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { viewModel.onSubmit() }),
)
```

`FeatureSearchBar(..., onFoodIntentNavigate: () -> Unit)` — actually food intent is handled in App via ViewModel listener set in `App.kt` / `AppRootContent`:

```kotlin
// when creating VM
onFoodIntent = {
    selectedTab = AppTab.Today
    diaryFoodHint = "Опишите блюдо текстом или сфотографируйте — кнопка +"
}
```

Show hint above diary content for ~5s (`LaunchedEffect` clear).

- [ ] **Step 2–4: Implement, manual smoke, commit**

```bash
git commit -m "feat: search IME submit opens diary hint on food intent"
```

---

### Task 8: HasLoggedAnything storage (expect/actual)

**Files:**
- Create: `shared/.../onboarding/HasLoggedAnythingStorage.kt` (interface + expect factory)
- Create: android / jvm / wasmJs actuals (mirror `LaunchRetentionStorageFactory` prefs pattern)
- Create: `InMemoryHasLoggedAnythingStorage` for tests
- Create: `HasLoggedAnythingStorageTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
@Test
fun defaultsFalse_thenPersistsTrue() {
    val store = InMemoryHasLoggedAnythingStorage()
    store.hasLoggedAnything() shouldBe false
    store.markLoggedAnything()
    store.hasLoggedAnything() shouldBe true
}
```

- [ ] **Step 2: FAIL**

- [ ] **Step 3: Implement**

```kotlin
interface HasLoggedAnythingStorage {
    fun hasLoggedAnything(): Boolean
    fun markLoggedAnything()
}
```

Android: SharedPreferences key `has_logged_anything`.

- [ ] **Step 4: PASS + commit**

```bash
git commit -m "feat: persist hasLoggedAnything for FAB attention gate"
```

---

### Task 9: Mark flag on successful food / workout add

**Files:**
- Modify: diary add success path + workout add success path (ViewModel or `AppRootContent` analytics hooks — wherever `ADD_TO_DIARY` / workout success is confirmed)
- Test: ViewModel/integration test that success calls `markLoggedAnything`

Find existing success callbacks (e.g. after `addEntry` / `addWorkout` in `DiaryViewModel`) and inject storage.

- [ ] **Step 1: Failing test** on DiaryViewModel or a small `FirstLogTracker` helper:

```kotlin
class FirstLogTracker(private val storage: HasLoggedAnythingStorage) {
    fun onFoodOrWorkoutLogged() = storage.markLoggedAnything()
}
```

Prefer thin helper tested in isolation, then call from diary VM success paths.

- [ ] **Step 2–4: Implement wire-up + commit**

```bash
git commit -m "feat: mark hasLoggedAnything after first food or workout"
```

---

### Task 10: FAB sparkle attention animation

**Files:**
- Modify: `KkalComponents.kt` (FAB block ~206–310)
- Create: `composeApp/.../components/FabAttentionOverlay.kt` (stars + pulse)
- Create: `shared/.../onboarding/FabAttentionScheduler.kt` (pure timing logic — easy TDD)
- Analytics: `FAB_ATTENTION_SHOWN = "fab_attention_shown"`
- Test: `FabAttentionSchedulerTest.kt`

**Interfaces:**

```kotlin
class FabAttentionScheduler(
    private val intervalMs: Long = 60_000,
    private val clock: () -> Long,
) {
    fun shouldShow(nowMs: Long, hasLoggedAnything: Boolean, fabExpanded: Boolean, loading: Boolean, lastShownMs: Long?): Boolean
}
```

Rules: false if logged / expanded / loading; true if `lastShownMs == null` or `now - lastShownMs >= intervalMs`.

- [ ] **Step 1: Failing scheduler tests** (interval, gating)

- [ ] **Step 2: FAIL**

- [ ] **Step 3: Implement scheduler + Compose**

In FAB composable:
- `LaunchedEffect(Unit)` loop: delay 1s tick; if `shouldShow` → play animation 1.2s, set lastShown, report analytics
- Overlay: 4–6 small star Icons with offset animation around FAB (Canvas or multiple `Icon` with `animateFloatAsState` / `Animatable`)
- Pulse: scale FAB slightly 1.0 → 1.08 → 1.0

Pass `hasLoggedAnything` from storage remembered at composition + updated when marked.

- [ ] **Step 4: Unit tests PASS; optional instrumented smoke**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: FAB sparkle attention until first diary log"
```

---

### Task 11: Docs + analytics registry sync

**Files:**
- `mobile/docs/appmetrica-events.md`
- `AnalyticsEvents.allImplemented`
- `AnalyticsEventsTest`
- Backend `RouteCatalog` / `docs/methods.md` if present

- [ ] Document `feature_search_food_intent`, `fab_attention_shown`
- [ ] Commit

```bash
git commit -m "docs: analytics events for food intent and FAB attention"
```

---

## Spec coverage checklist

| Spec requirement | Task |
|------------------|------|
| POST intent endpoint + LLM | 1–3 |
| No scan quota; fail → false | 3 |
| Submit-only LLM; ≥3 chars; no real feature match | 6 |
| Diary + exact snackbar copy | 7 |
| Remove dish keyword collision | 4 |
| FAB stars ≤1/min; flag after first food/workout | 8–10 |
| Analytics without raw query | 6, 11 |
| TDD | every task |

## Placeholder / consistency review

- Method name locked: `classifySearchIntent` (VisionClient) / `classifyIntent` (FeatureSearchService/Repository) / `classifyFeatureSearchIntent` (API).
- Response field locked: `isFoodIntent`.
- Snackbar string locked exactly as in Global Constraints.

---

## Execution handoff

Plan saved to `mobile/docs/superpowers/plans/2026-07-16-feature-search-food-intent-fab-attention.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
2. **Inline Execution** — execute tasks in this session with checkpoints  

Which approach?
