# Feature Specification: Mobile Player Feature

This document defines the requirements, user interface behaviors, and business logic for the mobile audiobook player feature, extending the core player engine capabilities.

---

## 1. Feature Context & Constraints

The `:feature:player` module is responsible for the user-facing mobile playback screen. It coordinates with `:core:player`'s `PlayerManager` to start, pause, seek, and format audiobook playback.

The mobile player UI must support cover art display, a progress scrubber bar, chapter selections, speed controls, sleep timers, and quick skip actions.

---

## 2. Specific Behavioral Rules

### A. Quick Skip Actions (Rewind / Fast Forward)
1. **Configurable Durations**: Skip durations are not hardcoded. The application must retrieve the skip values dynamically from the `SettingsRepository`:
   - **Skip Forward**: Seek forward by `getSkipForwardDuration()` seconds (default: 30s).
   - **Skip Backward**: Seek backward by `getSkipBackwardDuration()` seconds (default: 10s).
2. **Constraint Boundary**: Seeking must not exceed the audiobook boundaries:
   - Fast-forwarding past the total duration should pause the book at the very end.
   - Rewinding past 0.0s must reset the playback position to 0.0s.

### B. Playback Speed Control
1. **Speed Range & Increments**: The user can adjust the playback speed multiplier. The speed cycles from `0.5x` to `3.0x` in `0.25x` steps (e.g., `1.0x -> 1.25x -> 1.5x -> 1.75x -> 2.0x -> 2.25x -> 2.5x -> 2.75x -> 3.0x -> 0.5x`).
2. **Speed Persistence**:
   - The active playback speed must be saved immediately to the `SettingsRepository` via `savePlaybackSpeed(Float)`.
   - The active player speed must be applied to `PlayerManager` and the underlying `ExoPlayer` instance.
   - When a new book begins or playback resumes, the player must initialize using the last persisted speed value.

### C. Sleep Timer
1. **Trigger Options**: The sleep timer can be set to:
   - Off
   - Fixed durations (e.g., 15 minutes, 30 minutes, 45 minutes, 60 minutes)
   - End of the active chapter (triggers pause when `currentChapter` ends)
2. **UI Updates**: The player screen must display a real-time countdown of the remaining milliseconds/seconds when active.

### D. Chapter Skip Actions
1. **Skip to Next Chapter**:
   - Seeks to the start position of the subsequent chapter in the book's chapter list.
   - If the user is currently on the last chapter, the skip action should do nothing or seek to the end of the book.
2. **Skip to Previous Chapter**:
   - **Chapter Restart Threshold**: If the current chapter has been playing for more than `5` seconds, triggering previous chapter skip must restart the current chapter (seek to the current chapter's start position).
   - **Jump to Prior Chapter**: If the current chapter has been playing for `5` seconds or less, seek to the start position of the previous chapter.
   - If there is no previous chapter, seek to the start of the book (0.0s).

---

## 3. UI Component Specifications

- **Scrubber Bar**: Display current absolute position (formatted as `HH:MM:SS`) and total remaining/duration. The slider handles drag-to-seek, invoking `seekTo()` upon release.
- **Chapters Sheet**: A bottom sheet lists all chapters with titles and start/end times. Selecting a chapter seeks directly to the chapter's start position.
- **Speed Selector**: Tapping the speed label opens a dialog or cycles through the speed options, showing the active speed prominently.
- **Settings Hook**: Quick settings inside the player screen let the user change the default skip durations (e.g., 10s, 30s, 60s) via dropdowns, persisting the choices to the preferences store.
