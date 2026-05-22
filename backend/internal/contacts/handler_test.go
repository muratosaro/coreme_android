package contacts_test

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

	"github.com/muratosaro/coreme/api/internal/contacts"
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

func newRouter(userID string) *gin.Engine {
	r := gin.New()
	r.Use(func(c *gin.Context) { c.Set("userId", userID); c.Next() })
	contacts.RegisterRoutes(r.Group("/contacts"), testDB)
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

func doDelete(t *testing.T, r *gin.Engine, path string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodDelete, path, nil)
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

// ── GET /contacts ─────────────────────────────────────────────────────────────

func TestGetContacts_Empty(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "gc_empty_"+uid, "gc_empty_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/contacts")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	var result []interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) != 0 {
		t.Errorf("expected empty contacts list, got %d items", len(result))
	}
}

func TestGetContacts_ReturnsContacts(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	ownerID := testutil.SeedUser(t, testDB, "owner_"+uid, "owner_"+uid+"@x.com")
	contactID := testutil.SeedUser(t, testDB, "contact_"+uid, "contact_"+uid+"@x.com")

	// Add contact directly in DB.
	testDB.Exec(context.Background(),
		"INSERT INTO contacts (user_id, contact_id) VALUES ($1,$2)", ownerID, contactID)
	t.Cleanup(func() {
		testDB.Exec(context.Background(),
			"DELETE FROM contacts WHERE user_id=$1 AND contact_id=$2", ownerID, contactID)
	})

	w := doGet(t, newRouter(ownerID), "/contacts")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var result []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) != 1 {
		t.Errorf("expected 1 contact, got %d", len(result))
	}
	if result[0]["id"] != contactID {
		t.Errorf("expected contact id=%s, got %v", contactID, result[0]["id"])
	}
}

// ── POST /contacts ────────────────────────────────────────────────────────────

func TestAddContact_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	ownerID := testutil.SeedUser(t, testDB, "add_owner_"+uid, "add_owner_"+uid+"@x.com")
	contactID := testutil.SeedUser(t, testDB, "add_contact_"+uid, "add_contact_"+uid+"@x.com")

	w := doPost(t, newRouter(ownerID), "/contacts", map[string]string{"contact_id": contactID})
	t.Cleanup(func() {
		testDB.Exec(context.Background(),
			"DELETE FROM contacts WHERE user_id=$1 AND contact_id=$2", ownerID, contactID)
	})

	if w.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] != contactID {
		t.Errorf("expected contact id=%s in response, got %v", contactID, body["id"])
	}
	if body["username"] == nil {
		t.Error("expected username in response")
	}
}

func TestAddContact_WithNickname(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	ownerID := testutil.SeedUser(t, testDB, "nick_owner_"+uid, "nick_owner_"+uid+"@x.com")
	contactID := testutil.SeedUser(t, testDB, "nick_contact_"+uid, "nick_contact_"+uid+"@x.com")

	w := doPost(t, newRouter(ownerID), "/contacts", map[string]interface{}{
		"contact_id": contactID,
		"nickname":   "My Friend",
	})
	t.Cleanup(func() {
		testDB.Exec(context.Background(),
			"DELETE FROM contacts WHERE user_id=$1 AND contact_id=$2", ownerID, contactID)
	})

	if w.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["nickname"] != "My Friend" {
		t.Errorf("expected nickname=My Friend, got %v", body["nickname"])
	}
}

func TestAddContact_CannotAddSelf(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "self_"+uid, "self_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/contacts", map[string]string{"contact_id": userID})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

func TestAddContact_MissingContactID(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "miss_"+uid, "miss_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/contacts", map[string]string{})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
}

// ── PATCH /contacts/:id ───────────────────────────────────────────────────────

func TestUpdateNickname_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	ownerID := testutil.SeedUser(t, testDB, "nick_upd_"+uid, "nick_upd_"+uid+"@x.com")
	contactID := testutil.SeedUser(t, testDB, "nick_trgt_"+uid, "nick_trgt_"+uid+"@x.com")

	testDB.Exec(context.Background(),
		"INSERT INTO contacts (user_id, contact_id) VALUES ($1,$2)", ownerID, contactID)
	t.Cleanup(func() {
		testDB.Exec(context.Background(),
			"DELETE FROM contacts WHERE user_id=$1 AND contact_id=$2", ownerID, contactID)
	})

	w := doPatch(t, newRouter(ownerID), "/contacts/"+contactID, map[string]string{"nickname": "BFF"})
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	// Verify nickname was persisted.
	var nickname string
	testDB.QueryRow(context.Background(),
		"SELECT COALESCE(nickname,'') FROM contacts WHERE user_id=$1 AND contact_id=$2", ownerID, contactID,
	).Scan(&nickname)
	if nickname != "BFF" {
		t.Errorf("expected nickname=BFF in DB, got %q", nickname)
	}
}

// ── DELETE /contacts/:id ──────────────────────────────────────────────────────

func TestRemoveContact_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	ownerID := testutil.SeedUser(t, testDB, "rm_owner_"+uid, "rm_owner_"+uid+"@x.com")
	contactID := testutil.SeedUser(t, testDB, "rm_contact_"+uid, "rm_contact_"+uid+"@x.com")

	testDB.Exec(context.Background(),
		"INSERT INTO contacts (user_id, contact_id) VALUES ($1,$2)", ownerID, contactID)

	w := doDelete(t, newRouter(ownerID), "/contacts/"+contactID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM contacts WHERE user_id=$1 AND contact_id=$2", ownerID, contactID,
	).Scan(&count)
	if count != 0 {
		t.Error("contact was not deleted from DB")
	}
}

func TestRemoveContact_NonExistentIsNoop(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "rm_noop_"+uid, "rm_noop_"+uid+"@x.com")

	// Deleting a contact that was never added should still return 200.
	w := doDelete(t, newRouter(userID), "/contacts/00000000-0000-0000-0000-000000000000")
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", w.Code)
	}
}
