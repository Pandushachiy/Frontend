package com.health.companion.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.health.companion.presentation.screens.chat.ChatScreen
import com.health.companion.presentation.theme.HealthCompanionTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ChatScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun chatScreen_displaysHeader() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                ChatScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Health Companion")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysInputField() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                ChatScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Write a message...")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_sendButtonDisabledWhenEmpty() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                ChatScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Send")
            .assertIsNotEnabled()
    }

    @Test
    fun chatScreen_sendButtonEnabledWhenTextEntered() {
        // Arrange
        composeTestRule.setContent {
            HealthCompanionTheme {
                ChatScreen()
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Write a message...")
            .performTextInput("Hello")

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Send")
            .assertIsEnabled()
    }

    @Test
    fun chatScreen_documentsButtonExists() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                ChatScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Documents")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_documentsButtonNavigates() {
        // Arrange
        var navigatedToDocuments = false
        
        composeTestRule.setContent {
            HealthCompanionTheme {
                ChatScreen(
                    onNavigateToDocuments = { navigatedToDocuments = true }
                )
            }
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Documents")
            .performClick()

        // Assert
        assert(navigatedToDocuments)
    }
}
