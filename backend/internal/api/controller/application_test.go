package controller

//go:generate mockgen -source=application.go -destination=mocks/application.go -package=mocks

import (
	"net/http"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/laburin/backend/internal/api/controller/mocks"
	"github.com/laburin/backend/internal/model"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type applicationControllerSetup struct {
	applications *mocks.MockApplicationService
	router       http.Handler
	user         uuid.UUID
}

func setupApplicationController(t *testing.T) applicationControllerSetup {
	applications := mocks.NewMockApplicationService(gomock.NewController(t))
	controller := NewApplicationController(applications)
	user := uuid.New()
	router := newRouter()
	authenticated := router.Group("", asUser(user, model.RoleCandidate))
	authenticated.PUT("/applications/:id", controller.Apply)
	authenticated.GET("/applications/mine", controller.Mine)
	authenticated.GET("/offers/:id/applicants", controller.Applicants)
	authenticated.PUT("/applications/:id/review", controller.Review)
	authenticated.GET("/applications/:id/cv", controller.ApplicantCV)
	return applicationControllerSetup{applications: applications, router: router, user: user}
}

var applicationID = uuid.MustParse("44444444-4444-4444-4444-444444444444")

func TestApplyController_Accepted(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	setup.applications.EXPECT().Apply(gomock.Any(), setup.user, applicationID, offerID).Return(nil)

	// when
	rec := do(setup.router, http.MethodPut, "/applications/"+applicationID.String(), `{"offerId":"`+offerID.String()+`"}`)

	// then
	require.Equal(t, http.StatusOK, rec.Code)
}

func TestApplyController_Errors(t *testing.T) {
	cases := map[error]int{model.ErrConflict: http.StatusConflict, model.ErrCVRequired: http.StatusUnprocessableEntity}
	for err, status := range cases {
		t.Run(err.Error(), func(t *testing.T) {
			// begin
			setup := setupApplicationController(t)
			setup.applications.EXPECT().Apply(gomock.Any(), setup.user, applicationID, offerID).Return(err)

			// when
			rec := do(setup.router, http.MethodPut, "/applications/"+applicationID.String(), `{"offerId":"`+offerID.String()+`"}`)

			// then
			require.Equal(t, status, rec.Code)
		})
	}
}

func TestApplyController_BadIdsAreBadRequest(t *testing.T) {
	cases := map[string]string{
		"application id": "/applications/x",
		"offer id":       "/applications/" + applicationID.String(),
	}
	for name, path := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupApplicationController(t)

			// when
			rec := do(setup.router, http.MethodPut, path, `{"offerId":"f-1"}`)

			// then
			require.Equal(t, http.StatusBadRequest, rec.Code)
		})
	}
}

func TestMyApplicationsController_ListsThem(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	mine := []model.CandidateApplication{{ID: applicationID, OfferID: offerID, OfferTitle: "Mozo", OfferStatus: model.OfferStatusClosed, BusinessName: "[COMERCIO]", Status: model.ApplicationStatusOfferClosed, CreatedAt: time.Date(2026, 11, 2, 9, 30, 0, 5, time.UTC)}}
	setup.applications.EXPECT().Mine(gomock.Any(), setup.user).Return(mine, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/applications/mine", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, `[{"id":"44444444-4444-4444-4444-444444444444","offerId":"22222222-2222-2222-2222-222222222222","offerTitle":"Mozo","businessName":"[COMERCIO]","status":"OFFER_CLOSED","createdAt":"2026-11-02T09:30:00Z"}]`, rec.Body.String())
}

func TestMyApplicationsController_EmptyIsAnEmptyArray(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	setup.applications.EXPECT().Mine(gomock.Any(), setup.user).Return(nil, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/applications/mine", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, `[]`, rec.Body.String())
}

func TestApplicantsController_PendingReviewWithoutContact(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	applicants := []model.Applicant{
		{ApplicationID: applicationID, OfferID: offerID, CandidateName: "[NOMBRE]", CVFileName: null.StringFrom("cv.pdf"), Status: model.ApplicationStatusSent},
		{ApplicationID: offerID, OfferID: offerID, CandidateName: "[NOMBRE]", CandidateEmail: "[email]@demo.laburin", Status: model.ApplicationStatusApproved},
	}
	setup.applications.EXPECT().Applicants(gomock.Any(), setup.user, offerID).Return(applicants, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/offers/"+offerID.String()+"/applicants", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, `[
		{"applicationId":"44444444-4444-4444-4444-444444444444","offerId":"22222222-2222-2222-2222-222222222222","candidateName":"[NOMBRE]","cvFileName":"cv.pdf","status":"PENDING_REVIEW","contactEmail":null},
		{"applicationId":"22222222-2222-2222-2222-222222222222","offerId":"22222222-2222-2222-2222-222222222222","candidateName":"[NOMBRE]","cvFileName":null,"status":"APPROVED","contactEmail":"[email]@demo.laburin"}
	]`, rec.Body.String())
}

func TestApplicantsController_ForbiddenAndUnknown(t *testing.T) {
	cases := map[error]int{model.ErrForbidden: http.StatusForbidden, model.ErrNotFound: http.StatusNotFound}
	for err, status := range cases {
		t.Run(err.Error(), func(t *testing.T) {
			// begin
			setup := setupApplicationController(t)
			setup.applications.EXPECT().Applicants(gomock.Any(), setup.user, offerID).Return(nil, err)

			// when
			rec := do(setup.router, http.MethodGet, "/offers/"+offerID.String()+"/applicants", "")

			// then
			require.Equal(t, status, rec.Code)
		})
	}
}

func TestReviewController_ApprovalReturnsTheContact(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	approved := model.Applicant{ApplicationID: applicationID, OfferID: offerID, CandidateName: "[NOMBRE]", CandidateEmail: "[email]@demo.laburin", Status: model.ApplicationStatusApproved}
	setup.applications.EXPECT().Review(gomock.Any(), setup.user, applicationID, true).Return(approved, nil)

	// when
	rec := do(setup.router, http.MethodPut, "/applications/"+applicationID.String()+"/review", `{"approve":true}`)

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, `{"applicationId":"44444444-4444-4444-4444-444444444444","offerId":"22222222-2222-2222-2222-222222222222","candidateName":"[NOMBRE]","cvFileName":null,"status":"APPROVED","contactEmail":"[email]@demo.laburin"}`, rec.Body.String())
}

func TestReviewController_RejectionIsPassedAsFalse(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	setup.applications.EXPECT().Review(gomock.Any(), setup.user, applicationID, false).Return(model.Applicant{Status: model.ApplicationStatusRejected}, nil)

	// when
	rec := do(setup.router, http.MethodPut, "/applications/"+applicationID.String()+"/review", `{"approve":false}`)

	// then
	require.Equal(t, http.StatusOK, rec.Code)
}

func TestReviewController_MissingDecisionIsBadRequest(t *testing.T) {
	// begin
	setup := setupApplicationController(t)

	// when
	rec := do(setup.router, http.MethodPut, "/applications/"+applicationID.String()+"/review", `{}`)

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}

func TestApplicantCVController_ServesThePdf(t *testing.T) {
	// begin
	setup := setupApplicationController(t)
	setup.applications.EXPECT().ApplicantCV(gomock.Any(), setup.user, applicationID).Return(model.CV{FileName: "mi cv.pdf", Content: []byte("%PDF-1.4")}, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/applications/"+applicationID.String()+"/cv", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.Equal(t, "application/pdf", rec.Header().Get("Content-Type"))
	require.Equal(t, `attachment; filename="mi cv.pdf"`, rec.Header().Get("Content-Disposition"))
	require.Equal(t, "%PDF-1.4", rec.Body.String())
}

func TestApplicantCVController_Errors(t *testing.T) {
	cases := map[error]int{model.ErrForbidden: http.StatusForbidden, model.ErrNotFound: http.StatusNotFound}
	for err, status := range cases {
		t.Run(err.Error(), func(t *testing.T) {
			// begin
			setup := setupApplicationController(t)
			setup.applications.EXPECT().ApplicantCV(gomock.Any(), setup.user, applicationID).Return(model.CV{}, err)

			// when
			rec := do(setup.router, http.MethodGet, "/applications/"+applicationID.String()+"/cv", "")

			// then
			require.Equal(t, status, rec.Code)
		})
	}
}
