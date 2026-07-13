package ru.kkalscan.data.local

import ru.kkalscan.domain.model.DiaryDay

data class DiaryResource(
    val day: DiaryDay?,
    val isRefreshing: Boolean,
    val error: Throwable? = null,
)
