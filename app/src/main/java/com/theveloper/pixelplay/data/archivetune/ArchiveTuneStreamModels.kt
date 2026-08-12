package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.constants.AudioQuality

data class ArchiveTuneStreamResult(
    val videoId: String,
    val streamUrl: String,
    val mimeType: String,
    val bitrate: Int,
    val expiresInSeconds: Int,
    val clientName: String,
    val userAgent: String,
    val origin: String?,
    val referer: String?
)

enum class StreamBackendMode {
    NATIVE_INNER_TUBE,
    KOIVERSE_EXTRACTOR,
    AUTO_FALLBACK
}
