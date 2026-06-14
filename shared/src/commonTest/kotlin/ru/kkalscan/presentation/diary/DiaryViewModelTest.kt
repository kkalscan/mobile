package ru.kkalscan.presentation.diary

import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.runTest
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import kotlin.test.Test

class DiaryViewModelTest {

    @Test
    fun refresh_loadsDiary() = runTest {
        val repo = DiaryRepository(
            TestApiFixtures.api(),
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val vm = DiaryViewModel(repo, this)
        vm.refresh()

        vm.state.value.isLoading shouldBe false
        vm.state.value.day.shouldNotBeNull()
        vm.state.value.day!!.totalKcal shouldBe 350
    }

    @Test
    fun deleteEntry_triggersRefresh() = runTest {
        val repo = DiaryRepository(
            TestApiFixtures.api(),
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val vm = DiaryViewModel(repo, this)
        vm.refresh()
        vm.deleteEntry("entry-1")

        vm.state.value.errorMessage shouldBe null
    }
}
