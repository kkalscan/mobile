package ru.kkalscan.data.api

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.stats.StatsAggregator
import ru.kkalscan.stats.WeekDates
import kotlin.test.Test

class FakeKkalScanApiTest {

    private val deviceId = TestApiFixtures.DEVICE_ID
    private val today = "2026-06-11"
    private val weekStart = WeekDates.iso(WeekDates.mondayOf(WeekDates.parse(today)))

    @Test
    fun scanPhoto_returnsFiberInDishes() = runTest {
        val api = FakeKkalScanApi(todayProvider = { today })
        val scan = api.scanPhoto(deviceId, byteArrayOf(1, 2, 3), timezoneOffsetMinutes = 180)

        scan.totalFiber shouldNotBe 0.0
        scan.dishes.single().fiber shouldNotBe 0.0
    }

    @Test
    fun addDiaryEntry_persistsFiber() = runTest {
        val api = FakeKkalScanApi(todayProvider = { today })
        val scan = api.scanPhoto(deviceId, byteArrayOf(5), timezoneOffsetMinutes = 180)
        api.addDiaryEntry(deviceId, ru.kkalscan.domain.model.MealType.lunch, scan.scanId, null)

        val day = api.getDiary(deviceId, today, timezoneOffsetMinutes = 180)
        day.entries shouldHaveSize 1
        day.entries.single().dishes.single().fiber shouldBe scan.dishes.single().fiber
    }

    @Test
    fun seedSampleWeek_populatesFiberForJournal() = runTest {
        val api = FakeKkalScanApi(seedSampleWeek = true, todayProvider = { today })
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(deviceId) },
            todayProvider = { today },
        )
        val days = repo.getWeek(weekStart)
        val stats = StatsAggregator.weekStats(days, weekStart)

        stats.daysWithData shouldBe 5
        stats.avgFiber shouldNotBe 0.0
        stats.days.count { it.fiber > 0 } shouldBe 5
    }

    @Test
    fun describeFood_recognizesBorscht() = runTest {
        val api = FakeKkalScanApi(todayProvider = { today })
        val scan = api.describeFood(deviceId, "тарелка борща", timezoneOffsetMinutes = 180)

        scan.dishes.single().name shouldBe "Борщ с говядиной"
        scan.totalKcal shouldBe 250
    }

    @Test
    fun describeFood_addToDiary_persistsEntry() = runTest {
        val api = FakeKkalScanApi(todayProvider = { today })
        val scan = api.describeFood(deviceId, "тарелка борща", timezoneOffsetMinutes = 180)
        api.addDiaryEntry(deviceId, ru.kkalscan.domain.model.MealType.lunch, scan.scanId, null)

        val day = api.getDiary(deviceId, today, timezoneOffsetMinutes = 180)
        day.entries shouldHaveSize 1
        day.entries.single().dishes.single().name shouldBe "Борщ с говядиной"
        day.totalKcal shouldBe 250
        day.entries.single().mealType shouldBe ru.kkalscan.domain.model.MealType.lunch
    }

    @Test
    fun searchFeatures_findsProfileAndDeeplink() = runTest {
        val api = FakeKkalScanApi(todayProvider = { today })
        val result = api.searchFeatures(deviceId, "профиль", limit = 10, locale = "ru")

        result.items.any { it.deeplink == "kkalscan://profile" } shouldBe true
        result.items.any { it.title.contains("Профиль", ignoreCase = true) } shouldBe true
    }
}
