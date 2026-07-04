package ru.kkalscan.analytics

import kotlinx.datetime.LocalDate

expect fun createLaunchRetentionStorage(): LaunchRetentionStorage

internal class PersistentLaunchRetentionStorage(
    private val readFirstLaunch: () -> String?,
    private val writeFirstLaunch: (String) -> Unit,
    private val readDay1: () -> Boolean,
    private val writeDay1: (Boolean) -> Unit,
    private val readDay7: () -> Boolean,
    private val writeDay7: (Boolean) -> Unit,
) : LaunchRetentionStorage {
    override fun readFirstLaunchDate(): LocalDate? =
        readFirstLaunch()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    override fun writeFirstLaunchDate(date: LocalDate) {
        writeFirstLaunch(date.toString())
    }

    override fun readDay1Sent(): Boolean = readDay1()
    override fun writeDay1Sent(sent: Boolean) = writeDay1(sent)
    override fun readDay7Sent(): Boolean = readDay7()
    override fun writeDay7Sent(sent: Boolean) = writeDay7(sent)
}
