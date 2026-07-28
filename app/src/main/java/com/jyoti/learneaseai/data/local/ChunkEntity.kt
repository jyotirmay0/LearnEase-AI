package com.jyoti.learneaseai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chunks",
    foreignKeys = [ForeignKey(
        entity = DocumentEntity::class,
        parentColumns = ["id"],
        childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE      // delete doc → auto-deletes its chunks
    )],
    indices = [Index("documentId")]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: String,              // FK → DocumentEntity.id
    val chunkIndex: Int,
    val text: String,
    val embedding: FloatArray
) {
    override fun equals(other: Any?) = other is ChunkEntity &&
            id == other.id && documentId == other.documentId &&
            chunkIndex == other.chunkIndex && text == other.text &&
            embedding.contentEquals(other.embedding)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}