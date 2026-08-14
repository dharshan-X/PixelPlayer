package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.utils.StreamClientUtils
import okhttp3.Interceptor
import okhttp3.Response

class ArchiveTuneHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        if (!url.encodedPath.contains("videoplayback") && url.queryParameter("c") == null) {
            return chain.proceed(originalRequest)
        }

        val requestProfile = StreamClientUtils.resolveRequestProfile(url)
        val builder = originalRequest.newBuilder()
        StreamClientUtils.applyRequestProfile(builder, requestProfile)

        return chain.proceed(builder.build())
    }
}
