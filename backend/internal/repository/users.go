package repository

import (
	"context"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/dbmodel"
	"github.com/laburin/backend/internal/model"
)

func (p *Postgres) GetUserByEmail(ctx context.Context, email string) (model.User, error) {
	row, err := p.queries.GetUserByEmail(ctx, email)
	if err != nil {
		return model.User{}, mapError(err)
	}
	return model.User{ID: row.ID, Role: model.Role(row.Role), Email: row.Email, PasswordHash: row.PasswordHash, DisplayName: row.DisplayName}, nil
}

func (p *Postgres) CreateCandidate(ctx context.Context, user model.User) error {
	return mapError(p.queries.CreateUser(ctx, userParams(user)))
}

func (p *Postgres) CreateBusiness(ctx context.Context, user model.User, business model.Business) error {
	err := p.inTx(ctx, func(q *dbmodel.Queries) error {
		if err := q.CreateUser(ctx, userParams(user)); err != nil {
			return err
		}
		params := dbmodel.CreateBusinessParams{UserID: business.UserID, Name: business.Name, Category: dbmodel.OfferCategory(business.Category), Schedule: business.Schedule, Address: business.Address, Latitude: business.Latitude, Longitude: business.Longitude}
		return q.CreateBusiness(ctx, params)
	})
	return mapError(err)
}

func (p *Postgres) GetBusiness(ctx context.Context, userID uuid.UUID) (model.Business, error) {
	row, err := p.queries.GetBusiness(ctx, userID)
	if err != nil {
		return model.Business{}, mapError(err)
	}
	return model.Business{UserID: row.UserID, Name: row.Name, Category: model.Category(row.Category), Schedule: row.Schedule, Address: row.Address, Latitude: row.Latitude, Longitude: row.Longitude}, nil
}

func userParams(user model.User) dbmodel.CreateUserParams {
	return dbmodel.CreateUserParams{ID: user.ID, Role: dbmodel.UserRole(user.Role), Email: user.Email, PasswordHash: user.PasswordHash, DisplayName: user.DisplayName}
}
