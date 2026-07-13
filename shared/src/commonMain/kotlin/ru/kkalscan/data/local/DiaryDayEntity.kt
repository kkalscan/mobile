package ru.kkalscan.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "diary_days")
data class DiaryDayEntity(
    @PrimaryKey val date: String,
    val payloadJson: String,
    val updatedAt: Long,
)
