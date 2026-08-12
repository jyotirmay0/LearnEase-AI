package com.jyoti.learneaseai.domain.embedding

import kotlin.math.sqrt

object EmbeddingMath {
     fun meanPool(tokenEmbeddings: Array<FloatArray>, attentionMask: IntArray): FloatArray {
        val dim = tokenEmbeddings[0].size
        val pooled = FloatArray(dim)
        var validTokens = 0
        for (i in tokenEmbeddings.indices) {
            if (attentionMask[i] == 1) {
                for (j in 0 until dim) pooled[j] += tokenEmbeddings[i][j]
                validTokens++
            }
        }
        for (j in 0 until dim) pooled[j] /= validTokens.coerceAtLeast(1)
        return pooled  // this is your final 384-dim embedding
    }

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) sumSquares += v * v
        val norm = sqrt(sumSquares)
        return if (norm > 0f) FloatArray(vector.size) { vector[it] / norm } else vector
    }
}
