package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
actual fun MaestroDevBridge(
    onStubScan: () -> Unit,
    onConfirmAdd: () -> Unit,
    onGramsPlus: () -> Unit,
    onGramsMinus: () -> Unit,
    onPortionHalf: () -> Unit,
    onPortionDouble: () -> Unit,
    onOpenFoodSearch: () -> Unit,
    onOpenDescribeFood: () -> Unit,
    onDescribeFoodDemo: () -> Unit,
    onOpenDescribeWorkout: () -> Unit,
    onDescribeWorkoutDemo: () -> Unit,
    onConfirmWorkoutAdd: () -> Unit,
    onFoodSearchDemo: () -> Unit,
    onFoodSearchAddFirst: () -> Unit,
    onDeepLinkProfile: () -> Unit,
    onDeepLinkJournal: () -> Unit,
    onDeepLinkDiary: () -> Unit,
    onFeatureSearchOpenFirst: () -> Unit,
) {
    DisposableEffect(
        onStubScan,
        onConfirmAdd,
        onGramsPlus,
        onGramsMinus,
        onPortionHalf,
        onPortionDouble,
        onOpenFoodSearch,
        onOpenDescribeFood,
        onDescribeFoodDemo,
        onOpenDescribeWorkout,
        onDescribeWorkoutDemo,
        onConfirmWorkoutAdd,
        onFoodSearchDemo,
        onFoodSearchAddFirst,
        onDeepLinkProfile,
        onDeepLinkJournal,
        onDeepLinkDiary,
        onFeatureSearchOpenFirst,
    ) {
        val stubBtn = document.getElementById("maestro-stub-scan")
        val confirmBtn = document.getElementById("maestro-confirm-add")
        val plusBtn = document.getElementById("maestro-grams-plus")
        val minusBtn = document.getElementById("maestro-grams-minus")
        val halfBtn = document.getElementById("maestro-portion-half")
        val doubleBtn = document.getElementById("maestro-portion-double")
        val foodSearchBtn = document.getElementById("maestro-open-food-search")
        val describeFoodBtn = document.getElementById("maestro-open-describe-food")
        val describeFoodDemoBtn = document.getElementById("maestro-describe-food-demo")
        val describeWorkoutBtn = document.getElementById("maestro-open-describe-workout")
        val describeWorkoutDemoBtn = document.getElementById("maestro-describe-workout-demo")
        val confirmWorkoutBtn = document.getElementById("maestro-confirm-workout-add")
        val foodSearchDemoBtn = document.getElementById("maestro-food-search-demo")
        val foodSearchAddBtn = document.getElementById("maestro-food-search-add")
        val deeplinkProfileBtn = document.getElementById("maestro-deeplink-profile")
        val deeplinkJournalBtn = document.getElementById("maestro-deeplink-journal")
        val deeplinkDiaryBtn = document.getElementById("maestro-deeplink-diary")
        val featureSearchFirstBtn = document.getElementById("maestro-feature-search-first")
        val stubHandler: (Event) -> Unit = { onStubScan() }
        val confirmHandler: (Event) -> Unit = { onConfirmAdd() }
        val plusHandler: (Event) -> Unit = { onGramsPlus() }
        val minusHandler: (Event) -> Unit = { onGramsMinus() }
        val halfHandler: (Event) -> Unit = { onPortionHalf() }
        val doubleHandler: (Event) -> Unit = { onPortionDouble() }
        val foodSearchHandler: (Event) -> Unit = { onOpenFoodSearch() }
        val describeFoodHandler: (Event) -> Unit = { onOpenDescribeFood() }
        val describeFoodDemoHandler: (Event) -> Unit = { onDescribeFoodDemo() }
        val describeWorkoutHandler: (Event) -> Unit = { onOpenDescribeWorkout() }
        val describeWorkoutDemoHandler: (Event) -> Unit = { onDescribeWorkoutDemo() }
        val confirmWorkoutHandler: (Event) -> Unit = { onConfirmWorkoutAdd() }
        val foodSearchDemoHandler: (Event) -> Unit = { onFoodSearchDemo() }
        val foodSearchAddHandler: (Event) -> Unit = { onFoodSearchAddFirst() }
        val deeplinkProfileHandler: (Event) -> Unit = { onDeepLinkProfile() }
        val deeplinkJournalHandler: (Event) -> Unit = { onDeepLinkJournal() }
        val deeplinkDiaryHandler: (Event) -> Unit = { onDeepLinkDiary() }
        val featureSearchFirstHandler: (Event) -> Unit = { onFeatureSearchOpenFirst() }
        stubBtn?.addEventListener("click", stubHandler)
        confirmBtn?.addEventListener("click", confirmHandler)
        plusBtn?.addEventListener("click", plusHandler)
        minusBtn?.addEventListener("click", minusHandler)
        halfBtn?.addEventListener("click", halfHandler)
        doubleBtn?.addEventListener("click", doubleHandler)
        foodSearchBtn?.addEventListener("click", foodSearchHandler)
        describeFoodBtn?.addEventListener("click", describeFoodHandler)
        describeFoodDemoBtn?.addEventListener("click", describeFoodDemoHandler)
        describeWorkoutBtn?.addEventListener("click", describeWorkoutHandler)
        describeWorkoutDemoBtn?.addEventListener("click", describeWorkoutDemoHandler)
        confirmWorkoutBtn?.addEventListener("click", confirmWorkoutHandler)
        foodSearchDemoBtn?.addEventListener("click", foodSearchDemoHandler)
        foodSearchAddBtn?.addEventListener("click", foodSearchAddHandler)
        deeplinkProfileBtn?.addEventListener("click", deeplinkProfileHandler)
        deeplinkJournalBtn?.addEventListener("click", deeplinkJournalHandler)
        deeplinkDiaryBtn?.addEventListener("click", deeplinkDiaryHandler)
        featureSearchFirstBtn?.addEventListener("click", featureSearchFirstHandler)
        onDispose {
            stubBtn?.removeEventListener("click", stubHandler)
            confirmBtn?.removeEventListener("click", confirmHandler)
            plusBtn?.removeEventListener("click", plusHandler)
            minusBtn?.removeEventListener("click", minusHandler)
            halfBtn?.removeEventListener("click", halfHandler)
            doubleBtn?.removeEventListener("click", doubleHandler)
            foodSearchBtn?.removeEventListener("click", foodSearchHandler)
            describeFoodBtn?.removeEventListener("click", describeFoodHandler)
            describeFoodDemoBtn?.removeEventListener("click", describeFoodDemoHandler)
            describeWorkoutBtn?.removeEventListener("click", describeWorkoutHandler)
            describeWorkoutDemoBtn?.removeEventListener("click", describeWorkoutDemoHandler)
            confirmWorkoutBtn?.removeEventListener("click", confirmWorkoutHandler)
            foodSearchDemoBtn?.removeEventListener("click", foodSearchDemoHandler)
            foodSearchAddBtn?.removeEventListener("click", foodSearchAddHandler)
            deeplinkProfileBtn?.removeEventListener("click", deeplinkProfileHandler)
            deeplinkJournalBtn?.removeEventListener("click", deeplinkJournalHandler)
            deeplinkDiaryBtn?.removeEventListener("click", deeplinkDiaryHandler)
            featureSearchFirstBtn?.removeEventListener("click", featureSearchFirstHandler)
        }
    }
}
