# ABS Client App - Project Specification

This document serves as the high-level source of truth for the ABS Client App project's purpose, design principles, supported form factors, global architectural constraints, and success measurements.

---

## 1. Application Purpose & User Context

The ABS Client App is an Android client for **Audiobookshelf**, a self-hosted audiobook and podcast server. 

### User Context
Users self-host their own media collections and expect a premium mobile listening experience. Typical usage patterns:
- Listening during commutes, exercise, or in transit where cellular network availability is intermittent.
- Switching devices (e.g., phone to desktop web player) and expecting their playback position to resume exactly where they left off.
- Pre-downloading audiobooks to local device storage to listen offline (e.g., on flights or in remote areas).

---

## 2. Core App Goals & Value Proposition

- **Seamless Synchronization**: Media playback progress must stay in sync with the Audiobookshelf server automatically and transparently.
- **Offline-First Resilience**: Cached library metadata, database records, and downloaded audio tracks must allow uninterrupted playback and offline operation.
- **Host Agility & Robust Login**: Support arbitrary self-hosted servers (local IPs, custom domains/ports, reverse proxies) with automatic formatting of host inputs.
- **Background Playback Stability**: Maintain reliable, long-running audio playback sessions when the screen is off or the app is minimized.
- **Multi-Device Adaptability**: Scale, format, and optimize the UI/UX cleanly across the following supported device types:
  - **Phone**: Responsive touch layout, portrait lock option, notification controls.
  - **Tablet**: Expanded multi-pane layout, optimal screen real estate utilization.
  - **Desktop (Chromebook/Googlebook)**: Android app compatibility, window resizing, keyboard navigation support.
  - **Android Auto**: Driver-safe UI, large tap targets, standard media player templates.

---

## 3. Global Architectural Constraints

To ensure consistency, maintainability, and compatibility, the codebase must adhere strictly to these architectural boundaries:
- **Architecture Style**: Clean Architecture with strict separation of concerns (Presentation, Domain, and Data layers).
- **Design Patterns**: Repository pattern isolating data access operations from the business domain.
- **Code Organization**: Multi-Module Gradle Architecture. The project is split into independent Gradle modules representing feature boundaries and architectural layers:
  - **App Module (`:app`)**: App entry point, Main Activity, Compose Navigation routing, global styling/theme, and Koin dependency injection configuration.
  - **Feature Modules**: Self-contained user experience flows containing Presentation layer components (UI views, ViewModels, and Compose layouts):
    - `:feature:login`: Login flows, server connection configuration, and credentials processing.
    - `:feature:library`: Library browsing, search grid, and book detail screens.
    - `:feature:player`: Audiobook player controls, sleep timer, speed controller, and chapters bottom sheet.
    - `:feature:androidauto`: Android Auto media integration, browsing content tree, and media session delegation.
  - **Data Module (`:data`)**: Repository implementations coordinating network, local database, and preferences data access.
  - **Domain Module (`:domain`)**: Use Cases and repository interfaces (pure Kotlin, free of Android platform dependencies).
  - **Core Utility & Infrastructure Modules**: Core cross-cutting services and platform interfaces:
    - `:core:model`: Shared Kotlin domain entities (e.g. `Book`, `Library`) and pure-Kotlin formatting helpers.
    - `:core:preferences`: Storage configuration and token persistence (`PreferencesManager`).
    - `:core:database`: Room local cache configuration, tables, and entities DTO (`AppDatabase`, `LocalEntities`).
    - `:core:network`: Ktor API client calling and response deserialization DTOs (`AudiobookshelfRemoteDataSource`).
    - `:core:player`: Media3 ExoPlayer orchestration, `AudiobookPlayerService` background service, and `PlayerManager`.
- **Language**: Developed using Kotlin.
- **UI Framework**: Built with Jetpack Compose using Material Design 3 guidelines.
- **Navigation**: Controlled globally using the **Androidx Navigation3** library (`androidx.navigation3.runtime` and `androidx.navigation3.ui`). Feature routing configuration follows the `api`/`impl` pattern of the `nowinandroid` project:
  - **Feature API**: Exposes navigation keys, destination routes, arguments, and public action interfaces.
  - **Feature Implementation**: Encapsulates internal screen composables, ViewModels, and UI fragments. Client modules interact with features exclusively through their public API, minimizing inter-module coupling.
- **Networking**: All remote API communications are handled using Ktor Client with `kotlinx.serialization`.
- **Dependency Injection**: Application services and ViewModels are wired and managed via Koin.
- **Local Caching & Persistence**: Cached media files, libraries, progress, and download queues are persisted locally in a SQLite database via Room.
- **Media Playback**: Audio playback and OS media controls integration must utilize Android Media3 (ExoPlayer and MediaSession).
- **Image Loading**: Image loading, dynamic cover rendering, and caching are managed using the **Coil** library (`io.coil-kt:coil-compose`).
- **Localization**: Always use localizable strings (Android resource strings) for displayable user-facing content that does not come directly from the server. Hardcoded string literals in UI layouts, Compose components, and platform integration logic are prohibited.

---

## 4. Success Metrics

The overall quality and performance of the application will be measured against the following targets:
- **Playback Start Delay**: Audio output must start within 1.5 seconds under standard network conditions (>10Mbps) for online streaming. Offline playback must start within 500ms.
- **Sync Fidelity**: 100% of finished book statuses and playback positions must be successfully synced to the server. No listening progress should ever be lost due to application crashes or abrupt closures.
- **Offline Transition Time**: The app must transition from online to offline mode (rendering downloaded and cached items) in under 300ms upon network loss.
- **Battery Draw**: Active background audio playback must not consume more than 5% of device battery per hour on standard modern devices.
- **Data Minimization**: Playback synchronization payloads sent over Ktor must remain lightweight (under 1KB per sync request) to conserve user bandwidth.
