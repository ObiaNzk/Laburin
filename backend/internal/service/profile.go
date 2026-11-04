package service

import (
	"bytes"
	"context"
	"fmt"
	"strings"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
)

type ProfileRepository interface {
	// SaveCV replaces the user's CV.
	SaveCV(ctx context.Context, userID uuid.UUID, cv model.CV) error
	// GetCVFileName returns model.ErrNotFound when the user has no CV.
	GetCVFileName(ctx context.Context, userID uuid.UUID) (string, error)
	SavePushToken(ctx context.Context, userID uuid.UUID, token string) error
}

const MaxCVBytes = 5 * 1024 * 1024

type ProfileService struct {
	repo ProfileRepository
}

func NewProfileService(repo ProfileRepository) *ProfileService {
	return &ProfileService{repo: repo}
}

// UploadCV accepts a PDF of up to MaxCVBytes: the content must start with the PDF signature, whatever its name says.
func (s *ProfileService) UploadCV(ctx context.Context, userID uuid.UUID, cv model.CV) error {
	if strings.TrimSpace(cv.FileName) == "" {
		return fmt.Errorf("%w: file name required", model.ErrInvalid)
	}
	if len(cv.Content) > MaxCVBytes {
		return fmt.Errorf("%w: the CV is larger than %d bytes", model.ErrInvalid, MaxCVBytes)
	}
	if !bytes.HasPrefix(cv.Content, []byte("%PDF")) {
		return fmt.Errorf("%w: the CV is not a PDF", model.ErrInvalid)
	}
	return s.repo.SaveCV(ctx, userID, cv)
}

// CVFileName returns model.ErrNotFound when the user has no CV.
func (s *ProfileService) CVFileName(ctx context.Context, userID uuid.UUID) (string, error) {
	return s.repo.GetCVFileName(ctx, userID)
}

func (s *ProfileService) SavePushToken(ctx context.Context, userID uuid.UUID, token string) error {
	token = strings.TrimSpace(token)
	if token == "" {
		return fmt.Errorf("%w: token required", model.ErrInvalid)
	}
	return s.repo.SavePushToken(ctx, userID, token)
}
