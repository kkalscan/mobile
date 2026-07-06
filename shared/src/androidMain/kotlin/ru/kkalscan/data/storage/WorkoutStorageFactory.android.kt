package ru.kkalscan.data.storage

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kkalscan.domain.model.WorkoutEntry

private const val PREFS_NAME = "kkalscan_workouts"
private const val PREFS_KEY = "workouts_json"

class PersistentWorkoutStorage(
    private val readJson: () -> String?,
    private val writeJson: (String) -> Unit,
) : IWorkoutStorage {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, MutableList<WorkoutEntry>>()

    private fun load() {
        if (cache.isNotEmpty()) return
        val raw = readJson() ?: return
        runCatching {
            val stored = json.decodeFromString<Map<String, List<WorkoutEntry>>>(raw)
            stored.forEach { (key, entries) ->
                cache[key] = entries.toMutableList()
            }
        }
    }

    private fun persist() {
        writeJson(json.encodeToString(cache.mapValues { it.value.toList() }))
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

actual fun createWorkoutStorage(): IWorkoutStorage {
    val prefs = AndroidDeviceIdContext.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return PersistentWorkoutStorage(
        readJson = { prefs.getString(PREFS_KEY, null) },
        writeJson = { prefs.edit().putString(PREFS_KEY, it).apply() },
    )
}
