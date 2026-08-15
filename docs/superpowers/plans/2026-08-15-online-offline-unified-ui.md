# Unified Online Streaming & Offline Device UI Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate ArchiveTune (YouTube Music) streaming directly into PixelPlayer's primary Home screen and Search screen with an intuitive Online vs Offline (Device Only) mode switcher.

**Architecture:** 
- Add a Top Bar Storage Mode selector (`Online` vs `Device`) in `HomeScreen.kt` driven by `PlayerViewModel.storageFilter`.
- Embed an `HomeOnlineQuickPicksSection` in the Home feed displaying trending songs, albums, and charts from `YouTube.home()` with instant stream playback.
- Implement Dual Search in `SearchScreen.kt` providing tabs for "Online (YouTube Music)" and "Device (Offline Files)".

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, Hilt, Media3 / ExoPlayer, Coil, `:archivetune-core`.

---

## Tasks

### Task 1: Top Bar Storage Mode Switcher on Home

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeGradientTopBar.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `PlayerViewModel.storageFilter: StateFlow<StorageFilter>` and `PlayerViewModel.setStorageFilter(StorageFilter)`.
- Produces: Visual toggle on Home top bar allowing users to switch between `StorageFilter.ALL` (Online) and `StorageFilter.OFFLINE` (Device Only).

- [ ] **Step 1: Add string resources for storage modes**
  In `app/src/main/res/values/strings.xml`:
  ```xml
  <string name="home_mode_all">Online</string>
  <string name="home_mode_offline">Device Only</string>
  ```

- [ ] **Step 2: Add Storage Mode toggle chip to HomeGradientTopBar**
  Update `HomeGradientTopBar.kt` to accept `currentStorageFilter: StorageFilter` and `onToggleStorageFilter: (StorageFilter) -> Unit`, displaying an expressive pill chip next to greeting/actions.

- [ ] **Step 3: Connect Storage Mode in HomeScreen**
  Update `HomeScreen.kt` to collect `storageFilter` from `playerViewModel` and pass it to `HomeGradientTopBar`.

- [ ] **Step 4: Verify Compilation & Test**
  Run: `./gradlew testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeGradientTopBar.kt app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt app/src/main/res/values/strings.xml
  git commit -m "feat(ui): add top bar online/offline storage mode switcher on Home"
  ```

---

### Task 2: Online Quick Picks & Trending Section on Home

**Files:**
- Create: `app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeOnlineQuickPicksSection.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt`

**Interfaces:**
- Consumes: `moe.rukamori.archivetune.innertube.YouTube.home()`, `ArchiveTuneStreamResolver.resolveStream()`, and `PlayerViewModel.showAndPlaySong()`.
- Produces: Composable section on Home displaying trending songs and albums with play overlay and dynamic stream resolution.

- [ ] **Step 1: Create HomeOnlineQuickPicksSection Composable**
  Create `app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeOnlineQuickPicksSection.kt` featuring horizontal cards with artwork, play overlay, and resolving spinner.

- [ ] **Step 2: Embed HomeOnlineQuickPicksSection in HomeScreen**
  In `HomeScreen.kt`, conditionally render `HomeOnlineQuickPicksSection` when `storageFilter != StorageFilter.OFFLINE`.

- [ ] **Step 3: Verify Compilation & Playback Handling**
  Run: `./gradlew testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeOnlineQuickPicksSection.kt app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt
  git commit -m "feat(ui): integrate online quick picks and trending feed into Home screen"
  ```

---

### Task 3: Dual Search Integration (Online vs Device)

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SearchViewModel.kt`
- Test: `app/src/test/java/com/theveloper/pixelplay/presentation/viewmodel/SearchViewModelTest.kt`

**Interfaces:**
- Consumes: `YouTube.search(query, filter)` and `MusicRepository.searchSongs(query)`.
- Produces: Dual search tabs in `SearchScreen` ("Online" and "Device") with instant song stream playback.

- [ ] **Step 1: Add Online search state & functions in SearchViewModel**
  Add `onlineSearchResults: StateFlow<List<YTItem>>`, `isOnlineSearching: StateFlow<Boolean>`, `searchSource: StateFlow<SearchSource>`, and `performOnlineSearch(query)` in `SearchViewModel.kt`.

- [ ] **Step 2: Add Online / Device segmented tabs in SearchScreen**
  Update `SearchScreen.kt` with filter tabs to toggle between Online YouTube Music search and Local Device search, rendering online result rows with play button.

- [ ] **Step 3: Verify Compilation & Search Unit Tests**
  Run: `./gradlew testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SearchViewModel.kt app/src/test/java/com/theveloper/pixelplay/presentation/viewmodel/SearchViewModelTest.kt
  git commit -m "feat(search): integrate YouTube Music online search alongside local device search"
  ```

---

### Task 4: Integration Verification & Build Testing

**Files:**
- Test: All unit test suites

- [ ] **Step 1: Execute Full Unit Test Suite**
  Run: `./gradlew testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Execute Debug APK Build**
  Run: `./gradlew :app:assembleDebug -Ppixelplay.enableAbiSplits=true`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit and Push**
  ```bash
  git commit --allow-empty -m "chore(ui): complete unified online streaming and offline device UI integration"
  git push origin master
  ```
