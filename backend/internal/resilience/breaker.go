package resilience

import (
	"time"

	"github.com/sony/gobreaker"
)

// NewDBBreaker returns a circuit breaker tuned for database calls.
//
// State transitions:
//   - Closed → Open  : 5 consecutive failures OR >50% failures over 10+ requests
//   - Open → Half-open: after 30 s; allows up to 3 trial requests
//   - Half-open → Closed: all 3 trial requests succeed
//   - Half-open → Open  : any trial request fails
func NewDBBreaker(name string) *gobreaker.CircuitBreaker {
	return gobreaker.NewCircuitBreaker(gobreaker.Settings{
		Name:        name,
		MaxRequests: 3,
		Interval:    60 * time.Second,
		Timeout:     30 * time.Second,
		ReadyToTrip: func(c gobreaker.Counts) bool {
			if c.ConsecutiveFailures >= 5 {
				return true
			}
			if c.Requests >= 10 {
				failRate := float64(c.TotalFailures) / float64(c.Requests)
				return failRate > 0.5
			}
			return false
		},
	})
}
