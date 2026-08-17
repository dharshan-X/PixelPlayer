# Online Infinite Queue, Radio Mixes & Comprehensive Stats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement continuous infinite auto-queue / radio playback for online songs using YouTube Music InnerTube Next API, infinite scroll pagination in online search, seamless online song inclusion in Daily Mix & Your Mix generation, and unified listening stats tracking for both offline and online music.

**Architecture:** 
1. **Infinite Radio / Auto-Queue:** Implement `ArchiveTuneRadioManager` to query `YouTube.next(WatchEndpoint(videoId))` whenever an online song plays or the active queue approaches the end, dynamically resolving and appending recommended tracks to ExoPlayer without interrupting playback.
2. **Infinite Search Pagination:** Enhance `ArchiveTuneExploreViewModel` and `OnlineSearchResults` with continuation tokens (`YouTube.searchContinuation`) to automatically fetch additional pages on scroll.
3. **Unified Daily Mix & Online Stats:** Update `MusicRepository.getAllSongsOnce()` to query all songs across Room DB (`musicDao.getAllSongsList()`), enabling `DailyMixManager`, `AiPlaylistGenerator`, and `PlaybackStatsRepository` to seamlessly rank, blend, and display listening statistics for both offline and online tracks.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer), Dagger Hilt, Room Database, InnerTube API (ArchiveTune Core), StateFlow / Coroutines.

## Global Constraints

- Never run local Gradle build or test commands on the host machine (`./gradlew test`, `./gradlew assembleDebug`) as the user's system cannot handle it. Verification is done through static analysis and remote CI/CD pipelines.
- All online songs must strictly use the persistent `yt://${videoId}` path and `id = "yt_${videoId}"` format to guarantee persistent URI resolution, Room DB storage, and duplicate prevention.
- Seamlessly preserve existing offline playback, local playlist operations, and offline stats.

---

### Task 1: Create `ArchiveTuneRadioManager` for Infinite Auto-Queue

**Files:**
- Create: `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneRadioManager.kt`
- Create: `app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneRadioManagerTest.kt`

**Interfaces:**
- Produces:
  - `ArchiveTuneRadioManager.fetchRadioQueue(videoId: String, existingSongIds: Set<String>): List<Song>`
  - `ArchiveTuneRadioManager.fetchContinuation(continuationToken: String, existingSongIds: Set<String>): Pair<List<Song>, String?>`

- [ ] **Step 1: Write unit test for radio queue filtering and mapping**

Create `app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneRadioManagerTest.kt`:

```kotlin
package com.theveloper.pixelplay.data.archivetune

import moe.rukamori.archivetune.innertube.models.Artist
import moe.rukamori.archivetune.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveTuneRadioManagerTest {
    @Test
    fun testSongDeduplication() {
        val existingIds = setOf("yt_song1", "yt_song2")
        val candidateItems = listOf(
            SongItem(id = "song1", title = "Song 1", artists = listOf(Artist("A1", "id1"))),
            SongItem(id = "song3", title = "Song 3", artists = listOf(Artist("A3", "id3")))
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
```

- [ ] **Step 2: Implement `ArchiveTuneRadioManager`**

Create `app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneRadioManager.kt`:

```kotlin
package com.theveloper.pixelplay.data.archivetune

import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.SongSource
import com.theveloper.pixelplay.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveTuneRadioManager @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend fun fetchRadioQueue(
        videoId: String,
        existingSongIds: Set<String> = emptySet()
    ): Pair<List<Song>, String?> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanVideoId = videoId.removePrefix("yt_").removePrefix("yt://").trim()
            val nextResult = YouTube.next(WatchEndpoint(videoId = cleanVideoId)).getOrThrow()
            
            val songs = nextResult.items
                .filter { "yt_${it.id}" !in existingSongIds && it.id != cleanVideoId }
                .map { item ->
                    val durationMs = (item.duration ?: 0) * 1000L
                    val artistName = item.artists.firstOrNull()?.name ?: "Unknown Artist"
                    Song(
                        id = "yt_${item.id}",
                        title = item.title,
                        artist = artistName,
                        artistId = -1L,
                        album = nextResult.title ?: "Radio",
                        albumId = -1L,
                        albumArtist = artistName,
                        path = "yt://${item.id}",
                        contentUriString = "yt://${item.id}",
                        albumArtUriString = item.thumbnail,
                        duration = durationMs,
                        mimeType = "audio/webm",
                        bitrate = 160000,
                        sampleRate = 44100,
                        source = SongSource.YOUTUBE_MUSIC
                    )
                }

            if (songs.isNotEmpty()) {
                runCatching {
                    musicRepository.saveOnlineSongs(songs)
                }
            }

            Pair(songs, nextResult.continuation)
        }.getOrElse { throwable ->
            Timber.e(throwable, "Failed to fetch radio queue for videoId=%s", videoId)
            Pair(emptyList(), null)
        }
    }
}
```

- [ ] **Step 3: Commit Task 1**

```bash
git add app/src/main/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneRadioManager.kt app/src/test/java/com/theveloper/pixelplay/data/archivetune/ArchiveTuneRadioManagerTest.kt
git commit -m "feat(radio): implement ArchiveTuneRadioManager for YouTube Music auto-queue and radio recommendations"
```

---

### Task 2: Integrate Infinite Radio / Autoplay in `PlayerViewModel` & `MusicService`

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlayerViewModel.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlaybackDispatchStateHolder.kt`

**Interfaces:**
- Consumes:
  - `ArchiveTuneRadioManager.fetchRadioQueue(videoId, existingSongIds)`
  - `PlayerViewModel.onSongChanged` / `PlaybackDispatchStateHolder.handleTrackEnded`
- Produces:
  - Continuous infinite auto-enqueue when playing online music or queue runs low.

- [ ] **Step 1: Wire auto-queue trigger into `PlayerViewModel.kt` / `PlaybackDispatchStateHolder.kt`**

Inject `ArchiveTuneRadioManager` into `PlayerViewModel` and `PlaybackDispatchStateHolder`.
When the currently playing song is an online song (`song.contentUriString.startsWith("yt://")` or `song.id.startsWith("yt_")`), or when `remainingQueueCount <= 2`:
- Asynchronously query `archiveTuneRadioManager.fetchRadioQueue(currentSong.id, currentQueueSongIds)`
- Append the resolved songs into the active player queue using `playerViewModel.addSongsToQueue(newSongs)`
- Ensure it only triggers once per song transition to prevent duplicate network calls.

- [ ] **Step 2: Commit Task 2**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlayerViewModel.kt app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlaybackDispatchStateHolder.kt
git commit -m "feat(player): auto-enqueue next recommended radio songs when playing online tracks"
```

---

### Task 3: Implement Infinite Scroll Pagination in Online Search

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/archivetune/ArchiveTuneExploreViewModel.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/search/components/OnlineSearchResults.kt`

**Interfaces:**
- Consumes:
  - `moe.rukamori.archivetune.innertube.YouTube.searchContinuation` or `searchSummary`
- Produces:
  - `ArchiveTuneExploreViewModel.loadMoreSearchResults()`
  - `OnlineSearchResults` scroll detection invoking `onLoadMore`

- [ ] **Step 1: Add pagination state and `loadMoreSearchResults()` in `ArchiveTuneExploreViewModel.kt`**

In `ArchiveTuneExploreViewModel.kt`:
- Track `searchContinuationToken: String?` in UI state.
- Store continuation token when `YouTube.search()` returns a `SearchPage`.
- Implement `loadMoreSearchResults()`:
  ```kotlin
  fun loadMoreSearchResults() {
      val token = _uiState.value.searchContinuationToken ?: return
      if (_uiState.value.isLoadingMoreSearch) return
      
      viewModelScope.launch {
          _uiState.update { it.copy(isLoadingMoreSearch = true) }
          val result = YouTube.searchContinuation(token)
          result.onSuccess { continuationPage ->
              _uiState.update { current ->
                  current.copy(
                      searchResults = current.searchResults + continuationPage.items,
                      searchContinuationToken = continuationPage.continuation,
                      isLoadingMoreSearch = false
                  )
              }
          }.onFailure {
              _uiState.update { it.copy(isLoadingMoreSearch = false) }
          }
      }
  }
  ```

- [ ] **Step 2: Detect end of list in `OnlineSearchResults.kt` and trigger `onLoadMore`**

In `OnlineSearchResults.kt`:
- Add `onLoadMore: () -> Unit` callback.
- Add `LaunchedEffect(listState)` observing `derivedStateOf { listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size >= listState.layoutInfo.totalItemsCount - 3 }` to invoke `onLoadMore()`.
- Display a bottom loading spinner when `isLoadingMore` is true.

- [ ] **Step 3: Commit Task 3**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/archivetune/ArchiveTuneExploreViewModel.kt app/src/main/java/com/theveloper/pixelplay/presentation/screens/search/components/OnlineSearchResults.kt
git commit -m "feat(search): add infinite scroll pagination for online music search results"
```

---

### Task 4: Enable Online Songs in `DailyMixManager`, `AiPlaylistGenerator`, and `StatsViewModel`

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/data/repository/MusicRepository.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/data/repository/MusicRepositoryImpl.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/StatsViewModel.kt`

**Interfaces:**
- Consumes:
  - `MusicDao.getAllSongsList(): List<SongEntity>`
- Produces:
  - `MusicRepository.getAllSongsOnce()` returning all songs (local files + saved online/stream tracks)
  - Full listening stats and Daily Mix generation across all played songs.

- [ ] **Step 1: Update `getAllSongsOnce()` in `MusicRepositoryImpl.kt`**

Update `MusicRepositoryImpl.kt`:
- Change `getAllSongsOnce()` to query `musicDao.getAllSongsList()` so all library songs (local + saved online tracks) are retrieved.
- This feeds both offline files and online songs into `DailyMixManager.computeRankedSongs()`, `AiPlaylistGenerator`, and `StatsViewModel.loadSongs()`.

- [ ] **Step 2: Commit Task 4**

```bash
git add app/src/main/java/com/theveloper/pixelplay/data/repository/MusicRepository.kt app/src/main/java/com/theveloper/pixelplay/data/repository/MusicRepositoryImpl.kt app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/StatsViewModel.kt
git commit -m "feat(stats): include online music in Daily Mix generation, AI playlists, and listening statistics"
```

---

### Task 5: End-to-End Verification & CI Remote Push

**Files:**
- Check: All modified files and git status.

- [ ] **Step 1: Verify clean static syntax and no broken imports**
- [ ] **Step 2: Push changes to GitLab and GitHub remotes**

```bash
git push gitlab master
git push origin master
```
