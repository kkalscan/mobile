package ru.kkalscan.data.storage

import ru.kkalscan.domain.model.WorkoutEntry

class InMemoryWorkoutStorage : IWorkoutStorage {
    private val workoutsByKey = mutableMapOf<String, MutableList<WorkoutEntry>>()

    override suspend fun getWorkouts(deviceId: String, date: String): List<WorkoutEntry> =
        workoutsByKey[key(deviceId, date)].orEmpty()

    override suspend fun addWorkout(deviceId: String, date: String, entry: WorkoutEntry) {
        workoutsByKey.getOrPut(key(deviceId, date)) { mutableListOf() }.add(entry)
    }

    override suspend fun deleteWorkout(deviceId: String, entryId: String) {
        workoutsByKey.filterKeys { it.startsWith("$deviceId|") }.values.forEach { list ->
            list.removeAll { it.id == entryId }
        }
    }

    private fun key(deviceId: String, date: String) = "$deviceId|$date"
}
