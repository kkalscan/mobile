package ru.kkalscan.presentation.features

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
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
    fun searchProfile_returnsProfileDeeplink() = runTest {
        val vm = createViewModel(this)
        vm.onQueryChange("профиль")
        advanceUntilIdle()

        val results = vm.state.value.results
        results.shouldNotBeEmpty()
        vm.state.value.showPopular shouldBe false
        val profile = results.first { it.deeplink == "kkalscan://profile" }
        resolveDeepLinkNavigation(profile.deeplink)!!.screen shouldBe DeepLinkScreen.Profile
    }

    @Test
    fun emptyQuery_returnsNoItems() = runTest {
        val vm = createViewModel(this)
        vm.onQueryChange("")
        advanceUntilIdle()

        vm.state.value.results shouldBe emptyList()
        vm.state.value.showPopular shouldBe false
        vm.state.value.isSearching shouldBe false
    }

    @Test
    fun emptyQuery_reportsAnalytics() = runTest {
        val tracked = mutableListOf<Pair<String, Int>>()
        val vm = createViewModel(this) { query, count -> tracked += query to count }
        vm.onQueryChange("")
        advanceUntilIdle()

        tracked shouldBe listOf("" to 0)
    }

    @Test
    fun unknownQuery_showsPopularFallback() = runTest {
        val vm = createViewModel(this)
        vm.onQueryChange("xyzunknown123")
        advanceUntilIdle()

        assertTrue(vm.state.value.showPopular)
        assertTrue(vm.state.value.results.isNotEmpty())
    }

    @Test
    fun clear_resetsState() = runTest {
        val vm = createViewModel(this)
        vm.onQueryChange("профиль")
        advanceUntilIdle()
        vm.clear()

        vm.state.value.query shouldBe ""
        vm.state.value.results shouldBe emptyList()
        vm.state.value.showPopular shouldBe false
    }

    private fun createViewModel(
        scope: CoroutineScope,
        onSearchCompleted: (String, Int) -> Unit = { _, _ -> },
    ): FeatureSearchViewModel {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(deviceId) }
        val repo = FeatureSearchRepository(FakeKkalScanApi(), storage)
        return FeatureSearchViewModel(repo, scope, onSearchCompleted)
    }
}
