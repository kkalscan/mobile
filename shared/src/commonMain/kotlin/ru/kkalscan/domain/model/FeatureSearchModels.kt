package ru.kkalscan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureSearchItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val deeplink: String,
    val icon: String,
)

@Serializable
data class FeatureSearchResult(
    val query: String,
    val items: List<FeatureSearchItem>,
    val total: Int,
    @SerialName("popular_fallback")
    val popularFallback: Boolean = false,
)

@Serializable
data class FeatureSearchIntentResult(
    val query: String,
    @SerialName("is_food_intent")
    val isFoodIntent: Boolean,
)
