// Package config reads the environment. Required variables panic at startup when missing (RequireEnv).
package config

import (
	"fmt"
	"os"
	"time"
)

type Config struct {
	DatabaseURL string
	RedisAddr   string
	JWTSecret   []byte
	TokenTTL    time.Duration
	Port        string
}

// MinSecretBytes keeps HS256 keys at least as long as the hash.
const MinSecretBytes = 32

func Load() Config {
	secret := RequireEnv("JWT_SECRET")
	if len(secret) < MinSecretBytes {
		panic(fmt.Sprintf("JWT_SECRET must have at least %d bytes", MinSecretBytes))
	}
	ttl, err := time.ParseDuration(envOr("TOKEN_TTL", "720h"))
	if err != nil {
		panic(fmt.Sprintf("TOKEN_TTL: %v", err))
	}
	return Config{
		DatabaseURL: RequireEnv("DATABASE_URL"),
		RedisAddr:   RequireEnv("REDIS_ADDR"),
		JWTSecret:   []byte(secret),
		TokenTTL:    ttl,
		Port:        envOr("PORT", "8080"),
	}
}

func RequireEnv(key string) string {
	value := os.Getenv(key)
	if value == "" {
		panic(fmt.Sprintf("missing required environment variable %s", key))
	}
	return value
}

func envOr(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
