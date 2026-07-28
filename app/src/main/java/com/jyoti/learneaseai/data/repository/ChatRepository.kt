package com.jyoti.learneaseai.data.repository

import android.util.Log
import com.jyoti.learneaseai.data.local.ChunkDao
import com.jyoti.learneaseai.data.local.DocumentDao
import com.jyoti.learneaseai.data.remote.model.Content
import com.jyoti.learneaseai.data.remote.model.EmbedRequest
import com.jyoti.learneaseai.data.remote.GeminiApi
import com.jyoti.learneaseai.data.remote.model.Part
import com.jyoti.learneaseai.domain.CosineSimilarity
import com.jyoti.learneaseai.domain.LocalLlmEngine
import com.jyoti.learneaseai.domain.PromptBuilder
import com.jyoti.learneaseai.domain.models.ScoredChunk
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository that implements the full RAG pipeline:
 *
 *  1. Embed the user's query          → Gemini cloud API (RETRIEVAL_QUERY)
 *  2. Find the most relevant chunks   → Cosine similarity against Room DB
 *  3. Build a context-augmented prompt → [PromptBuilder]
 *  4. Generate an answer              → On-device Gemma via [LocalLlmEngine]
 */
class ChatRepository @Inject constructor(
    private val api: GeminiApi,
    private val documentDao: DocumentDao,
    private val chunkDao: ChunkDao,
    private val localLlm: LocalLlmEngine
) {

    companion object {
        private const val TAG = "ChatRepository"
        private const val TOP_K = 5
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Full RAG answer (non-streaming).
     *
     * @param question The user's natural-language question.
     * @return The complete generated answer string.
     */
    suspend fun answer(question: String,docId: String): String {
        val prompt = buildRagPrompt(question,docId)
        Log.d(TAG, "Sending prompt to local LLM (${prompt.length} chars)")
        return localLlm.generateFull(prompt)
    }

    /**
     * Full RAG answer (streaming, token-by-token).
     *
     * @param question The user's natural-language question.
     * @return A [Flow] emitting tokens as they are generated.
     */
    suspend fun answerStream(question: String,docId: String): Flow<String> {
        val prompt = buildRagPrompt(question,docId)
        Log.d(TAG, "Streaming prompt to local LLM (${prompt.length} chars)")
        return localLlm.generate(prompt)
    }

    // ── Internal pipeline ─────────────────────────────────────────────

    /**
     * Steps 1-3 of the RAG pipeline:
     * embed query → similarity search → build prompt.
     */
    private suspend fun buildRagPrompt(question: String,documetId: String): String {
        // 1. Embed the query via Gemini cloud (taskType = RETRIEVAL_QUERY)
        val queryEmbedding = embedQuery(question)
        Log.d(TAG, "Query embedded (${queryEmbedding.size} dims)")

        // 2. Fetch all stored document embeddings and rank by similarity
        val allEntities = chunkDao.getChunksForDocument(documetId)
        if (allEntities.isEmpty()) {
            Log.w(TAG, "No document embeddings in DB — returning raw question")
            return PromptBuilder.build(question, emptyList())
        }

        val scoredIndices = CosineSimilarity.rankTopK(
            queryEmbedding = queryEmbedding,
            candidateEmbeddings = allEntities.map { it.embedding },
            topK = TOP_K
        )

        val topChunks = scoredIndices.map { scored ->
            val entity = allEntities[scored.index]
            ScoredChunk(
                chunkText = entity.text,
                score = scored.score,
                chunkIndex = entity.chunkIndex,
                documentId = entity.documentId
            )
        }

        Log.d(TAG, "Top-$TOP_K chunks: ${topChunks.map { 
            "${it.documentId}#${it.chunkIndex} (${String.format("%.3f", it.score)})"
        }}")

        // 3. Build the prompt
        return PromptBuilder.build(question, topChunks)
    }

    /**
     * Embed a user query using the Gemini embedding API with
     * taskType = RETRIEVAL_QUERY (different from document embedding).
     */
    private suspend fun embedQuery(question: String): FloatArray {
        val request = EmbedRequest(
            content = Content(parts = listOf(Part(text = question))),
            taskType = "RETRIEVAL_QUERY"
        )
        return api.embedContent( request)
            .embedding.values.toFloatArray()
    }
}
