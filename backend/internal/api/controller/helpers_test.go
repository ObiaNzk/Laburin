package controller

import (
	"bytes"
	"io"
	"net/http"
	"net/http/httptest"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/middleware"
	"github.com/laburin/backend/internal/auth"
	"github.com/laburin/backend/internal/model"
)

func newRouter() *gin.Engine {
	gin.SetMode(gin.TestMode)
	return gin.New()
}

// asUser stands in for middleware.Authorized: the controllers only read the authenticated user.
func asUser(id uuid.UUID, role model.Role) gin.HandlerFunc {
	return func(c *gin.Context) {
		middleware.SetClaims(c, auth.Claims{UserID: id, Role: role})
		c.Next()
	}
}

func do(router http.Handler, method, path, body string) *httptest.ResponseRecorder {
	var reader io.Reader
	if body != "" {
		reader = bytes.NewBufferString(body)
	}
	req := httptest.NewRequest(method, path, reader)
	if body != "" {
		req.Header.Set("Content-Type", "application/json")
	}
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec
}

func serveRequest(router http.Handler, req *http.Request) *httptest.ResponseRecorder {
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec
}
