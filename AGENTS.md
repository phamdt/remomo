# Agent guide — remomo (Remote Cursor Agent mobile app)

Context for AI coding agents working in this repository.

## TL;DR

- **Stack:** Kotlin Multiplatform, Jetpack Compose, Ktor Client, kotlinx.serialization, Coroutines.
- **Run:** `gradlew.bat :androidApp:installDebug` (Windows) or `./gradlew :androidApp:installDebug`.
- **Test:** `gradlew.bat :shared:jvmTest` after behavior changes.
- **Backend:** [`phamdt/remo`](https://github.com/phamdt/remo) — TypeScript API at `/v1`.
- **Rules:** Small diffs; match local style; API config is runtime-only (no hardcoded URLs).

## What this is

Kotlin Multiplatform Android app that controls remote Cursor agent runs. Users configure API URL and bearer token in Settings, pick a server-defined workspace, submit prompts, stream SSE progress, continue/cancel runs, and open PR links.

The app never talks to GitHub or repos directly — only to the remo HTTPS API.

## First session

1. Install **Android Studio** with Android SDK 35 and **JDK 17**.
2. Create `local.properties` (gitignored):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

3. Start the remo API (sibling repo or deployed host).
4. Build and install:

```powershell
cd C:\path\to\remomo
.\gradlew.bat :androidApp:installDebug
```

5. In app **Settings**: base URL `http://10.0.2.2:8080`, bearer token matching `REMOTE_AGENT_TOKEN`.

### Windows developers

Use **PowerShell** or **cmd** with `gradlew.bat`. JDK 17 (Android Studio embedded JBR or Temurin) is required.

## Repository map

| Area | Role |
|------|------|
| `androidApp/` | Compose UI, ViewModels, navigation |
| `shared/src/commonMain/` | API client, DTOs, repository, validation |
| `shared/src/androidMain/` | OkHttp client, EncryptedSharedPreferences |
| `shared/src/commonTest/` | JVM unit tests |
| `docs/remote-cursor-agent-kmp-app-spec.md` | Full app spec |

### Key files

| Purpose | Path |
|---------|------|
| HTTP + SSE client | `shared/.../api/RemoteAgentApi.kt` |
| DTOs | `shared/.../api/dto/ApiModels.kt`, `SseEvent.kt` |
| Repository | `shared/.../repository/RemoteAgentRepository.kt` |
| Settings model | `shared/.../data/SettingsModels.kt` |
| URL validation | `shared/.../validation/InputValidation.kt` |
| Settings screen | `androidApp/.../ui/screens/SettingsScreen.kt` |

## Screens / navigation

```
Settings → Workspace list → New run → Run detail → (PR links in browser)
```

## API connection (remo)

No build-time env vars. All config is runtime in encrypted prefs:

| Setting | Field |
|---------|-------|
| API base URL | `AppSettings.baseUrl` |
| Bearer token | `AppSettings.bearerToken` |
| Apply token | `AppSettings.applyToken` |

Endpoints called (client appends `/v1`):

| Method | Path | Client method |
|--------|------|---------------|
| `GET` | `/v1/workspaces` | `listWorkspaces()` |
| `POST` | `/v1/runs` | `createRun()` |
| `GET` | `/v1/runs/{id}` | `getRunSummary()` |
| `GET` | `/v1/runs/{id}/events` | `streamEvents()` |
| `POST` | `/v1/runs/{id}/continue` | `continueRun()` |
| `POST` | `/v1/runs/{id}/cancel` | `cancelRun()` |

Auth: `Authorization: Bearer <token>` on every request.

## Tests

```powershell
.\gradlew.bat :shared:jvmTest   # primary unit tests
.\gradlew.bat test              # all module tests
.\gradlew.bat :androidApp:assembleDebug
```

Test files:
- `shared/src/commonTest/.../SseParserTest.kt`
- `shared/src/commonTest/.../InputValidationTest.kt`
- `shared/src/commonTest/.../KmpSecurityAuditTest.kt`

When you add or change behavior, **add or update unit tests** in `shared/src/commonTest/`.

## Conventions

- **Debug builds** allow `http://10.0.2.2`, `http://localhost`, `http://127.0.0.1`.
- **Release builds** require `https://` (`InputValidation.kt`).
- Cleartext allowed only for dev hosts via `network_security_config.xml`.
- Manual DI (no Hilt/Koin in MVP).
- Match existing Compose + glass UI theme patterns.

## Agent workflow

1. Read `docs/remote-cursor-agent-kmp-app-spec.md` before API or flow changes.
2. If changing DTOs, keep them aligned with remo (`src/types.ts`, `src/api-schema.ts`).
3. Run `:shared:jvmTest` after shared-module changes.
4. Do not hardcode API URLs or tokens in source.

## remo pairing checklist

1. remo running with `REMOTE_AGENT_TOKEN` set.
2. remomo Settings: base URL (no trailing `/v1`), matching bearer token.
3. Connection test → workspaces list loads from `GET /v1/workspaces`.
