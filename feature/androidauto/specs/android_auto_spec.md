# Feature Specification: Android Auto Integration

This document defines the requirements, architecture, and behavioral specifications for adding Android Auto support to the Skald. This feature allows users to browse their audiobook library, resume playback of in-progress books, and control playback directly from their vehicle's dashboard.

---

## 1. Feature Context & Constraints

Android Auto media applications do not run custom visual user interfaces. Instead, they project a standard template-based UI provided by the Android Auto platform. To interface with Android Auto, the app must expose its media library and playback controls via Android **Media3's MediaSession / MediaLibraryService APIs**.

### Constraints
- **Driver Safety**: The user interface is template-driven, optimized for quick interactions with large tap targets, and enforces strict limits on list scrolling depth.
- **Module Structure**: Implemented as a single `:feature:androidauto` module.
- **Layered Delegation (Decoupling)**: 
  - To avoid circular dependencies and ensure that lock screen notifications and Bluetooth controllers work without loading the car module, **playback session callbacks** are handled in `:core:player`.
  - `:feature:androidauto` is responsible *only* for the **content browsing tree** and delegates playback controller command intercepts directly down to the core player callback.

---

## 2. Specific Behavioral Rules

### A. Media Browsing Structure (Content Tree)

Android Auto queries the `MediaLibraryService` using parent IDs to build a hierarchical content tree. The tree must support the following structure:

```
[Root]
 ├── Continue Listening (Browsable)
 │    ├── Book A (Playable)
 │    └── Book B (Playable)
 ├── Downloads (Browsable)
 │    ├── Book C (Playable)
 │    └── Book D (Playable)
 └── All Audiobooks (Browsable)
      ├── By Author (Browsable)
      │    ├── Author X (Browsable)
      │    │    └── Book A (Playable)
      │    └── Author Y (Browsable)
      │         └── Book B (Playable)
      └── A-Z (Browsable)
           ├── A (Browsable)
           │    └── Book A (Playable)
           ├── B (Browsable)
           │    └── Book B (Playable)
           └── ...
```

#### Media Item Attributes
- **Root node**: ID `"root"`, must return the top-level categories (Continue Listening, Downloads, All Audiobooks) as browsable media items.
- **Category nodes**:
  - **Continue Listening** (`"continue_listening"`): Query the repository for books with progress `> 0%` and `< 99%` (or not marked finished), sorted by `lastUpdated` descending.
  - **Downloads** (`"downloads"`): Query the database/repository for books where `isDownloaded == true`. This folder must remain fully functional when the app is offline.
  - **All Audiobooks** (`"all_audiobooks"`): Under this Level 1 category, show two Level 2 subcategories:
    - **By Author** (`"by_author"`): Level 3 is the list of authors; Level 4 is the playable books by that author.
    - **A-Z** (`"a_z"`): Level 3 is the starting letters (A, B, C, etc.); Level 4 is the playable books whose titles start with that letter.
- **Leaf nodes (Audiobooks)**:
  - Must represent the `Book` domain entity.
  - Set `MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER` or `MEDIA_TYPE_PLAYLIST`.
  - Set `isPlayable = true` and `isBrowsable = false`.
  - Display the cover artwork, title, and author/narrator in metadata.
  - **Dynamic Cover Art Loading**:
    - Request cover art dynamically using the server's cover endpoint: `/api/items/{id}/cover`.
    - Retrieve local cache first (`coverPath` in the SQLite `books` table) to support offline rendering.
    - If the remote query fails, or the device is offline and the cover is not cached, fall back to a local vector asset drawable (`ic_book_placeholder`).

### B. Media Session & Playback Controls Layout

Android Auto uses the Media3 `MediaLibraryService` templates to render the playback screen. The controls must be organized as follows:

#### 1. Controls Arrangement
- **Primary Controls (Main Playback Row)**:
  - **Playback Speed**: A custom action command button that cycles through playback speeds (`0.5x` to `2.0x` in `0.25x` steps). Positioned as the first button (leftmost). For the "icon" use the speedX value.
  - **Skip Backward**: Seeks backward by the user-defined duration. Always maps to a custom action command button. Positioned as the second button (left-of-center). Use the Replay icon from Material Symbols.
  - **Play / Pause**: Standard play/pause toggling. Positioned in the center.
  - **Skip Forward**: Seeks forward by the user-defined duration. Always maps to a custom action command button. Positioned as the fourth button (right-of-center). Use the Forward Media icon from Material Symbols.
- **Secondary Controls (Overflow Menu)**: Only show the overflow when chapter metadata is available.
  - **Skip to Previous Chapter**: Skips to the previous chapter. Positioned behind the overflow. Use the Skip Previous icon from Material Symbols
  - **Skip to Next Chapter**: Skips to the next chapter. Positioned behind the overflow. Use the Skip Next icon from Material Symbols

#### 2. Callback Delegation Mechanism
- **AndroidAutoBrowseCallback**: The `MediaLibrarySession.Callback` implementation in `:feature:androidauto` handles browsing queries (`onGetLibraryRoot`, `onGetChildren`, `onGetItem`).
- **Control Delegation**: 
  - To prevent circular dependencies and ensure a single playback engine, `AndroidAutoBrowseCallback` delegates all playback-related callbacks (such as `onConnect`, `onPostConnect`, `onCustomCommand`, `onAddMediaItems`, `onSetPlaybackSpeed`) directly to `AudiobookSessionCallback` located in `:core:player`.

#### 3. Layout Slot Mapping Technical Notes

Due to Android Auto's template-based rendering architecture, custom session commands are mapped to available control slots in a non-linear index order. To enforce the specified control layout, developers must keep these technical constraints in mind:

- **Custom Button Slot Mapping**:
  - The **first** button (Index 0) in the custom layout list is mapped to **Slot 2** (left-of-center).
  - The **second** button (Index 1) in the custom layout list is mapped to **Slot 4** (right-of-center).
  - The **third** button (Index 2) in the custom layout list is mapped to **Slot 1** (leftmost).
  - Any subsequent buttons are mapped to **Slot 5** or pushed to the **Overflow Menu**.

- **Required Layout Order**:
  To achieve the correct visual order (Playback Speed in Slot 1, Skip Backward in Slot 2, Skip Forward in Slot 4), the list of buttons passed to `setCustomLayout()` and `setMediaButtonPreferences()` must be ordered exactly as follows:
  1. `COMMAND_SKIP_BACKWARD` (Index 0 -> Slot 2)
  2. `COMMAND_SKIP_FORWARD` (Index 1 -> Slot 4)
  3. `COMMAND_CYCLE_SPEED` (Index 2 -> Slot 1)
  4. `COMMAND_SKIP_TO_PREVIOUS_CHAPTER` (Index 3 -> Overflow)
  5. `COMMAND_SKIP_TO_NEXT_CHAPTER` (Index 4 -> Overflow)

- **Standard Command Disabling**:
  To prevent Android Auto from rendering its default transport buttons on the main row (such as standard Seek Forward/Backward and Skip Previous/Next), standard player commands `COMMAND_SEEK_TO_PREVIOUS`, `COMMAND_SEEK_TO_NEXT`, `COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM`, `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM`, `COMMAND_SEEK_BACK`, and `COMMAND_SEEK_FORWARD` must be explicitly removed in `ForwardingPlayer.getAvailableCommands()`.

- **Forcing Chapter Buttons into Overflow**:
  To prevent the chapter previous/next skip actions from occupying Slot 5 of the main row, the boolean extra key `"com.google.android.gms.car.media.ALWAYS_IN_OVERFLOW"` must be set to `true` on the `CommandButton`'s extras bundle.

### C. Authentication & Error Handling

- **Logged Out State**: If no server configuration or authentication token exists, the root node should return a single browsable media item with a title indicating "Please log in on your phone" and playability set to `false`.
- **Empty States**: If a category (e.g., "Downloads") is empty, return an empty list. Android Auto will display a standard empty state message.
- **Offline Mode**: When the device is offline, filter **all** categories (including "Continue Listening" and "All Audiobooks") to only show downloaded books, ensuring every visible item can be played immediately.

### D. Voice Command Integration (Google Assistant)

Google Assistant voice commands (e.g., in Android Auto or hands-free driving) must be supported to enhance driver safety. These commands bypass the manual browse tree:
- **Generic Resumption Requests**: Commands like "continue reading my book", "read my audiobook", or "pickup where I last left off" must automatically resume the most recently played audiobook with saved progress.
- **Specific Content Requests**: Commands like "play [Book Title] on Skald" must query local and remote repositories to find the matching title and start playback.

---

## 3. Verification Plan

### Automated / Integration Tests
- **Callback Unit Tests**: Verify that `AndroidAutoBrowseCallback` constructs the correct browse items based on mock server states.
- **Delegation Tests**: Verify that calling non-browsing callbacks on `AndroidAutoBrowseCallback` successfully forwards the commands to `AudiobookSessionCallback`.

### Manual Verification
- **Desktop Head Unit (DHU)**: Use the Android Auto Desktop Head Unit to verify:
  1. The app appears in the Android Auto launcher.
  2. The browse tree behaves correctly.
  3. Clicking a book loads player UI and triggers media session callbacks.
