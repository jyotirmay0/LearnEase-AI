package com.jyoti.learneaseai.domain.models

/**
 * Represents a document chunk that has been scored for relevance
 * against a user query via cosine similarity.
 *
 * @property chunkText  The original text of the chunk.
 * @property score      Cosine similarity score (higher = more relevant).
 * @property chunkIndex The position of this chunk in the original document.
 * @property documentName The source document this chunk belongs to.
 */
data class ScoredChunk(
    val chunkText: String,
    val score: Float,
    val chunkIndex: Int,
    val documentId: String
)
