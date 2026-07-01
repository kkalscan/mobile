package ru.kkalscan.app

import ru.kkalscan.AppDependencies

actual fun createAppDependencies(): AppDependencies =
    AppDependencies(apiConfig = appApiConfig())
