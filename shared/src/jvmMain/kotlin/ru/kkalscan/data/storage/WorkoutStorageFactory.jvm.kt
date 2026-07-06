package ru.kkalscan.data.storage

actual fun createWorkoutStorage(): IWorkoutStorage = InMemoryWorkoutStorage()
