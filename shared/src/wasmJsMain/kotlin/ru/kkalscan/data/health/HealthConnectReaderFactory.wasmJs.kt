package ru.kkalscan.data.health

/** Web preview: demo Health Connect data so calorie deficit is visible without a device. */
class WebDemoHealthConnectReader : IHealthConnectReader {
    override suspend fun isAvailable(): Boolean = true
    override suspend fun hasPermissions(): Boolean = true
    override suspend fun readTodayActiveCalories(): Int = 420
    override suspend fun readTodaySteps(): Int = 6_500
}

actual fun createHealthConnectReader(): IHealthConnectReader = WebDemoHealthConnectReader()
