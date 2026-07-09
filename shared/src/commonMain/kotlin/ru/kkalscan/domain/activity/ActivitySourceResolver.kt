package ru.kkalscan.domain.activity

import ru.kkalscan.domain.model.ActivityEmulator

data class ResolvedActivity(
    val source: ActivitySource,
    val activeKcal: Int,
    val steps: Int?,
)

object ActivitySourceResolver {
    fun resolve(
        sensorSteps: Int?,
        sensorAvailable: Boolean,
        sensorPermissionGranted: Boolean,
        emulator: ActivityEmulator?,
    ): ResolvedActivity {
        if (sensorAvailable && sensorPermissionGranted) {
            val steps = sensorSteps?.takeIf { it > 0 }
            if (steps != null) {
                return ResolvedActivity(
                    source = ActivitySource.DeviceSensor,
                    activeKcal = StepCalorieEstimator.estimate(steps),
                    steps = steps,
                )
            }
        }
        if (emulator != null) {
            return ResolvedActivity(
                source = ActivitySource.Emulator,
                activeKcal = emulator.estimatedActiveKcal,
                steps = emulator.estimatedSteps,
            )
        }
        return ResolvedActivity(
            source = ActivitySource.None,
            activeKcal = 0,
            steps = null,
        )
    }
}
