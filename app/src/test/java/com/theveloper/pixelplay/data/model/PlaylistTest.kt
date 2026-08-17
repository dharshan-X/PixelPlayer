package com.theveloper.pixelplay.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTest {
    @Test
    fun testIsOnlinePlaylistId() {
        assertTrue(isOnlinePlaylistId("yt_pl_RDCLAK5uy_k"))
        assertTrue(isOnlinePlaylistId("yt_ab_MPREb_12345"))
        assertTrue(isOnlinePlaylistId("PL1234567890"))
        assertTrue(isOnlinePlaylistId("VLPL1234567890"))
        assertTrue(isOnlinePlaylistId("RDCLAK5uy"))
        assertFalse(isOnlinePlaylistId("local_playlist_123"))
        assertFalse(isOnlinePlaylistId("folder_playlist_/storage/emulated/0/Music"))
    }

    @Test
    fun testExtractCleanOnlineId() {
        assertEquals("RDCLAK5uy_k", extractCleanOnlineId("yt_pl_RDCLAK5uy_k"))
        assertEquals("MPREb_12345", extractCleanOnlineId("yt_ab_MPREb_12345"))
        assertEquals("PL12345", extractCleanOnlineId("PL12345"))
    }
}
