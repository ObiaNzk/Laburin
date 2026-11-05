package service

//go:generate mockgen -source=auth.go -destination=mocks/auth.go -package=mocks

import (
	"context"
	"errors"
	"testing"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service/mocks"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
	"golang.org/x/crypto/bcrypt"
)

type authTestSetup struct {
	ctrl    *gomock.Controller
	users   *mocks.MockUserRepository
	tokens  *mocks.MockTokenIssuer
	service *AuthService
}

func setupAuthTest(t *testing.T) authTestSetup {
	ctrl := gomock.NewController(t)
	users := mocks.NewMockUserRepository(ctrl)
	tokens := mocks.NewMockTokenIssuer(ctrl)
	return authTestSetup{ctrl: ctrl, users: users, tokens: tokens, service: NewAuthService(users, tokens, bcrypt.MinCost)}
}

func hash(t *testing.T, password string) string {
	h, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.MinCost)
	require.NoError(t, err)
	return string(h)
}

func TestLogin_Success(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	user := model.User{ID: uuid.New(), Role: model.RoleCandidate, Email: "[email]@demo.laburin", PasswordHash: hash(t, "demo1234"), DisplayName: "[NOMBRE]"}
	setup.users.EXPECT().GetUserByEmail(gomock.Any(), "[email]@demo.laburin").Return(user, nil)
	setup.tokens.EXPECT().Issue(user).Return("token-1", nil)

	// when
	got, err := setup.service.Login(context.Background(), " [EMAIL]@Demo.Laburin ", "demo1234")

	// then
	require.NoError(t, err)
	require.Equal(t, Session{Token: "token-1", User: user}, got)
}

func TestLogin_WrongPassword(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	user := model.User{ID: uuid.New(), Role: model.RoleCandidate, Email: "[email]@demo.laburin", PasswordHash: hash(t, "demo1234")}
	setup.users.EXPECT().GetUserByEmail(gomock.Any(), "[email]@demo.laburin").Return(user, nil)

	// when
	_, err := setup.service.Login(context.Background(), "[email]@demo.laburin", "otra-clave")

	// then
	require.ErrorIs(t, err, model.ErrUnauthorized)
}

func TestLogin_UnknownEmail(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	setup.users.EXPECT().GetUserByEmail(gomock.Any(), "[email]@demo.laburin").Return(model.User{}, model.ErrNotFound)

	// when
	_, err := setup.service.Login(context.Background(), "[email]@demo.laburin", "demo1234")

	// then
	require.ErrorIs(t, err, model.ErrUnauthorized)
}

func TestLogin_RepositoryError(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	boom := errors.New("db down")
	setup.users.EXPECT().GetUserByEmail(gomock.Any(), "[email]@demo.laburin").Return(model.User{}, boom)

	// when
	_, err := setup.service.Login(context.Background(), "[email]@demo.laburin", "demo1234")

	// then
	require.ErrorIs(t, err, boom)
	require.NotErrorIs(t, err, model.ErrUnauthorized)
}

func TestSignUpCandidate_Success(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	var created model.User
	setup.users.EXPECT().CreateCandidate(gomock.Any(), gomock.Any()).DoAndReturn(func(_ context.Context, got model.User) error {
		require.NotEqual(t, uuid.Nil, got.ID)
		require.NoError(t, bcrypt.CompareHashAndPassword([]byte(got.PasswordHash), []byte("demo1234")))
		expected := model.User{ID: got.ID, Role: model.RoleCandidate, Email: "[email]@demo.laburin", PasswordHash: got.PasswordHash, DisplayName: "[NOMBRE] [APELLIDO]"}
		require.Equal(t, expected, got)
		created = got
		return nil
	})
	setup.tokens.EXPECT().Issue(gomock.Any()).DoAndReturn(func(got model.User) (string, error) {
		require.Equal(t, created, got)
		return "token-1", nil
	})

	// when
	got, err := setup.service.SignUpCandidate(context.Background(), SignUpCandidateParams{FirstName: " [NOMBRE] ", LastName: "[APELLIDO]", Email: "[EMAIL]@demo.laburin", Password: "demo1234"})

	// then
	require.NoError(t, err)
	require.Equal(t, Session{Token: "token-1", User: created}, got)
}

func TestSignUpCandidate_EmailTaken(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	setup.users.EXPECT().CreateCandidate(gomock.Any(), gomock.Any()).Return(model.ErrConflict)

	// when
	_, err := setup.service.SignUpCandidate(context.Background(), SignUpCandidateParams{FirstName: "a", LastName: "b", Email: "[email]@demo.laburin", Password: "demo1234"})

	// then
	require.ErrorIs(t, err, model.ErrConflict)
}

func TestSignUpCandidate_Invalid(t *testing.T) {
	cases := map[string]SignUpCandidateParams{
		"short password": {FirstName: "a", LastName: "b", Email: "[email]@demo.laburin", Password: "1234567"},
		"bad email":      {FirstName: "a", LastName: "b", Email: "sin-arroba", Password: "demo1234"},
		"blank name":     {FirstName: " ", LastName: "b", Email: "[email]@demo.laburin", Password: "demo1234"},
		"blank lastname": {FirstName: "a", LastName: "", Email: "[email]@demo.laburin", Password: "demo1234"},
	}
	for name, params := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupAuthTest(t)

			// when
			_, err := setup.service.SignUpCandidate(context.Background(), params)

			// then
			require.ErrorIs(t, err, model.ErrInvalid)
		})
	}
}

func TestSignUpBusiness_Success(t *testing.T) {
	// begin
	setup := setupAuthTest(t)
	var created model.User
	setup.users.EXPECT().CreateBusiness(gomock.Any(), gomock.Any(), gomock.Any()).DoAndReturn(func(_ context.Context, user model.User, business model.Business) error {
		require.NoError(t, bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte("demo1234")))
		expectedUser := model.User{ID: user.ID, Role: model.RoleBusiness, Email: "[email]@demo.laburin", PasswordHash: user.PasswordHash, DisplayName: "[COMERCIO]"}
		require.Equal(t, expectedUser, user)
		expectedBusiness := model.Business{UserID: user.ID, Name: "[COMERCIO]", Category: model.CategoryGastronomy, Schedule: "9 a 17", Address: "[DIRECCION]", Latitude: -34.6, Longitude: -58.38}
		require.Equal(t, expectedBusiness, business)
		created = user
		return nil
	})
	setup.tokens.EXPECT().Issue(gomock.Any()).DoAndReturn(func(got model.User) (string, error) {
		require.Equal(t, created, got)
		return "token-2", nil
	})
	params := SignUpBusinessParams{BusinessName: "[COMERCIO]", Category: model.CategoryGastronomy, Schedule: "9 a 17", Address: "[DIRECCION]", Latitude: -34.6, Longitude: -58.38, Email: "[EMAIL]@demo.laburin", Password: "demo1234"}

	// when
	got, err := setup.service.SignUpBusiness(context.Background(), params)

	// then
	require.NoError(t, err)
	require.Equal(t, Session{Token: "token-2", User: created}, got)
}

func TestSignUpBusiness_Invalid(t *testing.T) {
	valid := SignUpBusinessParams{BusinessName: "c", Category: model.CategoryGastronomy, Schedule: "s", Address: "a", Latitude: -34.6, Longitude: -58.38, Email: "[email]@demo.laburin", Password: "demo1234"}
	cases := map[string]func(p *SignUpBusinessParams){
		"blank name":       func(p *SignUpBusinessParams) { p.BusinessName = "" },
		"unknown category": func(p *SignUpBusinessParams) { p.Category = "PLUMBING" },
		"blank schedule":   func(p *SignUpBusinessParams) { p.Schedule = " " },
		"blank address":    func(p *SignUpBusinessParams) { p.Address = "" },
		"latitude":         func(p *SignUpBusinessParams) { p.Latitude = 91 },
		"longitude":        func(p *SignUpBusinessParams) { p.Longitude = -181 },
		"short password":   func(p *SignUpBusinessParams) { p.Password = "x" },
		"bad email":        func(p *SignUpBusinessParams) { p.Email = "x" },
	}
	for name, change := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupAuthTest(t)
			params := valid
			change(&params)

			// when
			_, err := setup.service.SignUpBusiness(context.Background(), params)

			// then
			require.ErrorIs(t, err, model.ErrInvalid)
		})
	}
}
