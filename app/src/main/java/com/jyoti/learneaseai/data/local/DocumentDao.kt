package com.jyoti.learneaseai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert suspend fun insert(doc: DocumentEntity)


    @Query("UPDATE documents SET status = :status, chunkCount = :count WHERE id = :id")
    suspend fun updateStatus(id: String, status: DocumentStatus, count: Int)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DocumentEntity>>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteByDocument(id: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()

    @Query("SELECT * FROM documents WHERE contentHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): DocumentEntity?
}
