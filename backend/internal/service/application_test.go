package service

//go:generate mockgen -source=application.go -destination=mocks/application.go -package=mocks

import (
	"context"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service/mocks"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type applicationTestSetup struct {
	ctrl    *gomock.Controller
	repo    *mocks.MockApplicationRepository
	service *ApplicationService
}

func setupApplicationTest(t *testing.T) applicationTestSetup {
	ctrl := gomock.NewController(t)
	repo := mocks.NewMockApplicationRepository(ctrl)
	return applicationTestSetup{ctrl: ctrl, repo: repo, service: NewApplicationService(repo)}
}

func TestApply_CreatesTheApplication(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	candidateID, applicationID, offerID := uuid.New(), uuid.New(), uuid.New()
	setup.repo.EXPECT().GetApplication(gomock.Any(), applicationID).Return(model.Application{}, model.ErrNotFound)
	setup.repo.EXPECT().HasCV(gomock.Any(), candidateID).Return(true, nil)
	setup.repo.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, Status: model.OfferStatusActive}, nil)
	setup.repo.EXPECT().CreateApplication(gomock.Any(), applicationID, offerID, candidateID).Return(nil)

	// when
	err := setup.service.Apply(context.Background(), candidateID, applicationID, offerID)

	// then
	require.NoError(t, err)
}

func TestApply_ResendingTheSameApplicationIsANoOp(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	candidateID, applicationID, offerID := uuid.New(), uuid.New(), uuid.New()
	setup.repo.EXPECT().GetApplication(gomock.Any(), applicationID).Return(model.Application{ID: applicationID, OfferID: offerID, CandidateID: candidateID}, nil)

	// when
	err := setup.service.Apply(context.Background(), candidateID, applicationID, offerID)

	// then
	require.NoError(t, err)
}

func TestApply_AnIdUsedByAnotherCandidateConflicts(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	applicationID, offerID := uuid.New(), uuid.New()
	setup.repo.EXPECT().GetApplication(gomock.Any(), applicationID).Return(model.Application{ID: applicationID, OfferID: offerID, CandidateID: uuid.New()}, nil)

	// when
	err := setup.service.Apply(context.Background(), uuid.New(), applicationID, offerID)

	// then
	require.ErrorIs(t, err, model.ErrConflict)
}

func TestApply_NeedsACV(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	candidateID, applicationID := uuid.New(), uuid.New()
	setup.repo.EXPECT().GetApplication(gomock.Any(), applicationID).Return(model.Application{}, model.ErrNotFound)
	setup.repo.EXPECT().HasCV(gomock.Any(), candidateID).Return(false, nil)

	// when
	err := setup.service.Apply(context.Background(), candidateID, applicationID, uuid.New())

	// then
	require.ErrorIs(t, err, model.ErrCVRequired)
}

func TestApply_ClosedOrUnknownOfferConflicts(t *testing.T) {
	cases := map[string]struct {
		offer model.Offer
		err   error
	}{
		"closed":  {model.Offer{Status: model.OfferStatusClosed}, nil},
		"unknown": {model.Offer{}, model.ErrNotFound},
	}
	for name, c := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupApplicationTest(t)
			candidateID, applicationID, offerID := uuid.New(), uuid.New(), uuid.New()
			setup.repo.EXPECT().GetApplication(gomock.Any(), applicationID).Return(model.Application{}, model.ErrNotFound)
			setup.repo.EXPECT().HasCV(gomock.Any(), candidateID).Return(true, nil)
			setup.repo.EXPECT().GetOffer(gomock.Any(), offerID).Return(c.offer, c.err)

			// when
			err := setup.service.Apply(context.Background(), candidateID, applicationID, offerID)

			// then
			require.ErrorIs(t, err, model.ErrConflict)
		})
	}
}

func TestApply_SecondApplicationToTheSameOfferIsAcceptedAsDone(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	candidateID, applicationID, offerID := uuid.New(), uuid.New(), uuid.New()
	setup.repo.EXPECT().GetApplication(gomock.Any(), applicationID).Return(model.Application{}, model.ErrNotFound)
	setup.repo.EXPECT().HasCV(gomock.Any(), candidateID).Return(true, nil)
	setup.repo.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, Status: model.OfferStatusActive}, nil)
	setup.repo.EXPECT().CreateApplication(gomock.Any(), applicationID, offerID, candidateID).Return(model.ErrConflict)

	// when
	err := setup.service.Apply(context.Background(), candidateID, applicationID, offerID)

	// then
	require.NoError(t, err)
}

func TestMine_SentApplicationToAClosedOfferIsOfferClosed(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	candidateID := uuid.New()
	createdAt := time.Date(2026, 11, 1, 12, 0, 0, 0, time.UTC)
	stored := []model.CandidateApplication{
		{ID: uuid.New(), OfferStatus: model.OfferStatusClosed, Status: model.ApplicationStatusSent, CreatedAt: createdAt},
		{ID: uuid.New(), OfferStatus: model.OfferStatusClosed, Status: model.ApplicationStatusApproved, CreatedAt: createdAt},
		{ID: uuid.New(), OfferStatus: model.OfferStatusActive, Status: model.ApplicationStatusSent, CreatedAt: createdAt},
	}
	setup.repo.EXPECT().ListCandidateApplications(gomock.Any(), candidateID).Return(stored, nil)

	// when
	got, err := setup.service.Mine(context.Background(), candidateID)

	// then
	require.NoError(t, err)
	expected := []model.CandidateApplication{stored[0], stored[1], stored[2]}
	expected[0].Status = model.ApplicationStatusOfferClosed
	require.Equal(t, expected, got)
}

func TestApplicants_HidesTheContactUntilApproved(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	setup.repo.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: businessID}, nil)
	pending := model.Applicant{ApplicationID: uuid.New(), OfferID: offerID, CandidateName: "[NOMBRE]", CandidateEmail: "[email]@a", Status: model.ApplicationStatusSent, CVFileName: null.StringFrom("cv.pdf")}
	approved := model.Applicant{ApplicationID: uuid.New(), OfferID: offerID, CandidateName: "[NOMBRE]", CandidateEmail: "[email]@b", Status: model.ApplicationStatusApproved}
	rejected := model.Applicant{ApplicationID: uuid.New(), OfferID: offerID, CandidateName: "[NOMBRE]", CandidateEmail: "[email]@c", Status: model.ApplicationStatusRejected}
	setup.repo.EXPECT().ListApplicants(gomock.Any(), offerID).Return([]model.Applicant{pending, approved, rejected}, nil)

	// when
	got, err := setup.service.Applicants(context.Background(), businessID, offerID)

	// then
	require.NoError(t, err)
	hiddenPending, hiddenRejected := pending, rejected
	hiddenPending.CandidateEmail, hiddenRejected.CandidateEmail = "", ""
	require.Equal(t, []model.Applicant{hiddenPending, approved, hiddenRejected}, got)
}

func TestApplicants_AnotherBusinessOfferIsForbidden(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	offerID := uuid.New()
	setup.repo.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: uuid.New()}, nil)

	// when
	_, err := setup.service.Applicants(context.Background(), uuid.New(), offerID)

	// then
	require.ErrorIs(t, err, model.ErrForbidden)
}

func TestApplicants_UnknownOffer(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	offerID := uuid.New()
	setup.repo.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{}, model.ErrNotFound)

	// when
	_, err := setup.service.Applicants(context.Background(), uuid.New(), offerID)

	// then
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestReview_ApprovalReturnsTheContact(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	businessID, applicationID := uuid.New(), uuid.New()
	applicant := model.Applicant{ApplicationID: applicationID, BusinessID: businessID, CandidateEmail: "[email]@a", Status: model.ApplicationStatusSent}
	setup.repo.EXPECT().GetApplicant(gomock.Any(), applicationID).Return(applicant, nil)
	setup.repo.EXPECT().UpdateApplicationStatus(gomock.Any(), applicationID, model.ApplicationStatusApproved).Return(nil)

	// when
	got, err := setup.service.Review(context.Background(), businessID, applicationID, true)

	// then
	require.NoError(t, err)
	expected := applicant
	expected.Status = model.ApplicationStatusApproved
	require.Equal(t, expected, got)
}

func TestReview_RejectionHidesTheContact(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	businessID, applicationID := uuid.New(), uuid.New()
	applicant := model.Applicant{ApplicationID: applicationID, BusinessID: businessID, CandidateEmail: "[email]@a", Status: model.ApplicationStatusSent}
	setup.repo.EXPECT().GetApplicant(gomock.Any(), applicationID).Return(applicant, nil)
	setup.repo.EXPECT().UpdateApplicationStatus(gomock.Any(), applicationID, model.ApplicationStatusRejected).Return(nil)

	// when
	got, err := setup.service.Review(context.Background(), businessID, applicationID, false)

	// then
	require.NoError(t, err)
	expected := applicant
	expected.Status, expected.CandidateEmail = model.ApplicationStatusRejected, ""
	require.Equal(t, expected, got)
}

func TestReview_AnotherBusinessApplicantIsForbidden(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	applicationID := uuid.New()
	setup.repo.EXPECT().GetApplicant(gomock.Any(), applicationID).Return(model.Applicant{ApplicationID: applicationID, BusinessID: uuid.New()}, nil)

	// when
	_, err := setup.service.Review(context.Background(), uuid.New(), applicationID, true)

	// then
	require.ErrorIs(t, err, model.ErrForbidden)
}

func TestReview_UnknownApplication(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	applicationID := uuid.New()
	setup.repo.EXPECT().GetApplicant(gomock.Any(), applicationID).Return(model.Applicant{}, model.ErrNotFound)

	// when
	_, err := setup.service.Review(context.Background(), uuid.New(), applicationID, true)

	// then
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestApplicantCV_OwnerGetsTheFile(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	businessID, applicationID, candidateID := uuid.New(), uuid.New(), uuid.New()
	cv := model.CV{FileName: "cv.pdf", Content: []byte("%PDF-1.4")}
	setup.repo.EXPECT().GetApplicant(gomock.Any(), applicationID).Return(model.Applicant{ApplicationID: applicationID, BusinessID: businessID, CandidateID: candidateID}, nil)
	setup.repo.EXPECT().GetCV(gomock.Any(), candidateID).Return(cv, nil)

	// when
	got, err := setup.service.ApplicantCV(context.Background(), businessID, applicationID)

	// then
	require.NoError(t, err)
	require.Equal(t, cv, got)
}

func TestApplicantCV_AnotherBusinessIsForbidden(t *testing.T) {
	// begin
	setup := setupApplicationTest(t)
	applicationID := uuid.New()
	setup.repo.EXPECT().GetApplicant(gomock.Any(), applicationID).Return(model.Applicant{ApplicationID: applicationID, BusinessID: uuid.New()}, nil)

	// when
	_, err := setup.service.ApplicantCV(context.Background(), uuid.New(), applicationID)

	// then
	require.ErrorIs(t, err, model.ErrForbidden)
}
