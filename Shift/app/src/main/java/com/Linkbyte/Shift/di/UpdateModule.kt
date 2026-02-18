package com.Linkbyte.Shift.di

import com.Linkbyte.Shift.data.repository.UpdateRepositoryImpl
import com.Linkbyte.Shift.domain.repository.UpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(client: OkHttpClient): UpdateRepository {
        return UpdateRepositoryImpl(client)
    }
}
