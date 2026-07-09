package ru.kkalscan.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kkalscan.data.storage.AndroidDeviceIdContext
import ru.kkalscan.health.HealthConnectFeature
import ru.kkalscan.util.kkalLog
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private const val LOG_TAG = "HealthConnect"

private val readPermissions = setOf(
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
)

class AndroidHealthConnectReader(
    private val context: Context,
) : IHealthConnectReader {

    private val client: HealthConnectClient? by lazy {
        if (!healthConnectAvailable(context)) {
            kkalLog(LOG_TAG, "client unavailable sdkStatus=${HealthConnectClient.getSdkStatus(context)}")
            null
        } else {
            HealthConnectClient.getOrCreate(context)
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val available = healthConnectAvailable(context)
        kkalLog(LOG_TAG, "isAvailable=$available sdkStatus=${HealthConnectClient.getSdkStatus(context)}")
        available
    }

    override suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        val hc = client ?: return@withContext false
        runCatching {
            val granted = hc.permissionController.getGrantedPermissions()
            val missing = readPermissions.filterNot { it in granted }
            val allGranted = missing.isEmpty()
            kkalLog(
                LOG_TAG,
                "hasPermissions=$allGranted granted=${granted.size} missing=${missing.joinToString()}",
            )
            allGranted
        }.getOrElse { error ->
            kkalLog(LOG_TAG, "hasPermissions error ${error::class.simpleName}: ${error.message}")
            false
        }
    }

    override suspend fun readTodayActiveCalories(): Int = withContext(Dispatchers.IO) {
        val hc = client ?: return@withContext logAndReturnZero("readTodayActiveCalories: no client")
        if (!hasPermissionsInternal(hc)) {
            return@withContext logAndReturnZero("readTodayActiveCalories: permissions missing")
        }
        val range = todayRange()
        runCatching {
            val aggregateKcal = hc.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(range.first, range.second),
                ),
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories
                ?.toInt()
                ?: 0

            val records = hc.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(range.first, range.second),
                ),
            ).records
            val recordsKcal = records.sumOf { it.energy.inKilocalories.toInt() }
            val origins = records
                .map { it.metadata.dataOrigin.packageName }
                .distinct()
                .joinToString()
            kkalLog(
                LOG_TAG,
                "activeCalories aggregate=$aggregateKcal records=$recordsKcal count=${records.size} " +
                    "range=${range.first}..${range.second} origins=[$origins]",
            )
            aggregateKcal
        }.getOrElse { error ->
            kkalLog(LOG_TAG, "readTodayActiveCalories error ${error::class.simpleName}: ${error.message}")
            0
        }
    }

    override suspend fun readTodaySteps(): Int? = withContext(Dispatchers.IO) {
        val hc = client ?: return@withContext logAndReturnNullSteps("readTodaySteps: no client")
        if (!hasPermissionsInternal(hc)) {
            return@withContext logAndReturnNullSteps("readTodaySteps: permissions missing")
        }
        val range = todayRange()
        runCatching {
            val aggregateSteps = hc.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(range.first, range.second),
                ),
            )[StepsRecord.COUNT_TOTAL]
                ?.toInt()

            val records = hc.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(range.first, range.second),
                ),
            ).records
            val recordsSteps = records.sumOf { it.count }.toInt()
            val origins = records
                .map { it.metadata.dataOrigin.packageName }
                .distinct()
                .joinToString()
            kkalLog(
                LOG_TAG,
                "steps aggregate=$aggregateSteps records=$recordsSteps count=${records.size} " +
                    "range=${range.first}..${range.second} origins=[$origins]",
            )
            aggregateSteps
        }.getOrElse { error ->
            kkalLog(LOG_TAG, "readTodaySteps error ${error::class.simpleName}: ${error.message}")
            null
        }
    }

    private suspend fun hasPermissionsInternal(hc: HealthConnectClient): Boolean {
        val granted = hc.permissionController.getGrantedPermissions()
        return readPermissions.all { it in granted }
    }

    private fun todayRange(): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val end = Instant.now()
        return start to end
    }

    private fun logAndReturnZero(reason: String): Int {
        kkalLog(LOG_TAG, reason)
        return 0
    }

    private fun logAndReturnNullSteps(reason: String): Int? {
        kkalLog(LOG_TAG, reason)
        return null
    }
}

fun healthConnectReadPermissions(): Set<String> = readPermissions

actual fun createHealthConnectReader(): IHealthConnectReader =
    if (HealthConnectFeature.ENABLED) {
        AndroidHealthConnectReader(AndroidDeviceIdContext.appContext)
    } else {
        NoOpHealthConnectReader()
    }

fun healthConnectAvailable(context: Context): Boolean =
    HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
