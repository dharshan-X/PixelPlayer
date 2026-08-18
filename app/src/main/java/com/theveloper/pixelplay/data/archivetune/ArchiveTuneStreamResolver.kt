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

        // YTPlayerUtils.playerResponseForPlayback already handles multi-client fallback
        // internally (via buildStreamClientOrder), PoToken minting, bot-detection recovery,
        // and login-context repair. Calling it once with the preferred client is sufficient.
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
        }

        // Native resolution failed — try KoiVerse extractor as fallback
        val nativeException = nativeResult.exceptionOrNull()
        Timber.tag("ArchiveTuneStreamResolver").w(
            nativeException,
            "Native stream resolution failed for videoId=%s, trying extractor fallback",
            videoId
        )

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

        throw nativeException ?: IllegalStateException("Failed to resolve stream for $videoId")
    }
}
