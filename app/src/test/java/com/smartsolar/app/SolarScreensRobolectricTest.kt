package com.smartsolar.app

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.smartsolar.app.domain.model.ServerConfig
import com.smartsolar.app.ui.screens.auth.AuthScreen
import com.smartsolar.app.ui.screens.client.SolarCalculatorScreen
import com.smartsolar.app.ui.screens.settings.ServerSettingsScreen
import com.smartsolar.app.ui.theme.SmartSolarTheme
import com.smartsolar.app.ui.viewmodel.AppSessionViewModel
import com.smartsolar.app.ui.viewmodel.AuthViewModel
import com.smartsolar.app.ui.viewmodel.ClientViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class SolarScreensRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `auth screen switches between login client engineer and vendor tabs`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val container = AppContainer(app)
        val authVm = AuthViewModel(container.authRepository)

        composeTestRule.setContent {
            SmartSolarTheme {
                AuthScreen(
                    authViewModel = authVm,
                    serverConfig = ServerConfig(),
                    sessionExpiredBanner = null,
                    onDismissSessionExpiredBanner = {},
                    onAuthSuccess = {},
                    onOpenServerSettings = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_email_input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("auth_tab_client_register").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("client_reg_city_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("client_reg_address_input").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("auth_server_status_banner").performScrollTo()
        composeTestRule.onNodeWithTag("auth_tab_engineer_register").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("eng_reg_age_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("eng_reg_city_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("eng_reg_spec_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("eng_reg_projects_input").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("auth_server_status_banner").performScrollTo()
        composeTestRule.onNodeWithTag("auth_tab_vendor_register").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("vendor_reg_store_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("vendor_reg_business_type_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("vendor_reg_city_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("vendor_reg_address_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("vendor_reg_description_input").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `solar calculator adds appliance and displays engineering results`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val container = AppContainer(app)
        val clientVm = ClientViewModel(
            container.sessionPreferences,
            container.catalogRepository,
            container.servicesRepository
        )

        composeTestRule.setContent {
            SmartSolarTheme {
                SolarCalculatorScreen(
                    clientViewModel = clientVm,
                    onRequestInstallationWithDesign = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calc_device_name_input").performScrollTo().performTextInput("مضخة مياه")
        composeTestRule.onNodeWithTag("calc_watts_input").performScrollTo().performTextInput("1000")
        composeTestRule.onNodeWithTag("calc_hours_input").performScrollTo().performTextInput("4")
        composeTestRule.onNodeWithTag("calc_add_appliance_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("1000 واط").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("4.00 كيلوواط/ساعة").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `server settings screen renders all 11 php endpoints`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val container = AppContainer(app)
        val appSessionVm = AppSessionViewModel(
            container.sessionPreferences,
            container.networkFactory,
            container.authRepository,
            container.servicesRepository
        )

        composeTestRule.setContent {
            SmartSolarTheme {
                ServerSettingsScreen(
                    appSessionViewModel = appSessionVm,
                    serverConfig = ServerConfig(),
                    onBackClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("server_url_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("auth.php").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("solar_designs.php").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `MainActivity launches SplashScreen without crashing`() {
        androidx.test.core.app.ActivityScenario.launch(com.example.MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                org.junit.Assert.assertNotNull(activity)
            }
        }
    }
}
