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
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.activity.CalorieBalance
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.presentation.diary.DiaryUiState
import ru.kkalscan.presentation.diary.IDiaryViewModel
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ActivityRecognitionUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AndroidDeviceIdContext.init(context.applicationContext)
    }

    @Test
    fun diaryScreen_showsPermissionButton_whenSensorAvailableWithoutPermission() {
        val viewModel = StubDiaryViewModel(
            DiaryUiState(
                isLoading = false,
                day = sampleDay(),
                balance = sampleBalance(),
                stepSensorAvailable = true,
                activityRecognitionGranted = false,
            ),
        )

        composeTestRule.setContent {
            KkalScanTheme {
                DiaryScreen(
                    viewModel = viewModel,
                    onScanClick = {},
                    onRequestActivityRecognition = {},
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("activity-recognition-request").assertIsDisplayed()
        saveScreenshot("activity-recognition-button-visible")
    }

    @Test
    fun diaryScreen_hidesPermissionButton_whenGranted() {
        val viewModel = StubDiaryViewModel(
            DiaryUiState(
                isLoading = false,
                day = sampleDay(),
                balance = sampleBalance(),
                stepSensorAvailable = true,
                activityRecognitionGranted = true,
            ),
        )

        composeTestRule.setContent {
            KkalScanTheme {
                DiaryScreen(
                    viewModel = viewModel,
                    onScanClick = {},
                    onRequestActivityRecognition = {},
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onAllNodesWithTag("activity-recognition-request").assertCountEquals(0)
        saveScreenshot("activity-recognition-button-hidden")
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
        burnedKcal = 520,
        activityKcal = 400,
        activitySource = ActivitySource.Emulator,
        workoutKcal = 120,
        deficitKcal = 0,
        steps = 10_000,
    )

    private class StubDiaryViewModel(
        initialState: DiaryUiState,
    ) : IDiaryViewModel {
        private val mutableState = MutableStateFlow(initialState)
        override val state: StateFlow<DiaryUiState> = mutableState

        override suspend fun refresh() = Unit
        override suspend fun refreshActivityOnly() = Unit
        override fun startActivityPolling() = Unit
        override fun stopActivityPolling() = Unit
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
