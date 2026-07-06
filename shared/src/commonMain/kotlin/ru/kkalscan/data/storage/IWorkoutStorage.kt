package ru.kkalscan.data.storage

import ru.kkalscan.domain.model.WorkoutEntry

interface IWorkoutStorage {
    suspend fun getWorkouts(deviceId: String, date: String): List<WorkoutEntry>
    suspend fun addWorkout(deviceId: String, date: String, entry: WorkoutEntry)
    suspend fun deleteWorkout(deviceId: String, entryId: String)
}

expect fun createWorkoutStorage(): IWorkoutStorage
