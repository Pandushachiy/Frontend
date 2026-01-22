package com.health.companion.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.health.companion.presentation.screens.mood.MoodScreen
import com.health.companion.presentation.theme.HealthCompanionTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MoodScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun moodScreen_displaysTitle() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("How are you feeling?")
            .assertIsDisplayed()
    }

    @Test
    fun moodScreen_displaysMoodSlider() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Mood Level", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun moodScreen_displaysStressSlider() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Stress Level", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun moodScreen_displaysJournalEntry() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Journal Entry")
            .assertIsDisplayed()
    }

    @Test
    fun moodScreen_displaysSymptomsSection() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Any symptoms today?")
            .assertIsDisplayed()
    }

    @Test
    fun moodScreen_displaysSaveButton() {
        // Arrange & Act
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Save Entry")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun moodScreen_journalInputWorks() {
        // Arrange
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Write how you're feeling...")
            .performTextInput("Feeling great today!")

        // Assert
        composeTestRule
            .onNodeWithText("Feeling great today!")
            .assertIsDisplayed()
    }

    @Test
    fun moodScreen_symptomChipSelection() {
        // Arrange
        composeTestRule.setContent {
            HealthCompanionTheme {
                MoodScreen()
            }
        }

        // Act - Click on a symptom chip
        composeTestRule
            .onNodeWithText("Headache")
            .performClick()

        // Assert - Chip should be selected (visual change)
        // Note: In real test, you'd verify the selection state
    }
}
