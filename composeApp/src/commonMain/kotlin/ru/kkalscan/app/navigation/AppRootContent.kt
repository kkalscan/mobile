package ru.kkalscan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.kkalscan.app.components.AppTab
import ru.kkalscan.app.components.KkalBottomBar
import ru.kkalscan.app.components.KkalScreenScaffold
import ru.kkalscan.app.platform.MaestroNavigationBridge
import ru.kkalscan.app.platform.MaestroScreenHook
import ru.kkalscan.app.platform.rememberPhotoPicker
import ru.kkalscan.app.ui.diary.DiaryScreen
import ru.kkalscan.app.ui.paywall.PaywallScreen
import ru.kkalscan.app.ui.result.ResultScreen
import ru.kkalscan.app.ui.scan.ScanScreen
import ru.kkalscan.presentation.diary.IDiaryViewModel
import ru.kkalscan.presentation.scan.IScanViewModel

@Composable
fun AppRootContent(
    componentContext: ComponentContext,
    diaryViewModel: IDiaryViewModel,
    scanViewModel: IScanViewModel,
    scope: CoroutineScope,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Diary) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Diary) }

    MaestroNavigationBridge(
        onOpenScan = {
            selectedTab = AppTab.Scan
            screen = AppScreen.Scan
        },
        onScanBack = {
            selectedTab = AppTab.Diary
            screen = AppScreen.Diary
        },
    )

    val pickPhoto = rememberPhotoPicker { bytes ->
        if (bytes != null) {
            scope.launch {
                scanViewModel.scanPhoto(bytes)
                screen = when {
                    scanViewModel.state.value.limitHit -> AppScreen.Paywall
                    scanViewModel.state.value.result != null -> AppScreen.Result
                    else -> AppScreen.Scan
                }
            }
        }
    }

    val showBottomBar = screen == AppScreen.Diary || screen == AppScreen.Scan

    KkalScreenScaffold(
        bottomBar = {
            if (showBottomBar) {
                KkalBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        screen = when (tab) {
                            AppTab.Diary -> AppScreen.Diary
                            AppTab.Scan -> AppScreen.Scan
                        }
                    },
                )
            }
        },
    ) {
        when (screen) {
            AppScreen.Diary -> {
                MaestroScreenHook("diary-screen")
                DiaryScreen(
                    viewModel = diaryViewModel,
                    onScanClick = {
                        selectedTab = AppTab.Scan
                        screen = AppScreen.Scan
                    },
                    onRefresh = { scope.launch { diaryViewModel.refresh() } },
                )
            }

            AppScreen.Scan -> {
                MaestroScreenHook("scan-screen")
                ScanScreen(
                    viewModel = scanViewModel,
                    onPickPhoto = pickPhoto,
                    onBack = {
                        selectedTab = AppTab.Diary
                        screen = AppScreen.Diary
                    },
                )
            }

            AppScreen.Result -> {
                MaestroScreenHook("result-screen")
                ResultScreen(
                    viewModel = scanViewModel,
                    onAddToDiary = {
                        scope.launch {
                            scanViewModel.addToDiary()
                            diaryViewModel.refresh()
                            scanViewModel.reset()
                            selectedTab = AppTab.Diary
                            screen = AppScreen.Diary
                        }
                    },
                    onBack = {
                        scanViewModel.reset()
                        selectedTab = AppTab.Scan
                        screen = AppScreen.Scan
                    },
                )
            }

            AppScreen.Paywall -> {
                MaestroScreenHook("paywall-screen")
                PaywallScreen(
                    scansLeft = scanViewModel.state.value.scansLeft,
                    onWatchAd = {
                        scope.launch {
                            scanViewModel.grantAdBonus()
                            if (!scanViewModel.state.value.limitHit) {
                                selectedTab = AppTab.Scan
                                screen = AppScreen.Scan
                            }
                        }
                    },
                    onBuyPro = { },
                    onBack = {
                        selectedTab = AppTab.Diary
                        screen = AppScreen.Diary
                    },
                )
            }
        }
    }
}
