# GoZipo

GoZipo is a ride-hailing project built with **Kotlin Multiplatform** and **Compose Multiplatform**. One repository builds two apps for **Android and iOS**, with the UI fully shared in Compose:

| App | Who it's for |
|---|---|
| **GoZipo** | Customers who book rides
| **GoZipo Rider** | Riders (drivers) who take rides

Main features:

- Email/password and passkey sign-in, with separate customer and rider accounts
- Live map with place search, routes and turn-by-turn navigation for riders
- Customers name their fare and riders send offers (fares in PKR)
- Real-time ride updates over WebSocket, plus push notifications
- Card payments with Stripe (Visa, Mastercard, Google Pay, Apple Pay); cash still works
- Rider vehicle management and earnings history
- Light and dark themes

The backend is a separate Spring Boot (Kotlin) project.

## Screenshots

### GoZipo (customer)

<table>
  <tr>
    <td align="center"><img src="screenshot/customer-app/android-login.jpeg" width="200" alt="Login"/><br/>Login</td>
    <td align="center"><img src="screenshot/customer-app/android-select-fare.jpeg" width="200" alt="Select vehicle and fare"/><br/>Select vehicle and fare</td>
    <td align="center"><img src="screenshot/customer-app/android-search-driver.jpeg" width="200" alt="Searching for a rider"/><br/>Searching for a rider</td>
    <td align="center"><img src="screenshot/customer-app/android-driver-on-the-way.jpeg" width="200" alt="Rider on the way"/><br/>Rider on the way</td>
  </tr>
  <tr>
    <td align="center"><img src="screenshot/customer-app/android-driver-coming.jpeg" width="200" alt="Rider arriving"/><br/>Rider arriving</td>
    <td align="center"><img src="screenshot/customer-app/android-you-arrived.jpeg" width="200" alt="Trip complete"/><br/>Trip complete</td>
    <td align="center"><img src="screenshot/customer-app/android-stripe-pay-sheet.jpeg" width="200" alt="Pay with card"/><br/>Pay with card</td>
    <td></td>
  </tr>
</table>

### GoZipo Rider

<table>
  <tr>
    <td align="center"><img src="screenshot/rider-app/rider-login.png" width="200" alt="Login"/><br/>Login</td>
    <td align="center"><img src="screenshot/rider-app/rider-vehicle.png" width="200" alt="Vehicle"/><br/>Vehicle</td>
    <td align="center"><img src="screenshot/rider-app/rider-waiting.png" width="200" alt="Waiting for requests"/><br/>Waiting for requests</td>
  </tr>
  <tr>
    <td align="center"><img src="screenshot/rider-app/ride-accept-ride.png" width="200" alt="Ride request"/><br/>Ride request</td>
    <td align="center"><img src="screenshot/rider-app/rider-pickup.png" width="200" alt="Pickup"/><br/>Pickup</td>
    <td align="center"><img src="screenshot/rider-app/rider-earnings.png" width="200" alt="Earnings"/><br/>Earnings</td>
  </tr>
</table>

## Tech stack

| Area | Libraries |
|---|---|
| Language and platforms | Kotlin 2.4, Kotlin Multiplatform (Android, iOS `arm64` and simulator `arm64`) |
| UI | Compose Multiplatform 1.12, Material 3, Navigation Compose (type-safe routes), Coil 3 |
| Architecture | Clean architecture (domain / data / presentation), MVI-style ViewModels, Koin 4 for dependency injection |
| Networking | Ktor 3 (REST and WebSocket), kotlinx.serialization, bearer auth with token refresh |
| Storage | DataStore |
| Maps | Mapbox Maps SDK 11, Mapbox Navigation SDK 3 (rider app), Mapbox Search and Directions APIs |
| Payments | Stripe PaymentSheet, FlowController and Embedded Payment Element (customer app) |
| Push | Firebase Cloud Messaging (Android and iOS) |
| Sign-in | Passkeys: Credential Manager (Android), AuthenticationServices (iOS) |
| Device | moko-permissions, Play Services location (Android), Core Location (iOS) |
| Other | kotlinx.coroutines, kotlinx.datetime, Kermit logging, BuildKonfig |
| Build | Gradle with convention plugins (`build-logic`), version catalog, AGP 9 |
| iOS shell | SwiftUI; Swift bridges for the Swift-only Mapbox and Stripe SDKs |
| Testing | kotlin.test, kotlinx-coroutines-test, Ktor MockEngine |

## Project structure

```
GoZipo/
├── apps/
│   ├── customer/            # GoZipo (customer) app
│   │   ├── shared/          #   composition root: navigation, Koin setup, iOS framework "CustomerShared"
│   │   ├── androidApp/      #   Android launcher
│   │   └── iosApp/          #   iOS launcher (SwiftUI) + Stripe bridge
│   ├── rider/               # GoZipo Rider app
│   │   ├── shared/          #   composition root, iOS framework "RiderShared"
│   │   ├── androidApp/
│   │   └── iosApp/          #   iOS launcher + Mapbox Navigation bridge
│   ├── common/              # App shell shared by both apps (session, deep links, push token)
│   └── ios-common/          # Swift code used by both iOS apps (Mapbox bridge, push setup)
│
├── core/
│   ├── domain/              # Pure Kotlin models, interfaces and the Result/error model
│   ├── data/                # Ktor client, session storage, realtime socket, push, platform code
│   ├── presentation/        # Shared UI utilities (UiText, events, permissions)
│   ├── designsystem/        # GoZipoTheme, typography, colors and shared components
│   ├── map/
│   │   ├── api/             # Provider-free map contract (GoZipoMap, camera, markers)
│   │   ├── mapbox/          # Mapbox implementation, search and directions
│   │   └── navigation/      # Turn-by-turn navigation (rider app only)
│   └── payment/
│       ├── api/             # Provider-free checkout contract
│       └── stripe/          # Stripe implementation (customer app only)
│
├── feature/
│   ├── auth/                # Sign-in, sign-up, passkeys, password reset (domain / data / presentation)
│   ├── ride/                # Shared ride models, REST, realtime events and ride UI
│   ├── booking/             # Customer ride screens (presentation)
│   ├── driving/             # Rider screens: vehicle, requests, trip, earnings (presentation)
│   └── payment/             # Card payments and saved cards (domain / data / presentation)
│
├── build-logic/             # Gradle convention plugins
├── gradle/libs.versions.toml
├── design/                  # UI reference images
└── screenshot/              # App screenshots used in this README
```

Each feature is split into `domain` (interfaces and models), `data` (Ktor implementations) and `presentation` (Compose screens and ViewModels). Mapbox and Stripe are used only inside their `core` modules, so either provider can be replaced without touching the features.
