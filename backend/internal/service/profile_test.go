package service

//go:generate mockgen -source=profile.go -destination=mocks/profile.go -package=mocks

import (
	"bytes"
	"context"
	"testing"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service/mocks"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type profileTestSetup struct {
	ctrl    *gomock.Controller
	repo    *mocks.MockProfileRepository
	service *ProfileService
}

func setupProfileTest(t *testing.T) profileTestSetup {
	ctrl := gomock.NewController(t)
	repo := mocks.NewMockProfileRepository(ctrl)
	return profileTestSetup{ctrl: ctrl, repo: repo, service: NewProfileService(repo)}
}

func TestUploadCV_StoresAPdf(t *testing.T) {
	// begin
	setup := setupProfileTest(t)
	userID := uuid.New()
	cv := model.CV{FileName: "cv.pdf", Content: []byte("%PDF-1.4 ...")}
	setup.repo.EXPECT().SaveCV(gomock.Any(), userID, cv).Return(nil)

	// when
	err := setup.service.UploadCV(context.Background(), userID, cv)

	// then
	require.NoError(t, err)
}

func TestUploadCV_Invalid(t *testing.T) {
	cases := map[string]model.CV{
		"not a pdf":       {FileName: "cv.pdf", Content: []byte("PK\x03\x04 zip")},
		"empty":           {FileName: "cv.pdf", Content: nil},
		"too large":       {FileName: "cv.pdf", Content: append([]byte("%PDF"), bytes.Repeat([]byte{0}, MaxCVBytes)...)},
		"blank file name": {FileName: " ", Content: []byte("%PDF-1.4")},
	}
	for name, cv := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupProfileTest(t)

			// when
			err := setup.service.UploadCV(context.Background(), uuid.New(), cv)

			// then
			require.ErrorIs(t, err, model.ErrInvalid)
		})
	}
}

func TestCVFileName_ReturnsTheStoredName(t *testing.T) {
	// begin
	setup := setupProfileTest(t)
	userID := uuid.New()
	setup.repo.EXPECT().GetCVFileName(gomock.Any(), userID).Return("cv.pdf", nil)

	// when
	got, err := setup.service.CVFileName(context.Background(), userID)

	// then
	require.NoError(t, err)
	require.Equal(t, "cv.pdf", got)
}

func TestSavePushToken_StoresIt(t *testing.T) {
	// begin
	setup := setupProfileTest(t)
	userID := uuid.New()
	setup.repo.EXPECT().SavePushToken(gomock.Any(), userID, "fcm-token").Return(nil)

	// when
	err := setup.service.SavePushToken(context.Background(), userID, " fcm-token ")

	// then
	require.NoError(t, err)
}

func TestSavePushToken_BlankIsInvalid(t *testing.T) {
	// begin
	setup := setupProfileTest(t)

	// when
	err := setup.service.SavePushToken(context.Background(), uuid.New(), "  ")

	// then
	require.ErrorIs(t, err, model.ErrInvalid)
}
