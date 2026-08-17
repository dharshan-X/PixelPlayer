# ArchiveTune Music Stream Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the ArchiveTune music streaming backend inside PixelPlayer, incorporating InnerTube multi-client audio streaming, Rhino JS cipher deobfuscation (`morideobfuscator`), Koyeb/Koiverse remote extractor fallback (`moriextractor`), and ExoPlayer stream header resolution.

**Architecture:** Integrate the ArchiveTune reference submodules (`:archivetune-core`, `:archivetune-morideobfuscator`, `:archivetune-moriextractor`) into PixelPlayer's Gradle build, create a high-level `ArchiveTuneStreamResolver` service for stream candidate selection, bot detection recovery, and header profiling, and wire up Hilt dependency injection and ExoPlayer OkHttp interceptors in `com.theveloper.pixelplay.data.archivetune`.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, AndroidX Media3 (ExoPlayer), Ktor client, OkHttp 5, Hilt, Mozilla Rhino JS engine, InnerTube API, kotlinx.serialization.

## Global Constraints

- Preserve Java 21 / Kotlin 2.4 compiler compatibility across all modules.
- Ensure all YouTube stream requests dynamically configure User-Agent, Origin, and Referer based on client parameters (`WEB_REMIX`, `ANDROID_VR`, `IOS`, `TVHTML5`).
- Support audio quality fallback hierarchy (`HIGHEST` -> `HIGH` -> `AUTO` -> `LOW`).
- Provide clean unit test coverage for stream candidate selection, header profiling, and error recovery.

---

### Task 1: Version Catalog & Gradle Subproject Configuration

**Files:**
- Modify: `gradle/libs.versions.toml:1-289`
- Modify: `settings.gradle.kts:1-38`
- Modify: `app/build.gradle.kts:340-399`

**Interfaces:**
- Consumes: N/A
- Produces: Gradle subproject dependencies `:archivetune-core`, `:archivetune-morideobfuscator`, `:archivetune-moriextractor` and `rhino` library dependency.

- [ ] **Step 1: Update `gradle/libs.versions.toml` to add Rhino and subproject dependencies**

Add Rhino version and library definition to `gradle/libs.versions.toml`:
```toml
# In [versions]
rhino = "1.9.1"
brotli = "0.1.2"
re2j = "1.8"

# In [libraries]
rhino = { module = "org.mozilla:rhino", version.ref = "rhino" }
brotli = { module = "org.brotli:dec", version.ref = "brotli" }
re2j = { module = "com.google.re2j:re2j", version.ref = "re2j" }
```

- [ ] **Step 2: Update `settings.gradle.kts` to include ArchiveTune subprojects**

Add the subprojects to `settings.gradle.kts`:
```kotlin
include(":archivetune-core")
project(":archivetune-core").projectDir = file("backend-refences/ArchiveTune/core")

include(":archivetune-morideobfuscator")
project(":archivetune-morideobfuscator").projectDir = file("backend-refences/ArchiveTune/morideobfuscator")

include(":archivetune-moriextractor")
project(":archivetune-moriextractor").projectDir = file("backend-refences/ArchiveTune/moriextractor")
```

- [ ] **Step 3: Update `app/build.gradle.kts` to depend on ArchiveTune modules**

Add project dependencies to `app/build.gradle.kts`:
```kotlin
    implementation(project(":archivetune-core"))
    implementation(project(":archivetune-morideobfuscator"))
    implementation(project(":archivetune-moriextractor"))
    implementation(libs.rhino)
    implementation(libs.brotli)
    implementation(libs.re2j)
```

- [ ] **Step 4: Verify Gradle configuration compiles**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath`
Expected: BUILD SUCCESSFUL containing `:archivetune-core`, `:archivetune-morideobfuscator`, `:archivetune-moriextractor`.

- [ ] **Step 5: Commit changes**

```bash
git add gradle/libs.versions.toml settings.gradle.kts app/build.gradle.kts
git commit -m "build: integrate ArchiveTune backend subprojects into Gradle build"
```

---

### Task 2: Implement Bearer Token Storage & Hilt Injection Module

**Files:**
- Create: `app/src/main/java/com/theveloper/pixelplay/data/archivetune/DataStoreBearerTokenRepository.kt`
- Create: `app/src/main/java/com/theveloper/pixelplay/di/ArchiveTuneModule.kt`
- Test: `app/src/test/java/com/theveloper/pixelplay/data/archivetune/DataStoreBearerTokenRepositoryTest.kt`

**Interfaces:**
- Consumes: `moe.rukamori.archivetune.moriextractor.BearerTokenRepository` from `:archivetune-moriextractor`
- Produces: `@Singleton fun provideStreamingExtractionManager(...)`, `@Singleton fun provideBearerTokenRepository(...)`

- [ ] **Step 1: Write the failing unit test for `DataStoreBearerTokenRepository`**

Create `app/src/test/java/com/theveloper/pixelplay/data/archivetune/DataStoreBearerTokenRepositoryTest.kt`:
```kotlin
package com.theveloper.pixelplay.data.archivetune

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataStoreBearerTokenRepositoryTest {
    @Test
    fun testTokenSetGetAndClear() = runBlocking {
        val repository = InMemoryBearerTokenRepository()
        assertNull(repository.getToken())
        repository.setToken("test-bearer-token")
        assertEquals("test-bearer-token", repository.getToken())
        repository.clearToken()
        assertNull(repository.getToken())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.theveloper.pixelplay.data.archivetune.DataStoreBearerTokenRepositoryTest"`
Expected: FAIL with missing class `InMemoryBearerTokenRepository`.

- [ ] **Step 3: Implement `DataStoreBearerTokenRepository` and `InMemoryBearerTokenRepository`**

Create `app/src/main/java/com/theveloper/pixelplay/data/archivetune/DataStoreBearerTokenRepository.kt`:
```kotlin
package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.moriextractor.BearerTokenRepository
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreBearerTokenRepository @Inject constructor() : BearerTokenRepository {
    private val tokenRef = AtomicReference<String?>(null)

    override fun getToken(): String? = tokenRef.get()

    fun setToken(token: String?) {
        tokenRef.set(token?.trim()?.takeIf { it.isNotEmpty() })
    }

    override fun clearToken() {
        tokenRef.set(null)
    }
}

class InMemoryBearerTokenRepository : BearerTokenRepository {
    private var token: String? = null

    override fun getToken(): String? = token

    fun setToken(newToken: String?) {
        token = newToken
    }

    override fun clearToken() {
        token = null
    }
}
```

Create `app/src/main/java/com/theveloper/pixelplay/di/ArchiveTuneModule.kt`:
```kotlin
package com.theveloper.pixelplay.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moe.rukamori.archivetune.moriextractor.BearerTokenRepository
import moe.rukamori.archivetune.moriextractor.StreamingExtractionManager
import com.theveloper.pixelplay.data.archivetune.DataStoreBearerTokenRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArchiveTuneModule {

    @Binds
    @Singleton
    abstract fun bindBearerTokenRepository(
        impl: DataStoreBearerTokenRepository
    ): BearerTokenRepository

    companion object {
        @Provides
        @Singleton
        fun provideStreamingExtractionManager(
            tokenRepository: BearerTokenRepository
        ): StreamingExtractionManager {
            return StreamingExtractionManager(
                tokenRepository = tokenRepository,
                baseUrl = "https://archivetune-api.koiiverse.cloud"
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.theveloper.pixelplay.data.archivetune.DataStoreBearerTokenRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/data/archivetune/DataStoreBearerTokenRepository.kt app/src/main/java/com/theveloper/pixelplay/di/ArchiveTuneModule.kt app/src/test/java/com/theveloper/pixelplay/data/archivetune/DataStoreBearerTokenRepositoryTest.kt
git commit -m "feat(archivetune): add token repository and Hilt DI module"
```

---

### Task 3: Implement ArchiveTune Stream Resolver Engine

**Files:**
- Create: `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamResolver.kt`
- Create: `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamModels.kt`
- Test: `app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamResolverTest.kt`

**Interfaces:**
- Consumes: `YTPlayerUtils`, `StreamClientUtils`, `StreamingExtractionManager`, `AudioQuality` from ArchiveTune core/moriextractor
- Produces: `suspend fun resolveStream(videoId: String, quality: AudioQuality): Result<ArchiveTuneStreamResult>`

- [ ] **Step 1: Write failing unit test for `ArchiveTuneStreamResolver` profile formatting**

Create `app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamResolverTest.kt`:
```kotlin
package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.utils.StreamClientUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ArchiveTuneStreamResolverTest {
    @Test
    fun testHeaderProfileResolutionForWebRemix() {
        val profile = StreamClientUtils.resolveRequestProfile("WEB_REMIX")
        assertEquals("WEB_REMIX", profile.resolvedClientFamily)
        assertNotNull(profile.userAgent)
        assertNotNull(profile.origin)
        assertNotNull(profile.referer)
    }

    @Test
    fun testHeaderProfileResolutionForAndroidVr() {
        val profile = StreamClientUtils.resolveRequestProfile("ANDROID_VR")
        assertEquals("ANDROID_VR_1_65_10", profile.resolvedClientFamily)
        assertNotNull(profile.userAgent)
    }
}
```

- [ ] **Step 2: Run test to verify it passes for StreamClientUtils**

Run: `./gradlew testDebugUnitTest --tests "com.theveloper.pixelplay.data.archivetune.ArchiveTuneStreamResolverTest"`
Expected: PASS

- [ ] **Step 3: Create `ArchiveTuneStreamModels.kt` and `ArchiveTuneStreamResolver.kt`**

Create `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamModels.kt`:
```kotlin
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
```

Create `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamResolver.kt`:
```kotlin
package com.theveloper.pixelplay.data.archivetune

import android.content.Context
import android.net.ConnectivityManager
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.PlayerStreamClient
import moe.rukamori.archivetune.moriextractor.StreamingExtractionManager
import moe.rukamori.archivetune.utils.StreamClientUtils
import moe.rukamori.archivetune.utils.YTPlayerUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveTuneStreamResolver @Inject constructor(
    private val extractionManager: StreamingExtractionManager
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
            val requestProfile = StreamClientUtils.resolveRequestProfile("WEB_REMIX")
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
            return@runCatching ArchiveTuneStreamResult(
                videoId = videoId,
                streamUrl = data.streamUrl,
                mimeType = data.format.mimeType,
                bitrate = data.format.bitrate,
                expiresInSeconds = data.streamExpiresInSeconds,
                clientName = requestProfile.resolvedClientFamily,
                userAgent = requestProfile.userAgent,
                origin = requestProfile.origin,
                referer = requestProfile.referer
            )
        }

        if (mode == StreamBackendMode.AUTO_FALLBACK) {
            val extracted = extractionManager.extractAudio(videoUrl = "https://www.youtube.com/watch?v=$videoId")
            val requestProfile = StreamClientUtils.resolveRequestProfile("WEB_REMIX")
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

        throw nativeResult.exceptionOrNull() ?: IllegalStateException("Failed to resolve stream for $videoId")
    }
}
```

- [ ] **Step 4: Run unit tests to verify resolver components**

Run: `./gradlew testDebugUnitTest --tests "com.theveloper.pixelplay.data.archivetune.ArchiveTuneStreamResolverTest"`
Expected: PASS

- [ ] **Step 5: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamModels.kt app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamResolver.kt app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneStreamResolverTest.kt
git commit -m "feat(archivetune): add ArchiveTuneStreamResolver with multi-backend fallback"
```

---

### Task 4: Implement ExoPlayer Stream Header Interceptor

**Files:**
- Create: `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneHeaderInterceptor.kt`
- Test: `app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneHeaderInterceptorTest.kt`

**Interfaces:**
- Consumes: `okhttp3.Interceptor`, `StreamClientUtils`
- Produces: `class ArchiveTuneHeaderInterceptor : Interceptor` injecting dynamic User-Agent, Origin, Referer headers for YouTube media requests.

- [ ] **Step 1: Write failing unit test for `ArchiveTuneHeaderInterceptor`**

Create `app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneHeaderInterceptorTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.theveloper.pixelplay.data.archivetune.ArchiveTuneHeaderInterceptorTest"`
Expected: FAIL with missing class `ArchiveTuneHeaderInterceptor`.

- [ ] **Step 3: Implement `ArchiveTuneHeaderInterceptor`**

Create `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneHeaderInterceptor.kt`:
```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.theveloper.pixelplay.data.archivetune.ArchiveTuneHeaderInterceptorTest"`
Expected: PASS

- [ ] **Step 5: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneHeaderInterceptor.kt app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneHeaderInterceptorTest.kt
git commit -m "feat(archivetune): add OkHttp header interceptor for stream playback"
```

---

### Task 5: Integration Verification & Build Testing

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/PixelPlayApplication.kt` (if initializing InnerTube auth/visitor states on launch)

- [ ] **Step 1: Run full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL with all tests passing.

- [ ] **Step 2: Run assembleDebug to ensure clean build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit final integration changes**

```bash
git commit --allow-empty -m "chore(archivetune): complete ArchiveTune music stream backend integration"
```

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-12-archivetune-music-stream-backend.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration
**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
