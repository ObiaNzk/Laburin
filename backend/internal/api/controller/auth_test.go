package controller

//go:generate mockgen -source=auth.go -destination=mocks/auth.go -package=mocks

import (
	"net/http"
	"testing"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/controller/mocks"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type authControllerSetup struct {
	auth   *mocks.MockAuthService
	router http.Handler
}

func setupAuthController(t *testing.T) authControllerSetup {
	auth := mocks.NewMockAuthService(gomock.NewController(t))
	controller := NewAuthController(auth)
	router := newRouter()
	router.POST("/auth/login", controller.Login)
	router.POST("/auth/signup/candidate", controller.SignUpCandidate)
	router.POST("/auth/signup/business", controller.SignUpBusiness)
	return authControllerSetup{auth: auth, router: router}
}

var sessionUser = model.User{ID: uuid.MustParse("11111111-1111-1111-1111-111111111111"), Role: model.RoleCandidate, Email: "[email]@demo.laburin", PasswordHash: "secret-hash", DisplayName: "[NOMBRE]"}

// The password hash must never be in the body.
const sessionJSON = `{"token":"t-1","user":{"id":"11111111-1111-1111-1111-111111111111","role":"CANDIDATE","displayName":"[NOMBRE]","email":"[email]@demo.laburin"}}`

func TestLoginController_Success(t *testing.T) {
	// begin
	setup := setupAuthController(t)
	setup.auth.EXPECT().Login(gomock.Any(), "[email]@demo.laburin", "demo1234").Return(service.Session{Token: "t-1", User: sessionUser}, nil)

	// when
	rec := do(setup.router, http.MethodPost, "/auth/login", `{"email":"[email]@demo.laburin","password":"demo1234"}`)

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, sessionJSON, rec.Body.String())
}

func TestLoginController_WrongCredentials(t *testing.T) {
	// begin
	setup := setupAuthController(t)
	setup.auth.EXPECT().Login(gomock.Any(), "[email]@demo.laburin", "x").Return(service.Session{}, model.ErrUnauthorized)

	// when
	rec := do(setup.router, http.MethodPost, "/auth/login", `{"email":"[email]@demo.laburin","password":"x"}`)

	// then
	require.Equal(t, http.StatusUnauthorized, rec.Code)
}

func TestLoginController_MalformedBody(t *testing.T) {
	// begin
	setup := setupAuthController(t)

	// when
	rec := do(setup.router, http.MethodPost, "/auth/login", `{"email":`)

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}

func TestSignUpCandidateController_Created(t *testing.T) {
	// begin
	setup := setupAuthController(t)
	params := service.SignUpCandidateParams{FirstName: "[NOMBRE]", LastName: "[APELLIDO]", Email: "[email]@demo.laburin", Password: "demo1234"}
	setup.auth.EXPECT().SignUpCandidate(gomock.Any(), params).Return(service.Session{Token: "t-1", User: sessionUser}, nil)

	// when
	rec := do(setup.router, http.MethodPost, "/auth/signup/candidate", `{"firstName":"[NOMBRE]","lastName":"[APELLIDO]","email":"[email]@demo.laburin","password":"demo1234"}`)

	// then
	require.Equal(t, http.StatusCreated, rec.Code)
	require.JSONEq(t, sessionJSON, rec.Body.String())
}

func TestSignUpCandidateController_EmailTaken(t *testing.T) {
	// begin
	setup := setupAuthController(t)
	setup.auth.EXPECT().SignUpCandidate(gomock.Any(), gomock.Any()).Return(service.Session{}, model.ErrConflict)

	// when
	rec := do(setup.router, http.MethodPost, "/auth/signup/candidate", `{"firstName":"a","lastName":"b","email":"[email]@demo.laburin","password":"demo1234"}`)

	// then
	require.Equal(t, http.StatusConflict, rec.Code)
}

func TestSignUpBusinessController_Created(t *testing.T) {
	// begin
	setup := setupAuthController(t)
	business := sessionUser
	business.Role = model.RoleBusiness
	params := service.SignUpBusinessParams{BusinessName: "[COMERCIO]", Category: model.CategoryGastronomy, Schedule: "9 a 17", Address: "[DIRECCION]", Latitude: -34.6, Longitude: -58.38, Email: "[email]@demo.laburin", Password: "demo1234"}
	setup.auth.EXPECT().SignUpBusiness(gomock.Any(), params).Return(service.Session{Token: "t-1", User: business}, nil)
	body := `{"businessName":"[COMERCIO]","category":"GASTRONOMY","schedule":"9 a 17","address":"[DIRECCION]","latitude":-34.6,"longitude":-58.38,"email":"[email]@demo.laburin","password":"demo1234"}`

	// when
	rec := do(setup.router, http.MethodPost, "/auth/signup/business", body)

	// then
	require.Equal(t, http.StatusCreated, rec.Code)
	require.JSONEq(t, `{"token":"t-1","user":{"id":"11111111-1111-1111-1111-111111111111","role":"BUSINESS","displayName":"[NOMBRE]","email":"[email]@demo.laburin"}}`, rec.Body.String())
}

func TestSignUpBusinessController_MissingLocationIsBadRequest(t *testing.T) {
	// begin
	setup := setupAuthController(t)
	body := `{"businessName":"[COMERCIO]","category":"GASTRONOMY","schedule":"9 a 17","address":"[DIRECCION]","email":"[email]@demo.laburin","password":"demo1234"}`

	// when
	rec := do(setup.router, http.MethodPost, "/auth/signup/business", body)

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}
