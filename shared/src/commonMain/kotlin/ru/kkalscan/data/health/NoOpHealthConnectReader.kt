package ru.kkalscan.data.health

class NoOpHealthConnectReader(
    private val activeCalories: Int = 0,
    private val steps: Int? = null,
    private val available: Boolean = false,
    private val permissionsGranted: Boolean = false,
) : IHealthConnectReader {
    override suspend fun isAvailable(): Boolean = available
    override suspend fun hasPermissions(): Boolean = permissionsGranted || available
    override suspend fun readTodayActiveCalories(): Int = activeCalories
    override suspend fun readTodaySteps(): Int? = steps
}
