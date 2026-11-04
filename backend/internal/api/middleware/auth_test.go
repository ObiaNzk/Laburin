package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/auth"
	"github.com/laburin/backend/internal/model"
	"github.com/stretchr/testify/require"
)

type fakeParser map[string]auth.Claims

func (f fakeParser) Parse(token string) (auth.Claims, error) {
	claims, ok := f[token]
	if !ok {
		return auth.Claims{}, model.ErrUnauthorized
	}
	return claims, nil
}

var (
	businessID = uuid.New()
	parser     = fakeParser{"business-token": {UserID: businessID, Role: model.RoleBusiness}}
)

func serve(t *testing.T, header string, roles ...model.Role) (*httptest.ResponseRecorder, uuid.UUID) {
	gin.SetMode(gin.TestMode)
	router := gin.New()
	var seen uuid.UUID
	router.GET("/x", Authorized(parser, roles...), func(c *gin.Context) {
		seen = UserID(c)
		c.Status(http.StatusOK)
	})
	req := httptest.NewRequest(http.MethodGet, "/x", nil)
	if header != "" {
		req.Header.Set("Authorization", header)
	}
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec, seen
}

func TestAuthorized_ValidTokenAndRolePasses(t *testing.T) {
	// when
	rec, seen := serve(t, "Bearer business-token", model.RoleBusiness)

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.Equal(t, businessID, seen)
}

func TestAuthorized_AnyRoleWhenNoneListed(t *testing.T) {
	// when
	rec, seen := serve(t, "Bearer business-token")

	// then
	require.Equal(t, http.StatusOK, rec.Code)
	require.Equal(t, businessID, seen)
}

func TestAuthorized_WrongRoleIsForbidden(t *testing.T) {
	// when
	rec, seen := serve(t, "Bearer business-token", model.RoleCandidate)

	// then
	require.Equal(t, http.StatusForbidden, rec.Code)
	require.Equal(t, uuid.Nil, seen)
}

func TestAuthorized_MissingOrBadTokenIsUnauthorized(t *testing.T) {
	cases := map[string]string{
		"no header":    "",
		"not bearer":   "Basic business-token",
		"bad token":    "Bearer otro",
		"empty bearer": "Bearer ",
	}
	for name, header := range cases {
		t.Run(name, func(t *testing.T) {
			// when
			rec, seen := serve(t, header, model.RoleBusiness)

			// then
			require.Equal(t, http.StatusUnauthorized, rec.Code)
			require.JSONEq(t, `{"error":"unauthorized"}`, rec.Body.String())
			require.Equal(t, uuid.Nil, seen)
		})
	}
}
