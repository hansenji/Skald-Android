# Feature Specification: Library Container & Navigation

This document defines the requirements, behavioral rules, layout structures, and synchronization entry points for the main library container screen (`:feature:library`) in the Skald application. 

The library screen acts as the host for a tabbed navigation interface that categorizes the user's content library into five distinct views: **Books**, **Series**, **Collections**, **Authors**, and **Playlists**.

---

## 1. Feature Context & Architecture

The library container handles global screen state, library database synchronization coordination, the library switcher interface, and delegates specific tab contents to dedicated sub-components.

Data and event propagation flow through the following layers:
- **`:core:preferences`**: Persists active library and tab selections.
- **`:core:database`**: Stores available libraries lists for offline dropdown loading.
- **`:domain`**: Exposes use cases like `FetchLibrariesUseCase`.
- **`:feature:library`**: The main `LibraryScreen` container, `LibraryViewModel`, and the modular tab content composables.

---

## 2. Library Selection & Management

### A. Library Auto-Selection on Login
1. On initial login, the client queries available server libraries via `GET /api/libraries`.
2. The client auto-selects the first library where `mediaType == "book"` (or fallback to `"audiobook"`). If none exist, it falls back to the first available library in the response.
3. The selected library ID is saved in `PreferencesManager` and persisted across application restarts.

### B. Library Selector UI
1. The container's top navigation bar must include a dropdown menu or top-bar sheet containing the list of cached libraries.
2. Changing the active library:
   - Stores the new library ID.
   - Clears the active memory of the current view.
   - Triggers a full synchronization flow across all tabs (Books, Series, Collections, Authors, and Playlists) to ensure all data is immediately aligned for the new library.
3. If no library is selected or preferences are cleared, the screen displays a friendly, scrollable empty state prompting the user to select a library, including a "Refresh Libraries" button to re-fetch libraries from the server (which operates even without an active library selection).

### C. Library List Caching
1. The libraries list returned from `GET /api/libraries` is cached in the Room local database (`libraries` table) to support offline picker loading.

---

## 3. Global Library Synchronization

### A. Background & Manual Synchronization
1. **Scope**: All synchronization operations triggered globally—either automatically on a periodic interval or manually via the "Sync Now" button in the Settings Screen—must query and update local database caches for all five tabs (Books, Series, Collections, Authors, and Playlists).
2. **Execution**: The sync worker or manager executes calls to the individual synchronization use cases (e.g. `SyncLibraryBooksUseCase`, `SyncLibrarySeriesUseCase`, `SyncLibraryCollectionsUseCase`, etc.) sequentially or concurrently.
3. **Caching**: Uses ETag checks (`If-None-Match`) for each endpoint during periodic syncs to minimize bandwidth. The manual "Sync Now" option bypasses ETag checks (omits `If-None-Match`) for all endpoints.

### B. Swipe-To-Refresh Sync
1. Pulling to refresh within the library container triggers an immediate force synchronization (bypassing ETag checks / omitting `If-None-Match` header) for all library tabs (Books, Series, Collections, Authors, and Playlists) concurrently or sequentially. If no library is active yet, pull-to-refresh will fetch the list of available libraries.
2. The UI displays an active refresh indicator/spinner until all tab synchronizations have completed or failed.

---

## 4. Tab Container UI Layout

The library container screen is laid out according to the [Design Specification](../../../specs/design_spec.md).

```
+-----------------------------------+
| [Library Dropdown]        [Search]|  <-- Top Bar (Sticky)
+-----------------------------------+
| Books | Series | Coll. | Auth | Pl|  <-- M3 ScrollableTabRow
+-----------------------------------+
|                                   |
|                                   |
|       Active Tab Content          |  <-- Swipe-To-Refresh Wrapper
|       (Lazy Paged Grid/List)      |
|                                   |
|                                   |
+-----------------------------------+
| ====== Mini-Player Banner ======= |  <-- sticks above bottom navigation
+-----------------------------------+
```

### A. Component Hierarchy
1. **Top App Bar**: Sticky bar hosting the Library Selector dropdown button and the Search action trigger.
2. **Tab Bar**: A Material 3 `ScrollableTabRow` hosting the visible tab buttons in order: Books, Series, Collections, Authors, Playlists. Tabs are filtered dynamically if "Hide Empty Tabs" is enabled (see §4.B.2).
3. **Swipe-To-Refresh Wrapper**: A container wrapping the entire active tab screen area. Pulling down triggers the active tab and background sync behavior defined in §3.B.
4. **Active Content View**: The container switches out the display view depending on the selected tab state (`BooksTabContent`, `SeriesTabContent`, etc.).

### B. State Management & Tab Visibility
1. **Selected Tab State**: `LibraryViewModel` maintains the current selected tab state: `currentTab: StateFlow<LibraryTab>`.
2. **Dynamic Tab Visibility**:
   - The view model monitors the `hide_empty_library_tabs` preference setting.
   - If enabled (`true`), the container queries the database item counts for each of the secondary tabs (Series, Collections, Authors, Playlists).
   - Any secondary tab with an item count of `0` is excluded from the list of tabs rendered in the `ScrollableTabRow`.
   - **Exception**: The **Books** tab is the primary landing view and remains **always visible**, even if empty.
   - **Fallback**: If the currently active tab becomes hidden (e.g. after a library sync or when the setting is toggled on), the selection automatically falls back to the **Books** tab.
3. **Persisted Tab**: Selected tab index is cached in memory (optionally persisted in `PreferencesManager` to return the user to their last active tab on app reload, provided it is not hidden).

### C. Global Search Bar & Delegation
1. Tapping the search icon in the top app bar expands the search field.
2. As the user types, the query string is delegated to the currently active tab:
   - The query filter is passed down to the active tab's ViewModel.
   - Each tab performs its own scoped search query against its respective database tables (e.g. searching authors, series names, book titles).

### D. Empty & Error State Rendering
1. **Scrolling Support**: All empty and error state layouts (no library selected, book loading error, empty books list, empty series list) must use vertical scrolling (via `Modifier.verticalScroll(rememberScrollState())`) to ensure that swipe-to-refresh gestures correctly propagate up to the container's parent `PullToRefreshBox`.
2. **Direct Action Buttons**: Empty and error states must render a direct action button (e.g. "Sync Now" or "Refresh Libraries") to allow manual synchronization/recovery.

---

## 5. Offline Resilience

1. **Local Collections**: The list of available libraries is loaded from Room when offline.
2. **Tab State Navigation**: Users can switch between tabs even when disconnected. Each tab displays its own cached metadata from local SQLite tables.
3. **Sync Feedback**: Network failures during background or swipe-to-refresh syncs show a transient network error message but do not block access to existing cached metadata.

---

## 6. Sub-Specifications & Sections

To see detailed rules, endpoints, and behaviors for each library tab, refer to their individual specifications:
- **[Books Tab Specification](library_books_spec.md)**: Details book grids, paging, badges, and cover loading.
- **[Series Tab Specification](library_series_spec.md)**: Details series list structures and series details navigation.
- **[Collections Tab Specification](library_collections_spec.md)**: Details custom collection list structures and image collages.
- **[Authors Tab Specification](library_authors_spec.md)**: Details alphabetic index lists and author detail navigations.
- **[Playlists Tab Specification](library_playlists_spec.md)**: Details user custom playlists and playback entry points.

For detail view specifications of books:
- **[Book Detail Specification](book_detail_spec.md)**: Details individual book metadata and downloading files.
