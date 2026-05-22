package db

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/cenkalti/backoff/v4"
	"github.com/jackc/pgx/v5/pgxpool"
)

func Connect(ctx context.Context) (*pgxpool.Pool, error) {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		host := os.Getenv("DB_HOST")
		if host == "" {
			host = "localhost"
		}
		port := os.Getenv("DB_PORT")
		if port == "" {
			port = "5432"
		}
		name := os.Getenv("DB_NAME")
		if name == "" {
			name = "coreme"
		}
		user := os.Getenv("DB_USER")
		if user == "" {
			user = "postgres"
		}
		pass := os.Getenv("DB_PASSWORD")
		dsn = fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable", user, pass, host, port, name)
	}

	config, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		return nil, fmt.Errorf("pgxpool.ParseConfig: %w", err)
	}

	config.ConnConfig.RuntimeParams["timezone"] = "UTC"

	config.MaxConns = 20
	config.MinConns = 5
	config.MaxConnLifetime = 30 * time.Minute
	config.MaxConnIdleTime = 10 * time.Minute
	config.HealthCheckPeriod = 1 * time.Minute

	// Retry with exponential backoff so the API survives a slow DB startup.
	b := backoff.NewExponentialBackOff()
	b.InitialInterval = 1 * time.Second
	b.MaxInterval = 10 * time.Second
	b.MaxElapsedTime = 60 * time.Second

	var pool *pgxpool.Pool
	attempt := 0
	err = backoff.Retry(func() error {
		attempt++
		p, connErr := pgxpool.NewWithConfig(ctx, config)
		if connErr != nil {
			log.Printf("[db] connect attempt %d failed: %v", attempt, connErr)
			return connErr
		}
		if pingErr := p.Ping(ctx); pingErr != nil {
			p.Close()
			log.Printf("[db] ping attempt %d failed: %v", attempt, pingErr)
			return pingErr
		}
		pool = p
		return nil
	}, backoff.WithContext(b, ctx))

	if err != nil {
		return nil, fmt.Errorf("db connect after %d attempts: %w", attempt, err)
	}

	log.Printf("[db] connected after %d attempt(s)", attempt)
	return pool, nil
}
