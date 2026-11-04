package repository

import (
	"context"
	"errors"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/dbmodel"
	"github.com/laburin/backend/internal/model"
)

func (p *Postgres) GetApplication(ctx context.Context, id uuid.UUID) (model.Application, error) {
	row, err := p.queries.GetApplication(ctx, id)
	if err != nil {
		return model.Application{}, mapError(err)
	}
	return model.Application{ID: row.ID, OfferID: row.OfferID, CandidateID: row.CandidateID, Status: model.ApplicationStatus(row.Status), CreatedAt: row.CreatedAt}, nil
}

func (p *Postgres) CreateApplication(ctx context.Context, id, offerID, candidateID uuid.UUID) error {
	params := dbmodel.CreateApplicationParams{ID: id, OfferID: offerID, CandidateID: candidateID}
	return mapError(p.queries.CreateApplication(ctx, params))
}

func (p *Postgres) ListCandidateApplications(ctx context.Context, candidateID uuid.UUID) ([]model.CandidateApplication, error) {
	rows, err := p.queries.ListCandidateApplications(ctx, candidateID)
	if err != nil {
		return nil, mapError(err)
	}
	applications := make([]model.CandidateApplication, len(rows))
	for i, row := range rows {
		applications[i] = model.CandidateApplication{ID: row.ID, OfferID: row.OfferID, OfferTitle: row.OfferTitle, OfferStatus: model.OfferStatus(row.OfferStatus), BusinessName: row.BusinessName, Status: model.ApplicationStatus(row.Status), CreatedAt: row.CreatedAt}
	}
	return applications, nil
}

func (p *Postgres) ListApplicants(ctx context.Context, offerID uuid.UUID) ([]model.Applicant, error) {
	rows, err := p.queries.ListApplicants(ctx, offerID)
	if err != nil {
		return nil, mapError(err)
	}
	applicants := make([]model.Applicant, len(rows))
	for i, row := range rows {
		applicants[i] = applicantFromRow(dbmodel.GetApplicantRow(row))
	}
	return applicants, nil
}

func (p *Postgres) GetApplicant(ctx context.Context, applicationID uuid.UUID) (model.Applicant, error) {
	row, err := p.queries.GetApplicant(ctx, applicationID)
	if err != nil {
		return model.Applicant{}, mapError(err)
	}
	return applicantFromRow(row), nil
}

func (p *Postgres) UpdateApplicationStatus(ctx context.Context, applicationID uuid.UUID, status model.ApplicationStatus) error {
	params := dbmodel.UpdateApplicationStatusParams{ID: applicationID, Status: dbmodel.ApplicationStatus(status)}
	return mapError(p.queries.UpdateApplicationStatus(ctx, params))
}

func (p *Postgres) HasCV(ctx context.Context, userID uuid.UUID) (bool, error) {
	_, err := p.GetCVFileName(ctx, userID)
	if errors.Is(err, model.ErrNotFound) {
		return false, nil
	}
	return err == nil, err
}

func (p *Postgres) GetCV(ctx context.Context, userID uuid.UUID) (model.CV, error) {
	row, err := p.queries.GetCV(ctx, userID)
	if err != nil {
		return model.CV{}, mapError(err)
	}
	return model.CV{FileName: row.FileName, Content: row.Content}, nil
}

func applicantFromRow(row dbmodel.GetApplicantRow) model.Applicant {
	return model.Applicant{
		ApplicationID: row.ID, OfferID: row.OfferID, CandidateID: row.CandidateID, BusinessID: row.BusinessID,
		CandidateName: row.DisplayName, CandidateEmail: row.Email, CVFileName: row.CvFileName, Status: model.ApplicationStatus(row.Status),
	}
}
