package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult

interface IScanRepository {
    suspend fun scanPhoto(photoBytes: ByteArray, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): ScanResult
    suspend fun grantAdBonus(): ScanBonusResult
}

class ScanRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : IScanRepository {

    override suspend fun scanPhoto(photoBytes: ByteArray, timezoneOffsetMinutes: Int): ScanResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.scanPhoto(deviceId, photoBytes, timezoneOffsetMinutes)
    }

    override suspend fun grantAdBonus(): ScanBonusResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.grantScanBonus(deviceId)
    }
}
