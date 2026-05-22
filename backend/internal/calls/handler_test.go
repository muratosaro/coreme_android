package calls_test

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/muratosaro/coreme/api/internal/calls"
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
	calls.RegisterRoutes(r.Group("/calls"), testDB)
	return r
}

func doGet(t *testing.T, r *gin.Engine, path string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodGet, path, nil)
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

// ── GET /calls/history ────────────────────────────────────────────────────────

func TestGetCallHistory_Empty(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "ch_empty_"+uid, "ch_empty_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/calls/history")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	var result []interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) != 0 {
		t.Errorf("expected empty history, got %d records", len(result))
	}
}

func TestGetCallHistory_ReturnsBothCallerAndCallee(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	callerID := testutil.SeedUser(t, testDB, "caller_"+uid, "caller_"+uid+"@x.com")
	calleeID := testutil.SeedUser(t, testDB, "callee_"+uid, "callee_"+uid+"@x.com")

	recordID := testutil.CreateCallRecord(t, testDB, callerID, calleeID)

	// Caller sees the record.
	wCaller := doGet(t, newRouter(callerID), "/calls/history")
	if wCaller.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", wCaller.Code)
	}
	var callerResult []map[string]interface{}
	json.NewDecoder(wCaller.Body).Decode(&callerResult)
	foundByCaller := false
	for _, r := range callerResult {
		if r["id"] == recordID {
			foundByCaller = true
		}
	}
	if !foundByCaller {
		t.Error("caller should see the call record in history")
	}

	// Callee also sees the record.
	wCallee := doGet(t, newRouter(calleeID), "/calls/history")
	if wCallee.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", wCallee.Code)
	}
	var calleeResult []map[string]interface{}
	json.NewDecoder(wCallee.Body).Decode(&calleeResult)
	foundByCallee := false
	for _, r := range calleeResult {
		if r["id"] == recordID {
			foundByCallee = true
		}
	}
	if !foundByCallee {
		t.Error("callee should see the call record in history")
	}
}

func TestGetCallHistory_ReturnsCallerAndCalleeName(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	callerID := testutil.SeedUser(t, testDB, "cn_caller_"+uid, "cn_caller_"+uid+"@x.com")
	calleeID := testutil.SeedUser(t, testDB, "cn_callee_"+uid, "cn_callee_"+uid+"@x.com")
	testutil.CreateCallRecord(t, testDB, callerID, calleeID)

	w := doGet(t, newRouter(callerID), "/calls/history")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var result []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) == 0 {
		t.Fatal("expected at least one record")
	}
	if result[0]["caller_name"] == nil || result[0]["callee_name"] == nil {
		t.Error("expected caller_name and callee_name in response")
	}
}

// ── DELETE /calls/history/:id ─────────────────────────────────────────────────

func TestDeleteCallRecord_AsCaller(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	callerID := testutil.SeedUser(t, testDB, "del_caller_"+uid, "del_caller_"+uid+"@x.com")
	calleeID := testutil.SeedUser(t, testDB, "del_callee_"+uid, "del_callee_"+uid+"@x.com")
	recordID := testutil.CreateCallRecord(t, testDB, callerID, calleeID)

	w := doDelete(t, newRouter(callerID), "/calls/history/"+recordID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM call_history WHERE id=$1", recordID,
	).Scan(&count)
	if count != 0 {
		t.Error("call record was not deleted from DB")
	}
}

func TestDeleteCallRecord_NonOwnerIsNoop(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	callerID := testutil.SeedUser(t, testDB, "nown_caller_"+uid, "nown_caller_"+uid+"@x.com")
	calleeID := testutil.SeedUser(t, testDB, "nown_callee_"+uid, "nown_callee_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "nown_stranger_"+uid, "nown_stranger_"+uid+"@x.com")
	recordID := testutil.CreateCallRecord(t, testDB, callerID, calleeID)

	// Stranger tries to delete — DELETE WHERE caller_id=$2 OR callee_id=$2 won't match.
	w := doDelete(t, newRouter(stranger), "/calls/history/"+recordID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200 (noop), got %d", w.Code)
	}
	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM call_history WHERE id=$1", recordID,
	).Scan(&count)
	if count != 1 {
		t.Error("call record should not be deleted by a stranger")
	}
}
