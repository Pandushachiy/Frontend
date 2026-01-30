package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface MedicalRepository {
    suspend fun checkSymptoms(symptoms: String, duration: String? = null): Result<SymptomCheckResponse>
    suspend fun checkDrugInteractions(drugs: List<String>, includeCurrentMeds: Boolean = true): Result<DrugInteractionResponse>
    suspend fun analyzeLabResults(results: Map<String, Float>): Result<LabResultsResponse>
    suspend fun searchMedical(query: String): Result<MedicalSearchResponse>
    suspend fun getRecommendations(focusArea: String? = null): Result<HealthRecommendationsResponse>
    suspend fun getEmergencyInfo(): Result<EmergencyInfoResponse>
}

@Singleton
class MedicalRepositoryImpl @Inject constructor(
    private val api: MedicalApi
) : MedicalRepository {
    
    override suspend fun checkSymptoms(symptoms: String, duration: String?): Result<SymptomCheckResponse> {
        return try {
            val request = SymptomCheckRequest(symptoms, duration)
            val response = api.checkSymptoms(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check symptoms")
            Result.failure(e)
        }
    }
    
    override suspend fun checkDrugInteractions(drugs: List<String>, includeCurrentMeds: Boolean): Result<DrugInteractionResponse> {
        return try {
            val request = DrugInteractionRequest(drugs, includeCurrentMeds)
            val response = api.checkDrugInteractions(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check drug interactions")
            Result.failure(e)
        }
    }
    
    override suspend fun analyzeLabResults(results: Map<String, Float>): Result<LabResultsResponse> {
        return try {
            val request = LabResultsRequest(results)
            val response = api.analyzeLabResults(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze lab results")
            Result.failure(e)
        }
    }
    
    override suspend fun searchMedical(query: String): Result<MedicalSearchResponse> {
        return try {
            val request = MedicalSearchRequest(query)
            val response = api.searchMedical(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search medical info")
            Result.failure(e)
        }
    }
    
    override suspend fun getRecommendations(focusArea: String?): Result<HealthRecommendationsResponse> {
        return try {
            val response = api.getRecommendations(focusArea)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get recommendations")
            Result.failure(e)
        }
    }
    
    override suspend fun getEmergencyInfo(): Result<EmergencyInfoResponse> {
        return try {
            val response = api.getEmergencyInfo()
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get emergency info")
            Result.failure(e)
        }
    }
}
