package ru.kkalscan

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.domain.features.FeatureSearchCatalog
import ru.kkalscan.domain.food.LocalFoodCatalog
import ru.kkalscan.domain.model.FeatureSearchResult
import ru.kkalscan.domain.activity.ActivityEmulatorTimeProration
import ru.kkalscan.domain.activity.StepCalorieEstimator
import ru.kkalscan.domain.model.ActivityEmulator
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.CreateWorkoutResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.FoodSearchResult
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.PromoApplyResult
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionOffer
import ru.kkalscan.domain.model.SubscriptionOffers
import ru.kkalscan.domain.model.SubscriptionStatus
import ru.kkalscan.domain.model.WorkoutEntry
import ru.kkalscan.domain.model.WorkoutParseResult
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.activity.wireName
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory API fake with per-device diary state (mirrors backend keying by device_id).
 */
class StatefulDiaryApi(
    private val diaryDate: String = TestApiFixtures.TODAY,
    private val todayProvider: () -> String = { diaryDate },
) : IKkalScanApi {

    private val entriesByKey = ConcurrentHashMap<String, MutableList<DiaryEntry>>()
    private val workoutsByKey = ConcurrentHashMap<String, MutableList<WorkoutEntry>>()
    private val activityByKey = ConcurrentHashMap<String, ActivitySnapshot>()
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

    override suspend fun parseWorkout(deviceId: String, description: String): WorkoutParseResult {
        val normalized = description.trim().lowercase()
        val minutes = Regex("(\\d+)\\s*мин").find(normalized)?.groupValues?.get(1)?.toIntOrNull() ?: 30
        val title = when {
            normalized.contains("бег") -> "Бег"
            else -> "Тренировка"
        }
        return WorkoutParseResult(title = title, burnedKcal = 300, durationMinutes = minutes)
    }

    override suspend fun grantScanBonus(deviceId: String): ScanBonusResult =
        ScanBonusResult(scansLeft = 5, bonusGranted = true)

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay {
        val entries = entriesByKey[key(deviceId, date)].orEmpty()
        val workouts = workoutsByKey[key(deviceId, date)].orEmpty()
        val consumed = entries.sumOf { it.totalKcal }
        val workoutBurned = workouts.sumOf { it.kcal }
        val activity = activityByKey[key(deviceId, date)]
        val activityBurned = activity?.kcal ?: 0
        val burned = workoutBurned + activityBurned
        return DiaryDay(
            date = date,
            totalKcal = consumed,
            totalBurnedKcal = burned,
            netKcal = consumed - burned,
            activityKcal = activityBurned,
            activitySteps = activity?.steps?.takeIf { it > 0 },
            activitySource = activity?.source,
            scansLeft = (3 - entries.size).coerceAtLeast(0),
            isPro = false,
            entries = entries,
            workouts = workouts,
        )
    }

    override suspend fun getActivityEmulator(deviceId: String, timezoneOffsetMinutes: Int): ActivityEmulator {
        val date = todayProvider()
        val entries = entriesByKey[key(deviceId, date)].orEmpty()
        val consumed = entries.sumOf { it.totalKcal }
        if (consumed <= 0) {
            val active = ActivityEmulatorTimeProration.prorateForDaylight(
                ActivityEmulatorTimeProration.FULL_DAYLIGHT_ACTIVE_KCAL,
                timezoneOffsetMinutes,
            )
            return ActivityEmulator(
                mode = "population_default",
                estimatedActiveKcal = active,
                estimatedSteps = StepCalorieEstimator.stepsForKcal(active),
            )
        }
        val fullDayActive = (consumed - 1500).coerceIn(100, 800)
        val active = ActivityEmulatorTimeProration.prorateForDaylight(fullDayActive, timezoneOffsetMinutes)
        return ActivityEmulator(
            mode = "diary_based",
            estimatedActiveKcal = active,
            estimatedSteps = ru.kkalscan.domain.activity.StepCalorieEstimator.stepsForKcal(active),
            avgConsumedKcalPerDay = consumed,
            diaryDaysWithEntries = 1,
        )
    }

    override suspend fun addDiaryEntry(
        deviceId: String,
        mealType: MealType,
        scanId: String?,
        dishes: List<Dish>?,
    ): CreateDiaryEntryResponse {
        val savedDishes = dishes ?: scanId?.let { scansById[it]?.dishes } ?: error("scan_id не найден")
        val date = todayProvider()
        val entry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            createdAt = "${date}T12:00:00Z",
            mealType = mealType,
            totalKcal = savedDishes.sumOf { it.kcal },
            dishes = savedDishes,
        )
        entriesByKey.computeIfAbsent(key(deviceId, date)) { mutableListOf() }.add(entry)
        val left = (3 - entriesByKey[key(deviceId, date)]!!.size).coerceAtLeast(0)
        return CreateDiaryEntryResponse(entry = entry, scansLeft = left)
    }

    override suspend fun deleteDiaryEntry(deviceId: String, entryId: String) {
        entriesByKey.values.forEach { list -> list.removeAll { it.id == entryId } }
    }

    override suspend fun addWorkout(deviceId: String, name: String, kcal: Int): CreateWorkoutResponse {
        val date = todayProvider()
        val workout = WorkoutEntry(
            id = UUID.randomUUID().toString(),
            createdAt = "${date}T12:00:00Z",
            name = name.trim(),
            kcal = kcal,
        )
        workoutsByKey.computeIfAbsent(key(deviceId, date)) { mutableListOf() }.add(workout)
        return CreateWorkoutResponse(workout = workout)
    }

    override suspend fun deleteWorkout(deviceId: String, workoutId: String) {
        workoutsByKey.values.forEach { list -> list.removeAll { it.id == workoutId } }
    }

    override suspend fun syncActivity(
        deviceId: String,
        steps: Int,
        kcal: Int,
        source: ActivitySource,
        timezoneOffsetMinutes: Int,
    ): DiaryDay {
        val date = todayProvider()
        val existing = activityByKey[key(deviceId, date)]
        if (existing != null && kcal <= existing.kcal && steps <= existing.steps) {
            return getDiary(deviceId, date, timezoneOffsetMinutes)
        }
        activityByKey[key(deviceId, date)] = ActivitySnapshot(
            steps = steps,
            kcal = kcal,
            source = source.wireName(),
        )
        return getDiary(deviceId, date, timezoneOffsetMinutes)
    }

    private fun key(deviceId: String, date: String) = "$deviceId|$date"

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus =
        SubscriptionStatus(isPro = false, accountLinked = false)

    override suspend fun getSubscriptionOffers(deviceId: String): SubscriptionOffers {
        val bound = promoByDevice[deviceId]
        val discount = bound?.discountPercent ?: 0
        return SubscriptionOffers(
            offers = listOf(
                offer("pro_monthly_199", "KkalScan Pro — месяц", 200, discount, bound?.promoCode),
                offer("pro_lifetime_5000", "KkalScan Pro — навсегда", 5000, discount, bound?.promoCode),
            ),
        )
    }

    override suspend fun applyPromo(deviceId: String, promoCode: String): PromoApplyResult {
        val promo = if (promoCode.trim().equals("Lida", ignoreCase = true)) {
            PromoApplyResult(promoCode = "Lida", discountPercent = 50)
        } else {
            throw ru.kkalscan.domain.error.KkalScanException.Api("Неверный промокод")
        }
        promoByDevice[deviceId] = promo
        return promo
    }

    override suspend fun startProSubscription(deviceId: String, tariff: String): ProSubscriptionStart =
        ProSubscriptionStart(
            isPro = true,
            proUntil = "${diaryDate}T12:00:00Z",
            tariff = tariff,
            paymentRequired = false,
            message = "Pro активирован",
        )

    private fun offer(
        tariff: String,
        title: String,
        priceRub: Int,
        discount: Int,
        promoCode: String?,
    ): SubscriptionOffer {
        val amountRub = priceRub - (priceRub * discount / 100)
        return SubscriptionOffer(
            tariff = tariff,
            title = title,
            priceRub = priceRub,
            amountRub = amountRub,
            amountKopecks = amountRub * 100,
            discountPercent = discount,
            promoCode = promoCode?.takeIf { discount > 0 },
        )
    }

    private val promoByDevice = ConcurrentHashMap<String, PromoApplyResult>()

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

    private data class ActivitySnapshot(
        val steps: Int,
        val kcal: Int,
        val source: String,
    )
}
