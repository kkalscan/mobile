package ru.kkalscan.domain.features

import ru.kkalscan.domain.model.FeatureSearchItem

data class FeatureSearchQueryResult(
    val items: List<FeatureSearchItem>,
    val popularFallback: Boolean,
)

/** Offline mirror of backend feature_search_items (V5 migration). */
object FeatureSearchCatalog {
    val items: List<FeatureSearchItem> = listOf(
        FeatureSearchItem("diary_today", "Сегодня", "Дневник питания и калории за день", "kkalscan://diary", "today"),
        FeatureSearchItem("scan", "Сканировать еду", "Калории и БЖУ по фото", "kkalscan://scan", "scan"),
        FeatureSearchItem("food_search", "Найти продукт", "Добавить блюдо из каталога", "kkalscan://food-search", "search"),
        FeatureSearchItem("journal", "Дневник за неделю", "Графики калорий и БЖУ", "kkalscan://journal", "journal"),
        FeatureSearchItem("fiber", "Клетчатка", "График клетчатки за неделю", "kkalscan://journal/fiber", "fiber"),
        FeatureSearchItem("profile", "Профиль", "Подписка Pro и настройки", "kkalscan://profile", "profile"),
        FeatureSearchItem("paywall", "Pro подписка", "Безлимитные сканы — 199 ₽/мес", "kkalscan://paywall", "pro"),
        FeatureSearchItem("macros", "БЖУ за неделю", "Белки, жиры и углеводы", "kkalscan://journal", "macros"),
        FeatureSearchItem("free_scans", "Бесплатные сканы", "3 скана каждый день", "kkalscan://diary", "gift"),
        FeatureSearchItem("dietitian", "Анализ диетолога", "AI-разбор питания за неделю", "kkalscan://journal/dietitian", "dietitian"),
    )

    private val keywordMap: Map<String, List<String>> = mapOf(
        "diary_today" to listOf("сегодня", "дневник", "калории", "ккал", "день", "съедено", "питание"),
        "scan" to listOf("скан", "фото", "камера", "распознать", "добавить", "еда", "сфотографировать"),
        "food_search" to listOf("продукт", "каталог", "найти", "борщ", "творог", "добавить еду", "без фото"),
        "journal" to listOf("дневник", "неделя", "график", "статистика", "калории", "прогресс", "журнал"),
        "fiber" to listOf("клетчатка", "график", "кл", "волокна", "клетчатки"),
        "profile" to listOf("профиль", "настройки", "pro", "подписка", "аккаунт", "личный"),
        "paywall" to listOf("pro", "подписка", "безлимит", "199", "оплата", "лимит"),
        "macros" to listOf("бжу", "белки", "жиры", "углеводы", "макросы", "белок"),
        "free_scans" to listOf("сканы", "бесплатно", "лимит", "осталось", "бесплатный"),
        "dietitian" to listOf("диетолог", "анализ", "разбор", "ai", "insight", "рекомендации"),
    )

    private val sortOrder = items.mapIndexed { index, item -> item.id to index }.toMap()

    fun normalize(query: String): String =
        query.trim().lowercase().replace(Regex("\\s+"), " ")

    fun query(query: String, limit: Int): FeatureSearchQueryResult {
        val normalized = normalize(query)
        val safeLimit = limit.coerceIn(1, 50)
        if (normalized.isBlank()) {
            return FeatureSearchQueryResult(items = emptyList(), popularFallback = false)
        }
        val matched = rankMatches(normalized, safeLimit)
        if (matched.isNotEmpty()) {
            return FeatureSearchQueryResult(items = matched, popularFallback = false)
        }
        return FeatureSearchQueryResult(
            items = items.take(safeLimit),
            popularFallback = true,
        )
    }

    fun search(query: String, limit: Int): List<FeatureSearchItem> = query(query, limit).items

    private fun rankMatches(normalized: String, limit: Int): List<FeatureSearchItem> =
        items.mapNotNull { item ->
            score(item, normalized)?.let { item to it }
        }.sortedWith(
            compareByDescending<Pair<FeatureSearchItem, Int>> { it.second }
                .thenBy { sortOrder[it.first.id] ?: 0 },
        ).map { it.first }.take(limit)

    private fun score(item: FeatureSearchItem, normalized: String): Int? {
        val title = normalize(item.title)
        val subtitle = item.subtitle?.let { normalize(it) }.orEmpty()
        val keywords = keywordMap[item.id].orEmpty()
        if (title.contains(normalized) || subtitle.contains(normalized)) return 100
        val tokens = normalized.split(' ').filter { it.length >= 2 }
        if (tokens.isEmpty()) return null
        var score = 0
        for (token in tokens) {
            when {
                title.contains(token) -> score += 40
                subtitle.contains(token) -> score += 25
                keywords.any { it.contains(token) || token.contains(it) } -> score += 30
                else -> return null
            }
        }
        return score
    }
}
