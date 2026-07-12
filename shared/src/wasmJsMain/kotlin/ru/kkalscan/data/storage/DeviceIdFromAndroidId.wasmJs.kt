package ru.kkalscan.data.storage

/**
 * Deterministic stand-in for web (no ANDROID_ID). Not cryptographically UUID v3 —
 * only needed so the shared expect API stays stable.
 */
internal actual fun deviceIdFromAndroidId(androidId: String): String {
    var h1 = 0xCBF29CE484222325UL
    var h2 = 0x100000001B3UL
    for (c in "kkalscan:$androidId") {
        h1 = h1 xor c.code.toULong()
        h1 *= 0x100000001B3UL
        h2 = h2 xor c.code.toULong()
        h2 *= 0x100000001B3UL
        h2 = h2.rotateLeft(7)
    }
    fun hex(v: ULong, n: Int) = v.toString(16).padStart(n, '0').takeLast(n)
    return "${hex(h1, 8)}-${hex(h1 shr 32, 4)}-4${hex(h2, 3)}-8${hex(h2 shr 12, 3)}-${hex(h2 shr 24, 12)}"
}
