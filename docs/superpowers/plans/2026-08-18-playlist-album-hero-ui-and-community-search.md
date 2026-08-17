# Playlist & Album Hero UI Redesign & Community Playlist Search Plan

## Problem Statement
1. **Playlist / Album Page UI**: The user requested a modern, immersive playlist and album detail page matching reference Image 1 with:
   - Full-bleed / large top artwork banner fading smoothly into the surface background.
   - Transparent top navigation bar with back arrow, search icon, and overflow options menu.
   - Centered large bold title, metadata (song count, duration, view count, badge).
   - Centered floating action row with circular Shuffle button, wide rounded "▶ Play" pill button, "+" Add button, and More options button.
   - Polished song list items with square thumbnail, title, artist • duration • metadata, and 3-dots menu.
2. **Community Playlist Search**: When searching playlists, only official featured playlists were returned (`FILTER_FEATURED_PLAYLIST`) and community / user-created playlists were omitted. Searching in the "Playlists" filter must return community playlists (`FILTER_COMMUNITY_PLAYLIST`) merged with featured playlists.

---

## User Constraints & Rules
- **CRITICAL**: Do NOT run any local Gradle build or test commands (`./gradlew test`, etc.) on the host machine.
- Verify everything via static code analysis, code review, and remote CI push to GitLab and GitHub.

---

## Proposed Changes

### Task 1: Fix Community & Official Playlist Search in `ArchiveTuneExploreViewModel` & `SearchScreen`
- Update `ArchiveTuneExploreViewModel.kt`:
  - When `ArchiveTuneSearchCategory.PLAYLISTS` is selected, perform concurrent searches for both `FILTER_COMMUNITY_PLAYLIST` and `FILTER_FEATURED_PLAYLIST`.
  - Merge the results with community playlists prioritized, deduplicate by ID, and filter items to only include `PlaylistItem` when in the Playlists category.
  - Update `loadMoreSearchResults()` to page continuation tokens for community playlists.
- Update `OnlineSearchResults.kt` to ensure category-appropriate rendering.

### Task 2: Create Reusable Immersive `HeroDetailHeader` Component
- Create `app/src/main/java/com/theveloper/pixelplay/presentation/components/HeroDetailHeader.kt`:
  - Immersive artwork banner with vertical gradient fade into `MaterialTheme.colorScheme.surface`.
  - Floating transparent top app bar with Back button, Search button, and Overflow menu (`...`).
  - Centered large bold title (headlineMedium/Large, GoogleSansRounded, multiline).
  - Centered subtitle/metadata with optional badge (e.g. "YouTube Music", view count, song count, duration).
  - Centered Action Row:
    - Circular Shuffle button
    - Large high-contrast "▶ Play" pill button
    - Circular "+" Add to library / playlist button
    - Circular More / Sort button

### Task 3: Redesign `PlaylistDetailScreen` with the New Hero Header Layout
- Update `app/src/main/java/com/theveloper/pixelplay/presentation/screens/PlaylistDetailScreen.kt`:
  - Replace the old text-only `LargeFlexibleTopAppBar` with the new immersive `HeroDetailHeader`.
  - Connect Shuffle, Play, Add to Library/Playlist, Search, and Options actions.
  - Enhance song list items to match the reference layout with high-resolution thumbnails, metadata, and 3-dots menu.

### Task 4: Redesign `AlbumDetailScreen` with the New Hero Header Layout
- Update `app/src/main/java/com/theveloper/pixelplay/presentation/screens/AlbumDetailScreen.kt`:
  - Adopt the new `HeroDetailHeader` for album art, artist subtitle, play/shuffle actions, and add-to-playlist action.
  - Ensure smooth collapsing/scrolling behavior.

### Task 5: End-to-End Verification & Remote Push to GitLab and GitHub
- Static verification of all changed files.
- Commit and push to `gitlab master` and `github master`.

---

## Execution Choice
We will execute using Subagent-Driven Development with the progress ledger in `.superpowers/sdd/2026-08-18-playlist-album-hero-ui-and-community-search/progress.md`.
