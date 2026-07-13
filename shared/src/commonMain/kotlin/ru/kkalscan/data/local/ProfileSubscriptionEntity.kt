package ru.kkalscan.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "profile_subscription")
data class ProfileSubscriptionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payloadJson: String,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
