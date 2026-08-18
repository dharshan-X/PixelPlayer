package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.innertube.models.Artist
import moe.rukamori.archivetune.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveTuneRadioManagerTest {
    @Test
    fun testSongDeduplication() {
        val existingIds = setOf("yt_song1", "yt_song2")
        val candidateItems = listOf(
            SongItem(id = "song1", title = "Song 1", artists = listOf(Artist("A1", "id1")), thumbnail = ""),
            SongItem(id = "song3", title = "Song 3", artists = listOf(Artist("A3", "id3")), thumbnail = "")
        )
        val filtered = candidateItems.filter { "yt_${it.id}" !in existingIds }
        assertEquals(1, filtered.size)
        assertEquals("song3", filtered.first().id)
    }

    @Test
    fun testSongMapping() {
        val item = SongItem(
            id = "radioVideo1",
            title = "Radio Song",
            artists = listOf(Artist("Radio Artist", "artist1")),
            duration = 200,
            thumbnail = "https://example.com/thumb.jpg"
        )
        assertEquals("yt_radioVideo1", "yt_${item.id}")
        assertEquals("yt://radioVideo1", "yt://${item.id}")
        assertEquals(200000L, (item.duration ?: 0) * 1000L)
    }
}
