package com.jyoti.learneaseai.data.remote

data class EmbedRequest(
    val model: String="models/gemini-embedding-001",
    val content: Content,
    val taskType:String= "RETRIEVAL_DOCUMENT",
    val output_dimensionality: Int=768

)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)