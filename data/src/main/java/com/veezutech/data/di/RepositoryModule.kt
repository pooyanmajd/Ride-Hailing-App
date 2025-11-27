package com.veezutech.data.di

import com.veezutech.data.repository.DriverRepositoryImpl
import com.veezutech.data.repository.LocationRepositoryImpl
import com.veezutech.domain.repository.DriverRepository
import com.veezutech.domain.repository.LocationRepository
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
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindDriverRepository(impl: DriverRepositoryImpl): DriverRepository
}