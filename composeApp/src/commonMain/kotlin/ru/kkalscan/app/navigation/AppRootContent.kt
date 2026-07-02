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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kkalscan.app.analytics.KkalAnalytics
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
import ru.kkalscan.app.ui.features.FeatureSearchBar
import ru.kkalscan.app.ui.food.FoodSearchSheet
import ru.kkalscan.app.ui.journal.JournalScreen
import ru.kkalscan.app.ui.paywall.PaywallScreen
import ru.kkalscan.app.ui.profile.ProfileScreen
import ru.kkalscan.app.ui.result.AddToDiaryDialog
import ru.kkalscan.app.ui.scan.ScanScreen
import ru.kkalscan.navigation.resolveDeepLinkNavigation
import ru.kkalscan.presentation.features.IFeatureSearchViewModel
import ru.kkalscan.presentation.food.IFoodSearchViewModel
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
    foodSearchViewModel: IFoodSearchViewModel,
    featureSearchViewModel: IFeatureSearchViewModel,
    scope: CoroutineScope,
    apiConfig: IApiConfig,
    deviceId: String,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Diary) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    var showFoodSearch by rememberSaveable { mutableStateOf(false) }
    var journalScrollAnchor by rememberSaveable { mutableStateOf<String?>(null) }
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
        scope.launch {
            runCatching {
                profileViewModel.startProSubscription()
            }.onSuccess { result ->
                val paymentUrl = result.paymentUrl
                if (result.paymentRequired && !paymentUrl.isNullOrBlank()) {
                    openProPayment(paymentUrl)
                    repeat(20) {
                        delay(3_000)
                        profileViewModel.refresh()
                        diaryViewModel.refresh()
                        if (profileViewModel.state.value.status?.isPro == true) {
                            KkalAnalytics.reportAction("subscription_start")
                            scanViewModel.onProActivated()
                            screen = AppScreen.Diary
                            selectedTab = AppTab.Today
                            return@launch
                        }
                    }
                } else if (result.isPro) {
                    KkalAnalytics.reportAction("subscription_start")
                    profileViewModel.refresh()
                    diaryViewModel.refresh()
                    scanViewModel.onProActivated()
                    screen = AppScreen.Diary
                    selectedTab = AppTab.Today
                }
            }
        }
    }

    val runScan: (ByteArray) -> Unit = { bytes ->
        KkalAnalytics.reportAction("photo_scan")
        scope.launch {
            scanViewModel.scanPhoto(bytes)
            val stateAfterScan = scanViewModel.state.value
            if (stateAfterScan.limitHit) {
                KkalAnalytics.reportAction(
                    "limit_hit",
                    mapOf("scans_left" to stateAfterScan.scansLeft.orEmptyAnalyticsValue()),
                )
                screen = AppScreen.Paywall
            } else if (stateAfterScan.result != null) {
                reportScanSuccess(stateAfterScan.scansLeft)
            } else if (stateAfterScan.errorMessage != null) {
                KkalAnalytics.reportAction(
                    "scan_error",
                    mapOf("reason" to stateAfterScan.errorMessage.analyticsReason()),
                )
            }
        }
    }

    val pickPhoto = rememberPhotoPicker { bytes ->
        if (bytes != null && !scanState.isLoading) {
            KkalAnalytics.reportAction("photo_selected")
            runScan(bytes)
        } else if (bytes == null) {
            KkalAnalytics.reportAction("photo_picker_cancel")
        }
    }

    val openDeepLink: (String) -> Unit = { deeplink ->
        KkalAnalytics.reportAction("deeplink_open", mapOf("link" to deeplink))
        resolveDeepLinkNavigation(deeplink)?.let { effect ->
            effect.toAppTab()?.let { selectedTab = it }
            screen = effect.toAppScreen()
            journalScrollAnchor = effect.journalScrollAnchor
            if (effect.openFoodSearch) showFoodSearch = true
            if (effect.triggerScan && !scanState.isLoading) pickPhoto()
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
            scanViewModel.reset()
            selectedTab = AppTab.Today
            screen = AppScreen.Diary
        },
    )

    MaestroDevBridge(
        onStubScan = {
            val state = scanViewModel.state.value
            if (!state.isLoading) {
                KkalAnalytics.reportAction("dev_stub_scan")
                val bytes = devStubScanPhotoBytes() ?: byteArrayOf(1, 2, 3)
                runScan(bytes)
            }
        },
        onConfirmAdd = {
            scanViewModel.launchAddToDiary {
                scope.launch { refreshAfterDiaryAdd(diaryViewModel, journalViewModel, profileViewModel) }
                scanViewModel.reset()
            }
        },
        onGramsPlus = { scanViewModel.adjustDishGrams(0, DishPortion.STEP_GRAMS) },
        onGramsMinus = { scanViewModel.adjustDishGrams(0, -DishPortion.STEP_GRAMS) },
        onPortionHalf = { scanViewModel.scaleDishFromBaseline(0, 0.5) },
        onPortionDouble = { scanViewModel.scaleDishFromBaseline(0, 2.0) },
        onOpenFoodSearch = {
            KkalAnalytics.reportAction("feature_search_open")
            featureSearchViewModel.onQueryChange("")
        },
        onFoodSearchDemo = {
            KkalAnalytics.reportAction("feature_search_open")
            featureSearchViewModel.onQueryChange("профиль")
        },
        onFoodSearchAddFirst = {
            foodSearchViewModel.launchAddFirstResult()
        },
        onDeepLinkProfile = { openDeepLink("kkalscan://profile") },
        onDeepLinkJournal = { openDeepLink("kkalscan://journal") },
        onDeepLinkDiary = { openDeepLink("kkalscan://diary") },
        onFeatureSearchOpenFirst = {
            featureSearchViewModel.state.value.results.firstOrNull()?.deeplink?.let { link ->
                featureSearchViewModel.clear()
                openDeepLink(link)
            }
        },
    )

    val showBottomBar = screen == AppScreen.Diary || screen == AppScreen.Journal || screen == AppScreen.Profile

    KkalScreenScaffold(
        hasBottomBar = showBottomBar,
        topBar = {
            FeatureSearchBar(
                viewModel = featureSearchViewModel,
                onOpenDeepLink = openDeepLink,
            )
        },
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
                MaestroScreenHook("diary-screen feature-search-bar")
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
                MaestroScreenHook("journal-screen feature-search-bar")
                JournalScreen(
                    viewModel = journalViewModel,
                    scrollAnchor = journalScrollAnchor,
                    onScrollAnchorConsumed = { journalScrollAnchor = null },
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
                MaestroScreenHook("profile-screen feature-search-bar")
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
                MaestroScreenHook("scan-screen feature-search-bar")
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
                MaestroScreenHook("paywall-screen feature-search-bar")
                PaywallScreen(
                    scansLeft = scanState.scansLeft,
                    onWatchAd = {
                        KkalAnalytics.reportAction("ad_bonus_click")
                        scope.launch {
                            scanViewModel.grantAdBonus()
                            val stateAfterBonus = scanViewModel.state.value
                            if (stateAfterBonus.limitHit || stateAfterBonus.errorMessage != null) {
                                KkalAnalytics.reportAction(
                                    "ad_bonus_failed",
                                    mapOf("reason" to stateAfterBonus.errorMessage.analyticsReason()),
                                )
                                return@launch
                            }
                            KkalAnalytics.reportAction(
                                "ad_watch_complete",
                                mapOf("scans_left" to stateAfterBonus.scansLeft.orEmptyAnalyticsValue()),
                            )
                            profileViewModel.refresh()
                            if (!stateAfterBonus.limitHit) {
                                pickPhoto()
                            }
                        }
                    },
                    onBuyPro = startProPayment,
                    onBack = {
                        KkalAnalytics.reportAction("paywall_back")
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
                    val ok = scanViewModel.addToDiary().isSuccess
                    if (!ok) {
                        KkalAnalytics.reportAction(
                            "add_to_diary_failed",
                            mapOf("reason" to scanViewModel.state.value.errorMessage.analyticsReason()),
                        )
                    }
                    ok
                },
                onAddFinished = { scanViewModel.reset() },
                onRefreshAfterAdd = {
                    scope.launch {
                        refreshAfterDiaryAdd(diaryViewModel, journalViewModel, profileViewModel)
                    }
                },
            )
        }

        if (showFoodSearch) {
            MaestroScreenHook("food-search-sheet")
            FoodSearchSheet(
                viewModel = foodSearchViewModel,
                onDismiss = {
                    showFoodSearch = false
                    foodSearchViewModel.clear()
                },
                onAdded = {
                    KkalAnalytics.reportAction("food_search_add")
                    scope.launch {
                        diaryViewModel.refresh()
                        journalViewModel.refresh()
                    }
                },
            )
        }
    }
}

private fun Int?.orEmptyAnalyticsValue(): String = this?.toString() ?: "unknown"

private fun String?.analyticsReason(): String = this?.takeIf { it.isNotBlank() } ?: "unknown"

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

private suspend fun confirmAddToDiary(
    scanViewModel: IScanViewModel,
    diaryViewModel: IDiaryViewModel,
    journalViewModel: IJournalViewModel,
    profileViewModel: IProfileViewModel,
) {
    KkalAnalytics.reportAction("add_to_diary")
    if (scanViewModel.addToDiary().isFailure) {
        KkalAnalytics.reportAction(
            "add_to_diary_failed",
            mapOf("reason" to scanViewModel.state.value.errorMessage.analyticsReason()),
        )
        return
    }
    refreshAfterDiaryAdd(diaryViewModel, journalViewModel, profileViewModel)
    scanViewModel.reset()
}

private suspend fun refreshAfterDiaryAdd(
    diaryViewModel: IDiaryViewModel,
    journalViewModel: IJournalViewModel,
    profileViewModel: IProfileViewModel,
) {
    coroutineScope {
        launch { diaryViewModel.refresh() }
        launch { journalViewModel.refresh() }
        launch { profileViewModel.refresh() }
    }
}
