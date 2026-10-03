# GoZipo: Progress and Phase Plan

Last updated: 2026-10-02

Tick a box (`- [x]`) when an item is done and verified. Add new items under the phase they belong to.

**Status at a glance**

| Phase | Scope | Status |
| --- | --- | --- |
| 0 | Project foundation | Done |
| 1 | Design system | Done |
| 2 | Customer auth UI + Android passkeys | Done |
| 3 | Multi-app setup + Rider app shell | In progress |
| 4 | Backend ride domain + realtime | Not started |
| 5 | Location + Mapbox | Not started |
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
  - App: `~/development/Chirp/composeMultiplatform/`
  - Backend: `~/development/Chirp/springboot/chirp`
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

## Phase 3: Multi-app setup + Rider app shell

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
- [ ] Firebase: add `com.mobile.gozipo.rider` to the Firebase project, put its `google-services.json` in `apps/rider/androidApp/`, then apply the Google Services plugin there
- [ ] Passkeys in the rider app: add the rider package and its signing SHA-256 to the backend's `assetlinks.json`
- [ ] Rider email links: the backend sends one verify/reset URL, which the customer app claims. Decide on role-aware links (for example a separate path or host for rider emails) and add the rider intent filters
- [ ] Rider app icon and brand variant (it currently reuses the customer icon)
- [ ] Rider onboarding fields (vehicle type, vehicle details), or move this to Phase 7
- [ ] Role in the JWT (or a server-side role check) for Phase 4 ride endpoints

## Phase 4: Backend ride domain + realtime

Goal: the backend owns ride state and pushes changes to both apps in real time.

- [ ] Ride domain model: ride, pickup and drop-off stops, vehicle type, offered fare, status
- [ ] Ride state machine on the server (requested, offers, accepted, rider arriving, in progress, completed, cancelled); invalid transitions rejected
- [ ] Bidding: customer offer, rider accept or counter-offer in PKR 10 steps, customer accepts one offer
- [ ] Ownership checks: only the ride's customer and assigned rider can read or change it
- [ ] REST endpoints for create ride, offers, accept, cancel, status updates, ride history
- [ ] WebSocket endpoint with JSON messages, reusing the Chirp backend implementation
- [ ] FCM fallback when the WebSocket is not connected
- [ ] App side: realtime client in `core/data` reusing the Chirp app implementation (connect, reconnect, auth)
- [ ] Tests for state transitions and ownership rules

## Phase 5: Location + Mapbox

Goal: maps and location behind abstractions so features never touch Mapbox directly.

- [ ] Location abstraction in domain (current location, location updates, permission state)
- [ ] Android location implementation; iOS later
- [ ] Map abstraction: camera, markers (using the existing marker visuals), route line
- [ ] Mapbox Android implementation with the public token (token kept out of source control)
- [ ] Geocoding / place search and routing (distance, duration) through Mapbox APIs
- [ ] iOS Mapbox (deferred; needs the secret token)

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
