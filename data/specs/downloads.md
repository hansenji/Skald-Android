# Feature Specification: Offline Download Manager

This document defines the specific behaviors, rules, and local directory configurations for the offline media download manager, extending the high-level goals in [app_spec.md](file:///home/hansenji/src/abs-client-app/specs/app_spec.md).

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
