# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

GoZipo is a Kotlin Multiplatform + Compose Multiplatform app targeting Android and iOS (`iosArm64`, `iosSimulatorArm64`; `iosX64` is disabled). All UI is shared Compose; `iosApp/` is only a SwiftUI shell that hosts `MainViewController()` from the `Shared` framework.

## Commands

- Build Android debug APK: `./gradlew :androidApp:assembleDebug`
- iOS: open `iosApp/` in Xcode and run (Xcode builds the `Shared` framework via Gradle).
- Shared tests (Android host): `./gradlew :shared:testAndroidHostTest`
- Shared tests (iOS simulator): `./gradlew :shared:iosSimulatorArm64Test`
- Tests in a library module: `./gradlew :core:domain:allTests` (or `:<module>:iosSimulatorArm64Test`)
- Single test: `./gradlew :shared:testAndroidHostTest --tests "com.mobile.gozipo.SharedLogicAndroidHostTest"`

**Required setup:** `local.properties` must contain `API_KEY=...`. The BuildKonfig convention plugin throws at configuration time without it. The key is sent as the `x-api-key` header on every request.

Optional `local.properties` flags: `HTTP_LOGGING_ENABLED` (default `true`) turns Ktor request logging on or off. `HTTP_LOG_SENSITIVE_DATA` (default `false`) disables masking. Masking normally hides auth headers, the API key, tokens, passwords, sign-up codes and passkey credentials (`HttpLogSanitizer`). Only set it to `true` while debugging locally.

The backend is expected locally on port 8080: `http://10.0.2.2:8080/api` on the Android emulator and `http://localhost:8080/api` on the iOS simulator. These come from `core/data/.../network/UrlConstants.kt`, chosen per platform via `expect fun platformBaseUrl()`.

## Build structure

- `build-logic/convention` is an included build of convention plugins. Module `build.gradle.kts` files are deliberately thin: they apply one convention plugin through `libs.plugins.convention.*` and list dependencies.
  - `kmp.library`: KMP + Android library target + iOS frameworks + kotlinx.serialization (domain/data modules)
  - `cmp.library`: adds Compose + Material3
  - `cmp.feature`: adds Koin, ViewModel, lifecycle and Compose Navigation (presentation modules)
  - `cmp.application`: used by `:shared` and produces the static `Shared` iOS framework
  - `buildkonfig`, `room`: opt-in extras (`:core:data` uses buildkonfig)
- **Namespaces and packages come from the Gradle path**: `:feature:auth:data` → `com.mobile.feature.auth.data` (see `PathUtil.kt`). BuildKonfig's generated package follows the same rule (e.g. `com.mobile.core.data.BuildKonfig`). Compose resources are generated as `gozipo.<module path>.generated.resources.Res`.
- Versions, SDK levels and the app ID live in `gradle/libs.versions.toml` (`project*` entries). Type-safe project accessors (`projects.core.domain`) are enabled.
- Register new modules in `settings.gradle.kts` and add them as dependencies of `:shared`.

## Architecture

Clean-architecture layering per module group:

- `core/domain`: pure Kotlin interfaces and models (`SessionStorage`, `PushNotificationService`, `DeviceTokenService`, validators), plus the error model: `Result<D, E : Error>` with `DataError.Remote/Local/Connection`, `EmptyResult`, and `map`/`onSuccess`/`onFailure`. Data-layer calls return `Result` and don't throw.
- `core/data`: implementations. `HttpClientFactory` builds the single Ktor client (JSON, logging via Kermit, bearer auth backed by DataStore `SessionStorage`, automatic refresh against `/auth/refresh`, which skips `auth/` routes). Use the `HttpClient.get/post/put/delete` extensions in `HttpClientExt.kt`. They resolve relative routes against the platform base URL and map HTTP status codes to `DataError.Remote`. Platform specifics go through `expect`/`actual` (`platformSafeCall`, `createDataStore`, `platformCoreDataModule`, Firebase push).
- `core/presentation`: shared UI utilities (`UiText`, `DataError.toUiText()`, `ObserveAsEvents`, permissions, device configuration).
- `core/designsystem`: `GoZipoTheme` (light/dark), Plus Jakarta Sans typography, shapes, spacing, and shared components. `publicResClass = true`, so its resources are usable from other modules.
  - Colors: use `MaterialTheme.colorScheme` plus `MaterialTheme.colorScheme.extended` for semantic slots (text levels, surfaces, cash, info, map colors). Use `GoZipoTheme.spacing` for spacing. Never use raw palette values or hardcoded colors in features.
  - Components: `components/buttons`, `textfields` (built on `TextFieldState`), `cards`, `sheets` (`GoZipoMapSheetScaffold` = map + draggable sheet), `ride` (fare stepper, vehicle options, route stops, driver summary, sheet rows, status), and `map` (marker visuals that don't depend on any map library).
- `feature/<name>/{domain,data,presentation}`: feature slices. `auth` is the only one so far. Domain defines a service interface (`AuthService`), data implements it with Ktor (`KtorAuthService`), and presentation holds the screens.
- `shared`: the app composition root. `App.kt` (MainViewModel decides the start destination from the session and handles `OnSessionExpired`), `NavigationRoot`, deep-link handling, and `initKoin`, which lists every Koin module. **New Koin modules must be added to `initKoin`.**

### Presentation pattern (MVI-style)

Each screen package (e.g. `auth/presentation/login/`) contains `XState`, `XAction`, optional `XEvent`, `XViewModel` and `XScreen.kt`:

- The ViewModel exposes `state: StateFlow` (from `MutableStateFlow` + `onStart { load once }` + `stateIn(WhileSubscribed(5_000))`), a one-shot `events` flow backed by a `Channel`, and a single `onAction(action)` entry point.
- `XScreen.kt` has an `XRoot` composable (gets the VM via `koinViewModel()`, collects state, handles events with `ObserveAsEvents`) and a stateless `XScreen(state, onAction)` with a `@Preview`.
- Text inputs use Compose `TextFieldState` held in the state and observed with `snapshotFlow`.
- ViewModels are registered with `viewModelOf(::XViewModel)` in the feature's presentation Koin module.
- Navigation uses type-safe `@Serializable` routes in a sealed interface (`AuthGraphRoutes`), wired as a nested graph (`authGraph`).

## Current state and gotchas

- `initKoin` is called from `GoZipoApplication` on Android and from `iOSApp.init()` on iOS.
- The backend's passkey RP ID is a temporary trycloudflare tunnel domain used for testing. It changes whenever the tunnel restarts.
- Logged-in users land on `HomeRoute` (`shared/.../home/HomePlaceholderScreen`), a temporary screen with logout, until the customer map flow exists. Logout goes through `MainViewModel.logout()`, which clears the session; the existing session-expired event then navigates back to auth.
- `design/` holds UI reference screenshots, not code. They are dark-mode only; the light theme is derived from them.
- Passkeys (Android only; iOS returns `isSupported = false` and the UI hides passkey buttons):
  - `PasskeyService` (domain) / `KtorPasskeyService` (data) call `/auth/passkey/*`.
  - `PasskeyAuthenticator` (domain) is the platform side. It is implemented in `feature/auth/data`: `CredentialManagerPasskeyAuthenticator` on Android, `UnsupportedPasskeyAuthenticator` on iOS, bound through `platformAuthDataModule`. ViewModels inject it and run the whole ceremony themselves.
  - Credential Manager needs an Activity. It gets it from `CurrentActivityProvider` (`core/data` androidMain), a weak reference to the foreground Activity, eagerly created by Koin.
  - Login uses usernameless passkey login. `NO_CREDENTIAL` routes to `PasskeySignup` (email → 6-digit code → create passkey).
  - Never log request/credential JSON.
- Auth forms use `GoZipoAuthFormLayout` + `GoZipoSnackbarScaffold`; outcome screens use `GoZipoResultLayout`. Previews containing `GoZipoPasswordTextField` need `@Preview(apiLevel = 36)` (layoutlib lacks `ShowSecretsSetting` on API 37).
