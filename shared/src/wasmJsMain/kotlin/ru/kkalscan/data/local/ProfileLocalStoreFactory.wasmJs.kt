package ru.kkalscan.data.local

actual fun createProfileLocalStore(): IProfileLocalStore = InMemoryProfileLocalStore()
