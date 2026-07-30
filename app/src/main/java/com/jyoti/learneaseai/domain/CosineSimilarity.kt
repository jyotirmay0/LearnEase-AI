package com.jyoti.learneaseai.domain

import kotlin.math.sqrt

/**
 * Pure-math utility for cosine-similarity computations on float vectors.
 *
 * cosine_sim(A, B) = (A · B) / (‖A‖ * ‖B‖)
 *
 * Result range: [-1, 1]  (1 = identical direction, 0 = orthogonal, -1 = opposite)
 */
object CosineSimilarity {

    /**
     * Compute the cosine similarity between two equal-length float vectors.
     *
     * @throws IllegalArgumentException if the vectors differ in length or are empty.
     * @return similarity score in [-1, 1]
     */
    fun compute(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) {
            "Vectors must have the same dimensionality (${a.size} vs ${b.size})"
        }
        require(a.isNotEmpty()) { "Vectors must not be empty" }

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dot += a[i].toDouble() * b[i].toDouble()
            normA += a[i].toDouble() * a[i].toDouble()
            normB += b[i].toDouble() * b[i].toDouble()
        }

        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator == 0.0) return 0f   // zero-vector edge case

        return (dot / denominator).toFloat()
    }

    /**
     * Rank a list of candidate embeddings against the [queryEmbedding] and return
     * the indices + scores of the top-[topK] most similar candidates.
     *
     * The returned list is sorted descending by similarity score.
     */
    fun rankTopK(
        queryEmbedding: FloatArray,
        candidateEmbeddings: List<FloatArray>,
        topK: Int = 5
    ): List<ScoredIndex> {
        return candidateEmbeddings
            .mapIndexed { index, candidate ->
                ScoredIndex(index = index, score = compute(queryEmbedding, candidate))
            }
            .sortedByDescending { it.score }
            .take(topK)
    }
}


data class ScoredIndex(
    val index: Int,
    val score: Float
)
