package ru.kkalscan.data.steps

class StepCounterStore(
    private val counter: ILocalStepCounter,
    private val baselineStorage: IStepBaselineStorage,
    private val todayProvider: () -> String,
) {
    suspend fun readTodaySteps(): Int? {
        if (!counter.isSensorAvailable() || !counter.hasPermission()) return null
        val cumulative = counter.readCumulativeSteps() ?: return null
        val today = todayProvider()
        val baseline = baselineStorage.getBaseline(today) ?: run {
            baselineStorage.setBaseline(today, cumulative)
            cumulative
        }
        return (cumulative - baseline).toInt().coerceAtLeast(0)
    }
}
