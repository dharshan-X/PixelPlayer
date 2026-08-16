package com.theveloper.pixelplay.data.service.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ExoPlayer's disk cache for online and streaming audio tracks.
 * Provides a 512 MB LRU cache backed by StandaloneDatabaseProvider so
 * streamed tracks from YouTube Music / ArchiveTune and cloud sources
 * are cached locally for fast repeat playback and offline replay.
 */
@Singleton
@OptIn(UnstableApi::class)
class OnlineMediaCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "OnlineMediaCache"
        private const val MAX_CACHE_SIZE_BYTES = 512L * 1024L * 1024L // 512 MB
    }

    private val cacheDir = File(context.cacheDir, "online_media_cache")

    @Volatile
    private var simpleCacheInstance: SimpleCache? = null

    val cache: SimpleCache
        get() {
            simpleCacheInstance?.let { return it }
            return synchronized(this) {
                simpleCacheInstance ?: run {
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs()
                    }
                    val databaseProvider = StandaloneDatabaseProvider(context)
                    val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
                    SimpleCache(cacheDir, evictor, databaseProvider).also {
                        simpleCacheInstance = it
                        Timber.tag(TAG).d("Initialized ExoPlayer SimpleCache in %s with limit %d MB", cacheDir.absolutePath, MAX_CACHE_SIZE_BYTES / (1024 * 1024))
                    }
                }
            }
        }

    fun release() {
        synchronized(this) {
            simpleCacheInstance?.release()
            simpleCacheInstance = null
        }
    }
}
