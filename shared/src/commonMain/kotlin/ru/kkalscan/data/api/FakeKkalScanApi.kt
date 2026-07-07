package ru.kkalscan.data.api

import ru.kkalscan.data.repository.currentDateIso
import ru.kkalscan.domain.features.FeatureSearchCatalog
import ru.kkalscan.domain.food.LocalFoodCatalog
import ru.kkalscan.domain.model.FeatureSearchResult
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.FoodSearchResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.CreateWorkoutResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionStatus
import ru.kkalscan.domain.model.WorkoutEntry
import ru.kkalscan.domain.model.WorkoutParseResult
import ru.kkalscan.stats.WeekDates
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory API fake for local web dev and unit tests — no HTTP backend required.
 */
class FakeKkalScanApi(
    private val seedSampleWeek: Boolean = false,
    private val todayProvider: () -> String = { currentDateIso() },
) : IKkalScanApi {

    private val entriesByKey = mutableMapOf<String, MutableList<DiaryEntry>>()
    private val workoutsByKey = mutableMapOf<String, MutableList<WorkoutEntry>>()
    private val scansById = mutableMapOf<String, ScanResult>()
    private val weekSeedMarkers = mutableSetOf<String>()
    private val seedMutex = Mutex()

    private val scanPresets: List<Dish> = listOf(
        Dish("Борщ с говядиной", 300, 250, 12.0, 8.0, 22.0, fiber = 5.5),
        Dish("Куриная грудка с рисом", 350, 420, 45.0, 6.0, 52.0, fiber = 3.2),
        Dish("Салат Цезарь", 200, 280, 14.0, 18.0, 12.0, fiber = 4.8),
        Dish("Овсянка с бананом", 250, 320, 12.0, 8.0, 52.0, fiber = 8.1),
        Dish("Плов с бараниной", 320, 480, 22.0, 16.0, 58.0, fiber = 6.0),
    )

    override suspend fun scanPhoto(deviceId: String, photoBytes: ByteArray, timezoneOffsetMinutes: Int): ScanResult {
        ensureSampleWeekSeeded(deviceId)
        val dish = scanPresets[(photoBytes.sumOf { it.toInt() }.let { if (it < 0) -it else it }) % scanPresets.size]
        val scanId = nextId("scan")
        val result = ScanResult(
            scanId = scanId,
            dishes = listOf(dish),
            totalKcal = dish.kcal,
            totalProtein = dish.protein,
            totalFat = dish.fat,
            totalCarbs = dish.carbs,
            totalFiber = dish.fiber,
            scansLeft = scansLeft(deviceId),
            isPro = deviceId in proDevices,
        )
        scansById[scanId] = result
        return result
    }

    override suspend fun describeFood(
        deviceId: String,
        description: String,
        timezoneOffsetMinutes: Int,
    ): ScanResult {
        ensureSampleWeekSeeded(deviceId)
        val normalized = description.trim().lowercase()
        val dish = when {
            normalized.contains("борщ") -> scanPresets[0]
            normalized.contains("куриц") || normalized.contains("рис") -> scanPresets[1]
            normalized.contains("салат") -> scanPresets[2]
            normalized.contains("овсян") -> scanPresets[3]
            normalized.contains("плов") -> scanPresets[4]
            else -> scanPresets[normalized.hashCode().let { if (it < 0) -it else it } % scanPresets.size]
        }
        val scanId = nextId("scan")
        val result = ScanResult(
            scanId = scanId,
            dishes = listOf(dish),
            totalKcal = dish.kcal,
            totalProtein = dish.protein,
            totalFat = dish.fat,
            totalCarbs = dish.carbs,
            totalFiber = dish.fiber,
            scansLeft = scansLeft(deviceId),
            isPro = deviceId in proDevices,
        )
        scansById[scanId] = result
        return result
    }

    override suspend fun parseWorkout(deviceId: String, description: String): WorkoutParseResult {
        ensureSampleWeekSeeded(deviceId)
        val normalized = description.trim().lowercase()
        val minutes = Regex("(\\d+)\\s*мин").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(\\d+)\\s*ч").find(normalized)?.groupValues?.get(1)?.toIntOrNull()?.times(60)
            ?: 30
        val (title, kcalPerMinute) = when {
            normalized.contains("бег") || normalized.contains("пробеж") -> "Бег" to 10
            normalized.contains("йога") -> "Йога" to 4
            normalized.contains("плава") -> "Плавание" to 8
            normalized.contains("ходьб") -> "Ходьба" to 5
            normalized.contains("велос") -> "Велосипед" to 9
            else -> "Тренировка" to 7
        }
        return WorkoutParseResult(
            title = title,
            burnedKcal = (kcalPerMinute * minutes).coerceIn(50, 1500),
            durationMinutes = minutes,
        )
    }

    override suspend fun grantScanBonus(deviceId: String): ScanBonusResult =
        ScanBonusResult(scansLeft = 5, bonusGranted = true)

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay {
        ensureSampleWeekSeeded(deviceId)
        return buildDiaryDay(deviceId, date)
    }

    private fun buildDiaryDay(deviceId: String, date: String): DiaryDay {
        val entries = entriesByKey[key(deviceId, date)].orEmpty()
        val workouts = workoutsByKey[key(deviceId, date)].orEmpty()
        val consumed = entries.sumOf { it.totalKcal }
        val burned = workouts.sumOf { it.kcal }
        return DiaryDay(
            date = date,
            totalKcal = consumed,
            totalBurnedKcal = burned,
            netKcal = consumed - burned,
            scansLeft = scansLeft(deviceId),
            isPro = deviceId in proDevices,
            entries = entries,
            workouts = workouts,
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
            id = nextId("entry"),
            createdAt = "${date}T12:00:00Z",
            mealType = mealType,
            totalKcal = savedDishes.sumOf { it.kcal },
            dishes = savedDishes,
        )
        entriesByKey.getOrPut(key(deviceId, date)) { mutableListOf() }.add(entry)
        return CreateDiaryEntryResponse(entry = entry, scansLeft = scansLeft(deviceId))
    }

    override suspend fun deleteDiaryEntry(deviceId: String, entryId: String) {
        entriesByKey.filterKeys { it.startsWith("$deviceId|") }.values.forEach { list ->
            list.removeAll { it.id == entryId }
        }
    }

    override suspend fun addWorkout(deviceId: String, name: String, kcal: Int): CreateWorkoutResponse {
        val date = todayProvider()
        val workout = WorkoutEntry(
            id = nextId("workout"),
            createdAt = "${date}T12:00:00Z",
            name = name.trim(),
            kcal = kcal,
        )
        workoutsByKey.getOrPut(key(deviceId, date)) { mutableListOf() }.add(workout)
        return CreateWorkoutResponse(workout = workout)
    }

    override suspend fun deleteWorkout(deviceId: String, workoutId: String) {
        workoutsByKey.filterKeys { it.startsWith("$deviceId|") }.values.forEach { list ->
            list.removeAll { it.id == workoutId }
        }
    }

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus =
        SubscriptionStatus(
            isPro = deviceId in proDevices,
            accountLinked = false,
        )

    override suspend fun startProSubscription(deviceId: String, tariff: String): ProSubscriptionStart {
        proDevices.add(deviceId)
        return ProSubscriptionStart(
            isPro = true,
            proUntil = "${todayProvider()}T12:00:00Z",
            tariff = tariff,
            paymentRequired = false,
            message = "Pro активирован",
        )
    }

    private val searchLogs = mutableListOf<Triple<String, String, Int>>()

    override suspend fun searchFood(
        deviceId: String,
        query: String,
        limit: Int,
        source: String,
    ): FoodSearchResult {
        val trimmed = query.trim()
        val normalized = LocalFoodCatalog.normalize(trimmed)
        val items = LocalFoodCatalog.search(trimmed, limit)
        if (normalized.isNotBlank()) {
            searchLogs.add(Triple(deviceId, normalized, items.size))
        }
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
    ): BugReportResult {
        proDevices.add(deviceId)
        return BugReportResult(
            reportId = nextId("bug"),
            isPro = true,
            proUntil = "${todayProvider()}T12:00:00Z",
            message = "Спасибо! Pro на месяц активирован.",
        )
    }

    private suspend fun ensureSampleWeekSeeded(deviceId: String) {
        if (!seedSampleWeek) return
        val weekStart = WeekDates.iso(WeekDates.mondayOf(WeekDates.parse(todayProvider())))
        val marker = "$deviceId|$weekStart"
        seedMutex.withLock {
            if (marker in weekSeedMarkers) return
            val sampleDays = listOf(
                Triple(12.0, 15.0, 10.0) to 6.0,
                Triple(18.0, 8.0, 45.0) to 8.5,
                Triple(14.0, 12.0, 30.0) to 12.0,
                Triple(20.0, 10.0, 55.0) to 9.0,
                Triple(16.0, 14.0, 40.0) to 15.5,
            )
            WeekDates.weekFrom(WeekDates.parse(weekStart)).take(sampleDays.size).forEachIndexed { index, date ->
                val (macros, fiber) = sampleDays[index]
                val (protein, fat, carbs) = macros
                val dish = Dish(
                    name = "Обед ${index + 1}",
                    grams = 300,
                    kcal = (protein * 4 + fat * 9 + carbs * 4).toInt(),
                    protein = protein,
                    fat = fat,
                    carbs = carbs,
                    fiber = fiber,
                )
                entriesByKey.getOrPut(key(deviceId, date)) { mutableListOf() }.add(
                    DiaryEntry(
                        id = nextId("seed"),
                        createdAt = "${date}T12:00:00Z",
                        mealType = MealType.lunch,
                        totalKcal = dish.kcal,
                        dishes = listOf(dish),
                    ),
                )
            }
            weekSeedMarkers.add(marker)
        }
    }

    private val proDevices = mutableSetOf<String>()
    private var idCounter = 0

    private fun scansLeft(deviceId: String): Int {
        val today = todayProvider()
        val used = entriesByKey[key(deviceId, today)]?.size ?: 0
        return (3 - used).coerceAtLeast(0)
    }

    private fun key(deviceId: String, date: String) = "$deviceId|$date"

    private fun nextId(prefix: String): String {
        idCounter++
        return "$prefix-$idCounter"
    }
}
