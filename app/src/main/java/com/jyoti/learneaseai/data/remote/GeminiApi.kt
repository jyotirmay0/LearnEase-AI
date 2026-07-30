package com.jyoti.learneaseai.data.remote



import com.jyoti.learneaseai.data.remote.model.EmbedRequest
import com.jyoti.learneaseai.data.remote.model.EmbedResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface GeminiApi {

        @POST("v1beta/models/gemini-embedding-2:embedContent")
        suspend fun embedContent(
            @Body request: EmbedRequest
        ): EmbedResponse
    }
