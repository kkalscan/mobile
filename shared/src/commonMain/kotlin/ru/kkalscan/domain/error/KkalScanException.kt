package ru.kkalscan.domain.error

sealed class KkalScanException(message: String) : Exception(message) {
    class Network(message: String) : KkalScanException(message)
    class LimitHit(val scansLeft: Int) : KkalScanException(message = "На сегодня бесплатные сканы закончились")
    class Api(message: String) : KkalScanException(message)
}
