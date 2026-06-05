# Skald - Settings Screen UI & Integration Specification

This document serves as the high-level and detailed specification for the **Settings Screen** feature in the Skald application. It outlines the UI layout, data sources, user preferences, account management, and synchronization controls, combining constraints and behaviors from other related specifications.

---

## 1. Feature Context & Constraints

The Settings Screen is the central configuration panel for the application. It is accessible as a top-level destination from the main navigation bar/rail, as specified in [project_spec.md](file:///home/hansenji/src/abs-client-app/specs/project_spec.md).

### Key Responsibilities:
1. **Account Management**: Display active connection details and provide secure session termination.
2. **Playback Customization**: Let users adjust skip durations, speed parameters, and interruption behavior.
3. **Sync Configuration**: Allow users to adjust library and progress synchronization intervals, see cache freshness, and trigger manual updates.
4. **Data Cache Management**: Provide visibility into downloaded media sizes and a mechanism to clear local caches.

### Architectural Dependencies:
- **Presentation**: `:feature:settings` (`SettingsScreen`, `SettingsViewModel`).
- **Domain**: `:domain` (`LogoutUseCase`, `GetPreferencesUseCase`, `UpdatePreferencesUseCase`).
- **Data/Preferences**: `:core:preferences` (`PreferencesManager`), storing state in `skald_prefs` and `secure_tokens.pb` as defined in [settings_spec.md](file:///home/hansenji/src/abs-client-app/core/preferences/specs/settings_spec.md).
- **Authentication**: `:core:network` / `:feature:login` for credentials cleanup, complying with [auth_spec.md](file:///home/hansenji/src/abs-client-app/specs/auth_spec.md).

---

## 2. Settings Screen Layout & UI Components

The Settings Screen is built using Jetpack Compose and adheres to **Material Design 3 (M3)** styling guidelines. It is styled with the application's unified dark-mode theme, providing a premium visual appearance with structured category cards and distinct lists.

The UI is organized vertically into grouped categories using `Card` containers or `HorizontalDivider` dividers:

```
+-------------------------------------------------+
| Settings                                        |  <-- M3 TopAppBar
+-------------------------------------------------+
|                                                 |
|  [ ACCOUNT ]                                    |  <-- Group Category Header
|  +-------------------------------------------+  |
|  | Server: https://audiobooks.example.com    |  |  <-- Read-only label with copy action
|  | User: audio_listener_123                  |  |  <-- Read-only label
|  | Active Library: Audiobooks                |  |  <-- Library indicator
|  |                                           |  |
|  | [ LOG OUT ] (Red button / Error Container)|  |  <-- Clear session & cache confirmation
|  +-------------------------------------------+  |
|                                                 |
|  [ PLAYBACK PREFERENCES ]                       |
|  +-------------------------------------------+  |
|  | Skip Forward Duration            [ 30s > ]|  |  <-- Dialog selection list
|  | Skip Backward Duration           [ 10s > ]|  |  <-- Dialog selection list
|  | Default Playback Speed           [ 1.0x >]|  |  <-- Slider or list picker
|  | Go Back on Interrupt             [ Switch]|  |  <-- 5-second rewind on pause/call
|  +-------------------------------------------+  |
|                                                 |
|  [ SYNC & STORAGE ]                             |
|  +-------------------------------------------+  |
|  | Periodic Sync Interval          [ 24h > ] |  |  <-- Dropdown picker
|  | Last Synced: 2 hours ago                  |  |  <-- Relative or absolute time indicator
|  | [ Sync Now ] (Button)                     |  |  <-- Force-sync network pull
|  |                                           |  |
|  | Cached Media Size: 2.4 GB                 |  |  <-- Computed offline track size
|  | [ Clear Cache ] (Outlined Button)         |  |  <-- Delete downloaded files & DB cache
|  +-------------------------------------------+  |
|                                                 |
+-------------------------------------------------+
```

### A. Account Section
- **Server URL**: Displays the active normalized server address (`server_url` from `PreferencesManager`). Includes a subtle icon to copy the URL to the clipboard.
- **Logged User**: Displays the active username (`username` from `PreferencesManager`).
- **Active Library**: Displays the name of the currently active library.
- **Log Out Button**:
  - Rendered as a prominent Material 3 `Button` with `MaterialTheme.colorScheme.error` container.
  - Tapping triggers a **Confirmation Dialog**:
    - *Title*: "Log Out?"
    - *Message*: "Are you sure you want to log out? Unsynced listening progress may be lost."
    - *Confirm*: "Log Out" (Destructive red)
    - *Dismiss*: "Cancel"
  - **Logout Behavior**: Resolves `LogoutUseCase`. It clears access/refresh tokens in `secure_tokens.pb`, server configuration details, and cached sync ETags. Resets filters to defaults but preserves playback control durations (as specified in [auth_spec.md](file:///home/hansenji/src/abs-client-app/specs/auth_spec.md) and [settings_spec.md](file:///home/hansenji/src/abs-client-app/core/preferences/specs/settings_spec.md)).

### B. Playback Preferences Section
- **Skip Forward Duration**:
  - Key: `skip_forward_duration` (Default: `30` seconds)
  - Layout: Clicking displays a single-choice list dialog with options: `10s`, `15s`, `30s`, `45s`, `60s`.
  - Impact: Directly alters the jump size of the skip-forward action in both the phone player UI and custom Android Auto actions, as detailed in [session_spec.md](file:///home/hansenji/src/abs-client-app/core/player/specs/session_spec.md).
- **Skip Backward Duration**:
  - Key: `skip_backward_duration` (Default: `10` seconds)
  - Layout: Clicking displays a single-choice list dialog with options: `5s`, `10s`, `15s`, `30s`, `45s`, `60s`.
  - Impact: Alters the skip-backward distance in the player and car dashboards.
- **Default Playback Speed**:
  - Key: `playback_speed` (Default: `1.0f`)
  - Layout: A horizontal slider or dropdown selection cycling from `0.5x` to `2.0x` in `0.25x` steps.
  - Impact: Sets the initial speed applied to the ExoPlayer controller on launch, and is kept in sync with the car's cycling speed commands.
- **Go Back on Interrupt**:
  - Key: `go_back_on_interrupt` (Boolean, Default: `true`)
  - Layout: An M3 `Switch` toggle item.
  - Behavior: When enabled, resuming playback after an interruption (e.g., pausing, phone call, transient audio focus loss, or background service restarts) will automatically seek the player backward by **5 seconds** to restore listening context. If the playback position is less than 5 seconds, it seeks to `0`.

### C. Sync & Storage Section
- **Periodic Sync Interval**:
  - Key: `library_sync_interval_hours` (Default: `24` hours)
  - Layout: Dropdown or list dialog picker offering: `Disabled (0h)`, `1 hour`, `6 hours`, `12 hours`, `24 hours`, `48 hours`, `72 hours`.
  - Behavior: Schedules or cancels background tasks (via Android `WorkManager` or internal alarms) to periodically query the library items and progress, as specified in [library_spec.md](file:///home/hansenji/src/abs-client-app/feature/library/specs/library_spec.md).
- **Last Synced Timestamp**:
  - Key: `library_last_sync_timestamp`
  - Layout: Plain status text showing the relative elapsed time since the last successful sync (e.g., "Last synced: 35 minutes ago" or "Never synced").
- **Sync Now Button**:
  - Layout: Small filled or elevated button.
  - Behavior: Bypasses ETag caching checks (omits `If-None-Match` header) to trigger an immediate full library pull and global progress synchronization (`GET /api/libraries/{id}/items` and `GET /api/me`), updating local tables. Disable button with a spinner during active syncing.
- **Storage Metrics & Clear Cache**:
  - Displays the total size occupied by offline downloaded tracks (sum of files under `downloads/` directory) and cached database records.
  - **Clear Cache Button**: Outlined button that deletes downloaded audiobook tracks, clears search indices, and wipes DB caches. Retains active login session credentials.

---

## 3. Data Integration & Preferences Map

All configurations adjusted on this screen map directly to fields within the persistent storage configuration:

| UI Control | Preference Key | Data Type | Default Value | Target Persistence API |
| :--- | :--- | :--- | :--- | :--- |
| **Server URL** | `server_url` | String | `null` | `PreferencesManager.getServerUrl()` |
| **Username** | `username` | String | `null` | `PreferencesManager.getUsername()` |
| **User ID** | `user_id` | String | `null` | `PreferencesManager.getUserId()` |
| **Skip Forward** | `skip_forward_duration` | Integer | `30` | `PreferencesManager.saveSkipForwardDuration()` |
| **Skip Backward** | `skip_backward_duration` | Integer | `10` | `PreferencesManager.saveSkipBackwardDuration()` |
| **Playback Speed** | `playback_speed` | Float | `1.0f` | `PreferencesManager.savePlaybackSpeed()` |
| **Go Back on Interrupt** | `go_back_on_interrupt` | Boolean | `true` | `PreferencesManager.saveGoBackOnInterrupt()` (to be added) |
| **Sync Interval** | `library_sync_interval_hours` | Integer | `24` | `PreferencesManager.saveLibrarySyncIntervalHours()` |
| **Last Sync** | `library_last_sync_timestamp` | Long | `0L` | `PreferencesManager.getLibraryLastSyncTimestamp()` |

---

## 4. Special Behavioral & Lifecycle Rules

### A. Re-authentication & User Verification Policy
If a session expires (detected when a token refresh fails or the refresh token expires), the Settings Screen or root navigators prompt the user to log in again.
- **Same User**: If the new credentials match the previous `user_id` / `username`, all local caches, search history, downloaded tracks, and settings are preserved.
- **Different User**: If a different user account logs in, the app must initiate a **Full Wipe**:
  1. Delete all rows in local database tables (`books`, `libraries`, `playback_progress`, `home_shelves`, `home_shelf_items`).
  2. Recursively delete the entire `downloads/` directory containing stored audio files.
  3. Reset all query filters (`filter_read_status`, `filter_downloaded_only`, `sort_option`) to defaults.

### B. Offline State Behavior
When the device lacks internet connectivity:
- The Settings Screen must show a prominent "Offline Mode" badge or banner.
- Account information is displayed using cached preferences details.
- The **Sync Now** action button must be disabled, and a tooltip or toast must indicate "Connection required to sync".
- Playback customization preferences (Skip Forward, Skip Backward, Playback Speed, Go Back on Interrupt) remain fully editable and local updates are saved immediately.

### C. Periodic Sync Rescheduling
When the **Periodic Sync Interval** is changed by the user:
1. If set to `0` (Disabled), cancel any scheduled background synchronization workers.
2. If set to a positive hour count (`1` to `72`), cancel the existing worker and schedule a new repeating work request with the new interval parameter.

---

## 5. Verification & Quality Plan

To verify that the settings configurations behave correctly and update adjacent features without regressions, the following tests are defined:

### Automated Tests
1. **Preference Persistence Unit Test**:
   - Verify that altering settings in the UI correctly calls the corresponding setter on `PreferencesManager`.
   - Verify that the defaults are properly returned when preference values are empty.
2. **Logout Action Integrity Test**:
   - Assert that invoking logout correctly clears tokens and credentials, while maintaining standard playback preferences (skip times, speed).
3. **Interrupt Rewind Service Test**:
   - Mock a player interruption (e.g. pause command or audio focus transient loss) while `go_back_on_interrupt` is enabled.
   - Assert that the ExoPlayer seek action successfully rewinds by 5 seconds when resume is invoked.
4. **SettingsViewModel Unit Test** (`SettingsViewModelTest`):
   - Verify `updateSkipForwardDuration`, `updateSkipBackwardDuration`, `updatePlaybackSpeed`, `updateGoBackOnInterrupt`, and `updateSyncInterval` update the matching repository fields immediately.
   - Verify `syncNow()` invokes both library book sync and global progress sync with `forceRefresh = true`.
   - Verify `clearCache()` deletes all offline downloads recursively, calls `clearLocalData()` on the repository, and updates the formatted cache size flow.
   - Verify `logout()` delegates to `LogoutUseCase` and executes the navigation callback.
   - Verify `calculateCacheSize()` correctly formats and combines database file size and downloads directory size.
   - Verify `checkOfflineStatus()` updates the `isOffline` flow using `ConnectivityManager` active network capabilities.

### Manual Verification
1. **Android Auto Sync Verification**:
   - Change the skip backward duration to `15s` and playback speed to `1.5x` in the settings screen.
   - Boot the Desktop Head Unit (DHU) and verify that pressing skip-backward on the dashboard seeks by exactly 15 seconds, and active playback speed is initialized at `1.5x`.
2. **Cross-User Session Overwrite Verification**:
   - Log out from the active session. Log in as a different user account.
   - Verify that the app-specific `downloads/` directory is completely empty and no books from the prior user remain in the library grid.
3. **Periodic Sync Interval Modification**:
   - Verify using Android's command line tool that the scheduled `WorkManager` periodicity matches the chosen settings interval.
