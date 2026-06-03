# Core Specification: Client Relational Caching

This document defines the relational cache storage (Room Database) schema and behavior for the Skald application.

---

## 1. Storage Architecture

The client uses a Relational SQLite Database (`AppDatabase` via Room) for structured entities like audiobook metadata, audio files download state, and historical playback progress.

Implemented in `AppDatabase.kt` with database filename `skald_db`. Currently defined under Schema Version `1`.

---

## 2. Room Database Schema

### Table: `books`
Caches the list of audiobooks retrieved from the server, alongside download metadata.

| Column Name | DB Type | Data Type | Primary Key | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`id`** | TEXT | String | Yes | Unique ID of the book. |
| **`title`** | TEXT | String | No | Title of the audiobook. |
| **`author`** | TEXT | String | No | Book author name. |
| **`narrator`** | TEXT | String | No | Narrator name. |
| **`description`** | TEXT | String | No | Synopsis or description. |
| **`duration`** | REAL | Double | No | Total playback duration (in seconds). |
| **`coverPath`** | TEXT | String? | No | Path to locally cached cover artwork, if available. |
| **`isDownloaded`** | INTEGER | Boolean | No | Indicates if all audio files are saved locally. |
| **`audioFiles`** | TEXT | JSON String | No | List of `LocalAudioFile` objects (see below) mapped via `Converters`. |
| **`chapters`** | TEXT | JSON String | No | List of `LocalChapter` objects (see below) mapped via `Converters`. |

#### Nested Type: `LocalAudioFile` (Serialized JSON)
- `index` (Int): Sequential file order.
- `ino` (String): Unique inode file ID on the server.
- `duration` (Double): File length in seconds.
- `mimeType` (String): File codec type (e.g. `audio/mpeg`).
- `filename` (String): Remote filename on the server.
- `size` (Long): File weight in bytes.
- `localPath` (String?): Absolute disk path to downloaded copy, or null.
- `downloadStatus` (String): State string (`"NOT_DOWNLOADED"`, `"DOWNLOADING"`, or `"COMPLETED"`).

#### Nested Type: `LocalChapter` (Serialized JSON)
- `start` (Double): Start timestamp within the absolute audiobook duration (in seconds).
- `end` (Double): End timestamp within the absolute audiobook duration (in seconds).
- `title` (String): Display name of the chapter.

---

### Table: `playback_progress`
Maintains the latest local playback positions for each audiobook. Crucial for both offline resume operations and active server synchronization.

| Column Name | DB Type | Data Type | Primary Key | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`bookId`** | TEXT | String | Yes | ID of the book. |
| **`currentTime`** | REAL | Double | No | Current playback timestamp in seconds. |
| **`progress`** | REAL | Float | No | Percentage of completion (from `0.0f` to `1.0f`). |
| **`isFinished`** | INTEGER | Boolean | No | Mark if the book was finished. |
| **`lastUpdated`** | INTEGER | Long | No | Timestamp of the last local update (epoch milliseconds). |

---

## 3. Reset & Clear Rules

When `clearLocalData()` is triggered:
- **Room Database**:
  - The `books` table is cleared. Any local metadata is wiped, but actual physical files in storage remain subject to file cleanup processes.
  - The `playback_progress` table is cleared to ensure a clean slate for the next authenticated user.
