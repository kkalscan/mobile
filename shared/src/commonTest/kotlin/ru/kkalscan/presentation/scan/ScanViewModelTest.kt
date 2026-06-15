package ru.kkalscan.presentation.scan

import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.ScanRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import kotlin.test.Test

class ScanViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun scanPhoto_setsResult() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.scanPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        vm.state.value.isLoading shouldBe false
        vm.state.value.result.shouldNotBeNull()
        vm.state.value.result!!.totalKcal shouldBe 350
    }

    @Test
    fun adjustDishGrams_updatesTotals() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.scanPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        vm.adjustDishGrams(0, -100)
        vm.state.value.result!!.dishes.single().grams shouldBe 100
        vm.state.value.result!!.totalKcal shouldBe 175
    }

    @Test
    fun scaleDishFromBaseline_doublesPortion() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.scanPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        vm.scaleDishFromBaseline(0, 2.0)
        vm.state.value.result!!.dishes.single().grams shouldBe 400
        vm.state.value.result!!.totalKcal shouldBe 700
    }

    @Test
    fun scaleDishFromBaseline_halvesPortion() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.scanPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        vm.scaleDishFromBaseline(0, 0.5)
        vm.state.value.result!!.dishes.single().grams shouldBe 100
        vm.state.value.result!!.totalKcal shouldBe 175
        vm.state.value.result!!.totalProtein shouldBe 7.5
    }

    @Test
    fun adjustDishGrams_incrementsAndUpdatesMacros() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.scanPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        vm.adjustDishGrams(0, 50)
        vm.state.value.result!!.dishes.single().grams shouldBe 250
        vm.state.value.result!!.totalKcal shouldBe 438
    }

    @Test
    fun removeDish_clearsTotals() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.scanPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()
        vm.removeDish(0)

        vm.state.value.result!!.dishes shouldBe emptyList()
        vm.state.value.result!!.totalKcal shouldBe 0
    }

    @Test
    fun grantAdBonus_updatesScansLeft() = runTest(dispatcher) {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val vm = ScanViewModel(
            ScanRepository(TestApiFixtures.api(), storage),
            DiaryRepository(TestApiFixtures.api(), storage, { TestApiFixtures.TODAY }),
            this,
        )

        vm.grantAdBonus()
        advanceUntilIdle()

        vm.state.value.scansLeft shouldBe 5
    }
}
