package api

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/api/controller"
	"github.com/laburin/backend/internal/auth"
	"github.com/laburin/backend/internal/repository"
	"github.com/laburin/backend/internal/service"
	"github.com/laburin/backend/internal/testutil"
	"github.com/stretchr/testify/require"
	"golang.org/x/crypto/bcrypt"
)

// client talks to the full stack (router → controllers → services → Postgres + Redis) like the app does.
type client struct {
	t      *testing.T
	server *httptest.Server
	token  string
}

func newServer(t *testing.T) *httptest.Server {
	gin.SetMode(gin.TestMode)
	postgres := repository.NewPostgres(testutil.Postgres(t))
	geo := repository.NewRedisGeo(testutil.Redis(t))
	tokens := auth.NewTokens([]byte("e2e-secret-at-least-32-bytes-long!!"), time.Hour, time.Now)
	controllers := Controllers{
		Auth:         controller.NewAuthController(service.NewAuthService(postgres, tokens, bcrypt.MinCost)),
		Offers:       controller.NewOfferController(service.NewOfferService(postgres, geo)),
		Applications: controller.NewApplicationController(service.NewApplicationService(postgres)),
		Profile:      controller.NewProfileController(service.NewProfileService(postgres)),
	}
	server := httptest.NewServer(NewRouter(tokens, controllers))
	t.Cleanup(server.Close)
	return server
}

func (c *client) call(method, path string, body any) (int, []byte) {
	c.t.Helper()
	var reader io.Reader
	if body != nil {
		encoded, err := json.Marshal(body)
		require.NoError(c.t, err)
		reader = bytes.NewReader(encoded)
	}
	req, err := http.NewRequestWithContext(context.Background(), method, c.server.URL+path, reader)
	require.NoError(c.t, err)
	req.Header.Set("Content-Type", "application/json")
	return c.send(req)
}

func (c *client) send(req *http.Request) (int, []byte) {
	c.t.Helper()
	if c.token != "" {
		req.Header.Set("Authorization", "Bearer "+c.token)
	}
	res, err := http.DefaultClient.Do(req)
	require.NoError(c.t, err)
	defer res.Body.Close()
	data, err := io.ReadAll(res.Body)
	require.NoError(c.t, err)
	return res.StatusCode, data
}

func (c *client) uploadCV(content []byte) int {
	c.t.Helper()
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	part, err := writer.CreateFormFile("file", "cv.pdf")
	require.NoError(c.t, err)
	_, err = part.Write(content)
	require.NoError(c.t, err)
	require.NoError(c.t, writer.Close())
	req, err := http.NewRequestWithContext(context.Background(), http.MethodPut, c.server.URL+"/me/cv", &body)
	require.NoError(c.t, err)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	status, _ := c.send(req)
	return status
}

func decode[T any](t *testing.T, data []byte) T {
	var value T
	require.NoError(t, json.Unmarshal(data, &value), string(data))
	return value
}

func signUp(t *testing.T, server *httptest.Server, path string, body map[string]any) *client {
	anonymous := &client{t: t, server: server}
	status, data := anonymous.call(http.MethodPost, path, body)
	require.Equal(t, http.StatusCreated, status, string(data))
	return &client{t: t, server: server, token: decode[controller.SessionResp](t, data).Token}
}

// The whole app flow: publish → map → apply with a CV → review → Match → close.
func TestFullFlow(t *testing.T) {
	server := newServer(t)
	guest := &client{t: t, server: server}

	business := signUp(t, server, "/auth/signup/business", map[string]any{
		"businessName": "[COMERCIO]", "category": "GASTRONOMY", "schedule": "9 a 17", "address": "[DIRECCION]",
		"latitude": -34.6037, "longitude": -58.3816, "email": "[comercio]@e2e.laburin", "password": "demo1234",
	})
	candidate := signUp(t, server, "/auth/signup/candidate", map[string]any{
		"firstName": "[NOMBRE]", "lastName": "[APELLIDO]", "email": "[postulante]@e2e.laburin", "password": "demo1234",
	})

	// Login works with the same credentials, in any case.
	status, _ := guest.call(http.MethodPost, "/auth/login", map[string]any{"email": "[POSTULANTE]@E2E.laburin", "password": "demo1234"})
	require.Equal(t, http.StatusOK, status)
	status, _ = guest.call(http.MethodPost, "/auth/login", map[string]any{"email": "[postulante]@e2e.laburin", "password": "otra-clave"})
	require.Equal(t, http.StatusUnauthorized, status)

	// Publish (the id comes from the app); a retry of the same PUT doesn't duplicate it.
	offerID := uuid.NewString()
	offer := map[string]any{"title": "Mozo", "category": "GASTRONOMY", "schedule": "Sábados", "description": "Salón",
		"salary": map[string]any{"min": 600000, "max": 750000, "period": "MONTH"}}
	status, _ = business.call(http.MethodPut, "/offers/"+offerID, offer)
	require.Equal(t, http.StatusCreated, status)
	status, _ = business.call(http.MethodPut, "/offers/"+offerID, offer)
	require.Equal(t, http.StatusOK, status)

	// A candidate can't publish.
	status, _ = candidate.call(http.MethodPut, "/offers/"+uuid.NewString(), offer)
	require.Equal(t, http.StatusForbidden, status)

	// The map finds it without an account, within the radius and the category.
	status, data := guest.call(http.MethodGet, "/offers?lat=-34.6080&lng=-58.3712&radiusKm=2&category=GASTRONOMY", nil)
	require.Equal(t, http.StatusOK, status)
	found := decode[[]controller.OfferResp](t, data)
	require.Len(t, found, 1)
	require.Equal(t, offerID, found[0].ID)
	require.Equal(t, "[COMERCIO]", found[0].BusinessName)
	require.Equal(t, &controller.SalaryBody{Min: 600000, Max: ptr(int64(750000)), Period: "MONTH"}, found[0].Salary)
	status, data = guest.call(http.MethodGet, "/offers?lat=-34.6080&lng=-58.3712&radiusKm=2&category=STOCK", nil)
	require.Equal(t, http.StatusOK, status)
	require.JSONEq(t, `[]`, string(data))
	status, data = guest.call(http.MethodGet, "/offers?lat=-34.70&lng=-58.50&radiusKm=2", nil)
	require.Equal(t, http.StatusOK, status)
	require.JSONEq(t, `[]`, string(data))

	// Applying needs a CV first.
	applicationID := uuid.NewString()
	status, _ = candidate.call(http.MethodPut, "/applications/"+applicationID, map[string]any{"offerId": offerID})
	require.Equal(t, http.StatusUnprocessableEntity, status)
	status, _ = candidate.call(http.MethodGet, "/me/cv", nil)
	require.Equal(t, http.StatusNotFound, status)
	require.Equal(t, http.StatusBadRequest, candidate.uploadCV([]byte("no es un pdf")))
	require.Equal(t, http.StatusOK, candidate.uploadCV([]byte("%PDF-1.4 cv de prueba")))
	status, data = candidate.call(http.MethodGet, "/me/cv", nil)
	require.Equal(t, http.StatusOK, status)
	require.JSONEq(t, `{"fileName":"cv.pdf"}`, string(data))

	// Apply, twice (retry): one application.
	for range 2 {
		status, _ = candidate.call(http.MethodPut, "/applications/"+applicationID, map[string]any{"offerId": offerID})
		require.Equal(t, http.StatusOK, status)
	}

	// The business sees the applicant without the contact.
	status, data = business.call(http.MethodGet, "/offers/"+offerID+"/applicants", nil)
	require.Equal(t, http.StatusOK, status)
	applicants := decode[[]controller.ApplicantResp](t, data)
	require.Len(t, applicants, 1)
	require.Equal(t, "PENDING_REVIEW", applicants[0].Status)
	require.Nil(t, applicants[0].ContactEmail)
	require.Equal(t, "[NOMBRE] [APELLIDO]", applicants[0].CandidateName)

	// And can download the CV; the candidate can't use that route.
	status, data = business.call(http.MethodGet, "/applications/"+applicationID+"/cv", nil)
	require.Equal(t, http.StatusOK, status)
	require.Equal(t, "%PDF-1.4 cv de prueba", string(data))
	status, _ = candidate.call(http.MethodGet, "/applications/"+applicationID+"/cv", nil)
	require.Equal(t, http.StatusForbidden, status)

	// Another business can't see or review them.
	other := signUp(t, server, "/auth/signup/business", map[string]any{
		"businessName": "[OTRO]", "category": "STOCK", "schedule": "s", "address": "[DIRECCION]",
		"latitude": -34.6, "longitude": -58.4, "email": "[otro]@e2e.laburin", "password": "demo1234",
	})
	status, _ = other.call(http.MethodGet, "/offers/"+offerID+"/applicants", nil)
	require.Equal(t, http.StatusForbidden, status)
	status, _ = other.call(http.MethodPut, "/applications/"+applicationID+"/review", map[string]any{"approve": true})
	require.Equal(t, http.StatusForbidden, status)
	status, _ = other.call(http.MethodPost, "/offers/"+offerID+"/close", nil)
	require.Equal(t, http.StatusForbidden, status)

	// Approve: the Match brings the contact.
	status, data = business.call(http.MethodPut, "/applications/"+applicationID+"/review", map[string]any{"approve": true})
	require.Equal(t, http.StatusOK, status)
	approved := decode[controller.ApplicantResp](t, data)
	require.Equal(t, "APPROVED", approved.Status)
	require.Equal(t, ptr("[postulante]@e2e.laburin"), approved.ContactEmail)

	// The candidate sees it approved.
	status, data = candidate.call(http.MethodGet, "/applications/mine", nil)
	require.Equal(t, http.StatusOK, status)
	mine := decode[[]controller.ApplicationResp](t, data)
	require.Len(t, mine, 1)
	require.Equal(t, controller.ApplicationResp{ID: applicationID, OfferID: offerID, OfferTitle: "Mozo", BusinessName: "[COMERCIO]", Status: "APPROVED", CreatedAt: mine[0].CreatedAt}, mine[0])

	// Close (idempotent): it leaves the map, can't be edited, refuses new applications, and the detail says CLOSED.
	for range 2 {
		status, _ = business.call(http.MethodPost, "/offers/"+offerID+"/close", nil)
		require.Equal(t, http.StatusNoContent, status)
	}
	status, data = guest.call(http.MethodGet, "/offers?lat=-34.6037&lng=-58.3816&radiusKm=5", nil)
	require.Equal(t, http.StatusOK, status)
	require.JSONEq(t, `[]`, string(data))
	status, _ = business.call(http.MethodPut, "/offers/"+offerID, offer)
	require.Equal(t, http.StatusConflict, status)
	status, _ = candidate.call(http.MethodPut, "/applications/"+uuid.NewString(), map[string]any{"offerId": offerID})
	require.Equal(t, http.StatusConflict, status)
	status, data = guest.call(http.MethodGet, "/offers/"+offerID, nil)
	require.Equal(t, http.StatusOK, status)
	require.Equal(t, "CLOSED", decode[controller.OfferResp](t, data).Status)
	status, data = business.call(http.MethodGet, "/offers/mine", nil)
	require.Equal(t, http.StatusOK, status)
	require.Len(t, decode[[]controller.OfferResp](t, data), 1)

	// Push token for any account; private routes need a token.
	status, _ = candidate.call(http.MethodPost, "/me/push-token", map[string]any{"token": "fcm-e2e"})
	require.Equal(t, http.StatusNoContent, status)
	status, _ = guest.call(http.MethodGet, "/applications/mine", nil)
	require.Equal(t, http.StatusUnauthorized, status)
	status, _ = business.call(http.MethodGet, "/applications/mine", nil)
	require.Equal(t, http.StatusForbidden, status)
}

func TestHealth(t *testing.T) {
	server := newServer(t)

	status, data := (&client{t: t, server: server}).call(http.MethodGet, "/healthz", nil)

	require.Equal(t, http.StatusOK, status)
	require.JSONEq(t, `{"status":"ok"}`, string(data))
}

func ptr[T any](v T) *T { return &v }
