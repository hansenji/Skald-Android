# Core Specification: Settings & User Preferences

This document defines the persistent settings and user preferences stored locally within the application, utilizing Android `SharedPreferences` (or eventually Jetpack DataStore).

---

## 1. Context & Constraints

The application persists user configurations and runtime settings to ensure continuity across app relaunches. These settings are managed by the `PreferencesManager` under the filename `skald_prefs`.

All setting keys must remain consistent to avoid breaking migrations during app upgrades.

---

## 2. Persisted Settings Schema

The table below lists all settings keys, types, default values, and description of behaviors:

| Setting Key | Data Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| **`server_url`** | String | `null` | Normalized base URL of the self-hosted Audiobookshelf instance. |
| **`username`** | String | `null` | Username used to login to the server. |
| **`token`** | String | `null` | Authentication JWT returned by the server, used for Ktor authorization headers. |
| **`library_id`** | String | `null` | The active library ID browsed by the user. |
| **`filter_read_status`** | String | `null` | Read status filter option (e.g. "ALL", "READ", "UNREAD"). |
| **`filter_downloaded_only`** | Boolean | `false` | If `true`, lists only display downloaded audiobooks. |
| **`sort_option`** | String | `null` | Active sorting choice for lists/grids (e.g. "TITLE", "AUTHOR", "LAST_PLAYED"). |
| **`skip_forward_duration`** | Integer | `30` | Duration (in seconds) to jump forward when tapping skip-forward. Configured in mobile settings. |
| **`skip_backward_duration`** | Integer | `10` | Duration (in seconds) to jump backward when tapping skip-backward. Configured in mobile settings. |
| **`playback_speed`** | Float | `1.0f` | Persisted playback speed multiplier, cycling from `0.5f` to `3.0f` in `0.25f` steps. Shared between mobile and Android Auto players. |
| **`use_chapter_track`** | Boolean | `false` | If `true`, the player progress seekbar displays progress relative to the current chapter. If `false`, it displays relative to the total book. |
| **`library_sync_interval_hours`** | Integer | `24` | Configurable library synchronization interval (in hours). A value of `0` disables periodic sync. |
| **`library_last_sync_timestamp`** | Long | `0L` | Timestamp of the last successful library synchronization, used to compute elapsed time against sync interval. |
| **`etag_library_{libraryId}`** | String | `null` | ETag string returned by the server for conditional HTTP requests, cached dynamically per library ID to avoid redundant full syncing. |
| **`etag_user`** | String | `null` | ETag string returned by the server for `GET /api/me` conditional requests, cached dynamically to avoid redundant full user profile and progress syncing. |


---

## 3. Specific Behavioral Rules

### A. Initialization & Default Fallbacks
- If a preference is requested before it is written, the system must return its documented default value (`30` for skip forward, `10` for skip backward, and `1.0f` for playback speed).

### B. Speed Synchronization & Persistence
- Whenever the user changes the playback speed (either on their phone via the player UI or in the car via Android Auto's speed selector), the value must be updated instantly in `PreferencesManager` and applied to the active `ExoPlayer` instance.
- Upon starting a new audiobook or resuming playback, the player must load and apply the persisted `playback_speed`.

### C. Clear & Logout Actions
- When the user calls logout or wipes data (`clearLocalData()` or logout actions):
  - Credentials and cached sync states (`server_url`, `username`, `token`, `library_id`, `etag_library_{libraryId}`, `etag_user`) must be deleted immediately.
  - UI preferences (`filter_read_status`, `filter_downloaded_only`, `sort_option`) must be reset to defaults.
  - Playback preferences (`skip_forward_duration`, `skip_backward_duration`, `playback_speed`) should be preserved to retain the user's customized control experience, unless a full application reset is triggered.
