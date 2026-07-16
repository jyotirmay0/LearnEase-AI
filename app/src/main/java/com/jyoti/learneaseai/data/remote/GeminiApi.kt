package com.jyoti.learneaseai.data.remote



import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query


interface GeminiApi {

        @POST("v1beta/models/gemini-embedding-2:embedContent")
        suspend fun embedContent(
            @Header("x-goog-api-key") apiKey: String,
            @Body request: EmbedRequest
        ): EmbedResponse
    }
