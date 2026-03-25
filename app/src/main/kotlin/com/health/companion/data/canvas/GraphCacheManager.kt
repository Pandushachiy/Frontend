package com.health.companion.data.canvas

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val graphFile get() = File(context.filesDir, "canvas_graph_cache.json")
    private val insightsFile get() = File(context.filesDir, "canvas_insights_cache.json")

    suspend fun saveGraph(graph: CanvasGraph) = withContext(Dispatchers.IO) {
        try {
            graphFile.writeText(json.encodeToString(graph))
            Timber.d("Graph cache saved: ${graph.totalNodes} nodes")
        } catch (e: Exception) {
            Timber.w(e, "Failed to save graph cache")
        }
    }

    suspend fun loadGraph(): CanvasGraph? = withContext(Dispatchers.IO) {
        try {
            val file = graphFile
            if (!file.exists()) return@withContext null
            val text = file.readText()
            if (text.isBlank()) return@withContext null
            json.decodeFromString<CanvasGraph>(text)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load graph cache")
            null
        }
    }

    suspend fun saveInsights(insights: List<CanvasInsight>) = withContext(Dispatchers.IO) {
        try {
            insightsFile.writeText(json.encodeToString(insights))
        } catch (e: Exception) {
            Timber.w(e, "Failed to save insights cache")
        }
    }

    suspend fun loadInsights(): List<CanvasInsight> = withContext(Dispatchers.IO) {
        try {
            val file = insightsFile
            if (!file.exists()) return@withContext emptyList()
            json.decodeFromString<List<CanvasInsight>>(file.readText())
        } catch (e: Exception) {
            Timber.w(e, "Failed to load insights cache")
            emptyList()
        }
    }
}
