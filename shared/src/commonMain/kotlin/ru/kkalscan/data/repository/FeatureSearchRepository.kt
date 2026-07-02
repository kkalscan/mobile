package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.FeatureSearchResult

interface IFeatureSearchRepository {
    suspend fun search(query: String, locale: String = "ru"): FeatureSearchResult
}

class FeatureSearchRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : IFeatureSearchRepository {

    override suspend fun search(query: String, locale: String): FeatureSearchResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.searchFeatures(deviceId, query, limit = 20, locale = locale)
    }
}
