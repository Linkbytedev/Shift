package com.Linkbyte.Shift.di

import com.Linkbyte.Shift.data.repository.*
import com.Linkbyte.Shift.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: MockAuthRepository
    ): AuthRepository

    // Add other repository bindings as they are implemented
}
