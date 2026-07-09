package ru.kkalscan.data.steps

interface ILocalStepCounter {
    suspend fun isSensorAvailable(): Boolean
    suspend fun hasPermission(): Boolean
    suspend fun readCumulativeSteps(): Long?
}

expect fun createLocalStepCounter(): ILocalStepCounter
