package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse
}

@Serializable
data class DashboardResponse(
    val userName: String? = null,
    val greeting: String = "",
    val moodEmoji: String = "😊",
    val overallStatus: String = "neutral",
    val widgets: List<Widget> = emptyList(),
    val generatedAt: String? = null
)

@Serializable
data class Widget(
    val type: String,
    val priority: String? = null,
    val title: String? = null,
    val data: Map<String, JsonElement> = emptyMap(),
    val action: WidgetAction? = null
)

@Serializable
data class WidgetAction(
    val label: String,
    val route: String
)
