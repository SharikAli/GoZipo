# GoZipo: Progress and Phase Plan

Last updated: 2026-10-06

Tick a box (`- [x]`) when an item is done and verified. Add new items under the phase they belong to.

**Status at a glance**

| Phase | Scope | Status |
| --- | --- | --- |
| 0 | Project foundation | Done |
| 1 | Design system | Done |
| 2 | Customer auth UI + Android passkeys | Done |
| 3 | Multi-app setup + Rider app shell | Done |
| 4 | Backend ride domain + realtime | Done except FCM device check |
| 5 | Location + Mapbox | Android done; iOS map and location deferred |
| 6 | Customer ride flow | Not started |
| 7 | Rider ride flow | Not started |
| 8 | Hardening | Not started |

---

## Overview

GoZipo is a ride-hailing system built to learn Mapbox, with no Google Maps.

| Part | Stack | Location |
| --- | --- | --- |
| GoZipo (customer app) | Kotlin + Compose Multiplatform, Android + iOS | `apps/customer/` |
| GoZipo Rider (rider app) | Kotlin + Compose Multiplatform, Android + iOS | `apps/rider/` |
| Backend | Kotlin Spring Boot | `~/development/projects/ride-hailing` |

### Standing rules

- Do not rewrite existing infrastructure. Reuse the current architecture and utilities.
- Do not log tokens, credentials or sensitive information.
- The backend is the source of truth for ride state, fare, payment status and ownership. Never trust these values from the client.

### Product decisions

- Riders may counter-offer the customer's fare. The fare step is PKR 10.
- Realtime uses a plain WebSocket with JSON messages (no STOMP), with FCM as the fallback. Reuse the Chirp implementation:
  - App: `~/Development/Chirp/ComposeMultiplatform/`
  - Backend: `~/Development/Chirp/Spring boot/chirp`
- An account is either CUSTOMER or RIDER, never both.
- The passkey RP domain stays on the current Cloudflare tunnel for now.
- Mapbox uses the public token only for now. The secret token and iOS Mapbox work are deferred.
- Backend changes are allowed.

---

## Phase 0: Project foundation (done)

- [x] Convention plugins in `build-logic`: `kmp.library`, `cmp.library`, `cmp.feature`, `cmp.application`, `buildkonfig`, `room`
- [x] Module layout: `core/{domain,data,presentation,designsystem}` and `feature/<name>/{domain,data,presentation}`
- [x] Error model: `Result<D, E>` with `DataError.Remote/Local/Connection`
- [x] Single Ktor client with bearer auth (DataStore session), automatic token refresh, `x-api-key` header
- [x] HTTP log masking for tokens, passwords, codes and passkey JSON (`HttpLogSanitizer`)
- [x] MVI screen pattern (State / Action / Event / ViewModel / Root + Screen), Koin, type-safe navigation
- [x] Push notification and device token interfaces (Firebase on Android)

## Phase 1: Design system (done)

- [x] `GoZipoTheme`: light and dark colors, Plus Jakarta Sans, shapes, spacing
- [x] Extended semantic colors: text levels, surfaces, cash, info, map colors
- [x] Buttons, text fields, cards
- [x] Sheets: `GoZipoMapSheetScaffold` (map + draggable sheet)
- [x] Ride components: fare stepper, vehicle options, route stops, driver summary, sheet rows, status
- [x] Map marker visuals that don't depend on a map library

## Phase 2: Customer auth UI + Android passkeys (done)

### Screens

- [x] Login: email + password, forgot password, "Continue with passkey" (hidden when unsupported), link to register
- [x] Register: inline errors and hints, entry to passkey sign-up
- [x] Register success: resend verification email
- [x] Email verification: success and failure states
- [x] Forgot password and reset password
- [x] Passkey sign-up: email, then 6-digit code, then create passkey
- [x] Temporary Home screen with log out (`HomeRoute`)

### Shared pieces

- [x] Layouts: `GoZipoAuthFormLayout` (phone portrait, phone landscape, tablet), `GoZipoResultLayout`, `GoZipoSnackbarScaffold`
- [x] Components: `GoZipoBrandLogo`, status icons, `GoZipoCodeTextField`
- [x] `DataError.Remote.GONE` (HTTP 410) with an "expired" message

### Passkeys

- [x] `PasskeyService` (domain) / `KtorPasskeyService` (data) for `/auth/passkey/*`
- [x] `PasskeyAuthenticator` (domain), implemented in the data layer:
  - Android: `CredentialManagerPasskeyAuthenticator` (Credential Manager 1.6.0)
  - iOS: `UnsupportedPasskeyAuthenticator` (passkey buttons hidden)
- [x] `CurrentActivityProvider`: weak reference to the foreground Activity for Credential Manager
- [x] Usernameless passkey login; no passkey on the phone routes to passkey sign-up
- [x] Cancelled passkey prompt reuses the sign-up request (the email code is single use)
- [x] Tested on a physical Android device: sign-up, login, passkey visible in Samsung Pass

### Fixes found while testing

| Issue | Fix | Where |
| --- | --- | --- |
| Resend verification returned 500, then hit the rate limit | `resendVerificationEmail` is `@Transactional` and checks "already verified" first | Backend `EmailVerificationService` |
| Nightly token cleanup would fail the same way | Both `cleanupExpiredTokens` jobs are `@Transactional` | Backend `EmailVerificationService`, `PasswordResetService` |
| No passkey on the phone showed the system "No available sign-in" sheet | `preferImmediatelyAvailableCredentials = true`, routes to passkey sign-up | App `CredentialManagerPasskeyAuthenticator` |
| Expired passkey request showed a generic error | 410 maps to `GONE` | App `HttpClientExt`, `DataErrorToUiText` |
| Logout left tokens in memory when the server call failed | Bearer tokens cleared whatever the result | App `KtorAuthService` |

### Wrap-up

- [x] Phase 2 work committed in the app and the backend
- [x] Resend verification re-tested after the transaction fix
- Not planned: "Use a passkey from another device" (QR). Each phone creates its own passkey.

---

## Phase 3: Multi-app setup + Rider app shell (done)

Goal: one repo produces two apps, GoZipo and GoZipo Rider, that share core modules and auth.

### Project layout

```
apps/
  common/              :apps:common - shell shared by both apps (MainViewModel, deep links, appCommonModule)
  customer/
    androidApp/        com.mobile.gozipo
    shared/            composition root, iOS framework CustomerShared
    iosApp/            Xcode project (GoZipo)
  rider/
    androidApp/        com.mobile.gozipo.rider
    shared/            composition root, iOS framework RiderShared
    iosApp/            Xcode project (GoZipo Rider)
core/                  shared by both apps
feature/
  auth/                shared by both apps
  ride/                Phase 4: domain + data for both apps
  booking/             Phase 6: customer screens
  driving/             Phase 7: rider screens
```

### Done

- [x] Customer app moved to `apps/customer/` (Android app, `shared`, Xcode project)
- [x] Convention plugins: each app sets its own `namespace`/`applicationId`; iOS framework named per app (`CustomerShared`, `RiderShared`)
- [x] `:apps:common` holds the shell both apps share: `MainViewModel` (session, push token, logout), deep link handler, `appCommonModule(appRole)`
- [x] Rider Android app (`com.mobile.gozipo.rider`, "GoZipo Rider") and rider `shared` module
- [x] Rider Xcode project (bundle ID `com.mobile.gozipo.rider`, display name "GoZipo Rider")
- [x] Rider app shell: auth screens + placeholder home with log out
- [x] Backend: `role` (CUSTOMER / RIDER) on the account, default CUSTOMER for existing rows; returned in the user object
- [x] Backend: register, login, passkey sign-up and passkey login take the app's role; an account of the other role gets 409 `ROLE_MISMATCH`
- [x] App: each app provides its `UserRole` through Koin; auth requests send it; 409 shows "This is a rider/customer account. Log in with the other app."
- [x] Role-aware login and register subtitles in the rider app
- [x] Builds verified: both Android APKs, both iOS frameworks, both Xcode projects (simulator)
- [x] `CLAUDE.md` updated with the new layout and commands

### Remaining

- [ ] Test on device: register and log in with the rider app; a customer account in the rider app (and the other way round) shows the wrong-app message
- [x] Firebase: rider app added to the Firebase project (`google-services.json`, `GoogleService-Info.plist`); Google Services plugin applied to the rider Android app
- [x] Backend push: removed the fixed `restrictedPackageName` (`com.mobile.gozipo`) so FCM reaches rider devices too
- [x] Passkeys in the rider app: rider package added to the backend's `assetlinks.json`. Both apps use the same debug key, so the existing `android:apk-key-hash` origin in `application.yml` already covers the rider app (release key still to add)
- [x] Role-aware email links as verified Android App Links (no chooser):
  - Backend: `UserEvent` carries the role; `EmailService` sends `https://<tunnel>/link/{customer|rider}/{verify|reset-password}?token=…`; dev `email.url` follows `webauthn.rp-id`; `AppLinkController` serves browser fallback pages; rider gets `handle_all_urls` in `assetlinks.json`
  - App: `APP_LINK_HOST` in `local.properties` feeds the manifest (`${appLinkHost}`) and `emailLinkBaseUrl(role)` for the nav deep links; each app claims only its own `/link/<role>/` paths; `gozipo://` and `localhost` links removed
- [ ] Verify App Links on a device with the tunnel up (`adb shell pm get-app-links <package>` shows `verified`), then tap a real customer and rider email
- [x] App icons: both apps use the `GoZipoBrandMark` look (lime background, black taxi); Android adaptive + monochrome, iOS 1024
- Moved out: rider vehicle profile (backend → Phase 4, onboarding screen → Phase 7); role check → Phase 4

## Phase 4: Backend ride domain + realtime

Goal: the backend owns ride state and pushes changes to both apps in real time.

### Foundations

- [x] Flyway owns the schema (`app/src/main/resources/db/migration`): `V1__baseline` (pre-Phase 4 tables, only runs on an empty database), `V2__rides`. Existing databases are baselined at version 1 automatically. Dev and prod use `ddl-auto: validate`
- [x] Role claim in the access token; `JwtAuthFilter` grants `ROLE_CUSTOMER` / `ROLE_RIDER`; ride endpoints use `@PreAuthorize`. Tokens issued before this have no role and get 403 on ride endpoints until refreshed
- [x] Rider vehicle profile (`rider_profiles`): set with `PUT /api/riders/me/vehicle` (the Phase 7 screen will call the same endpoint); riders without one can't go online or offer

### Rides

Backend module `ride` (`~/Development/Projects/ride-hailing/ride`). Rules: [vehicle types, fares and rounds](#ride-rules).

- [x] Ride domain model: ride, pickup and drop-off stops, vehicle type, recommended/offered/agreed fare, status, cash payment
- [x] Server state machine: `SEARCHING → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED`, `CANCELLED` until the trip starts; invalid transitions return 409 `INVALID_RIDE_STATE`
- [x] Bidding: customer offer (70%–300% of recommended), rider accepts the fare or counters in PKR 10 steps up to 150%, customer accepts one offer; server-side auto-accept
- [x] 60 s search rounds: when a round ends the customer gets `SEARCH_ROUND_ENDED`, raises the fare (new round) or cancels; searches left for 15 min are cancelled by the system
- [x] Ownership checks: only the ride's customer and assigned rider can read or change it (others get 404); one active ride per customer and per rider, enforced by partial unique indexes
- [x] Rider availability: online/offline and live location in Redis GEO (presence expires after 2 min without an update); requests go to online riders of the same vehicle type within 5 km
- [x] REST endpoints (below) and WebSocket `/ws/rides` (Chirp envelope `{type, payload}`, token in the handshake `Authorization` header, ping/pong)
- [x] FCM fallback: ride messages with push text go out as `RideEvent.PushRequested` (RabbitMQ) when the user has no open WebSocket; `NotificationRideEventListener` sends them
- [x] Tests: unit tests for fare rules, state transitions and the ride entity (`./gradlew :ride:test`, 19 tests); end-to-end check of the whole flow, ownership and roles against a local backend (49 checks)
- [ ] FCM fallback verified end to end with RabbitMQ and a real device
- [x] App side: realtime client in `core/data` adapted from the Chirp app: `RealtimeClient` (core/domain) / `KtorRealtimeClient`. One shared socket while collected, signed in, in the foreground and online; backoff reconnect (2 s → 30 s, reset after each connection); the expired-token handshake refreshes the session through the shared, mutex-guarded `refreshSession` (also used by the REST client). Verified live on the iOS simulator against a local backend with 1-minute tokens: connect, `RIDE_REQUESTED`, outgoing `LOCATION_UPDATE`, network drop, background/foreground, token expiry, sign-out
- [x] Keep-alive: the app pings every 20 s. Android sets it on the OkHttp engine (`pingInterval`, since OkHttp ignores the WebSockets plugin's `pingIntervalMillis`); iOS uses the plugin setting. The server pings every 30 s and drops sockets with no pong for 60 s; Tomcat answers the app's pings itself
- [x] Token refresh race: `refreshSession` takes the access token the caller saw rejected and returns the stored session without a network call when another caller already refreshed it. Without this, a REST 401 and a socket 1008 on the same expired token would send the rotated refresh token twice and the second failure would sign the user out
- [x] HTTP logging masks sensitive data by default (`HTTP_LOG_SENSITIVE_DATA` defaults to `false`)
- [ ] Check on a live Android socket that OkHttp sends the 20 s pings (server log or packet capture)

#### Ride rules

| | |
| --- | --- |
| Vehicle types | `MOTO`, `RICKSHAW`, `MINI`, `RIDE_AC`, `PREMIUM` |
| Recommended fare | `max(minimum, base + perKm·km + perMinute·min)`, rounded to PKR 10; per-type rates in `application.yml` (`ride-hailing.ride.fares`) |
| Route | Mapbox Directions `driving-traffic` (`MapboxRouteEstimator`, env `MAPBOX_ACCESS_TOKEN`, 3 s budget); without a token or on any Mapbox failure, straight line × 1.3 at 25 km/h (`StraightLineRouteEstimator`) |
| Customer offer | PKR 10 steps, ≥ 70% of recommended (and the type's minimum), ≤ 300% |
| Rider counter | PKR 10 steps, above the customer's offer, ≤ 150% of it |
| Search round | 60 s; raising the fare starts a new round and expires older offers |

#### Ride API

All under `/api`, bearer token required.

| Method and path | Role | Purpose |
| --- | --- | --- |
| `POST /rides/estimate` | Customer | Recommended fare and allowed range per vehicle type |
| `POST /rides` | Customer | Request a ride (fare, vehicle type, stops, auto-accept) |
| `POST /rides/{id}/fare` | Customer | Raise the fare / search again |
| `GET /rides/{id}/offers` | Customer | Pending offers in the current round |
| `POST /rides/{id}/offers/{offerId}/accept` · `/decline` | Customer | Pick or reject an offer |
| `GET /rides/open` | Rider | Open requests nearby for the rider's vehicle |
| `POST /rides/{id}/offers` | Rider | Accept the fare (`{}`) or counter (`{"amount": …}`) |
| `DELETE /rides/{id}/offers/{offerId}` | Rider | Withdraw an offer |
| `POST /rides/{id}/arrived` · `/start` · `/complete` | Rider | Trip steps |
| `POST /rides/{id}/cancel` | Both | Cancel (customer until the trip starts; rider once assigned) |
| `GET /rides/active` · `/rides/history?page=` · `/rides/{id}` | Both | Resync and history |
| `GET /riders/me/status` · `PUT /riders/me/vehicle` | Rider | Vehicle profile |
| `POST /riders/me/online` · `/offline` · `PUT /riders/me/location` | Rider | Availability (location normally over the WebSocket) |

WebSocket messages: in `LOCATION_UPDATE` (rider); out `RIDE_REQUESTED`, `RIDE_UNAVAILABLE`, `OFFER_RECEIVED`, `OFFER_UPDATED`, `RIDE_UPDATED`, `SEARCH_ROUND_ENDED`, `RIDER_LOCATION`, `ERROR`.

## Phase 5: Location + Mapbox

Goal: maps and location behind abstractions so features never touch Mapbox directly. Swapping to another provider (e.g. Google Maps) means a new `:core:map:<provider>` module implementing the same three interfaces, and changing one Koin module in each app's `initKoin`.

| Layer | Where | What |
| --- | --- | --- |
| Domain | `core/domain`: `location/`, `place/`, `route/` | `GeoPoint`, `LocationProvider`, `PlaceSearchService`, `RouteService` (no map SDK types) |
| Map UI contract | `:core:map:api` | `GoZipoMap`, `MapCameraState` (position, `isMoving`, `moveTo`, `fitBounds`), `MapMarker` (Compose content), `MapPolyline`, `MapProvider` |
| Mapbox | `:core:map:mapbox` (`mapboxModule`) | `MapboxMapProvider` (Android), `MapboxPlaceSearchService` (Search Box API), `MapboxRouteService` (Directions API); its own HTTP client, so our API key and bearer token never go to Mapbox |
| Device location | `core/data` | `FusedLocationProvider` (Android, Play services location); iOS stub |

- [x] Location abstraction in domain (availability/permission state, current location, location updates)
- [x] Android location implementation (`FusedLocationProvider`); iOS returns `UNSUPPORTED` for now
- [x] Map abstraction: camera, markers (the existing marker visuals as view annotations), route line; light/dark style follows the theme
- [x] Mapbox Android implementation (Maps SDK 11.32, `ndk27` artifacts, public Maven repo, no secret token). Token: `MAPBOX_ACCESS_TOKEN` in `local.properties`, optional; without it the map shows a placeholder instead of crashing
- [x] Place search, reverse geocoding and routing through Mapbox APIs, unit-tested against recorded responses (`./gradlew :core:map:mapbox:iosSimulatorArm64Test`)
- [x] Backend fares use Mapbox road distance and time, with the straight-line fallback (`MapboxRouteEstimatorTest`)
- [x] Temporary home of both apps (`HomeMapRoot` in `apps/common`): pickup pin + reverse geocode, tap for destination + route, recenter on location
- [ ] Device check with a real token: map tiles, puck, pin, search/route (smoke-tested on an emulator without a token only)
- [ ] iOS Mapbox map (deferred; needs the secret token and a Swift bridge) and iOS Core Location

## Phase 6: Customer ride flow

Goal: a customer can book and complete a ride. Depends on Phases 4 and 5.

- [ ] Map home replaces the temporary Home screen
- [ ] Pick pickup and drop-off (search and map pin)
- [ ] Choose vehicle type and see the route
- [ ] Offer a fare with the fare stepper (PKR 10 steps)
- [ ] See incoming rider offers and counter-offers in real time; accept one
- [ ] Track the rider: arriving, arrived, in progress
- [ ] Trip completion and fare summary (fare from the backend)
- [ ] Cancel ride (allowed states enforced by the backend)
- [ ] Ride history

## Phase 7: Rider ride flow

Goal: a rider can receive, bid on and complete rides. Depends on Phases 3, 4 and 5.

- [ ] Onboarding screen for the vehicle profile, required before going online
- [ ] Go online / offline
- [ ] Share location while online
- [ ] Receive nearby ride requests in real time
- [ ] Accept the customer's fare or counter-offer in PKR 10 steps
- [ ] Navigate to pickup; mark arrived; start trip
- [ ] Complete trip; fare and cash collection from the backend
- [ ] Earnings and ride history

## Phase 8: Hardening

- [ ] Error and offline handling across ride flows; WebSocket reconnect and resync from the server
- [ ] Process death and app restart in the middle of a ride
- [ ] Rate limits and abuse checks on ride endpoints
- [ ] Logging review: no tokens, credentials or personal data
- [ ] Tests: ViewModels, data layer, backend integration
- [ ] Accessibility and dark/light review against `design/`
- [ ] Passkey RP domain moved off the temporary tunnel; iOS passkeys
- [ ] Release builds: signing, ProGuard/R8 rules, Mapbox and Firebase config
