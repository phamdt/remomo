# remomo — Remote Cursor Agent (Android)

Kotlin Multiplatform Android app for controlling [remo](https://github.com/phamdt/remo) agent runs from your phone. Pick a server-defined workspace, submit prompts, stream live progress, continue or cancel runs, and open PR results.

## Features

- Configure remote API URL and bearer token in Settings
- Browse server-defined workspaces (no repo URLs exposed to the app)
- Create runs in `plan_only` or `apply` mode
- Live SSE timeline (status, logs, tools, results)
- Continue conversation or cancel in-flight runs
- Open GitHub PR links in the browser

## Prerequisites

- **Android Studio** (latest stable) with Android SDK 35
- **JDK 17**
- Running **remo** API instance ([setup](https://github.com/phamdt/remo))

### Windows

1. Install [Android Studio](https://developer.android.com/studio).
2. Create `local.properties` in the repo root:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

## Quick start

```powershell
git clone https://github.com/phamdt/remomo.git
cd remomo
.\gradlew.bat :androidApp:installDebug
```

Or open the project in Android Studio and run the **androidApp** configuration.

## Pair with remo API

1. Start remo on your machine:

```powershell
cd C:\path\to\remo
$env:REMOTE_AGENT_TOKEN = "dev-token"
$env:CURSOR_API_KEY = "your-cursor-api-key"
npm run dev
```

2. Launch remomo on an **Android emulator** (or device on same network).
3. Open **Settings** and enter:

| Field | Local emulator value |
|-------|----------------------|
| Base URL | `http://10.0.2.2:8080` |
| Bearer token | same as `REMOTE_AGENT_TOKEN` |
| Apply token | optional; matches `REMOTE_AGENT_APPLY_TOKEN` |

4. Tap **Test connection** — you should see the workspace list.

For a physical device, use your PC's LAN IP (e.g. `http://192.168.1.10:8080`). Production deploys must use **HTTPS**.

## Development

```powershell
.\gradlew.bat :androidApp:installDebug   # build + install debug APK
.\gradlew.bat :shared:jvmTest            # unit tests
.\gradlew.bat :androidApp:assembleDebug    # build APK only
```

## Project layout

```
remomo/
├── androidApp/              # Jetpack Compose UI
│   └── src/main/kotlin/com/remomo/agent/
│       ├── ui/screens/      # Settings, WorkspaceList, NewRun, RunDetail
│       └── viewmodel/
├── shared/                  # KMP shared module
│   └── src/commonMain/kotlin/com/remomo/agent/
│       ├── api/             # Ktor client, DTOs, SSE parser
│       ├── repository/      # RemoteAgentRepository
│       └── validation/
└── docs/                    # App specification
```

## Architecture

```
Compose UI → ViewModels → RemoteAgentRepository → RemoteAgentApi → remo /v1 API
```

- **Networking:** Ktor Client (OkHttp on Android)
- **Serialization:** kotlinx.serialization
- **Secure storage:** EncryptedSharedPreferences for tokens and URL
- **Streaming:** Custom SSE parser over Ktor byte channel

Full spec: [`docs/remote-cursor-agent-kmp-app-spec.md`](docs/remote-cursor-agent-kmp-app-spec.md)

## API contract

The app implements the remo `/v1` REST + SSE contract. See the [remo API spec](https://github.com/phamdt/remo/blob/main/docs/remote-cursor-agent-api-spec.md).

| Screen | API used |
|--------|----------|
| Settings (connection test) | `GET /v1/workspaces` |
| Workspace list | `GET /v1/workspaces` |
| New run | `POST /v1/runs` |
| Run detail | `GET /v1/runs/{id}`, `GET /v1/runs/{id}/events`, continue/cancel |

## License

See [LICENSE](LICENSE) (AGPL-3.0).
