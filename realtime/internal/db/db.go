package db

import (
	"context"
	"fmt"
	"os"

	"github.com/jackc/pgx/v5/pgxpool"
)

func Connect(ctx context.Context) (*pgxpool.Pool, error) {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		host := envOr("DB_HOST", "localhost")
		port := envOr("DB_PORT", "5432")
		name := envOr("DB_NAME", "coreme")
		user := envOr("DB_USER", "postgres")
		pass := envOr("DB_PASSWORD", "postgres")
		dsn = fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable", user, pass, host, port, name)
	}
	return pgxpool.New(ctx, dsn)
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
