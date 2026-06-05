# Feature Specification: Library - Playlists Tab

This document defines the requirements, behavioral rules, and caching patterns for the **Playlists Tab** within the library browsing screen. It is a sub-specification of the main [Library Specification](library_spec.md).

---

## 1. Context & Dependencies

Playlists represent user-constructed playback queues (ordered sequences of chapters or audiobooks). Playlists are globally associated with a user profile on the server, rather than isolated within a single library.

It depends on:
- **`:core:database`**: Local Room database caching using `playlists` and `playlist_items` tables.
- **`:domain`**: Use cases `GetPlaylistsUseCase`, `SyncPlaylistsUseCase`, and `PlayPlaylistUseCase`.
- **`:feature:library`**: The `PlaylistsTabContent` list UI and `PlaylistDetailScreen` views.
- **`:core:player`**: `PlayerManager` to handle sequential playback queue loading.

---

## 2. Data Integration & Sync

### A. API Endpoints
- **List All Playlists**: `GET /api/playlists`
  - *Returns*: An array of playlists containing playlist ID, name, description, user ID, cover/image url, total duration, and list of items.
- **Get Playlist Details**: `GET /api/playlists/{playlistId}`

### B. Sync Strategy & Caching
1. **Trigger**: Synchronizes when:
   - The Playlists Tab is loaded (stale cache check).
   - Swipe-to-refresh is executed on any library tab (triggers global force sync).
2. **ETag Check & Force Sync**: Uses standard HTTP ETag checks via `If-None-Match`. If `304 Not Modified` is returned, the local cache is retained. A global swipe-to-refresh bypasses the ETag check (omits `If-None-Match`) to force update all playlists data.
3. **Database Schema**:
   - `PlaylistEntity`: `id` (String, PK), `name` (String), `description` (String?), `duration` (Double), `itemCount` (Int), `lastUpdated` (Long).
   - `PlaylistItemEntity`: `id` (String, PK), `playlistId` (String, FK), `libraryItemId` (String), `sequence` (Int), `title` (String), `duration` (Double).

---

## 3. UI & Interaction Rules

### A. List Layout & Design
Playlists are presented in a list layout:
- **Row Content**:
  - **Thumbnail**: A generic, styled playlist icon (`Icons.AutoMirrored.Filled.PlaylistPlay` colored with `CyanAccent` in a `DarkSurfaceVariant` rounded square wrapper).
  - **Name & Meta**: Bold playlist title, brief description, and combined meta info row: `"15 tracks • 8h 24m"`.
  - **Play Action Button** (right-aligned): A circular button containing a play icon (`Icons.Filled.PlayArrow`). Tapping this button immediately launches playback of the playlist from the beginning, bypassing detail screens.
- Tapping anywhere else on the row navigates to the **Playlist Detail Screen**.

### B. Navigation (Playlist Detail Screen)
- Tapping a playlist navigates to the **Playlist Detail Screen**.
- **Playlist Detail UI**:
  - Header showing the playlist title, total tracks count, and accumulated duration.
  - Large **Play Playlist** action button at the top.
  - **Interactive List**: An ordered list of tracks/items inside the playlist.
    - Displays book cover (small), track title, and track duration.
    - Supports **reordering** via drag-and-drop (updates sequence positions and syncs back to the server using a background sync task).
    - Each track row has a delete/remove button to remove that item from the playlist.
    - Tapping an item starts playback immediately from that specific index in the queue.

### C. Search & Sorting
- **Search**: Scopes search input to playlist names or descriptions.
- **Sorting**:
  - `NAME_ASC` / `NAME_DESC`: Alphabetical by name.
  - `TRACKS_COUNT_DESC`: By number of tracks in the playlist.
  - `DURATION_DESC`: By total running duration.
