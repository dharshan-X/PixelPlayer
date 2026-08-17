package com.theveloper.pixelplay.presentation.viewmodel

import moe.rukamori.archivetune.innertube.models.Artist
import moe.rukamori.archivetune.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlinePlaylistMapperTest {
    @Test
    fun testSongItemMapping() {
        val songItem = SongItem(
            id = "testVideo123",
            title = "Test Song",
            artists = listOf(Artist("Test Artist", "artist123")),
            duration = 180,
            thumbnail = "https://example.com/thumb.jpg"
        )

        val durationMs = (songItem.duration ?: 0) * 1000L
        assertEquals("yt_testVideo123", "yt_${songItem.id}")
        assertEquals(180000L, durationMs)
        assertEquals("yt://testVideo123", "yt://${songItem.id}")
    }
}
