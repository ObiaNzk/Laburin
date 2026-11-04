# LaburIN API (Go)

Back de LaburIN: implementa los contratos de `../docs/api/` que consume la app Android.

**Stack:**
- Go con Gin;
- PostgreSQL (fuente de verdad), con SQLC para las queries y dbmate para las migraciones;
- Redis (índice geográfico de las ofertas activas);
- zerolog para logs, JWT HS256 para la sesión y bcrypt para las contraseñas.

## Arquitectura

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controller | `internal/api/controller` | Contrato HTTP: structs `…Req` / `…Resp` con tags `json`. Traduce errores a códigos HTTP (`internal/api/response`). |
| Service | `internal/service` | Reglas de negocio y validación. Declara las interfaces que necesita de cada repositorio. |
| Repository | `internal/repository` | `Postgres` (envuelve el código de SQLC) y `RedisGeo`. |

Además:
- **Modelo** (`internal/model`): tipos de dominio y errores (`ErrNotFound`, `ErrConflict`…), sin tags `json`.
- **Rutas y roles**: `internal/api/api.go`. El middleware `Authorized` responde 401 sin token válido y 403 si el rol no corresponde.
- **Redis es una vista derivada**: cada escritura va primero a Postgres y después a Redis. Si Redis falla, la app reintenta el mismo pedido (es idempotente). Al arrancar, `SyncGeoIndex` reconstruye el índice desde Postgres.

## Correrlo localmente

**Requisitos:** Go 1.26 o superior, PostgreSQL 17 y Redis.

- Con Homebrew: `brew install postgresql@17 redis`.
- O con Docker: `make up`. En ese caso la URL es `postgres://laburin:laburin@localhost:5432/laburin?sslmode=disable`.
- Herramientas: `go install github.com/amacneil/dbmate/v2@latest github.com/sqlc-dev/sqlc/cmd/sqlc@latest go.uber.org/mock/mockgen@latest`.

```bash
cp dev.env.example dev.env      # completá JWT_SECRET con: openssl rand -hex 32 (dev.env no se commitea)
source dev.env
createdb laburin && createdb laburin_test   # solo sin Docker
make migrate                    # aplica db/migrations
make seed                       # cuentas demo y ofertas ficticias; se puede correr varias veces
make run                        # API en http://localhost:8080
```

**Cuentas demo** (ficticias, solo para desarrollo): `postulante@demo.laburin` y `comercio@demo.laburin`, contraseña `demo1234`. El postulante demo ya tiene un CV cargado.

**Para que la app use este back**, agregá en `local.properties`:

```
laburin.apiBaseUrl=http://10.0.2.2:8080/
```

Sacá esa línea para volver al back falso en memoria. Los tests instrumentados de la app asumen el back falso.

## Tests

```bash
make test        # todos: unitarios + integración contra Postgres (laburin_test) y Redis (DB 15)
make test-unit   # solo los que no necesitan base (los de integración se saltean)
```

- **Service y controller:** tests unitarios con gomock y `require`, con bloques `// begin` / `// when` / `// then`.
- **Repository:** tests de integración contra Postgres y Redis reales.
- **`internal/api/api_test.go`:** recorre todo el flujo de la app por HTTP, con las tres capas reales.

Después de cambiar `db/query/*.sql` o una interfaz, corré `make generate` (SQLC y mocks).

## Endpoints

| Método | Ruta | Quién |
|---|---|---|
| POST | `/auth/login`, `/auth/signup/candidate`, `/auth/signup/business` | público |
| GET | `/offers?lat=&lng=&radiusKm=&category=`, `/offers/{id}` | público |
| GET | `/offers/mine` | comercio |
| PUT | `/offers/{id}` (publicar o editar) | comercio |
| POST | `/offers/{id}/close` | comercio dueño |
| GET | `/offers/{id}/applicants` | comercio dueño |
| PUT | `/applications/{id}/review` | comercio dueño |
| GET | `/applications/{id}/cv` | comercio dueño |
| PUT | `/applications/{id}` | postulante |
| GET | `/applications/mine` | postulante |
| PUT / GET | `/me/cv` | postulante |
| POST | `/me/push-token` | cualquier cuenta |
| GET | `/healthz` | público |

## Pendiente

- **Mandar las notificaciones push:** el token se guarda, pero faltan las credenciales de Firebase.
- **Abrir el PDF desde la app:** el endpoint ya existe, falta la pantalla.
- **Despliegue:** HTTPS, secretos en un gestor y no en archivos, y límites de tasa en `/auth/*`.
