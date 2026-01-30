package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface LifeContextRepository {
    // Questionnaire
    suspend fun getQuestionnaire(section: String? = null): Result<QuestionnaireResponse>
    suspend fun saveAnswers(answers: Map<String, Any>): Result<SaveAnswersResponse>
    suspend fun getProfile(): Result<UserProfile>
    
    // Important Dates
    suspend fun getImportantDates(): Result<List<ImportantDate>>
    suspend fun addImportantDate(date: ImportantDateCreate): Result<AddDateResponse>
    suspend fun updateImportantDate(id: String, update: ImportantDateUpdate): Result<ImportantDate>
    suspend fun deleteImportantDate(id: String): Result<Unit>
    
    // Important People
    suspend fun getImportantPeople(): Result<List<ImportantPerson>>
    suspend fun addImportantPerson(person: ImportantPersonCreate): Result<AddPersonResponse>
    suspend fun updateImportantPerson(id: String, update: ImportantPersonUpdate): Result<ImportantPerson>
    suspend fun deleteImportantPerson(id: String): Result<Unit>
    
    // Patterns
    suspend fun getLifePatterns(): Result<LifePatternsResponse>
}

@Singleton
class LifeContextRepositoryImpl @Inject constructor(
    private val api: LifeContextApi
) : LifeContextRepository {
    
    // ==================== QUESTIONNAIRE ====================
    
    override suspend fun getQuestionnaire(section: String?): Result<QuestionnaireResponse> {
        return try {
            val response = api.getQuestionnaire(section)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get questionnaire")
            Result.failure(e)
        }
    }
    
    override suspend fun saveAnswers(answers: Map<String, Any>): Result<SaveAnswersResponse> {
        return try {
            val request = QuestionnaireRequest(answers)
            val response = api.saveAnswers(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save answers")
            Result.failure(e)
        }
    }
    
    override suspend fun getProfile(): Result<UserProfile> {
        return try {
            val response = api.getProfile()
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get profile")
            Result.failure(e)
        }
    }
    
    // ==================== IMPORTANT DATES ====================
    
    override suspend fun getImportantDates(): Result<List<ImportantDate>> {
        return try {
            val response = api.getImportantDates()
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get important dates")
            Result.failure(e)
        }
    }
    
    override suspend fun addImportantDate(date: ImportantDateCreate): Result<AddDateResponse> {
        return try {
            val response = api.addImportantDate(date)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add important date")
            Result.failure(e)
        }
    }
    
    override suspend fun updateImportantDate(id: String, update: ImportantDateUpdate): Result<ImportantDate> {
        return try {
            val response = api.updateImportantDate(id, update)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update important date")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteImportantDate(id: String): Result<Unit> {
        return try {
            api.deleteImportantDate(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete important date")
            Result.failure(e)
        }
    }
    
    // ==================== IMPORTANT PEOPLE ====================
    
    override suspend fun getImportantPeople(): Result<List<ImportantPerson>> {
        return try {
            val response = api.getImportantPeople()
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get important people")
            Result.failure(e)
        }
    }
    
    override suspend fun addImportantPerson(person: ImportantPersonCreate): Result<AddPersonResponse> {
        return try {
            val response = api.addImportantPerson(person)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add important person")
            Result.failure(e)
        }
    }
    
    override suspend fun updateImportantPerson(id: String, update: ImportantPersonUpdate): Result<ImportantPerson> {
        return try {
            val response = api.updateImportantPerson(id, update)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update important person")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteImportantPerson(id: String): Result<Unit> {
        return try {
            api.deleteImportantPerson(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete important person")
            Result.failure(e)
        }
    }
    
    // ==================== PATTERNS ====================
    
    override suspend fun getLifePatterns(): Result<LifePatternsResponse> {
        return try {
            val response = api.getLifePatterns()
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get life patterns")
            Result.failure(e)
        }
    }
}
