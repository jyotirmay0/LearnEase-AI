package com.jyoti.learneaseai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.jyoti.learneaseai.data.local.ChunkDao
import com.jyoti.learneaseai.data.local.ChunkEntity
import com.jyoti.learneaseai.data.local.DocumentDao
import com.jyoti.learneaseai.data.local.DocumentEntity
import com.jyoti.learneaseai.data.local.DocumentStatus
import com.jyoti.learneaseai.data.remote.model.Content
import com.jyoti.learneaseai.data.remote.model.EmbedRequest
import com.jyoti.learneaseai.data.remote.GeminiApi
import com.jyoti.learneaseai.data.remote.model.Part
import com.jyoti.learneaseai.domain.Chunker
import com.jyoti.learneaseai.domain.models.Document
import com.jyoti.learneaseai.pdf.PdfExatractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val chunkDao: ChunkDao, private val documentDao: DocumentDao,
     private val api: GeminiApi,
    @ApplicationContext private val context: Context,
    private val pdfExatractor: PdfExatractor)
{




    suspend fun processDocument  (docName:String,uri: Uri){
        val hash=calculatePdfHash(context,uri)
        if(documentDao.getByHash(hash)!=null) {
            Log.e("Repository", "Dpcument already exist", )

            return}

        val docId = UUID.randomUUID().toString()

        documentDao.insert(
            DocumentEntity(
                docId,
                docName,
                hash,
                chunkCount = 0,
                status = DocumentStatus.PROCESSING,
                createdAt = System.currentTimeMillis()
            )
        )

        try {
            // Process page-by-page to avoid OOM on large PDFs
            val allChunks = mutableListOf< String>()
            pdfExatractor.extractTextPageByPage(uri) { pageText ->
                allChunks.addAll(Chunker.chunk(pageText))
            }
            val chunktexts = allChunks.toList()
            val embeddings=getEmbedChunks(chunktexts,5)
            saveEmbeddingsToDb(chunktexts,embeddings,docId)
            documentDao.updateStatus(docId, DocumentStatus.READY,chunktexts.size)
        }catch (e: Exception)
        {
            documentDao.updateStatus(
                id = hash,
                status = DocumentStatus.FAILED,
                count = 0
            )
            Log.e("Repository", "Dpcument coudnot processs", e)

          throw e
        }





    }


    suspend fun getEmbedChunks(
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
        return documentDao.getAll().map { entities ->
            entities.map {
                    entity ->
                Document(
                    id = entity.id,
                    name = entity.name
                )
            }
        }

}
    suspend fun getDocumentbyId(docID: String): DocumentEntity?{
        return withContext(Dispatchers.IO){
           documentDao.getByDocID(docID)
        }


    }

    suspend fun getChunksbyDocumentId(docId: String): List<String>{
        return withContext(Dispatchers.IO){
            chunkDao.getChunksForDocument(docId).map {
                chunkEntity ->
                chunkEntity.text
            }
        }
    }


    suspend fun saveEmbeddingsToDb(chunks: List<String>, embeddings: List<FloatArray>,docId: String) {



        if (chunks.size != embeddings.size) {
            Log.e("embed", "Chunks and embeddings size mismatch!")
            return
        }

        val entities = chunks.mapIndexed { index, chunkText ->
            ChunkEntity(
                documentId = docId,
                chunkIndex = index,
                text = chunkText,
                embedding = embeddings[index]
            )
        }

        chunkDao.insertAll(entities)
        Log.d("embed", "Saved ${entities.size} embeddings to database for: $docId")
    }




    private fun calculatePdfHash(
        context: Context,
        uri: Uri
    ): String {

        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver.openInputStream(uri)?.use { input ->

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest()
            .joinToString("") { "%02x".format(it) }
    }
}
