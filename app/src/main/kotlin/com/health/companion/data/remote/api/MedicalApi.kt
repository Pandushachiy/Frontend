package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

/**
 * Medical Assistant API — Симптомы, Лекарства, Анализы
 */
interface MedicalApi {
    
    // ==================== SYMPTOMS ====================
    
    @POST("medical/symptoms")
    suspend fun checkSymptoms(@Body request: SymptomCheckRequest): SymptomCheckResponse
    
    // ==================== DRUG INTERACTIONS ====================
    
    @POST("medical/drug-interactions")
    suspend fun checkDrugInteractions(@Body request: DrugInteractionRequest): DrugInteractionResponse
    
    // ==================== LAB RESULTS ====================
    
    @POST("medical/lab-results")
    suspend fun analyzeLabResults(@Body request: LabResultsRequest): LabResultsResponse
    
    // ==================== MEDICAL SEARCH ====================
    
    @POST("medical/search")
    suspend fun searchMedical(@Body request: MedicalSearchRequest): MedicalSearchResponse
    
    // ==================== RECOMMENDATIONS ====================
    
    @GET("medical/recommendations")
    suspend fun getRecommendations(
        @Query("focus_area") focusArea: String? = null
    ): HealthRecommendationsResponse
    
    // ==================== EMERGENCY INFO ====================
    
    @GET("medical/emergency-info")
    suspend fun getEmergencyInfo(): EmergencyInfoResponse
}

// ==================== SYMPTOMS MODELS ====================

@Serializable
data class SymptomCheckRequest(
    val symptoms: String,
    val duration: String? = null
)

@Serializable
data class SymptomCheckResponse(
    val symptoms: List<String> = emptyList(),
    val severity: String, // low, medium, high, urgent
    val recommendations: List<String> = emptyList(),
    @SerialName("when_to_see_doctor") val whenToSeeDoctor: String = "",
    @SerialName("specialist_type") val specialistType: String? = null,
    @SerialName("possible_causes") val possibleCauses: List<String> = emptyList(),
    val disclaimer: String = ""
)

// ==================== DRUG INTERACTIONS MODELS ====================

@Serializable
data class DrugInteractionRequest(
    val drugs: List<String>,
    @SerialName("include_current_medications") val includeCurrentMedications: Boolean = true
)

@Serializable
data class DrugInteractionResponse(
    @SerialName("drugs_checked") val drugsChecked: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    @SerialName("detailed_analysis") val detailedAnalysis: String? = null,
    val safe: Boolean = true,
    val disclaimer: String = ""
)

// ==================== LAB RESULTS MODELS ====================

@Serializable
data class LabResultsRequest(
    val results: Map<String, Float>
)

@Serializable
data class LabResultsResponse(
    val analyses: List<LabAnalysis> = emptyList(),
    @SerialName("critical_values") val criticalValues: List<String> = emptyList(),
    val concerns: List<String> = emptyList(),
    val explanation: String = "",
    val recommendation: String = "",
    val disclaimer: String = ""
)

@Serializable
data class LabAnalysis(
    val name: String,
    val value: Float,
    val unit: String = "",
    val status: String, // normal, low, high, critical_low, critical_high
    @SerialName("reference_low") val referenceLow: Float? = null,
    @SerialName("reference_high") val referenceHigh: Float? = null
)

// ==================== MEDICAL SEARCH MODELS ====================

@Serializable
data class MedicalSearchRequest(
    val query: String
)

@Serializable
data class MedicalSearchResponse(
    val query: String,
    val information: String = "",
    val citations: List<String> = emptyList(),
    val disclaimer: String = ""
)

// ==================== RECOMMENDATIONS MODELS ====================

@Serializable
data class HealthRecommendationsResponse(
    @SerialName("focus_area") val focusArea: String = "",
    val recommendations: String = "",
    val personalized: Boolean = false,
    val tips: List<String> = emptyList(),
    val disclaimer: String = ""
)

// ==================== EMERGENCY INFO MODELS ====================

@Serializable
data class EmergencyInfoResponse(
    @SerialName("emergency_numbers") val emergencyNumbers: EmergencyNumbers,
    @SerialName("when_to_call_emergency") val whenToCallEmergency: List<String> = emptyList(),
    @SerialName("fast_stroke_check") val fastStrokeCheck: FastStrokeCheck? = null,
    @SerialName("first_aid_basics") val firstAidBasics: List<FirstAidItem> = emptyList(),
    val disclaimer: String = ""
)

@Serializable
data class EmergencyNumbers(
    val ambulance: String = "103",
    val universal: String = "112",
    @SerialName("poison_control") val poisonControl: String = "8-495-628-16-87"
)

@Serializable
data class FastStrokeCheck(
    @SerialName("F") val face: String = "Лицо: попросите улыбнуться — асимметрия?",
    @SerialName("A") val arms: String = "Руки: поднять обе — одна слабее?",
    @SerialName("S") val speech: String = "Речь: повторить фразу — невнятно?",
    @SerialName("T") val time: String = "Время: сразу вызывайте 103!"
)

@Serializable
data class FirstAidItem(
    val situation: String,
    val steps: List<String>
)
