package ru.kkalscan.app.platform

fun buildProPayUrl(webBaseUrl: String, deviceId: String): String =
    "${webBaseUrl.trimEnd('/')}/pay?device_id=$deviceId"
