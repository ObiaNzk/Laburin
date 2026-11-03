# API · Ofertas (propuesta para el back en Go)

Contrato que consume la app. Mientras la API no exista, la app usa `FakeOfferRemoteDataSource`, que respeta este mismo contrato. Para usar la API real, cambiá en `di/AppModules.kt` el binding de `OfferRemoteDataSource` a `RetrofitOfferRemoteDataSource` y configurá `API_BASE_URL` en `app/build.gradle.kts`.

## `GET /offers`

Devuelve las búsquedas **activas** dentro de un radio.

| Query param | Tipo | Requerido | Descripción |
|---|---|---|---|
| `lat` | double | sí | Latitud del centro |
| `lng` | double | sí | Longitud del centro |
| `radiusKm` | int | sí | Radio en km (la app usa 2, 5, 10, 15) |
| `category` | string | no | `GASTRONOMY`, `MECHANICS`, `STOCK`, `CLOTHING`. Si se omite, devuelve todos los rubros. |

**200 OK**

```json
[
  {
    "id": "f-1",
    "title": "Ayudante de taller",
    "category": "MECHANICS",
    "description": "…",
    "schedule": "Lunes a viernes, 9 a 17 h",
    "jobType": "Jornada completa",
    "businessName": "[COMERCIO]",
    "address": "[DIRECCION]",
    "latitude": -34.608,
    "longitude": -58.3712,
    "status": "ACTIVE",
    "updatedAt": "2026-11-01T12:00:00Z"
  }
]
```

### Reglas que la app asume

- El servidor filtra por **distancia real** (círculo) y por rubro. Solo devuelve `status = ACTIVE`.
- `updatedAt` está en ISO-8601 UTC.
- La respuesta es la verdad del servidor para esa zona: la app borra de su caché las ofertas de la zona que no vengan en la respuesta.
- La app descarta las ofertas con un `category` desconocido o con una fecha inválida, sin romper la lista.
- La app ignora los campos extra (`ignoreUnknownKeys`), así que agregar campos no rompe clientes viejos.
- Este endpoint no requiere autenticación: el mapa se puede explorar sin cuenta (RF-01). No debe exponer datos personales de los dueños de los comercios.
- Si la API responde un error (cualquier código fuera del rango 2xx), la app mantiene la caché y avisa al usuario.

## `GET /offers/{id}`

Devuelve una búsqueda por id **en cualquier estado**. La usa el Detalle para refrescar la oferta al abrirla.

- **200 OK**: un `OfferDto` (mismo formato que arriba). Si `status != ACTIVE`, la app la borra de su caché y muestra "Esta búsqueda ya no está disponible".
- **404 Not Found**: el id no existe. La app hace lo mismo que con una búsqueda cerrada.
- **Cualquier otro error**: la app mantiene la versión cacheada, si la tiene.
- No requiere autenticación. La app oculta el detalle a quien no tiene sesión, pero **cuando exista el login conviene que el servidor tampoco devuelva los campos privados sin token**: hoy la ocultación es solo visual.
