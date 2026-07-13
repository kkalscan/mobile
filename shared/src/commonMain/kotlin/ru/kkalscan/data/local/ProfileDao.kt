package ru.kkalscan.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile_subscription WHERE id = 1 LIMIT 1")
    fun observeSubscription(): Flow<ProfileSubscriptionEntity?>

    @Query("SELECT * FROM profile_subscription WHERE id = 1 LIMIT 1")
    suspend fun getSubscription(): ProfileSubscriptionEntity?

    @Upsert
    suspend fun upsert(entity: ProfileSubscriptionEntity)
}
