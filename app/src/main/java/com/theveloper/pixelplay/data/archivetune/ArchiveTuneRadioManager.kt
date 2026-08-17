package com.theveloper.pixelplay.data.archivetune

import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveTuneRadioManager @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend fun fetchRadioQueue(
        videoId: String,
        existingSongIds: Set<String> = emptySet()
    ): Pair<List<Song>, String?> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanVideoId = videoId.removePrefix("yt_").removePrefix("yt://").trim()
            val nextResult = YouTube.next(WatchEndpoint(videoId = cleanVideoId)).getOrThrow()
            
            val songs = nextResult.items
                .filter { "yt_${it.id}" !in existingSongIds && it.id != cleanVideoId }
                .map { item ->
                    val durationMs = (item.duration ?: 0) * 1000L
                    val artistName = item.artists.firstOrNull()?.name ?: "Unknown Artist"
                    Song(
                        id = "yt_${item.id}",
                        title = item.title,
                        artist = artistName,
                        artistId = -1L,
                        album = nextResult.title ?: "Radio",
                        albumId = -1L,
                        albumArtist = artistName,
                        path = "yt://${item.id}",
                        contentUriString = "yt://${item.id}",
                        albumArtUriString = item.thumbnail,
                        duration = durationMs,
                        mimeType = "audio/webm",
                        bitrate = 160000,
                        sampleRate = 44100
                    )
                }

            if (songs.isNotEmpty()) {
                runCatching {
                    musicRepository.saveOnlineSongs(songs)
                }
            }

            Pair(songs, nextResult.continuation)
        }.getOrElse { throwable ->
            Timber.e(throwable, "Failed to fetch radio queue for videoId=%s", videoId)
            Pair(emptyList(), null)
        }
    }
}
