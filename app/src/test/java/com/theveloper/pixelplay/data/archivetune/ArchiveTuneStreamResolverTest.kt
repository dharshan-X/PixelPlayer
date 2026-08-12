package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.utils.StreamClientUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ArchiveTuneStreamResolverTest {
    @Test
    fun testHeaderProfileResolutionForWebRemix() {
        val profile = StreamClientUtils.resolveRequestProfile(clientParam = "WEB_REMIX")
        assertEquals("WEB_REMIX", profile.resolvedClientFamily)
        assertNotNull(profile.userAgent)
        assertNotNull(profile.origin)
        assertNotNull(profile.referer)
    }

    @Test
    fun testHeaderProfileResolutionForAndroidVr() {
        val profile = StreamClientUtils.resolveRequestProfile(clientParam = "ANDROID_VR")
        assertEquals("ANDROID_VR", profile.resolvedClientFamily)
        assertNotNull(profile.userAgent)
    }
}
