package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

/**
 * Reminders API — Smart Reminders CRUD + AI Parsing
 */
interface RemindersApi {

    @GET("reminders")
    suspend fun getReminders(
        @Query("status") status: String = "active",
        @Query("category") category: String? = null
    ): RemindersListResponse

    @POST("reminders")
    suspend fun createReminder(@Body request: CreateReminderRequest): CreateReminderResponse

    @POST("reminders/parse")
    suspend fun parseReminder(@Body request: ParseReminderRequest): CreateReminderResponse

    @GET("reminders/upcoming")
    suspend fun getUpcoming(@Query("hours") hours: Int = 24): UpcomingRemindersResponse

    @GET("reminders/due")
    suspend fun getDue(): DueRemindersResponse

    @PATCH("reminders/{id}")
    suspend fun updateReminder(
        @Path("id") id: String,
        @Body request: UpdateReminderRequest
    ): CreateReminderResponse

    @DELETE("reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): ReminderActionResponse

    @POST("reminders/{id}/snooze")
    suspend fun snoozeReminder(
        @Path("id") id: String,
        @Body request: SnoozeReminderRequest
    ): ReminderActionResponse

    @POST("reminders/{id}/complete")
    suspend fun completeReminder(@Path("id") id: String): ReminderActionResponse

    @GET("reminders/stats")
    suspend fun getStats(): ReminderStatsResponse

    @PATCH("reminders/{id}")
    suspend fun editReminder(
        @Path("id") id: String,
        @Body request: EditReminderRequest
    ): CreateReminderResponse
}

// ==================== REQUEST MODELS ====================

@Serializable
data class EditReminderRequest(
    @SerialName("new_text") val newText: String
)

@Serializable
data class CreateReminderRequest(
    val title: String,
    val description: String? = null,
    @SerialName("trigger_at") val triggerAt: String? = null,
    val frequency: String = "once",
    @SerialName("recurring_days") val recurringDays: List<Int>? = null,
    @SerialName("recurring_time_hour") val recurringTimeHour: Int = 9,
    @SerialName("recurring_time_minute") val recurringTimeMinute: Int = 0,
    val priority: String = "medium",
    val category: String = "general",
    val timezone: String = java.util.TimeZone.getDefault().id
)

@Serializable
data class ParseReminderRequest(
    val text: String,
    @SerialName("conversation_id") val conversationId: String? = null,
    val timezone: String = java.util.TimeZone.getDefault().id
)

@Serializable
data class UpdateReminderRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("trigger_at") val triggerAt: String? = null,
    val frequency: String? = null,
    @SerialName("recurring_days") val recurringDays: List<Int>? = null,
    @SerialName("recurring_time_hour") val recurringTimeHour: Int? = null,
    @SerialName("recurring_time_minute") val recurringTimeMinute: Int? = null,
    val priority: String? = null,
    val category: String? = null
)

@Serializable
data class SnoozeReminderRequest(
    val minutes: Int = 30
)

// ==================== RESPONSE MODELS ====================

@Serializable
data class RemindersListResponse(
    val reminders: List<ReminderDTO> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateReminderResponse(
    val success: Boolean = true,
    val reminder: ReminderDTO,
    val message: String? = null
)

@Serializable
data class UpcomingRemindersResponse(
    val upcoming: List<ReminderDTO> = emptyList(),
    val total: Int = 0,
    val hours: Int = 24
)

@Serializable
data class DueRemindersResponse(
    val due: List<ReminderDTO> = emptyList(),
    val total: Int = 0,
    val timestamp: String? = null
)

@Serializable
data class ReminderActionResponse(
    val success: Boolean = true,
    val reminder: ReminderDTO? = null,
    val message: String? = null
)

@Serializable
data class ReminderStatsResponse(
    val total: Int = 0,
    val active: Int = 0,
    val recurring: Int = 0,
    val completed: Int = 0,
    @SerialName("total_triggers") val totalTriggers: Int = 0
)

// ==================== DATA MODELS ====================

@Serializable
data class ReminderDTO(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("trigger_at") val triggerAt: String? = null,
    val frequency: String = "once",
    @SerialName("custom_interval_minutes") val customIntervalMinutes: Int? = null,
    @SerialName("recurring_days") val recurringDays: List<Int> = emptyList(),
    @SerialName("recurring_time") val recurringTime: String = "09:00",
    val priority: String = "medium",
    val status: String = "active",
    val category: String = "general",
    @SerialName("created_from") val createdFrom: String = "manual",
    @SerialName("snooze_count") val snoozeCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_triggered_at") val lastTriggeredAt: String? = null,
    @SerialName("trigger_count") val triggerCount: Int = 0,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("next_trigger") val nextTrigger: String? = null,
    @SerialName("minutes_until") val minutesUntil: Int? = null
)

// ==================== DOMAIN ENUMS ====================

enum class ReminderFrequency(val value: String) {
    ONCE("once"),
    DAILY("daily"),
    WEEKLY("weekly"),
    WEEKDAYS("weekdays"),
    BIWEEKLY("biweekly"),
    MONTHLY("monthly"),
    CUSTOM("custom");

    companion object {
        fun from(value: String) = entries.find { it.value == value } ?: ONCE
    }
}

enum class ReminderStatus(val value: String) {
    ACTIVE("active"),
    SNOOZED("snoozed"),
    COMPLETED("completed"),
    DISMISSED("dismissed"),
    EXPIRED("expired");

    companion object {
        fun from(value: String) = entries.find { it.value == value } ?: ACTIVE
    }
}

enum class ReminderPriority(val value: String, val emoji: String, val color: Long) {
    LOW("low", "🟢", 0xFF4CAF50),
    MEDIUM("medium", "🟡", 0xFFFFC107),
    HIGH("high", "🟠", 0xFFFF9800),
    URGENT("urgent", "🔴", 0xFFF44336);

    companion object {
        fun from(value: String) = entries.find { it.value == value } ?: MEDIUM
    }
}

// ==================== NOTIFICATION MODELS ====================

/**
 * Push notification from WebSocket
 */
data class ReminderNotification(
    val reminderId: String,
    val title: String,
    val description: String?,
    val priority: ReminderPriority,
    val isRecurring: Boolean,
    val category: String,
    val icon: String,
    val actions: List<ReminderAction>
)

data class ReminderAction(
    val type: String,    // "complete" | "snooze" | "dismiss"
    val label: String
)

/**
 * Inline reminder in SSE chat stream
 */
data class InChatReminder(
    val id: String,
    val title: String,
    val priority: ReminderPriority,
    val isRecurring: Boolean
)
