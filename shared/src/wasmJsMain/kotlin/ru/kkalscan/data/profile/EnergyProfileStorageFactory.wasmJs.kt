package ru.kkalscan.data.profile

actual fun createEnergyProfileStorage(): IEnergyProfileStorage = InMemoryEnergyProfileStorage()
