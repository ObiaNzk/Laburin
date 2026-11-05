package service

//go:generate mockgen -source=offer.go -destination=mocks/offer.go -package=mocks

import (
	"context"
	"errors"
	"testing"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/service/mocks"
	"github.com/stretchr/testify/require"
	"go.uber.org/mock/gomock"
)

type offerTestSetup struct {
	ctrl    *gomock.Controller
	offers  *mocks.MockOfferRepository
	geo     *mocks.MockGeoIndex
	service *OfferService
}

func setupOfferTest(t *testing.T) offerTestSetup {
	ctrl := gomock.NewController(t)
	offers := mocks.NewMockOfferRepository(ctrl)
	geo := mocks.NewMockGeoIndex(ctrl)
	return offerTestSetup{ctrl: ctrl, offers: offers, geo: geo, service: NewOfferService(offers, geo)}
}

var content = model.OfferContent{Title: "Mozo", Category: model.CategoryGastronomy, Schedule: "Sábados", Description: "Atención en salón"}

func TestNearby_ReturnsActiveOffersNearestFirst(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	near, far, closed := uuid.New(), uuid.New(), uuid.New()
	category := model.CategoryGastronomy
	setup.geo.EXPECT().Search(gomock.Any(), -34.6, -58.38, 5).Return([]uuid.UUID{near, closed, far}, nil)
	nearOffer := model.Offer{ID: near, Status: model.OfferStatusActive}
	farOffer := model.Offer{ID: far, Status: model.OfferStatusActive}
	setup.offers.EXPECT().ListActiveOffersByIDs(gomock.Any(), []uuid.UUID{near, closed, far}, &category).Return([]model.Offer{farOffer, nearOffer}, nil)

	// when
	got, err := setup.service.Nearby(context.Background(), NearbyParams{Latitude: -34.6, Longitude: -58.38, RadiusKm: 5, Category: &category})

	// then
	require.NoError(t, err)
	require.Equal(t, []model.Offer{nearOffer, farOffer}, got)
}

func TestNearby_NothingInRangeSkipsTheDatabase(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	setup.geo.EXPECT().Search(gomock.Any(), -34.6, -58.38, 2).Return(nil, nil)

	// when
	got, err := setup.service.Nearby(context.Background(), NearbyParams{Latitude: -34.6, Longitude: -58.38, RadiusKm: 2})

	// then
	require.NoError(t, err)
	require.Equal(t, []model.Offer{}, got)
}

func TestNearby_Invalid(t *testing.T) {
	unknown := model.Category("PLUMBING")
	cases := map[string]NearbyParams{
		"zero radius":      {Latitude: -34.6, Longitude: -58.38, RadiusKm: 0},
		"radius too large": {Latitude: -34.6, Longitude: -58.38, RadiusKm: MaxRadiusKm + 1},
		"latitude":         {Latitude: -91, Longitude: -58.38, RadiusKm: 5},
		"longitude":        {Latitude: -34.6, Longitude: 181, RadiusKm: 5},
		"unknown category": {Latitude: -34.6, Longitude: -58.38, RadiusKm: 5, Category: &unknown},
	}
	for name, params := range cases {
		t.Run(name, func(t *testing.T) {
			// begin
			setup := setupOfferTest(t)

			// when
			_, err := setup.service.Nearby(context.Background(), params)

			// then
			require.ErrorIs(t, err, model.ErrInvalid)
		})
	}
}

func TestGet_ReturnsAnyStatus(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	offer := model.Offer{ID: uuid.New(), Status: model.OfferStatusClosed}
	setup.offers.EXPECT().GetOffer(gomock.Any(), offer.ID).Return(offer, nil)

	// when
	got, err := setup.service.Get(context.Background(), offer.ID)

	// then
	require.NoError(t, err)
	require.Equal(t, offer, got)
}

func TestPut_CreatesANewOfferAtTheBusinessLocation(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	business := model.Business{UserID: businessID, Latitude: -34.6, Longitude: -58.38}
	setup.offers.EXPECT().GetBusiness(gomock.Any(), businessID).Return(business, nil)
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{}, model.ErrNotFound)
	setup.offers.EXPECT().CreateOffer(gomock.Any(), offerID, businessID, content).Return(nil)
	setup.geo.EXPECT().Add(gomock.Any(), model.OfferLocation{ID: offerID, Latitude: -34.6, Longitude: -58.38}).Return(nil)

	// when
	created, err := setup.service.Put(context.Background(), businessID, offerID, content)

	// then
	require.NoError(t, err)
	require.True(t, created)
}

func TestPut_ReplacesTheContentOfItsOwnOpenOffer(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	business := model.Business{UserID: businessID, Latitude: -34.6, Longitude: -58.38}
	setup.offers.EXPECT().GetBusiness(gomock.Any(), businessID).Return(business, nil)
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: businessID, Status: model.OfferStatusActive}, nil)
	setup.offers.EXPECT().UpdateOfferContent(gomock.Any(), offerID, content).Return(nil)
	// Re-adding heals a geo index that missed the first publish (e.g. Redis was down and the app retried).
	setup.geo.EXPECT().Add(gomock.Any(), model.OfferLocation{ID: offerID, Latitude: -34.6, Longitude: -58.38}).Return(nil)

	// when
	created, err := setup.service.Put(context.Background(), businessID, offerID, content)

	// then
	require.NoError(t, err)
	require.False(t, created)
}

func TestPut_AnotherBusinessOfferIsForbidden(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	setup.offers.EXPECT().GetBusiness(gomock.Any(), businessID).Return(model.Business{UserID: businessID}, nil)
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: uuid.New(), Status: model.OfferStatusActive}, nil)

	// when
	_, err := setup.service.Put(context.Background(), businessID, offerID, content)

	// then
	require.ErrorIs(t, err, model.ErrForbidden)
}

func TestPut_AClosedOfferCantChange(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	setup.offers.EXPECT().GetBusiness(gomock.Any(), businessID).Return(model.Business{UserID: businessID}, nil)
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: businessID, Status: model.OfferStatusClosed}, nil)

	// when
	_, err := setup.service.Put(context.Background(), businessID, offerID, content)

	// then
	require.ErrorIs(t, err, model.ErrConflict)
}

func TestPut_WithoutBusinessProfileIsForbidden(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	userID := uuid.New()
	setup.offers.EXPECT().GetBusiness(gomock.Any(), userID).Return(model.Business{}, model.ErrNotFound)

	// when
	_, err := setup.service.Put(context.Background(), userID, uuid.New(), content)

	// then
	require.ErrorIs(t, err, model.ErrForbidden)
}

func TestPut_GeoIndexErrorIsReturnedSoTheAppRetries(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	boom := errors.New("redis down")
	setup.offers.EXPECT().GetBusiness(gomock.Any(), businessID).Return(model.Business{UserID: businessID}, nil)
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{}, model.ErrNotFound)
	setup.offers.EXPECT().CreateOffer(gomock.Any(), offerID, businessID, content).Return(nil)
	setup.geo.EXPECT().Add(gomock.Any(), gomock.Any()).Return(boom)

	// when
	_, err := setup.service.Put(context.Background(), businessID, offerID, content)

	// then
	require.ErrorIs(t, err, boom)
}

func TestClose_ClosesItsActiveOffer(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: businessID, Status: model.OfferStatusActive}, nil)
	setup.offers.EXPECT().CloseOffer(gomock.Any(), offerID).Return(nil)
	setup.geo.EXPECT().Remove(gomock.Any(), offerID).Return(nil)

	// when
	err := setup.service.Close(context.Background(), businessID, offerID)

	// then
	require.NoError(t, err)
}

func TestClose_AlreadyClosedIsANoOpButStillLeavesTheIndex(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID, offerID := uuid.New(), uuid.New()
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: businessID, Status: model.OfferStatusClosed}, nil)
	setup.geo.EXPECT().Remove(gomock.Any(), offerID).Return(nil)

	// when
	err := setup.service.Close(context.Background(), businessID, offerID)

	// then
	require.NoError(t, err)
}

func TestClose_UnknownOffer(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	offerID := uuid.New()
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{}, model.ErrNotFound)

	// when
	err := setup.service.Close(context.Background(), uuid.New(), offerID)

	// then
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestClose_AnotherBusinessOfferIsForbidden(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	offerID := uuid.New()
	setup.offers.EXPECT().GetOffer(gomock.Any(), offerID).Return(model.Offer{ID: offerID, BusinessID: uuid.New(), Status: model.OfferStatusActive}, nil)

	// when
	err := setup.service.Close(context.Background(), uuid.New(), offerID)

	// then
	require.ErrorIs(t, err, model.ErrForbidden)
}

func TestMine_ReturnsTheBusinessOffers(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	businessID := uuid.New()
	offers := []model.Offer{{ID: uuid.New(), BusinessID: businessID}}
	setup.offers.EXPECT().ListBusinessOffers(gomock.Any(), businessID).Return(offers, nil)

	// when
	got, err := setup.service.Mine(context.Background(), businessID)

	// then
	require.NoError(t, err)
	require.Equal(t, offers, got)
}

func TestSyncGeoIndex_ReplacesTheIndexWithPostgres(t *testing.T) {
	// begin
	setup := setupOfferTest(t)
	locations := []model.OfferLocation{{ID: uuid.New(), Latitude: -34.6, Longitude: -58.38}}
	setup.offers.EXPECT().ListActiveOfferLocations(gomock.Any()).Return(locations, nil)
	setup.geo.EXPECT().Replace(gomock.Any(), locations).Return(nil)

	// when
	err := setup.service.SyncGeoIndex(context.Background())

	// then
	require.NoError(t, err)
}
