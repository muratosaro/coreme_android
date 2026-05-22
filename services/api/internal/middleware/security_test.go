package middleware_test

import (
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// TestAuth_AlgorithmNone verifies that a JWT with alg=none is rejected.
// This is the classic "none algorithm" attack where an attacker strips the
// signature and sets the algorithm to "none" hoping the server accepts it.
func TestAuth_AlgorithmNone(t *testing.T) {
	// jwt-go v5 does not expose UnsafeAllowNoneSignatureType via simple
	// SignedString, but we can build the raw token string manually:
	// header.payload.  (empty signature)
	//   header: {"alg":"none","typ":"JWT"}
	//   payload: {"userId":"attacker","exp":<future>}
	import64 := func(s string) string {
		const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
		_ = chars
		return s
	}
	_ = import64

	// Use the library's unsafe path if available, else craft manually.
	// The golang-jwt library exports jwt.UnsafeAllowNoneSignatureType for this.
	tok, err := jwt.NewWithClaims(jwt.SigningMethodNone, jwt.MapClaims{
		"userId": "attacker-id",
		"exp":    time.Now().Add(15 * time.Minute).Unix(),
	}).SignedString(jwt.UnsafeAllowNoneSignatureType)
	if err != nil {
		t.Fatalf("failed to create none-algorithm token: %v", err)
	}

	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != 401 {
		t.Errorf("alg=none attack: expected 401, got %d — server accepted unsigned token!", w.Code)
	}
}

// TestAuth_EmptyBearerValue verifies that "Bearer " with no token is rejected.
func TestAuth_EmptyBearerValue(t *testing.T) {
	w := get(buildRouter(), "Bearer ")
	if w.Code != 401 {
		t.Errorf("expected 401 for empty bearer value, got %d", w.Code)
	}
}

// TestAuth_TokenWithFutureIssued verifies tokens with far-future nbf still work
// if exp is valid — the middleware only checks exp, not nbf.
func TestAuth_FutureExpiryIsAccepted(t *testing.T) {
	tok := signHS256("user-future", 24*time.Hour)
	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != 200 {
		t.Errorf("expected 200 for valid future-expiry token, got %d", w.Code)
	}
}

// TestAuth_JustExpiredToken verifies a token expired 1 second ago is rejected.
func TestAuth_JustExpiredToken(t *testing.T) {
	tok := signHS256("user-expired", -1*time.Second)
	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != 401 {
		t.Errorf("expected 401 for just-expired token, got %d", w.Code)
	}
}

// TestAuth_NullByteInToken verifies the middleware handles unusual inputs safely.
func TestAuth_MalformedTokenVariants(t *testing.T) {
	cases := []struct {
		name   string
		header string
	}{
		{"only_dots", "Bearer ..."},
		{"sql_injection", "Bearer ' OR '1'='1"},
		{"two_parts_only", "Bearer header.payload"},
		{"unicode_token", "Bearer héllo.wörld.tëst"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			w := get(buildRouter(), tc.header)
			if w.Code != 401 {
				t.Errorf("%s: expected 401, got %d", tc.name, w.Code)
			}
		})
	}
}
