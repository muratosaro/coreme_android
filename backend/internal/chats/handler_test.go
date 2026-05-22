package chats_test

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

	"github.com/muratosaro/coreme/api/internal/chats"
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
	chats.RegisterRoutes(r.Group("/chats"), testDB)
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

// ── GET /chats ────────────────────────────────────────────────────────────────

func TestGetChats_Empty(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "gc_empty_"+uid, "gc_empty_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/chats")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	var result []interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) != 0 {
		t.Errorf("expected empty chat list, got %d", len(result))
	}
}

func TestGetChats_ReturnsUserChats(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "gc_a_"+uid, "gc_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "gc_b_"+uid, "gc_b_"+uid+"@x.com")
	testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doGet(t, newRouter(userA), "/chats")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var result []interface{}
	json.NewDecoder(w.Body).Decode(&result)
	if len(result) == 0 {
		t.Error("expected at least one chat in response")
	}
}

// ── POST /chats (direct) ──────────────────────────────────────────────────────

func TestCreateDirectChat_NewChat(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "cd_a_"+uid, "cd_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "cd_b_"+uid, "cd_b_"+uid+"@x.com")

	w := doPost(t, newRouter(userA), "/chats", map[string]interface{}{
		"type":       "direct",
		"member_ids": []string{userB},
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] == nil {
		t.Error("expected id in response")
	}
	if body["type"] != "direct" {
		t.Errorf("expected type=direct, got %v", body["type"])
	}
	t.Cleanup(func() {
		if id, ok := body["id"].(string); ok {
			testDB.Exec(context.Background(), "DELETE FROM chat_members WHERE chat_id=$1", id)
			testDB.Exec(context.Background(), "DELETE FROM chats WHERE id=$1", id)
		}
	})
}

func TestCreateDirectChat_ReturnsExistingChat(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "cde_a_"+uid, "cde_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "cde_b_"+uid, "cde_b_"+uid+"@x.com")
	existingID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doPost(t, newRouter(userA), "/chats", map[string]interface{}{
		"type":       "direct",
		"member_ids": []string{userB},
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["id"] != existingID {
		t.Errorf("expected existing chat id=%s, got %v", existingID, body["id"])
	}
}

func TestCreateDirectChat_MissingFields(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "cd_miss_"+uid, "cd_miss_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/chats", map[string]interface{}{})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d: %s", w.Code, w.Body.String())
	}
}

// ── POST /chats/group ─────────────────────────────────────────────────────────

func TestCreateGroup_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "cg_a_"+uid, "cg_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "cg_b_"+uid, "cg_b_"+uid+"@x.com")

	w := doPost(t, newRouter(userA), "/chats/group", map[string]interface{}{
		"name":       "Test Group " + uid,
		"member_ids": []string{userB},
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] == nil {
		t.Error("expected id in response")
	}
	if body["type"] != "group" {
		t.Errorf("expected type=group, got %v", body["type"])
	}
	t.Cleanup(func() {
		if id, ok := body["id"].(string); ok {
			testDB.Exec(context.Background(), "DELETE FROM chat_members WHERE chat_id=$1", id)
			testDB.Exec(context.Background(), "DELETE FROM chats WHERE id=$1", id)
		}
	})
}

func TestCreateGroup_MissingName(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "cg_miss_"+uid, "cg_miss_"+uid+"@x.com")

	w := doPost(t, newRouter(userID), "/chats/group", map[string]interface{}{})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d: %s", w.Code, w.Body.String())
	}
}

// ── GET /chats/:id ────────────────────────────────────────────────────────────

func TestGetChatById_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "gcid_a_"+uid, "gcid_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "gcid_b_"+uid, "gcid_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doGet(t, newRouter(userA), "/chats/"+chatID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["id"] != chatID {
		t.Errorf("expected id=%s, got %v", chatID, body["id"])
	}
}

func TestGetChatById_NotFound(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userID := testutil.SeedUser(t, testDB, "gcid_nf_"+uid, "gcid_nf_"+uid+"@x.com")

	w := doGet(t, newRouter(userID), "/chats/00000000-0000-0000-0000-000000000000")
	if w.Code != http.StatusNotFound {
		t.Errorf("expected 404, got %d", w.Code)
	}
}

// ── PATCH /chats/:id ─────────────────────────────────────────────────────────

func TestUpdateGroup_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "ug_owner_"+uid, "ug_owner_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "Original "+uid, owner)

	w := doPatch(t, newRouter(owner), "/chats/"+chatID, map[string]interface{}{
		"name":        "Updated Name " + uid,
		"description": "New desc",
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	if body := jsonBody(t, w); body["name"] != "Updated Name "+uid {
		t.Errorf("expected name=Updated Name %s, got %v", uid, body["name"])
	}
}

func TestUpdateGroup_NothingToUpdate(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "ug_none_"+uid, "ug_none_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "G "+uid, owner)

	w := doPatch(t, newRouter(owner), "/chats/"+chatID, map[string]interface{}{})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
}

// ── DELETE /chats/:id/leave ───────────────────────────────────────────────────

func TestLeaveGroup_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "lg_owner_"+uid, "lg_owner_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "lg_mem_"+uid, "lg_mem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "LG "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doDelete(t, newRouter(member), "/chats/"+chatID+"/leave")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatID, member,
	).Scan(&count)
	if count != 0 {
		t.Error("user should have been removed from chat_members")
	}
}

// ── GET /chats/:id/members ────────────────────────────────────────────────────

func TestGetGroupMembers_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "gm_owner_"+uid, "gm_owner_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "gm_mem_"+uid, "gm_mem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "GM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doGet(t, newRouter(owner), "/chats/"+chatID+"/members")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	var members []map[string]interface{}
	json.NewDecoder(w.Body).Decode(&members)
	if len(members) != 2 {
		t.Errorf("expected 2 members, got %d", len(members))
	}
}

// ── POST /chats/:id/members ───────────────────────────────────────────────────

func TestAddMember_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "am_owner_"+uid, "am_owner_"+uid+"@x.com")
	newMember := testutil.SeedUser(t, testDB, "am_new_"+uid, "am_new_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "AM "+uid, owner)

	w := doPost(t, newRouter(owner), "/chats/"+chatID+"/members", map[string]string{"user_id": newMember})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatID, newMember,
	).Scan(&count)
	if count != 1 {
		t.Error("new member was not added to chat_members")
	}
}

func TestAddMember_MissingUserID(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "am_miss_"+uid, "am_miss_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "AM2 "+uid, owner)

	w := doPost(t, newRouter(owner), "/chats/"+chatID+"/members", map[string]string{})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
}

// ── DELETE /chats/:id/members/:userId ────────────────────────────────────────

func TestRemoveMember_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "rm_owner_"+uid, "rm_owner_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "rm_mem_"+uid, "rm_mem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "RM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doDelete(t, newRouter(owner), "/chats/"+chatID+"/members/"+member)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatID, member,
	).Scan(&count)
	if count != 0 {
		t.Error("member was not removed from chat_members")
	}
}

// ── PATCH /chats/:id/members/:userId ─────────────────────────────────────────

func TestUpdateMemberRole_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "umr_owner_"+uid, "umr_owner_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "umr_mem_"+uid, "umr_mem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "UMR "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doPatch(t, newRouter(owner), "/chats/"+chatID+"/members/"+member, map[string]string{"role": "admin"})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var role string
	testDB.QueryRow(context.Background(),
		"SELECT role FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatID, member,
	).Scan(&role)
	if role != "admin" {
		t.Errorf("expected role=admin in DB, got %q", role)
	}
}

func TestUpdateMemberRole_InvalidRole(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "umr_inv_"+uid, "umr_inv_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "umr_invmem_"+uid, "umr_invmem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "UMR2 "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doPatch(t, newRouter(owner), "/chats/"+chatID+"/members/"+member, map[string]string{"role": "superadmin"})
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

// ── GET /chats/:id/messages ───────────────────────────────────────────────────

func TestGetMessages_EmptyChat(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "gmes_a_"+uid, "gmes_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "gmes_b_"+uid, "gmes_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doGet(t, newRouter(userA), "/chats/"+chatID+"/messages")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	var msgs []interface{}
	json.NewDecoder(w.Body).Decode(&msgs)
	if len(msgs) != 0 {
		t.Errorf("expected empty messages list, got %d", len(msgs))
	}
}

func TestGetMessages_ReturnsPaginatedMessages(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "gmsp_a_"+uid, "gmsp_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "gmsp_b_"+uid, "gmsp_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	testutil.CreateMessage(t, testDB, chatID, userA, "Hello")
	testutil.CreateMessage(t, testDB, chatID, userB, "World")

	w := doGet(t, newRouter(userA), "/chats/"+chatID+"/messages?limit=1")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}
	var msgs []interface{}
	json.NewDecoder(w.Body).Decode(&msgs)
	if len(msgs) != 1 {
		t.Errorf("expected 1 message (limit=1), got %d", len(msgs))
	}
}

// ── POST /chats/:id/messages ──────────────────────────────────────────────────

func TestSendMessage_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "sm_a_"+uid, "sm_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "sm_b_"+uid, "sm_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doPost(t, newRouter(userA), "/chats/"+chatID+"/messages", map[string]string{
		"content": "Test message",
	})
	if w.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["id"] == nil {
		t.Error("expected message id in response")
	}
	if body["content"] != "Test message" {
		t.Errorf("expected content=Test message, got %v", body["content"])
	}
	if body["type"] != "text" {
		t.Errorf("expected default type=text, got %v", body["type"])
	}
}

func TestSendMessage_MissingContent(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "sm_miss_a_"+uid, "sm_miss_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "sm_miss_b_"+uid, "sm_miss_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doPost(t, newRouter(userA), "/chats/"+chatID+"/messages", map[string]string{})
	if w.Code != http.StatusUnprocessableEntity {
		t.Errorf("expected 422, got %d", w.Code)
	}
}

// ── PATCH /chats/:id/messages/:msgId ─────────────────────────────────────────

func TestEditMessage_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "em_a_"+uid, "em_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "em_b_"+uid, "em_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userA, "Original")

	w := doPatch(t, newRouter(userA), "/chats/"+chatID+"/messages/"+msgID, map[string]string{
		"content": "Edited",
	})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["content"] != "Edited" {
		t.Errorf("expected content=Edited, got %v", body["content"])
	}
	if body["is_edited"] != true {
		t.Errorf("expected is_edited=true, got %v", body["is_edited"])
	}
}

func TestEditMessage_NotYourMessage(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "em_nya_"+uid, "em_nya_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "em_nyb_"+uid, "em_nyb_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userA, "Sender A's msg")

	// userB tries to edit userA's message.
	w := doPatch(t, newRouter(userB), "/chats/"+chatID+"/messages/"+msgID, map[string]string{
		"content": "Hijacked",
	})
	if w.Code != http.StatusForbidden {
		t.Errorf("expected 403, got %d: %s", w.Code, w.Body.String())
	}
}

func TestEditMessage_NotFound(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "em_nf_"+uid, "em_nf_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "em_nfb_"+uid, "em_nfb_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doPatch(t, newRouter(userA), "/chats/"+chatID+"/messages/00000000-0000-0000-0000-000000000000",
		map[string]string{"content": "Ghost"})
	if w.Code != http.StatusNotFound {
		t.Errorf("expected 404, got %d", w.Code)
	}
}

// ── DELETE /chats/:id/messages/:msgId ────────────────────────────────────────

func TestDeleteMessage_ForMe(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "dm_me_a_"+uid, "dm_me_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "dm_me_b_"+uid, "dm_me_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userB, "B's message")

	// userA deletes for themselves only (forAll=false / default).
	w := doDelete(t, newRouter(userA), "/chats/"+chatID+"/messages/"+msgID)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	// Message still exists in DB — only a deletion record was created.
	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM messages WHERE id=$1", msgID,
	).Scan(&count)
	if count != 1 {
		t.Error("message should still exist in DB after forMe deletion")
	}

	var deletionCount int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM message_deletions WHERE message_id=$1 AND user_id=$2", msgID, userA,
	).Scan(&deletionCount)
	if deletionCount != 1 {
		t.Error("expected a message_deletions row for the requesting user")
	}
}

func TestDeleteMessage_ForAll_BySender(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "dm_all_a_"+uid, "dm_all_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "dm_all_b_"+uid, "dm_all_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userA, "A's message")

	w := doDelete(t, newRouter(userA), "/chats/"+chatID+"/messages/"+msgID+"?forAll=true")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM messages WHERE id=$1", msgID,
	).Scan(&count)
	if count != 0 {
		t.Error("message should be deleted from DB after forAll deletion by sender")
	}
}

func TestDeleteMessage_ForAll_ForbiddenForMember(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "dm_fb_own_"+uid, "dm_fb_own_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "dm_fb_mem_"+uid, "dm_fb_mem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "DelFB "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")
	msgID := testutil.CreateMessage(t, testDB, chatID, owner, "Owner's message")

	// member (not the sender, not an admin) tries to delete for all.
	w := doDelete(t, newRouter(member), "/chats/"+chatID+"/messages/"+msgID+"?forAll=true")
	if w.Code != http.StatusForbidden {
		t.Errorf("expected 403, got %d: %s", w.Code, w.Body.String())
	}
}

// ── POST /chats/:id/messages/:msgId/pin ───────────────────────────────────────

func TestPinMessage_DirectChat_ForAll(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "pin_a_"+uid, "pin_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "pin_b_"+uid, "pin_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userA, "Pin this")

	w := doPost(t, newRouter(userA), "/chats/"+chatID+"/messages/"+msgID+"/pin",
		map[string]bool{"forAll": true})
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	t.Cleanup(func() {
		testDB.Exec(context.Background(), "DELETE FROM chat_pins WHERE chat_id=$1", chatID)
	})

	body := jsonBody(t, w)
	if body["messageId"] != msgID {
		t.Errorf("expected messageId=%s, got %v", msgID, body["messageId"])
	}
}

func TestPinMessage_ForbiddenForGroupMember(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "pin_gown_"+uid, "pin_gown_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "pin_gmem_"+uid, "pin_gmem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "PinG "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")
	msgID := testutil.CreateMessage(t, testDB, chatID, owner, "Admin's message")

	w := doPost(t, newRouter(member), "/chats/"+chatID+"/messages/"+msgID+"/pin",
		map[string]bool{"forAll": true})
	if w.Code != http.StatusForbidden {
		t.Errorf("expected 403, got %d: %s", w.Code, w.Body.String())
	}
}

func TestPinMessage_ForbiddenForNonMember(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "pin_nm_own_"+uid, "pin_nm_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "pin_nm_str_"+uid, "pin_nm_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "PinNM "+uid, owner)
	msgID := testutil.CreateMessage(t, testDB, chatID, owner, "Some message")

	w := doPost(t, newRouter(stranger), "/chats/"+chatID+"/messages/"+msgID+"/pin", nil)
	if w.Code != http.StatusForbidden {
		t.Errorf("expected 403, got %d: %s", w.Code, w.Body.String())
	}
}

// ── GET /chats/:id/pin ────────────────────────────────────────────────────────

func TestGetPinnedMessage_NoPinReturnsNull(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "gpm_a_"+uid, "gpm_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "gpm_b_"+uid, "gpm_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)

	w := doGet(t, newRouter(userA), "/chats/"+chatID+"/pin")
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", w.Code)
	}
	// Handler returns null (JSON) when no pin exists.
	got := w.Body.String()
	if got != "null\n" && got != "null" && got != "" {
		t.Errorf("expected null body when no pin exists, got %q", got)
	}
}

func TestGetPinnedMessage_WithPin(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "gpm2_a_"+uid, "gpm2_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "gpm2_b_"+uid, "gpm2_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userA, "Pinned content")

	// Pin the message.
	wPin := doPost(t, newRouter(userA), "/chats/"+chatID+"/messages/"+msgID+"/pin",
		map[string]bool{"forAll": true})
	if wPin.Code != http.StatusOK {
		t.Fatalf("setup: expected 200, got %d: %s", wPin.Code, wPin.Body.String())
	}
	t.Cleanup(func() {
		testDB.Exec(context.Background(), "DELETE FROM chat_pins WHERE chat_id=$1", chatID)
	})

	w := doGet(t, newRouter(userA), "/chats/"+chatID+"/pin")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["messageId"] != msgID {
		t.Errorf("expected messageId=%s, got %v", msgID, body["messageId"])
	}
	if body["content"] != "Pinned content" {
		t.Errorf("expected content=Pinned content, got %v", body["content"])
	}
}

// ── DELETE /chats/:id/pin ─────────────────────────────────────────────────────

func TestUnpinMessage_Success(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "unpin_a_"+uid, "unpin_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "unpin_b_"+uid, "unpin_b_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	msgID := testutil.CreateMessage(t, testDB, chatID, userA, "To unpin")

	doPost(t, newRouter(userA), "/chats/"+chatID+"/messages/"+msgID+"/pin",
		map[string]bool{"forAll": true})

	w := doDelete(t, newRouter(userA), "/chats/"+chatID+"/pin?forAll=true")
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}

	var count int
	testDB.QueryRow(context.Background(),
		"SELECT COUNT(*) FROM chat_pins WHERE chat_id=$1 AND scope='all'", chatID,
	).Scan(&count)
	if count != 0 {
		t.Error("pin should have been removed from chat_pins")
	}
}
