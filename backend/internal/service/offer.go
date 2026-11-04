package service

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
)

type OfferRepository interface {
	// GetOffer returns the offer in any status, or model.ErrNotFound.
	GetOffer(ctx context.Context, id uuid.UUID) (model.Offer, error)
	// ListActiveOffersByIDs skips closed or unknown ids; a nil category means every category. Order is not kept.
	ListActiveOffersByIDs(ctx context.Context, ids []uuid.UUID, category *model.Category) ([]model.Offer, error)
	ListBusinessOffers(ctx context.Context, businessID uuid.UUID) ([]model.Offer, error)
	ListActiveOfferLocations(ctx context.Context) ([]model.OfferLocation, error)
	// GetBusiness returns model.ErrNotFound when the user has no business profile.
	GetBusiness(ctx context.Context, userID uuid.UUID) (model.Business, error)
	CreateOffer(ctx context.Context, id, businessID uuid.UUID, content model.OfferContent) error
	UpdateOfferContent(ctx context.Context, id uuid.UUID, content model.OfferContent) error
	CloseOffer(ctx context.Context, id uuid.UUID) error
}

// GeoIndex is the derived index of active offers' locations (Redis). Postgres stays the source of truth.
type GeoIndex interface {
	Add(ctx context.Context, location model.OfferLocation) error
	Remove(ctx context.Context, offerID uuid.UUID) error
	// Search returns the ids within radiusKm of the point, nearest first.
	Search(ctx context.Context, latitude, longitude float64, radiusKm int) ([]uuid.UUID, error)
	// Replace rebuilds the whole index from these locations.
	Replace(ctx context.Context, locations []model.OfferLocation) error
}

type NearbyParams struct {
	Latitude  float64
	Longitude float64
	RadiusKm  int
	Category  *model.Category
}

const MaxRadiusKm = 50

type OfferService struct {
	offers OfferRepository
	geo    GeoIndex
}

func NewOfferService(offers OfferRepository, geo GeoIndex) *OfferService {
	return &OfferService{offers: offers, geo: geo}
}

// Nearby returns the active offers within the radius, nearest first.
func (s *OfferService) Nearby(ctx context.Context, params NearbyParams) ([]model.Offer, error) {
	if params.RadiusKm < 1 || params.RadiusKm > MaxRadiusKm {
		return nil, fmt.Errorf("%w: radiusKm must be between 1 and %d", model.ErrInvalid, MaxRadiusKm)
	}
	if !validCoordinates(params.Latitude, params.Longitude) {
		return nil, fmt.Errorf("%w: invalid coordinates", model.ErrInvalid)
	}
	if params.Category != nil && !params.Category.Valid() {
		return nil, fmt.Errorf("%w: unknown category %q", model.ErrInvalid, *params.Category)
	}
	ids, err := s.geo.Search(ctx, params.Latitude, params.Longitude, params.RadiusKm)
	if err != nil {
		return nil, err
	}
	if len(ids) == 0 {
		return []model.Offer{}, nil
	}
	offers, err := s.offers.ListActiveOffersByIDs(ctx, ids, params.Category)
	if err != nil {
		return nil, err
	}
	// Postgres doesn't keep the order of the ids; the geo index gave them nearest first.
	byID := make(map[uuid.UUID]model.Offer, len(offers))
	for _, offer := range offers {
		byID[offer.ID] = offer
	}
	nearestFirst := make([]model.Offer, 0, len(offers))
	for _, id := range ids {
		if offer, ok := byID[id]; ok {
			nearestFirst = append(nearestFirst, offer)
		}
	}
	return nearestFirst, nil
}

// Get returns the offer in any status, or model.ErrNotFound.
func (s *OfferService) Get(ctx context.Context, id uuid.UUID) (model.Offer, error) {
	return s.offers.GetOffer(ctx, id)
}

// Put publishes the offer when the id is new, or replaces its content (edit). Returns true when it was created.
// model.ErrForbidden: not a business or not the owner. model.ErrConflict: the offer is closed.
func (s *OfferService) Put(ctx context.Context, businessID, offerID uuid.UUID, content model.OfferContent) (bool, error) {
	business, err := s.offers.GetBusiness(ctx, businessID)
	if errors.Is(err, model.ErrNotFound) {
		return false, fmt.Errorf("%w: not a business", model.ErrForbidden)
	}
	if err != nil {
		return false, err
	}
	existing, err := s.offers.GetOffer(ctx, offerID)
	created := errors.Is(err, model.ErrNotFound)
	switch {
	case created:
		err = s.offers.CreateOffer(ctx, offerID, businessID, content)
	case err != nil:
		return false, err
	case existing.BusinessID != businessID:
		return false, fmt.Errorf("%w: the offer belongs to another business", model.ErrForbidden)
	case existing.Status != model.OfferStatusActive:
		return false, fmt.Errorf("%w: the offer is closed", model.ErrConflict)
	default:
		err = s.offers.UpdateOfferContent(ctx, offerID, content)
	}
	if err != nil {
		return false, err
	}
	// Postgres first: if Redis fails the app retries the same idempotent PUT, which adds it again.
	location := model.OfferLocation{ID: offerID, Latitude: business.Latitude, Longitude: business.Longitude}
	if err := s.geo.Add(ctx, location); err != nil {
		return false, err
	}
	return created, nil
}

// Close is idempotent. model.ErrNotFound: unknown offer. model.ErrForbidden: not the owner.
func (s *OfferService) Close(ctx context.Context, businessID, offerID uuid.UUID) error {
	offer, err := s.offers.GetOffer(ctx, offerID)
	if err != nil {
		return err
	}
	if offer.BusinessID != businessID {
		return fmt.Errorf("%w: the offer belongs to another business", model.ErrForbidden)
	}
	if offer.Status == model.OfferStatusActive {
		if err := s.offers.CloseOffer(ctx, offerID); err != nil {
			return err
		}
	}
	// Also on a repeated close, in case Redis failed the first time.
	return s.geo.Remove(ctx, offerID)
}

// Mine returns the business's offers in any status, most recently updated first.
func (s *OfferService) Mine(ctx context.Context, businessID uuid.UUID) ([]model.Offer, error) {
	return s.offers.ListBusinessOffers(ctx, businessID)
}

// SyncGeoIndex rebuilds the Redis index from Postgres, healing any drift between the two.
func (s *OfferService) SyncGeoIndex(ctx context.Context) error {
	locations, err := s.offers.ListActiveOfferLocations(ctx)
	if err != nil {
		return err
	}
	return s.geo.Replace(ctx, locations)
}
