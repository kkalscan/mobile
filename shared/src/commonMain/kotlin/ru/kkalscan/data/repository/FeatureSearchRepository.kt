package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.FeatureSearchIntentResult
import ru.kkalscan.domain.model.FeatureSearchResult

interface IFeatureSearchRepository {
    suspend fun search(query: String, locale: String = "ru"): FeatureSearchResult

    suspend fun classifyIntent(query: String): FeatureSearchIntentResult
}

class FeatureSearchRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : IFeatureSearchRepository {

    override suspend fun search(query: String, locale: String): FeatureSearchResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.searchFeatures(deviceId, query, limit = 20, locale = locale)
    }

    override suspend fun classifyIntent(query: String): FeatureSearchIntentResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.classifyFeatureSearchIntent(deviceId, query)
    }
}
