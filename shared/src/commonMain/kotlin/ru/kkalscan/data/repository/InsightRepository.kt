package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.insights.DietitianInsight
import ru.kkalscan.stats.WeekStats

interface IInsightRepository {
    suspend fun requestDietitianInsight(weekStart: String, week: WeekStats): DietitianInsight
}

class InsightRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : IInsightRepository {

    override suspend fun requestDietitianInsight(weekStart: String, week: WeekStats): DietitianInsight {
        val deviceId = deviceIdStorage.getDeviceId()
        if (!week.isPro) throw KkalScanException.Api("Доступно в Pro")
        if (week.daysWithData < 3) {
            throw KkalScanException.Api("Нужно минимум 3 дня с записями")
        }
        return api.requestDietitianInsight(
            deviceId = deviceId,
            weekStart = weekStart,
            timezoneOffsetMinutes = currentTimezoneOffsetMinutes(),
        )
    }
}
