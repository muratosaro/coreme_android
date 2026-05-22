package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"sync/atomic"
	"syscall"
	"time"

	"github.com/getsentry/sentry-go"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	"github.com/sony/gobreaker"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"go.opentelemetry.io/contrib/instrumentation/github.com/gin-gonic/gin/otelgin"

	"github.com/muratosaro/coreme/api/internal/auth"
	"github.com/muratosaro/coreme/api/internal/calls"
	"github.com/muratosaro/coreme/api/internal/channels"
	"github.com/muratosaro/coreme/api/internal/chats"
	"github.com/muratosaro/coreme/api/internal/contacts"
	"github.com/muratosaro/coreme/api/internal/db"
	"github.com/muratosaro/coreme/api/internal/media"
	"github.com/muratosaro/coreme/api/internal/middleware"
	"github.com/muratosaro/coreme/api/internal/resilience"
	"github.com/muratosaro/coreme/api/internal/settings"
	"github.com/muratosaro/coreme/api/internal/tracing"
	"github.com/muratosaro/coreme/api/internal/users"
)

var ready atomic.Bool

func main() {
	_ = godotenv.Load()

	initSentry()
	defer sentry.Flush(2 * time.Second)

	ctx := context.Background()
	shutdownTracing := tracing.Init(ctx)
	defer shutdownTracing(ctx) //nolint:errcheck
	pool, err := db.Connect(ctx)
	if err != nil {
		sentry.CaptureException(err)
		log.Fatalf("db connect: %v", err)
	}
	defer pool.Close()

	port := os.Getenv("API_PORT")
	if port == "" {
		port = "3001"
	}

	uploadsDir := filepath.Join("..", "uploads")
	if err := os.MkdirAll(uploadsDir, 0755); err != nil {
		log.Fatalf("create uploads dir: %v", err)
	}

	dbBreaker := resilience.NewDBBreaker("postgres")

	r := gin.Default()
	r.Use(otelgin.Middleware(envOrDefault("OTEL_SERVICE_NAME", "coreme-api")))
	r.Use(sentryMiddleware())
	r.Use(corsMiddleware())
	r.Use(timeoutMiddleware(30 * time.Second))
	r.Static("/uploads", uploadsDir)

	// Prometheus metrics endpoint — scraped by Prometheus every 15 s
	r.GET("/metrics", gin.WrapH(promhttp.Handler()))

	r.GET("/health", func(c *gin.Context) {
		_, err := dbBreaker.Execute(func() (interface{}, error) {
			hctx, cancel := context.WithTimeout(c.Request.Context(), 2*time.Second)
			defer cancel()
			return nil, pool.Ping(hctx)
		})
		if err != nil {
			state := "db unavailable"
			if err == gobreaker.ErrOpenState {
				state = "circuit open"
			}
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"status":  "error",
				"service": "api",
				"error":   state,
			})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "ok", "service": "api"})
	})

	r.GET("/ready", func(c *gin.Context) {
		if !ready.Load() {
			c.JSON(http.StatusServiceUnavailable, gin.H{"status": "not ready"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "ready"})
	})

	api := r.Group("/api")
	auth.RegisterRoutes(api.Group("/auth"), pool)

	protected := api.Group("")
	protected.Use(middleware.Auth())
	users.RegisterRoutes(protected.Group("/users"), pool)
	chats.RegisterRoutes(protected.Group("/chats"), pool)
	contacts.RegisterRoutes(protected.Group("/contacts"), pool)
	settings.RegisterRoutes(protected.Group("/settings"), pool)
	calls.RegisterRoutes(protected.Group("/calls"), pool)
	channels.RegisterRoutes(protected.Group("/channels"), pool)
	media.RegisterRoutes(protected.Group("/media"), uploadsDir)

	srv := &http.Server{
		Addr:         ":" + port,
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	go func() {
		ready.Store(true)
		log.Printf("[api] Listening on :%s", port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("run: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGTERM, syscall.SIGINT)
	<-quit

	ready.Store(false)
	log.Println("[api] Shutting down...")

	shutCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := srv.Shutdown(shutCtx); err != nil {
		log.Printf("[api] Shutdown error: %v", err)
	}
	log.Println("[api] Stopped")
}

// timeoutMiddleware cancels the request context after d, preventing goroutine
// leaks when DB or downstream calls hang.
func timeoutMiddleware(d time.Duration) gin.HandlerFunc {
	return func(c *gin.Context) {
		ctx, cancel := context.WithTimeout(c.Request.Context(), d)
		defer cancel()
		c.Request = c.Request.WithContext(ctx)
		c.Next()
	}
}

func initSentry() {
	dsn := os.Getenv("SENTRY_DSN")
	if dsn == "" {
		return
	}
	if err := sentry.Init(sentry.ClientOptions{
		Dsn:              dsn,
		Environment:      envOrDefault("APP_ENV", "production"),
		TracesSampleRate: 0.2,
		BeforeSend: func(event *sentry.Event, _ *sentry.EventHint) *sentry.Event {
			if event.Request != nil {
				delete(event.Request.Headers, "Authorization")
				delete(event.Request.Headers, "Cookie")
				url := event.Request.URL
				if strings.Contains(url, "/auth/") || strings.Contains(url, "/messages") {
					event.Request.Data = "[redacted]"
				}
			}
			return event
		},
	}); err != nil {
		log.Printf("[sentry] init error: %v", err)
	}
}

func sentryMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		hub := sentry.CurrentHub().Clone()
		hub.Scope().SetRequest(c.Request)
		defer func() {
			if r := recover(); r != nil {
				hub.RecoverWithContext(c.Request.Context(), r)
				sentry.Flush(2 * time.Second)
				panic(r)
			}
		}()
		c.Next()
		if len(c.Errors) > 0 {
			hub.CaptureMessage(c.Errors.String())
		}
	}
}

func corsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusOK)
			return
		}
		c.Next()
	}
}

func envOrDefault(key, defaultValue string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultValue
}
