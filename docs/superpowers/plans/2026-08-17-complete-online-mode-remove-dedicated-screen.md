# Complete Inline Online Mode & Remove Dedicated ArchiveTuneExplore Screen

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fully integrate all ArchiveTuneExplore browsing features (full home feed, category browsing, search, error handling) into the existing inline Home/Search screens, then remove the standalone `ArchiveTuneExploreScreen` and all navigation to it.

**Architecture:** The existing Home screen already shows a quick picks preview via `HomeOnlineQuickPicksSection`; this plan expands it to render all home feed sections (not just 3), adds "View All" section expansion, and makes album/artist/playlist items clickable with proper navigation. The existing Search screen already has dual Online/Device tabs; this plan adds missing functionality (error banners, retry). Once inline features are complete, the dedicated `ArchiveTuneExploreScreen` route, screen, and all `onNavigateToExplore` callbacks are removed.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Coil, `:archivetune-core`.

## Global Constraints

- Min SDK: as established in project
- All online sections gated on `storageFilter != StorageFilter.OFFLINE`
- Use `GoogleSansRounded` font family consistently
- Use `AbsoluteSmoothCornerShape(18.dp, 60)` for card shapes
- Stream playback via `ArchiveTuneExploreViewModel.playSongItem()`
- Compilation check: `./gradlew testDebugUnitTest` must pass at each task
- Build check: `./gradlew :app:assembleDebug` must pass at final task

---

### Task 1: Expand HomeOnlineQuickPicksSection to Show All Sections

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeOnlineQuickPicksSection.kt`

**Interfaces:**
- Consumes: `List<HomePage.Section>`, `isLoading: Boolean`, `resolvingSongId: String?`, `onSongClick`, `onAlbumClick: (AlbumItem) -> Unit`, `onArtistClick: (ArtistItem) -> Unit`, `onPlaylistClick: (PlaylistItem) -> Unit`
- Produces: Full home feed carousel rendering with clickable album/artist/playlist items; "Explore more" button removed.

- [ ] **Step 1: Update HomeOnlineQuickPicksSection signature**

Open `app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeOnlineQuickPicksSection.kt`.

Replace the current function signature and add new callback parameters. Remove `onNavigateToExplore` and add item-type-specific callbacks:

```kotlin
@Composable
fun HomeOnlineQuickPicksSection(
    sections: List<HomePage.Section>,
    isLoading: Boolean,
    resolvingSongId: String?,
    onSongClick: (SongItem, List<SongItem>) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onArtistClick: (ArtistItem) -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
)
```

Add imports:
```kotlin
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
```

- [ ] **Step 2: Remove "Explore more" button from header**

In the section header `Row`, remove the `TextButton` that called `onNavigateToExplore`. Replace the header content with just the YouTube Music icon and "Trending & Quick Picks" title, plus a refresh icon button:

```kotlin
// Section Header
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_youtube_music),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.archivetune_trending),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    IconButton(onClick = onRefresh) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Refresh",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

Add import:
```kotlin
import androidx.compose.material.icons.rounded.Refresh
```

- [ ] **Step 3: Render all sections instead of just 3**

Change `sections.take(3).forEach` to `sections.forEach` so all home feed sections are rendered.

- [ ] **Step 4: Wire click handlers for all item types**

In the `HomeOnlineCard` click handler inside the `LazyRow items` block, replace the current `else -> onNavigateToExplore()` with type-specific callbacks:

```kotlin
items(section.items, key = { it.id }) { item ->
    HomeOnlineCard(
        item = item,
        isResolving = resolvingSongId == item.id,
        onClick = {
            when (item) {
                is SongItem -> onSongClick(item, songsInSection)
                is AlbumItem -> onAlbumClick(item)
                is ArtistItem -> onArtistClick(item)
                is PlaylistItem -> onPlaylistClick(item)
            }
        }
    )
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew testDebugUnitTest`
Expected: Compile errors expected until HomeScreen call site is updated in Task 2 — that is OK.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeOnlineQuickPicksSection.kt
git commit -m "feat(ui): expand HomeOnlineQuickPicksSection to show all sections with typed click handlers"
```

---

### Task 2: Update HomeScreen Call Site for Expanded Quick Picks

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt`

**Interfaces:**
- Consumes: `HomeOnlineQuickPicksSection` updated signature from Task 1, `ArchiveTuneExploreViewModel.loadHomeFeed()`
- Produces: HomeScreen renders all online sections inline; album/artist/playlist clicks show a toast (placeholder until detail screens exist); "Explore more" no longer navigates to dedicated screen.

- [ ] **Step 1: Update HomeOnlineQuickPicksSection call in HomeScreen**

In `HomeScreen.kt` around line 440, replace the current `HomeOnlineQuickPicksSection(...)` call:

```kotlin
HomeOnlineQuickPicksSection(
    sections = archiveTuneUiState.homeSections,
    isLoading = archiveTuneUiState.isLoadingHome,
    resolvingSongId = archiveTuneUiState.isResolvingSongId,
    onSongClick = { songItem, contextList ->
        archiveTuneViewModel.playSongItem(
            songItem = songItem,
            contextSongs = contextList,
            playerViewModel = playerViewModel
        )
    },
    onAlbumClick = { albumItem ->
        // TODO: Navigate to online album detail when implemented
        android.widget.Toast.makeText(
            context,
            "Album: ${albumItem.title}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    },
    onArtistClick = { artistItem ->
        // TODO: Navigate to online artist detail when implemented
        android.widget.Toast.makeText(
            context,
            "Artist: ${artistItem.title}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    },
    onPlaylistClick = { playlistItem ->
        // TODO: Navigate to online playlist detail when implemented
        android.widget.Toast.makeText(
            context,
            "Playlist: ${playlistItem.title}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    },
    onRefresh = {
        archiveTuneViewModel.loadHomeFeed()
    }
)
```

- [ ] **Step 2: Remove onNavigateToExplore import if unused**

Check if `Screen.ArchiveTuneExplore` is still referenced in `HomeScreen.kt` for the `StreamingProviderSheet`. If so, leave it for now — Task 4 handles the StreamingProviderSheet cleanup. If not, remove the unused import.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt
git commit -m "feat(ui): wire expanded inline online quick picks in HomeScreen"
```

---

### Task 3: Add Error Banner to Search Online Mode

**Files:**
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/search/components/OnlineSearchResults.kt`

**Interfaces:**
- Consumes: `ArchiveTuneExploreUiState.errorMessage`
- Produces: Error banner displayed above online search results when `errorMessage != null`; retry button clears error and retries search.

- [ ] **Step 1: Add error banner to online search section in SearchScreen**

In `SearchScreen.kt`, inside the `if (isOnlineSearch)` block (around line 688, just before `OnlineSearchResults`), add an error banner when `archiveTuneUiState.errorMessage` is not null:

```kotlin
if (isOnlineSearch) {
    // Error banner for online search
    archiveTuneUiState.errorMessage?.let { error ->
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = GoogleSansRounded,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { archiveTuneViewModel.performSearch() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    OnlineSearchResults(
        // ... existing params unchanged
    )
}
```

Add necessary imports if not already present:
```kotlin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
```

- [ ] **Step 2: Add empty-state hint for no query in OnlineSearchResults**

In `OnlineSearchResults.kt`, update the empty state (line 75-89) to show a hint when no search has been performed yet versus when results are genuinely empty:

```kotlin
if (items.isEmpty() && !isSearching) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Search YouTube Music above",
            fontFamily = GoogleSansRounded,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    return
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt app/src/main/java/com/theveloper/pixelplay/presentation/screens/search/components/OnlineSearchResults.kt
git commit -m "feat(search): add error banner and improved empty state for online search"
```

---

### Task 4: Remove Dedicated ArchiveTuneExplore Screen & Navigation

**Files:**
- Delete: `app/src/main/java/com/theveloper/pixelplay/presentation/archivetune/ArchiveTuneExploreScreen.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/navigation/Screen.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt`
- Modify: `app/src/main/java/com/theveloper/pixelplay/presentation/components/StreamingProviderSheet.kt`

**Interfaces:**
- Consumes: Nothing new
- Produces: `Screen.ArchiveTuneExplore` route removed; all navigation references to it removed; `StreamingProviderSheet` YouTube Music row updated to show info-only.

- [ ] **Step 1: Remove ArchiveTuneExplore composable route from AppNavigation**

In `app/src/main/java/com/theveloper/pixelplay/presentation/navigation/AppNavigation.kt`, delete lines 507-516 (the `composable(Screen.ArchiveTuneExplore.route)` block):

```diff
-            composable(
-                Screen.ArchiveTuneExplore.route,
-            ) {
-                ScreenWrapper(navController = navController, playerViewModel = playerViewModel, animatedVisibilityScope = this) {
-                    com.theveloper.pixelplay.presentation.archivetune.ArchiveTuneExploreScreen(
-                        onBack = { navController.popBackStack() },
-                        playerViewModel = playerViewModel
-                    )
-                }
-            }
```

- [ ] **Step 2: Remove Screen.ArchiveTuneExplore from Screen sealed class**

In `app/src/main/java/com/theveloper/pixelplay/presentation/navigation/Screen.kt`, delete line 58:

```diff
-    object ArchiveTuneExplore : Screen("archivetune_explore")
```

- [ ] **Step 3: Remove onNavigateToArchiveTuneExplore from StreamingProviderSheet**

In `app/src/main/java/com/theveloper/pixelplay/presentation/components/StreamingProviderSheet.kt`:

Remove the `onNavigateToArchiveTuneExplore` parameter (line 44):
```diff
-    onNavigateToArchiveTuneExplore: () -> Unit = {},
```

Update the YouTube Music `ProviderRow` (lines 108-119) to remove the navigation callback. Change the onClick to just dismiss the sheet (since YouTube Music is now inline on Home & Search):

```kotlin
ProviderRow(
    iconPainter = painterResource(R.drawable.ic_youtube_music),
    iconTint = Color(0xFFFF0000),
    title = stringResource(R.string.archivetune_provider_name),
    subtitle = "Available on Home & Search",
    shape = providerSegmentItemShape,
    isConnected = true,
    onClick = {
        onDismissRequest()
    }
)
```

- [ ] **Step 4: Remove ArchiveTuneExplore navigation from HomeScreen**

In `app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt`:

Remove the `onNavigateToArchiveTuneExplore` lambda from the `StreamingProviderSheet` call (around line 592-594):
```diff
         StreamingProviderSheet(
             onDismissRequest = { showStreamingProviderSheet = false },
-            onNavigateToArchiveTuneExplore = {
-                navController.navigateSafely(Screen.ArchiveTuneExplore.route)
-            },
             isNeteaseLoggedIn = isNeteaseLoggedIn,
```

Remove the import of `Screen.ArchiveTuneExplore` if no longer used anywhere in `HomeScreen.kt`.

- [ ] **Step 5: Delete the ArchiveTuneExploreScreen file**

```bash
rm app/src/main/java/com/theveloper/pixelplay/presentation/archivetune/ArchiveTuneExploreScreen.kt
```

**Important:** Do NOT delete `ArchiveTuneExploreViewModel.kt` — it is still used by `HomeScreen.kt` and `SearchScreen.kt`.

- [ ] **Step 6: Verify compilation**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(ui): remove dedicated ArchiveTuneExplore screen, consolidate online into Home/Search"
```

---

### Task 5: Cleanup Unused Imports & String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (if explore-only strings exist)
- Modify: Various source files (remove unused imports)

**Interfaces:**
- Consumes: Nothing
- Produces: Clean codebase with no dangling references to removed screen

- [ ] **Step 1: Search for unused ArchiveTuneExplore string resources**

Run:
```bash
grep -rn "archivetune_explore" app/src/main/res/values/strings.xml
```

If `archivetune_explore_title` and `archivetune_explore_subtitle` string resources exist and are no longer used (they were only used in `ArchiveTuneExploreScreen.kt` which is now deleted), remove them from `strings.xml`.

- [ ] **Step 2: Search for any remaining references to ArchiveTuneExploreScreen**

Run:
```bash
grep -rn "ArchiveTuneExploreScreen\|archivetune_explore\|onNavigateToExplore\|onNavigateToArchiveTuneExplore" app/src/main/java/ app/src/main/res/
```

Fix any remaining references found.

- [ ] **Step 3: Verify full build**

Run: `./gradlew :app:assembleDebug -Ppixelplay.enableAbiSplits=true`
Expected: BUILD SUCCESSFUL

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: cleanup unused imports and string resources after ArchiveTuneExplore removal"
```

---

### Task 6: Final Integration Verification

**Files:**
- No file changes

- [ ] **Step 1: Execute full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Execute debug APK build**

Run: `./gradlew :app:assembleDebug -Ppixelplay.enableAbiSplits=true`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify no remaining references to removed code**

Run:
```bash
grep -rn "ArchiveTuneExploreScreen\|Screen\.ArchiveTuneExplore\|archivetune_explore" app/src/main/java/ app/src/main/res/
```

Expected: No results (zero remaining references).

- [ ] **Step 4: Final commit**

```bash
git commit --allow-empty -m "chore: complete inline online mode consolidation and dedicated screen removal"
```
