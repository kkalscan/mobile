package ru.kkalscan.navigation

data class DeepLinkRoute(
    val host: String,
    val path: String = "",
)

fun parseDeepLink(raw: String): DeepLinkRoute? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val withoutScheme = when {
        trimmed.startsWith("kkalscan://", ignoreCase = true) -> trimmed.substringAfter("://")
        trimmed.startsWith("/") -> trimmed.removePrefix("/")
        else -> trimmed
    }
    val parts = withoutScheme.split('/').filter { it.isNotBlank() }
    if (parts.isEmpty()) return null
    return DeepLinkRoute(host = parts.first().lowercase(), path = parts.drop(1).joinToString("/").lowercase())
}
