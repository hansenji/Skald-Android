# Skald - Design Specification

This document defines the design language, color palette, typography system, component guidelines, adaptive layouts, and iconography rules for the Skald application. It acts as the visual source of truth to ensure a premium, modern, and cohesive user interface across all supported platforms and form factors.

---

## 1. Design Philosophy

Skald is inspired by the ancient Norse oral tradition of storytelling. The visual design reflects this heritage by combining deep, night-sky dark modes with vibrant, electric accents. The design principles are:

*   **Immersive Listening**: The UI feels alive and integrated with the active audiobook content. Dynamic, highly blurred backgrounds reflect the cover art of the playing media, providing a premium visual feel.
*   **Tactile and Readable**: High-contrast, slate-based text scales and balances cleanly across light and dark layers. Layouts use semi-transparent overlays (glassmorphism) and subtle borders to define hierarchy without clutter.
*   **Adaptive and Fluid**: The interfaces scale gracefully from portrait phone layouts and Android Auto dashboard screens to multi-pane tablet views and desktop displays.
*   **Micro-interactions**: Subtle gradients, clean progress bars, and state-reflective icons guide the user's eyes to playback progress and status changes.

---

## 2. Color System & Palettes

Skald implements a modern **Premium Obsidian & Electric Purple** color palette. The color scheme is locked to a dark mode layout to provide a theater-like listening experience.

### A. Core Color Swatches

These values are declared in `dev.vikingsen.skald.theme.Color.kt`:

| Color Token | Hex Value | Purpose |
| :--- | :--- | :--- |
| **`DarkBg`** | `#0B0F19` | Deep obsidian background. Used for full-screen layout backdrops. |
| **`DarkSurface`** | `#151D30` | Dark slate surface container. Used for cards, dialog backgrounds, and navigation elements. |
| **`DarkSurfaceVariant`** | `#222E4B` | Slate variant surface. Used for secondary containers, chips, and text input outlines. |
| **`ElectricPurple`** | `#BB86FC` | Vibrant purple accent. The primary brand color. Used for progress tracks, focus elements, and major buttons. |
| **`CyanAccent`** | `#03DAC6` | Teal/Cyan secondary accent. Used for secondary controls, selected chip details, and highlight badges. |
| **`SoftPink`** | `#FFFF79C6` | Vibrant tertiary accent. Used for alternate branding elements, secondary badges, and error-state fallbacks. |
| **`TextPrimary`** | `#F1F5F9` | Slate 100. High-contrast neutral color for headers and body text. |
| **`TextSecondary`** | `#94A3B8` | Slate 400. Mid-contrast neutral color for subheadings, descriptive text, and metadata. |
| **`TextMuted`** | `#64748B` | Slate 500. Low-contrast neutral color for disabled controls and placeholder text. |

### B. Material 3 Color Scheme Mapping

These colors are mapped directly inside the `SkaldTheme` in `dev.vikingsen.skald.theme.Theme.kt` using `darkColorScheme`:

```kotlin
private val PremiumDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    secondary = CyanAccent,
    tertiary = SoftPink,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color(0xFF1E1B4B), // Deep violet for text on primary elements
    onSecondary = Color(0xFF0C4A6E), // Deep cyan for text on secondary elements
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)
```

---

## 3. Typography System

The typography configuration ensures readability of metadata (book titles, authors, narrators) at various sizes. The typography is declared in `dev.vikingsen.skald.theme.Type.kt` and utilizes Material Design 3 styles.

### A. Typography Hierarchy

| Material Type | Weight | Size (SP) | Line Height | Letter Spacing | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`Title Large`** | Bold / Normal | `22.sp` | `28.sp` | `0.sp` | Top app bar titles, prominent headings. |
| **`Body Large`** | Normal | `16.sp` | `24.sp` | `0.5.sp` | Descriptions, list item details, and input text. |
| **`Body Medium`** | Normal | `14.sp` | `20.sp` | `0.25.sp` | Card titles, action texts, and secondary descriptions. |
| **`Label Medium`** | Medium / Bold | `12.sp` | `16.sp` | `0.5.sp` | Badges, time indicators, and small chip labels. |
| **`Label Small`** | Medium | `11.sp` | `16.sp` | `0.5.sp` | Status micro-text and chapter timestamp details. |

---

## 4. Visual Components & Styling Patterns

### A. Book Card Grid (`BookCard`)

The main entry point to library media uses a highly visual, grid-based card structure:
*   **Dimensions**: Formats in an adaptive grid with a minimum size of `140.dp` width.
*   **Aspect Ratio**: Cover artwork is loaded in a square `1:1` aspect ratio inside the card header.
*   **Border & Corners**: The entire card is clipped to a `16.dp` rounded corner layout.
*   **Badges**:
    *   *Read Badge*: An icon-only green checkmark (`Icons.Default.CheckCircle` colored with `MaterialTheme.colorScheme.primary`) in a translucent circle background (`Color(0x99000000)`) at the top-left (`TopStart`) corner.
    *   *Downloaded Badge*: An icon-only green download badge (`Icons.Default.OfflinePin` colored with `Color.Green`) in a translucent circle background at the top-right (`TopEnd`) corner.
*   **Progress Bar**: A lightweight `4.dp` `LinearProgressIndicator` overlays the bottom edge of the cover art if the audiobook is in-progress (and not completed).

```
+---------------------------+
| [Check]           [Pin]   |
|                           |
|       (Cover Art)         |
|           1:1             |
|                           |
|=======[Progress]==========|  <-- Overlay progress (4dp)
| Book Title                |  <-- Bold, 14sp, Max 1 line
| Author name               |  <-- Slate 400, 12sp, Max 1 line
+---------------------------+
```

### B. Full-Screen Player Layout (`PlayerScreen`)

The playback screen utilizes an immersive layered aesthetic:
1.  **Bottom Layer**: The active book's cover art, expanded to fill the entire container and heavily blurred (`50.dp`).
2.  **Overlay Layer**: A dark, semi-transparent color cover (`Color(0xCD0A0E1A)`) applied over the blurred image to ensure maximum text and control readability.
3.  **Content Layer**:
    *   *App Bar*: Transparent background with white control buttons (`ArrowBack`, `List` for chapters, `Settings` for speed/durations).
    *   *Main Cover Card*: An elevated book cover card centered on the screen, rounded to `24.dp` and bordered by a subtle semi-transparent white stroke (`1.dp`, `Color(0x33FFFFFF)`).
    *   *Track & Meta*: Active chapter displayed in a capsule pill (`Color(0x33FFFFFF)` surface background with `1.dp` semi-transparent borders).
    *   *Play/Pause Button*: A prominent circular button (`72.dp` diameter) colored with `primary` (`ElectricPurple`) to stand out as the primary action.

### C. Mini-Player Banner (`MiniPlayerView`)

Provides persistent playback controls when the user browses the library:
*   **Placement**: Sticks to the bottom of the screen, overlaying the main navigation.
*   **Adaptive Styling**:
    *   *Compact (Phone)*: Spans the full width, rounded corners at `0.dp`, with a thin top border stroke (`1.dp`, `Color(0x1EFFFFFF)`).
    *   *Expanded (Tablet)*: Floats above the screen, padded by `16.dp` on all sides, with `24.dp` rounded corners, subtle drop shadow (`8.dp`), and a full border stroke (`1.dp`, `Color(0x1EFFFFFF)`).
*   **Backdrop**: Transparent blend using surface color with `0.85f` alpha opacity.
*   **Progress Track**: A micro-sized `2.dp` reading progress indicator runs along the top edge of the mini-player banner.

### D. Contextual Item Action Menu (`ItemMoreMenuBottomSheet`)

Provides a unified contextual operations menu for audiobooks:
*   **Container**: Uses a Material 3 `ModalBottomSheet` matching the obsidian design language.
*   **Styling**:
    *   *Background*: Deep slate surface (`DarkSurface`, `#151D30`).
    *   *Border*: Subtle top border stroke (`1.dp`, `DarkSurfaceVariant`, `#222E4B`).
    *   *Corners*: Rounded top corners at `24.dp` (`RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`).
*   **Menu Items**: Styled as clickable row items spanning full width:
    *   *Row Height*: Minimum `48.dp` for comfortable tap targets.
    *   *Icon*: Outlined vector glyphs using `TextSecondary` (`#94A3B8`) or brand color accents (`ElectricPurple`, `#BB86FC`).
    *   *Label Text*: `TextPrimary` (`#F1F5F9`) using `BodyLarge` (16sp) typography.
    *   *Destructive Actions* (e.g., *Discard Progress*, *Delete Download*): Use red/error highlights (`MaterialTheme.colorScheme.error` or `SoftPink`) on text and icons.

---

## 5. Adaptive Layouts & Form Factors

### A. Navigation Layout Suite

The app uses `NavigationSuiteScaffold` to automatically switch between top-level navigation interfaces based on window width:

*   **Phone (Compact, width < 600dp)**: Standard Material 3 `NavigationBar` at the bottom.
*   **Tablet/Desktop (Medium/Expanded, width >= 600dp)**: Material 3 `NavigationRail` along the leading edge (left side).
*   **Transitions**: Icons cycle dynamically between an outlined style (unselected) and a filled style (selected).

### B. Android Auto Integration

A critical subset of the design language scales to the vehicle dashboard using standardized media session templates:
*   **Background**: Dominated by system dark UI, utilizing standard driver-safe layouts.
*   **Playback Slots Mapping**: Standardized Media3 command mappings ensure consistent layout structure:
    *   **Slot 1 (Far Left)**: Playback speed indicator/selector. Note: The Android Auto interface is restricted to standard cycle speed (0.25x steps), whereas the mobile player supports more granular (0.1x steps) speed controls.
    *   **Slot 2 (Left of Center)**: Skip Backward (utilizes configured duration).
    *   **Center Play/Pause**: Primary status toggle button.
    *   **Slot 4 (Right of Center)**: Skip Forward (utilizes configured duration).
    *   **Slot 5 (Far Right / Overflow)**: Secondary actions (e.g. Chapter jumps) are forced into the overflow three-dot menu using the `"com.google.android.gms.car.media.ALWAYS_IN_OVERFLOW"` extra key.

---

## 6. Custom Vector Icon Assets

For player speed indicators, Skald utilizes a custom set of vector speed dial icons located in `:feature:player` (`dev.vikingsen.skald.feature.player.icons`):

*   **Speed States**: Custom drawings displaying `0.5x`, `0.7x`, `1x`, `1.2x`, `1.5x`, `1.7x`, and `2x` inside a standard speed-meter glyph.
*   **Granular Speed Indicator (Mobile)**: Since the mobile player supports granular speed controls in `0.1x` steps (up to `2.0x`), a text label displaying the exact multiplier (e.g. `1.1x`, `1.3x`) must accompany the icon. The icon should map to the closest lower standard speed vector icon.
*   **Control Actions**: Custom vector paths for skip and playback controls:
    *   `Replay.kt` (Skip Backward)
    *   `ForwardMedia.kt` (Skip Forward)
    *   `SkipNext.kt` (Next Chapter)
    *   `SkipPrevious.kt` (Previous Chapter)

---

## 7. Dynamic Asset Fallbacks

When book covers fail to load or are missing from the Audiobookshelf server:
*   **Fallback Art**: A linear gradient is drawn dynamically across the artwork space using:
    *   `MaterialTheme.colorScheme.primary` (ElectricPurple) as the start color.
    *   `MaterialTheme.colorScheme.tertiary` (SoftPink) as the end color.
*   **Fallback Text**: The first character of the book's title is drawn in white, bold, and centered:
    *   `64.sp` font size for full-screen cover displays.
    *   `42.sp` font size for library book cards.
    *   `32.sp` font size for detail headers.

---

## 8. Related Specifications

For other technical structures and rules:
*   **[Project Specification](file:///home/hansenji/src/abs-client-app/specs/project_spec.md)**: Details structural layout and navigation destinations.
*   **[Authentication Specification](file:///home/hansenji/src/abs-client-app/specs/auth_spec.md)**: Details auth status persistence.
