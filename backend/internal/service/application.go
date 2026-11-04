package service

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
)

type ApplicationRepository interface {
	// GetApplication returns model.ErrNotFound for an unknown id.
	GetApplication(ctx context.Context, id uuid.UUID) (model.Application, error)
	// CreateApplication returns model.ErrConflict when the candidate already applied to that offer.
	CreateApplication(ctx context.Context, id, offerID, candidateID uuid.UUID) error
	ListCandidateApplications(ctx context.Context, candidateID uuid.UUID) ([]model.CandidateApplication, error)
	ListApplicants(ctx context.Context, offerID uuid.UUID) ([]model.Applicant, error)
	// GetApplicant returns model.ErrNotFound for an unknown application.
	GetApplicant(ctx context.Context, applicationID uuid.UUID) (model.Applicant, error)
	UpdateApplicationStatus(ctx context.Context, applicationID uuid.UUID, status model.ApplicationStatus) error
	// GetOffer returns model.ErrNotFound for an unknown offer.
	GetOffer(ctx context.Context, id uuid.UUID) (model.Offer, error)
	HasCV(ctx context.Context, userID uuid.UUID) (bool, error)
	// GetCV returns model.ErrNotFound when the user has no CV.
	GetCV(ctx context.Context, userID uuid.UUID) (model.CV, error)
}

type ApplicationService struct {
	repo ApplicationRepository
}

func NewApplicationService(repo ApplicationRepository) *ApplicationService {
	return &ApplicationService{repo: repo}
}

// Apply is idempotent by application id. model.ErrCVRequired: no CV uploaded. model.ErrConflict: the offer is closed
// or unknown, or the id belongs to another candidate.
func (s *ApplicationService) Apply(ctx context.Context, candidateID, applicationID, offerID uuid.UUID) error {
	existing, err := s.repo.GetApplication(ctx, applicationID)
	if err == nil {
		if existing.CandidateID != candidateID || existing.OfferID != offerID {
			return fmt.Errorf("%w: the application id is already used", model.ErrConflict)
		}
		return nil
	}
	if !errors.Is(err, model.ErrNotFound) {
		return err
	}
	hasCV, err := s.repo.HasCV(ctx, candidateID)
	if err != nil {
		return err
	}
	if !hasCV {
		return model.ErrCVRequired
	}
	offer, err := s.repo.GetOffer(ctx, offerID)
	if errors.Is(err, model.ErrNotFound) {
		return fmt.Errorf("%w: unknown offer", model.ErrConflict)
	}
	if err != nil {
		return err
	}
	if offer.Status != model.OfferStatusActive {
		return fmt.Errorf("%w: the offer is closed", model.ErrConflict)
	}
	err = s.repo.CreateApplication(ctx, applicationID, offerID, candidateID)
	if errors.Is(err, model.ErrConflict) {
		// One application per offer and candidate: an earlier one (with another id) already counts.
		return nil
	}
	return err
}

// Mine returns the candidate's applications; a sent application whose offer closed is OFFER_CLOSED.
func (s *ApplicationService) Mine(ctx context.Context, candidateID uuid.UUID) ([]model.CandidateApplication, error) {
	applications, err := s.repo.ListCandidateApplications(ctx, candidateID)
	if err != nil {
		return nil, err
	}
	for i, application := range applications {
		if application.Status == model.ApplicationStatusSent && application.OfferStatus == model.OfferStatusClosed {
			applications[i].Status = model.ApplicationStatusOfferClosed
		}
	}
	return applications, nil
}

// Applicants lists the offer's applicants for its owner. The contact is cleared unless the applicant was approved.
func (s *ApplicationService) Applicants(ctx context.Context, businessID, offerID uuid.UUID) ([]model.Applicant, error) {
	offer, err := s.repo.GetOffer(ctx, offerID)
	if err != nil {
		return nil, err
	}
	if offer.BusinessID != businessID {
		return nil, fmt.Errorf("%w: the offer belongs to another business", model.ErrForbidden)
	}
	applicants, err := s.repo.ListApplicants(ctx, offerID)
	if err != nil {
		return nil, err
	}
	for i := range applicants {
		applicants[i] = withContactOnlyIfApproved(applicants[i])
	}
	return applicants, nil
}

// Review approves or rejects an applicant (idempotent). The contact is returned only for an approval.
func (s *ApplicationService) Review(ctx context.Context, businessID, applicationID uuid.UUID, approve bool) (model.Applicant, error) {
	applicant, err := s.ownedApplicant(ctx, businessID, applicationID)
	if err != nil {
		return model.Applicant{}, err
	}
	status := model.ApplicationStatusRejected
	if approve {
		status = model.ApplicationStatusApproved
	}
	if err := s.repo.UpdateApplicationStatus(ctx, applicationID, status); err != nil {
		return model.Applicant{}, err
	}
	applicant.Status = status
	return withContactOnlyIfApproved(applicant), nil
}

// ApplicantCV returns the applicant's CV to the business that owns the offer.
func (s *ApplicationService) ApplicantCV(ctx context.Context, businessID, applicationID uuid.UUID) (model.CV, error) {
	applicant, err := s.ownedApplicant(ctx, businessID, applicationID)
	if err != nil {
		return model.CV{}, err
	}
	return s.repo.GetCV(ctx, applicant.CandidateID)
}

func (s *ApplicationService) ownedApplicant(ctx context.Context, businessID, applicationID uuid.UUID) (model.Applicant, error) {
	applicant, err := s.repo.GetApplicant(ctx, applicationID)
	if err != nil {
		return model.Applicant{}, err
	}
	if applicant.BusinessID != businessID {
		return model.Applicant{}, fmt.Errorf("%w: the application belongs to another business's offer", model.ErrForbidden)
	}
	return applicant, nil
}

// withContactOnlyIfApproved enforces RNF-05: the contact leaves the server only after the Match.
func withContactOnlyIfApproved(applicant model.Applicant) model.Applicant {
	if applicant.Status != model.ApplicationStatusApproved {
		applicant.CandidateEmail = ""
	}
	return applicant
}
