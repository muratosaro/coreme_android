package settings_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"

	"github.com/muratosaro/coreme/api/internal/settings"
	"github.com/muratosaro/coreme/api/internal/testutil"
)

var testDB *pgxpool.Pool

func TestMain(m *testing.M) {
	gin.SetMode(gin.TestMode)
	testDB = testutil.InitTestDB()
	code := m.Run()
	if testDB != nil {
		testDB.Close()
	}
	os.Exit(code)
}

func skipIfNoDB(t *testing.T) {
	t.Helper()
	if testDB == nil {
		t.Skip("test DB not available – skipping integration test")
	}
}

// seedUserWithPassword creates a user with a known plain-text password for
// tests that exercise the change-password flow.
func seedUserWithPassword(t *testing.T, username, email, plainPassword string) string {
	t.Helper()
	hash, err := bcrypt.GenerateFromPassword([]byte(plainPassword), bcrypt.MinCost)
	if err != nil {
		t.Fatalf("bcrypt: %v", err)
	}
	return testutil.CreateUser(t, testDB, username, email, string(hash))
}

func newRouter(userID string) *gin.Engine {
	r := gin.New()
	r.Use(func(c *gin.Context) { c.Set("userId", userID); c.Next() })
	settings.RegisterRoutes(r.Group("/settings"), testDB)
	return r
}

func doGet(t *testing.T, r *gin.Engine, path string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodGet, path, nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	return w
}

func doPost(t *testing.T, r *gin.Engine, path string, body interface{}) *httptest.ResponseRecorder {
	t.Helper()
	b, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPost, path, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	return w
}

func doPatch(t *testing.T, r *gin.Engine, path string, body interface{}) *httptest.ResponseRecorder {
	t.Helper()
	b, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPatch, path, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	return w
}

func jsonBody(t *testing.T, w *httptest.ResponseRecorder) map[string]interface{} {
	t.Helper()
	var m map[string]interface{}
	if err := json.NewDecoder(w.Body).Decode(&m); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	return m
}

// ── GET /settings ─────────────────────────────────────────────────────────────

func TestGetSettings_CreatesDefaultsOnFirstCall(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "gs_"+uid, "gs_"+uid+"@x.com")
	t.Cleanup(func() {
		testDB.Exec(context.Background(), "DELETE FROM user_settings WHERE user_id=$1", userID)
	})

	w := doGet(t, newRouter(userID), "/settings")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["user_id"] != userID {
		t.Errorf("expected user_id=%s, got %v", userID, body["user_id"])
	}
	// Idempotent: calling again should not fail.
	w2 := doGet(t, newRouter(userID), "/settings")
	if w2.Code != http.StatusOK {
		t.Errorf("second call: expected 200, got %d", w2.Code)
	}
}

// ── PATCH /settings ───────────────────────────────────────────────────────────

func TestUpdateSettings_Theme(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "upd_st_"+uid, "upd_st_"+uid+"@x.com")
	t.Cleanup(func() {
		testDB.Exec(context.Background(), "DELETE FROM user_settings WHERE user_id=$1", userID)
	})

	w := doPatch(t, newRouter(userID), "/settings", map[string]interface{}{"theme": "dark"})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["theme"] != "dark" {
		t.Errorf("expected theme=dark, got %v", body["theme"])
	}
}

func TestUpdateSettings_MultipleFields(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "upd_multi_"+uid, "upd_multi_"+uid+"@x.com")
	t.Cleanup(func() {
		testDB.Exec(context.Background(), "DELETE FROM user_settings WHERE user_id=$1", userID)
	})

	w := doPatch(t, newRouter(userID), "/settings", map[string]interface{}{
		"sound_enabled":        false,
		"notifications_enabled": true,
		"show_read_receipts":   false,
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["sound_enabled"] != false {
		t.Errorf("expected sound_enabled=false, got %v", body["sound_enabled"])
	}
	if body["notifications_enabled"] != true {
		t.Errorf("expected notifications_enabled=true, got %v", body["notifications_enabled"])
	}
}

func TestUpdateSettings_NothingToUpdate(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "upd_none_"+uid, "upd_none_"+uid+"@x.com")
	t.Cleanup(func() {
		testDB.Exec(context.Background(), "DELETE FROM user_settings WHERE user_id=$1", userID)
	})

	// Body with only unknown keys — no allowed fields present.
	w := doPatch(t, newRouter(userID), "/settings", map[string]interface{}{"unknown_field": "x"})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

// ── POST /settings/change-password ───────────────────────────────────────────

func TestChangePassword_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := seedUserWithPassword(t, "chpw_ok_"+uid, "chpw_ok_"+uid+"@x.com", "oldpassword")

	w := doPost(t, newRouter(userID), "/settings/change-password", map[string]string{
		"current_password": "oldpassword",
		"new_password":     "newpassword123",
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["message"] == nil {
		t.Error("expected message in response")
	}

	// Verify the new hash works.
	var hash string
	testDB.QueryRow(context.Background(), "SELECT password_hash FROM users WHERE id=$1", userID).Scan(&hash)
	if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte("newpassword123")); err != nil {
		t.Error("new password hash does not match the supplied new password")
	}
}

func TestChangePassword_WrongCurrentPassword(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := seedUserWithPassword(t, "chpw_wrong_"+uid, "chpw_wrong_"+uid+"@x.com", "correct")

	w := doPost(t, newRouter(userID), "/settings/change-password", map[string]string{
		"current_password": "incorrect",
		"new_password":     "newpassword123",
	})
	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected 401, got %d: %s", w.Code, w.Body.String())
	}
}

func TestChangePassword_ShortNewPassword(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := seedUserWithPassword(t, "chpw_short_"+uid, "chpw_short_"+uid+"@x.com", "correct")

	w := doPost(t, newRouter(userID), "/settings/change-password", map[string]string{
		"current_password": "correct",
		"new_password":     "12345", // < 6 chars
	})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

// ── POST /settings/fcm ────────────────────────────────────────────────────────

func TestSaveFcmToken_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "fcm_"+uid, "fcm_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/settings/fcm", map[string]string{"token": "fcm-token-abc123"})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	// Verify token was persisted.
	var token string
	testDB.QueryRow(context.Background(), "SELECT COALESCE(fcm_token,'') FROM users WHERE id=$1", userID).Scan(&token)
	if token != "fcm-token-abc123" {
		t.Errorf("expected fcm_token=fcm-token-abc123 in DB, got %q", token)
	}
}

func TestSaveFcmToken_MissingToken(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "fcm_miss_"+uid, "fcm_miss_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/settings/fcm", map[string]string{})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}
