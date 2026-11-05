# CLAUDE.md (backend)

This file guides Claude Code in `backend/`, the Go API behind the LaburIN Android app. The HTTP contract is defined in `../docs/api/*.md`, and the app's Retrofit DTOs depend on it. Change a response only together with the docs and the app.

## Commands

```bash
source dev.env                 # DATABASE_URL, REDIS_ADDR, JWT_SECRET (copy dev.env.example; never commit dev.env)
make migrate | make seed | make run
make test                      # unit + integration (needs Postgres laburin_test and Redis)
make test-unit                 # no database; integration tests skip themselves
make generate                  # after editing db/query/*.sql or a mocked interface
go test -run TestName ./internal/service/
```

## Stack (don't introduce alternatives)

- **HTTP:** Gin.
- **Data:** PostgreSQL through SQLC with pgx/v5. dbmate migrations live in `db/migrations`. Redis via go-redis v9.
- **Libraries:** zerolog for logs, `guregu/null/v5` for nullable values, golang-jwt v5, bcrypt.
- **Tests:** testify `require` plus `go.uber.org/mock`.
- **Money:** salaries are whole pesos in `int64` / `bigint`. There is no fractional money in this domain, so `shopspring/decimal` isn't used. Revisit this if cents ever appear.

## Layers

`controller → service → repository`. Each layer declares the interfaces it consumes; implementations satisfy them structurally.

- **Controllers** (`internal/api/controller`):
  - They own the wire contract: `XReq` / `XResp` structs with `json` / `binding` tags.
  - Never serialize `model` types.
  - Map errors with `response.WriteError`.
  - Validating constructors in `model` (`NewSalary`, `NewOfferContent`) may be called here.
- **Services** (`internal/service`):
  - Business rules: ownership (403), closed offers (409), CV required (422), contact hidden until approval (RNF-05).
  - Return `model.Err*`; never import Gin.
- **Repository** (`internal/repository`):
  - One `Postgres` type wraps the SQLC `Queries`. `mapError` turns `pgx.ErrNoRows` into `ErrNotFound` and a unique violation into `ErrConflict`.
  - `RedisGeo` is the derived geo index.
  - Write to Postgres first, then Redis. Startup calls `OfferService.SyncGeoIndex`.
- **Generated code:** `internal/dbmodel` (SQLC) and the `mocks/` folders. Don't edit them by hand.

## Contract rules the app relies on

- Offer and application ids are client-generated UUIDs; PUTs are idempotent.
- A non-UUID id in a lookup returns 404; in a create it returns 400.
- Timestamps are ISO-8601 UTC in whole seconds (`isoTime`).
- `salary` is always present, either `null` or `{min, max|null, period}`.
- Applicant `status` `SENT` goes on the wire as `PENDING_REVIEW`.
- `OFFER_CLOSED` is derived in `ApplicationService.Mine`.
- Closing is idempotent and returns 204.

## Conventions (from the team's reference Go repos)

- Keep each function signature on one line.
- Bind multi-field struct literals to a local variable before passing them.
- No trivial constructors for domain structs.
- Tests:
  - Name them `TestMethod_Scenario`, with `// begin` / `// when` / `// then`.
  - Use `require.*`, and assert outputs, not just `NoError`.
  - Put the `//go:generate mockgen` directive in the `_test.go` file.
  - Each controller's tests live in the matching `_test.go` file.
- **Strict TDD:** write the test against a compiling stub, watch it fail, then implement. Mutation-check any test that passes immediately.
- **Sample data:** fictional only, with placeholders like `[NOMBRE]` and `[EMAIL]`. No real personal data.
