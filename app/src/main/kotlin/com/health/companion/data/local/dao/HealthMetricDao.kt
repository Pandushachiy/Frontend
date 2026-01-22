package com.health.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.health.companion.data.local.database.HealthMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: HealthMetricEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<HealthMetricEntity>)
    
    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC")
    fun getAllMetricsFlow(): Flow<List<HealthMetricEntity>>
    
    @Query("SELECT * FROM health_metrics WHERE metricType = :type ORDER BY timestamp DESC LIMIT :limit")
    fun getMetricsByTypeFlow(type: String, limit: Int = 30): Flow<List<HealthMetricEntity>>
    
    @Query("SELECT * FROM health_metrics WHERE metricType = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetricByType(type: String): HealthMetricEntity?
    
    @Query("SELECT * FROM health_metrics WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getMetricsInRange(startTime: Long, endTime: Long): List<HealthMetricEntity>
    
    @Query("DELETE FROM health_metrics WHERE id = :metricId")
    suspend fun deleteById(metricId: String)
}
