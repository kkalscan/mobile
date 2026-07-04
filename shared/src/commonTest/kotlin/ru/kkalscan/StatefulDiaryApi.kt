package ru.kkalscan

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.domain.features.FeatureSearchCatalog
import ru.kkalscan.domain.food.LocalFoodCatalog
import ru.kkalscan.domain.model.FeatureSearchResult
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.FoodSearchResult
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionStatus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory API fake with per-device diary state (mirrors backend keying by device_id).
 */
class StatefulDiaryApi(
    private val diaryDate: String = TestApiFixtures.TODAY,
) : IKkalScanApi {

    private val entriesByDevice = ConcurrentHashMap<String, MutableList<DiaryEntry>>()
    private val scansById = ConcurrentHashMap<String, ScanResult>()
    private var scanCounter = 0

    override suspend fun scanPhoto(deviceId: String, photoBytes: ByteArray, timezoneOffsetMinutes: Int): ScanResult {
        scanCounter++
        val dish = Dish(
            name = "Блюдо $scanCounter",
            grams = 200,
            kcal = 100 * scanCounter,
            protein = 10.0,
            fat = 5.0,
            carbs = 20.0,
            fiber = 4.0,
        )
        val scanId = UUID.randomUUID().toString()
        val result = ScanResult(
            scanId = scanId,
            dishes = listOf(dish),
            totalKcal = dish.kcal,
            totalProtein = dish.protein,
            totalFat = dish.fat,
            totalCarbs = dish.carbs,
            totalFiber = dish.fiber,
            scansLeft = 3,
            isPro = false,
        )
        scansById[scanId] = result
        return result
    }

    override suspend fun describeFood(
        deviceId: String,
        description: String,
        timezoneOffsetMinutes: Int,
    ): ScanResult {
        scanCounter++
        val normalized = description.trim().lowercase()
        val dish = if (normalized.contains("борщ")) {
            Dish(
                name = "Борщ с говядиной",
                grams = 300,
                kcal = 250,
                protein = 12.0,
                fat = 8.0,
                carbs = 22.0,
                fiber = 5.5,
            )
        } else {
            Dish(
                name = "Блюдо $scanCounter (описание)",
                grams = 200,
                kcal = 100 * scanCounter,
                protein = 10.0,
                fat = 5.0,
                carbs = 20.0,
                fiber = 4.0,
            )
        }
        val scanId = UUID.randomUUID().toString()
        val result = ScanResult(
            scanId = scanId,
            dishes = listOf(dish),
            totalKcal = dish.kcal,
            totalProtein = dish.protein,
            totalFat = dish.fat,
            totalCarbs = dish.carbs,
            totalFiber = dish.fiber,
            scansLeft = 3,
            isPro = false,
        )
        scansById[scanId] = result
        return result
    }

    override suspend fun grantScanBonus(deviceId: String): ScanBonusResult =
        ScanBonusResult(scansLeft = 5, bonusGranted = true)

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay {
        val entries = if (date == diaryDate) {
            entriesByDevice[deviceId].orEmpty()
        } else {
            emptyList()
        }
        return DiaryDay(
            date = date,
            totalKcal = entries.sumOf { it.totalKcal },
            scansLeft = (3 - entries.size).coerceAtLeast(0),
            isPro = false,
            entries = entries,
        )
    }

    override suspend fun addDiaryEntry(
        deviceId: String,
        mealType: MealType,
        scanId: String?,
        dishes: List<Dish>?,
    ): CreateDiaryEntryResponse {
        val savedDishes = dishes ?: scanId?.let { scansById[it]?.dishes } ?: error("scan_id не найден")
        val entry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            createdAt = "${diaryDate}T12:00:00Z",
            mealType = mealType,
            totalKcal = savedDishes.sumOf { it.kcal },
            dishes = savedDishes,
        )
        entriesByDevice.computeIfAbsent(deviceId) { mutableListOf() }.add(entry)
        val left = (3 - entriesByDevice[deviceId]!!.size).coerceAtLeast(0)
        return CreateDiaryEntryResponse(entry = entry, scansLeft = left)
    }

    override suspend fun deleteDiaryEntry(deviceId: String, entryId: String) {
        entriesByDevice[deviceId]?.removeAll { it.id == entryId }
    }

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus =
        SubscriptionStatus(isPro = false, accountLinked = false)

    override suspend fun startProSubscription(deviceId: String, tariff: String): ProSubscriptionStart =
        ProSubscriptionStart(
            isPro = true,
            proUntil = "${diaryDate}T12:00:00Z",
            tariff = tariff,
            paymentRequired = false,
            message = "Pro активирован",
        )

    override suspend fun searchFood(
        deviceId: String,
        query: String,
        limit: Int,
        source: String,
    ): FoodSearchResult {
        val trimmed = query.trim()
        val items = LocalFoodCatalog.search(trimmed, limit)
        return FoodSearchResult(query = trimmed, items = items, total = items.size)
    }

    override suspend fun searchFeatures(
        deviceId: String,
        query: String,
        limit: Int,
        locale: String,
    ): FeatureSearchResult {
        val trimmed = query.trim()
        val outcome = FeatureSearchCatalog.query(trimmed, limit)
        return FeatureSearchResult(
            query = trimmed,
            items = outcome.items,
            total = outcome.items.size,
            popularFallback = outcome.popularFallback,
        )
    }

    override suspend fun submitBugReport(
        deviceId: String,
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ): BugReportResult =
        BugReportResult(
            reportId = UUID.randomUUID().toString(),
            isPro = true,
            proUntil = "${diaryDate}T12:00:00Z",
            message = "Спасибо! Pro на месяц активирован.",
        )
}
