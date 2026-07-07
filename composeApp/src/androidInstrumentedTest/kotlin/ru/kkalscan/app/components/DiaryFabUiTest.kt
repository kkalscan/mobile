package ru.kkalscan.app.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.kkalscan.app.theme.KkalScanTheme

@RunWith(AndroidJUnit4::class)
class DiaryFabUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainFab_onTodayTab_expandsToThreeActions_thenCollapses() {
        composeTestRule.setContent {
            KkalScanTheme {
                KkalBottomBar(
                    selectedTab = AppTab.Today,
                    onTabSelected = {},
                    onDescribeClick = {},
                    onAddWorkoutClick = {},
                    onScanClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("diary-fab-describe-food").assertDoesNotExist()
        composeTestRule.onNodeWithTag("diary-fab-add-workout").assertDoesNotExist()
        composeTestRule.onNodeWithTag("diary-fab-scan-photo").assertDoesNotExist()
        composeTestRule.onAllNodesWithTag("diary-main-fab").assertCountEquals(1)

        composeTestRule.onNodeWithTag("diary-main-fab").performClick()
        composeTestRule.waitForFabActions(visible = true)

        composeTestRule.onNodeWithTag("diary-fab-describe-food").assertExists()
        composeTestRule.onNodeWithTag("diary-fab-add-workout").assertExists()
        composeTestRule.onNodeWithTag("diary-fab-scan-photo").assertExists()
        composeTestRule.onAllNodesWithTag("diary-main-fab").assertCountEquals(1)

        composeTestRule.onNodeWithTag("diary-main-fab").performClick()
        composeTestRule.waitForFabActions(visible = false)

        composeTestRule.onNodeWithTag("diary-fab-describe-food").assertDoesNotExist()
        composeTestRule.onNodeWithTag("diary-fab-add-workout").assertDoesNotExist()
        composeTestRule.onNodeWithTag("diary-fab-scan-photo").assertDoesNotExist()
    }

    @Test
    fun collapsedState_hasExactlyOneMainFab_notLegacyDualLayout() {
        composeTestRule.setContent {
            KkalScanTheme {
                KkalBottomBar(
                    selectedTab = AppTab.Today,
                    onTabSelected = {},
                    onDescribeClick = {},
                    onAddWorkoutClick = {},
                    onScanClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithTag("diary-main-fab").assertCountEquals(1)
        composeTestRule.onNodeWithTag("diary-fab-describe-food").assertDoesNotExist()
        composeTestRule.onNodeWithTag("diary-fab-add-workout").assertDoesNotExist()
        composeTestRule.onNodeWithTag("diary-fab-scan-photo").assertDoesNotExist()
    }

    @Test
    fun mainFab_hiddenOnJournalTab() {
        composeTestRule.setContent {
            KkalScanTheme {
                KkalBottomBar(
                    selectedTab = AppTab.Journal,
                    onTabSelected = {},
                    onDescribeClick = {},
                    onAddWorkoutClick = {},
                    onScanClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("diary-main-fab").assertDoesNotExist()
    }

    private fun ComposeContentTestRule.waitForFabActions(visible: Boolean) {
        waitUntil(timeoutMillis = 5_000) {
            val hasActions = onAllNodesWithTag("diary-fab-describe-food")
                .fetchSemanticsNodes()
                .isNotEmpty()
            hasActions == visible
        }
    }
}
