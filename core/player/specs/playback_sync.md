# Feature Specification: Playback Tracking & Progress Sync

This document defines the specific behaviors, rules, and network payload formats for media playback tracking and synchronization, extending the high-level goals in [project_spec.md](../../../specs/project_spec.md).

---

## 1. Feature Context & Constraints

The client must report playback progress to the self-hosted server to maintain synchronicity with other clients. The server supports two synchronization methods:
1. **Active Session Sync**: Tracks active listening durations inside a registered playback session.
2. **Static Progress Sync**: Direct progress state overrides (progress percentage and current time).

---

## 2. Specific Behavioral Rules

### A. Session Registration & Initiation
1. **Playback Session Register**: Before beginning audio playback, the client must request a playback session from the server via `POST /api/items/{bookId}/play`.
2. **Device Info Payload**: The request must pass a JSON payload with device information:
   - `clientName`: Hardcoded to `"Skald Android"`.
   - `deviceId`: A unique string identifying the user's device.
   - `deviceName`: The user-facing name of the device (e.g., `"Pixel 8"`).
3. **Session ID Preservation**: The client must store the returned session ID to use for subsequent active session sync requests.

### B. Periodic Session Syncing
1. **Synchronization Endpoint**: During active playback, progress must be pushed periodically to the server via `POST /api/session/{sessionId}/sync`.
2. **Sync Payload**: The sync request must supply:
   - `timeListened`: The exact amount of time (in seconds) the user has spent listening since the last sync.
   - `currentTime`: The current playback timestamp (in seconds) within the audiobook.

### C. Static Progress Syncing
1. **Synchronization Endpoint**: When finishing a book or performing major seek events, the client must sync the progress directly using `POST /api/me/progress`.
2. **Sync Payload**: The payload must be formatted as a JSON array containing progress update objects:
   - `libraryItemId`: The ID of the book item.
   - `currentTime`: The current media timestamp (in seconds).
   - `progress`: A float value from `0.0f` to `1.0f` indicating completion progress.
   - `isFinished`: A boolean flag representing whether the item has been marked finished.

### D. Offline Resilience
1. **Local Stash**: If progress synchronization fails due to offline states, the progress must be stored in the local Room database (`PlaybackProgressEntity`) to prevent data loss.
2. **Sync Re-attempt**: The client should attempt to push cached offline progress to the server once network connectivity is re-established.

### E. Global Progress Synchronization (Periodic/Background Sync)

To ensure media playback progress stays in sync with the Audiobookshelf server automatically and transparently across multiple devices without manual user action, the client must perform periodic global progress synchronization.

1. **Synchronization Endpoint**: The client fetches the user profile data via `GET /api/me`.
2. **ETag-Based Conditional Requests (Prevent Server Overload)**:
   - On each sync request, the client must send an `If-None-Match` header with the cached ETag from `PreferencesManager` (stored under the key `etag_user`).
   - If the server responds with `304 Not Modified`, skip the sync processing (no changes to database).
   - If the server responds with `200 OK`, update the `etag_user` entry in `PreferencesManager` and proceed with the update strategy.
3. **Synchronization Trigger Points**:
   - **Application Startup**: Trigger a background sync during app initialization.
   - **App Foregrounding (Resume)**: Trigger a background sync when the application returns to the foreground.
   - **Manual Library Refresh**: When a user performs a swipe-to-refresh/manual refresh on the library screen, a global progress sync must be triggered alongside the library sync.
4. **Resolution Strategy**:
   For each item in the server's `mediaProgress` list:
   - Check if a local `PlaybackProgressEntity` exists for the given `bookId` (or `bookId-episodeId`).
   - **Server is Newer**: If the server progress `lastUpdate` timestamp is greater than the local progress `lastUpdated` timestamp, update the local database cache with the server's `currentTime`, `progress`, and `isFinished`.
   - **Local is Newer**: If the local progress `lastUpdated` timestamp is greater than the server's progress `lastUpdate` timestamp, push the local progress update to the server via the static progress update endpoint.
   - **Missing Locally**: If the item exists on the server but has no record in the local database, insert a new local `PlaybackProgressEntity` with the server's values.
5. **Offline Sessions Bundling**:
   Immediately after resolving the media progress fetched via `GET /api/me`, the sync routine must check the local database for any queued offline playback sessions (`PlaybackSessionEntity`). If found:
   - Sync them in a batch request to `POST /api/session/local-all`.
   - Upon successful server response, clear those synced sessions from the local database.

