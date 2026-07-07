package ru.kkalscan.data.health

interface IHealthConnectReader {
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun readTodayActiveCalories(): Int
    suspend fun readTodaySteps(): Int?
}

expect fun createHealthConnectReader(): IHealthConnectReader
