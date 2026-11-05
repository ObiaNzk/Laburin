// Package repository implements the services' storage interfaces: Postgres (source of truth) and Redis (geo index).
package repository

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/laburin/backend/internal/dbmodel"
	"github.com/laburin/backend/internal/model"
)

// Postgres wraps the SQLC queries. One type serves every service interface; each service declares only what it uses.
type Postgres struct {
	pool    *pgxpool.Pool
	queries *dbmodel.Queries
}

func NewPostgres(pool *pgxpool.Pool) *Postgres {
	return &Postgres{pool: pool, queries: dbmodel.New(pool)}
}

const uniqueViolation = "23505"

// mapError turns driver errors into the domain errors the services understand.
func mapError(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return model.ErrNotFound
	}
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == uniqueViolation {
		return model.ErrConflict
	}
	return err
}

func (p *Postgres) inTx(ctx context.Context, fn func(q *dbmodel.Queries) error) error {
	tx, err := p.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx) //nolint:errcheck // no-op after a commit
	if err := fn(p.queries.WithTx(tx)); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
