# Feature Specification: Mini Player

This document defines the requirements, user interface behaviors, and layout integration rules for the global Mini Player component.

---

## 1. Feature Context & Constraints

The `:feature:miniplayer` module is responsible for the persistent, compact playback interface displayed across non-player screens. It communicates with the core player engine (`PlayerManager`) to reflect active media metadata, status, and progress.

To prevent visual obstruction and maintain a coherent navigation flow, the Mini Player is designed as an outer layout decoration surrounding the main navigation graph rather than an independent screen.

---

## 2. Behavioral Rules

### A. Visibility Constraints
1. **Active Playback State**: The Mini Player must only be visible when there is an active audiobook loaded in memory (`PlayerManager.currentBook.value != null`).
2. **Screen Exclusion**: The Mini Player must be hidden on the following routes:
   - `LoginScreen` (before the user is authenticated).
   - `PlayerScreen` (where the full-screen player controls are presented).

### B. Playback Interaction
1. **Play/Pause Toggle**: 
   - Tapping the Play/Pause button on the Mini Player must directly control playback.
   - If the player is currently playing, tap must invoke `PlayerManager.pause()`.
   - If the player is paused, tap must invoke `PlayerManager.play()`.
2. **Navigation Expansion**:
   - Tapping anywhere else on the Mini Player bar (outside the control buttons) must navigate to the full-screen `PlayerScreen` by adding `Player` to the navigation backstack.
3. **Dismiss Playback**:
   - Tapping the Close/Dismiss (X) button on the end side of the Mini Player must stop playback, stop the background playback service, and clear the active book session (by setting `currentBook` to null in `PlayerManager`).

---

## 3. UI Component Specifications

The Mini Player must offer a premium, modern design matching the rest of the application.

- **Layout Structure**: Adaptive layout based on screen width/device class:
  - **Compact Screens (Phones)**: A docked full-width horizontal bar at the bottom.
  - **Medium/Expanded Screens (Tablets / Large Devices)**: A floating horizontal pill/card with margins (e.g., `16.dp` from the screen edges) and fully rounded corners.
- **Dynamic Background**: Glassmorphism aesthetic (semi-transparent dark surface with a subtle border and blur backdrop, e.g., using `Modifier.blur` or a translucent color fill) to blend elegantly with content scrolling behind it. On compact screens, it extends edge-to-edge. On expanded screens, it is clipped to the floating pill shape.
- **Book Cover**: A small, rounded-corner square card (e.g., `8.dp` corner radius) on the far start. It should load cover art using Coil. If the cover fails to load or is not available, it must fall back to a subtle color gradient using theme primary/tertiary colors with a default audiobook icon in the center.
- **Text Labels**:
  - **Title**: Displays the audiobook title. Single line, bold, with trailing ellipsis if truncated.
  - **Subtitle**: Displays the author's name. Single line, muted color, small font size.
- **Control Buttons**: A horizontal group on the end containing:
  - Play/Pause icon button. Displays a loading spinner (e.g. `CircularProgressIndicator`) instead of the play/pause icon when the player is in the buffering/preparing state before playback.
  - Close/Dismiss icon button (standard `Close` / `X` vector icon).
- **Progress Track**: A very thin, unobtrusive read-only linear progress bar (e.g., `2.dp` height) positioned at the very top edge of the Mini Player row. It reflects the playback progress ratio (`currentPosition / duration`) and is not interactive to prevent accidental seeks when tapping the mini player container.

---

## 4. Verification Plan

The following manual and automated tests should be performed to verify the correctness of the Mini Player:

### A. Automated Checks
- **Build compilation check**: Verify the module and the whole app compile without errors using `./gradlew assembleDebug`.
- **Unit test integrity**: Verify existing unit tests run successfully using `./gradlew test`.

### B. Manual / Interaction Verification Checklist
1. **Initial Launch (No Active Book)**:
   - Log in to the application and view the library screen before playing any books.
   - *Expected Behavior*: The mini player must **not** be visible at all.
2. **Playback Activation & Back Navigation**:
   - Select a book from the library to open the details page, and click **Play**. Navigate back using the top-bar back arrow.
   - *Expected Behavior*: Playback starts, and after navigating back, the mini player appears anchored to the bottom of the detail screen. The title, author, and cover art match the active book.
3. **Playback Controls Integration**:
   - Tap the **Play/Pause** button on the mini player.
   - *Expected Behavior*: Playback state toggles instantly. The audio stream pauses/resumes, and the button icon updates (Play $\leftrightarrow$ Pause).
4. **Expanded Player Interaction**:
   - Tap the container/body of the mini player (excluding the Play/Pause button).
   - *Expected Behavior*: The app navigates smoothly to the full-screen `PlayerScreen`.
5. **Navigation Flow & Out-of-Scope Visibility**:
   - Navigate from the detail page back to the library page. Finally, trigger a logout from the library page.
   - *Expected Behavior*:
     - The mini player remains pinned at the bottom across both screens.
     - Once logged out and returned to the `LoginScreen`, the mini player disappears.
6. **Dismiss Player Action**:
   - Tap the Close/Dismiss (X) button on the mini player.
   - *Expected Behavior*:
     - Playback stops immediately.
     - The background notification and playback service are stopped/removed.
     - The mini player disappears from the screen.

