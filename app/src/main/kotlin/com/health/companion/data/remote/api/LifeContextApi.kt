package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

/**
 * Life Context API — Профиль, Анкета, Важные даты, Люди
 */
interface LifeContextApi {
    
    // ==================== QUESTIONNAIRE ====================
    
    @GET("life-context/questionnaire")
    suspend fun getQuestionnaire(
        @Query("section") section: String? = null
    ): QuestionnaireResponse
    
    @POST("life-context/questionnaire")
    suspend fun saveAnswers(@Body request: QuestionnaireRequest): SaveAnswersResponse
    
    @GET("life-context/profile")
    suspend fun getProfile(): UserProfile
    
    // ==================== IMPORTANT DATES ====================
    
    @GET("life-context/important-dates")
    suspend fun getImportantDates(): List<ImportantDate>
    
    @POST("life-context/important-dates")
    suspend fun addImportantDate(@Body request: ImportantDateCreate): AddDateResponse
    
    @PUT("life-context/important-dates/{id}")
    suspend fun updateImportantDate(
        @Path("id") id: String,
        @Body request: ImportantDateUpdate
    ): ImportantDate
    
    @DELETE("life-context/important-dates/{id}")
    suspend fun deleteImportantDate(@Path("id") id: String)
    
    // ==================== IMPORTANT PEOPLE ====================
    
    @GET("life-context/important-people")
    suspend fun getImportantPeople(): List<ImportantPerson>
    
    @POST("life-context/important-people")
    suspend fun addImportantPerson(@Body request: ImportantPersonCreate): AddPersonResponse
    
    @PUT("life-context/important-people/{id}")
    suspend fun updateImportantPerson(
        @Path("id") id: String,
        @Body request: ImportantPersonUpdate
    ): ImportantPerson
    
    @DELETE("life-context/important-people/{id}")
    suspend fun deleteImportantPerson(@Path("id") id: String)
    
    // ==================== PATTERNS ====================
    
    @GET("life-context/patterns")
    suspend fun getLifePatterns(): LifePatternsResponse
}

// ==================== QUESTIONNAIRE MODELS ====================

@Serializable
data class QuestionnaireResponse(
    val basic: QuestionnaireSection? = null,
    val health: QuestionnaireSection? = null,
    val lifestyle: QuestionnaireSection? = null,
    val social: QuestionnaireSection? = null,
    val goals: QuestionnaireSection? = null,
    val mental: QuestionnaireSection? = null,
    val preferences: QuestionnaireSection? = null,
    @SerialName("important_dates") val importantDates: QuestionnaireSection? = null
)

@Serializable
data class QuestionnaireSection(
    val title: String,
    val questions: List<Question>
)

@Serializable
data class Question(
    val key: String,
    val question: String,
    val type: String, // text, number, choice, list, textarea, slider, boolean, date, date_list
    val options: List<String>? = null,
    val min: Int? = null,
    val max: Int? = null,
    val placeholder: String? = null,
    val required: Boolean = false
)

@Serializable
data class QuestionnaireRequest(
    val answers: Map<String, @Serializable(with = AnswerValueSerializer::class) Any>
)

// Custom serializer for mixed types
object AnswerValueSerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "AnswerValue",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Int -> encoder.encodeInt(value)
            is Boolean -> encoder.encodeBoolean(value)
            is List<*> -> {
                val jsonEncoder = encoder as kotlinx.serialization.json.JsonEncoder
                val jsonArray = kotlinx.serialization.json.buildJsonArray {
                    value.forEach { item -> 
                        when (item) {
                            is String -> add(kotlinx.serialization.json.JsonPrimitive(item))
                            else -> add(kotlinx.serialization.json.JsonPrimitive(item.toString()))
                        }
                    }
                }
                jsonEncoder.encodeJsonElement(jsonArray)
            }
            else -> encoder.encodeString(value.toString())
        }
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        return decoder.decodeString()
    }
}

@Serializable
data class SaveAnswersResponse(
    val success: Boolean,
    val message: String,
    @SerialName("profile_version") val profileVersion: Int? = null
)

@Serializable
data class UserProfile(
    @SerialName("preferred_name") val preferredName: String? = null,
    val age: Int? = null,
    val location: String? = null,
    val gender: String? = null,
    @SerialName("health_conditions") val healthConditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    @SerialName("current_medications") val currentMedications: List<String> = emptyList(),
    @SerialName("short_term_goals") val shortTermGoals: List<String> = emptyList(),
    @SerialName("long_term_goals") val longTermGoals: List<String> = emptyList(),
    @SerialName("stress_level") val stressLevel: Int? = null,
    @SerialName("sleep_quality") val sleepQuality: Int? = null,
    @SerialName("energy_level") val energyLevel: Int? = null,
    val hobbies: List<String> = emptyList(),
    @SerialName("living_situation") val livingSituation: String? = null,
    val occupation: String? = null,
    @SerialName("work_schedule") val workSchedule: String? = null,
    @SerialName("profile_version") val profileVersion: Int = 0,
    @SerialName("last_updated") val lastUpdated: String? = null
)

// ==================== IMPORTANT DATES MODELS ====================

@Serializable
data class ImportantDate(
    val id: String,
    val date: String,
    val title: String,
    @SerialName("event_type") val eventType: String = "custom", // birthday, anniversary, custom
    val recurring: Boolean = true,
    @SerialName("days_until") val daysUntil: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ImportantDateCreate(
    val date: String,
    val title: String,
    @SerialName("event_type") val eventType: String = "custom",
    val recurring: Boolean = true
)

@Serializable
data class ImportantDateUpdate(
    val date: String? = null,
    val title: String? = null,
    @SerialName("event_type") val eventType: String? = null,
    val recurring: Boolean? = null
)

@Serializable
data class AddDateResponse(
    val success: Boolean,
    val message: String,
    val date: ImportantDate? = null
)

// ==================== IMPORTANT PEOPLE MODELS ====================

@Serializable
data class ImportantPerson(
    val id: String,
    val name: String,
    val relation: String,
    val details: String? = null,
    val birthday: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ImportantPersonCreate(
    val name: String,
    val relation: String,
    val details: String? = null,
    val birthday: String? = null
)

@Serializable
data class ImportantPersonUpdate(
    val name: String? = null,
    val relation: String? = null,
    val details: String? = null,
    val birthday: String? = null
)

@Serializable
data class AddPersonResponse(
    val success: Boolean,
    val message: String,
    val person: ImportantPerson? = null
)

// ==================== PATTERNS MODELS ====================

@Serializable
data class LifePatternsResponse(
    @SerialName("mood_trend") val moodTrend: String? = null, // improving, stable, declining
    @SerialName("avg_mood") val avgMood: Float? = null,
    @SerialName("avg_stress") val avgStress: Float? = null,
    @SerialName("active_times") val activeTimes: List<ActiveTime> = emptyList(),
    val recommendations: List<String> = emptyList()
)

@Serializable
data class ActiveTime(
    val hour: Int,
    val messages: Int
)
