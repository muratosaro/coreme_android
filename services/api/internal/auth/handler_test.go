package auth_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"

	"github.com/muratosaro/coreme/api/internal/auth"
	"github.com/muratosaro/coreme/api/internal/testutil"
)

// testDB holds the shared pool for the whole test binary.
var testDB *pgxpool.Pool

func TestMain(m *testing.M) {
	// JWT secrets must be set before any handler is invoked.
	os.Setenv("JWT_SECRET", "test-secret-32-chars-xxxxxxxxxx")
	os.Setenv("JWT_REFRESH_SECRET", "test-refresh-32-chars-xxxxxxxxxx")

	gin.SetMode(gin.TestMode)

	// Connect – pool is nil when DB is not available (tests will skip below).
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	pool, err := pgxpool.New(ctx, testDSN())
	if err == nil {
		if pingErr := pool.Ping(ctx); pingErr != nil {
			pool.Close()
			pool = nil
		}
	} else {
		pool = nil
	}
	testDB = pool

	code := m.Run()
	if testDB != nil {
		testDB.Close()
	}
	os.Exit(code)
}

func testDSN() string {
	if v := os.Getenv("DATABASE_URL"); v != "" {
		return v
	}
	host := envOrDefault("DB_HOST", "localhost")
	port := envOrDefault("DB_PORT", "5432")
	name := envOrDefault("DB_NAME", "coreme")
	user := envOrDefault("DB_USER", "postgres")
	pass := envOrDefault("DB_PASSWORD", "postgres")
	return fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable", user, pass, host, port, name)
}

func envOrDefault(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

// skipIfNoDB marks the test as skipped when the database is unavailable.
func skipIfNoDB(t *testing.T) {
	t.Helper()
	if testDB == nil {
		t.Skip("test DB not available – skipping integration test")
	}
}

// newRouter builds a fresh gin engine with the auth routes mounted.
func newRouter() *gin.Engine {
	r := gin.New()
	auth.RegisterRoutes(r.Group("/auth"), testDB)
	return r
}

// post is a tiny helper that fires a POST request against the test router and
// returns the recorded response.
func post(t *testing.T, router *gin.Engine, path string, body interface{}) *httptest.ResponseRecorder {
	t.Helper()
	var b []byte
	if body != nil {
		var err error
		b, err = json.Marshal(body)
		if err != nil {
			t.Fatalf("json.Marshal: %v", err)
		}
	}
	req := httptest.NewRequest(http.MethodPost, path, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)
	return w
}

// jsonBody unmarshals the recorder body into a map for easy field access.
func jsonBody(t *testing.T, w *httptest.ResponseRecorder) map[string]interface{} {
	t.Helper()
	var m map[string]interface{}
	if err := json.NewDecoder(w.Body).Decode(&m); err != nil {
		t.Fatalf("decode response body: %v", err)
	}
	return m
}

// ── Register ──────────────────────────────────────────────────────────────────

func TestRegister_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "reg_ok_" + uid
	email := "reg_ok_" + uid + "@example.com"

	w := post(t, newRouter(), "/auth/register", map[string]string{
		"username":     username,
		"display_name": "Reg OK",
		"email":        email,
		"password":     "password123",
	})

	if w.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", w.Code, w.Body.String())
	}

	body := jsonBody(t, w)
	if body["accessToken"] == nil {
		t.Error("expected accessToken in response")
	}
	if body["refreshToken"] == nil {
		t.Error("expected refreshToken in response")
	}
	userMap, ok := body["user"].(map[string]interface{})
	if !ok {
		t.Fatal("expected user object in response")
	}
	if userMap["username"] != username {
		t.Errorf("username mismatch: got %v, want %s", userMap["username"], username)
	}

	// Cleanup: delete the user that was just registered.
	var userID string
	testDB.QueryRow(context.Background(), "SELECT id FROM users WHERE username=$1", username).Scan(&userID)
	if userID != "" {
		t.Cleanup(func() { testutil.DeleteUser(testDB, userID) })
	}
}

func TestRegister_DuplicateUsername(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "dup_user_" + uid
	email1 := "dup1_" + uid + "@example.com"
	email2 := "dup2_" + uid + "@example.com"

	r := newRouter()

	// First registration – must succeed.
	w := post(t, r, "/auth/register", map[string]string{
		"username":     username,
		"display_name": "Dup",
		"email":        email1,
		"password":     "password123",
	})
	if w.Code != http.StatusCreated {
		t.Fatalf("setup register failed: %d %s", w.Code, w.Body.String())
	}

	// Cleanup the first user.
	var id string
	testDB.QueryRow(context.Background(), "SELECT id FROM users WHERE username=$1", username).Scan(&id)
	t.Cleanup(func() { testutil.DeleteUser(testDB, id) })

	// Second registration with same username but different email – must conflict.
	w2 := post(t, r, "/auth/register", map[string]string{
		"username":     username,
		"display_name": "Dup2",
		"email":        email2,
		"password":     "password123",
	})
	if w2.Code != http.StatusConflict {
		t.Errorf("expected 409 Conflict, got %d: %s", w2.Code, w2.Body.String())
	}
}

func TestRegister_DuplicateEmail(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	user1 := "dup_email1_" + uid
	user2 := "dup_email2_" + uid
	sharedEmail := "shared_" + uid + "@example.com"

	r := newRouter()

	w := post(t, r, "/auth/register", map[string]string{
		"username":     user1,
		"display_name": "DE1",
		"email":        sharedEmail,
		"password":     "password123",
	})
	if w.Code != http.StatusCreated {
		t.Fatalf("setup failed: %d %s", w.Code, w.Body.String())
	}

	var id string
	testDB.QueryRow(context.Background(), "SELECT id FROM users WHERE username=$1", user1).Scan(&id)
	t.Cleanup(func() { testutil.DeleteUser(testDB, id) })

	w2 := post(t, r, "/auth/register", map[string]string{
		"username":     user2,
		"display_name": "DE2",
		"email":        sharedEmail,
		"password":     "password123",
	})
	if w2.Code != http.StatusConflict {
		t.Errorf("expected 409, got %d: %s", w2.Code, w2.Body.String())
	}
}

func TestRegister_MissingFields(t *testing.T) {
	skipIfNoDB(t)
	r := newRouter()

	// Completely empty body.
	w := post(t, r, "/auth/register", map[string]string{})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d: %s", w.Code, w.Body.String())
	}

	// Missing email.
	uid := testutil.UniqueID()
	w2 := post(t, r, "/auth/register", map[string]string{
		"username":     "miss_" + uid,
		"display_name": "Miss",
		"password":     "password123",
	})
	if w2.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422 (missing email), got %d: %s", w2.Code, w2.Body.String())
	}
}

func TestRegister_ShortPassword(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	w := post(t, newRouter(), "/auth/register", map[string]string{
		"username":     "short_pw_" + uid,
		"display_name": "Short",
		"email":        "short_" + uid + "@example.com",
		"password":     "12345", // 5 chars – minimum is 6
	})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d: %s", w.Code, w.Body.String())
	}
}

func TestRegister_InvalidEmail(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	w := post(t, newRouter(), "/auth/register", map[string]string{
		"username":     "bademail_" + uid,
		"display_name": "Bad",
		"email":        "not-an-email",
		"password":     "password123",
	})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d: %s", w.Code, w.Body.String())
	}
}

// ── Login ─────────────────────────────────────────────────────────────────────

// seedUser creates a test user with a known plain-text password via direct DB
// INSERT so tests are fast (avoids double bcrypt).
func seedUser(t *testing.T, username, email, plainPassword string) string {
	t.Helper()
	hash, err := bcrypt.GenerateFromPassword([]byte(plainPassword), bcrypt.MinCost)
	if err != nil {
		t.Fatalf("bcrypt: %v", err)
	}
	id := testutil.CreateUser(t, testDB, username, email, string(hash))
	return id
}

func TestLogin_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "login_ok_" + uid
	email := "login_ok_" + uid + "@example.com"
	seedUser(t, username, email, "secret123")

	w := post(t, newRouter(), "/auth/login", map[string]string{
		"username": username,
		"password": "secret123",
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	body := jsonBody(t, w)
	if body["accessToken"] == nil {
		t.Error("expected accessToken")
	}
	if body["refreshToken"] == nil {
		t.Error("expected refreshToken")
	}
}

func TestLogin_WrongPassword(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "login_wp_" + uid
	email := "login_wp_" + uid + "@example.com"
	seedUser(t, username, email, "correct_password")

	w := post(t, newRouter(), "/auth/login", map[string]string{
		"username": username,
		"password": "wrong_password",
	})
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401, got %d: %s", w.Code, w.Body.String())
	}
}

func TestLogin_WrongUsername(t *testing.T) {
	skipIfNoDB(t)
	w := post(t, newRouter(), "/auth/login", map[string]string{
		"username": "no_such_user_zzzz",
		"password": "irrelevant",
	})
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401, got %d: %s", w.Code, w.Body.String())
	}
}

func TestLogin_MissingFields(t *testing.T) {
	skipIfNoDB(t)
	r := newRouter()

	// Empty body.
	w := post(t, r, "/auth/login", map[string]string{})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d: %s", w.Code, w.Body.String())
	}

	// Missing password.
	w2 := post(t, r, "/auth/login", map[string]string{"username": "someone"})
	if w2.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422 (missing password), got %d: %s", w2.Code, w2.Body.String())
	}
}

// ── Refresh ───────────────────────────────────────────────────────────────────

// makeRefreshToken generates a refresh JWT signed with the test secret.
func makeRefreshToken(userID string, ttl time.Duration) string {
	tok, _ := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": userID,
		"exp":    time.Now().Add(ttl).Unix(),
	}).SignedString([]byte("test-refresh-32-chars-xxxxxxxxxx"))
	return tok
}

func TestRefresh_ValidToken(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "ref_ok_" + uid
	email := "ref_ok_" + uid + "@example.com"
	userID := seedUser(t, username, email, "password123")

	refreshToken := makeRefreshToken(userID, 30*24*time.Hour)
	testutil.CreateSession(t, testDB, userID, refreshToken, time.Now().Add(30*24*time.Hour))

	w := post(t, newRouter(), "/auth/refresh", map[string]string{
		"refreshToken": refreshToken,
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	body := jsonBody(t, w)
	if body["accessToken"] == nil {
		t.Error("expected accessToken in refresh response")
	}
}

func TestRefresh_InvalidToken(t *testing.T) {
	skipIfNoDB(t)
	w := post(t, newRouter(), "/auth/refresh", map[string]string{
		"refreshToken": "this.is.not.a.jwt",
	})
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401, got %d: %s", w.Code, w.Body.String())
	}
}

func TestRefresh_ExpiredSession(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "ref_exp_" + uid
	email := "ref_exp_" + uid + "@example.com"
	userID := seedUser(t, username, email, "password123")

	// Make a token that is still valid from a JWT perspective but whose session
	// has already expired in the DB.
	refreshToken := makeRefreshToken(userID, 30*24*time.Hour)
	// Insert session with expires_at in the past.
	testutil.CreateSession(t, testDB, userID, refreshToken, time.Now().UTC().Add(-1*time.Hour))

	w := post(t, newRouter(), "/auth/refresh", map[string]string{
		"refreshToken": refreshToken,
	})
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401 (expired session), got %d: %s", w.Code, w.Body.String())
	}
}

func TestRefresh_MissingBody(t *testing.T) {
	skipIfNoDB(t)
	// Send a request with no body at all.
	req := httptest.NewRequest(http.MethodPost, "/auth/refresh", nil)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	newRouter().ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

func TestRefresh_RotatesToken(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := seedUser(t, "rot_"+uid, "rot_"+uid+"@example.com", "password123")

	oldToken := makeRefreshToken(userID, 30*24*time.Hour)
	testutil.CreateSession(t, testDB, userID, oldToken, time.Now().UTC().Add(30*24*time.Hour))

	w := post(t, newRouter(), "/auth/refresh", map[string]string{"refreshToken": oldToken})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var body map[string]interface{}
	json.NewDecoder(w.Body).Decode(&body)

	newRefresh, _ := body["refreshToken"].(string)
	if newRefresh == "" {
		t.Fatal("response must contain refreshToken")
	}
	if newRefresh == oldToken {
		t.Error("new refreshToken must differ from old one")
	}

	// Old token must be rejected after rotation.
	w2 := post(t, newRouter(), "/auth/refresh", map[string]string{"refreshToken": oldToken})
	if w2.Code != http.StatusUnauthorized {
		t.Errorf("expected old token to be rejected (401), got %d", w2.Code)
	}
}

// ── Logout ────────────────────────────────────────────────────────────────────

func TestLogout_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	username := "logout_ok_" + uid
	email := "logout_ok_" + uid + "@example.com"
	userID := seedUser(t, username, email, "password123")

	refreshToken := makeRefreshToken(userID, 30*24*time.Hour)
	testutil.CreateSession(t, testDB, userID, refreshToken, time.Now().Add(30*24*time.Hour))

	w := post(t, newRouter(), "/auth/logout", map[string]string{
		"refreshToken": refreshToken,
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	body := jsonBody(t, w)
	if body["message"] == nil {
		t.Error("expected message in logout response")
	}

	// Session must have been removed.
	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM sessions WHERE refresh_token=$1", refreshToken,
	).Scan(&count)
	if count != 0 {
		t.Error("session was not deleted after logout")
	}
}

func TestLogout_UnknownToken(t *testing.T) {
	skipIfNoDB(t)
	// Logging out with a token that does not exist in the DB is still 200.
	w := post(t, newRouter(), "/auth/logout", map[string]string{
		"refreshToken": "unknown.refresh.token",
	})
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
}

func TestLogout_MissingBody(t *testing.T) {
	skipIfNoDB(t)
	// No body – the handler binds refreshToken as required, so this is a 400.
	req := httptest.NewRequest(http.MethodPost, "/auth/logout", nil)
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	newRouter().ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}
