package com.jyoti.learneaseai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChunkDao {
    @Insert
    suspend fun insertAll(chunks: List<ChunkEntity>)

    @Query("SELECT * FROM chunks WHERE documentId = :docId ORDER BY chunkIndex")
    suspend fun getChunksForDocument(docId: String): List<ChunkEntity>

    @Query("SELECT * FROM chunks")
    suspend fun getAll(): List<ChunkEntity>

    @Query("DELETE FROM chunks WHERE documentId = :docId")
    suspend fun deleteForDocument(docId: String)
}