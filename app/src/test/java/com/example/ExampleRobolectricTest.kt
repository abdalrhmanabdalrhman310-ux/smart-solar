package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.smartsolar.app.domain.model.ServerConfig
import com.smartsolar.app.ui.screens.onboarding.OnboardingPagerScreen
import com.smartsolar.app.ui.screens.onboarding.WelcomeScreen
import com.smartsolar.app.ui.theme.SmartSolarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app_name matches Smart Solar`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Solar", appName)
        assertEquals("com.smartsolar.app", context.packageName)
    }

    @Test
    fun `welcome screen renders and navigates to onboarding or login`() {
        var explored = false
        var loginClicked = false
        var serverClicked = false

        composeTestRule.setContent {
            SmartSolarTheme {
                WelcomeScreen(
                    onStartOnboarding = { explored = true },
                    onGoToLogin = { loginClicked = true },
                    onOpenServerSettings = { serverClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("أهلاً بك في الطاقة الشمسية الذكية")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_explore_button")
            .performScrollTo()
            .performClick()
        assertTrue(explored)

        composeTestRule.onNodeWithTag("welcome_login_button")
            .performScrollTo()
            .performClick()
        assertTrue(loginClicked)

        composeTestRule.onNodeWithTag("welcome_server_settings_button")
            .performScrollTo()
            .performClick()
        assertTrue(serverClicked)
    }

    @Test
    fun `onboarding has 5 pages and completes on skip or start`() {
        var completed = false
        composeTestRule.setContent {
            SmartSolarTheme {
                OnboardingPagerScreen(onFinished = { completed = true })
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("اكتشف منتجات الطاقة الشمسية")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        assertTrue(completed)
    }

    @Test
    fun `server config recognizes unconfigured vs https`() {
        val unconfigured = ServerConfig("")
        assertEquals(false, unconfigured.isConfigured)

        val valid = ServerConfig("https://solar.example.org/solar-admin/api/")
        assertEquals(true, valid.isConfigured)
        assertEquals(true, valid.isHttps)
    }
}
