package controller

import (
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
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
