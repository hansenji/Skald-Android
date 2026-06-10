# Feature Specification: Library - Collections Tab

This document defines the requirements, behavioral rules, and caching patterns for the **Collections Tab** within the library browsing screen. It is a sub-specification of the main [Library Specification](library_spec.md).

---

## 1. Context & Dependencies

The Collections Tab enables users to browse server-defined custom groups of audiobooks (e.g. "Summer Reads", "Norse Mythology"). It reads collection lists and structures from the database, renders image layouts, and links to collection detail views.

It depends on:
- **`:core:database`**: Local Room database caching using `collections` and `collection_books` mapping tables.
- **`:domain`**: Use cases `GetCollectionsUseCase` and `SyncLibraryCollectionsUseCase`.
- **`:feature:library`**: The `CollectionsTabContent` and `CollectionDetailScreen` layout views.

---

## 2. Data Integration & Sync

### A. API Endpoints
- **List Library Collections**: `GET /api/libraries/{libraryId}/collections`
  - *Returns*: An array of collections containing collection ID, name, description, list of associated book IDs, and cover information.
- **Get Collection Details**: `GET /api/collections/{collectionId}` (to load additional details or verify item association).

### B. Sync Strategy & Caching
1. **Trigger**: Synchronizes when:
   - The Collections Tab is initialized (stale cache check).
   - Swipe-to-refresh is executed on any library tab (triggers global force sync).
2. **ETag Check & Force Sync**: Uses standard HTTP ETag checks via `If-None-Match`. If `304 Not Modified` is returned, the local cache is retained. A global swipe-to-refresh bypasses the ETag check (omits `If-None-Match`) to force update all collections data.
3. **Database Schema**:
   - `CollectionEntity`: `id` (String, PK), `libraryId` (String), `name` (String), `description` (String?), `bookIds` (List of Strings, stored as JSON or in an association table), `lastUpdated` (Long).

---

## 3. UI & Interaction Rules

### A. Grid Layout
- Collections are displayed in an adaptive grid.
- **Card Content**:
  - **Cover Collage**: A `1:1` square aspect ratio placeholder displaying a **2x2 grid collage** composed of the cover art of the first four books in the collection. If the collection has fewer than four books, the available covers are stretched or filled with fallback gradients.
  - **Collection Title**: Bold typography title (max 2 lines).
  - **Book Count**: Small subtitle text (e.g., `"12 books"`).

### B. Navigation (Collection Detail Screen)
- Tapping a Collection Card navigates to the **Collection Detail Screen**.
- **Collection Detail UI**:
  - Header showcasing the collection name, description, and total duration of all books in the collection combined.
  - Grid or list of books inside the collection.
  - **Ordering**: Books must follow the specific order defined by the collection on the server (custom sort order), rather than standard alphabetical order.
  - Tapping a book card navigates to the **Book Detail Screen**.
  - **Contextual Action Menu**: Long-pressing any book card in the grid/list or tapping its contextual overflow icon opens the `ItemMoreMenuBottomSheet` as defined in [book_detail_spec.md](book_detail_spec.md#e-contextual-action-menu).

### C. Search & Sorting
- **Search**: Scopes search input to collection names.
- **Sorting**:
  - `NAME_ASC` / `NAME_DESC`: Alphabetical by collection name.
  - `BOOKS_COUNT_DESC`: By collection size.
  - `LAST_MODIFIED`: By server's collection update timestamp.

---

## 4. UI States & Offline Resilience

- **Scrollable Empty State**: If no collections are found in the active library, the Collections tab renders a scrollable empty state (via `LibraryEmptyState` supporting vertical scroll). This ensures that swipe-to-refresh gestures propagate properly, and renders a manual "Sync Now" button to trigger a database refresh.
