package com.jyoti.learneaseai.data.repository

import com.jyoti.learneaseai.data.local.ChunkDao
import com.jyoti.learneaseai.data.local.ChunkEntity
import com.jyoti.learneaseai.data.local.DocumentDao
import com.jyoti.learneaseai.domain.CosineSimilarity
import com.jyoti.learneaseai.domain.PromptBuilder
import com.jyoti.learneaseai.domain.models.ScoredChunk
import javax.inject.Inject

/**
 * Use-case that implements the full RAG retrieval pipeline:
 *
 * 1. Embed the user's query (embedding provided by the caller).
 * 2. Load all stored document embeddings from Room.
 * 3. Rank them via cosine similarity.
 * 4. Return the top-K [com.jyoti.learneaseai.domain.models.ScoredChunk]s ready for prompt building.
 */
class RetrievalReposatory
    @Inject constructor(
    private val documentDao: DocumentDao,
    private val chunkDao: ChunkDao
) {

    /**
     * @param queryEmbedding The embedding of the user's query (768-d float vector).
     * @param topK           How many chunks to return.
     * @return A list of [com.jyoti.learneaseai.domain.models.ScoredChunk] sorted by relevance (highest first).
     */
    suspend fun retrieve(
        queryEmbedding: FloatArray,
        topK: Int = 5,
        docId:String,
    ): List<ScoredChunk> {

        val allEntities: List<ChunkEntity> = chunkDao.getChunksForDocument(docId)

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
                chunkText = entity.text,
                score = scored.score,
                chunkIndex = entity.chunkIndex,
                documentId = entity.documentId
            )
        }
    }

    /**
     * Convenience: retrieve chunks AND build the final prompt in one call.
     */
    suspend fun retrieveAndBuildPrompt(
        query: String,
        queryEmbedding: FloatArray,
        topK: Int = 5,
        docID: String
    ): String {
        val relevantChunks = retrieve(queryEmbedding, topK,docID)
        return PromptBuilder.build(query = query, relevantChunks = relevantChunks)
    }
}