package repository

import (
	"context"
	"testing"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/testutil"
	"github.com/stretchr/testify/require"
)

// Around Obelisco: ~0.5 km, ~1.3 km and ~5 km away.
var (
	center   = model.OfferLocation{Latitude: -34.6037, Longitude: -58.3816}
	nearLoc  = model.OfferLocation{ID: uuid.New(), Latitude: -34.6080, Longitude: -58.3790}
	midLoc   = model.OfferLocation{ID: uuid.New(), Latitude: -34.6080, Longitude: -58.3680}
	farLoc   = model.OfferLocation{ID: uuid.New(), Latitude: -34.6400, Longitude: -58.4100}
	geoCtx   = context.Background()
	geoTestR = 2
)

func TestGeo_SearchReturnsNearestFirstWithinTheRadius(t *testing.T) {
	// begin
	geo := NewRedisGeo(testutil.Redis(t))
	for _, l := range []model.OfferLocation{farLoc, midLoc, nearLoc} {
		require.NoError(t, geo.Add(geoCtx, l))
	}

	// when
	got, err := geo.Search(geoCtx, center.Latitude, center.Longitude, geoTestR)

	// then
	require.NoError(t, err)
	require.Equal(t, []uuid.UUID{nearLoc.ID, midLoc.ID}, got)
}

func TestGeo_RemoveTakesItOut(t *testing.T) {
	// begin
	geo := NewRedisGeo(testutil.Redis(t))
	require.NoError(t, geo.Add(geoCtx, nearLoc))
	require.NoError(t, geo.Add(geoCtx, midLoc))

	// when
	require.NoError(t, geo.Remove(geoCtx, nearLoc.ID))
	require.NoError(t, geo.Remove(geoCtx, nearLoc.ID)) // idempotent

	// then
	got, err := geo.Search(geoCtx, center.Latitude, center.Longitude, geoTestR)
	require.NoError(t, err)
	require.Equal(t, []uuid.UUID{midLoc.ID}, got)
}

func TestGeo_ReplaceDropsWhatIsNotListed(t *testing.T) {
	// begin
	geo := NewRedisGeo(testutil.Redis(t))
	require.NoError(t, geo.Add(geoCtx, nearLoc))

	// when
	err := geo.Replace(geoCtx, []model.OfferLocation{midLoc})

	// then
	require.NoError(t, err)
	got, err := geo.Search(geoCtx, center.Latitude, center.Longitude, geoTestR)
	require.NoError(t, err)
	require.Equal(t, []uuid.UUID{midLoc.ID}, got)
}

func TestGeo_ReplaceWithNothingEmptiesTheIndex(t *testing.T) {
	// begin
	geo := NewRedisGeo(testutil.Redis(t))
	require.NoError(t, geo.Add(geoCtx, nearLoc))

	// when
	err := geo.Replace(geoCtx, nil)

	// then
	require.NoError(t, err)
	got, err := geo.Search(geoCtx, center.Latitude, center.Longitude, geoTestR)
	require.NoError(t, err)
	require.Empty(t, got)
}
