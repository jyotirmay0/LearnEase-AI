package com.jyoti.learneaseai.data.local

import androidx.room.Entity

import androidx.room.PrimaryKey


// ---- Document table: one row per uploaded document ----
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,          // use content hash, not autoIncrement — see below
    val name: String,
    val contentHash: String,             // SHA-256 of file content —dedup key
    val chunkCount: Int,
    val status: DocumentStatus,                  // "PROCESSING", "READY", "FAILED"
    val createdAt: Long
)


