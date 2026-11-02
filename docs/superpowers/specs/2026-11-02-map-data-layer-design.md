# Pantalla Mapa: capas de datos, dominio y ViewModel (RF-01)

**Fecha:** 2026-11-02 · **Rama:** `feature/map-data-layer`

## Objetivo

Reemplazar los datos hardcodeados de `MapScreen` / `NoConnectionMapScreen` por una implementación MVVM + Clean Architecture offline-first, siguiendo las convenciones del material de la cátedra (ver `CLAUDE.md`). Es la primera pantalla y monta la base (Hilt, Room, repositorio, ViewModel) que van a reutilizar las siguientes.

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| API Go | No existe aún. Definimos el contrato (`docs/api/offers.md`) y usamos `FakeOfferRemoteDataSource` detrás de una interfaz. También se incluye la implementación Retrofit, que se activa cambiando una línea de Hilt. |
| Ubicación | Fused Location, una lectura al abrir o refrescar (RNF-07). Si se deniega o falla, se usa un centro por defecto con aviso. La zona manual (geocoding) queda para otra iteración. |
| Idioma del código | Inglés, para mantener consistencia con el código existente. |
| Rubros | Lista fija en el dominio (`Category`). |
| Proveedor de mapa | El dominio es agnóstico: `GeoLocation` es un tipo propio. El borde de la UI es `DisplayMap(offers, center, …)` y el de la ubicación es la interfaz `LocationProvider`. |
| Serialización | kotlinx.serialization. |
| Tests | JUnit4 + kotlinx-coroutines-test + Turbine, con fakes en lugar de mocks. |

## Paquetes

```
di/            DatabaseModule, NetworkModule, DataModule (binds), DispatchersModule
domain/model   GeoLocation, Category, Offer, NearbyOffer, BoundingBox, RefreshResult
domain/geo     Haversine (distancia + bbox), Kotlin puro
domain/repository  OfferRepository
domain/usecase     ObserveNearbyOffersUseCase, RefreshNearbyOffersUseCase
data/local     LaburinDatabase, OfferDao, OfferEntity, SyncMetadataEntity/Dao, Converters
data/remote    OfferRemoteDataSource, FakeOfferRemoteDataSource, RetrofitOfferRemoteDataSource, OfferApi, OfferDto
data/mapper    OfferMappers.kt
data/repository OfflineFirstOfferRepository
data/location  LocationProvider (interface en domain), FusedLocationProvider
data/connectivity ConnectivityObserver (interface en domain), AndroidConnectivityObserver
data/preferences  FiltersRepository (interface en domain), DataStoreFiltersRepository
ui/screens/map MapRoute, MapContent, MapViewModel, MapUiState, MapEffect
```

Las interfaces que el ViewModel o los casos de uso necesitan (`LocationProvider`, `ConnectivityObserver`, `FiltersRepository`) viven en `domain/` y sus implementaciones en `data/`.

## Flujo de datos

**Lectura:** la UI solo lee de Room. El `MapViewModel` combina filtros (DataStore) + texto + centro y hace `flatMapLatest` sobre `ObserveNearbyOffersUseCase`. El caso de uso calcula el bounding box, pide `repository.observeInArea(bbox, category, query)` (filtro SQL) y luego calcula la distancia haversine, filtra por radio exacto y ordena por distancia.

**Refresco** (al abrir, al cambiar un filtro y al tocar Reintentar): `RefreshNearbyOffersUseCase` llama a `repository.refresh(center, radiusKm, category)`, que llama a `remote.getOffers(...)`. Después, en una `@Transaction`, borra las ofertas del bbox (y del rubro, si hay filtro) que el servidor ya no devuelve, hace upsert de las devueltas y guarda `lastSyncedAt`. Devuelve `RefreshResult.Success | NetworkError | ServerError`. Siempre gana el servidor. La `CancellationException` se relanza.

**Ubicación:** `FusedLocationProvider` tiene un timeout de 8 s. Si no obtiene ubicación, devuelve `Unavailable` y se usa el centro por defecto.

**Conectividad:** `ConnectivityObserver.isOnline: Flow<Boolean>` (callbackFlow sobre ConnectivityManager). Al pasar de offline a online se dispara un refresco.

## Estado

```kotlin
data class MapUiState(
    val query: String = "",
    val radiusKm: Int = 2,
    val category: Category? = null,
    val center: GeoLocation = DEFAULT_CENTER,
    val locationStatus: LocationStatus = LocationStatus.Unknown,
    val isOnline: Boolean = true,
    val lastSyncedAt: Instant? = null,
    val isRefreshing: Boolean = false,
    val offers: OffersState = OffersState.Loading,
)
sealed interface OffersState { Loading; Content(list); Empty; Unavailable(isOffline) }
sealed interface MapEffect { NavigateToOffer(id); NavigateToScan; NavigateToPublish; ShowMessage(MapMessage) }
```

- Sin conexión y con caché: `Content` con `isOnline = false`. `MapContent` muestra la variante "sin conexión · datos de hace X min" y `NoConnectionMapScreen` se absorbe.
- Sin caché y con el refresco fallido: `Unavailable(isOffline)`, que muestra "Necesitás conexión" o "No pudimos cargar", con botón Reintentar.
- Si falla el refresco y hay caché: se mantiene `Content` y se emite `ShowMessage(RefreshFailed)`.
- `Empty` ofrece "Ampliar a 5 km" y "Ver todos los rubros".

## Contrato API (propuesto)

`GET /offers?lat={lat}&lng={lng}&radiusKm={r}&category={c?}` devuelve `200 [OfferDto]`, con `OfferDto { id, title, category, description, schedule, jobType, businessName, address, latitude, longitude, status, updatedAt (ISO-8601) }`. Solo devuelve ofertas `ACTIVE`.

## Tests

- **JVM:** `Haversine`, `ObserveNearbyOffersUseCase`, mappers, `OfflineFirstOfferRepository` (con DAO falso y remoto falso) y `MapViewModel` (con Turbine, cubriendo cada estado).
- **Instrumentado:** `OfferDao` con Room en memoria.

## Fuera de alcance

Navegación (NavHost), detalle, QR, sesión, zona manual, límite de caché (RNF-08) y `OfertaVista`.

## Plan de implementación

1. Dependencias + plugins (KSP, Hilt, serialization), `LaburinApplication` y verificar el build.
2. Dominio: modelos, `Haversine` (TDD), interfaces y casos de uso (TDD con fakes).
3. Data: entities, DAO, DB, mappers (TDD) y remoto falso + Retrofit.
4. `OfflineFirstOfferRepository` (TDD con fakes).
5. Ubicación, conectividad, DataStore y módulos Hilt.
6. `MapViewModel` + `MapUiState` (TDD con Turbine).
7. UI: `MapRoute`/`MapContent`, adaptar `DisplayMap`/`OfferItem`, absorber `NoConnectionMapScreen` y actualizar `MainActivity`.
8. Test instrumentado del DAO (compila; se corre si hay un dispositivo) y verificación final: `testDebugUnitTest`, `assembleDebug` y `lint`.
