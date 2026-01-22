package com.health.companion.data.repositories

import com.health.companion.data.local.dao.HealthMetricDao
import com.health.companion.data.local.dao.MoodEntryDao
import com.health.companion.data.local.database.HealthMetricEntity
import com.health.companion.data.local.database.MoodEntryEntity
import com.health.companion.data.remote.api.DailySummaryDTO
import com.health.companion.data.remote.api.HealthApi
import com.health.companion.data.remote.api.HealthMetricDTO
import com.health.companion.data.remote.api.ManualMetricRequest
import com.health.companion.data.remote.api.MoodEntryDTO
import com.health.companion.data.remote.api.MoodEntryRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

interface HealthRepository {
    suspend fun getMetrics(metricType: String? = null): Result<List<HealthMetricDTO>>
    suspend fun addManualMetric(metricType: String, value: Float, unit: String): Result<HealthMetricDTO>
    suspend fun getInsights(): Result<DailySummaryDTO>
    suspend fun submitMoodEntry(
        moodLevel: Int,
        stressLevel: Int,
        symptoms: List<String>,
        journalText: String
    ): Result<MoodEntryDTO>
    fun getLocalMetrics(): Flow<List<HealthMetricEntity>>
    fun getLocalMoodEntries(): Flow<List<MoodEntryEntity>>
}

class HealthRepositoryImpl @Inject constructor(
    private val healthApi: HealthApi,
    private val healthMetricDao: HealthMetricDao,
    private val moodEntryDao: MoodEntryDao
) : HealthRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun getMetrics(metricType: String?): Result<List<HealthMetricDTO>> {
        return try {
            val response = healthApi.getMetrics(metricType = metricType)
            val metrics = response.items
            
            // Cache locally
            metrics.forEach { dto ->
                healthMetricDao.insert(
                    HealthMetricEntity(
                        id = dto.id,
                        metricType = dto.metric_type,
                        value = dto.value,
                        unit = dto.unit,
                        source = "server"
                    )
                )
            }
            
            Timber.d("Fetched ${metrics.size} health metrics")
            Result.success(metrics)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch health metrics")
            Result.failure(e)
        }
    }
    
    override suspend fun addManualMetric(
        metricType: String,
        value: Float,
        unit: String
    ): Result<HealthMetricDTO> {
        return try {
            val response = healthApi.addManualMetric(
                ManualMetricRequest(
                    metric_type = metricType,
                    value = value,
                    unit = unit
                )
            )
            
            // Save locally
            healthMetricDao.insert(
                HealthMetricEntity(
                    id = response.id,
                    metricType = response.metric_type,
                    value = response.value,
                    unit = response.unit,
                    source = "manual"
                )
            )
            
            Timber.d("Added manual metric: $metricType = $value $unit")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add manual metric")
            Result.failure(e)
        }
    }
    
    override suspend fun getInsights(): Result<DailySummaryDTO> {
        return try {
            val summary = healthApi.getDailySummary()
            Timber.d("Fetched daily health summary: ${summary.summary_text}")
            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch daily health summary")
            Result.failure(e)
        }
    }
    
    override suspend fun submitMoodEntry(
        moodLevel: Int,
        stressLevel: Int,
        symptoms: List<String>,
        journalText: String
    ): Result<MoodEntryDTO> {
        return try {
            val response = healthApi.submitMoodEntry(
                MoodEntryRequest(
                    mood_level = moodLevel,
                    stress_level = stressLevel,
                    symptoms = symptoms,
                    journal_text = journalText
                )
            )
            
            // Save locally
            moodEntryDao.insert(
                MoodEntryEntity(
                    id = response.id,
                    moodLevel = response.mood_level,
                    stressLevel = stressLevel,
                    symptoms = json.encodeToString(symptoms),
                    journalText = journalText
                )
            )
            
            Timber.d("Submitted mood entry: level=$moodLevel, stress=$stressLevel")
            Result.success(response)
        } catch (e: Exception) {
            // Save locally even if API fails (offline support)
            moodEntryDao.insert(
                MoodEntryEntity(
                    id = UUID.randomUUID().toString(),
                    moodLevel = moodLevel,
                    stressLevel = stressLevel,
                    symptoms = json.encodeToString(symptoms),
                    journalText = journalText
                )
            )
            Timber.e(e, "Failed to submit mood entry to server, saved locally")
            Result.failure(e)
        }
    }
    
    override fun getLocalMetrics(): Flow<List<HealthMetricEntity>> {
        return healthMetricDao.getAllMetricsFlow()
    }
    
    override fun getLocalMoodEntries(): Flow<List<MoodEntryEntity>> {
        return moodEntryDao.getRecentEntriesFlow()
    }
}
