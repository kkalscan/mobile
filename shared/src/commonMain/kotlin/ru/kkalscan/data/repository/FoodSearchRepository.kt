package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.FoodSearchResult

interface IFoodSearchRepository {
    suspend fun search(query: String, source: String = "diary"): FoodSearchResult
}

class FoodSearchRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : IFoodSearchRepository {

    override suspend fun search(query: String, source: String): FoodSearchResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.searchFood(deviceId, query, limit = 20, source = source)
    }
}
