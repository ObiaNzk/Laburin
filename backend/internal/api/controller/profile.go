package controller

import (
	"context"
	"fmt"
	"io"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/middleware"
	"github.com/laburin/backend/internal/api/response"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service"
)

type ProfileService interface {
	UploadCV(ctx context.Context, userID uuid.UUID, cv model.CV) error
	CVFileName(ctx context.Context, userID uuid.UUID) (string, error)
	SavePushToken(ctx context.Context, userID uuid.UUID, token string) error
}

type ProfileController struct {
	profile ProfileService
}

func NewProfileController(profile ProfileService) *ProfileController {
	return &ProfileController{profile: profile}
}

type CVResp struct {
	FileName string `json:"fileName"`
}

type PushTokenReq struct {
	Token string `json:"token" binding:"required"`
}

// multipartOverhead leaves room for the multipart headers around a CV of the maximum size.
const multipartOverhead = 64 * 1024

// PUT /me/cv (candidate): multipart/form-data with the PDF in the "file" part.
func (p *ProfileController) UploadCV(c *gin.Context) {
	c.Request.Body = http.MaxBytesReader(c.Writer, c.Request.Body, service.MaxCVBytes+multipartOverhead)
	header, err := c.FormFile("file")
	if err != nil {
		response.WriteError(c, fmt.Errorf("%w: a \"file\" part of up to %d bytes is required", model.ErrInvalid, service.MaxCVBytes))
		return
	}
	file, err := header.Open()
	if err != nil {
		response.WriteError(c, err)
		return
	}
	defer file.Close()
	content, err := io.ReadAll(io.LimitReader(file, service.MaxCVBytes+1))
	if err != nil {
		response.WriteError(c, err)
		return
	}
	cv := model.CV{FileName: header.Filename, Content: content}
	if err := p.profile.UploadCV(c.Request.Context(), middleware.UserID(c), cv); err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, CVResp{FileName: header.Filename})
}

// GET /me/cv (candidate): 404 until a CV is uploaded.
func (p *ProfileController) GetCV(c *gin.Context) {
	name, err := p.profile.CVFileName(c.Request.Context(), middleware.UserID(c))
	if err != nil {
		response.WriteError(c, err)
		return
	}
	c.JSON(http.StatusOK, CVResp{FileName: name})
}

// POST /me/push-token (any account): the FCM token for this device.
func (p *ProfileController) SavePushToken(c *gin.Context) {
	var req PushTokenReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.WriteError(c, fmt.Errorf("%w: %v", model.ErrInvalid, err))
		return
	}
	if err := p.profile.SavePushToken(c.Request.Context(), middleware.UserID(c), req.Token); err != nil {
		response.WriteError(c, err)
		return
	}
	c.Status(http.StatusNoContent)
}
