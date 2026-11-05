// Package testutil connects integration tests to real Postgres and Redis. Tests are skipped when TEST_DB_DSN /
// TEST_REDIS_ADDR are not set; `make test` sets them and migrates the test database first.
package testutil

import (
	"context"
	"os"
	"testing"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
	"github.com/stretchr/testify/require"
)

// Postgres returns a pool to the migrated test database with every table emptied.
func Postgres(t *testing.T) *pgxpool.Pool {
	t.Helper()
	dsn := os.Getenv("TEST_DB_DSN")
	if dsn == "" {
		t.Skip("TEST_DB_DSN not set: skipping Postgres integration test")
	}
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	require.NoError(t, err)
	t.Cleanup(pool.Close)
	_, err = pool.Exec(ctx, "TRUNCATE push_tokens, cvs, applications, offers, businesses, users CASCADE")
	require.NoError(t, err)
	return pool
}

// testRedisDB keeps tests away from the development data in DB 0.
const testRedisDB = 15

// Redis returns a client to an emptied test Redis database.
func Redis(t *testing.T) *redis.Client {
	t.Helper()
	addr := os.Getenv("TEST_REDIS_ADDR")
	if addr == "" {
		t.Skip("TEST_REDIS_ADDR not set: skipping Redis integration test")
	}
	client := redis.NewClient(&redis.Options{Addr: addr, DB: testRedisDB})
	t.Cleanup(func() { _ = client.Close() })
	require.NoError(t, client.FlushDB(context.Background()).Err())
	return client
}
