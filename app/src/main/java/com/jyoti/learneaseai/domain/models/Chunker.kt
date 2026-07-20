package com.jyoti.learneaseai.domain

object Chunker {

    private const val DEFAULT_CHUNK_SIZE = 500      // characters
    private const val DEFAULT_OVERLAP = 100         // characters

    fun chunk(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<String> {

        if (text.isBlank()) return emptyList()

        val cleaned = text
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.length <= chunkSize) {
            return listOf(cleaned)
        }

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < cleaned.length) {

            // Last chunk
            if (cleaned.length - start <= chunkSize) {
                chunks.add(cleaned.substring(start).trim())
                break
            }

            val tentativeEnd = minOf(start + chunkSize, cleaned.length)

            val end = findBestBreak(cleaned, start, tentativeEnd)

            chunks.add(cleaned.substring(start, end).trim())

            if (end >= cleaned.length) break

            // Overlap
            start = maxOf(end - overlap, 0)

            // Don't start in the middle of a word
            while (
                start < cleaned.length &&
                start > 0 &&
                cleaned[start] != ' ' &&
                cleaned[start - 1] != ' '
            ) {
                start++
            }
        }

        return chunks
    }

    /**
     * Try to end on a sentence.
     * If impossible, end on a space.
     */
    private fun findBestBreak(
        text: String,
        start: Int,
        tentativeEnd: Int
    ): Int {

        // Search backwards for sentence ending
        for (i in tentativeEnd downTo start) {
            val c = text[i - 1]
            if (c == '.' || c == '!' || c == '?') {
                return i
            }
        }

        // Otherwise break on whitespace
        for (i in tentativeEnd downTo start) {
            if (text[i - 1].isWhitespace()) {
                return i
            }
        }

        // Fallback
        return tentativeEnd
    }
}