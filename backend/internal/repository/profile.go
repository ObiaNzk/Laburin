package repository

import (
	"context"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/dbmodel"
	"github.com/laburin/backend/internal/model"
)

func (p *Postgres) SaveCV(ctx context.Context, userID uuid.UUID, cv model.CV) error {
	params := dbmodel.UpsertCVParams{UserID: userID, FileName: cv.FileName, Content: cv.Content}
	return mapError(p.queries.UpsertCV(ctx, params))
}

func (p *Postgres) GetCVFileName(ctx context.Context, userID uuid.UUID) (string, error) {
	name, err := p.queries.GetCVFileName(ctx, userID)
	return name, mapError(err)
}

func (p *Postgres) SavePushToken(ctx context.Context, userID uuid.UUID, token string) error {
	params := dbmodel.UpsertPushTokenParams{Token: token, UserID: userID}
	return mapError(p.queries.UpsertPushToken(ctx, params))
}
