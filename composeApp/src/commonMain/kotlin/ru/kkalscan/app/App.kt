package ru.kkalscan.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import ru.kkalscan.AppDependencies
import ru.kkalscan.app.analytics.KkalAnalytics
import ru.kkalscan.app.navigation.AppRootContent
import ru.kkalscan.app.theme.KkalScanTheme

@Composable
fun App(componentContext: ComponentContext = remember {
    DefaultComponentContext(lifecycle = LifecycleRegistry())
}) {
    val scope = rememberCoroutineScope()
    val deps = remember { createAppDependencies() }
    val deviceId = remember { deps.deviceIdStorage.getDeviceId() }
    val diaryViewModel = remember(deps, scope) { deps.diaryViewModel(scope) }
    val journalViewModel = remember(deps, scope) { deps.journalViewModel(scope) }
    val scanViewModel = remember(deps, scope) { deps.scanViewModel(scope) }
    val profileViewModel = remember(deps, scope) { deps.profileViewModel(scope) }

    val foodSearchViewModel = remember(deps, scope) { deps.foodSearchViewModel(scope) }
    val featureSearchViewModel = remember(deps, scope) {
        deps.featureSearchViewModel(scope) { query, resultsCount ->
            KkalAnalytics.reportAction(
                "feature_search_query",
                mapOf(
                    "query" to query.take(200),
                    "query_length" to query.length.toString(),
                    "results" to resultsCount.toString(),
                    "empty_query" to query.isBlank().toString(),
                ),
            )
        }
    }

    LaunchedEffect(deps) {
        KkalAnalytics.setDeviceId(deviceId)
        KkalAnalytics.reportAppLaunch()
    }

    KkalScanTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppRootContent(
                componentContext = componentContext,
                diaryViewModel = diaryViewModel,
                journalViewModel = journalViewModel,
                scanViewModel = scanViewModel,
                profileViewModel = profileViewModel,
                foodSearchViewModel = foodSearchViewModel,
                featureSearchViewModel = featureSearchViewModel,
                scope = scope,
                apiConfig = deps.apiConfig,
                deviceId = deviceId,
            )
        }
    }
}
