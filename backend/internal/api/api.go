// Package api wires the HTTP routes to the controllers, with the role each route needs.
package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/laburin/backend/internal/api/controller"
	"github.com/laburin/backend/internal/api/middleware"
	"github.com/laburin/backend/internal/model"
)

type Controllers struct {
	Auth         *controller.AuthController
	Offers       *controller.OfferController
	Applications *controller.ApplicationController
	Profile      *controller.ProfileController
}

type HealthResp struct {
	Status string `json:"status"`
}

// NewRouter registers every endpoint of docs/api/*.md. Extra middleware (e.g. the request logger) runs on every route.
func NewRouter(tokens middleware.TokenParser, controllers Controllers, extra ...gin.HandlerFunc) *gin.Engine {
	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(extra...)
	router.MaxMultipartMemory = 8 << 20

	router.GET("/healthz", func(c *gin.Context) { c.JSON(http.StatusOK, HealthResp{Status: "ok"}) })

	// Public: the map can be browsed without an account (RF-01).
	router.POST("/auth/login", controllers.Auth.Login)
	router.POST("/auth/signup/candidate", controllers.Auth.SignUpCandidate)
	router.POST("/auth/signup/business", controllers.Auth.SignUpBusiness)
	router.GET("/offers", controllers.Offers.Nearby)
	router.GET("/offers/:id", controllers.Offers.Get)

	business := router.Group("", middleware.Authorized(tokens, model.RoleBusiness))
	business.GET("/offers/mine", controllers.Offers.Mine)
	business.PUT("/offers/:id", controllers.Offers.Put)
	business.POST("/offers/:id/close", controllers.Offers.Close)
	business.GET("/offers/:id/applicants", controllers.Applications.Applicants)
	business.PUT("/applications/:id/review", controllers.Applications.Review)
	business.GET("/applications/:id/cv", controllers.Applications.ApplicantCV)

	candidate := router.Group("", middleware.Authorized(tokens, model.RoleCandidate))
	candidate.PUT("/applications/:id", controllers.Applications.Apply)
	candidate.GET("/applications/mine", controllers.Applications.Mine)
	candidate.PUT("/me/cv", controllers.Profile.UploadCV)
	candidate.GET("/me/cv", controllers.Profile.GetCV)

	anyAccount := router.Group("", middleware.Authorized(tokens))
	anyAccount.POST("/me/push-token", controllers.Profile.SavePushToken)

	return router
}
