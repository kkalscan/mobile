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
import ru.kkalscan.app.platform.MaestroDevBridge
import ru.kkalscan.app.platform.MaestroNavigationBridge
import ru.kkalscan.app.platform.MaestroScreenHook
import ru.kkalscan.app.platform.rememberPhotoPicker
import ru.kkalscan.app.ui.diary.DiaryScreen
import ru.kkalscan.app.ui.journal.JournalScreen
import ru.kkalscan.app.ui.paywall.PaywallScreen
import ru.kkalscan.app.ui.profile.ProfileScreen
import ru.kkalscan.app.ui.result.AddToDiaryDialog
import ru.kkalscan.app.ui.scan.ScanScreen
import ru.kkalscan.presentation.diary.IDiaryViewModel
import ru.kkalscan.presentation.journal.IJournalViewModel
import ru.kkalscan.presentation.journal.InsightRequestResult
import ru.kkalscan.presentation.profile.IProfileViewModel
import ru.kkalscan.presentation.scan.IScanViewModel

@Composable
fun AppRootContent(
    componentContext: ComponentContext,
    diaryViewModel: IDiaryViewModel,
    journalViewModel: IJournalViewModel,
    scanViewModel: IScanViewModel,
    profileViewModel: IProfileViewModel,
    scope: CoroutineScope,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Diary) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    val scanState by scanViewModel.state.collectAsState()

    val runScan: (ByteArray) -> Unit = { bytes ->
        scope.launch {
            scanViewModel.scanPhoto(bytes)
            if (scanViewModel.state.value.limitHit) {
                screen = AppScreen.Paywall
            }
        }
    }

    val pickPhoto = rememberPhotoPicker { bytes ->
        if (bytes != null && !scanState.isLoading) {
            runScan(bytes)
        }
    }

    MaestroNavigationBridge(
        onOpenScan = pickPhoto,
        onOpenProfile = {
            selectedTab = AppTab.Profile
            screen = AppScreen.Profile
        },
        onOpenJournal = {
            selectedTab = AppTab.Journal
            screen = AppScreen.Journal
        },
        onScanBack = {
            selectedTab = AppTab.Today
            screen = AppScreen.Diary
        },
    )

    MaestroDevBridge(
        onStubScan = { if (!scanState.isLoading) runScan(byteArrayOf(1, 2, 3)) },
        onConfirmAdd = {
            if (scanState.result != null && !scanState.isSaving) {
                scope.launch {
                    if (scanViewModel.addToDiary().isFailure) return@launch
                    diaryViewModel.refresh()
                    journalViewModel.refresh()
                    profileViewModel.refresh()
                    scanViewModel.reset()
                }
            }
        },
    )

    val showBottomBar = screen == AppScreen.Diary || screen == AppScreen.Journal || screen == AppScreen.Profile

    KkalScreenScaffold(
        bottomBar = {
            if (showBottomBar) {
                KkalBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        screen = when (tab) {
                            AppTab.Today -> AppScreen.Diary
                            AppTab.Journal -> AppScreen.Journal
                            AppTab.Profile -> AppScreen.Profile
                        }
                    },
                    onScanClick = { if (!scanState.isLoading) pickPhoto() },
                    scanLoading = scanState.isLoading,
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

            AppScreen.Journal -> {
                MaestroScreenHook("journal-screen")
                JournalScreen(
                    viewModel = journalViewModel,
                    onRefresh = { scope.launch { journalViewModel.refresh() } },
                    onRequestInsight = {
                        scope.launch {
                            when (journalViewModel.requestDietitianInsight()) {
                                InsightRequestResult.NeedPro -> screen = AppScreen.Paywall
                                else -> Unit
                            }
                        }
                    },
                    onNeedPro = { screen = AppScreen.Paywall },
                    onDismissInsight = { journalViewModel.clearInsight() },
                )
            }

            AppScreen.Profile -> {
                MaestroScreenHook("profile-screen")
                ProfileScreen(
                    viewModel = profileViewModel,
                    onRefresh = { scope.launch { profileViewModel.refresh() } },
                    onBuyPro = { },
                    onSubmitBugReport = { email, description, screenshots ->
                        scope.launch {
                            profileViewModel.submitBugReport(email, description, screenshots)
                        }
                    },
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
                        selectedTab = AppTab.Today
                        screen = AppScreen.Diary
                    },
                )
            }

            AppScreen.Result -> {
                selectedTab = AppTab.Today
                screen = AppScreen.Diary
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
                        selectedTab = AppTab.Today
                        screen = AppScreen.Diary
                    },
                )
            }
        }

        if (scanState.result != null) {
            MaestroScreenHook("scan-result-dialog")
            AddToDiaryDialog(
                viewModel = scanViewModel,
                onDismiss = { scanViewModel.reset() },
                onConfirm = {
                    scope.launch {
                        if (scanViewModel.addToDiary().isFailure) return@launch
                        diaryViewModel.refresh()
                        journalViewModel.refresh()
                        profileViewModel.refresh()
                        scanViewModel.reset()
                    }
                },
            )
        }
    }
}
