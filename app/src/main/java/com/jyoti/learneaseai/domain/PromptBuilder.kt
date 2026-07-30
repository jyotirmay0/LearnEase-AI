package com.jyoti.learneaseai.domain

import android.util.Log
import com.jyoti.learneaseai.domain.models.ScoredChunk


object PromptBuilder {

    private const val DEFAULT_MAX_CONTEXT_CHARS = 6000


    fun build(
        relevantChunks: List<ScoredChunk>,
        maxContextChars: Int = DEFAULT_MAX_CONTEXT_CHARS
    ): String {
        val contextBlock = buildContextBlock(relevantChunks, maxContextChars)

        return buildString {
            appendLine("You are LearnEase AI, a helpful study assistant.")
            appendLine("Answer the student's questions using ONLY the document context provided below.")
            appendLine("If the context does not contain enough information, say so honestly.")
            appendLine("Cite the relevant section numbers when possible.")
            appendLine("Keep answers concise and well-structured.")
            appendLine()
            appendLine("--- CONTEXT START ---")
            appendLine(contextBlock)
            Log.d("LLM", "build: {${contextBlock.toString()}}")
            appendLine("--- CONTEXT END ---")

        }
    }


    fun buildCustom(
        query: String,
        relevantChunks: List<ScoredChunk>,
        template: String,
        maxContextChars: Int = DEFAULT_MAX_CONTEXT_CHARS
    ): String {
        val contextBlock = buildContextBlock(relevantChunks, maxContextChars)
        return template
            .replace("{{CONTEXT}}", contextBlock)
            .replace("{{QUERY}}", query)
    }

    // ── Internal ──────────────────────────────────────────────────────


    private fun buildContextBlock(
        chunks: List<ScoredChunk>,
        maxChars: Int
    ): String {
        val sb = StringBuilder()
        var usedChars = 0
        var sectionNum = 1

        for (chunk in chunks) {
            val section = "[Section $sectionNum – ${chunk.documentId}]\n${chunk.chunkText}\n\n"
            if (usedChars + section.length > maxChars) break
            sb.append(section)
            usedChars += section.length
            sectionNum++
        }

        return sb.toString().trimEnd()
    }
}
