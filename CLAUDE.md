# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

LaburIN is an Android app (UADE, "Desarrollo de Aplicaciones 1") that digitizes the "Se busca empleado" shop sign. Neighborhood shops (*comercio*) publish job offers with a printable QR. Job seekers (*postulante*) find offers on a map within a radius, filtered by category (*rubro*), and apply with a digital CV. The shop sees the CV with contact details hidden until it approves the *Match*.

This repo is **only the Android client**. The REST API is a separate Go backend and is out of scope here. The app talks to it through Retrofit. The source of truth for requirements is the course "Pre Entrega 1" document (RF-01..04, RNF-01..09). Its key points are summarized below.

## Commands

Use the Gradle wrapper (Gradle 9.5, AGP 9.x, Kotlin 2.2, JDK 11 target):

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew testDebugUnitTest             # JVM unit tests
./gradlew testDebugUnitTest --tests "com.example.laburin.SomeTest.someMethod"   # single test
./gradlew connectedDebugAndroidTest     # instrumented/Compose UI tests (needs device)
./gradlew lint                          # Android lint
```

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Some libraries are still hardcoded as string coordinates in `app/build.gradle.kts` (osmdroid, CameraX, material-icons-extended). Add new dependencies to the catalog.

## Current state of the code

- Single module `:app`, package `com.example.laburin`, `minSdk 31`, `compileSdk/targetSdk 37`.
- **Map (RF-01) and Offer detail (RF-02, guest/full) are the reference implementations** of the full stack. Copy their patterns for new screens: `ui/screens/map/` and `ui/screens/detail/` (Route/Content/ViewModel/UiState), `domain/` (models, `OfferRepository`, use cases, `Haversine`), `data/` (Room, remote, mappers, `OfflineFirstOfferRepository`, location, connectivity, DataStore), `di/AppModules.kt`. Design doc: `docs/superpowers/specs/2026-11-02-map-data-layer-design.md`.
- **The Go API doesn't exist yet.** `DataModule` binds `FakeOfferRemoteDataSource`. The contract is in `docs/api/offers.md`. `RetrofitOfferRemoteDataSource` is ready; swap the binding when the API exists.
- Interfaces the ViewModel/use cases need (`LocationProvider`, `ConnectivityObserver`, `FiltersRepository`) live in `domain/repository`, with implementations in `data/`. The repository depends on `OfferLocalDataSource`/`OfferRemoteDataSource` interfaces so it is JVM-testable with fakes (see `app/src/test/.../fakes`).
- **Navigation**: single `LaburinNavHost` (`ui/navigation/`) with type-safe `@Serializable` destinations. ViewModels read their arguments from `SavedStateHandle` by property name (constants in the destination's companion), not with `toRoute()`, so they stay JVM-testable. Routes expose `onNavigateTo*` lambdas, and only the NavHost knows the `NavController`.
- **Auth/session**: `DefaultAuthRepository` (implements `AuthRepository` and `SessionRepository`) talks to `AuthRemoteDataSource` and persists the session via `KeystoreSessionStore` (AES-GCM, key in the Android Keystore, RNF-09). Contract: `docs/api/auth.md`.
- **Fake backend**: `data/fakebackend/FakeBackend` is a single in-memory server shared by the fake remotes, so flows connect end to end (fictional demo accounts in `docs/api/auth.md`). Swap each binding in `di/AppModules.kt` to Retrofit once the Go API exists.
- **App shell**: `LaburinNavHost` owns the bottom bar (shown only on top-level destinations, "Ingresar"/"Perfil" from `AppShellViewModel`). Login/sign-up return to where the user was (`leaveAuthFlow`).
- **Applying (RF-02, RNF-03)**: `ApplyToOfferUseCase` (account + CV required, no duplicates) → `DefaultApplicationRepository` writes the application and a `pending_actions` row in one transaction → `WorkManagerSyncScheduler` enqueues `SyncWorker` (needs a connection, exponential backoff) → `PendingActionsSyncer` sends the outbox in order; the server always wins. Contract: `docs/api/applications.md`. Room is at **version 2** with a real `MIGRATION_1_2` (tested with `MigrationTestHelper`).
- **WorkManager** is initialized on demand with `HiltWorkerFactory` (`LaburinApplication`, default initializer removed in the manifest). E2E tests must call `WorkManagerTestInitHelper.initializeTestWorkManager` and satisfy constraints with the test driver.
- **Publishing (RF-03)**: `PublishOfferUseCase` (business account + complete draft) → `DefaultMyOffersRepository` stores a `published_offers` row (`PENDING_PUBLISH`) plus a `PUBLISH` outbox action. The same `PendingActionsSyncer` sends it. The form can be filled without an account: publishing without one goes to `BusinessSignUp` and `leaveAuthFlow` returns to the kept form. The QR encodes `OfferLink.build(id)` = `laburin://offer/{id}` (ZXing). Contract: `docs/api/business.md`. Room is at **version 3**.
- Shared form components live in `ui/components/Forms.kt`.
- Still **UI-only, legacy**: `ScanScreen`. Not wired yet: QR scanning, applicant review (Match).
- The map uses **osmdroid** (OpenStreetMap). `ui/components/DisplayMap.kt` is the only file that may import it. It takes `MapMarker`/`GeoLocation`, so switching provider touches only that file. OSM tile policy needs a non-default user agent (`"Laburin"`).
- Strings for new UI go in `res/values/strings.xml` (the legacy screens still hardcode them).

### Local environment gotchas

- `local.properties`, `.gradle/`, `.idea/` and `build/` are git-ignored and machine-specific. Android Studio regenerates `local.properties` with your `sdk.dir`.
- Use the JBR bundled with Android Studio as `JAVA_HOME`: the system JDK is newer than what Gradle supports.
- Sources live in `src/*/kotlin/` (not `java/`).

## Target architecture (from the planning doc)

MVVM + Clean Architecture. Each layer depends only on the one below it:

- **Presentation**: Compose screens plus ViewModels that expose screen state via `StateFlow`. Every screen covers loading / content / empty / error / offline / permission-denied states.
- **Domain**: models (Oferta, Comercio, Postulante, Postulación) and use cases (search nearby offers, apply, publish offer, review applicant). Business rules live here, for example "the CV must be uploaded before applying".
- **Data**: repositories combining Room (local) and Retrofit (remote), DTO ↔ entity ↔ domain mappers, and WorkManager sync.

Offline-first rules that shape the data layer:
- **The UI reads only from Room.** The network updates Room, and Room's `Flow` notifies the UI.
- User actions (apply, review, publish) are written to Room as pending (`AcciónPendiente` queue) and sent by WorkManager when the network is back. They must never be lost on app close or network loss (RNF-03).
- `Postulación.id` is a **client-generated UUID** so that retries are idempotent.
- On conflict, **the server always wins**. A server rejection, for example HTTP 409 for a closed offer, is shown to the user with its reason. 5xx errors retry with backoff.
- Stale cache is shown with a "datos de hace X min" notice and refreshed in the background.
- Distance only filters the map. A viewed or scanned offer (`OfertaVista`, local only) can be applied to from anywhere.
- Browsing the map needs no account. Viewing offer details and applying need one.

### Course conventions (from the course material — follow these)

- **DI with Hilt**: `@HiltViewModel` + `@Inject constructor`, `hiltViewModel()` in composables. No hand-written ViewModelFactories. One app-wide Room database provided via DI.
- **ViewModel state**: `private val _uiState = MutableStateFlow(XUiState())` / `val uiState = _uiState.asStateFlow()`, updated with `_uiState.update { it.copy(...) }`; repo flows via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. `XUiState` is a data class for independent fields (search, filters) plus a sealed result (loading / content / error). A recoverable error keeps the previous cached data. Don't use multiple boolean flags, and don't model a network error as an empty list.
- **One-shot events** (navigation, snackbar) go through `MutableSharedFlow<UiEffect>` exposed with `asSharedFlow()`, collected in `LaunchedEffect`. Anything important must also live in state or data, because `replay=0` can drop it.
- **Compose**: split each screen into a `XRoute` (gets the VM, `collectAsStateWithLifecycle()`, never `collectAsState()`) and a stateless `XContent(state, onAction...)` that is previewable and testable with fabricated states. Hoist state. Keep logic, DB and navigation out of `onClick` bodies and composable bodies.
- **Domain is pure Kotlin**: no Android imports. Repositories are interfaces in domain with implementations in data. Reads return `Flow`, writes are `suspend`. Use cases expose `operator fun invoke`.
- **Data**: `LocalDataSource` (Room) + `RemoteDataSource` (Retrofit) coordinated by the repository. Entities and DTOs never leave the data layer. Use extension mappers `toDomain()` / `toEntity()`.
- **Room**: DAO reads return `Flow`, one-shot reads/writes are `suspend`. Choose the conflict strategy deliberately. Filter in SQL rather than the UI, using `:param` and never string concatenation. Use `@Transaction` for compound ops, `TypeConverter`s for `Instant`↔`Long`, and `exportSchema = true` with real migrations (never a blanket `fallbackToDestructiveMigration`). Use KSP for `room-compiler`.
- **Coroutines**: use `viewModelScope`. Never swallow `CancellationException` in `catch`.
- **Tests**: test ViewModels against a fake repository, test the DAO and migrations (`MigrationTestHelper`), and test `XContent` with fabricated states (Compose `createComposeRule`, instrumented).
- **Instrumented test infra**: the runner is `HiltTestRunner` (androidTest); it also initializes a test WorkManager because `HiltTestApplication` doesn't. End-to-end tests use `@HiltAndroidTest` with the real graph. Device location is pinned by `FixedLocationModule`, which replaces `LocationModule` via `@TestInstallIn`, so tests never depend on where the device is. E2E helpers live in `androidTest/.../ui/navigation/E2eRobot.kt`; E2E tests log out in `@Before` because the session persists on the device. Run a single class with `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fqcn>`.

Other constraints: session tokens are encrypted with the Android Keystore and never stored in plain text (RNF-09). User preferences such as radius and category go in DataStore. Location is requested only when the map opens or refreshes, with no background tracking (RNF-07). Applicant phone and email stay hidden until the server confirms the Match (RNF-05).

## Workflow conventions

- Branches: `main` (protected), `develop` (integration), `feature/<name>`. PRs into `develop` need one review from another team member.
- Commits follow Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`). Versioning/changelog follows Keep a Changelog 1.1.0.
- **Strict TDD**: write the test cases first, add a compiling stub (`TODO()` / empty), run and confirm they fail *for the right reason*, then implement the minimum to pass. For tests that pass immediately against existing code, prove they can fail with a temporary mutation. Report anything left untested.
- The team is building the data/ViewModel layer **one screen at a time**. Follow the patterns taught in the course material and ask the user when a best practice isn't covered there.
- Never put real personal data (names, emails, DNI, phones, addresses) in sample data, previews or tests. Use placeholders like `[NOMBRE]`, `[EMAIL]`.
