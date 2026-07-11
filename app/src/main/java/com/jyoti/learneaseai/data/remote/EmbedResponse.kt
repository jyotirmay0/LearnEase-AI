package com.jyoti.learneaseai.data.remote

data class EmbedResponse(
    val embedding: Embedding
)

data class Embedding(
    val values: List<Float>
)
