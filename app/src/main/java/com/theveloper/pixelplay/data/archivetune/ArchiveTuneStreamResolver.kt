package com.theveloper.pixelplay.data.archivetune

import android.content.Context
import android.net.ConnectivityManager
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.PlayerStreamClient
import moe.rukamori.archivetune.moriextractor.BearerTokenRepository
import moe.rukamori.archivetune.moriextractor.StreamingExtractionManager
import moe.rukamori.archivetune.utils.StreamClientUtils
import moe.rukamori.archivetune.utils.YTPlayerUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveTuneStreamResolver @Inject constructor(
    private val extractionManager: StreamingExtractionManager,
    private val tokenRepository: BearerTokenRepository
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

        // Try clients in order: IOS, ANDROID_VR, ANDROID_MUSIC, TVHTML5 first (immune to web poToken 403 CDN blocks), then WEB_REMIX.
        val clientCandidates = listOf(
            PlayerStreamClient.IOS,
            PlayerStreamClient.ANDROID_VR,
            PlayerStreamClient.ANDROID_MUSIC,
            PlayerStreamClient.TVHTML5,
            PlayerStreamClient.WEB_REMIX
        )

        var lastException: Throwable? = null
        for (client in clientCandidates) {
            val nativeResult = YTPlayerUtils.playerResponseForPlayback(
                videoId = videoId,
                playlistId = null,
                audioQuality = quality,
                connectivityManager = connectivityManager,
                preferredStreamClient = client
            )

            if (nativeResult.isSuccess) {
                val data = nativeResult.getOrThrow()
                val requestProfile = StreamClientUtils.resolveRequestProfile(data.streamUrl)

                Timber.tag("ArchiveTuneStreamResolver").d(
                    "Successfully resolved stream for %s using client %s (format=%s)",
                    videoId,
                    requestProfile.resolvedClientFamily,
                    data.format.mimeType
                )

                return@runCatching ArchiveTuneStreamResult(
                    videoId = videoId,
                    streamUrl = data.streamUrl,
                    mimeType = data.format.mimeType.substringBefore(';').trim(),
                    bitrate = data.format.bitrate,
                    expiresInSeconds = data.streamExpiresInSeconds,
                    clientName = requestProfile.resolvedClientFamily,
                    userAgent = requestProfile.userAgent,
                    origin = requestProfile.origin,
                    referer = requestProfile.referer
                )
            } else {
                lastException = nativeResult.exceptionOrNull()
                Timber.tag("ArchiveTuneStreamResolver").w(
                    lastException,
                    "Stream resolution failed with client=%s for videoId=%s",
                    client,
                    videoId
                )
            }
        }

        // Native resolution failed across all clients — try KoiVerse extractor as fallback if token configured
        if (mode == StreamBackendMode.AUTO_FALLBACK) {
            val token = tokenRepository.getToken()
            if (!token.isNullOrBlank()) {
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
        }

        throw lastException ?: IllegalStateException("Failed to resolve stream for $videoId")
    }
}
