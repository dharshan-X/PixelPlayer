package com.theveloper.pixelplay.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

const val ONLINE_PLAYLIST_PREFIX = "yt_pl_"
const val ONLINE_ALBUM_PREFIX = "yt_ab_"

fun isOnlinePlaylistId(playlistId: String): Boolean {
    return playlistId.startsWith(ONLINE_PLAYLIST_PREFIX) ||
        playlistId.startsWith(ONLINE_ALBUM_PREFIX) ||
        playlistId.startsWith("PL") ||
        playlistId.startsWith("VL") ||
        playlistId.startsWith("RD") ||
        playlistId.startsWith("MPRE")
}

fun extractCleanOnlineId(playlistId: String): String {
    return playlistId
        .removePrefix(ONLINE_PLAYLIST_PREFIX)
        .removePrefix(ONLINE_ALBUM_PREFIX)
        .trim()
}

@Immutable
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false,
    val isQueueGenerated: Boolean = false,
    val coverImageUri: String? = null,
    val coverColorArgb: Int? = null,
    val coverIconName: String? = null,
    val coverShapeType: String? = null, // "Circle", "SmoothRect", etc. Storing as String to avoid Enum import issues if moved
    val coverShapeDetail1: Float? = null, // e.g., CornerRadius / StarCurve
    val coverShapeDetail2: Float? = null, // e.g., Smoothness / StarRotation
    val coverShapeDetail3: Float? = null, // e.g., StarScale
    val coverShapeDetail4: Float? = null, // e.g., Star Sides (Int)
    val source: String = "LOCAL", // Source: "LOCAL", "NETEASE", "TELEGRAM", "AI", etc.
    val isOnline: Boolean = false
)

enum class PlaylistShapeType {
    Circle,
    SmoothRect,
    RotatedPill,
    Star
}
