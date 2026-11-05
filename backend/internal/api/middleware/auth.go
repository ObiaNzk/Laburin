// Package middleware authenticates requests with the session token.
package middleware

import (
	"slices"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/response"
	"github.com/laburin/backend/internal/auth"
	"github.com/laburin/backend/internal/model"
)

type TokenParser interface {
	Parse(token string) (auth.Claims, error)
}

const claimsKey = "claims"

// Authorized requires "Authorization: Bearer <token>": 401 without a valid token, 403 when the role isn't allowed.
// With no roles, any logged-in user passes.
func Authorized(tokens TokenParser, roles ...model.Role) gin.HandlerFunc {
	return func(c *gin.Context) {
		token, found := strings.CutPrefix(c.GetHeader("Authorization"), "Bearer ")
		if !found || token == "" {
			response.WriteError(c, model.ErrUnauthorized)
			return
		}
		claims, err := tokens.Parse(token)
		if err != nil {
			response.WriteError(c, model.ErrUnauthorized)
			return
		}
		if len(roles) > 0 && !slices.Contains(roles, claims.Role) {
			response.WriteError(c, model.ErrForbidden)
			return
		}
		SetClaims(c, claims)
		c.Next()
	}
}

// SetClaims stores the authenticated user in the request context.
func SetClaims(c *gin.Context, claims auth.Claims) {
	c.Set(claimsKey, claims)
}

// UserID is the authenticated user's id; only valid behind Authorized.
func UserID(c *gin.Context) uuid.UUID {
	claims, _ := c.Get(claimsKey)
	authenticated, _ := claims.(auth.Claims)
	return authenticated.UserID
}
