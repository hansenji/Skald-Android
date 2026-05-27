# Core Specification: Media Session Callbacks & Control

This document defines the behavioral specifications, custom session commands, and platform integrations for the Media3 playback session callback engine.

---

## 1. Context & Architecture

To avoid circular dependencies between feature-level browsing components and the core playback service, all playback actions are decoupled and handled in the `:core:player` module. 

- **`AudiobookSessionCallback`**: Extends Media3's `MediaSession.Callback` (or `MediaLibrarySession.Callback` base functions).
- **Service Integration**: The callback is instantiated and attached to the `MediaSession` inside `AudiobookPlayerService`.
- **Target Interface**: Controls map directly to the `PlayerManager` API which encapsulates standard `ExoPlayer` controls.
- **Settings Hook**: Custom command durations and speeds are read from and persisted to the `SettingsRepository`.

---

## 2. Playback Command Specifications

### A. Standard Media3 Operations
- **Play / Pause**: Invokes `PlayerManager.play()` and `PlayerManager.pause()` respectively.
- **Seek To**: Receives a `positionMs` value and seeks using `PlayerManager.seekTo(positionMs)`.

### B. Custom Action Commands
The callback intercepts and executes the following custom Media3 commands (`SessionCommand`):

#### 1. Skip Forward (`COMMAND_SKIP_FORWARD`)
- **Action**: Seeks forward by the user's preferred duration.
- **Rules**:
  1. Retrieve duration in seconds via `SettingsRepository.getSkipForwardDuration()` (defaults to `30`).
  2. Compute new position: `currentPosition + (duration * 1000)`.
  3. If new position exceeds the total audiobook duration, seek to the end of the book and pause playback.

#### 2. Skip Backward (`COMMAND_SKIP_BACKWARD`)
- **Action**: Seeks backward by the user's preferred duration.
- **Rules**:
  1. Retrieve duration in seconds via `SettingsRepository.getSkipBackwardDuration()` (defaults to `10`).
  2. Compute new position: `currentPosition - (duration * 1000)`.
  3. If new position is less than `0`, seek to `0` and do not change play/pause state.

#### 3. Cycle Playback Speed (`COMMAND_CYCLE_SPEED`)
- **Action**: Cycles the playback speed.
- **Rules**:
  1. Cycle current speed among: `0.5x -> 0.75x -> 1.0x -> 1.25x -> 1.5x -> 1.75x -> 2.0x -> 2.25x -> 2.5x -> 2.75x -> 3.0x -> 0.5x`.
  2. Update the speed on the active player engine immediately.
  3. Save the chosen speed to preferences via `SettingsRepository.savePlaybackSpeed(speed: Float)`.

#### 4. Skip to Next Chapter (`COMMAND_SKIP_TO_NEXT_CHAPTER`)
- **Action**: Skips to the subsequent chapter.
- **Rules**:
  1. Determine current absolute position and query active book chapter list.
  2. If a subsequent chapter exists, seek to its start timestamp.
  3. If on the final chapter, do nothing or seek to the end of the book.

#### 5. Skip to Previous Chapter (`COMMAND_SKIP_TO_PREVIOUS_CHAPTER`)
- **Action**: Restarts or jumps to the preceding chapter.
- **Rules**:
  1. Determine current absolute position and active chapter start timestamp.
  2. **Chapter Restart Threshold**: If the current chapter has been playing for more than `5` seconds, seek to the start of the current chapter.
  3. **Jump to Prior Chapter**: If the current chapter has been playing for `5` seconds or less, seek to the start of the previous chapter.
  4. If there is no previous chapter, seek to the beginning of the book (`0.0s`).

---

## 3. Dependency Injection Configuration

The core player module declares Koin bindings for the session callback:

```kotlin
val corePlayerModule = module {
    // Default base implementation for lock screen and standard systems
    single<MediaSession.Callback> {
        AudiobookSessionCallback(
            playerManager = get(),
            settingsRepository = get()
        )
    }
}
```
