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
  - *Returns*: A JSON object containing a `playlists` array of playlist objects: `{"playlists": [NetworkPlaylistResponse]}`. Each playlist contains ID, name, description, user ID, cover path, total duration, and items.
- **Get Playlist Details**: `GET /api/playlists/{playlistId}`
- **Update Playlist (Reordering/Deletion)**: `PATCH /api/playlists/{playlistId}`

### B. Sync Strategy & Caching
1. **Trigger**: Synchronizes when:
   - The Playlists Tab is loaded (stale cache check).
   - Swipe-to-refresh is executed on any library tab (triggers global force sync).
2. **ETag Check & Force Sync**: Uses standard HTTP ETag checks via `If-None-Match`. If `304 Not Modified` is returned, the local cache is retained. A global swipe-to-refresh bypasses the ETag check (omits `If-None-Match`) to force update all playlists data.
3. **Database Schema & Item Deserialization**:
   - `PlaylistEntity`: `id` (String, PK), `name` (String), `description` (String?), `duration` (Double), `itemCount` (Int), `lastUpdated` (Long).
   - `PlaylistItemEntity`: `id` (String, PK), `playlistId` (String, FK), `libraryItemId` (String), `sequence` (Int), `title` (String), `duration` (Double).
   - *Playlist Item ID Fallback*: The server's playlist item objects might not return a unique `id` field. To prevent deserialization errors and maintain database primary key integrity, the `id` field is optional/nullable in the model. If missing from the server response, the client automatically generates a unique identifier locally using the structure `"${playlistId}_${libraryItemId}_${sequence}"` to populate `PlaylistItemEntity.id`.

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
    - **Contextual Action Menu**: Tapping the overflow menu button on a track row opens the `ItemMoreMenuBottomSheet` defined in [book_detail_spec.md](book_detail_spec.md#e-contextual-action-menu), featuring an additional **Remove from Playlist** option. Selecting this action triggers a call to `DELETE /api/playlists/{playlistId}/item/{bookId}` (with episode ID if applicable), updates the local cache, and removes the item from the display list.

### C. Search & Sorting
- **Search**: Scopes search input to playlist names or descriptions.
- **Sorting**:
  - `NAME_ASC` / `NAME_DESC`: Alphabetical by name.
  - `TRACKS_COUNT_DESC`: By number of tracks in the playlist.
  - `DURATION_DESC`: By total running duration.

---

## 4. UI States & Offline Resilience

- **Scrollable Empty State**: If no playlists are found, the Playlists tab renders a scrollable empty state (via `LibraryEmptyState` supporting vertical scroll). This ensures that swipe-to-refresh gestures propagate properly, and renders a manual "Sync Now" button to trigger a database refresh.
