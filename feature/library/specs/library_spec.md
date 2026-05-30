# Feature Specification: Library Browsing & Sync

This document defines the requirements, behavioral rules, and sync workflows for the library browsing, book metadata synchronization, book detail fetching, and offline resilience within the `:feature:library` module and its supporting data layers.

---

## 1. Feature Context & Constraints

The `:feature:library` module is responsible for presenting the user's audiobook collection. It provides the primary browsing experience after login, including a searchable cover grid, filtering by read status and download state, sorting, and navigation into individual book detail screens.

Library data flows through these layers:
- **`:core:network`**: Fetches library lists and book metadata from the Audiobookshelf server via Ktor.
- **`:core:database`**: Caches book metadata locally in the `books` table (Room) for offline access.
- **`:data`**: Repository implementations coordinating network fetches, local cache reads/writes, and download management.
- **`:domain`**: Use cases (`SyncLibraryBooksUseCase`, `FetchBookDetailsUseCase`, `GetBooksUseCase`, `GetPlaybackProgressUseCase`) exposing clean operations to ViewModels.
- **`:feature:library`**: Presentation layer containing `LibraryViewModel`, `DetailViewModel`, `LibraryScreen`, and `DetailScreen`.

---

## 2. Library Sync

### A. Library Selection
1. **Auto-Selection on Login**: After successful authentication, the client must fetch the list of available libraries via `GET /api/libraries`. The first library with `type == "audiobook"` is automatically selected. If no audiobook library exists, the first library in the list is selected as a fallback.
2. **Library Selector UI**: The library screen must include a library selector component (e.g., a dropdown or top bar picker) that allows the user to switch between available libraries at any time. Changing the selected library must:
   - Save the new library ID to `PreferencesManager` via `saveLibraryId()`.
   - Trigger a full book list sync for the newly selected library.
   - Clear the current in-memory book list and reload from the new library's cached data.
3. **Library List Caching**: The list of available libraries returned from `GET /api/libraries` must be cached locally in the Room database (`libraries` table) to allow offline accessibility and populating the selector without network calls.
4. **Library ID Persistence**: The selected library ID is saved to `PreferencesManager` via `saveLibraryId()` and persisted across app restarts.
5. **No Library Selected**: If no library ID is stored (e.g., preferences were cleared), the library screen must display an error message and prevent sync attempts.

### B. Book List Sync (Pull from Server)
1. **Sync Trigger Points**: A book list sync must occur at these moments:
   - On initial login (fired-and-forgotten from `LoginViewModel` after library selection).
   - When the `LibraryScreen` is first loaded (`LibraryViewModel.init`).
   - On explicit user-initiated pull-to-refresh.
   - **On a configurable time interval** (see §2.B.5 below).
2. **Network Endpoint**: `GET /api/libraries/{libraryId}/items?limit={limit}&page={page}`
   - The `limit` parameter controls the maximum number of items returned per page.
   - Default page size: `100`.
3. **Pagination**: The client must page through the entire library item set:
   - Start at `page=0` and increment until all items have been retrieved.
   - The server response includes a `total` field indicating the total number of items in the library. Continue fetching pages until the cumulative number of fetched items reaches or exceeds `total`.
   - All pages are aggregated into a single batch for the upsert operation.
4. **ETag-Based Conditional Requests**:
   - On each sync request, send an `If-None-Match` header with the previously stored ETag value for the library (per library ID).
   - If the server responds with `304 Not Modified`, skip the sync and retain the existing local cache.
   - If the server responds with `200 OK`, store the new `ETag` response header value in `PreferencesManager` (keyed by library ID) and proceed with the sync.
   - If no ETag is stored (first sync), omit the `If-None-Match` header.
   - **Force Refresh**: An explicit user-initiated pull-to-refresh / swipe-to-refresh on the library screen must bypass the ETag check (omit the `If-None-Match` header), forcing a full sync from the server and updating the database cache.
5. **Automatic Periodic Sync**:
   - The client must automatically re-sync the library at a user-configurable interval.
   - **Default interval**: 24 hours.
   - **Setting key**: `library_sync_interval_hours` (Integer), stored in `PreferencesManager`.
   - **Configurable in Settings**: The user can adjust this value in the Settings screen. Supported values: `1`, `6`, `12`, `24`, `48`, `72` hours, or `0` to disable automatic sync.
   - **Implementation**: On `LibraryViewModel.init`, check the timestamp of the last successful sync (`library_last_sync_timestamp` in `PreferencesManager`). If the elapsed time exceeds the configured interval, trigger a background sync.
   - **Last Sync Timestamp**: After every successful sync, persist the current epoch milliseconds as `library_last_sync_timestamp`.
6. **Sync Strategy (Replace with Preserve)**:
   - For each server item, check for an existing local `BookEntity` by ID.
   - Create a new `BookEntity` using server metadata (title, author, narrator).
   - **Preserve local-only fields** from the existing entity if present: `coverPath`, `isDownloaded`, `audioFiles`, and `chapters`. These fields contain locally cached cover paths and download state that must not be overwritten by the sync.
   - Fields not available from the list endpoint (`description`, `duration`) are set to defaults (`""` and `0.0` respectively) and are populated later via the book detail fetch.
   - Insert all entities via `insertAll()` (upsert behavior).
7. **Error Handling**: If the sync network request fails, the error message is surfaced to the UI via `LibraryViewModel.error`. The locally cached book list remains displayed.

### C. Book Detail Fetch (Per-Book Enrichment)
1. **Trigger**: When the user navigates to a book's detail screen, `DetailViewModel.setBookId()` fires a detail fetch.
2. **Network Endpoint**: `GET /api/items/{bookId}`
3. **ETag-Based Conditional Requests**:
   - Store a per-book ETag value in the `BookEntity` (new column: `etag`).
   - On each detail fetch, send an `If-None-Match` header with the stored ETag.
   - If the server responds with `304 Not Modified`, skip the update and use the cached entity.
   - If the server responds with `200 OK`, store the new `ETag` from the response header and proceed with the merge.
4. **Time-Based Refresh**:
   - Store a `lastDetailFetchTimestamp` (epoch milliseconds) in the `BookEntity`.
   - On navigating to the detail screen, if the elapsed time since the last fetch exceeds a threshold (default: **24 hours**, matching the library sync interval), trigger a fresh network fetch even if the user has visited before.
   - If the elapsed time is within the threshold, skip the network call and display the cached entity (unless the user explicitly refreshes).
5. **Force Refresh**:
   - The user can explicitly trigger a refresh (e.g., via swipe to refresh on the detail screen).
   - A force refresh must bypass the `lastDetailFetchTimestamp` 24-hour cache threshold and bypass the ETag-based conditional check (omit the `If-None-Match` header), ensuring a full refresh from the server and updating the local database cache.
6. **Detail Sync Strategy**:
   - Fetch the full book response including `audioFiles` and `chapters` arrays.
   - Merge with existing local entity: preserve `coverPath` and per-file `localPath` / `downloadStatus` from previously downloaded files.
   - Compute `duration` by summing all audio file durations.
   - Update `etag` and `lastDetailFetchTimestamp`.
   - Insert the updated `BookEntity` to the local database.
6. **Server Progress Fetch**: After storing the book entity, fetch the server-side playback progress via `GET /api/me/progress/{bookId}`.
   - If server progress exists and `currentTime` exceeds the locally stored progress, update the local `PlaybackProgressEntity` to match the server state.
   - If no server progress exists (404), take no action.
7. **Offline Fallback**: If the detail network request fails, attempt to load the book from the local Room cache. If a cached entity exists, return it. If no cached entity exists, propagate the error to the UI.

---

## 3. Library Browsing UI

### A. Book Grid Display
1. **Layout**: Books are displayed in an adaptive grid using `LazyVerticalGrid` with a minimum cell width of `140.dp`.
2. **Database-Backed Paging**: The grid must use Room's Paging 3 integration (`PagingSource`) to load books from the database in pages, rather than loading the entire book list into memory. The `LazyVerticalGrid` should consume a `LazyPagingItems` collection.
3. **Cover Art Loading**:
   - If the book has a local `coverPath` (non-null, non-empty), load the image directly from the local file path with no authorization header.
   - If no local cover exists, construct the remote URL: `{serverUrl}/api/items/{bookId}/cover` with a `Bearer {token}` authorization header.
   - If the remote cover load fails, display a gradient fallback showing the first letter of the title.
4. **Card Content**: Each book card displays:
   - Cover image with **`1:1` square aspect ratio**.
   - Title (single line, ellipsis overflow).
   - Author (single line, ellipsis overflow).
5. **Card Badges** (icon-based, no text labels):
   - **Read icon** (top-start): A check/done icon displayed when `progress.isFinished == true` or `progress.progress >= 0.99f`.
   - **Downloaded icon** (top-end): A download-done or offline-pin icon displayed when `isDownloaded == true`.
   - **Progress bar** (bottom of cover): Displayed when `progress > 0f` and the book is not finished.

### B. Search
1. **Scope**: Database-level search query. Search is performed via a Room DAO query, **not** by filtering an in-memory list.
2. **Searchable Fields**: Title, author, and narrator.
3. **Matching**: Case-insensitive `LIKE` query matching against title, author, and narrator columns. The search term is wrapped with `%` wildcards (e.g., `%query%`).
4. **Integration with Paging**: The search query must integrate with the Paging 3 data source so that filtered results are also paged from the database.

> [!NOTE]
> **Future Consideration — Local AI-Assisted Search**: Evaluate using an on-device language model (e.g., Gemini Nano via ML Kit or a local embedding model) to enable semantic search across book metadata. This could support natural-language queries like "that mystery series by the British author" or fuzzy matching of misspelled titles. Not to be implemented in the current version.

### C. Filtering
1. **Read Status Filter**: Filters books by their playback progress state. Options:
   - `ALL`: No filtering applied (default).
   - `UNREAD`: Books with no progress or `progress == 0f` and not finished.
   - `IN_PROGRESS`: Books with `progress > 0f` and not finished.
   - `READ`: Books where `isFinished == true` or `progress >= 0.99f`.
2. **Downloaded Only Filter**: When enabled, only books with `isDownloaded == true` are shown.
3. **Persistence**: Both filter selections are persisted to `PreferencesManager` and restored on ViewModel initialization.

### D. Sorting
1. **Available Sort Options**:
   - `TITLE_ASC` / `TITLE_DESC`: Alphabetical by title (case-insensitive).
   - `AUTHOR_ASC` / `AUTHOR_DESC`: Alphabetical by author (case-insensitive).
   - `DURATION_ASC` / `DURATION_DESC`: By total book duration.
   - `LAST_PLAYED`: By `lastUpdated` timestamp descending. Books with no progress are sorted by title as a tiebreaker.
2. **Default**: `TITLE_ASC`.
3. **Persistence**: The selected sort option is persisted to `PreferencesManager` and restored on ViewModel initialization.

### E. Swipe to Refresh
1. **Behavior**: The library screen must support swipe-to-refresh gesture.
2. **Force Refresh Integration**: Triggering swipe-to-refresh initiates a force sync of the library books, bypassing the ETag conditional checks, forcing the database cache to be updated with fresh metadata from the server (as defined in §2.B.4).

---

## 4. Book Detail Screen

### A. Detail Content
1. **Displayed Fields**: Cover image, title, author, narrator, description, total duration (formatted), chapter list, and playback progress.
2. **Chapter List**: Displayed as a **collapsible section** on the detail screen. The section header shows "Chapters ({count})" and can be expanded or collapsed by tapping. Default state is **collapsed**. When expanded, each chapter shows:
   - Title (with fallback to `"Chapter {index + 1}"` if empty).
   - Start time (formatted as `HH:MM:SS`).
   - Duration (formatted).
3. **Progress Display**: If progress exists, show remaining time formatted as a duration string.

### B. Download Management
1. **Download Action**: Initiates download of all audio files not yet marked `COMPLETED` using the Android `DownloadManager`.
2. **Download URL Format**: `{serverUrl}/api/items/{bookId}/file/{ino}/download` with `Bearer {token}` authorization header.
3. **Download Progress**: A reactive flow polls the `DownloadManager` every second and reports aggregate progress across all files as a `0f` to `1f` float.
4. **Delete Downloaded Files**: Cancels any active downloads for the book, recursively deletes the local file directory (`downloads/{bookId}`), and resets all `audioFiles` entries to `NOT_DOWNLOADED` status.

### C. Playback Initiation
1. Tapping the play action calls `PlayerManager.playBook(book, startPosition)` where `startPosition` is derived from saved progress (`currentTime`) or `0.0` for new books.

### D. Swipe to Refresh
1. **Behavior**: The detail screen must support swipe-to-refresh gesture.
2. **Force Refresh Integration**: Triggering swipe-to-refresh initiates a force refresh of the book detail network request, bypassing the ETag check and time-based threshold, forcing the database cache to be updated with fresh metadata from the server (as defined in §2.C.5).

---

## 5. Offline Resilience

1. **Cached Book List**: The `books` table in Room serves as the offline source of truth. When the network is unavailable, the locally cached book list continues to be displayed via the Room Paging 3 subscription.
2. **Reactive Updates**: Any local database mutation (from sync, detail fetch, or download completion) automatically invalidates the Paging source and refreshes the UI without manual intervention.
3. **Graceful Degradation**: Failed network calls surface user-facing error messages but never clear or corrupt the existing local cache.

---

## 6. Future Considerations

> [!IMPORTANT]
> The following items are documented for future architectural planning. They are **not** in scope for the current implementation.

### A. Multi-Server Support
Consider supporting connections to multiple Audiobookshelf server instances simultaneously. This would allow users who self-host across different machines (e.g., home NAS and a VPS) to browse and sync libraries from all servers within a single app session. Key architectural implications:
- The `PreferencesManager` would need to store multiple server configurations (URL, token, username) keyed by a server ID.
- The `books` table would need a `serverId` foreign key to disambiguate books from different servers.
- The library selector would need a two-level hierarchy: server → library.
- Network requests would need to be scoped to the correct server base URL and auth token.

### B. Local AI-Assisted Search
See note in §3.B for details on potential on-device semantic search capabilities.
