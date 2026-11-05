package controller

import (
	"context"
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/laburin/backend/internal/api/middleware"
	"github.com/laburin/backend/internal/api/response"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service"
)

type OfferService interface {
	Nearby(ctx context.Context, params service.NearbyParams) ([]model.Offer, error)
	Get(ctx context.Context, id uuid.UUID) (model.Offer, error)
	Put(ctx context.Context, businessID, offerID uuid.UUID, content model.OfferContent) (bool, error)
	Close(ctx context.Context, businessID, offerID uuid.UUID) error
	Mine(ctx context.Context, businessID uuid.UUID) ([]model.Offer, error)
}

type OfferController struct {
	offers OfferService
}

func NewOfferController(offers OfferService) *OfferController {
	return &OfferController{offers: offers}
}

type NearbyReq struct {
	Latitude  *float64 `form:"lat" binding:"required"`
	Longitude *float64 `form:"lng" binding:"required"`
	RadiusKm  *int     `form:"radiusKm" binding:"required"`
	Category  string   `form:"category"`
}

// SalaryBody is the salary on the wire, both ways: whole pesos, max null for a single amount.
type SalaryBody struct {
	Min    int64  `json:"min"`
	Max    *int64 `json:"max"`
	Period string `json:"period"`
}

// PutOfferReq: salary null means "A convenir".
type PutOfferReq struct {
	Title       string      `json:"title"`
	Category    string      `json:"category"`
	Schedule    string      `json:"schedule"`
	Description string      `json:"description"`
	Salary      *SalaryBody `json:"salary"`
}

type OfferResp struct {
	ID           string      `json:"id"`
	Title        string      `json:"title"`
	Category     string      `json:"category"`
	Description  string      `json:"description"`
	Schedule     string      `json:"schedule"`
	JobType      string      `json:"jobType"`
	BusinessName string      `json:"businessName"`
	Address      string      `json:"address"`
	Latitude     float64     `json:"latitude"`
	Longitude    float64     `json:"longitude"`
	Status       string      `json:"status"`
	UpdatedAt    string      `json:"updatedAt"`
	Salary       *SalaryBody `json:"salary"`
}

// GET /offers?lat=&lng=&radiusKm=&category= (public)
func (o *OfferController) Nearby(c *gin.Context) {
	var req NearbyReq
	if err := c.ShouldBindQuery(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	params := service.NearbyParams{Latitude: *req.Latitude, Longitude: *req.Longitude, RadiusKm: *req.RadiusKm}
	if req.Category != "" {
		category := model.Category(req.Category)
		params.Category = &category
	}
	offers, err := o.offers.Nearby(c.Request.Context(), params)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, offerResps(offers))
}

// GET /offers/:id (public, any status)
func (o *OfferController) Get(c *gin.Context) {
	id, ok := pathID(c)
	if !ok {
		response.WriteError(c, model.ErrNotFound)
		return
	}
	offer, err := o.offers.Get(c.Request.Context(), id)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, offerResp(offer))
}

// PUT /offers/:id (business): publish when new, replace the content when it exists.
func (o *OfferController) Put(c *gin.Context) {
	id, ok := pathID(c)
	if !ok {
		response.WriteError(c, fmt.Errorf("%w: the offer id must be a UUID", model.ErrInvalid))
		return
	}
	var req PutOfferReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	var salary *model.Salary
	if req.Salary != nil {
		valid, err := model.NewSalary(req.Salary.Min, null.IntFromPtr(req.Salary.Max), model.SalaryPeriod(req.Salary.Period))
		if err != nil {
			response.WriteError(c, err)
			return
		}
		salary = &valid
	}
	content, err := model.NewOfferContent(req.Title, model.Category(req.Category), req.Schedule, req.Description, salary)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	created, err := o.offers.Put(c.Request.Context(), middleware.UserID(c), id, content)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	if created {
		c.Status(http.StatusCreated)
		return
	}
	c.Status(http.StatusOK)
}

// POST /offers/:id/close (business owner). An id that isn't a UUID can't exist: 404, which the app takes as closed.
func (o *OfferController) Close(c *gin.Context) {
	id, ok := pathID(c)
	if !ok {
		response.WriteError(c, model.ErrNotFound)
		return
	}
	if err := o.offers.Close(c.Request.Context(), middleware.UserID(c), id); err != nil {
		response.WriteError(c, err)
		return
	}
	c.Status(http.StatusNoContent)
}

// GET /offers/mine (business)
func (o *OfferController) Mine(c *gin.Context) {
	offers, err := o.offers.Mine(c.Request.Context(), middleware.UserID(c))
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, offerResps(offers))
}

func offerResps(offers []model.Offer) []OfferResp {
	resps := make([]OfferResp, len(offers))
	for i, offer := range offers {
		resps[i] = offerResp(offer)
	}
	return resps
}

func offerResp(offer model.Offer) OfferResp {
	resp := OfferResp{
		ID: offer.ID.String(), Title: offer.Content.Title, Category: string(offer.Content.Category), Description: offer.Content.Description,
		Schedule: offer.Content.Schedule, JobType: offer.JobType, BusinessName: offer.BusinessName, Address: offer.Address,
		Latitude: offer.Latitude, Longitude: offer.Longitude, Status: string(offer.Status), UpdatedAt: isoTime(offer.UpdatedAt),
	}
	if salary := offer.Content.Salary; salary != nil {
		resp.Salary = &SalaryBody{Min: salary.Min, Max: salary.Max.Ptr(), Period: string(salary.Period)}
	}
	return resp
}
