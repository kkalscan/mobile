package ru.kkalscan.data.local

import ru.kkalscan.domain.model.SubscriptionStatus

data class ProfileResource(
    val status: SubscriptionStatus?,
    val scansLeft: Int?,
    val isRefreshing: Boolean,
    val error: Throwable?,
)
