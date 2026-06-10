# Feature Specification: Library - Books Tab

This document defines the requirements, behavioral rules, and offline caching patterns for the **Books Tab** within the library browsing screen. It is a sub-specification of the main [Library Specification](library_spec.md).

---

## 1. Context & Dependencies

The Books Tab presents the user's audiobook collection. It handles paging through cached local book metadata, visual rendering of covers, state badges, reading progress, and filters/sort preferences.

It depends on:
- **`:core:database`**: The local `books` table as the offline source of truth.
- **`:domain`**: Use cases `GetBooksUseCase`, `SyncLibraryBooksUseCase`, and `GetPlaybackProgressUseCase`.
- **`:feature:library`**: The `BooksTabContent` composable, `BookMenuActionUtil`, and corresponding ViewModel state.

---

## 2. Book List Sync (Pull from Server)

To ensure the local cache is populated, the Books Tab coordinates book synchronization.

### A. Sync Trigger Points
A book list sync must occur at these moments:
1. When a new library is selected from the library selector (triggered by container level).
2. When the Books Tab is first loaded/initialized (`LibraryViewModel.init`).
3. When a global swipe-to-refresh action is initiated on any library tab.
4. **On a configurable time interval** (defined below).

### B. Network Integration
- **Endpoint**: `GET /api/libraries/{libraryId}/items?limit={limit}&page={page}&minified=0`
- **Default Page Size**: `100`.
- **Enhanced Book Sync**: Setting `minified=0` (or omitting the parameter) instructs the server to return full, non-minified library items. This allows the client to retrieve critical metadata fields such as the book description and running duration directly during list synchronization.
- **Pagination**: The client must page through the entire library item set. Start at `page=0` and increment until the cumulative number of fetched items reaches or exceeds the `total` field in the server response. Aggregate all pages into a single database upsert batch.

### C. ETag-Based Conditional Requests
- Send an `If-None-Match` header with the previously stored ETag for the active library.
- If the server returns `304 Not Modified`, skip the sync and retain the existing local cache.
- If the server returns `200 OK`, store the new `ETag` response header value in `PreferencesManager` (keyed by library ID) and upsert the database.
- **Force Refresh**: An explicit pull-to-refresh action on any library tab triggers a global force sync that bypasses the ETag check (omits the `If-None-Match` header) for books and all other library categories simultaneously.

### D. Automatic Periodic Sync
- **Default interval**: 24 hours.
- **Setting key**: `library_sync_interval_hours` (Integer) in `PreferencesManager`, adjustable in settings (`1`, `6`, `12`, `24`, `48`, `72` hours, or `0` to disable).
- On initialization, if the elapsed time since `library_last_sync_timestamp` exceeds the configured interval, trigger a background sync.

### E. Database Merge Strategy (Preserve Local State)
- For each item from the server:
  - Check for an existing local `BookEntity` by ID.
  - Create/update the entity using server metadata (title, author, narrator, as well as the enhanced `description` and `duration` fields).
  - **Preserve local-only fields** if present: `coverPath`, `isDownloaded`, `audioFiles`, and `chapters` (so local downloads are not lost or overwritten).
  - Heavy fields not available in the library items list endpoint (e.g. `chapters` list and `audioFiles` array) remain empty/unpopulated until loaded via the book detail fetch.
  - Write all updated entities to the database in a single transaction.

---

## 3. UI & Interaction Rules

### A. Grid Layout
- Display books in an adaptive grid (`LazyVerticalGrid`) with a minimum cell width of `140.dp`.
- **Database-Backed Paging**: Use Room's Paging 3 integration (`PagingSource`) to load books from the database dynamically in pages. The UI consumes `LazyPagingItems`.

### B. Card Content & Styling
Each card displays:
1. **Cover Image**: Loaded in a square `1:1` aspect ratio.
   - If local `coverPath` exists, load from local file path (no auth headers).
   - Otherwise, fetch from `{serverUrl}/api/items/{bookId}/cover` with a `Bearer {token}` header.
   - On failure, draw a dynamic gradient fallback showing the first letter of the book's title centered.
2. **Text**: Title (single line, ellipsis overflow) and Author (single line, ellipsis overflow) styled in accordance with the [Design Specification](../../../specs/design_spec.md).
3. **Badges**:
   - **Read badge** (top-start): A check icon shown when `progress.isFinished == true` or `progress.progress >= 0.99f`.
   - **Downloaded badge** (top-end): A download-done or offline-pin icon shown when `isDownloaded == true`.
   - **Progress Indicator**: A `4.dp` linear progress indicator overlaying the bottom edge of the cover if the book is in-progress (progress > 0% and not finished).

### C. Search
- **Scope**: Database-level query against Title, Author, Narrator (case-insensitive `LIKE` matching).
- **Paging Integration**: The search query must integrate with Paging 3 to load matched results dynamically from Room.

### D. Filtering
Filters are stored in `PreferencesManager` and restored on initialization:
- **Read Status**: Options are `ALL`, `UNREAD` (`progress == 0`), `IN_PROGRESS` (`progress > 0` and not finished), or `READ` (`progress >= 0.99f` or finished).
- **Downloaded Only**: Show only books with `isDownloaded == true`.

### E. Sorting
Sort preferences are persisted in `PreferencesManager` and restored on initialization:
- `TITLE_ASC` / `TITLE_DESC` (default: `TITLE_ASC`)
- `AUTHOR_ASC` / `AUTHOR_DESC`
- `DURATION_ASC` / `DURATION_DESC`
- `LAST_PLAYED` (using `lastUpdated` timestamp descending; uses title alphabetical as a tiebreaker).

### F. Contextual Action Menu
1. **Trigger**: Long-pressing a book card in the grid or tapping the overflow menu icon on the card launches the `ItemMoreMenuBottomSheet`.
2. **Behavior**: Offers quick actions on the selected book item, matching the options defined in [book_detail_spec.md](book_detail_spec.md#e-contextual-action-menu) (Mark Finished/Unfinished, Discard Progress, Add to Playlist, Delete Download, and Go to Web Client).

---

## 4. Offline Resilience

1. **Cached Data source**: Room's `books` table is the offline source of truth. When offline, Paging 3 automatically feeds the cached books grid.
2. **Graceful Failures**: Network failures during sync show transient error messages (e.g. Snackbars) and do not disrupt access to cached book metadata.
3. **Scrollable Empty and Error States**: When there are no books or a sync error occurs on initial load, a scrollable empty/error state is rendered (via `LibraryEmptyState` supporting vertical scroll). This ensures that swipe-to-refresh gestures propagate properly, and displays a manual "Sync Now" button.
