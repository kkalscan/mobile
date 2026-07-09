package ru.kkalscan.data.steps

private class NoOpLocalStepCounter : ILocalStepCounter {
    override suspend fun isSensorAvailable(): Boolean = false
    override suspend fun hasPermission(): Boolean = false
    override suspend fun readCumulativeSteps(): Long? = null
}

actual fun createLocalStepCounter(): ILocalStepCounter = NoOpLocalStepCounter()

actual fun createStepBaselineStorage(): IStepBaselineStorage = InMemoryStepBaselineStorage()
