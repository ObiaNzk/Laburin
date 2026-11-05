package controller

//go:generate mockgen -source=offer.go -destination=mocks/offer.go -package=mocks

import (
	"net/http"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/laburin/backend/internal/api/controller/mocks"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type offerControllerSetup struct {
	offers   *mocks.MockOfferService
	router   http.Handler
	business uuid.UUID
}

func setupOfferController(t *testing.T) offerControllerSetup {
	offers := mocks.NewMockOfferService(gomock.NewController(t))
	controller := NewOfferController(offers)
	business := uuid.New()
	router := newRouter()
	router.GET("/offers", controller.Nearby)
	router.GET("/offers/:id", controller.Get)
	authenticated := router.Group("", asUser(business, model.RoleBusiness))
	authenticated.GET("/offers/mine", controller.Mine)
	authenticated.PUT("/offers/:id", controller.Put)
	authenticated.POST("/offers/:id/close", controller.Close)
	return offerControllerSetup{offers: offers, router: router, business: business}
}

var (
	offerID     = uuid.MustParse("22222222-2222-2222-2222-222222222222")
	sampleOffer = model.Offer{
		ID: offerID, BusinessID: uuid.New(),
		Content:      model.OfferContent{Title: "Mozo", Category: model.CategoryGastronomy, Schedule: "Sábados", Description: "Salón", Salary: &model.Salary{Min: 600_000, Period: model.SalaryPeriodMonth}},
		BusinessName: "[COMERCIO]", Address: "[DIRECCION]", Latitude: -34.6, Longitude: -58.38, Status: model.OfferStatusActive,
		UpdatedAt: time.Date(2026, 11, 1, 12, 0, 0, 123, time.UTC),
	}
)

const sampleOfferJSON = `{"id":"22222222-2222-2222-2222-222222222222","title":"Mozo","category":"GASTRONOMY","description":"Salón",
	"schedule":"Sábados","jobType":"","businessName":"[COMERCIO]","address":"[DIRECCION]","latitude":-34.6,"longitude":-58.38,
	"status":"ACTIVE","updatedAt":"2026-11-01T12:00:00Z","salary":{"min":600000,"max":null,"period":"MONTH"}}`

func TestNearbyController_ParsesTheQueryAndKeepsTheOrder(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	category := model.CategoryGastronomy
	noSalary := sampleOffer
	noSalary.ID = uuid.MustParse("33333333-3333-3333-3333-333333333333")
	noSalary.Content.Salary = nil
	params := service.NearbyParams{Latitude: -34.6037, Longitude: -58.3816, RadiusKm: 5, Category: &category}
	setup.offers.EXPECT().Nearby(gomock.Any(), params).Return([]model.Offer{sampleOffer, noSalary}, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/offers?lat=-34.6037&lng=-58.3816&radiusKm=5&category=GASTRONOMY", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	noSalaryJSON := `{"id":"33333333-3333-3333-3333-333333333333","title":"Mozo","category":"GASTRONOMY","description":"Salón",
		"schedule":"Sábados","jobType":"","businessName":"[COMERCIO]","address":"[DIRECCION]","latitude":-34.6,"longitude":-58.38,
		"status":"ACTIVE","updatedAt":"2026-11-01T12:00:00Z","salary":null}`
	require.JSONEq(t, "["+sampleOfferJSON+","+noSalaryJSON+"]", rec.Body.String())
}

func TestNearbyController_WithoutCategoryAndEmpty(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	setup.offers.EXPECT().Nearby(gomock.Any(), service.NearbyParams{Latitude: -34.6, Longitude: -58.38, RadiusKm: 2}).Return([]model.Offer{}, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/offers?lat=-34.6&lng=-58.38&radiusKm=2", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, `[]`, rec.Body.String())
}

func TestNearbyController_MissingOrBadQueryIsBadRequest(t *testing.T) {
	for _, query := range []string{"lng=-58&radiusKm=2", "lat=-34&radiusKm=2", "lat=-34&lng=-58", "lat=x&lng=-58&radiusKm=2", "lat=-34&lng=-58&radiusKm=2.5"} {
		t.Run(query, func(t *testing.T) {
			// begin
			setup := setupOfferController(t)

			// when
			rec := do(setup.router, http.MethodGet, "/offers?"+query, "")

			// then
			require.Equal(t, http.StatusBadRequest, rec.Code)
		})
	}
}

func TestGetOfferController_Found(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	setup.offers.EXPECT().Get(gomock.Any(), offerID).Return(sampleOffer, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/offers/"+offerID.String(), "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, sampleOfferJSON, rec.Body.String())
}

func TestGetOfferController_UnknownOrNotAnIdIsNotFound(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	setup.offers.EXPECT().Get(gomock.Any(), offerID).Return(model.Offer{}, model.ErrNotFound)

	// when
	unknown := do(setup.router, http.MethodGet, "/offers/"+offerID.String(), "")
	notAnID := do(setup.router, http.MethodGet, "/offers/f-1", "")

	// then
	require.Equal(t, http.StatusNotFound, unknown.Code)
	require.Equal(t, http.StatusNotFound, notAnID.Code)
}

const putBody = `{"title":"Mozo","category":"GASTRONOMY","schedule":"Sábados","description":"Salón","salary":{"min":5000,"max":7000,"period":"HOUR"}}`

func TestPutOfferController_CreatedAndUpdated(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	content := model.OfferContent{Title: "Mozo", Category: model.CategoryGastronomy, Schedule: "Sábados", Description: "Salón", Salary: &model.Salary{Min: 5_000, Max: null.IntFrom(7_000), Period: model.SalaryPeriodHour}}
	gomock.InOrder(
		setup.offers.EXPECT().Put(gomock.Any(), setup.business, offerID, content).Return(true, nil),
		setup.offers.EXPECT().Put(gomock.Any(), setup.business, offerID, content).Return(false, nil),
	)

	// when
	created := do(setup.router, http.MethodPut, "/offers/"+offerID.String(), putBody)
	updated := do(setup.router, http.MethodPut, "/offers/"+offerID.String(), putBody)

	// then
	require.Equal(t, http.StatusCreated, created.Code)
	require.Equal(t, http.StatusOK, updated.Code)
}

func TestPutOfferController_NullSalaryIsNoSalary(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	content := model.OfferContent{Title: "Mozo", Category: model.CategoryGastronomy, Schedule: "Sábados", Description: "Salón"}
	setup.offers.EXPECT().Put(gomock.Any(), setup.business, offerID, content).Return(true, nil)

	// when
	rec := do(setup.router, http.MethodPut, "/offers/"+offerID.String(), `{"title":"Mozo","category":"GASTRONOMY","schedule":"Sábados","description":"Salón","salary":null}`)

	// then
	require.Equal(t, http.StatusCreated, rec.Code)
}

func TestPutOfferController_InvalidContentNeverReachesTheService(t *testing.T) {
	cases := map[string]string{
		"zero salary":      `{"title":"Mozo","category":"GASTRONOMY","schedule":"s","description":"d","salary":{"min":0,"max":null,"period":"MONTH"}}`,
		"unknown period":   `{"title":"Mozo","category":"GASTRONOMY","schedule":"s","description":"d","salary":{"min":5,"max":null,"period":"WEEK"}}`,
		"blank title":      `{"title":" ","category":"GASTRONOMY","schedule":"s","description":"d","salary":null}`,
		"unknown category": `{"title":"Mozo","category":"PLUMBING","schedule":"s","description":"d","salary":null}`,
		"malformed":        `{"title":`,
	}
	for name, body := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupOfferController(t)

			// when
			rec := do(setup.router, http.MethodPut, "/offers/"+offerID.String(), body)

			// then
			require.Equal(t, http.StatusBadRequest, rec.Code)
		})
	}
}

func TestPutOfferController_NotAnIdIsBadRequest(t *testing.T) {
	// begin
	setup := setupOfferController(t)

	// when
	rec := do(setup.router, http.MethodPut, "/offers/f-1", putBody)

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}

func TestPutOfferController_ServiceErrors(t *testing.T) {
	cases := map[error]int{model.ErrConflict: http.StatusConflict, model.ErrForbidden: http.StatusForbidden}
	for err, status := range cases {
		t.Run(err.Error(), func(t *testing.T) {
			// begin
			setup := setupOfferController(t)
			setup.offers.EXPECT().Put(gomock.Any(), setup.business, offerID, gomock.Any()).Return(false, err)

			// when
			rec := do(setup.router, http.MethodPut, "/offers/"+offerID.String(), putBody)

			// then
			require.Equal(t, status, rec.Code)
		})
	}
}

func TestCloseOfferController_NoContent(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	setup.offers.EXPECT().Close(gomock.Any(), setup.business, offerID).Return(nil)

	// when
	rec := do(setup.router, http.MethodPost, "/offers/"+offerID.String()+"/close", "")

	// then
	require.Equal(t, http.StatusNoContent, rec.Code)
}

func TestCloseOfferController_UnknownOrNotAnIdIsNotFound(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	setup.offers.EXPECT().Close(gomock.Any(), setup.business, offerID).Return(model.ErrNotFound)

	// when
	unknown := do(setup.router, http.MethodPost, "/offers/"+offerID.String()+"/close", "")
	notAnID := do(setup.router, http.MethodPost, "/offers/f-1/close", "")

	// then
	require.Equal(t, http.StatusNotFound, unknown.Code)
	require.Equal(t, http.StatusNotFound, notAnID.Code)
}

func TestMyOffersController_ListsThem(t *testing.T) {
	// begin
	setup := setupOfferController(t)
	setup.offers.EXPECT().Mine(gomock.Any(), setup.business).Return([]model.Offer{sampleOffer}, nil)

	// when
	rec := do(setup.router, http.MethodGet, "/offers/mine", "")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, "["+sampleOfferJSON+"]", rec.Body.String())
}
