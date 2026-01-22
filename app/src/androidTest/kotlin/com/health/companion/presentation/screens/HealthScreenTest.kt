package com.health.companion.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.health.companion.presentation.screens.health.HealthScreen
import com.health.companion.presentation.theme.HealthCompanionTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HealthScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun healthScreen_displaysTitle() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                HealthScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Health Dashboard")
            .assertIsDisplayed()
    }

    @Test
    fun healthScreen_displaysInsightsSection() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                HealthScreen()
            }
        }

        // Assert - Insights title should be visible when data loads
        composeTestRule
            .onNodeWithText("Insights")
            .assertIsDisplayed()
    }
}
