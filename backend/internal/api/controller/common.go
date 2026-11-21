package controller

import (
	"mime"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
)

// pathID parses the ":id" path parameter; ok is false when it isn't a UUID.
func pathID(c *gin.Context) (uuid.UUID, bool) {
	id, err := uuid.Parse(c.Param("id"))
	return id, err == nil
}

// isoTime is the wire format the app parses: ISO-8601 in UTC, whole seconds.
func isoTime(t time.Time) string {
	return t.UTC().Truncate(time.Second).Format(time.RFC3339)
}

// writePDF sends a CV as a download that keeps its original file name.
func writePDF(c *gin.Context, cv model.CV) {
	c.Header("Content-Disposition", mime.FormatMediaType("attachment", map[string]string{"filename": cv.FileName}))
	c.Data(http.StatusOK, "application/pdf", cv.Content)
}
