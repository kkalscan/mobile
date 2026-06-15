package ru.kkalscan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import ru.kkalscan.app.ui.profile.ProfileScreen
import ru.kkalscan.app.ui.result.ResultScreen
import ru.kkalscan.app.ui.scan.ScanScreen
import ru.kkalscan.presentation.diary.IDiaryViewModel
import ru.kkalscan.presentation.profile.IProfileViewModel
import ru.kkalscan.presentation.scan.IScanViewModel

@Composable
fun AppRootContent(
    componentContext: ComponentContext,
    diaryViewModel: IDiaryViewModel,
    scanViewModel: IScanViewModel,
    profileViewModel: IProfileViewModel,
    scope: CoroutineScope,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Diary) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Diary) }
    val scanState by scanViewModel.state.collectAsState()

    val pickPhoto = rememberPhotoPicker { bytes ->
        if (bytes != null) {
            scope.launch {
                scanViewModel.scanPhoto(bytes)
                screen = when {
                    scanViewModel.state.value.limitHit -> AppScreen.Paywall
                    scanViewModel.state.value.result != null -> AppScreen.Result
                    else -> if (selectedTab == AppTab.Profile) AppScreen.Profile else AppScreen.Diary
                }
            }
        }
    }

    MaestroNavigationBridge(
        onOpenScan = pickPhoto,
        onOpenProfile = {
            selectedTab = AppTab.Profile
            screen = AppScreen.Profile
        },
        onScanBack = {
            selectedTab = AppTab.Diary
            screen = AppScreen.Diary
        },
    )

    val showBottomBar = screen == AppScreen.Diary || screen == AppScreen.Profile

    KkalScreenScaffold(
        bottomBar = {
            if (showBottomBar) {
                KkalBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        screen = when (tab) {
                            AppTab.Diary -> AppScreen.Diary
                            AppTab.Profile -> AppScreen.Profile
                        }
                    },
                    onScanClick = pickPhoto,
                )
            }
        },
    ) {
        when (screen) {
            AppScreen.Diary -> {
                MaestroScreenHook("diary-screen")
                DiaryScreen(
                    viewModel = diaryViewModel,
                    onScanClick = pickPhoto,
                    onRefresh = { scope.launch { diaryViewModel.refresh() } },
                    scanErrorMessage = scanState.errorMessage,
                    onRetryScan = pickPhoto,
                )
            }

            AppScreen.Profile -> {
                MaestroScreenHook("profile-screen")
                ProfileScreen(
                    viewModel = profileViewModel,
                    onRefresh = { scope.launch { profileViewModel.refresh() } },
                    onBuyPro = { },
                    scanErrorMessage = scanState.errorMessage,
                    onRetryScan = pickPhoto,
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
                            profileViewModel.refresh()
                            scanViewModel.reset()
                            selectedTab = AppTab.Diary
                            screen = AppScreen.Diary
                        }
                    },
                    onBack = {
                        scanViewModel.reset()
                        screen = if (selectedTab == AppTab.Profile) AppScreen.Profile else AppScreen.Diary
                    },
                )
            }

            AppScreen.Paywall -> {
                MaestroScreenHook("paywall-screen")
                PaywallScreen(
                    scansLeft = scanState.scansLeft,
                    onWatchAd = {
                        scope.launch {
                            scanViewModel.grantAdBonus()
                            profileViewModel.refresh()
                            if (!scanViewModel.state.value.limitHit) {
                                pickPhoto()
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
