package com.theveloper.pixelplay.data.archivetune

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ArchiveTuneHeaderInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(ArchiveTuneHeaderInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testHeaderInjectionForYouTubeStreamRequest() {
        server.enqueue(MockResponse().setBody("ok"))
        val requestUrl = server.url("/videoplayback?c=WEB_REMIX&cver=1.20260101.01.00").toString()
        val request = Request.Builder().url(requestUrl).build()

        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertNotNull(recorded.getHeader("User-Agent"))
        assertEquals("https://music.youtube.com", recorded.getHeader("Origin"))
        assertEquals("https://music.youtube.com/", recorded.getHeader("Referer"))
    }
}
