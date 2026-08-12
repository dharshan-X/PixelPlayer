package com.theveloper.pixelplay.data.archivetune

import android.content.Context
import android.net.ConnectivityManager
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.PlayerStreamClient
import moe.rukamori.archivetune.moriextractor.StreamingExtractionManager
import moe.rukamori.archivetune.utils.StreamClientUtils
import moe.rukamori.archivetune.utils.YTPlayerUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveTuneStreamResolver @Inject constructor(
    private val extractionManager: StreamingExtractionManager
) {
    suspend fun resolveStream(
        context: Context,
        videoId: String,
        quality: AudioQuality = AudioQuality.HIGH,
        mode: StreamBackendMode = StreamBackendMode.AUTO_FALLBACK
    ): Result<ArchiveTuneStreamResult> = runCatching {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (mode == StreamBackendMode.KOIVERSE_EXTRACTOR) {
            val extracted = extractionManager.extractAudio(videoUrl = "https://www.youtube.com/watch?v=$videoId")
            val requestProfile = StreamClientUtils.resolveRequestProfile(clientParam = "WEB_REMIX")
            return@runCatching ArchiveTuneStreamResult(
                videoId = videoId,
                streamUrl = extracted.streamUrl,
                mimeType = extracted.mimeType ?: "audio/webm",
                bitrate = 160_000,
                expiresInSeconds = (extracted.streamExpiresAt - System.currentTimeMillis() / 1000).toInt().coerceAtLeast(300),
                clientName = "KOIVERSE_EXTRACTOR",
                userAgent = requestProfile.userAgent,
                origin = requestProfile.origin,
                referer = requestProfile.referer
            )
        }

        val nativeResult = YTPlayerUtils.playerResponseForPlayback(
            videoId = videoId,
            playlistId = null,
            audioQuality = quality,
            connectivityManager = connectivityManager,
            preferredStreamClient = PlayerStreamClient.WEB_REMIX
        )

        if (nativeResult.isSuccess) {
            val data = nativeResult.getOrThrow()
            val requestProfile = StreamClientUtils.resolveRequestProfile(data.streamUrl)
            return@runCatching ArchiveTuneStreamResult(
                videoId = videoId,
                streamUrl = data.streamUrl,
                mimeType = data.format.mimeType,
                bitrate = data.format.bitrate,
                expiresInSeconds = data.streamExpiresInSeconds,
                clientName = requestProfile.resolvedClientFamily,
                userAgent = requestProfile.userAgent,
                origin = requestProfile.origin,
                referer = requestProfile.referer
            )
        }

        if (mode == StreamBackendMode.AUTO_FALLBACK) {
            val extracted = extractionManager.extractAudio(videoUrl = "https://www.youtube.com/watch?v=$videoId")
            val requestProfile = StreamClientUtils.resolveRequestProfile(clientParam = "WEB_REMIX")
            return@runCatching ArchiveTuneStreamResult(
                videoId = videoId,
                streamUrl = extracted.streamUrl,
                mimeType = extracted.mimeType ?: "audio/webm",
                bitrate = 160_000,
                expiresInSeconds = (extracted.streamExpiresAt - System.currentTimeMillis() / 1000).toInt().coerceAtLeast(300),
                clientName = "KOIVERSE_EXTRACTOR",
                userAgent = requestProfile.userAgent,
                origin = requestProfile.origin,
                referer = requestProfile.referer
            )
        }

        throw nativeResult.exceptionOrNull() ?: IllegalStateException("Failed to resolve stream for $videoId")
    }
}
