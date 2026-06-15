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
import ru.kkalscan.app.navigation.AppRootContent
import ru.kkalscan.app.theme.KkalScanTheme

@Composable
fun App(componentContext: ComponentContext = remember {
    DefaultComponentContext(lifecycle = LifecycleRegistry())
}) {
    val scope = rememberCoroutineScope()
    val deps = remember { AppDependencies(apiConfig = appApiConfig()) }
    val diaryViewModel = remember(deps, scope) { deps.diaryViewModel(scope) }
    val journalViewModel = remember(deps, scope) { deps.journalViewModel(scope) }
    val scanViewModel = remember(deps, scope) { deps.scanViewModel(scope) }
    val profileViewModel = remember(deps, scope) { deps.profileViewModel(scope) }

    LaunchedEffect(deps) {
        deps.deviceIdStorage.getDeviceId()
    }

    KkalScanTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppRootContent(
                componentContext = componentContext,
                diaryViewModel = diaryViewModel,
                journalViewModel = journalViewModel,
                scanViewModel = scanViewModel,
                profileViewModel = profileViewModel,
                scope = scope,
            )
        }
    }
}
