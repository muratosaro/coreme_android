package channels_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/muratosaro/coreme/api/internal/channels"
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
	channels.RegisterRoutes(r.Group("/channels"), testDB)
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

// ── GET /channels ─────────────────────────────────────────────────────────────

func TestGetChannels_Empty(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "ch_empty_"+uid, "ch_empty_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/channels")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	var result []interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) != 0 {
		t.Errorf("expected empty list, got %d", len(result))
	}
}

func TestGetChannels_ReturnsOwnedChannels(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "chown_"+uid, "chown_"+uid+"@x.com")
	testutil.CreateChannel(t, testDB, "MyChan "+uid, userID)

	w := doGet(t, newRouter(userID), "/channels")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var result []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) == 0 {
		t.Error("expected at least one channel in response")
	}
}

// ── POST /channels ────────────────────────────────────────────────────────────

func TestCreateChannel_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "cch_"+uid, "cch_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/channels", map[string]interface{}{
		"name": "New Channel " + uid,
	})
	if w.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] == nil {
		t.Error("expected id in response")
	}
	if body["role"] != "superadmin" {
		t.Errorf("creator should be superadmin, got %v", body["role"])
	}
	// Cleanup channel created by the handler.
	t.Cleanup(func() {
		if id, ok := body["id"].(string); ok {
			testDB.Exec(context.Background(), "DELETE FROM chat_members WHERE chat_id=$1", id)
			testDB.Exec(context.Background(), "DELETE FROM chats WHERE id=$1", id)
		}
	})
}

func TestCreateChannel_MissingName(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "cch_miss_"+uid, "cch_miss_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/channels", map[string]interface{}{})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

func TestCreateChannel_DuplicateUsernameHandle(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "cch_dup_a_"+uid, "cch_dup_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "cch_dup_b_"+uid, "cch_dup_b_"+uid+"@x.com")
	handle := "handle_" + uid

	// First channel takes the handle.
	w1 := doPost(t, newRouter(userA), "/channels", map[string]interface{}{
		"name":            "Chan A " + uid,
		"username_handle": handle,
	})
	if w1.Code != http.StatusCreated {
		t.Fatalf("setup: expected 201, got %d: %s", w1.Code, w1.Body.String())
	}
	var b1 map[string]interface{}
	json.NewDecoder(w1.Body).Decode(&b1)
	t.Cleanup(func() {
		if b1 != nil {
			if id, ok := b1["id"].(string); ok {
				testDB.Exec(context.Background(), "DELETE FROM chat_members WHERE chat_id=$1", id)
				testDB.Exec(context.Background(), "DELETE FROM chats WHERE id=$1", id)
			}
		}
	})

	// Second channel with the same handle should conflict.
	w2 := doPost(t, newRouter(userB), "/channels", map[string]interface{}{
		"name":            "Chan B " + uid,
		"username_handle": handle,
	})
	if w2.Code != http.StatusConflict {
		t.Errorf("expected 409, got %d: %s", w2.Code, w2.Body.String())
	}
}

// ── GET /channels/:id/scheduled ───────────────────────────────────────────────

func TestGetScheduledPosts_ForbiddenForMember(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sp_owner_"+uid, "sp_owner_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "sp_member_"+uid, "sp_member_"+uid+"@x.com")
	chanID := testutil.CreateChannel(t, testDB, "SpChan "+uid, owner)
	testutil.AddChatMember(t, testDB, chanID, member, "member")

	w := doGet(t, newRouter(member), "/channels/"+chanID+"/scheduled")
	if w.Code != http.StatusForbidden {
		t.Errorf("expected 403, got %d: %s", w.Code, w.Body.String())
	}
}

func TestGetScheduledPosts_SuccessForAdmin(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sp_adm_"+uid, "sp_adm_"+uid+"@x.com")
	chanID := testutil.CreateChannel(t, testDB, "SpAdmChan "+uid, owner)

	w := doGet(t, newRouter(owner), "/channels/"+chanID+"/scheduled")
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
}

// ── POST /channels/:id/scheduled ─────────────────────────────────────────────

func TestCreateScheduledPost_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "csp_owner_"+uid, "csp_owner_"+uid+"@x.com")
	chanID := testutil.CreateChannel(t, testDB, "CspChan "+uid, owner)

	future := time.Now().Add(24 * time.Hour).UTC().Format(time.RFC3339)
	w := doPost(t, newRouter(owner), "/channels/"+chanID+"/scheduled", map[string]interface{}{
		"content":      "Scheduled hello",
		"type":         "text",
		"scheduled_at": future,
	})
	if w.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] == nil {
		t.Error("expected id in response")
	}
	if body["content"] != "Scheduled hello" {
		t.Errorf("expected content=Scheduled hello, got %v", body["content"])
	}
}

func TestCreateScheduledPost_ForbiddenForMember(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "csp_own2_"+uid, "csp_own2_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "csp_mem_"+uid, "csp_mem_"+uid+"@x.com")
	chanID := testutil.CreateChannel(t, testDB, "CspChan2 "+uid, owner)
	testutil.AddChatMember(t, testDB, chanID, member, "member")

	future := time.Now().Add(24 * time.Hour).UTC().Format(time.RFC3339)
	w := doPost(t, newRouter(member), "/channels/"+chanID+"/scheduled", map[string]interface{}{
		"content":      "Unauthorized post",
		"scheduled_at": future,
	})
	if w.Code != http.StatusForbidden {
		t.Errorf("expected 403, got %d: %s", w.Code, w.Body.String())
	}
}

// ── DELETE /channels/:id/scheduled/:postId ────────────────────────────────────

func TestDeleteScheduledPost_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "dsp_owner_"+uid, "dsp_owner_"+uid+"@x.com")
	chanID := testutil.CreateChannel(t, testDB, "DspChan "+uid, owner)

	// Create a post via the handler.
	future := time.Now().Add(24 * time.Hour).UTC().Format(time.RFC3339)
	wCreate := doPost(t, newRouter(owner), "/channels/"+chanID+"/scheduled", map[string]interface{}{
		"content":      "To be deleted",
		"scheduled_at": future,
	})
	if wCreate.Code != http.StatusCreated {
		t.Fatalf("setup: expected 201, got %d: %s", wCreate.Code, wCreate.Body.String())
	}
	var created map[string]interface{}
	json.NewDecoder(wCreate.Body).Decode(&created)
	postID, _ := created["id"].(string)

	w := doDelete(t, newRouter(owner), "/channels/"+chanID+"/scheduled/"+postID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM scheduled_posts WHERE id=$1", postID,
	).Scan(&count)
	if count != 0 {
		t.Error("scheduled post was not deleted from DB")
	}
}
