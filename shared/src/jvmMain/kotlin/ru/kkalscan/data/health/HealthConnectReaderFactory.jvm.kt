package ru.kkalscan.data.health

actual fun createHealthConnectReader(): IHealthConnectReader = NoOpHealthConnectReader()
