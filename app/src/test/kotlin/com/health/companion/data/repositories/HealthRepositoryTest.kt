package com.health.companion.data.repositories

import com.health.companion.data.local.dao.HealthMetricDao
import com.health.companion.data.local.dao.MoodEntryDao
import com.health.companion.data.local.database.HealthMetricEntity
import com.health.companion.data.local.database.MoodEntryEntity
import com.health.companion.data.remote.api.HealthApi
import com.health.companion.data.remote.api.HealthInsightsDTO
import com.health.companion.data.remote.api.HealthMetricDTO
import com.health.companion.data.remote.api.ManualMetricRequest
import com.health.companion.data.remote.api.MoodEntryDTO
import com.health.companion.data.remote.api.MoodEntryRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HealthRepositoryTest {

    private lateinit var healthRepository: HealthRepository
    private val healthApi = mockk<HealthApi>()
    private val healthMetricDao = mockk<HealthMetricDao>()
    private val moodEntryDao = mockk<MoodEntryDao>()

    @Before
    fun setup() {
        healthRepository = HealthRepositoryImpl(healthApi, healthMetricDao, moodEntryDao)
    }

    @Test
    fun `getHealthMetrics should return metrics from database`() = runTest {
        // Arrange
        val entities = listOf(
            HealthMetricEntity(
                id = "metric-1",
                metricType = "heart_rate",
                value = 72f,
                unit = "bpm"
            ),
            HealthMetricEntity(
                id = "metric-2",
                metricType = "blood_pressure",
                value = 120f,
                unit = "mmHg"
            )
        )

        coEvery { healthMetricDao.getAllMetricsFlow() } returns flowOf(entities)

        // Act
        healthRepository.getHealthMetrics()
            .collect { metrics ->
                // Assert
                assertEquals(2, metrics.size)
                assertEquals("heart_rate", metrics[0].metric_type)
                assertEquals(72f, metrics[0].value)
                assertEquals("bpm", metrics[0].unit)
            }
    }

    @Test
    fun `getHealthMetrics with type filter should return filtered metrics`() = runTest {
        // Arrange
        val metricType = "heart_rate"
        val entities = listOf(
            HealthMetricEntity(
                id = "metric-1",
                metricType = metricType,
                value = 72f,
                unit = "bpm"
            )
        )

        coEvery { healthMetricDao.getMetricsByTypeFlow(metricType) } returns flowOf(entities)

        // Act
        healthRepository.getHealthMetrics(metricType = metricType)
            .collect { metrics ->
                // Assert
                assertEquals(1, metrics.size)
                assertEquals(metricType, metrics[0].metric_type)
            }
    }

    @Test
    fun `addManualMetric should save to API and database`() = runTest {
        // Arrange
        val request = ManualMetricRequest(
            metric_type = "weight",
            value = 70.5f,
            unit = "kg"
        )
        val expectedResponse = HealthMetricDTO(
            id = "new-metric-1",
            metric_type = "weight",
            value = 70.5f,
            unit = "kg",
            timestamp = "2024-01-15T10:00:00Z"
        )

        coEvery { healthApi.addManualMetric(request) } returns expectedResponse
        coEvery { healthMetricDao.insert(any()) } just runs

        // Act
        val result = healthRepository.addManualMetric(request)

        // Assert
        assertEquals(expectedResponse, result)
        assertEquals("weight", result.metric_type)
        assertEquals(70.5f, result.value)
        
        coVerify { healthMetricDao.insert(any()) }
    }

    @Test
    fun `getHealthInsights should return insights from API`() = runTest {
        // Arrange
        val expectedInsights = HealthInsightsDTO(
            summary = "Your health metrics look good!",
            trends = listOf("Heart rate stable", "Weight decreasing"),
            recommendations = listOf("Continue exercising", "Stay hydrated")
        )

        coEvery { healthApi.getInsights() } returns expectedInsights

        // Act
        val result = healthRepository.getHealthInsights()

        // Assert
        assertEquals(expectedInsights.summary, result.summary)
        assertEquals(2, result.trends.size)
        assertEquals(2, result.recommendations.size)
    }

    @Test
    fun `submitMoodEntry should save to API and database`() = runTest {
        // Arrange
        val request = MoodEntryRequest(
            mood_level = 4,
            symptoms = listOf("headache", "fatigue"),
            journal_text = "Feeling better today",
            stress_level = 2
        )
        val expectedResponse = MoodEntryDTO(
            id = "mood-1",
            mood_level = 4,
            created_at = "2024-01-15T08:00:00Z"
        )

        coEvery { healthApi.submitMoodEntry(request) } returns expectedResponse
        coEvery { moodEntryDao.insert(any()) } just runs

        // Act
        val result = healthRepository.submitMoodEntry(request)

        // Assert
        assertEquals(expectedResponse.id, result.id)
        assertEquals(4, result.mood_level)
        
        coVerify { moodEntryDao.insert(any()) }
    }

    @Test
    fun `getMoodHistory should return mood entries from database`() = runTest {
        // Arrange
        val entities = listOf(
            MoodEntryEntity(
                id = "mood-1",
                moodLevel = 4,
                symptoms = "headache,fatigue",
                journalText = "Good day",
                stressLevel = 2
            ),
            MoodEntryEntity(
                id = "mood-2",
                moodLevel = 3,
                symptoms = "",
                journalText = "Average day",
                stressLevel = 3
            )
        )

        coEvery { moodEntryDao.getRecentEntriesFlow(30) } returns flowOf(entities)

        // Act
        healthRepository.getMoodHistory(30)
            .collect { entries ->
                // Assert
                assertEquals(2, entries.size)
                assertEquals(4, entries[0].mood_level)
                assertEquals(3, entries[1].mood_level)
            }
    }

    @Test
    fun `addManualMetric should throw exception on API error`() = runTest {
        // Arrange
        val request = ManualMetricRequest(
            metric_type = "weight",
            value = 70f,
            unit = "kg"
        )
        val expectedException = RuntimeException("API error")

        coEvery { healthApi.addManualMetric(request) } throws expectedException

        // Act & Assert
        try {
            healthRepository.addManualMetric(request)
            assert(false) { "Expected exception to be thrown" }
        } catch (e: Exception) {
            assertEquals("API error", e.message)
        }
    }
}
