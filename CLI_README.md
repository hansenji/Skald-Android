# Audiobookshelf CLI (Kotlin Script)

A command-line utility for interacting with the Audiobookshelf server API, written as a self-contained Kotlin script. It is designed to be highly user-friendly and **extremely AI agent-friendly** by supporting both pretty-printed terminal layouts for humans and raw structured JSON responses for automated scripts.

## 🚀 Setup & Execution

### Prerequisites
- **Kotlin CLI Compiler:** The utility is executed as a Kotlin main script (`.main.kts`). You must have Kotlin installed (e.g., via [SDKMAN](https://sdkman.io/) or your package manager).
- **Java Runtime Environment (JRE):** Java 11 or higher.

### Execution
You can run it directly as an executable:
```bash
chmod +x abs-cli.main.kts
./abs-cli.main.kts --help
```
Or run it explicitly using the Kotlin runner:
```bash
kotlin abs-cli.main.kts --help
```

---

## 🛠️ Configuration & Authentication

The CLI supports automatic persistence of connection configuration and token auth credentials.
- By default, credentials are saved to `~/.abs-cli.json`.
- If a `.abs-cli.json` exists in the local directory, it will take precedence.
- You can override this using the global `-c` or `--config` parameter.

### 🔑 Authentication (Login)
To log in and persist your token:
```bash
./abs-cli.main.kts login -s http://your-abs-server.com -u username -p password
```

---

## 🤖 AI Agent Integration

This utility has been designed from the ground up to be called programmatically by AI coding assistants or integration scripts:
1. **JSON Output Mode (`--json`):** Appending the `--json` flag to any action prints the raw JSON response from the server directly to `stdout`. This bypasses human formatting, enabling direct piping (`| jq`) or parser mapping.
2. **Error Isolation (`stderr`):** All diagnostics, connection attempts, loading logs, and errors are printed to `stderr`. This guarantees that `stdout` contains only clean, parseable JSON or targeted command output.
3. **Deterministic Exit Codes:**
   - `0`: Success.
   - `1`: Invalid arguments, parsing errors, or missing credentials.
   - `2`: Network errors or non-2xx API HTTP responses from the server.
4. **Stateless Overrides:** Even if a persistent configuration exists, you can override the target server and token on the fly using `--server` and `--token` flags.

---

## 📋 Available Commands & Examples

### ℹ️ General Actions
- **Get help:**
  ```bash
  ./abs-cli.main.kts --help
  ```
- **Check connection status:**
  ```bash
  ./abs-cli.main.kts status
  ./abs-cli.main.kts status --json
  ```

### 📚 Library & Item Browsing
- **List libraries:**
  ```bash
  ./abs-cli.main.kts libraries
  ```
- **List items in a specific library:**
  ```bash
  ./abs-cli.main.kts library-items lib_xxxx
  ```
  *Supports options:* `--limit <n>` (default 50), `--page <n>` (default 0)
- **Get detailed item metadata & chapters:**
  ```bash
  ./abs-cli.main.kts item item_yyyy
  ```
- **Global search across all libraries:**
  ```bash
  ./abs-cli.main.kts search "Stephen King"
  ```

### 🔄 Listening Progress
- **Get playback progress for a book:**
  ```bash
  ./abs-cli.main.kts get-progress item_yyyy
  ```
- **Sync/Update playback progress:**
  ```bash
  ./abs-cli.main.kts sync-progress item_yyyy --currentTime 1500 --duration 3000 --finished false
  ```
  *Options:*
  - `--currentTime <seconds>` (required)
  - `--duration <seconds>` (optional, used to auto-calculate progress percentage)
  - `--progress <float>` (optional, custom progress value from 0.0 to 1.0)
  - `--finished <true|false>` (optional)

### 🎵 Playlists, Collections & Authors
- **List playlists:**
  ```bash
  ./abs-cli.main.kts playlists
  ```
- **Get details of a playlist:**
  ```bash
  ./abs-cli.main.kts playlist playlist_zzzz
  ```
- **List collections in a library:**
  ```bash
  ./abs-cli.main.kts collections lib_xxxx
  ```
- **Get details of a collection:**
  ```bash
  ./abs-cli.main.kts collection col_aaaa
  ```
- **List authors in a library:**
  ```bash
  ./abs-cli.main.kts authors lib_xxxx
  ```
- **Get details of an author (includes their books):**
  ```bash
  ./abs-cli.main.kts author auth_bbbb
  ```
- **List series in a library:**
  ```bash
  ./abs-cli.main.kts series lib_xxxx
  ```
