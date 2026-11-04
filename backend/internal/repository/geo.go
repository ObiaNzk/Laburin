package repository

import (
	"context"

	"github.com/google/uuid"
	"github.com/laburin/backend/internal/model"
	"github.com/redis/go-redis/v9"
)

// geoKey holds the active offers' locations: a Redis GEO set whose members are offer ids.
const geoKey = "offers:active:geo"

// RedisGeo is a view derived from Postgres: OfferService.SyncGeoIndex rebuilds it at startup.
type RedisGeo struct {
	client *redis.Client
}

func NewRedisGeo(client *redis.Client) *RedisGeo {
	return &RedisGeo{client: client}
}

func (g *RedisGeo) Add(ctx context.Context, location model.OfferLocation) error {
	return g.client.GeoAdd(ctx, geoKey, geoLocation(location)).Err()
}

func (g *RedisGeo) Remove(ctx context.Context, offerID uuid.UUID) error {
	return g.client.ZRem(ctx, geoKey, offerID.String()).Err()
}

func (g *RedisGeo) Search(ctx context.Context, latitude, longitude float64, radiusKm int) ([]uuid.UUID, error) {
	query := redis.GeoSearchQuery{Latitude: latitude, Longitude: longitude, Radius: float64(radiusKm), RadiusUnit: "km", Sort: "ASC"}
	members, err := g.client.GeoSearch(ctx, geoKey, &query).Result()
	if err != nil {
		return nil, err
	}
	ids := make([]uuid.UUID, 0, len(members))
	for _, member := range members {
		id, err := uuid.Parse(member)
		if err != nil {
			continue // not ours: ignore rather than break the map
		}
		ids = append(ids, id)
	}
	return ids, nil
}

// Replace swaps the whole index atomically (MULTI/EXEC), so a search never sees it half built.
func (g *RedisGeo) Replace(ctx context.Context, locations []model.OfferLocation) error {
	_, err := g.client.TxPipelined(ctx, func(pipe redis.Pipeliner) error {
		pipe.Del(ctx, geoKey)
		if len(locations) == 0 {
			return nil
		}
		members := make([]*redis.GeoLocation, len(locations))
		for i, location := range locations {
			members[i] = geoLocation(location)
		}
		pipe.GeoAdd(ctx, geoKey, members...)
		return nil
	})
	return err
}

func geoLocation(location model.OfferLocation) *redis.GeoLocation {
	return &redis.GeoLocation{Name: location.ID.String(), Latitude: location.Latitude, Longitude: location.Longitude}
}
