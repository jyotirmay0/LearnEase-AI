package com.jyoti.learneaseai.di

import com.jyoti.learneaseai.data.remote.ApiKeyInterceptor
import com.jyoti.learneaseai.data.remote.GeminiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn

import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


    private const val BASE_URL =
        "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    fun getClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor())
        .build()

    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit=Retrofit.Builder()
    .baseUrl(BASE_URL)
        .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()


    @Singleton
    @Provides
    fun provideApiservice(retrofit: Retrofit): GeminiApi=
        retrofit.create(GeminiApi::class.java)

}