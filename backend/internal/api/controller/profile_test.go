package controller

//go:generate mockgen -source=profile.go -destination=mocks/profile.go -package=mocks

import (
	"bytes"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/controller/mocks"
	"github.com/laburin/backend/internal/model"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type profileControllerSetup struct {
	profile *mocks.MockProfileService
	router  http.Handler
	user    uuid.UUID
}

func setupProfileController(t *testing.T) profileControllerSetup {
	profile := mocks.NewMockProfileService(gomock.NewController(t))
	controller := NewProfileController(profile)
	user := uuid.New()
	router := newRouter()
	authenticated := router.Group("", asUser(user, model.RoleCandidate))
	authenticated.PUT("/me/cv", controller.UploadCV)
	authenticated.GET("/me/cv", controller.GetCV)
	authenticated.POST("/me/push-token", controller.SavePushToken)
	return profileControllerSetup{profile: profile, router: router, user: user}
}

// multipartCV builds the request the app sends: a "file" part with the PDF.
func multipartCV(t *testing.T, field, fileName string, content []byte) *http.Request {
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	part, err := writer.CreateFormFile(field, fileName)
	require.NoError(t, err)
	_, err = part.Write(content)
	require.NoError(t, err)
	require.NoError(t, writer.Close())
	req := httptest.NewRequest(http.MethodPut, "/me/cv", &body)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	return req
}

func TestUploadCVController_PassesTheFile(t *testing.T) {
	// begin
	setup := setupProfileController(t)
	setup.profile.EXPECT().UploadCV(gomock.Any(), setup.user, model.CV{FileName: "cv.pdf", Content: []byte("%PDF-1.4")}).Return(nil)

	// when
	rec := serveRequest(setup.router, multipartCV(t, "file", "cv.pdf", []byte("%PDF-1.4")))

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.JSONEq(t, `{"fileName":"cv.pdf"}`, rec.Body.String())
}

func TestUploadCVController_WithoutFilePartIsBadRequest(t *testing.T) {
	// begin
	setup := setupProfileController(t)

	// when
	rec := serveRequest(setup.router, multipartCV(t, "otro", "cv.pdf", []byte("%PDF-1.4")))

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}

func TestUploadCVController_RejectedFileIsBadRequest(t *testing.T) {
	// begin
	setup := setupProfileController(t)
	setup.profile.EXPECT().UploadCV(gomock.Any(), setup.user, gomock.Any()).Return(model.ErrInvalid)

	// when
	rec := serveRequest(setup.router, multipartCV(t, "file", "cv.pdf", []byte("zip")))

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}

func TestGetCVController_FoundAndMissing(t *testing.T) {
	// begin
	setup := setupProfileController(t)
	gomock.InOrder(
		setup.profile.EXPECT().CVFileName(gomock.Any(), setup.user).Return("cv.pdf", nil),
		setup.profile.EXPECT().CVFileName(gomock.Any(), setup.user).Return("", model.ErrNotFound),
	)

	// when
	found := do(setup.router, http.MethodGet, "/me/cv", "")
	missing := do(setup.router, http.MethodGet, "/me/cv", "")

	// then
	require.Equal(t, http.StatusOK, found.Code)
	require.JSONEq(t, `{"fileName":"cv.pdf"}`, found.Body.String())
	require.Equal(t, http.StatusNotFound, missing.Code)
}

func TestSavePushTokenController_NoContent(t *testing.T) {
	// begin
	setup := setupProfileController(t)
	setup.profile.EXPECT().SavePushToken(gomock.Any(), setup.user, "fcm-1").Return(nil)

	// when
	rec := do(setup.router, http.MethodPost, "/me/push-token", `{"token":"fcm-1"}`)

	// then
	require.Equal(t, http.StatusNoContent, rec.Code)
}

func TestSavePushTokenController_MissingTokenIsBadRequest(t *testing.T) {
	// begin
	setup := setupProfileController(t)

	// when
	rec := do(setup.router, http.MethodPost, "/me/push-token", `{}`)

	// then
	require.Equal(t, http.StatusBadRequest, rec.Code)
}
