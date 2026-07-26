package com.jyoti.learneaseai.data.repository

import android.app.Application
import android.util.Log
import com.jyoti.learneaseai.data.local.AppDatabase
import com.jyoti.learneaseai.data.remote.Content
import com.jyoti.learneaseai.data.remote.EmbedRequest
import com.jyoti.learneaseai.data.remote.GeminiApi
import com.jyoti.learneaseai.data.remote.NetworkModule.api
import com.jyoti.learneaseai.data.remote.Part
import com.jyoti.learneaseai.domain.models.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class DocumentRepositoryImpl(  private val db: AppDatabase,
                               api: GeminiApi) {


    private val embeddingDao = db.embeddingDao()
    suspend fun embedChunksThrottled(
        chunks: List<String>, concurrency: Int = 3
    ): List<FloatArray> = coroutineScope {
        chunks.chunked(concurrency).flatMapIndexed { waveIndex, wave ->
            Log.d("embed", "Processing wave ${waveIndex + 1}, chunks: ${wave.size}")

            // Delay between waves to avoid rate limiting (skip first wave)
            if (waveIndex > 0) delay(1500)

            wave.map { chunk ->
                async(Dispatchers.IO) {
                    embedWithRetry(  chunk)
                }
            }.awaitAll()
        }
    }

    private suspend fun embedWithRetry(
       chunk: String, maxRetries: Int = 3
    ): FloatArray {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return api.embedContent(EmbedRequest(
                        content = Content(listOf(Part(chunk))),
                    )
                ).embedding.values.toFloatArray()
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    val waitTime = (2000L * (attempt + 1)) // 2s, 4s, 6s
                    Log.w("embed", "Rate limited (429), retrying in ${waitTime}ms (attempt ${attempt + 1}/$maxRetries)")
                    delay(waitTime)
                    lastException = e
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: Exception("Embedding failed after $maxRetries retries")
    }

    fun getAllDoc(): Flow<List<Document>> {
        return embeddingDao.getAll().map { entities ->
            entities.map {
                    entity ->
                Document(
                    id = entity.id,
                    name = entity.documentName
                )
            }
        }

}


}
