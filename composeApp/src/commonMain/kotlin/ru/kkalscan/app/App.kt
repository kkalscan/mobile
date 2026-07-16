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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.kkalscan.analytics.AnalyticsEvents
import ru.kkalscan.analytics.LaunchRetentionTracker
import ru.kkalscan.analytics.createLaunchRetentionStorage
import ru.kkalscan.app.analytics.KkalAnalytics
import ru.kkalscan.app.navigation.AppRootContent
import ru.kkalscan.app.platform.PlatformForegroundEffect
import ru.kkalscan.app.theme.KkalScanTheme

@Composable
fun App(componentContext: ComponentContext = remember {
    DefaultComponentContext(lifecycle = LifecycleRegistry())
}) {
    val scope = rememberCoroutineScope()
    val deps = remember { createAppDependencies() }
    val deviceId = remember { deps.deviceIdStorage.getDeviceId() }
    val diaryViewModel = remember(deps, scope) { deps.diaryViewModel(scope) }
    val journalViewModel = remember(deps, scope, diaryViewModel) {
        deps.journalViewModel(scope, todayPatchProvider = { diaryViewModel.journalDayPatch() })
    }
    val scanViewModel = remember(deps, scope) { deps.scanViewModel(scope) }
    val profileViewModel = remember(deps, scope) { deps.profileViewModel(scope) }

    val featureSearchViewModel = remember(deps, scope) {
        deps.featureSearchViewModel(
            scope = scope,
            onSearchCompleted = { query, resultsCount ->
                KkalAnalytics.reportAction(
                    AnalyticsEvents.FEATURE_SEARCH_QUERY,
                    mapOf(
                        "query" to query.take(200),
                        "query_length" to query.length.toString(),
                        "results" to resultsCount.toString(),
                        "empty_query" to query.isBlank().toString(),
                    ),
                )
            },
            onFoodIntentAnalytics = { queryLength, isFood ->
                KkalAnalytics.reportAction(
                    AnalyticsEvents.FEATURE_SEARCH_FOOD_INTENT,
                    mapOf(
                        "query_length" to queryLength.toString(),
                        "is_food" to isFood.toString(),
                    ),
                )
            },
        )
    }

    LaunchedEffect(deps) {
        KkalAnalytics.setDeviceId(deviceId)
        KkalAnalytics.reportAppLaunch()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        LaunchRetentionTracker(createLaunchRetentionStorage(), KkalAnalytics::reportAction)
            .onAppLaunch(today)
    }

    // When the app comes back from a long background stay, roll "today" over
    // to the current calendar day instead of showing yesterday until restart.
    PlatformForegroundEffect {
        scope.launch { diaryViewModel.onForeground() }
    }

    KkalScanTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppRootContent(
                componentContext = componentContext,
                diaryViewModel = diaryViewModel,
                journalViewModel = journalViewModel,
                scanViewModel = scanViewModel,
                profileViewModel = profileViewModel,
                featureSearchViewModel = featureSearchViewModel,
                scope = scope,
                apiConfig = deps.apiConfig,
                deviceId = deviceId,
                hasLoggedAnything = { deps.hasLoggedAnythingStorage.hasLoggedAnything() },
            )
        }
    }
}
