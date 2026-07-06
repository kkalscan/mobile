package ru.kkalscan.data.storage

import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kkalscan.domain.model.WorkoutEntry

private const val STORAGE_KEY = "kkalscan_workouts"

class WasmWorkoutStorage : IWorkoutStorage {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, MutableList<WorkoutEntry>>()

    private fun load() {
        if (cache.isNotEmpty()) return
        val raw = localStorage.getItem(STORAGE_KEY) ?: return
        runCatching {
            val stored = json.decodeFromString<Map<String, List<WorkoutEntry>>>(raw)
            stored.forEach { (key, entries) ->
                cache[key] = entries.toMutableList()
            }
        }
    }

    private fun persist() {
        localStorage.setItem(STORAGE_KEY, json.encodeToString(cache.mapValues { it.value.toList() }))
    }

    override suspend fun getWorkouts(deviceId: String, date: String): List<WorkoutEntry> {
        load()
        return cache["$deviceId|$date"].orEmpty()
    }

    override suspend fun addWorkout(deviceId: String, date: String, entry: WorkoutEntry) {
        load()
        cache.getOrPut("$deviceId|$date") { mutableListOf() }.add(entry)
        persist()
    }

    override suspend fun deleteWorkout(deviceId: String, entryId: String) {
        load()
        cache.filterKeys { it.startsWith("$deviceId|") }.values.forEach { list ->
            list.removeAll { it.id == entryId }
        }
        persist()
    }
}

actual fun createWorkoutStorage(): IWorkoutStorage = WasmWorkoutStorage()
