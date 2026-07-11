package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import ru.kkalscan.domain.model.ActivityEmulator
import kotlin.test.Test

class ActivitySourceResolverTest {

    @Test
    fun prefersSensorWhenPermissionAndSteps() {
        val result = ActivitySourceResolver.resolve(
            sensorSteps = 5000,
            sensorAvailable = true,
            sensorPermissionGranted = true,
            emulator = ActivityEmulator("population_default", 750, 18_750),
        )
        result.source shouldBe ActivitySource.DeviceSensor
        result.activeKcal shouldBe 191
        result.steps shouldBe 5000
    }

    @Test
    fun fallsBackToEmulatorWithoutPermission() {
        val result = ActivitySourceResolver.resolve(
            sensorSteps = 5000,
            sensorAvailable = true,
            sensorPermissionGranted = false,
            emulator = ActivityEmulator("population_default", 400, 10_000),
        )
        result.source shouldBe ActivitySource.Emulator
        result.activeKcal shouldBe 400
    }

    @Test
    fun fallsBackToEmulatorWhenNoSteps() {
        val result = ActivitySourceResolver.resolve(
            sensorSteps = 0,
            sensorAvailable = true,
            sensorPermissionGranted = true,
            emulator = ActivityEmulator("diary_based", 500, 12_500),
        )
        result.source shouldBe ActivitySource.Emulator
        result.activeKcal shouldBe 500
    }

    @Test
    fun noneWhenNoSources() {
        val result = ActivitySourceResolver.resolve(
            sensorSteps = null,
            sensorAvailable = false,
            sensorPermissionGranted = false,
            emulator = null,
        )
        result.source shouldBe ActivitySource.None
        result.activeKcal shouldBe 0
    }
}
