package com.health.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.health.companion.data.local.database.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)
    
    @Update
    suspend fun update(document: DocumentEntity)
    
    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>>
    
    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    suspend fun getAllDocuments(): List<DocumentEntity>
    
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): DocumentEntity?
    
    @Query("UPDATE documents SET status = :status WHERE id = :documentId")
    suspend fun updateStatus(documentId: String, status: String)
    
    @Query("UPDATE documents SET extractedText = :text, status = 'processed' WHERE id = :documentId")
    suspend fun updateExtractedText(documentId: String, text: String)
    
    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteById(documentId: String)
}
