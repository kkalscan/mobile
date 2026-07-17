package ru.kkalscan.presentation.features

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.repository.FeatureSearchRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.navigation.DeepLinkScreen
import ru.kkalscan.navigation.resolveDeepLinkNavigation
import kotlin.test.Test
import kotlin.test.assertTrue

class FeatureSearchViewModelTest {

    private val deviceId = "test-device-feature-search"

    @Test
    fun typing_doesNotSearchUntilSubmit() = runTest {
        val vm = createViewModel()
        vm.onQueryChange("профиль")
        advanceUntilIdle()

        vm.state.value.results shouldBe emptyList()
        vm.state.value.isSearching shouldBe false
    }

    @Test
    fun submitProfile_showsFeatureResults() = runTest {
        val vm = createViewModel()
        vm.onQueryChange("профиль")
        vm.onSubmit()
        advanceUntilIdle()

        val results = vm.state.value.results
        results.shouldNotBeEmpty()
        vm.state.value.showPopular shouldBe false
        val profile = results.first { it.deeplink == "kkalscan://profile" }
        resolveDeepLinkNavigation(profile.deeplink)!!.screen shouldBe DeepLinkScreen.Profile
    }

    @Test
    fun emptySubmit_returnsNoItems() = runTest {
        val vm = createViewModel()
        vm.onQueryChange("")
        vm.onSubmit()
        advanceUntilIdle()

        vm.state.value.results shouldBe emptyList()
        vm.state.value.showPopular shouldBe false
        vm.state.value.isSearching shouldBe false
    }

    @Test
    fun emptySubmit_reportsAnalytics() = runTest {
        val tracked = mutableListOf<Pair<String, Int>>()
        val vm = createViewModel(
            onSearchCompleted = { query, count -> tracked += query to count },
        )
        vm.onQueryChange("")
        vm.onSubmit()
        advanceUntilIdle()

        tracked shouldBe listOf("" to 0)
    }

    @Test
    fun submitUnknown_showsPopularWithoutFoodIntent() = runTest {
        var analyticsIsFood: Boolean? = null
        val vm = createViewModel(
            onFoodIntentAnalytics = { _, isFood -> analyticsIsFood = isFood },
        )
        val foodEvent = async { vm.foodIntentEvents.first() }
        vm.onQueryChange("xyzunknown123")
        vm.onSubmit()
        advanceUntilIdle()

        analyticsIsFood shouldBe false
        assertTrue(vm.state.value.showPopular)
        assertTrue(vm.state.value.results.isNotEmpty())
        foodEvent.isCompleted shouldBe false
        foodEvent.cancel()
    }

    @Test
    fun submitBorscht_triggersFoodIntentCallback() = runTest {
        var analyticsIsFood: Boolean? = null
        val vm = createViewModel(
            onFoodIntentAnalytics = { _, isFood -> analyticsIsFood = isFood },
        )
        val foodEvent = async { vm.foodIntentEvents.first() }
        vm.onQueryChange("борщ")
        vm.onSubmit()
        advanceUntilIdle()

        foodEvent.await() shouldBe "борщ"
        analyticsIsFood shouldBe true
        vm.state.value.query shouldBe ""
    }

    @Test
    fun clear_resetsState() = runTest {
        val vm = createViewModel()
        vm.onQueryChange("профиль")
        vm.onSubmit()
        advanceUntilIdle()
        vm.clear()

        vm.state.value.query shouldBe ""
        vm.state.value.results shouldBe emptyList()
        vm.state.value.showPopular shouldBe false
    }

    private fun TestScope.createViewModel(
        onSearchCompleted: (String, Int) -> Unit = { _, _ -> },
        onFoodIntentAnalytics: FeatureSearchFoodIntentAnalytics = { _, _ -> },
    ): FeatureSearchViewModel {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(deviceId) }
        val repo = FeatureSearchRepository(FakeKkalScanApi(), storage)
        return FeatureSearchViewModel(repo, this, onSearchCompleted, onFoodIntentAnalytics)
    }
}
