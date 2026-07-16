package com.jyoti.learneaseai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val documentName: String,
    val chunkIndex: Int,
    val chunkText: String,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingEntity) return false
        return id == other.id &&
                documentName == other.documentName &&
                chunkIndex == other.chunkIndex &&
                chunkText == other.chunkText &&
                embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + documentName.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + chunkText.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
