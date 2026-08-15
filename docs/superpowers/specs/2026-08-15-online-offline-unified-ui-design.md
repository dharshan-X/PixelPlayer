# Design Spec: Unified Online Streaming & Offline Device UI Integration

## 1. Overview & Goals
Integrate ArchiveTune (YouTube Music) streaming capabilities directly into PixelPlayer's primary user interface (Home screen and Search screen), allowing users to explore online trending music, search online catalogs, and toggle seamlessly between **Online Mode** and **Offline (Device Only) Mode**.

---

## 2. Core Architecture & UI Components

### A. Home Screen Integration (`HomeScreen.kt`)
1. **Mode Switcher (Top Bar)**:
   - A toggle/chip in the Home top bar allowing users to switch between:
     - `Online` (`StorageFilter.ALL`): Shows online feeds (Quick Picks, Trending Songs, Recommendations, Charts) + local music.
     - `Offline` (`StorageFilter.OFFLINE`): Hides network-based feeds and focuses exclusively on local MediaStore tracks, collages, and local playlists.
2. **Online Quick Picks & Trending Section**:
   - Integrated directly below the top bar on Home.
   - Horizontal carousels displaying trending songs, albums, and featured playlists fetched via `YouTube.home()`.
   - Each item includes high-resolution artwork thumbnail, song title, artist subtitle, and a play overlay.
   - Active resolving state: Displays a non-blocking loading spinner on the card when resolving the stream URL.

### B. Dual Search Screen (`SearchScreen.kt`)
1. **Source Filter Tabs**:
   - `Online (YouTube Music)`: Queries YouTube Music catalog in real-time (`YouTube.search()`).
   - `Device (Offline Files)`: Searches local device MediaStore database (songs, albums, artists, playlists).
2. **Categorized Online Search Results**:
   - Result chips: `All`, `Songs`, `Albums`, `Artists`, `Playlists`.
   - Song result rows with artwork, duration, and one-tap stream resolution + playback.

### C. Stream Playback & MediaItem Pipeline
1. **Stream Resolution**:
   - When an online track is tapped, `ArchiveTuneStreamResolver` resolves the playback stream URL using native InnerTube multi-client audio formats (`WEB_REMIX`, `ANDROID_VR`, `IOS`, `TVHTML5`) or Koyeb/Koiverse remote extractor fallback.
2. **MediaItem Construction**:
   - Maps resolved stream URL into `Song` model with ID `yt_<videoId>`, title, artist, album, thumbnail artwork URI, and duration.
   - Passes `Song` to `PlayerViewModel.showAndPlaySong(...)`.
3. **OkHttp Header Interception**:
   - `ArchiveTuneHeaderInterceptor` injects dynamic `User-Agent`, `Origin`, and `Referer` headers so ExoPlayer streams without 403 errors.

---

## 3. Data Flow & State Management

```
                 +-----------------------------+
                 |       User Interface        |
                 | (HomeScreen / SearchScreen) |
                 +--------------+--------------+
                                |
             +------------------+------------------+
             |                                     |
     [Online Mode]                          [Device Mode]
             |                                     |
             v                                     v
  +----------------------+              +----------------------+
  | ArchiveTune / YT Core|              |  MediaStore / Room   |
  |  (YouTube.home/search)              |   (Local database)   |
  +----------+-----------+              +----------+-----------+
             |                                     |
             v                                     v
  +----------------------+                         |
  | StreamResolver Engine|                         |
  | (YTPlayer / Extractor|                         |
  +----------+-----------+                         |
             |                                     |
             +------------------+------------------+
                                |
                                v
                 +-----------------------------+
                 |       PlayerViewModel       |
                 |      showAndPlaySong()      |
                 +--------------+--------------+
                                |
                                v
                 +-----------------------------+
                 |     Media3 / ExoPlayer      |
                 | ArchiveTuneHeaderInterceptor|
                 +-----------------------------+
```

---

## 4. Error Handling & Edge Cases
- **No Internet Connection**:
  - Gracefully displays a clean offline message with a "Switch to Device Mode" or "Retry" action.
- **Stream URL Expiry / 403 Errors**:
  - Automatically attempts next client profile (`ANDROID_VR` -> `IOS` -> `TVHTML5` -> `KOIVERSE_EXTRACTOR`).
- **Frictionless Fallback**:
  - Switching to Offline Device mode immediately loads local MediaStore files without delay.

---

## 5. Verification & Testing Plan
1. **Unit Tests**:
   - Test mode switching between `StorageFilter.ALL` and `StorageFilter.OFFLINE`.
   - Test search result filtering between online YouTube results and local database items.
   - Test song mapping and stream URL playback dispatch.
2. **Build Verification**:
   - `./gradlew testDebugUnitTest` to ensure all unit tests pass.
   - `./gradlew assembleDebug` to ensure successful compilation.
