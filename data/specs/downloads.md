# Feature Specification: Offline Download Manager

This document defines the specific behaviors, rules, and local directory configurations for the offline media download manager, extending the high-level goals in [project_spec.md](../../specs/project_spec.md).

---

## 1. Feature Context & Constraints

The client must download audio files from the server to enable offline listening. Because audiobook files are typically large, the download process must stream chunks, report progress, and support graceful recovery.

---

## 2. Specific Behavioral Rules

### A. Download Streaming & Processing
1. **API Endpoint**: Audio files must be downloaded via `GET /api/items/{bookId}/file/{ino}/download`.
2. **Chunk-by-Chunk Write**: Stream the file contents in chunks (e.g. 8KB buffers) to prevent memory overload.
3. **Local File Mapping**: Save files under the app-specific external files directory:
   `downloads/{bookId}/{ino}.{extension}`
   - *Example: `downloads/book_123/ino_456.mp3`*

### B. Progress Monitoring
1. **Flow Emission**: Expose a `Flow<Float>` that periodically emits a float value between `0.0f` (0%) and `1.0f` (100%) indicating current track download progress.
2. **Progress Calculation**: Calculated as:
   `Bytes Written / Total Bytes Expected`

### C. Error Recovery & Interruption Handling
1. **Interrupted Downloads**: If a network drop occurs during download, the client must abort the stream, clean up the incomplete/corrupt local file, and report a failure state to the user.
2. **Disk Space Check**: Before initiating a download, check available device storage. Throw an exception if local space is insufficient for the total expected file size.

### D. Background Continuity
1. **Background Resiliency**: Once started, downloads must continue in the background even if the screen is off or the application is not actively in use (minimized or backgrounded).
2. **Platform Integration**: Utilize Android's system `DownloadManager` service to delegate download scheduling, notifications, and background download persistence.

---

## 3. Download Maintenance & Relinking

To ensure the local relational database (`books` table) remains fully reconciled with the physical files stored in app-specific external storage, a background maintenance task must run regularly to perform disk-to-database reconciliation.

### A. Execution Scenarios
The reconciliation process must run:
1. **On Application Startup**: Triggered in `ABSApplication.onCreate()` inside a background coroutine scope.
2. **On Periodic Sync**: Triggered automatically when the periodic library sync runs (aligned with `library_sync_interval_hours`).

### B. Reconciliation and Relinking Logic
1. **Directory Traversal**:
   - The scanner resolves the downloads directory: `context.getExternalFilesDir(null)/downloads`.
   - It lists all subdirectories. Each subdirectory name represents a `bookId`.
2. **Entity Reconciliation**:
   - For each subdirectory (`bookId`):
     - Query the local database for a matching `BookEntity` by ID.
     - **If a corresponding book is found**:
       - **Detail Enrichment**: If the book's `audioFiles` array is empty, the system must trigger a fetch of the book details from the server to enrich the database record with the files list and inodes before scanning.
       - Traverse files within the `downloads/{bookId}` directory.
       - Match found files (named as `{ino}.{extension}`) against the list of `LocalAudioFile` entries stored in the book's `audioFiles` array.
       - If a matching file exists on disk:
         - Update its `downloadStatus` to `"COMPLETED"`.
         - Update its `localPath` to the file's absolute path on disk.
       - If a file in the database is marked as downloaded/completed but is missing on disk:
         - Reset its `downloadStatus` to `"NOT_DOWNLOADED"` and `localPath` to `null`.
       - Recalculate whether all audio files of the book are `"COMPLETED"`. Set `isDownloaded` to `true` if all are complete, else `false`.
       - If any properties of the `BookEntity` changed, save/upsert the updated entity back to the database.
     - **If no corresponding book is found**:
       - Do not delete the directory immediately.
       - Mark the directory as an **orphaned download folder** to be presented to the user for settings-based cleanup.

