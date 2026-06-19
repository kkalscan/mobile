package ru.kkalscan.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kkalscan.app.analytics.KkalAnalytics
import ru.kkalscan.app.platform.buildProPayUrl
import ru.kkalscan.app.platform.rememberProPaymentOpener
import ru.kkalscan.data.IApiConfig
import ru.kkalscan.app.components.AppTab
import ru.kkalscan.app.components.KkalBottomBar
import ru.kkalscan.app.components.KkalScreenScaffold
import ru.kkalscan.app.platform.MaestroDevBridge
import ru.kkalscan.app.platform.MaestroNavigationBridge
import ru.kkalscan.app.platform.MaestroScreenHook
import ru.kkalscan.app.platform.devStubScanPhotoBytes
import ru.kkalscan.app.platform.rememberPhotoPicker
import ru.kkalscan.domain.model.DishPortion
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
    apiConfig: IApiConfig,
    deviceId: String,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Diary) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    val scanState by scanViewModel.state.collectAsState()
    val openProPayment = rememberProPaymentOpener()

    LaunchedEffect(screen) {
        KkalAnalytics.reportFeatureOpen(screen.analyticsFeatureName())
        if (screen == AppScreen.Paywall) {
            KkalAnalytics.reportAction("paywall_shown")
            KkalAnalytics.reportAction("ad_offer_shown")
        }
    }

    val startProPayment: () -> Unit = {
        KkalAnalytics.reportAction("pro_click")
        openProPayment(buildProPayUrl(apiConfig.webBaseUrl, deviceId))
        scope.launch {
            repeat(20) {
                delay(3_000)
                profileViewModel.refresh()
                diaryViewModel.refresh()
                if (profileViewModel.state.value.status?.isPro == true) {
                    scanViewModel.onProActivated()
                    screen = AppScreen.Diary
                    selectedTab = AppTab.Today
                    return@launch
                }
            }
        }
    }

    val runScan: (ByteArray) -> Unit = { bytes ->
        KkalAnalytics.reportAction("photo_scan")
        scope.launch {
            scanViewModel.scanPhoto(bytes)
            if (scanViewModel.state.value.limitHit) {
                KkalAnalytics.reportAction("limit_hit")
                screen = AppScreen.Paywall
            } else if (scanViewModel.state.value.result != null) {
                reportScanSuccess(scanViewModel.state.value.scansLeft)
            }
        }
    }

    val pickPhoto = rememberPhotoPicker { bytes ->
        if (bytes != null && !scanState.isLoading) {
            KkalAnalytics.reportAction("photo_selected")
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
        onStubScan = {
            if (!scanState.isLoading) {
                KkalAnalytics.reportAction("dev_stub_scan")
                val bytes = devStubScanPhotoBytes() ?: byteArrayOf(1, 2, 3)
                runScan(bytes)
            }
        },
        onConfirmAdd = {
            if (scanState.result != null && !scanState.isSaving) {
                KkalAnalytics.reportAction("add_to_diary")
                scope.launch {
                    if (scanViewModel.addToDiary().isFailure) return@launch
                    diaryViewModel.refresh()
                    journalViewModel.refresh()
                    profileViewModel.refresh()
                    scanViewModel.reset()
                }
            }
        },
        onGramsPlus = { scanViewModel.adjustDishGrams(0, DishPortion.STEP_GRAMS) },
        onGramsMinus = { scanViewModel.adjustDishGrams(0, -DishPortion.STEP_GRAMS) },
        onPortionHalf = { scanViewModel.scaleDishFromBaseline(0, 0.5) },
        onPortionDouble = { scanViewModel.scaleDishFromBaseline(0, 2.0) },
    )

    val showBottomBar = screen == AppScreen.Diary || screen == AppScreen.Journal || screen == AppScreen.Profile

    KkalScreenScaffold(
        hasBottomBar = showBottomBar,
        bottomBar = {
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
                    onScanClick = {
                        if (!scanState.isLoading) {
                            KkalAnalytics.reportAction("scan_open")
                            pickPhoto()
                        }
                    },
                scanLoading = scanState.isLoading,
            )
        },
    ) {
        when (screen) {
            AppScreen.Diary -> {
                MaestroScreenHook("diary-screen")
                DiaryScreen(
                    viewModel = diaryViewModel,
                    onScanClick = {
                        KkalAnalytics.reportAction("scan_open")
                        pickPhoto()
                    },
                    onRefresh = { scope.launch { diaryViewModel.refresh() } },
                    scanErrorMessage = scanState.errorMessage,
                    onRetryScan = {
                        KkalAnalytics.reportAction("scan_retry")
                        pickPhoto()
                    },
                )
            }

            AppScreen.Journal -> {
                MaestroScreenHook("journal-screen")
                JournalScreen(
                    viewModel = journalViewModel,
                    onRefresh = { scope.launch { journalViewModel.refresh() } },
                    onRequestInsight = {
                        KkalAnalytics.reportAction("dietitian_insight_click")
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
                    onBuyPro = startProPayment,
                    onSubmitBugReport = { email, description, screenshots ->
                        scope.launch {
                            profileViewModel.submitBugReport(email, description, screenshots)
                        }
                    },
                    scanErrorMessage = scanState.errorMessage,
                    onRetryScan = {
                        KkalAnalytics.reportAction("scan_retry")
                        pickPhoto()
                    },
                )
            }

            AppScreen.Scan -> {
                MaestroScreenHook("scan-screen")
                ScanScreen(
                    viewModel = scanViewModel,
                    onPickPhoto = {
                        KkalAnalytics.reportAction("scan_open")
                        pickPhoto()
                    },
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
                        KkalAnalytics.reportAction("ad_bonus_click")
                        scope.launch {
                            scanViewModel.grantAdBonus()
                            KkalAnalytics.reportAction("ad_watch_complete")
                            profileViewModel.refresh()
                            if (!scanViewModel.state.value.limitHit) {
                                pickPhoto()
                            }
                        }
                    },
                    onBuyPro = startProPayment,
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
                    KkalAnalytics.reportAction("add_to_diary")
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

private fun AppScreen.analyticsFeatureName(): String = when (this) {
    AppScreen.Diary -> "diary"
    AppScreen.Journal -> "journal"
    AppScreen.Profile -> "profile"
    AppScreen.Scan -> "scan"
    AppScreen.Result -> "result"
    AppScreen.Paywall -> "paywall"
}

private fun reportScanSuccess(scansLeft: Int?) {
    KkalAnalytics.reportAction("scan_success")
    when (scansLeft) {
        3 -> KkalAnalytics.reportAction("first_scan_success")
        2 -> KkalAnalytics.reportAction("second_scan_success")
        1 -> KkalAnalytics.reportAction("third_scan_success")
    }
}
