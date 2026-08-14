package com.jyoti.learneaseai.domain

object Chunker {

    private const val DEFAULT_CHUNK_SIZE = 500      // characters
    private const val DEFAULT_OVERLAP = 100         // characters




    fun chunk(text: String, chunkSize: Int = 800, overlap: Int = 120): List<String> {
        if (text.isBlank()) return emptyList()
        val step = chunkSize - overlap
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + chunkSize, text.length)
            if (end < text.length) {
                // Scan back within the overlap window to avoid cutting mid-word
                val scanFrom = maxOf(end - overlap, start + 1)
                val ws = (end downTo scanFrom).firstOrNull { text[it - 1].isWhitespace() }
                if (ws != null) end = ws
            }
            val chunk = text.substring(start, end).trim()
            if (chunk.isNotEmpty()) chunks.add(chunk)
            start += step
        }
        return chunks
    }
    /**
     * Memory-efficient chunker that avoids creating a full cleaned copy of the text.
     * Instead, it streams through the input character-by-character, collapsing
     * whitespace on-the-fly and emitting chunks as it goes.
     */
    fun chunk1(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<String> {

        if (text.isBlank()) return emptyList()

        val chunks = mutableListOf<String>()

        // We'll build chunks incrementally without holding the full cleaned text.
        // 'buffer' holds the current window we're building a chunk from.
        val buffer = StringBuilder(chunkSize + overlap)
        var prevWasSpace = true  // treat start as if preceded by space to skip leading whitespace

        for (char in text) {
            if (char.isWhitespace()) {
                if (!prevWasSpace && buffer.isNotEmpty()) {
                    buffer.append(' ')
                }
                prevWasSpace = true
            } else {
                buffer.append(char)
                prevWasSpace = false
            }

            // When buffer is large enough, try to emit a chunk
            if (buffer.length >= chunkSize) {
                val content = buffer.toString().trim()
                if (content.isNotEmpty()) {
                    val breakIdx = findBestBreak(content, 0, minOf(chunkSize, content.length))
                    val chunk = content.substring(0, breakIdx).trim()
                    if (chunk.isNotEmpty()) {
                        chunks.add(chunk)
                    }

                    // Keep overlap portion for the next chunk
                    val remaining = content.substring(maxOf(breakIdx - overlap, 0))
                    buffer.clear()
                    buffer.append(remaining)
                }
            }
        }

        // Flush whatever is left in the buffer
        val remaining = buffer.toString().trim()
        if (remaining.isNotEmpty()) {
            // If the remaining text is too large, chunk it too
            if (remaining.length > chunkSize) {
                var start = 0
                while (start < remaining.length) {
                    if (remaining.length - start <= chunkSize) {
                        val piece = remaining.substring(start).trim()
                        if (piece.isNotEmpty()) chunks.add(piece)
                        break
                    }
                    val tentativeEnd = minOf(start + chunkSize, remaining.length)
                    val end = findBestBreak(remaining, start, tentativeEnd)
                    val piece = remaining.substring(start, end).trim()
                    if (piece.isNotEmpty()) chunks.add(piece)
                    start = maxOf(end - overlap, start + 1)
                }
            } else {
                chunks.add(remaining)
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
        for (i in tentativeEnd downTo start + 1) {
            val c = text[i - 1]
            if (c == '.' || c == '!' || c == '?') {
                return i
            }
        }

        // Otherwise break on whitespace
        for (i in tentativeEnd downTo start + 1) {
            if (text[i - 1].isWhitespace()) {
                return i
            }
        }

        // Fallback
        return tentativeEnd
    }
}