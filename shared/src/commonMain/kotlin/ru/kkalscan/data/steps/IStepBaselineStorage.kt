package ru.kkalscan.data.steps

interface IStepBaselineStorage {
    fun getBaseline(dateIso: String): Long?
    fun setBaseline(dateIso: String, cumulativeSteps: Long)
}

class InMemoryStepBaselineStorage : IStepBaselineStorage {
    private val baselines = mutableMapOf<String, Long>()

    override fun getBaseline(dateIso: String): Long? = baselines[dateIso]

    override fun setBaseline(dateIso: String, cumulativeSteps: Long) {
        baselines[dateIso] = cumulativeSteps
    }
}

expect fun createStepBaselineStorage(): IStepBaselineStorage
