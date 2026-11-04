package service

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"golang.org/x/crypto/bcrypt"
)

type UserRepository interface {
	// GetUserByEmail returns model.ErrNotFound when no account uses that email (case-insensitive).
	GetUserByEmail(ctx context.Context, email string) (model.User, error)
	// CreateCandidate returns model.ErrConflict when the email is taken.
	CreateCandidate(ctx context.Context, user model.User) error
	// CreateBusiness stores the account and its business profile atomically; model.ErrConflict when the email is taken.
	CreateBusiness(ctx context.Context, user model.User, business model.Business) error
}

type TokenIssuer interface {
	Issue(user model.User) (string, error)
}

// Session is what login and sign-up return: the user is logged in right away.
type Session struct {
	Token string
	User  model.User
}

type SignUpCandidateParams struct {
	FirstName string
	LastName  string
	Email     string
	Password  string
}

type SignUpBusinessParams struct {
	BusinessName string
	Category     model.Category
	Schedule     string
	Address      string
	Latitude     float64
	Longitude    float64
	Email        string
	Password     string
}

const MinPasswordLength = 8

type AuthService struct {
	users    UserRepository
	tokens   TokenIssuer
	hashCost int
}

func NewAuthService(users UserRepository, tokens TokenIssuer, hashCost int) *AuthService {
	return &AuthService{users: users, tokens: tokens, hashCost: hashCost}
}

// Login returns model.ErrUnauthorized for an unknown email or a wrong password, without telling which.
func (s *AuthService) Login(ctx context.Context, email, password string) (Session, error) {
	user, err := s.users.GetUserByEmail(ctx, normalizeEmail(email))
	if errors.Is(err, model.ErrNotFound) {
		return Session{}, model.ErrUnauthorized
	}
	if err != nil {
		return Session{}, err
	}
	if bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)) != nil {
		return Session{}, model.ErrUnauthorized
	}
	return s.session(user)
}

func (s *AuthService) SignUpCandidate(ctx context.Context, params SignUpCandidateParams) (Session, error) {
	firstName, lastName := strings.TrimSpace(params.FirstName), strings.TrimSpace(params.LastName)
	if firstName == "" || lastName == "" {
		return Session{}, fmt.Errorf("%w: first and last name are required", model.ErrInvalid)
	}
	user, err := s.newUser(model.RoleCandidate, params.Email, params.Password, firstName+" "+lastName)
	if err != nil {
		return Session{}, err
	}
	if err := s.users.CreateCandidate(ctx, user); err != nil {
		return Session{}, err
	}
	return s.session(user)
}

func (s *AuthService) SignUpBusiness(ctx context.Context, params SignUpBusinessParams) (Session, error) {
	name, schedule, address := strings.TrimSpace(params.BusinessName), strings.TrimSpace(params.Schedule), strings.TrimSpace(params.Address)
	if name == "" || schedule == "" || address == "" {
		return Session{}, fmt.Errorf("%w: name, schedule and address are required", model.ErrInvalid)
	}
	if !params.Category.Valid() {
		return Session{}, fmt.Errorf("%w: unknown category %q", model.ErrInvalid, params.Category)
	}
	if !validCoordinates(params.Latitude, params.Longitude) {
		return Session{}, fmt.Errorf("%w: invalid coordinates", model.ErrInvalid)
	}
	user, err := s.newUser(model.RoleBusiness, params.Email, params.Password, name)
	if err != nil {
		return Session{}, err
	}
	business := model.Business{UserID: user.ID, Name: name, Category: params.Category, Schedule: schedule, Address: address, Latitude: params.Latitude, Longitude: params.Longitude}
	if err := s.users.CreateBusiness(ctx, user, business); err != nil {
		return Session{}, err
	}
	return s.session(user)
}

// newUser validates the credentials and hashes the password.
func (s *AuthService) newUser(role model.Role, email, password, displayName string) (model.User, error) {
	email = normalizeEmail(email)
	// Deliberately loose: one "@" with something on both sides and no spaces.
	local, domain, found := strings.Cut(email, "@")
	if !found || local == "" || domain == "" || strings.ContainsAny(email, " \t") || strings.Contains(domain, "@") {
		return model.User{}, fmt.Errorf("%w: invalid email", model.ErrInvalid)
	}
	if len([]rune(password)) < MinPasswordLength {
		return model.User{}, fmt.Errorf("%w: password shorter than %d characters", model.ErrInvalid, MinPasswordLength)
	}
	hash, err := bcrypt.GenerateFromPassword([]byte(password), s.hashCost)
	if err != nil {
		return model.User{}, err
	}
	return model.User{ID: uuid.New(), Role: role, Email: email, PasswordHash: string(hash), DisplayName: displayName}, nil
}

func (s *AuthService) session(user model.User) (Session, error) {
	token, err := s.tokens.Issue(user)
	if err != nil {
		return Session{}, err
	}
	return Session{Token: token, User: user}, nil
}

func normalizeEmail(email string) string {
	return strings.ToLower(strings.TrimSpace(email))
}

func validCoordinates(latitude, longitude float64) bool {
	return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
}
