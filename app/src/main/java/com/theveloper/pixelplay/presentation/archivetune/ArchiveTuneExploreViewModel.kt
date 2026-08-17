package com.theveloper.pixelplay.presentation.archivetune

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theveloper.pixelplay.data.archivetune.ArchiveTuneStreamResolver
import com.theveloper.pixelplay.data.archivetune.StreamBackendMode
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.innertube.pages.HomePage
import android.net.Uri
import com.theveloper.pixelplay.data.service.player.DualPlayerEngine
import com.theveloper.pixelplay.data.repository.MusicRepository
import timber.log.Timber
import javax.inject.Inject

enum class ArchiveTuneSearchCategory(val label: String, val filter: YouTube.SearchFilter?) {
    ALL("All", null),
    SONGS("Songs", YouTube.SearchFilter.FILTER_SONG),
    ALBUMS("Albums", YouTube.SearchFilter.FILTER_ALBUM),
    ARTISTS("Artists", YouTube.SearchFilter.FILTER_ARTIST),
    PLAYLISTS("Playlists", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
}

data class ArchiveTuneExploreUiState(
    val isLoadingHome: Boolean = false,
    val homeSections: List<HomePage.Section> = emptyList(),
    val searchQuery: String = "",
    val activeCategory: ArchiveTuneSearchCategory = ArchiveTuneSearchCategory.ALL,
    val isSearching: Boolean = false,
    val searchResults: List<YTItem> = emptyList(),
    val isResolvingSongId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ArchiveTuneExploreViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val streamResolver: ArchiveTuneStreamResolver,
    private val musicRepository: MusicRepository,
    private val dualPlayerEngine: DualPlayerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveTuneExploreUiState())
    val uiState: StateFlow<ArchiveTuneExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHome = true, errorMessage = null) }
            try {
                val homeResult = withContext(Dispatchers.IO) {
                    YouTube.home()
                }
                homeResult.onSuccess { homePage ->
                    _uiState.update {
                        it.copy(
                            isLoadingHome = false,
                            homeSections = homePage.sections.filter { s -> s.items.isNotEmpty() },
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    if (err is CancellationException || err.message?.contains("cancel", ignoreCase = true) == true) {
                        return@launch
                    }
                    Timber.e(err, "Failed to load ArchiveTune home feed")
                    _uiState.update {
                        it.copy(
                            isLoadingHome = false,
                            errorMessage = err.localizedMessage ?: "Failed to load online feed"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || e.message?.contains("cancel", ignoreCase = true) == true) {
                    return@launch
                }
                Timber.e(e, "Exception loading ArchiveTune home feed")
                _uiState.update {
                    it.copy(
                        isLoadingHome = false,
                        errorMessage = e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        if (newQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), errorMessage = null) }
        }
    }

    fun onCategorySelected(category: ArchiveTuneSearchCategory) {
        _uiState.update { it.copy(activeCategory = category) }
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery, category, debounceMillis = 0L)
        }
    }

    fun performSearch(
        query: String = _uiState.value.searchQuery,
        category: ArchiveTuneSearchCategory = _uiState.value.activeCategory,
        debounceMillis: Long = 350L
    ) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), errorMessage = null) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMillis > 0) {
                delay(debounceMillis)
            }
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            try {
                val filter = category.filter
                val searchResult = withContext(Dispatchers.IO) {
                    if (filter != null) {
                        YouTube.search(trimmed, filter)
                    } else {
                        YouTube.search(trimmed, YouTube.SearchFilter.FILTER_SONG)
                    }
                }
                searchResult.onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = result.items,
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    if (err is CancellationException || err.message?.contains("cancel", ignoreCase = true) == true) {
                        return@launch
                    }
                    Timber.e(err, "Search failed for query: $trimmed")
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = err.localizedMessage ?: "Search failed"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || e.message?.contains("cancel", ignoreCase = true) == true) {
                    return@launch
                }
                Timber.e(e, "Search exception for query: $trimmed")
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = e.localizedMessage ?: "Search network error"
                    )
                }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList()
            )
        }
    }

    fun playSongItem(
        songItem: SongItem,
        contextSongs: List<SongItem> = emptyList(),
        playerViewModel: PlayerViewModel
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingSongId = songItem.id) }
            try {
                val streamResult = withContext(Dispatchers.IO) {
                    streamResolver.resolveStream(
                        context = appContext,
                        videoId = songItem.id,
                        quality = AudioQuality.HIGH,
                        mode = StreamBackendMode.AUTO_FALLBACK
                    )
                }

                streamResult.onSuccess { resolved ->
                    val resolvedUri = Uri.parse(resolved.streamUrl)
                    dualPlayerEngine.cacheResolvedUri("yt://${songItem.id}", resolvedUri)
                    dualPlayerEngine.cacheResolvedUri("yt_${songItem.id}", resolvedUri)

                    val resolvedSong = mapSongItemToSong(songItem, resolved.mimeType, resolved.bitrate)
                    
                    val queueSongs = if (contextSongs.isNotEmpty()) {
                        contextSongs.map { item ->
                            if (item.id == songItem.id) {
                                resolvedSong
                            } else {
                                mapSongItemToSong(item, "audio/webm", 160000)
                            }
                        }
                    } else {
                        listOf(resolvedSong)
                    }

                    runCatching {
                        musicRepository.saveOnlineSongs(queueSongs)
                    }

                    playerViewModel.showAndPlaySong(
                        song = resolvedSong,
                        contextSongs = queueSongs,
                        queueName = "YouTube Music Online"
                    )
                }.onFailure { err ->
                    Timber.e(err, "Failed to resolve stream for ${songItem.id}")
                    _uiState.update {
                        it.copy(errorMessage = "Unable to play stream: ${err.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error resolving song stream")
                _uiState.update {
                    it.copy(errorMessage = "Playback error: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isResolvingSongId = null) }
            }
        }
    }

    private fun mapSongItemToSong(
        songItem: SongItem,
        mimeType: String,
        bitrate: Int?
    ): Song {
        val durationMs = (songItem.duration ?: 0) * 1000L
        val artistName = songItem.artists.firstOrNull()?.name ?: "Unknown Artist"
        val albumName = songItem.album?.name ?: "YouTube Music"

        return Song(
            id = "yt_${songItem.id}",
            title = songItem.title,
            artist = artistName,
            artistId = -1L,
            album = albumName,
            albumId = -1L,
            albumArtist = artistName,
            path = "yt://${songItem.id}",
            contentUriString = "yt://${songItem.id}",
            albumArtUriString = songItem.thumbnail,
            duration = durationMs,
            mimeType = mimeType,
            bitrate = bitrate,
            sampleRate = 44100
        )
    }
}
