package ru.kkalscan.data.repository

import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.insights.DietitianInsight
import ru.kkalscan.insights.InsightSection
import ru.kkalscan.stats.WeekStats

interface IInsightRepository {
    suspend fun requestDietitianInsight(weekStart: String, week: WeekStats): DietitianInsight
}

class InsightRepository(
    private val deviceIdStorage: IDeviceIdStorage,
) : IInsightRepository {

    override suspend fun requestDietitianInsight(weekStart: String, week: WeekStats): DietitianInsight {
        deviceIdStorage.getDeviceId()
        if (!week.isPro) throw KkalScanException.Api("Доступно в Pro")
        if (week.daysWithData < 3) {
            throw KkalScanException.Api("Нужно минимум 3 дня с записями")
        }
        return stubInsight(weekStart, week)
    }

    private fun stubInsight(weekStart: String, week: WeekStats): DietitianInsight {
        return DietitianInsight(
            weekStart = weekStart,
            generatedAt = weekStart,
            headline = when {
                week.avgKcal > 2200 -> "Калорийность выше среднего — следите за порциями"
                week.avgKcal < 1400 -> "Недобор калорий — добавьте полноценные приёмы пищи"
                else -> "Неделя сбалансированная по калориям"
            },
            sections = listOf(
                InsightSection(
                    "Калории",
                    "В среднем ${week.avgKcal} ккал/день за ${week.daysWithData} дней с данными. " +
                        "Сумма за неделю: ${week.totalKcal} ккал.",
                ),
                InsightSection(
                    "Рекомендации",
                    buildString {
                        if (week.daysWithData < 5) append("Старайтесь логировать еду чаще — так точнее картина. ")
                        append("Продолжайте сканировать тарелки для точного учёта.")
                    },
                ),
            ),
        )
    }
}
