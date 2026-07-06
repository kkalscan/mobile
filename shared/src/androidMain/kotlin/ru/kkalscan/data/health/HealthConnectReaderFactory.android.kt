package ru.kkalscan.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kkalscan.data.storage.AndroidDeviceIdContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private val readPermissions = setOf(
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
)

class AndroidHealthConnectReader(
    private val context: Context,
) : IHealthConnectReader {

    private val client: HealthConnectClient? by lazy {
        if (!HealthConnectClient.isAvailable(context)) null
        else HealthConnectClient.getOrCreate(context)
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        HealthConnectClient.isAvailable(context)
    }

    override suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        val hc = client ?: return@withContext false
        runCatching {
            val granted = hc.permissionController.getGrantedPermissions()
            readPermissions.all { it in granted }
        }.getOrDefault(false)
    }

    override suspend fun readTodayActiveCalories(): Int = withContext(Dispatchers.IO) {
        val hc = client ?: return@withContext 0
        if (!hasPermissions()) return@withContext 0
        runCatching {
            val range = todayRange()
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(range.first, range.second),
                ),
            )
            response.records.sumOf { record ->
                record.energy.inKilocalories.toInt()
            }
        }.getOrDefault(0)
    }

    override suspend fun readTodaySteps(): Int? = withContext(Dispatchers.IO) {
        val hc = client ?: return@withContext null
        if (!hasPermissions()) return@withContext null
        runCatching {
            val range = todayRange()
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(range.first, range.second),
                ),
            )
            response.records.sumOf { it.count }.toInt()
        }.getOrNull()
    }

    private fun todayRange(): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val end = Instant.now()
        return start to end
    }
}

fun healthConnectReadPermissions(): Set<String> = readPermissions

actual fun createHealthConnectReader(): IHealthConnectReader =
    AndroidHealthConnectReader(AndroidDeviceIdContext.appContext)
