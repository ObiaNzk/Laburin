// Package model holds LaburIN's domain types and the rules that keep them valid.
package model

import (
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
)

// Errors the services return; the controllers translate them to HTTP status codes.
var (
	ErrInvalid      = errors.New("invalid input")
	ErrUnauthorized = errors.New("unauthorized")
	ErrForbidden    = errors.New("forbidden")
	ErrNotFound     = errors.New("not found")
	ErrConflict     = errors.New("conflict")
	ErrCVRequired   = errors.New("cv required")
)

type Role string

const (
	RoleCandidate Role = "CANDIDATE"
	RoleBusiness  Role = "BUSINESS"
)

type Category string

const (
	CategoryGastronomy Category = "GASTRONOMY"
	CategoryMechanics  Category = "MECHANICS"
	CategoryStock      Category = "STOCK"
	CategoryClothing   Category = "CLOTHING"
)

func (c Category) Valid() bool {
	switch c {
	case CategoryGastronomy, CategoryMechanics, CategoryStock, CategoryClothing:
		return true
	}
	return false
}

type SalaryPeriod string

const (
	SalaryPeriodHour  SalaryPeriod = "HOUR"
	SalaryPeriodDay   SalaryPeriod = "DAY"
	SalaryPeriodMonth SalaryPeriod = "MONTH"
)

func (p SalaryPeriod) Valid() bool {
	switch p {
	case SalaryPeriodHour, SalaryPeriodDay, SalaryPeriodMonth:
		return true
	}
	return false
}

// Salary in whole pesos. Max is null for a single amount.
type Salary struct {
	Min    int64
	Max    null.Int64
	Period SalaryPeriod
}

type OfferStatus string

const (
	OfferStatusActive OfferStatus = "ACTIVE"
	OfferStatusClosed OfferStatus = "CLOSED"
)

// OfferContent is what a business writes; the location comes from the business profile.
type OfferContent struct {
	Title       string
	Category    Category
	Schedule    string
	Description string
	Salary      *Salary
}

type Offer struct {
	ID           uuid.UUID
	BusinessID   uuid.UUID
	Content      OfferContent
	JobType      string
	BusinessName string
	Address      string
	Latitude     float64
	Longitude    float64
	Status       OfferStatus
	UpdatedAt    time.Time
}

type OfferLocation struct {
	ID        uuid.UUID
	Latitude  float64
	Longitude float64
}

type User struct {
	ID           uuid.UUID
	Role         Role
	Email        string
	PasswordHash string
	DisplayName  string
}

type Business struct {
	UserID    uuid.UUID
	Name      string
	Category  Category
	Schedule  string
	Address   string
	Latitude  float64
	Longitude float64
}

type ApplicationStatus string

const (
	ApplicationStatusSent     ApplicationStatus = "SENT"
	ApplicationStatusApproved ApplicationStatus = "APPROVED"
	ApplicationStatusRejected ApplicationStatus = "REJECTED"
	// ApplicationStatusOfferClosed is derived: a sent application whose offer was closed.
	ApplicationStatusOfferClosed ApplicationStatus = "OFFER_CLOSED"
)

type Application struct {
	ID          uuid.UUID
	OfferID     uuid.UUID
	CandidateID uuid.UUID
	Status      ApplicationStatus
	CreatedAt   time.Time
}

// CandidateApplication is an application as its candidate sees it.
type CandidateApplication struct {
	ID           uuid.UUID
	OfferID      uuid.UUID
	OfferTitle   string
	OfferStatus  OfferStatus
	BusinessName string
	Status       ApplicationStatus
	CreatedAt    time.Time
}

// Applicant is an application as the offer's business sees it. CandidateEmail is the contact: it must leave the
// server only once the business approved the applicant (RNF-05).
type Applicant struct {
	ApplicationID  uuid.UUID
	OfferID        uuid.UUID
	CandidateID    uuid.UUID
	BusinessID     uuid.UUID
	CandidateName  string
	CandidateEmail string
	CVFileName     null.String
	Status         ApplicationStatus
}

type CV struct {
	FileName string
	Content  []byte
}
