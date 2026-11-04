// Package auth issues and checks the session tokens (JWT, HS256) the app sends as "Authorization: Bearer".
package auth

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
)

type Claims struct {
	UserID uuid.UUID
	Role   model.Role
}

type Tokens struct {
	secret []byte
	ttl    time.Duration
	now    func() time.Time
}

func NewTokens(secret []byte, ttl time.Duration, now func() time.Time) *Tokens {
	return &Tokens{secret: secret, ttl: ttl, now: now}
}

func (t *Tokens) Issue(user model.User) (string, error) {
	issued := t.now()
	claims := jwt.MapClaims{"sub": user.ID.String(), "role": string(user.Role), "iat": issued.Unix(), "exp": issued.Add(t.ttl).Unix()}
	return jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString(t.secret)
}

// Parse returns model.ErrUnauthorized for a token that is malformed, expired or not signed by us.
func (t *Tokens) Parse(token string) (Claims, error) {
	parsed, err := jwt.Parse(token, func(*jwt.Token) (any, error) { return t.secret, nil },
		jwt.WithValidMethods([]string{jwt.SigningMethodHS256.Alg()}), jwt.WithExpirationRequired(), jwt.WithTimeFunc(t.now))
	if err != nil {
		return Claims{}, fmt.Errorf("%w: %v", model.ErrUnauthorized, err)
	}
	claims, _ := parsed.Claims.(jwt.MapClaims)
	subject, _ := claims["sub"].(string)
	userID, err := uuid.Parse(subject)
	if err != nil {
		return Claims{}, fmt.Errorf("%w: invalid subject", model.ErrUnauthorized)
	}
	role := model.Role(fmt.Sprint(claims["role"]))
	if role != model.RoleCandidate && role != model.RoleBusiness {
		return Claims{}, fmt.Errorf("%w: unknown role", model.ErrUnauthorized)
	}
	return Claims{UserID: userID, Role: role}, nil
}
