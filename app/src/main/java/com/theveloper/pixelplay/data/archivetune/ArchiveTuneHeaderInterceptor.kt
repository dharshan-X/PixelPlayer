package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.utils.StreamClientUtils
import okhttp3.Interceptor
import okhttp3.Response

class ArchiveTuneHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host
        val isYouTubeMediaHost =
            host.endsWith("googlevideo.com") ||
                host.endsWith("googleusercontent.com") ||
                host.endsWith("youtube.com") ||
                host.endsWith("youtube-nocookie.com") ||
                host.endsWith("ytimg.com")

        if (!isYouTubeMediaHost && !originalRequest.url.encodedPath.contains("videoplayback") && originalRequest.url.queryParameter("c") == null) {
            return chain.proceed(originalRequest)
        }

        val requestProfile = StreamClientUtils.resolveRequestProfile(originalRequest.url)
        val builder = originalRequest.newBuilder()
        StreamClientUtils.applyRequestProfile(builder, requestProfile)

        return chain.proceed(builder.build())
    }
}
