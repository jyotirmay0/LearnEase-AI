package com.jyoti.learneaseai.domain

import com.jyoti.learneaseai.data.local.EmbeddingDao
import com.jyoti.learneaseai.data.local.EmbeddingEntity
import com.jyoti.learneaseai.domain.models.ScoredChunk

/**
 * Use-case that implements the full RAG retrieval pipeline:
 *
 * 1. Embed the user's query (embedding provided by the caller).
 * 2. Load all stored document embeddings from Room.
 * 3. Rank them via cosine similarity.
 * 4. Return the top-K [ScoredChunk]s ready for prompt building.
 */
class RetrieveRelevantChunksUseCase(
    private val embeddingDao: EmbeddingDao
) {

    /**
     * @param queryEmbedding The embedding of the user's query (768-d float vector).
     * @param topK           How many chunks to return.
     * @return A list of [ScoredChunk] sorted by relevance (highest first).
     */
    suspend fun execute(
        queryEmbedding: FloatArray,
        topK: Int = 5
    ): List<ScoredChunk> {
        // 1. Fetch all stored embeddings (one-shot, no Flow)
        val allEntities: List<EmbeddingEntity> = embeddingDao.getAllOnce()

        if (allEntities.isEmpty()) return emptyList()

        // 2. Rank by cosine similarity
        val scoredIndices = CosineSimilarity.rankTopK(
            queryEmbedding = queryEmbedding,
            candidateEmbeddings = allEntities.map { it.embedding },
            topK = topK
        )

        // 3. Map back to ScoredChunk domain objects
        return scoredIndices.map { scored ->
            val entity = allEntities[scored.index]
            ScoredChunk(
                chunkText = entity.chunkText,
                score = scored.score,
                chunkIndex = entity.chunkIndex,
                documentName = entity.documentName
            )
        }
    }

    /**
     * Convenience: retrieve chunks AND build the final prompt in one call.
     */
    suspend fun retrieveAndBuildPrompt(
        query: String,
        queryEmbedding: FloatArray,
        topK: Int = 5
    ): String {
        val relevantChunks = execute(queryEmbedding, topK)
        return PromptBuilder.build(query = query, relevantChunks = relevantChunks)
    }
}
