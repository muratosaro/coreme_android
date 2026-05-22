package tracing

import (
	"context"
	"log"
	"os"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.21.0"
)

// Init sets up an OpenTelemetry tracer that exports to Jaeger (or any
// OTLP-compatible collector) via HTTP.
//
// Env vars:
//
//	OTEL_EXPORTER_OTLP_ENDPOINT  — default http://jaeger:4318
//	OTEL_SERVICE_NAME             — default "coreme-api"
//
// Returns a shutdown function — call it on process exit.
func Init(ctx context.Context) (shutdown func(context.Context) error) {
	endpoint := os.Getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
	if endpoint == "" {
		endpoint = "http://jaeger:4318"
	}

	serviceName := os.Getenv("OTEL_SERVICE_NAME")
	if serviceName == "" {
		serviceName = "coreme-api"
	}

	exporter, err := otlptracehttp.New(ctx,
		otlptracehttp.WithEndpoint(endpoint),
		otlptracehttp.WithInsecure(),
	)
	if err != nil {
		log.Printf("[tracing] exporter init error: %v — tracing disabled", err)
		return func(context.Context) error { return nil }
	}

	res := resource.NewWithAttributes(
		semconv.SchemaURL,
		semconv.ServiceName(serviceName),
		semconv.DeploymentEnvironment(envOrDefault("APP_ENV", "production")),
	)

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(exporter),
		sdktrace.WithResource(res),
		sdktrace.WithSampler(sdktrace.TraceIDRatioBased(0.2)),
	)

	otel.SetTracerProvider(tp)
	log.Printf("[tracing] OpenTelemetry → %s (service: %s)", endpoint, serviceName)

	return tp.Shutdown
}

func envOrDefault(key, defaultValue string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultValue
}
