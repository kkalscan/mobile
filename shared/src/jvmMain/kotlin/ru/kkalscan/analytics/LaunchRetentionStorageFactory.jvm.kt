package ru.kkalscan.analytics

actual fun createLaunchRetentionStorage(): LaunchRetentionStorage =
    InMemoryLaunchRetentionStorage()

private class InMemoryLaunchRetentionStorage : LaunchRetentionStorage {
    private var firstLaunch: String? = null
    private var day1Sent = false
    private var day7Sent = false

    override fun readFirstLaunchDate(): kotlinx.datetime.LocalDate? =
        firstLaunch?.let { runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull() }

    override fun writeFirstLaunchDate(date: kotlinx.datetime.LocalDate) {
        firstLaunch = date.toString()
    }

    override fun readDay1Sent(): Boolean = day1Sent
    override fun writeDay1Sent(sent: Boolean) {
        day1Sent = sent
    }

    override fun readDay7Sent(): Boolean = day7Sent
    override fun writeDay7Sent(sent: Boolean) {
        day7Sent = sent
    }
}
