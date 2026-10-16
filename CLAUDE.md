# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

GoZipo is a Kotlin Multiplatform + Compose Multiplatform ride-hailing project targeting Android and iOS (`iosArm64`, `iosSimulatorArm64`; `iosX64` is disabled). One repo builds two apps that share `core/` and `feature/`:

- **GoZipo** (customer): `apps/customer/`, Android app ID `com.mobile.gozipo`, iOS framework `CustomerShared`.
- **GoZipo Rider**: `apps/rider/`, Android app ID `com.mobile.gozipo.rider`, iOS framework `RiderShared`.

All UI is shared Compose. Each app's `iosApp/` is only a SwiftUI shell that hosts `MainViewController()` from its framework.

`document.md` tracks completed and remaining work by phase. Update it when a phase item is done.

## Commands

- Build Android debug APKs: `./gradlew :apps:customer:androidApp:assembleDebug` and `./gradlew :apps:rider:androidApp:assembleDebug`
- iOS: open `apps/customer/iosApp/` or `apps/rider/iosApp/` in Xcode and run. Xcode builds the app's framework via `:apps:<app>:shared:embedAndSignAppleFrameworkForXcode`.
- Shared tests (Android host): `./gradlew :apps:customer:shared:testAndroidHostTest`
- Shared tests (iOS simulator): `./gradlew :apps:customer:shared:iosSimulatorArm64Test`
- Tests in a library module: `./gradlew :core:domain:allTests` (or `:<module>:iosSimulatorArm64Test`)
- Single test: `./gradlew :apps:customer:shared:testAndroidHostTest --tests "com.mobile.gozipo.SharedLogicAndroidHostTest"`

**Required setup:** `local.properties` must contain `API_KEY=...` and `APP_LINK_HOST=...`. The convention plugins throw at configuration time without them. The key is sent as the `x-api-key` header on every request. `APP_LINK_HOST` is the public HTTPS host of the email App Links (today the backend's Cloudflare tunnel, the same host as the passkey RP ID); update it whenever the tunnel restarts.

Optional `local.properties` entries: `MAPBOX_ACCESS_TOKEN` (Mapbox public `pk.` token for maps, search and directions; without it the map shows a placeholder and Mapbox calls fail). `MAPBOX_SIMULATE_NAVIGATION` (default `false`) makes turn-by-turn guidance drive a simulated location along the route, for testing at a desk. `HTTP_LOGGING_ENABLED` (default `true`) turns Ktor request logging on or off. `HTTP_LOG_SENSITIVE_DATA` (default `false`) disables masking. Masking normally hides auth headers, the API key, tokens, passwords, sign-up codes and passkey credentials (`HttpLogSanitizer`). Only set it to `true` while debugging locally.

The backend is expected locally on port 8080: `http://10.0.2.2:8080/api` on the Android emulator and `http://localhost:8080/api` on the iOS simulator. These come from `core/data/.../network/UrlConstants.kt`, chosen per platform via `expect fun platformBaseUrl()`.

## Build structure

- `build-logic/convention` is an included build of convention plugins. Module `build.gradle.kts` files are deliberately thin: they apply one convention plugin through `libs.plugins.convention.*` and list dependencies.
  - `kmp.library`: KMP + Android library target + iOS frameworks + kotlinx.serialization (domain/data modules)
  - `cmp.library`: adds Compose + Material3
  - `cmp.feature`: adds Koin, ViewModel, lifecycle and Compose Navigation (presentation modules)
  - `cmp.application`: used by each app's `shared` module. Produces a static iOS framework named after the app folder (`:apps:rider:shared` → `RiderShared`).
  - `android.application`: used by each app's `androidApp`. It doesn't set `namespace` or `applicationId`; each app module sets its own.
  - `buildkonfig`, `room`: opt-in extras (`:core:data` uses buildkonfig)
- **Namespaces and packages come from the Gradle path**: `:feature:auth:data` → `com.mobile.feature.auth.data` (see `PathUtil.kt`). BuildKonfig's generated package follows the same rule (e.g. `com.mobile.core.data.BuildKonfig`). Compose resources are generated as `gozipo.<module path>.generated.resources.Res`.
- Versions, SDK levels and the app IDs live in `gradle/libs.versions.toml` (`project*` entries, `projectRiderApplicationId` for the rider app). Type-safe project accessors (`projects.core.domain`) are enabled.
- Register new modules in `settings.gradle.kts` and add them as dependencies of the app `shared` module(s) that use them.

## Architecture

Clean-architecture layering per module group:

- `core/domain`: pure Kotlin interfaces and models (`SessionStorage`, `PushNotificationService`, `DeviceTokenService`, validators), plus the error model: `Result<D, E : Error>` with `DataError.Remote/Local/Connection`, `EmptyResult`, and `map`/`onSuccess`/`onFailure`. Data-layer calls return `Result` and don't throw.
- `core/data`: implementations. `HttpClientFactory` builds the single Ktor client (JSON, logging via Kermit, bearer auth backed by DataStore `SessionStorage`, automatic refresh against `/auth/refresh`, which skips `auth/` routes). Use the `HttpClient.get/post/put/delete` extensions in `HttpClientExt.kt`. They resolve relative routes against the platform base URL and map HTTP status codes to `DataError.Remote`. Platform specifics go through `expect`/`actual` (`platformSafeCall`, `createDataStore`, `platformCoreDataModule`, Firebase push).
- `core/domain` also holds the provider-free geo contracts: `location/` (`GeoPoint`, `LocationProvider`), `place/` (`PlaceSearchService`), `route/` (`RouteService`), and `realtime/` (`RealtimeClient`, `ConnectionState`).
- Realtime: `KtorRealtimeClient` (`core/data/realtime`, adapted from Chirp) is the app's single `/ws/rides` socket. It is open while `messages` is collected, a user is signed in, the app is in the foreground and online. Feature data layers decode `RealtimeMessage.payload` by `type`, and resync over REST after reconnecting. Token refresh for both REST and the socket goes through `refreshSession` (`network/SessionRefresher.kt`), which is mutex-guarded because refresh tokens rotate.
- Maps: features use only `GoZipoMap` + `MapCameraState`/`MapMarker`/`MapPolyline` from `:core:map:api`, plus the domain services. `:core:map:mapbox` implements them (`mapboxModule`, included in each app's `initKoin`). Turn-by-turn guidance is `GoZipoNavigation` (`NavigationProvider` in `:core:map:api`), implemented with the Mapbox Navigation SDK v3 in `:core:map:navigation` (`mapboxNavigationModule`, rider app only, so the customer app doesn't ship the SDK). These two are the only modules that may import Mapbox. A different provider would be sibling modules bound instead. The Navigation SDK version must match the Maps SDK version it was released with (Android `mapbox-navigation` 3.32.1 ↔ `mapbox` 11.32.1 in `libs.versions.toml`; iOS 3.32.1 ↔ MapboxMaps 11.32.1). Mapbox calls use their own HTTP client: never send them through the main client (it adds our API key and bearer token). The Android Mapbox SDK comes from Mapbox's public Maven repo (`settings.gradle.kts`); the Google repo filter also allows `org.chromium` for its Cronet dependency.
- `core/presentation`: shared UI utilities (`UiText`, `DataError.toUiText()`, `ObserveAsEvents`, permissions, device configuration).
- `core/designsystem`: `GoZipoTheme` (light/dark), Plus Jakarta Sans typography, shapes, spacing, and shared components. `publicResClass = true`, so its resources are usable from other modules.
  - Colors: use `MaterialTheme.colorScheme` plus `MaterialTheme.colorScheme.extended` for semantic slots (text levels, surfaces, cash, info, map colors). Use `GoZipoTheme.spacing` for spacing. Never use raw palette values or hardcoded colors in features.
  - Components: `components/buttons`, `textfields` (built on `TextFieldState`), `cards`, `sheets` (`GoZipoMapSheetScaffold` = map + draggable sheet), `ride` (fare stepper, vehicle options, route stops, driver summary, sheet rows, status), and `map` (marker visuals that don't depend on any map library).
- `feature/<name>/{domain,data,presentation}`: feature slices. `auth` is used by both apps; `ride` (domain + data) is shared ride models, REST and realtime events (rider location goes out over the socket through `RiderService.shareLocation`); `ride` (presentation) holds ride UI shared by both apps (vehicle types, fare/distance text, countdown); `booking` (presentation) is the customer flow and `driving` (presentation) the rider flow (vehicle, availability, requests and offers, trip steps, earnings). Domain defines a service interface (`AuthService`), data implements it with Ktor (`KtorAuthService`), and presentation holds the screens.
- `apps/common`: app shell shared by both apps: `MainViewModel` (session, push token registration, logout), `ExternalUriHandler`/`DeepLinkListener`, and `appCommonModule(appRole)`. No UI.
- `apps/<app>/shared`: that app's composition root: `App.kt` (start destination from the session, `OnSessionExpired`), `NavigationRoot`, `initKoin`, and `MainViewController` + `handleDeepLink` for Swift. **New Koin modules must be added to the `initKoin` of each app that uses them.**
- `apps/<app>/androidApp`, `apps/<app>/iosApp`: platform launchers only.

### Presentation pattern (MVI-style)

Each screen package (e.g. `auth/presentation/login/`) contains `XState`, `XAction`, optional `XEvent`, `XViewModel` and `XScreen.kt`:

- The ViewModel exposes `state: StateFlow` (from `MutableStateFlow` + `onStart { load once }` + `stateIn(WhileSubscribed(5_000))`), a one-shot `events` flow backed by a `Channel`, and a single `onAction(action)` entry point.
- `XScreen.kt` has an `XRoot` composable (gets the VM via `koinViewModel()`, collects state, handles events with `ObserveAsEvents`) and a stateless `XScreen(state, onAction)` with a `@Preview`.
- Text inputs use Compose `TextFieldState` held in the state and observed with `snapshotFlow`.
- ViewModels are registered with `viewModelOf(::XViewModel)` in the feature's presentation Koin module.
- Navigation uses type-safe `@Serializable` routes in a sealed interface (`AuthGraphRoutes`), wired as a nested graph (`authGraph`).

## Current state and gotchas

- `initKoin` is called from `GoZipoApplication` / `GoZipoRiderApplication` on Android and from `iOSApp.init()` on iOS.
- Account roles: an account is CUSTOMER or RIDER, never both. Each app passes its role to `appCommonModule(UserRole.X)`, which provides `UserRole` through Koin. `KtorAuthService`/`KtorPasskeyService` send it on register, login, passkey sign-up and passkey login. The backend answers 409 (`ROLE_MISMATCH`) for an account of the other role, shown as "use the other app" (`role/UserRoleText.kt`).
- Both Android apps apply the Google Services plugin (one Firebase project).
- Email links are verified Android App Links split by role: `https://<APP_LINK_HOST>/link/{customer|rider}/{verify|reset-password}?token=…`. Each manifest claims only its own role's paths (host via the `${appLinkHost}` manifest placeholder), and `authGraph(emailLinkBaseUrl = emailLinkBaseUrl(role))` matches the same URLs. Never let both apps claim the same path or a custom scheme, or Android shows a chooser.
- The backend's passkey RP ID is a temporary trycloudflare tunnel domain used for testing. It changes whenever the tunnel restarts.
- Logged-in users land on `HomeRoute`. In the customer app that is `BookingRoot` (`:feature:booking:presentation`, the whole ride flow on one map screen); in the rider app it is `DrivingRoot` (`:feature:driving:presentation`), with `VehicleRoute` and `EarningsRoute` beside it. Logout goes through `MainViewModel.logout()`, which clears the session; the existing session-expired event then navigates back to auth.
- iOS map: the Mapbox iOS SDK is Swift-only, so `:core:map:mapbox` iosMain defines bridge interfaces (`MapboxIosBridge.kt`) that Swift implements in `apps/ios-common/Mapbox/GoZipoMapboxBridge.swift`. Both Xcode projects include that folder and the `mapbox-maps-ios` Swift package, and `iOSApp.init()` calls `IosInitKoinKt.doInitKoin(mapboxFactory: GoZipoMapboxBridgeFactory())`. The rider app also has the `mapbox-navigation-ios` package (3.32.1, which needs MapboxMaps 11.32.1), the background modes `audio` and `location` for guidance, and `apps/rider/iosApp/iosApp/Navigation/GoZipoNavigationBridge.swift`, which implements `:core:map:navigation`'s `NavigationIosBridge.kt` and is passed as `doInitKoin(mapboxFactory:navigationFactory:)`. Changing a bridge interface means updating the Swift file too. iOS location is `CoreLocationProvider`.
- Push (FCM): the backend sends ride updates only to users with no open ride socket. Android pushes are data-only, so `GoZipoFirebaseMessagingService.onMessageReceived` builds the notification ("Ride updates" channel). iOS gets normal APNs alerts. `apps/ios-common/Push/GoZipoAppDelegate.swift` (both Xcode projects, `firebase-ios-sdk` package, `aps-environment` entitlement, `remote-notification` background mode) configures Firebase and passes the FCM token to Kotlin via `MainViewControllerKt.updatePushToken`. The backend needs `FIREBASE_ENABLED=true`, and the Firebase project needs an APNs key for iOS. The home screens (`BookingRoot`/`DrivingRoot`) request the notification permission.
- Debug APKs are about 140 MB for the customer app and 255 MB for the rider app (Mapbox Maps and Navigation native libraries for every ABI); emulators low on storage fail to install them.
- `design/` holds UI reference screenshots, not code. They are dark-mode only; the light theme is derived from them.
- Passkeys (Android and iOS):
  - `PasskeyService` (domain) / `KtorPasskeyService` (data) call `/auth/passkey/*`.
  - `PasskeyAuthenticator` (domain) is the platform side. It is implemented in `feature/auth/data`: `CredentialManagerPasskeyAuthenticator` on Android, `AuthenticationServicesPasskeyAuthenticator` on iOS (AuthenticationServices called from Kotlin), bound through `platformAuthDataModule`. ViewModels inject it and run the whole ceremony themselves. The iOS one converts the backend's WebAuthn options JSON to an `ASAuthorizationPlatformPublicKeyCredentialProvider` request and returns the same WebAuthn response JSON as Android.
  - iOS setup: both apps have the Associated Domains entitlement `webcredentials:$(PASSKEY_RP_ID)$(ASSOCIATED_DOMAINS_MODE)`. `PASSKEY_RP_ID` lives in each app's `Configuration/Config.xcconfig` and must equal the backend's RP ID (update it with `APP_LINK_HOST` when the tunnel restarts). Debug adds `?mode=developer`, so a device fetches the file directly instead of through Apple's CDN (needs Developer Mode plus Settings > Developer > Associated Domains Development). The backend serves `/.well-known/apple-app-site-association` from `APPLE_APP_IDS` (`<Team ID>.<bundle ID>`), and accepts the `https://<RP ID>` origin. Associated Domains needs a real `TEAM_ID`.
  - Credential Manager needs an Activity. It gets it from `CurrentActivityProvider` (`core/data` androidMain), a weak reference to the foreground Activity, eagerly created by Koin.
  - Login uses usernameless passkey login. `NO_CREDENTIAL` routes to `PasskeySignup` (email → 6-digit code → create passkey).
  - Never log request/credential JSON.
- Auth forms use `GoZipoAuthFormLayout` + `GoZipoSnackbarScaffold`; outcome screens use `GoZipoResultLayout`. Previews containing `GoZipoPasswordTextField` need `@Preview(apiLevel = 36)` (layoutlib lacks `ShowSecretsSetting` on API 37).
