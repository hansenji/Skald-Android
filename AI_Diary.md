# AI Diary

## 2026-05-28

Today we aligned the Android Auto playback controls layout with the revised specifications and learned several critical details about Jetpack Media3 and Android Auto slot layout mapping:

1. **Android Auto Slot Mapping for Custom Actions**:
   In Media3, when standard seek previous/next/backward/forward player commands are disabled, custom command buttons are mapped to the transport control slots in a specific non-linear order:
   - **Index 0** in the custom layout list is mapped to **Slot 2** (left-of-center, traditionally *Skip Backward* / *Replay*).
   - **Index 1** in the custom layout list is mapped to **Slot 4** (right-of-center, traditionally *Skip Forward* / *Forward Media*).
   - **Index 2** in the custom layout list is mapped to **Slot 1** (leftmost, used for *Playback Speed*).
   - **Index 3 and beyond** are mapped to **Slot 5** (rightmost) or pushed to the **Overflow Menu**.
   
   To achieve the desired specification order (Slot 1: Speed, Slot 2: Backward, Slot 4: Forward), the custom command buttons must be added to the list in the order of `[COMMAND_SKIP_BACKWARD, COMMAND_SKIP_FORWARD, COMMAND_CYCLE_SPEED]`.

2. **Ensuring Controller-Specific Layouts via `onPostConnect`**:
   The `MediaLibrarySession.Callback` implementation (like `AndroidAutoBrowseCallback`) must explicitly override `onPostConnect` and delegate it to the core session callback. If omitted, the controller-specific layout is never established for Android Auto upon connection.

3. **Dynamic Layout Updates across Connected Controllers**:
   When updating the custom layout dynamically (e.g., when the book or playback speed changes), calling `session.setCustomLayout(layout)` globally is not enough. We must iterate over all controllers in `session.connectedControllers` and call `session.setCustomLayout(controller, layout)` to ensure the layout order propagates correctly to all active screens.

4. **Forcing Buttons Behind Overflow**:
   Setting the legacy boolean extra key `"com.google.android.gms.car.media.ALWAYS_IN_OVERFLOW"` to `true` on the `CommandButton`'s extras bundle successfully forces secondary buttons (like chapter skip buttons) into the overflow menu, keeping Slot 5 reserved for the three-dot overflow button.
