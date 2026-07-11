package ru.kkalscan.data.profile

import ru.kkalscan.domain.activity.EnergyProfile
import ru.kkalscan.domain.activity.Sex

interface IEnergyProfileStorage {
    fun getProfile(): EnergyProfile?
    fun saveProfile(profile: EnergyProfile)
}

class InMemoryEnergyProfileStorage(
    private var profile: EnergyProfile? = null,
) : IEnergyProfileStorage {
    override fun getProfile(): EnergyProfile? = profile
    override fun saveProfile(profile: EnergyProfile) {
        this.profile = profile.normalized()
    }
}

expect fun createEnergyProfileStorage(): IEnergyProfileStorage
