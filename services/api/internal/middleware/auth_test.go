package middleware_test

import (
	"crypto/rand"
	"crypto/rsa"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"

	"github.com/muratosaro/coreme/api/internal/middleware"
)

func TestMain(m *testing.M) {
	os.Setenv("JWT_SECRET", "test-secret-32-chars-xxxxxxxxxx")
	gin.SetMode(gin.TestMode)
	os.Exit(m.Run())
}

// ── helpers ───────────────────────────────────────────────────────────────────

// buildRouter returns a minimal gin engine that applies the Auth() middleware
// and responds 200 with the userId from context when authentication passes.
func buildRouter() *gin.Engine {
	r := gin.New()
	r.GET("/protected", middleware.Auth(), func(c *gin.Context) {
		userID := c.GetString("userId")
		c.JSON(http.StatusOK, gin.H{"userId": userID})
	})
	return r
}

// signHS256 creates a signed HS256 JWT with the test secret and the given
// expiry offset relative to now.
func signHS256(userID string, ttl time.Duration) string {
	tok, err := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": userID,
		"exp":    time.Now().Add(ttl).Unix(),
	}).SignedString([]byte("test-secret-32-chars-xxxxxxxxxx"))
	if err != nil {
		panic(err)
	}
	return tok
}

// signWrongSecret creates an HS256 JWT signed with the WRONG secret.
func signWrongSecret(userID string) string {
	tok, err := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": userID,
		"exp":    time.Now().Add(15 * time.Minute).Unix(),
	}).SignedString([]byte("completely-different-secret-xyz!"))
	if err != nil {
		panic(err)
	}
	return tok
}

// signRS256 creates a JWT using RS256 (wrong signing method for this service).
func signRS256(userID string) string {
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		panic(err)
	}
	tok, err := jwt.NewWithClaims(jwt.SigningMethodRS256, jwt.MapClaims{
		"userId": userID,
		"exp":    time.Now().Add(15 * time.Minute).Unix(),
	}).SignedString(key)
	if err != nil {
		panic(err)
	}
	return tok
}

// get fires a GET /protected request with the given Authorization header value.
// Pass an empty string to omit the header entirely.
func get(router *gin.Engine, authHeader string) *httptest.ResponseRecorder {
	req := httptest.NewRequest(http.MethodGet, "/protected", nil)
	if authHeader != "" {
		req.Header.Set("Authorization", authHeader)
	}
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)
	return w
}

// decodeBody unmarshals the recorder body into a map for convenient field access.
func decodeBody(t *testing.T, w *httptest.ResponseRecorder) map[string]interface{} {
	t.Helper()
	var m map[string]interface{}
	if err := json.NewDecoder(w.Body).Decode(&m); err != nil {
		t.Fatalf("decodeBody: %v", err)
	}
	return m
}

// ── Tests ─────────────────────────────────────────────────────────────────────

func TestAuth_MissingAuthorizationHeader(t *testing.T) {
	w := get(buildRouter(), "")
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (no header), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_NoBearerPrefix(t *testing.T) {
	tok := signHS256("user-1", 15*time.Minute)
	// Pass the raw token without the "Bearer " prefix.
	w := get(buildRouter(), tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (no Bearer prefix), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_TokenPrefixedWithWrongScheme(t *testing.T) {
	tok := signHS256("user-1", 15*time.Minute)
	// "Token " scheme instead of "Bearer ".
	w := get(buildRouter(), "Token "+tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (wrong scheme), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_InvalidJWT(t *testing.T) {
	w := get(buildRouter(), "Bearer this.is.garbage")
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (invalid JWT), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_ExpiredJWT(t *testing.T) {
	// Token whose exp is 1 minute in the past.
	tok := signHS256("user-2", -1*time.Minute)
	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (expired JWT), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_WrongSigningMethod_RS256(t *testing.T) {
	// The middleware explicitly rejects non-HMAC signing methods.
	tok := signRS256("user-3")
	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (RS256 signing method), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_WrongSecret(t *testing.T) {
	tok := signWrongSecret("user-4")
	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (wrong secret), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_ValidJWT_SetsUserID(t *testing.T) {
	const wantUserID = "abc-123-uuid"
	tok := signHS256(wantUserID, 15*time.Minute)
	w := get(buildRouter(), "Bearer "+tok)

	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	body := decodeBody(t, w)
	if body["userId"] != wantUserID {
		t.Errorf("userId: got %v, want %s", body["userId"], wantUserID)
	}
}

func TestAuth_ValidJWT_GinCreateTestContext(t *testing.T) {
	// Verify the middleware using gin.CreateTestContext directly so we can
	// inspect c.Get("userId") without an HTTP round-trip through a router.
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)

	const wantUserID = "ctx-test-user"
	tok := signHS256(wantUserID, 15*time.Minute)

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+tok)
	c.Request = req

	// Invoke the middleware directly.
	middleware.Auth()(c)

	// When the middleware succeeds it calls c.Next(), which is a no-op in a
	// bare TestContext; the response code is NOT set to 401.
	if w.Code == http.StatusUnauthorized {
		t.Fatalf("middleware aborted with 401, want pass-through")
	}

	gotUserID, exists := c.Get("userId")
	if !exists {
		t.Fatal("userId key not found in gin context")
	}
	if gotUserID != wantUserID {
		t.Errorf("userId: got %v, want %s", gotUserID, wantUserID)
	}
}

func TestAuth_MissingUserIDClaim(t *testing.T) {
	// Token has all required fields except userId.
	tok, _ := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"sub": "whatever",
		"exp": time.Now().Add(15 * time.Minute).Unix(),
	}).SignedString([]byte("test-secret-32-chars-xxxxxxxxxx"))

	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (missing userId claim), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_EmptyUserIDClaim(t *testing.T) {
	// Token where userId is explicitly an empty string.
	tok, _ := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": "",
		"exp":    time.Now().Add(15 * time.Minute).Unix(),
	}).SignedString([]byte("test-secret-32-chars-xxxxxxxxxx"))

	w := get(buildRouter(), "Bearer "+tok)
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (empty userId claim), got %d: %s", w.Code, w.Body.String())
	}
}

func TestAuth_AbortPreventsNextHandlerExecution(t *testing.T) {
	nextCalled := false
	r := gin.New()
	r.GET("/protected",
		middleware.Auth(),
		func(c *gin.Context) {
			nextCalled = true
			c.Status(http.StatusOK)
		},
	)

	// Invalid token – middleware should abort before the next handler runs.
	w := get(r, "Bearer invalid.token.here")
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401, got %d", w.Code)
	}
	if nextCalled {
		t.Error("next handler should NOT have been called after abort")
	}
}
