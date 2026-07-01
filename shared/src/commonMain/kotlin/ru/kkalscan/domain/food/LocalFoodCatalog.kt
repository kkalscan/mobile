package ru.kkalscan.domain.food

import ru.kkalscan.domain.model.Dish

/** Local catalog for fake API and offline search preview. */
object LocalFoodCatalog {
    val items: List<Dish> = listOf(
        Dish("Борщ", 300, 180, 8.0, 6.0, 22.0, 4.5),
        Dish("Гречка с курицей", 250, 320, 28.0, 8.0, 42.0, 3.0),
        Dish("Овсянка", 200, 150, 5.0, 3.0, 27.0, 4.0),
        Dish("Омлет", 150, 220, 14.0, 16.0, 2.0, 0.0),
        Dish("Творог 5%", 150, 180, 24.0, 7.5, 6.0, 0.0),
        Dish("Куриная грудка", 150, 165, 31.0, 3.6, 0.0, 0.0),
        Dish("Рис отварной", 200, 260, 5.4, 0.6, 58.0, 1.0),
        Dish("Плов", 300, 480, 22.0, 16.0, 58.0, 6.0),
        Dish("Салат Цезарь", 200, 280, 14.0, 18.0, 12.0, 4.8),
        Dish("Шаурма", 350, 520, 24.0, 22.0, 48.0, 3.5),
        Dish("Пельмени", 250, 420, 18.0, 14.0, 48.0, 2.0),
        Dish("Сырники", 180, 340, 16.0, 14.0, 38.0, 1.5),
        Dish("Блины", 120, 260, 7.0, 8.0, 38.0, 1.2),
        Dish("Яблоко", 150, 78, 0.4, 0.2, 21.0, 3.6),
        Dish("Банан", 120, 105, 1.3, 0.4, 27.0, 2.6),
        Dish("Греческий йогурт", 150, 120, 12.0, 4.0, 8.0, 0.0),
        Dish("Лосось", 150, 280, 30.0, 18.0, 0.0, 0.0),
        Dish("Авокадо", 100, 160, 2.0, 15.0, 9.0, 6.7),
        Dish("Хлеб белый", 50, 130, 4.0, 1.0, 26.0, 1.2),
        Dish("Кефир 1%", 250, 100, 8.0, 2.5, 12.0, 0.0),
        Dish("Котлета домашняя", 120, 260, 16.0, 18.0, 8.0, 0.5),
        Dish("Картофельное пюре", 200, 180, 4.0, 6.0, 28.0, 2.5),
        Dish("Оливье", 150, 220, 6.0, 16.0, 14.0, 2.0),
        Dish("Селёдка под шубой", 150, 240, 8.0, 18.0, 12.0, 2.5),
        Dish("Суп куриный", 300, 120, 10.0, 4.0, 12.0, 1.5),
        Dish("Макароны с сыром", 250, 380, 14.0, 12.0, 52.0, 2.5),
        Dish("Протеиновый батончик", 60, 200, 20.0, 7.0, 18.0, 3.0),
        Dish("Капучино", 250, 80, 4.0, 3.0, 8.0, 0.5),
        Dish("Черный кофе", 200, 2, 0.2, 0.0, 0.0, 0.0),
        Dish("Голубцы", 250, 220, 14.0, 10.0, 20.0, 3.0),
    )

    fun search(query: String, limit: Int): List<Dish> {
        val normalized = normalize(query)
        if (normalized.isBlank()) return emptyList()
        return items
            .filter { dish ->
                val name = normalize(dish.name)
                name.contains(normalized) ||
                    normalized.split(' ').all { token -> token.length >= 2 && name.contains(token) }
            }
            .take(limit.coerceIn(1, 50))
    }

    fun normalize(query: String): String =
        query.trim().lowercase().replace(Regex("\\s+"), " ")
}
