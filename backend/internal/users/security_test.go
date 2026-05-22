package users_test

import (
	"encoding/json"
	"net/http"
	"testing"

	"github.com/muratosaro/coreme/api/internal/testutil"
)

// TestGetUserById_EmailNotExposed verifies that GET /users/:id (viewing another
// user's profile) does not return the email field. Email is private data that
// must only be visible to the account owner via GET /users/me.
func TestGetUserById_EmailNotExposed(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	caller := testutil.SeedUser(t, testDB, "sec_eu_caller_"+uid, "sec_eu_caller_"+uid+"@x.com")
	target := testutil.SeedUser(t, testDB, "sec_eu_target_"+uid, "sec_eu_target_"+uid+"@x.com")

	w := doGet(t, newRouter(caller), "/users/"+target)
	if w.Code != http.StatusOK {
		t.Fatalf("setup failed: expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var body map[string]interface{}
	json.NewDecoder(w.Body).Decode(&body)
	if _, ok := body["email"]; ok {
		t.Errorf("GET /users/:id must not expose email field, got: %v", body)
	}
}

// TestGetMe_ContainsEmail verifies that GET /users/me (own profile) does
// include the email field, because the owner is entitled to see their own email.
func TestGetMe_ContainsEmail(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	email := "sec_me_" + uid + "@x.com"
	userID := testutil.SeedUser(t, testDB, "sec_me_"+uid, email)

	w := doGet(t, newRouter(userID), "/users/me")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var body map[string]interface{}
	json.NewDecoder(w.Body).Decode(&body)
	if body["email"] == nil {
		t.Error("GET /users/me must include email for the account owner")
	}
	if body["email"] != email {
		t.Errorf("expected email=%s, got %v", email, body["email"])
	}
}

// TestSearchUsers_EmailNotExposed verifies that the search endpoint does not
// leak email addresses for any user in the results.
func TestSearchUsers_EmailNotExposed(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	searcher := testutil.SeedUser(t, testDB, "sec_srch_s_"+uid, "sec_srch_s_"+uid+"@x.com")
	testutil.SeedUser(t, testDB, "sec_srch_t_"+uid, "sec_srch_t_"+uid+"@x.com")

	w := doGet(t, newRouter(searcher), "/users/search?q=sec_srch_t_"+uid)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}

	var results []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&results)
	if len(results) == 0 {
		t.Skip("no search results — cannot verify email absence")
	}
	for _, r := range results {
		if _, ok := r["email"]; ok {
			t.Errorf("GET /users/search must not expose email field, got result: %v", r)
		}
	}
}

// TestGetUserById_CannotAccessOwnEmailViaPublicEndpoint verifies that even when
// a user fetches their own profile via GET /users/:id (the public endpoint),
// the email field is still absent — the private /users/me endpoint is the only
// way to retrieve email.
func TestGetUserById_CannotAccessOwnEmailViaPublicEndpoint(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "sec_self_pub_"+uid, "sec_self_pub_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/users/"+userID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var body map[string]interface{}
	json.NewDecoder(w.Body).Decode(&body)
	if _, ok := body["email"]; ok {
		t.Errorf("GET /users/:id (self) must not expose email — use /users/me instead, got: %v", body)
	}
}
