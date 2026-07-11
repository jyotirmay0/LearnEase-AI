package com.jyoti.learneaseai.domain



object Chunker {
    private const val DEFAULT_CHUNK_SIZE = 500      // characters per chunk
    private const val DEFAULT_OVERLAP = 100         // overlap between consecutive chunks

    fun chunk(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<String> {

        if (text.isBlank()) return emptyList()
        val cleanedText = text
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleanedText.length <= chunkSize) {
            return listOf(cleanedText)
        }
        val chunks = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < cleanedText.length) {
            var endIndex = minOf(startIndex + chunkSize, cleanedText.length)
            // Try to break at a sentence boundary (. ! ?)
            if (endIndex < cleanedText.length) {
                val lastPeriod = cleanedText.lastIndexOf(". ", endIndex, startIndex)
                val lastExclamation = cleanedText.lastIndexOf("! ", endIndex, startIndex)
                val lastQuestion = cleanedText.lastIndexOf("? ", endIndex, startIndex)
                val bestBreak = maxOf(lastPeriod, lastExclamation, lastQuestion)
                if (bestBreak > startIndex) {
                    endIndex = bestBreak + 1 // include the punctuation
                }
            }
            chunks.add(cleanedText.substring(startIndex, endIndex).trim())
            // Move forward by (endIndex - startIndex - overlap), but at least 1 char
            startIndex += maxOf(endIndex - startIndex - overlap, 1)
        }
        return chunks
    }

    private fun String.lastIndexOf(str: String, endIndex: Int, startIndex: Int): Int {
        val searchIn = this.substring(startIndex, endIndex)
        val idx = searchIn.lastIndexOf(str)
        return if (idx >= 0) startIndex + idx else -1
    }
}
