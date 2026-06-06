package dev.vikingsen.skald

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_displaysLoginOrMainScreen() {
        // Since the app launches without prior authentication details in a fresh test run,
        // it should land on the Login screen and show the server connection text.
        composeTestRule.onNodeWithText("Connect to your server").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audiobookshelf").assertIsDisplayed()
    }
}
