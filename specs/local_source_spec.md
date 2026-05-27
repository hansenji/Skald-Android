# Local Source Specification: Client Data Persistence

This document serves as the unified specification tracking all values stored locally on the client device across both key-value storage (SharedPreferences) and relational cache storage (Room Database).

---

## 1. Storage Architectures

The client uses two primary data storage strategies:
1. **Key-Value Preferences (`PreferencesManager`)**: For simple flags, settings, session states, and user configurations.
2. **Relational SQLite Database (`AppDatabase` via Room)**: For structured entities like audiobook metadata, audio files download state, and historical playback progress.

---

## 2. Key-Value Storage (SharedPreferences)

Managed via `PreferencesManager` under the filename `abs_client_prefs`. For detailed behavioral rules, see [settings_spec.md](file:///home/hansenji/.gemini/antigravity/worktrees/abs-client-app/android-auto-spec-init/specs/settings_spec.md).

### Summary of Keys:
- `server_url` (String?): Base URL of the connection instance.
- `username` (String?): Authenticated user's account name.
- `token` (String?): Session authorization token.
- `library_id` (String?): Current audiobook library selected.
- `filter_read_status` (String?): Read filter status state.
- `filter_downloaded_only` (Boolean): Offline list filter flag.
- `sort_option` (String?): Library list sorting criteria.
- `skip_forward_duration` (Int): Skip-forward length in seconds.
- `skip_backward_duration` (Int): Skip-backward length in seconds.
- `playback_speed` (Float): Preserved playback speed.

---

## 3. Relational Caching (Room Database)

Implemented in `AppDatabase.kt` with database filename `abs_client_db`. Currently defined under Schema Version `1`.

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

## 4. Reset & Clear Rules

When `clearLocalData()` is triggered:
- **SharedPreferences**: Entirely cleared (except for user control preferences like skip durations and speed, which are preserved).
- **Room Database**:
  - The `books` table is cleared. Any local metadata is wiped, but actual physical files in storage remain subject to file cleanup processes.
  - The `playback_progress` table is cleared to ensure a clean slate for the next authenticated user.
