package com.jyoti.learneaseai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<EmbeddingEntity>)

    @Query("SELECT * FROM embeddings WHERE documentName = :docName ORDER BY chunkIndex ASC")
    fun getByDocument(docName: String): Flow<List<EmbeddingEntity>>

    @Query("SELECT * FROM embeddings ORDER BY chunkIndex ASC")
    fun getAll(): Flow<List<EmbeddingEntity>>

    @Query("DELETE FROM embeddings WHERE documentName = :docName")
    suspend fun deleteByDocument(docName: String)

    @Query("DELETE FROM embeddings")
    suspend fun deleteAll()
}
