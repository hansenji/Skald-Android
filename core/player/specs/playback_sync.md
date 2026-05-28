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
   - `clientName`: Hardcoded to `"ABS Client Android"`.
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
