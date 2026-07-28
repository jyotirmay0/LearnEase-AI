package com.jyoti.learneaseai.domain

import com.jyoti.learneaseai.domain.models.ScoredChunk

/**
 * Builds a context-augmented prompt for the LLM by combining the user's
 * question with the most relevant document chunks retrieved via cosine
 * similarity.
 *
 * This is the "Prompt" half of a standard RAG (Retrieval-Augmented
 * Generation) pipeline.
 */
object PromptBuilder {

    private const val DEFAULT_MAX_CONTEXT_CHARS = 6000

    /**
     * Build a RAG prompt from [relevantChunks] and the user's [query].
     *
     * @param query           The user's natural-language question.
     * @param relevantChunks  Chunks ranked by similarity (highest first).
     * @param maxContextChars Maximum characters of context to include.
     *                        Chunks are added greedily until the budget is
     *                        exhausted; remaining chunks are dropped.
     * @return A ready-to-send prompt string for the generative model.
     */
    fun build(
        query: String,
        relevantChunks: List<ScoredChunk>,
        maxContextChars: Int = DEFAULT_MAX_CONTEXT_CHARS
    ): String {
        val contextBlock = buildContextBlock(relevantChunks, maxContextChars)

        return buildString {
            appendLine("You are LearnEase AI, a helpful study assistant.")
            appendLine("Answer the student's question using ONLY the context provided below.")
            appendLine("If the context does not contain enough information, say so honestly.")
            appendLine("Cite the relevant section numbers when possible.")
            appendLine()
            appendLine("--- CONTEXT START ---")
            appendLine(contextBlock)
            appendLine("--- CONTEXT END ---")
            appendLine()
            appendLine("Question: $query")
            appendLine()
            appendLine("Answer:")
        }
    }

    /**
     * Build a prompt with a fully custom template.
     *
     * The [template] string must contain two placeholders:
     * - `{{CONTEXT}}` — replaced with the assembled context block.
     * - `{{QUERY}}`   — replaced with the user's question.
     *
     * Example template:
     * ```
     * Given these notes:\n{{CONTEXT}}\n\nQ: {{QUERY}}\nA:
     * ```
     */
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

    /**
     * Assemble numbered context sections from [chunks] while staying under
     * [maxChars]. Chunks are added in order (assumed highest-relevance-first).
     */
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
