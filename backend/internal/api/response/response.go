// Package response writes the error bodies; each controller owns its success bodies.
package response

import (
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/laburin/backend/internal/model"
	"github.com/rs/zerolog/log"
)

type ErrorBody struct {
	Error string `json:"error"`
}

// WriteError maps a domain error to its HTTP status. Unknown errors are logged and answered as 500 without details.
func WriteError(c *gin.Context, err error) {
	status := http.StatusInternalServerError
	switch {
	case errors.Is(err, model.ErrInvalid):
		status = http.StatusBadRequest
	case errors.Is(err, model.ErrUnauthorized):
		status = http.StatusUnauthorized
	case errors.Is(err, model.ErrForbidden):
		status = http.StatusForbidden
	case errors.Is(err, model.ErrNotFound):
		status = http.StatusNotFound
	case errors.Is(err, model.ErrConflict):
		status = http.StatusConflict
	case errors.Is(err, model.ErrCVRequired):
		status = http.StatusUnprocessableEntity
	}
	message := err.Error()
	if status == http.StatusInternalServerError {
		log.Error().Err(err).Str("path", c.FullPath()).Msg("request failed")
		message = "internal error"
	}
	c.AbortWithStatusJSON(status, ErrorBody{Error: message})
}
