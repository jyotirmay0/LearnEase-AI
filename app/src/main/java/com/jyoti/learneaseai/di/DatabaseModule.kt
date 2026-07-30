package com.jyoti.learneaseai.di

import android.content.Context
import androidx.room.Room
import com.jyoti.learneaseai.data.local.AppDatabase
import com.jyoti.learneaseai.data.local.ChunkDao
import com.jyoti.learneaseai.data.local.DocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {


    @Provides
    @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learnease_db"
                ).build()
            return instance




    }


    @Provides
    fun provideDocumentDao(
        db: AppDatabase
    ): DocumentDao = db.documentDao()

    @Provides
    fun provideChunkDao(
        db: AppDatabase
    ): ChunkDao = db.chunkDao()
}