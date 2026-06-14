package ru.kkalscan.data.storage

interface IDeviceIdStorage {
    fun getDeviceId(): String
    fun setDeviceId(id: String)
}
