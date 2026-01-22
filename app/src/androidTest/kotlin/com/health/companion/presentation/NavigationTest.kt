package com.health.companion.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.health.companion.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun bottomNavigation_chatToHealth() {
        // Act - Navigate to Health
        composeTestRule
            .onNodeWithText("Health")
            .performClick()

        // Assert
        composeTestRule
            .onNodeWithText("Health Dashboard")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_chatToMood() {
        // Act - Navigate to Mood
        composeTestRule
            .onNodeWithText("Mood")
            .performClick()

        // Assert
        composeTestRule
            .onNodeWithText("How are you feeling?")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_chatToDocuments() {
        // Act - Navigate to Docs
        composeTestRule
            .onNodeWithText("Docs")
            .performClick()

        // Assert
        composeTestRule
            .onNodeWithText("Documents")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_chatToSettings() {
        // Act - Navigate to Settings
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        // Assert
        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_roundTrip() {
        // Navigate through all tabs and back to Chat
        
        // Go to Health
        composeTestRule.onNodeWithText("Health").performClick()
        composeTestRule.onNodeWithText("Health Dashboard").assertIsDisplayed()
        
        // Go to Mood
        composeTestRule.onNodeWithText("Mood").performClick()
        composeTestRule.onNodeWithText("How are you feeling?").assertIsDisplayed()
        
        // Go to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Back to Chat
        composeTestRule.onNodeWithText("Chat").performClick()
        composeTestRule.onNodeWithText("Health Companion").assertIsDisplayed()
    }
}
