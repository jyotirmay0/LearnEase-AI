package com.jyoti.learneaseai.data.remote



import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query


interface GeminiApi {

        @POST("v1beta/models/gemini-embedding-001:embedContent")
        suspend fun embedContent(
            @Query("key") apiKey: String,
            @Body request: EmbedRequest
        ): EmbedResponse
    }
