# LaburIN API. `source dev.env` (or export the variables) before running.
TEST_DB_DSN ?= postgres://localhost:5432/laburin_test?sslmode=disable
TEST_REDIS_ADDR ?= localhost:6379
DBMATE = $(shell go env GOPATH)/bin/dbmate --migrations-dir db/migrations --no-dump-schema

.PHONY: run seed migrate generate test test-unit up down

run:            ## start the API on $$PORT (default 8080)
	go run ./cmd/api

seed:           ## fictional demo accounts and offers (idempotent)
	go run ./cmd/seed

migrate:        ## apply db/migrations to $$DATABASE_URL
	$(DBMATE) up

generate:       ## regenerate SQLC code and mocks after changing db/query or an interface
	$(shell go env GOPATH)/bin/sqlc generate
	PATH="$$PATH:$(shell go env GOPATH)/bin" go generate ./...

test:           ## every test, including Postgres/Redis integration (migrates the test DB first)
	DATABASE_URL="$(TEST_DB_DSN)" $(DBMATE) up
	TEST_DB_DSN="$(TEST_DB_DSN)" TEST_REDIS_ADDR="$(TEST_REDIS_ADDR)" go test -p 1 ./...

test-unit:      ## only tests that need no database
	go test ./...

up:             ## Postgres + Redis in Docker (alternative to local installs)
	docker compose up -d postgres redis

down:
	docker compose down
