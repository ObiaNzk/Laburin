package controller

import (
	"context"
	"fmt"
	"mime"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/middleware"
	"github.com/laburin/backend/internal/api/response"
	"github.com/laburin/backend/internal/model"
)

type ApplicationService interface {
	Apply(ctx context.Context, candidateID, applicationID, offerID uuid.UUID) error
	Mine(ctx context.Context, candidateID uuid.UUID) ([]model.CandidateApplication, error)
	Applicants(ctx context.Context, businessID, offerID uuid.UUID) ([]model.Applicant, error)
	Review(ctx context.Context, businessID, applicationID uuid.UUID, approve bool) (model.Applicant, error)
	ApplicantCV(ctx context.Context, businessID, applicationID uuid.UUID) (model.CV, error)
}

type ApplicationController struct {
	applications ApplicationService
}

func NewApplicationController(applications ApplicationService) *ApplicationController {
	return &ApplicationController{applications: applications}
}

type ApplyReq struct {
	OfferID string `json:"offerId" binding:"required"`
}

type ReviewReq struct {
	Approve *bool `json:"approve" binding:"required"`
}

type ApplicationResp struct {
	ID           string `json:"id"`
	OfferID      string `json:"offerId"`
	OfferTitle   string `json:"offerTitle"`
	BusinessName string `json:"businessName"`
	Status       string `json:"status"`
	CreatedAt    string `json:"createdAt"`
}

// ApplicantResp: contactEmail is null until the Match (RNF-05); the service already cleared it.
type ApplicantResp struct {
	ApplicationID string  `json:"applicationId"`
	OfferID       string  `json:"offerId"`
	CandidateName string  `json:"candidateName"`
	CVFileName    *string `json:"cvFileName"`
	Status        string  `json:"status"`
	ContactEmail  *string `json:"contactEmail"`
}

// PUT /applications/:id (candidate). The id comes from the app, so a retry is a no-op.
func (a *ApplicationController) Apply(c *gin.Context) {
	id, ok := pathID(c)
	if !ok {
		response.WriteError(c, fmt.Errorf("%w: the application id must be a UUID", model.ErrInvalid))
		return
	}
	var req ApplyReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	offerID, err := uuid.Parse(req.OfferID)
	if err != nil {
		response.WriteError(c, fmt.Errorf("%w: the offer id must be a UUID", model.ErrInvalid))
		return
	}
	if err := a.applications.Apply(c.Request.Context(), middleware.UserID(c), id, offerID); err != nil {
		response.WriteError(c, err)
		return
	}
	c.Status(http.StatusOK)
}

// GET /applications/mine (candidate)
func (a *ApplicationController) Mine(c *gin.Context) {
	applications, err := a.applications.Mine(c.Request.Context(), middleware.UserID(c))
	if err != nil {
		response.WriteError(c, err)
		return
	}
	resps := make([]ApplicationResp, len(applications))
	for i, application := range applications {
		resps[i] = ApplicationResp{ID: application.ID.String(), OfferID: application.OfferID.String(), OfferTitle: application.OfferTitle, BusinessName: application.BusinessName, Status: string(application.Status), CreatedAt: isoTime(application.CreatedAt)}
	}
	c.JSON(http.StatusOK, resps)
}

// GET /offers/:id/applicants (business owner)
func (a *ApplicationController) Applicants(c *gin.Context) {
	offerID, ok := pathID(c)
	if !ok {
		response.WriteError(c, model.ErrNotFound)
		return
	}
	applicants, err := a.applications.Applicants(c.Request.Context(), middleware.UserID(c), offerID)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	resps := make([]ApplicantResp, len(applicants))
	for i, applicant := range applicants {
		resps[i] = applicantResp(applicant)
	}
	c.JSON(http.StatusOK, resps)
}

// PUT /applications/:id/review (business owner). An approval's response carries the contact: the Match.
func (a *ApplicationController) Review(c *gin.Context) {
	id, ok := pathID(c)
	if !ok {
		response.WriteError(c, model.ErrNotFound)
		return
	}
	var req ReviewReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	applicant, err := a.applications.Review(c.Request.Context(), middleware.UserID(c), id, *req.Approve)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, applicantResp(applicant))
}

// GET /applications/:id/cv (business owner): the applicant's PDF.
func (a *ApplicationController) ApplicantCV(c *gin.Context) {
	id, ok := pathID(c)
	if !ok {
		response.WriteError(c, model.ErrNotFound)
		return
	}
	cv, err := a.applications.ApplicantCV(c.Request.Context(), middleware.UserID(c), id)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.Header("Content-Disposition", mime.FormatMediaType("attachment", map[string]string{"filename": cv.FileName}))
	c.Data(http.StatusOK, "application/pdf", cv.Content)
}

func applicantResp(applicant model.Applicant) ApplicantResp {
	status := string(applicant.Status)
	if applicant.Status == model.ApplicationStatusSent {
		status = "PENDING_REVIEW"
	}
	resp := ApplicantResp{ApplicationID: applicant.ApplicationID.String(), OfferID: applicant.OfferID.String(), CandidateName: applicant.CandidateName, CVFileName: applicant.CVFileName.Ptr(), Status: status}
	if applicant.CandidateEmail != "" {
		resp.ContactEmail = &applicant.CandidateEmail
	}
	return resp
}
