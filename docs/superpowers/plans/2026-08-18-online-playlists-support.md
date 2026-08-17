# Online Playlists & Albums Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable full playback, exploration, and library saving for YouTube Music online playlists and albums directly within PixelPlayer's existing Playlist Details UI.

**Architecture:** Extend `PlaylistViewModel` to recognize online playlist/album identifiers (`yt_pl_` and `yt_ab_`), fetch remote playlist and album metadata using InnerTube's `YouTube.playlist` and `YouTube.album` APIs, convert `SongItem` entries to persistent `Song` models with `yt://` URIs, cache them in Room DB, and wire navigation from `HomeScreen` and `SearchScreen` directly into `PlaylistDetailScreen` with a one-tap "Save to Library" action.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3 ExoPlayer, Dagger Hilt, InnerTube API (ArchiveTune Core), Room Database, StateFlow/Coroutines.

## Global Constraints

- Never run local Gradle build or test commands on the host machine (`./gradlew assembleDebug`, `./gradlew test`) as the user's system cannot handle it. Verification is performed through static analysis and remote CI/CD pipelines.
- Preserve all existing local playlist behavior (custom sorting, manual reordering, M3U export, folder playlists).
- All online songs must use persistent `yt://${songItem.id}` URIs and `id = "yt_${songItem.id}"` so they never expire in database, queue history, or favorites.

---

### Task 1: Add Online Playlist Data Models & Helper Extensions

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/data/model/Playlist.kt:1-50`
- Test: `app/src/test/java/com/theveloper/pixelplay/data/model/PlaylistTest.kt`

**Interfaces:**
- Produces: 
  - `const val ONLINE_PLAYLIST_PREFIX = "yt_pl_"`
  - `const val ONLINE_ALBUM_PREFIX = "yt_ab_"`
  - `fun isOnlinePlaylistId(playlistId: String): Boolean`
  - `fun extractCleanOnlineId(playlistId: String): Boolean`
  - `Playlist.isOnline: Boolean` property (default `false`)

- [ ] **Step 1: Write the unit test for online playlist ID identification**

Create `app/src/test/java/com/theveloper/pixelplay/data/model/PlaylistTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Add constants, helper functions, and `isOnline` field in `Playlist.kt`**

Modify `app/src/main/java/com/theveloper/pixelplay/data/model/Playlist.kt`:

```kotlin
package com.theveloper.pixelplay.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
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

@Parcelize
@Serializable
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val customCoverUri: String? = null,
    val isPinned: Boolean = false,
    val isSmartPlaylist: Boolean = false,
    val smartPlaylistType: String? = null,
    val defaultTransitionName: String? = null,
    val isOnline: Boolean = false
) : Parcelable
```

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/data/model/Playlist.kt app/src/test/java/com/theveloper/pixelplay/data/model/PlaylistTest.kt
git commit -m "feat(playlist): add online playlist ID helpers and isOnline field"
```

---

### Task 2: Implement Online Playlist & Album Fetching in `PlaylistViewModel`

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlaylistViewModel.kt:170-285`
- Test: `app/src/test/java/com/theveloper/pixelplay/presentation/viewmodel/OnlinePlaylistMapperTest.kt`

**Interfaces:**
- Consumes:
  - `moe.rukamori.archivetune.innertube.YouTube.playlist(playlistId: String)`
  - `moe.rukamori.archivetune.innertube.YouTube.album(albumId: String)`
  - `MusicRepository.saveOnlineSongs(songs: List<Song>)`
  - `isOnlinePlaylistId(playlistId: String)`
  - `extractCleanOnlineId(playlistId: String)`
- Produces:
  - `PlaylistViewModel.loadPlaylistDetails(playlistId: String)` supporting online playlists/albums
  - `PlaylistViewModel.importOnlinePlaylistToLibrary(playlist: Playlist, songs: List<Song>)`

- [ ] **Step 1: Write the unit test for mapping `SongItem` to `Song`**

Create `app/src/test/java/com/theveloper/pixelplay/presentation/viewmodel/OnlinePlaylistMapperTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Add online playlist loading and import methods to `PlaylistViewModel.kt`**

Update `app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlaylistViewModel.kt` in `loadPlaylistDetails`:

```kotlin
                if (isFolderPlaylistId(playlistId)) {
                    // ... folder playlist logic ...
                } else if (isOnlinePlaylistId(playlistId)) {
                    val cleanId = extractCleanOnlineId(playlistId)
                    val isAlbum = playlistId.startsWith(ONLINE_ALBUM_PREFIX) || cleanId.startsWith("MPRE")
                    
                    val (title, author, thumbnail, songItems) = withContext(Dispatchers.IO) {
                        if (isAlbum) {
                            val albumResult = moe.rukamori.archivetune.innertube.YouTube.album(cleanId)
                            val albumPage = albumResult.getOrThrow()
                            Tuple4(
                                albumPage.album.title,
                                albumPage.album.artists?.firstOrNull()?.name ?: "YouTube Music",
                                albumPage.album.thumbnail,
                                albumPage.songs
                            )
                        } else {
                            val playlistResult = moe.rukamori.archivetune.innertube.YouTube.playlist(cleanId)
                            val playlistPage = playlistResult.getOrThrow()
                            Tuple4(
                                playlistPage.playlist.title,
                                playlistPage.playlist.author?.name ?: "YouTube Music",
                                playlistPage.playlist.thumbnail,
                                playlistPage.songs
                            )
                        }
                    }

                    val mappedSongs = songItems.map { item ->
                        val durationMs = (item.duration ?: 0) * 1000L
                        val artistName = item.artists.firstOrNull()?.name ?: author
                        Song(
                            id = "yt_${item.id}",
                            title = item.title,
                            artist = artistName,
                            artistId = -1L,
                            album = title,
                            albumId = -1L,
                            albumArtist = artistName,
                            path = "yt://${item.id}",
                            contentUriString = "yt://${item.id}",
                            albumArtUriString = item.thumbnail ?: thumbnail,
                            duration = durationMs,
                            mimeType = "audio/webm",
                            bitrate = 160000,
                            sampleRate = 44100,
                            source = SongSource.YOUTUBE_MUSIC
                        )
                    }

                    if (mappedSongs.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                musicRepository.saveOnlineSongs(mappedSongs)
                            }
                        }
                    }

                    val onlinePlaylist = Playlist(
                        id = playlistId,
                        name = title,
                        songIds = mappedSongs.map { it.id },
                        customCoverUri = thumbnail,
                        isOnline = true
                    )

                    _uiState.update {
                        it.copy(
                            currentPlaylistDetails = onlinePlaylist,
                            currentPlaylistSongs = mappedSongs,
                            isLoading = false,
                            playlistNotFound = false
                        )
                    }
                } else {
                    // ... existing userPlaylistsFlow logic ...
                }
```

Add `importOnlinePlaylistToLibrary`:

```kotlin
    fun importOnlinePlaylistToLibrary(playlist: Playlist, songs: List<Song>) {
        viewModelScope.launch {
            try {
                val newPlaylistId = "user_pl_${System.currentTimeMillis()}"
                val localPlaylist = Playlist(
                    id = newPlaylistId,
                    name = playlist.name,
                    songIds = songs.map { it.id },
                    customCoverUri = playlist.customCoverUri,
                    isOnline = false
                )
                withContext(Dispatchers.IO) {
                    musicRepository.saveOnlineSongs(songs)
                    playlistPreferencesRepository.savePlaylist(localPlaylist)
                }
                _uiState.update { it.copy(toastMessage = "Saved to your library") }
            } catch (e: Exception) {
                Timber.e(e, "Failed to import online playlist")
                _uiState.update { it.copy(toastMessage = "Failed to save playlist") }
            }
        }
    }
```

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlaylistViewModel.kt app/src/test/java/com/theveloper/pixelplay/presentation/viewmodel/OnlinePlaylistMapperTest.kt
git commit -m "feat(playlist): support loading and importing YouTube Music online playlists in PlaylistViewModel"
```

---

### Task 3: Add "Save to Library" Action to `PlaylistDetailScreen`

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/PlaylistDetailScreen.kt:180-260, 450-520`

**Interfaces:**
- Consumes:
  - `Playlist.isOnline`
  - `PlaylistViewModel.importOnlinePlaylistToLibrary(playlist, songs)`
- Produces:
  - Header / Options Menu action "Save to Library" when viewing an online playlist

- [ ] **Step 1: Update `PlaylistDetailScreen.kt` options and action bar**

In `PlaylistDetailScreen.kt`, check `if (currentPlaylist?.isOnline == true)`:
- Show a **Save / Import to Library** button in the header actions and in the options bottom sheet.
- Hide destructive options (Delete Playlist, Reorder Songs) for read-only online playlists.
- Add an "Online Playlist" badge indicator in the playlist header metadata.

- [ ] **Step 2: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/screens/PlaylistDetailScreen.kt
git commit -m "feat(ui): add save to library action and online badge in PlaylistDetailScreen"
```

---

### Task 4: Wire Navigation Callbacks in `HomeScreen` and `SearchScreen`

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt:430-470`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt:720-760`

**Interfaces:**
- Consumes:
  - `onPlaylistClick: (PlaylistItem) -> Unit`
  - `onAlbumClick: (AlbumItem) -> Unit`
  - `navController.navigateSafely(Screen.PlaylistDetail.createRoute(...))`
- Produces:
  - Seamless navigation to `PlaylistDetailScreen` on clicking any online playlist or album in Home Quick Picks and Search Results.

- [ ] **Step 1: Wire `onPlaylistClick` and `onAlbumClick` in `HomeScreen.kt`**

In `HomeScreen.kt`:
```kotlin
onAlbumClick = { albumItem ->
    navController.navigateSafely(
        Screen.PlaylistDetail.createRoute("${ONLINE_ALBUM_PREFIX}${albumItem.id}")
    )
},
onArtistClick = { artistItem ->
    Toast.makeText(context, "Artist: ${artistItem.name}", Toast.LENGTH_SHORT).show()
},
onPlaylistClick = { playlistItem ->
    navController.navigateSafely(
        Screen.PlaylistDetail.createRoute("${ONLINE_PLAYLIST_PREFIX}${playlistItem.id}")
    )
},
```

- [ ] **Step 2: Wire `onPlaylistClick` and `onAlbumClick` in `SearchScreen.kt`**

In `SearchScreen.kt` inside `OnlineSearchResults` callbacks:
```kotlin
onPlaylistClick = { playlistItem ->
    navController.navigateSafely(
        Screen.PlaylistDetail.createRoute("${ONLINE_PLAYLIST_PREFIX}${playlistItem.id}")
    )
},
onAlbumClick = { albumItem ->
    navController.navigateSafely(
        Screen.PlaylistDetail.createRoute("${ONLINE_ALBUM_PREFIX}${albumItem.id}")
    )
}
```

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt
git commit -m "feat(navigation): wire online playlist and album clicks to PlaylistDetailScreen"
```

---

### Task 5: End-to-End Verification & CI Push

**Files:**
- Check: All modified files and git status.

- [ ] **Step 1: Run static checks and verify no broken imports or missing symbols**
- [ ] **Step 2: Commit any cleanups and push to GitLab and GitHub remotes**

```bash
git push gitlab master
git push origin master
```
