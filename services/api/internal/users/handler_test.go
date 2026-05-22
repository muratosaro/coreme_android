package users_test

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/muratosaro/coreme/api/internal/testutil"
	"github.com/muratosaro/coreme/api/internal/users"
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

func newRouter(userID string) *gin.Engine {
	r := gin.New()
	r.Use(func(c *gin.Context) { c.Set("userId", userID); c.Next() })
	users.RegisterRoutes(r.Group("/users"), testDB)
	return r
}

func doGet(t *testing.T, r *gin.Engine, path string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodGet, path, nil)
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

// ── GET /users/me ─────────────────────────────────────────────────────────────

func TestGetMe_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "getme_"+uid, "getme_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/users/me")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] != userID {
		t.Errorf("expected id=%s, got %v", userID, body["id"])
	}
	if body["username"] == nil {
		t.Error("expected username in response")
	}
}

func TestGetMe_UserNotFound(t *testing.T) {
	skipIfNoDB(t)
	w := doGet(t, newRouter("00000000-0000-0000-0000-000000000000"), "/users/me")
	if w.Code != http.StatusNotFound {
		t.Errorf("expected 404, got %d: %s", w.Code, w.Body.String())
	}
}

// ── PATCH /users/me ───────────────────────────────────────────────────────────

func TestUpdateMe_DisplayName(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "upd_"+uid, "upd_"+uid+"@x.com")

	w := doPatch(t, newRouter(userID), "/users/me", map[string]string{"display_name": "New Name"})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["display_name"] != "New Name" {
		t.Errorf("expected display_name=New Name, got %v", body["display_name"])
	}
}

func TestUpdateMe_Bio(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "bio_"+uid, "bio_"+uid+"@x.com")

	w := doPatch(t, newRouter(userID), "/users/me", map[string]string{"bio": "My bio"})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["bio"] != "My bio" {
		t.Errorf("expected bio=My bio, got %v", body["bio"])
	}
}

func TestUpdateMe_NothingToUpdate(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "noupd_"+uid, "noupd_"+uid+"@x.com")

	w := doPatch(t, newRouter(userID), "/users/me", map[string]string{})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

func TestUpdateMe_DuplicateUsername(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "ua_"+uid, "ua_"+uid+"@x.com")
	testutil.SeedUser(t, testDB, "ub_"+uid, "ub_"+uid+"@x.com")

	w := doPatch(t, newRouter(userA), "/users/me", map[string]string{"username": "ub_" + uid})
	if w.Code != http.StatusConflict {
		t.Errorf("expected 409, got %d: %s", w.Code, w.Body.String())
	}
}

// ── GET /users/search ─────────────────────────────────────────────────────────

func TestSearchUsers_ReturnsResults(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	searcher := testutil.SeedUser(t, testDB, "srch_"+uid, "srch_"+uid+"@x.com")
	testutil.SeedUser(t, testDB, "findme_"+uid, "findme_"+uid+"@x.com")

	w := doGet(t, newRouter(searcher), "/users/search?q=findme_"+uid)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var results []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&results)
	if len(results) == 0 {
		t.Error("expected at least one search result")
	}
}

func TestSearchUsers_ExcludesSelf(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "self_srch_"+uid, "self_srch_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/users/search?q=self_srch_"+uid)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var results []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&results)
	for _, r := range results {
		if r["id"] == userID {
			t.Error("search must not return the requesting user in results")
		}
	}
}

func TestSearchUsers_ShortQueryReturnsEmpty(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "sq_"+uid, "sq_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/users/search?q=x")
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", w.Code)
	}
	var results []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&results)
	if len(results) != 0 {
		t.Error("expected empty list for query shorter than 2 chars")
	}
}

// ── GET /users/:id ────────────────────────────────────────────────────────────

func TestGetUserById_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	caller := testutil.SeedUser(t, testDB, "caller_"+uid, "caller_"+uid+"@x.com")
	target := testutil.SeedUser(t, testDB, "target_"+uid, "target_"+uid+"@x.com")

	w := doGet(t, newRouter(caller), "/users/"+target)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["id"] != target {
		t.Errorf("expected id=%s, got %v", target, body["id"])
	}
}

func TestGetUserById_NotFound(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	caller := testutil.SeedUser(t, testDB, "caller2_"+uid, "caller2_"+uid+"@x.com")

	w := doGet(t, newRouter(caller), "/users/00000000-0000-0000-0000-000000000000")
	if w.Code != http.StatusNotFound {
		t.Errorf("expected 404, got %d", w.Code)
	}
}

