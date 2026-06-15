package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.BugReportResult

interface IBugReportRepository {
    suspend fun submit(email: String, description: String, screenshots: List<ByteArray>): BugReportResult
}

class BugReportRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : IBugReportRepository {

    override suspend fun submit(
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ): BugReportResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.submitBugReport(deviceId, email, description, screenshots)
    }
}
