# Minipassenger

Uber-style ride-hailing prototype that demonstrates a clean-architecture Kotlin Multiplatform foundation with Google Maps Compose, simulated drivers, and a basic booking flow.

---

## Features
- Fullscreen Google Map centered on the user’s current location once permission is granted.
- Synthetic drivers that move along road-snapped polylines and remain within a city-sized radius of the rider.
- Booking demo with assignment delays, cancellation, and state messaging (“Searching…”, “Driver en route”, “Driver arrived”).
- Driver marker colors:
  - **Black** — `DriverStatus.AVAILABLE`, casually patrolling.
  - **Yellow** — `DriverStatus.EN_ROUTE`, already assigned to your booking.
  - **Red** — `DriverStatus.BUSY`, finishing another passenger drop-off.
- Permission dialog fallback that guides the user when location access is denied.

---

## Architecture
| Layer / Module | Purpose |
| --- | --- |
| `domain` | Pure Kotlin use cases (`StartDriverSimulation`, `AssignDriverToPickup`, etc.), repositories, and models (`Driver`, `LocationPoint`). |
| `data` | Platform-facing implementations: fused location, Google Routes API (Ktor + kotlinx.serialization), `DriverRepositoryImpl`, `DriverRouteSimulator`. |
| `core:common` | Dispatchers abstraction for coroutine injection. |
| `core:ui` | Reusable UI helpers (`bitmapDescriptorFromVector`, permission checks). |
| `feature:map` | Jetpack Compose screen, ViewModel, navigation entry, booking panels. |
| `app` | Android host module (Hilt setup, Navigation 3 root). |

*Key patterns:* MVVM per feature, Clean Architecture boundaries, Hilt DI, Coroutines + Flow for streaming location and driver updates.

---

## Simulation Strategy
1. When the map is ready **and** the user location is known, `StartDriverSimulationUseCase` asks `DriverRepository` to seed:
   - Five nearby “addresses” (waypoints) around the rider.
   - Drivers anchored to those waypoints with road-aligned routes from Google Routes API v2.
2. `DriverRepositoryImpl` runs a long-lived coroutine (cancelable via `stopSimulation()`) that:
   - Interpolates along each driver’s route every 1.5s.
   - Re-queues routes when a patrol segment ends.
   - Switches navigation mode to **PICKUP** when a driver is assigned so the icon turns yellow.
3. Booking flow (`MapViewModel`):
   - Picks the nearest `DriverStatus.AVAILABLE` driver.
   - Waits a short delay before marking `assignedDriverId` and updating the UI state.
   - Monitors distance to mark “Driver arrived,” or reverts to available when cancelled.

---

## Driver Lifecycle
1. **Available** — spawned near the rider and looping along patrol waypoints until work arrives.
2. **En route** — after `AssignDriverToPickup` succeeds, the reservation is pinned and the marker turns yellow while `DriverRepository` feeds the pickup route.
3. **Busy** — once cancelled or released, drivers fall back to patrol mode, but those finishing other trips remain red until the simulator re-enters them into the available pool.

The Compose UI mirrors these states in marker colors/snippets, and `MapViewModel` only selects from the available subset to keep flows deterministic.

---

## Requirements & Setup
1. **Android Studio Hedgehog+** with JDK 17 and Android SDK 34.
2. **Google Maps / Routes API Key**
   - Enable Maps SDK for Android, Fused Location Provider, and **Routes API v2**.
   - In `local.properties`, add:  
     `MAPS_API_KEY=YOUR_WEB_SERVICE_KEY`
   - Ensure billing is enabled (Routes API requires it).
3. **Gradle Sync**
   ```bash
   ./gradlew :app:assembleDebug
   ```

> Note: Routes API calls require network access; the simulator falls back to synthetic polylines if the API returns errors.

---

## Testing
Unit tests cover location/driver use cases plus the repository contract. Run:
```bash
./gradlew test
```
If you’re missing a local JDK, install Temurin 17 and retry.

---

## Future Enhancements
- Destination picker + fare estimator UI.
- Persisted ride history and analytics events.
- Gradle-managed Secrets Manager instead of `local.properties`.
- UI tests for permission/booking flows.

Enjoy exploring Minipassenger! Reach out if you need any tweaks or further breakdowns. 🚗💨


