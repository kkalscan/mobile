package ru.kkalscan.data.local

/**
 * Room 3 wasm needs a dedicated SQLite web worker stack; use in-memory cache for wasm demos.
 * Offline-first Flow contract is identical; persistence is Room on Android/JVM.
 */
actual fun createDiaryLocalStore(): IDiaryLocalStore = InMemoryDiaryLocalStore()
