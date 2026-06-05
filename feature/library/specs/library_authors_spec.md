# Feature Specification: Library - Authors Tab

This document defines the requirements, behavioral rules, and caching patterns for the **Authors Tab** within the library browsing screen. It is a sub-specification of the main [Library Specification](library_spec.md).

---

## 1. Context & Dependencies

The Authors Tab displays a list of authors associated with media inside the selected library. It supports scanning the list alphabetically, displaying author avatars, and drilling down to view books written by a specific author.

It depends on:
- **`:core:database`**: Local Room database caching using `authors` and `author_books` relationship tables.
- **`:domain`**: Use cases `GetAuthorsUseCase`, `SyncLibraryAuthorsUseCase`, and `GetAuthorDetailsUseCase`.
- **`:feature:library`**: The `AuthorsTabContent` list UI and `AuthorDetailScreen` views.

---

## 2. Data Integration & Sync

### A. API Endpoints
- **List Library Authors**: `GET /api/libraries/{libraryId}/authors`
  - *Returns*: An array of author profiles including ID, name, description, book count, and image path.
- **Get Author Details / Bio**: `GET /api/authors/{authorId}`

### B. Sync Strategy & Caching
1. **Trigger**: Synchronizes when:
   - The Authors Tab is loaded (stale cache check).
   - Swipe-to-refresh is triggered on any library tab (triggers global force sync).
2. **ETag Check & Force Sync**: Uses standard HTTP ETag checks via `If-None-Match`. If `304 Not Modified` is returned, the local cache is retained. A global swipe-to-refresh bypasses the ETag check (omits `If-None-Match`) to force update all authors data.
3. **Database Schema**:
   - `AuthorEntity`: `id` (String, PK), `libraryId` (String), `name` (String), `description` (String?), `imagePath` (String?), `bookCount` (Int), `etag` (String?).

---

## 3. UI & Interaction Rules

### A. List Layout & Design
Unlike books and series, authors are displayed in a highly readable **vertical list layout** rather than a grid:
- **Row Content**:
  - **Author Avatar**: A circular thumbnail image (`48.dp` diameter).
    - If `imagePath` is available from the server, load the avatar using authorization headers.
    - If no image exists, draw a circular background using a color generated from the author name's hash (utilizing brand colors like `ElectricPurple`, `CyanAccent`, or `SoftPink`) and render the author's first initial in bold white text.
  - **Author Name**: Styled using `Body Large` bold text.
  - **Book Count**: Subtitle text (e.g. `"6 books"`).
- **A-Z Fast Scroller**: An optional vertical alphabet strip (A-Z) along the right edge of the list. Dragging or tapping letters jumps the scroll position directly to authors starting with that letter.

### B. Navigation (Author Detail Screen)
- Tapping an author item navigates to the **Author Detail Screen**.
- **Author Detail UI**:
  - Header with large avatar, name, and biographical description (collapsible/expandable if long).
  - Grid layout of all books written by this author, using the standard `BookCard` component.
  - Tapping a book card navigates to the **Book Detail Screen**.

### C. Search & Sorting
- **Search**: Scopes search queries to author names (e.g. "Stephen King").
- **Sorting**:
  - `NAME_ASC` / `NAME_DESC` (default: `NAME_ASC`): Alphabetical by name.
  - `BOOKS_COUNT_DESC`: Rank authors by how many of their books are in the library.

---

## 4. UI States & Offline Resilience

- **Scrollable Empty State**: If no authors are found in the active library, the Authors tab renders a scrollable empty state (via `LibraryEmptyState` supporting vertical scroll). This ensures that swipe-to-refresh gestures propagate properly, and renders a manual "Sync Now" button to trigger a database refresh.
