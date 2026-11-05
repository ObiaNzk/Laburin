package repository

import (
	"context"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/laburin/backend/internal/dbmodel"
	"github.com/laburin/backend/internal/model"
)

func (p *Postgres) GetOffer(ctx context.Context, id uuid.UUID) (model.Offer, error) {
	row, err := p.queries.GetOffer(ctx, id)
	if err != nil {
		return model.Offer{}, mapError(err)
	}
	return offerFromRow(row), nil
}

func (p *Postgres) ListActiveOffersByIDs(ctx context.Context, ids []uuid.UUID, category *model.Category) ([]model.Offer, error) {
	params := dbmodel.ListActiveOffersByIDsParams{Ids: ids}
	if category != nil {
		params.Category = dbmodel.NullOfferCategory{OfferCategory: dbmodel.OfferCategory(*category), Valid: true}
	}
	rows, err := p.queries.ListActiveOffersByIDs(ctx, params)
	if err != nil {
		return nil, mapError(err)
	}
	offers := make([]model.Offer, len(rows))
	for i, row := range rows {
		offers[i] = offerFromRow(dbmodel.GetOfferRow(row))
	}
	return offers, nil
}

func (p *Postgres) ListBusinessOffers(ctx context.Context, businessID uuid.UUID) ([]model.Offer, error) {
	rows, err := p.queries.ListBusinessOffers(ctx, businessID)
	if err != nil {
		return nil, mapError(err)
	}
	offers := make([]model.Offer, len(rows))
	for i, row := range rows {
		offers[i] = offerFromRow(dbmodel.GetOfferRow(row))
	}
	return offers, nil
}

func (p *Postgres) ListActiveOfferLocations(ctx context.Context) ([]model.OfferLocation, error) {
	rows, err := p.queries.ListActiveOfferLocations(ctx)
	if err != nil {
		return nil, mapError(err)
	}
	locations := make([]model.OfferLocation, len(rows))
	for i, row := range rows {
		locations[i] = model.OfferLocation{ID: row.ID, Latitude: row.Latitude, Longitude: row.Longitude}
	}
	return locations, nil
}

func (p *Postgres) CreateOffer(ctx context.Context, id, businessID uuid.UUID, content model.OfferContent) error {
	minimum, maximum, period := salaryColumns(content.Salary)
	params := dbmodel.CreateOfferParams{ID: id, BusinessID: businessID, Title: content.Title, Category: dbmodel.OfferCategory(content.Category), Schedule: content.Schedule, Description: content.Description, SalaryMin: minimum, SalaryMax: maximum, SalaryPeriod: period}
	return mapError(p.queries.CreateOffer(ctx, params))
}

func (p *Postgres) UpdateOfferContent(ctx context.Context, id uuid.UUID, content model.OfferContent) error {
	minimum, maximum, period := salaryColumns(content.Salary)
	params := dbmodel.UpdateOfferContentParams{ID: id, Title: content.Title, Category: dbmodel.OfferCategory(content.Category), Schedule: content.Schedule, Description: content.Description, SalaryMin: minimum, SalaryMax: maximum, SalaryPeriod: period}
	return mapError(p.queries.UpdateOfferContent(ctx, params))
}

func (p *Postgres) CloseOffer(ctx context.Context, id uuid.UUID) error {
	return mapError(p.queries.CloseOffer(ctx, id))
}

// offerFromRow maps any of the offer queries (they select the same columns).
func offerFromRow(row dbmodel.GetOfferRow) model.Offer {
	content := model.OfferContent{Title: row.Title, Category: model.Category(row.Category), Schedule: row.Schedule, Description: row.Description}
	if row.SalaryMin.Valid && row.SalaryPeriod.Valid {
		content.Salary = &model.Salary{Min: row.SalaryMin.Int64, Max: row.SalaryMax, Period: model.SalaryPeriod(row.SalaryPeriod.SalaryPeriod)}
	}
	return model.Offer{
		ID: row.ID, BusinessID: row.BusinessID, Content: content, JobType: row.JobType, BusinessName: row.BusinessName,
		Address: row.Address, Latitude: row.Latitude, Longitude: row.Longitude, Status: model.OfferStatus(row.Status), UpdatedAt: row.UpdatedAt,
	}
}

func salaryColumns(salary *model.Salary) (null.Int64, null.Int64, dbmodel.NullSalaryPeriod) {
	if salary == nil {
		return null.Int64{}, null.Int64{}, dbmodel.NullSalaryPeriod{}
	}
	return null.IntFrom(salary.Min), salary.Max, dbmodel.NullSalaryPeriod{SalaryPeriod: dbmodel.SalaryPeriod(salary.Period), Valid: true}
}
