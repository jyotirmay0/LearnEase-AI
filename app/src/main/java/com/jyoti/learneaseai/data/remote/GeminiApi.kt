package com.jyoti.learneaseai.data.remote



import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query


interface GeminiApi {

        @POST("v1beta/models/gemini-embedding-2:embedContent")
        suspend fun embedContent(
            @Body request: EmbedRequest
        ): EmbedResponse
    }
