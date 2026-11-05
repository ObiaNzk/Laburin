// Command api serves LaburIN's REST API (contracts in ../docs/api).
package main

import (
	"context"
	"errors"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/laburin/backend/internal/api"
	"github.com/laburin/backend/internal/api/controller"
	"github.com/laburin/backend/internal/auth"
	"github.com/laburin/backend/internal/config"
	"github.com/laburin/backend/internal/repository"
	"github.com/laburin/backend/internal/service"
	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog/log"
	"golang.org/x/crypto/bcrypt"
)

func main() {
	cfg := config.Load()
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	pool, err := pgxpool.New(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatal().Err(err).Msg("postgres")
	}
	defer pool.Close()
	if err := pool.Ping(ctx); err != nil {
		log.Fatal().Err(err).Msg("postgres ping")
	}
	redisClient := redis.NewClient(&redis.Options{Addr: cfg.RedisAddr})
	defer redisClient.Close()
	if err := redisClient.Ping(ctx).Err(); err != nil {
		log.Fatal().Err(err).Msg("redis ping")
	}

	postgres := repository.NewPostgres(pool)
	tokens := auth.NewTokens(cfg.JWTSecret, cfg.TokenTTL, time.Now)
	offers := service.NewOfferService(postgres, repository.NewRedisGeo(redisClient))
	// Redis is a derived view: rebuild it from Postgres so it never drifts across restarts.
	if err := offers.SyncGeoIndex(ctx); err != nil {
		log.Fatal().Err(err).Msg("geo index sync")
	}
	controllers := api.Controllers{
		Auth:         controller.NewAuthController(service.NewAuthService(postgres, tokens, bcrypt.DefaultCost)),
		Offers:       controller.NewOfferController(offers),
		Applications: controller.NewApplicationController(service.NewApplicationService(postgres)),
		Profile:      controller.NewProfileController(service.NewProfileService(postgres)),
	}
	gin.SetMode(gin.ReleaseMode)
	router := api.NewRouter(tokens, controllers, gin.Logger())
	server := &http.Server{Addr: ":" + cfg.Port, Handler: router, ReadHeaderTimeout: 10 * time.Second, ReadTimeout: 60 * time.Second, WriteTimeout: 60 * time.Second}

	go func() {
		log.Info().Str("port", cfg.Port).Msg("LaburIN API listening")
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatal().Err(err).Msg("http server")
		}
	}()
	<-ctx.Done()
	shutdown, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := server.Shutdown(shutdown); err != nil {
		log.Error().Err(err).Msg("shutdown")
	}
}
