package auth_test

import (
	"context"
	"encoding/json"
	"strings"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/muratosaro/coreme/api/internal/testutil"
)

// signHS256WithSecret creates an HS256 JWT signed with an explicit secret.
func signHS256WithSecret(userID, secret string, ttl time.Duration) string {
	tok, _ := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": userID,
		"exp":    time.Now().Add(ttl).Unix(),
	}).SignedString([]byte(secret))
	return tok
}

// ── Password / credential security ───────────────────────────────────────────

// TestRegister_PasswordHashNotInResponse ensures the DB password_hash is never
// returned in the registration response.
func TestRegister_PasswordHashNotInResponse(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	w := post(t, newRouter(), "/auth/register", map[string]string{
		"username":     "sec_pwnr_" + uid,
		"display_name": "PwNR",
		"email":        "sec_pwnr_" + uid + "@example.com",
		"password":     "supersecret123",
	})
	if w.Code != 201 {
		t.Fatalf("setup failed: %d %s", w.Code, w.Body.String())
	}

	raw := w.Body.String()
	if strings.Contains(strings.ToLower(raw), "password_hash") {
		t.Errorf("response must not expose password_hash, got: %s", raw)
	}

	var id string
	testDB.QueryRow(context.Background(),
		"SELECT id FROM users WHERE username=$1", "sec_pwnr_"+uid).Scan(&id)
	if id != "" {
		t.Cleanup(func() { testutil.DeleteUser(testDB, id) })
	}
}

// TestRegister_UsernameMaxLength verifies boundary enforcement on username length.
func TestRegister_UsernameMaxLength(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	r := newRouter()

	// Exactly 50 characters — must succeed (max=50).
	name50 := strings.Repeat("a", 45) + uid[:5]
	w := post(t, r, "/auth/register", map[string]string{
		"username":     name50,
		"display_name": "Boundary",
		"email":        "boundary50_" + uid + "@example.com",
		"password":     "password123",
	})
	if w.Code == 201 {
		var id string
		testDB.QueryRow(context.Background(), "SELECT id FROM users WHERE username=$1", name50).Scan(&id)
		t.Cleanup(func() { testutil.DeleteUser(testDB, id) })
	}

	// 51 characters — must be rejected (max=50).
	name51 := strings.Repeat("b", 51)
	w2 := post(t, r, "/auth/register", map[string]string{
		"username":     name51,
		"display_name": "TooLong",
		"email":        "boundary51_" + uid + "@example.com",
		"password":     "password123",
	})
	if w2.Code != 422 {
		t.Errorf("51-char username: expected 422, got %d: %s", w2.Code, w2.Body.String())
	}
}

// TestLogin_ConsistentErrorForBothFailureModes verifies that wrong username and
// wrong password return the same HTTP status and message, preventing username
// enumeration attacks.
func TestLogin_ConsistentErrorForBothFailureModes(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	seedUser(t, "sec_enum_"+uid, "sec_enum_"+uid+"@example.com", "correct_password")

	r := newRouter()

	wBadUser := post(t, r, "/auth/login", map[string]string{
		"username": "no_such_user_zzz_" + uid,
		"password": "anything",
	})
	wBadPass := post(t, r, "/auth/login", map[string]string{
		"username": "sec_enum_" + uid,
		"password": "wrong_password",
	})

	if wBadUser.Code != 401 {
		t.Errorf("wrong username: expected 401, got %d", wBadUser.Code)
	}
	if wBadPass.Code != 401 {
		t.Errorf("wrong password: expected 401, got %d", wBadPass.Code)
	}
	if wBadUser.Code != wBadPass.Code {
		t.Errorf("username enumeration risk: wrong-username=%d vs wrong-password=%d",
			wBadUser.Code, wBadPass.Code)
	}

	var bodyBadUser, bodyBadPass map[string]interface{}
	json.NewDecoder(wBadUser.Body).Decode(&bodyBadUser)
	json.NewDecoder(wBadPass.Body).Decode(&bodyBadPass)
	if bodyBadUser["message"] != bodyBadPass["message"] {
		t.Errorf("username enumeration: messages differ: %q vs %q",
			bodyBadUser["message"], bodyBadPass["message"])
	}
}

// TestLogout_RefreshTokenRejectedAfterLogout verifies that a token used for
// logout cannot subsequently be used to obtain a new access token.
func TestLogout_RefreshTokenRejectedAfterLogout(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := seedUser(t, "sec_lgout_"+uid, "sec_lgout_"+uid+"@example.com", "password123")

	refreshToken := makeRefreshToken(userID, 30*24*time.Hour)
	testutil.CreateSession(t, testDB, userID, refreshToken, time.Now().UTC().Add(30*24*time.Hour))

	r := newRouter()

	wLogout := post(t, r, "/auth/logout", map[string]string{"refreshToken": refreshToken})
	if wLogout.Code != 200 {
		t.Fatalf("logout: expected 200, got %d %s", wLogout.Code, wLogout.Body.String())
	}

	// Revoked token must be rejected.
	wRefresh := post(t, r, "/auth/refresh", map[string]string{"refreshToken": refreshToken})
	if wRefresh.Code != 401 {
		t.Errorf("post-logout token reuse: expected 401, got %d — token reuse attack possible!",
			wRefresh.Code)
	}
}

// TestRefresh_AccessTokenSecretRejectedAsRefreshToken verifies that a token
// signed with JWT_SECRET (access secret) is not accepted by /auth/refresh
// which expects JWT_REFRESH_SECRET, preventing token-type confusion attacks.
func TestRefresh_AccessTokenSecretRejectedAsRefreshToken(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := seedUser(t, "sec_toktype_"+uid, "sec_toktype_"+uid+"@example.com", "password123")

	// Token signed with the ACCESS secret, not the refresh secret.
	wrongToken := signHS256WithSecret(userID, "test-secret-32-chars-xxxxxxxxxx", 30*24*time.Hour)
	testutil.CreateSession(t, testDB, userID, wrongToken, time.Now().UTC().Add(30*24*time.Hour))

	w := post(t, newRouter(), "/auth/refresh", map[string]string{"refreshToken": wrongToken})
	if w.Code != 401 {
		t.Errorf("access-token secret used as refresh secret: expected 401, got %d — confusion attack possible!", w.Code)
	}
}
