package ru.kkalscan.app.ui.diary

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.kkalscan.app.theme.KkalScanTheme
import ru.kkalscan.data.storage.AndroidDeviceIdContext
import ru.kkalscan.domain.activity.CalorieBalance
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.health.createHealthConnectOnboardingStorage
import ru.kkalscan.presentation.diary.DiaryUiState
import ru.kkalscan.presentation.diary.IDiaryViewModel
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class HealthConnectOnboardingUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AndroidDeviceIdContext.init(context.applicationContext)
        context.getSharedPreferences("kkalscan", android.content.Context.MODE_PRIVATE)
            .edit()
            .remove("health_connect_initial_prompt_shown")
            .commit()
    }

    @Test
    fun diaryScreen_showsConnectButton_whenDisconnectedAfterFirstPrompt() {
        val viewModel = StubDiaryViewModel(
            DiaryUiState(
                isLoading = false,
                day = sampleDay(),
                balance = sampleBalance(),
                healthConnectAvailable = true,
                healthConnectPermissionsGranted = false,
            ),
        )

        composeTestRule.setContent {
            KkalScanTheme {
                DiaryScreen(
                    viewModel = viewModel,
                    onScanClick = {},
                    onRequestHealthConnect = {},
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("health-connect-request").assertIsDisplayed()
        saveScreenshot("health-connect-button-visible")
    }

    @Test
    fun diaryScreen_hidesConnectButton_whenConnected() {
        val viewModel = StubDiaryViewModel(
            DiaryUiState(
                isLoading = false,
                day = sampleDay(),
                balance = sampleBalance(),
                healthConnectAvailable = true,
                healthConnectPermissionsGranted = true,
            ),
        )

        composeTestRule.setContent {
            KkalScanTheme {
                DiaryScreen(
                    viewModel = viewModel,
                    onScanClick = {},
                    onRequestHealthConnect = {},
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onAllNodesWithTag("health-connect-request").assertCountEquals(0)
        saveScreenshot("health-connect-button-hidden")
    }

    @Test
    fun onboardingStorage_persistsPromptShownAcrossFreshInstances() {
        val first = createHealthConnectOnboardingStorage()
        assertFalse(first.wasInitialPromptShown())
        first.markInitialPromptShown()

        val second = createHealthConnectOnboardingStorage()
        assertTrue(second.wasInitialPromptShown())
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            ?: File(context.cacheDir, "screenshots")
        dir.mkdirs()
        FileOutputStream(File(dir, "$name.png")).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun sampleDay() = DiaryDay(
        date = "2026-07-08",
        entries = emptyList(),
        workouts = emptyList(),
        totalKcal = 350,
        scansLeft = 3,
    )

    private fun sampleBalance() = CalorieBalance(
        eatenKcal = 350,
        burnedKcal = 120,
        deficitKcal = 230,
        healthConnectKcal = 0,
        workoutKcal = 0,
    )

    private class StubDiaryViewModel(
        initialState: DiaryUiState,
    ) : IDiaryViewModel {
        private val mutableState = MutableStateFlow(initialState)
        override val state: StateFlow<DiaryUiState> = mutableState

        override suspend fun refresh() = Unit
        override suspend fun refreshHealthConnectOnly() = Unit
        override fun startHealthConnectPolling() = Unit
        override fun stopHealthConnectPolling() = Unit
        override suspend fun onForeground() = Unit
        override suspend fun deleteEntry(entryId: String) = Unit
        override suspend fun parseWorkoutDescription(description: String) = Unit
        override suspend fun confirmParsedWorkout(): Boolean = false
        override fun clearWorkoutParse() = Unit
        override suspend fun addWorkout(name: String, kcal: Int) = Unit
        override suspend fun deleteWorkout(workoutId: String) = Unit
        override fun clearError() = Unit
    }
}
