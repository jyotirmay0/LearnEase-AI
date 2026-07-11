package com.jyoti.learneaseai.data.remote

data class EmbedRequest(
    val model: String="models/gemini-embedding-001",
    val content: Content
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)