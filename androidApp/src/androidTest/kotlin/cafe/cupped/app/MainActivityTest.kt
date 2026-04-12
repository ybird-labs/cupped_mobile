package cafe.cupped.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun feed_is_selected_by_default() {
        composeRule.onNodeWithTag("screen-feed").assertIsDisplayed()
        composeRule.onNodeWithTag("tab-indicator-feed").assertIsDisplayed()
    }

    @Test
    fun switching_tabs_updates_visible_screen() {
        composeRule.onNodeWithTag("tab-discover").performClick()
        composeRule.onNodeWithTag("screen-discover").assertIsDisplayed()

        composeRule.onNodeWithTag("tab-community").performClick()
        composeRule.onNodeWithTag("screen-community").assertIsDisplayed()

        composeRule.onNodeWithTag("tab-profile").performClick()
        composeRule.onNodeWithTag("screen-profile").assertIsDisplayed()
    }

    @Test
    fun log_button_opens_sheet_without_changing_selected_tab() {
        composeRule.onNodeWithTag("tab-discover").performClick()
        composeRule.onNodeWithTag("screen-discover").assertIsDisplayed()

        composeRule.onNodeWithTag("log-action-button").performClick()

        composeRule.onNodeWithTag("log-sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Log a Brew").assertIsDisplayed()
        composeRule.onNodeWithTag("screen-discover").assertIsDisplayed()
    }
}
