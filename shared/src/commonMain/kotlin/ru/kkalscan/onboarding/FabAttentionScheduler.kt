package ru.kkalscan.onboarding

class FabAttentionScheduler(
    private val intervalMs: Long = 60_000,
) {
    fun shouldShow(
        nowMs: Long,
        hasLoggedAnything: Boolean,
        fabExpanded: Boolean,
        loading: Boolean,
        lastShownMs: Long?,
    ): Boolean {
        if (hasLoggedAnything || fabExpanded || loading) return false
        if (lastShownMs == null) return true
        return nowMs - lastShownMs >= intervalMs
    }
}
