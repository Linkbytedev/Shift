package com.Linkbyte.Shift.di

import com.Linkbyte.Shift.data.repository.FriendRepositoryImpl
import com.Linkbyte.Shift.domain.repository.FriendRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FriendModule {

    @Binds
    @Singleton
    abstract fun bindFriendRepository(
        friendRepositoryImpl: FriendRepositoryImpl
    ): FriendRepository
}
