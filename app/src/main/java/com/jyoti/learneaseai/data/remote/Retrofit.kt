package com.jyoti.learneaseai.data.remote

import com.jyoti.learneaseai.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ApiKeyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
            .newBuilder()
            .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .build()

        return chain.proceed(request)
    }
}