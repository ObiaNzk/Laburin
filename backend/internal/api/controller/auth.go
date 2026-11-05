// Package controller owns the HTTP contract (docs/api/*.md): request and response bodies live here, never in model.
package controller

import (
	"context"
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/laburin/backend/internal/api/response"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service"
)

type AuthService interface {
	Login(ctx context.Context, email, password string) (service.Session, error)
	SignUpCandidate(ctx context.Context, params service.SignUpCandidateParams) (service.Session, error)
	SignUpBusiness(ctx context.Context, params service.SignUpBusinessParams) (service.Session, error)
}

type AuthController struct {
	auth AuthService
}

func NewAuthController(auth AuthService) *AuthController {
	return &AuthController{auth: auth}
}

type LoginReq struct {
	Email    string `json:"email" binding:"required"`
	Password string `json:"password" binding:"required"`
}

type SignUpCandidateReq struct {
	FirstName string `json:"firstName" binding:"required"`
	LastName  string `json:"lastName" binding:"required"`
	Email     string `json:"email" binding:"required"`
	Password  string `json:"password" binding:"required"`
}

type SignUpBusinessReq struct {
	BusinessName string   `json:"businessName" binding:"required"`
	Category     string   `json:"category" binding:"required"`
	Schedule     string   `json:"schedule" binding:"required"`
	Address      string   `json:"address" binding:"required"`
	Latitude     *float64 `json:"latitude" binding:"required"`
	Longitude    *float64 `json:"longitude" binding:"required"`
	Email        string   `json:"email" binding:"required"`
	Password     string   `json:"password" binding:"required"`
}

type UserResp struct {
	ID          string `json:"id"`
	Role        string `json:"role"`
	DisplayName string `json:"displayName"`
	Email       string `json:"email"`
}

type SessionResp struct {
	Token string   `json:"token"`
	User  UserResp `json:"user"`
}

// POST /auth/login
func (a *AuthController) Login(c *gin.Context) {
	var req LoginReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	session, err := a.auth.Login(c.Request.Context(), req.Email, req.Password)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, sessionResp(session))
}

// POST /auth/signup/candidate
func (a *AuthController) SignUpCandidate(c *gin.Context) {
	var req SignUpCandidateReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	params := service.SignUpCandidateParams{FirstName: req.FirstName, LastName: req.LastName, Email: req.Email, Password: req.Password}
	session, err := a.auth.SignUpCandidate(c.Request.Context(), params)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusCreated, sessionResp(session))
}

// POST /auth/signup/business
func (a *AuthController) SignUpBusiness(c *gin.Context) {
	var req SignUpBusinessReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	params := service.SignUpBusinessParams{BusinessName: req.BusinessName, Category: model.Category(req.Category), Schedule: req.Schedule, Address: req.Address, Latitude: *req.Latitude, Longitude: *req.Longitude, Email: req.Email, Password: req.Password}
	session, err := a.auth.SignUpBusiness(c.Request.Context(), params)
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusCreated, sessionResp(session))
}

func sessionResp(session service.Session) SessionResp {
	user := UserResp{ID: session.User.ID.String(), Role: string(session.User.Role), DisplayName: session.User.DisplayName, Email: session.User.Email}
	return SessionResp{Token: session.Token, User: user}
}
