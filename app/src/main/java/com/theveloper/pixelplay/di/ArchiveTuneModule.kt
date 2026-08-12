package com.theveloper.pixelplay.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moe.rukamori.archivetune.moriextractor.BearerTokenRepository
import moe.rukamori.archivetune.moriextractor.StreamingExtractionManager
import com.theveloper.pixelplay.data.archivetune.DataStoreBearerTokenRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArchiveTuneModule {

    @Binds
    @Singleton
    abstract fun bindBearerTokenRepository(
        impl: DataStoreBearerTokenRepository
    ): BearerTokenRepository

    companion object {
        @Provides
        @Singleton
        fun provideStreamingExtractionManager(
            tokenRepository: BearerTokenRepository
        ): StreamingExtractionManager {
            return StreamingExtractionManager(
                tokenRepository = tokenRepository,
                baseUrl = "https://archivetune-api.koiiverse.cloud"
            )
        }
    }
}
