package auth

import (
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"github.com/stretchr/testify/require"
)

var (
	secret = []byte("test-secret-at-least-32-bytes-long!!")
	now    = time.Date(2026, 11, 4, 12, 0, 0, 0, time.UTC)
)

func fixedNow() time.Time { return now }

func TestIssue_ParseRoundTrip(t *testing.T) {
	// begin
	tokens := NewTokens(secret, time.Hour, fixedNow)
	user := model.User{ID: uuid.New(), Role: model.RoleBusiness}

	// when
	token, err := tokens.Issue(user)
	require.NoError(t, err)
	got, err := tokens.Parse(token)

	// then
	require.NoError(t, err)
	require.Equal(t, Claims{UserID: user.ID, Role: model.RoleBusiness}, got)
}

func TestParse_Expired(t *testing.T) {
	// begin
	issuer := NewTokens(secret, time.Hour, fixedNow)
	token, err := issuer.Issue(model.User{ID: uuid.New(), Role: model.RoleCandidate})
	require.NoError(t, err)
	later := NewTokens(secret, time.Hour, func() time.Time { return now.Add(2 * time.Hour) })

	// when
	_, err = later.Parse(token)

	// then
	require.ErrorIs(t, err, model.ErrUnauthorized)
}

func TestParse_SignedWithAnotherSecret(t *testing.T) {
	// begin
	other := NewTokens([]byte("another-secret-also-32-bytes-long!!"), time.Hour, fixedNow)
	token, err := other.Issue(model.User{ID: uuid.New(), Role: model.RoleCandidate})
	require.NoError(t, err)

	// when
	_, err = NewTokens(secret, time.Hour, fixedNow).Parse(token)

	// then
	require.ErrorIs(t, err, model.ErrUnauthorized)
}

func TestParse_RejectsUnsignedTokens(t *testing.T) {
	// begin
	claims := jwt.MapClaims{"sub": uuid.NewString(), "role": "BUSINESS", "exp": now.Add(time.Hour).Unix()}
	unsigned, err := jwt.NewWithClaims(jwt.SigningMethodNone, claims).SignedString(jwt.UnsafeAllowNoneSignatureType)
	require.NoError(t, err)

	// when
	_, err = NewTokens(secret, time.Hour, fixedNow).Parse(unsigned)

	// then
	require.ErrorIs(t, err, model.ErrUnauthorized)
}

func TestParse_Garbage(t *testing.T) {
	cases := []string{"", "abc", "a.b.c"}
	for _, token := range cases {
		t.Run(token, func(t *testing.T) {
			// when
			_, err := NewTokens(secret, time.Hour, fixedNow).Parse(token)

			// then
			require.ErrorIs(t, err, model.ErrUnauthorized)
		})
	}
}

func TestParse_UnknownRole(t *testing.T) {
	// begin
	claims := jwt.MapClaims{"sub": uuid.NewString(), "role": "ADMIN", "exp": now.Add(time.Hour).Unix()}
	token, err := jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString(secret)
	require.NoError(t, err)

	// when
	_, err = NewTokens(secret, time.Hour, fixedNow).Parse(token)

	// then
	require.ErrorIs(t, err, model.ErrUnauthorized)
}
