package chats_test

import (
	"net/http"
	"testing"

	"github.com/muratosaro/coreme/api/internal/testutil"
)

// ── IDOR: GET /chats/:id ──────────────────────────────────────────────────────

// TestGetChat_NonMemberForbidden verifies that a user who is not a member of a
// group chat receives 404 (not 200) — the membership JOIN makes non-member
// access indistinguishable from a missing chat, avoiding information leakage.
func TestGetChat_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_gc_own_"+uid, "sec_gc_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_gc_str_"+uid, "sec_gc_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecGC "+uid, owner)

	w := doGet(t, newRouter(stranger), "/chats/"+chatID)
	if w.Code != http.StatusNotFound {
		t.Errorf("non-member GET /chats/:id: expected 404, got %d — IDOR possible!", w.Code)
	}
}

// ── IDOR: GET /chats/:id/messages ────────────────────────────────────────────

// TestGetMessages_NonMemberForbidden verifies that a non-member cannot read
// another chat's messages.
func TestGetMessages_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_gm_own_"+uid, "sec_gm_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_gm_str_"+uid, "sec_gm_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecGM "+uid, owner)

	w := doGet(t, newRouter(stranger), "/chats/"+chatID+"/messages")
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member GET messages: expected 403, got %d — IDOR possible!", w.Code)
	}
}

// TestMessages_ChatIsolation verifies that user C, who has no relation to the
// chat between A and B, cannot read those messages.
func TestMessages_ChatIsolation(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	userA := testutil.SeedUser(t, testDB, "sec_iso_a_"+uid, "sec_iso_a_"+uid+"@x.com")
	userB := testutil.SeedUser(t, testDB, "sec_iso_b_"+uid, "sec_iso_b_"+uid+"@x.com")
	userC := testutil.SeedUser(t, testDB, "sec_iso_c_"+uid, "sec_iso_c_"+uid+"@x.com")
	chatID := testutil.CreateDirectChat(t, testDB, userA, userB)
	testutil.CreateMessage(t, testDB, chatID, userA, "Private message")

	w := doGet(t, newRouter(userC), "/chats/"+chatID+"/messages")
	if w.Code != http.StatusForbidden {
		t.Errorf("unrelated user GET messages: expected 403, got %d — chat isolation broken!", w.Code)
	}
}

// ── IDOR: POST /chats/:id/messages ───────────────────────────────────────────

// TestSendMessage_NonMemberForbidden verifies that a non-member cannot send
// messages into a chat they don't belong to.
func TestSendMessage_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_sm_own_"+uid, "sec_sm_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_sm_str_"+uid, "sec_sm_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecSM "+uid, owner)

	w := doPost(t, newRouter(stranger), "/chats/"+chatID+"/messages",
		map[string]string{"content": "Injected"})
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member POST message: expected 403, got %d — IDOR possible!", w.Code)
	}
}

// ── IDOR: PATCH /chats/:id/messages/:msgId ───────────────────────────────────

// TestEditMessage_NonMemberForbidden verifies that a non-member cannot edit
// any message in a chat they don't belong to.
func TestEditMessage_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_em_own_"+uid, "sec_em_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_em_str_"+uid, "sec_em_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecEM "+uid, owner)
	msgID := testutil.CreateMessage(t, testDB, chatID, owner, "Original")

	w := doPatch(t, newRouter(stranger), "/chats/"+chatID+"/messages/"+msgID,
		map[string]string{"content": "Hijacked"})
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member PATCH message: expected 403, got %d — IDOR possible!", w.Code)
	}
}

// ── IDOR: DELETE /chats/:id/messages/:msgId ──────────────────────────────────

// TestDeleteMessage_NonMemberForbidden verifies that a non-member cannot
// delete messages from a chat they don't belong to.
func TestDeleteMessage_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_dm_own_"+uid, "sec_dm_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_dm_str_"+uid, "sec_dm_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecDM "+uid, owner)
	msgID := testutil.CreateMessage(t, testDB, chatID, owner, "Message")

	w := doDelete(t, newRouter(stranger), "/chats/"+chatID+"/messages/"+msgID)
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member DELETE message: expected 403, got %d — IDOR possible!", w.Code)
	}
}

// ── IDOR: GET /chats/:id/members ─────────────────────────────────────────────

// TestGetGroupMembers_NonMemberForbidden verifies that the member list is not
// exposed to users who are not in the chat.
func TestGetGroupMembers_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_gmbr_own_"+uid, "sec_gmbr_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_gmbr_str_"+uid, "sec_gmbr_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecGMBR "+uid, owner)

	w := doGet(t, newRouter(stranger), "/chats/"+chatID+"/members")
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member GET members: expected 403, got %d — member list leaked!", w.Code)
	}
}

// ── Authorization: PATCH /chats/:id ──────────────────────────────────────────

// TestUpdateGroup_NonMemberForbidden verifies that a non-member cannot update
// group metadata.
func TestUpdateGroup_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_ug_own_"+uid, "sec_ug_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_ug_str_"+uid, "sec_ug_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecUG "+uid, owner)

	w := doPatch(t, newRouter(stranger), "/chats/"+chatID,
		map[string]interface{}{"name": "Hacked"})
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member PATCH group: expected 403, got %d", w.Code)
	}
}

// TestUpdateGroup_RegularMemberForbidden verifies that a regular member (not
// admin/superadmin) cannot update group metadata.
func TestUpdateGroup_RegularMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_ugm_own_"+uid, "sec_ugm_own_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "sec_ugm_mem_"+uid, "sec_ugm_mem_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecUGM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doPatch(t, newRouter(member), "/chats/"+chatID,
		map[string]interface{}{"name": "Hacked"})
	if w.Code != http.StatusForbidden {
		t.Errorf("regular member PATCH group: expected 403, got %d", w.Code)
	}
}

// ── Authorization: POST /chats/:id/members ───────────────────────────────────

// TestAddMember_NonMemberForbidden verifies that a non-member cannot add
// members to a chat.
func TestAddMember_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_am_own_"+uid, "sec_am_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_am_str_"+uid, "sec_am_str_"+uid+"@x.com")
	victim := testutil.SeedUser(t, testDB, "sec_am_vic_"+uid, "sec_am_vic_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecAM "+uid, owner)

	w := doPost(t, newRouter(stranger), "/chats/"+chatID+"/members",
		map[string]string{"user_id": victim})
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member POST member: expected 403, got %d", w.Code)
	}
}

// TestAddMember_RegularMemberForbidden verifies that a regular member cannot
// add new members to the chat (only admins may do so).
func TestAddMember_RegularMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_amrm_own_"+uid, "sec_amrm_own_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "sec_amrm_mem_"+uid, "sec_amrm_mem_"+uid+"@x.com")
	victim := testutil.SeedUser(t, testDB, "sec_amrm_vic_"+uid, "sec_amrm_vic_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecAMRM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")

	w := doPost(t, newRouter(member), "/chats/"+chatID+"/members",
		map[string]string{"user_id": victim})
	if w.Code != http.StatusForbidden {
		t.Errorf("regular member POST member: expected 403, got %d", w.Code)
	}
}

// ── Authorization: DELETE /chats/:id/members/:userId ─────────────────────────

// TestRemoveMember_NonMemberForbidden verifies that a non-member cannot kick
// members from a chat.
func TestRemoveMember_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_rm_own_"+uid, "sec_rm_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_rm_str_"+uid, "sec_rm_str_"+uid+"@x.com")
	victim := testutil.SeedUser(t, testDB, "sec_rm_vic_"+uid, "sec_rm_vic_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecRM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, victim, "member")

	w := doDelete(t, newRouter(stranger), "/chats/"+chatID+"/members/"+victim)
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member DELETE member: expected 403, got %d", w.Code)
	}
}

// TestRemoveMember_RegularMemberForbidden verifies that a regular member cannot
// kick other members.
func TestRemoveMember_RegularMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_rmrm_own_"+uid, "sec_rmrm_own_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "sec_rmrm_mem_"+uid, "sec_rmrm_mem_"+uid+"@x.com")
	victim := testutil.SeedUser(t, testDB, "sec_rmrm_vic_"+uid, "sec_rmrm_vic_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecRMRM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")
	testutil.AddChatMember(t, testDB, chatID, victim, "member")

	w := doDelete(t, newRouter(member), "/chats/"+chatID+"/members/"+victim)
	if w.Code != http.StatusForbidden {
		t.Errorf("regular member DELETE member: expected 403, got %d", w.Code)
	}
}

// ── Authorization: PATCH /chats/:id/members/:userId ──────────────────────────

// TestUpdateMemberRole_NonMemberForbidden verifies that a non-member cannot
// change member roles.
func TestUpdateMemberRole_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_umr_own_"+uid, "sec_umr_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_umr_str_"+uid, "sec_umr_str_"+uid+"@x.com")
	victim := testutil.SeedUser(t, testDB, "sec_umr_vic_"+uid, "sec_umr_vic_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecUMR "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, victim, "member")

	w := doPatch(t, newRouter(stranger), "/chats/"+chatID+"/members/"+victim,
		map[string]string{"role": "admin"})
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member PATCH role: expected 403, got %d", w.Code)
	}
}

// TestUpdateMemberRole_RegularMemberForbidden verifies that a regular member
// cannot promote or demote other members.
func TestUpdateMemberRole_RegularMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_umrm_own_"+uid, "sec_umrm_own_"+uid+"@x.com")
	member := testutil.SeedUser(t, testDB, "sec_umrm_mem_"+uid, "sec_umrm_mem_"+uid+"@x.com")
	victim := testutil.SeedUser(t, testDB, "sec_umrm_vic_"+uid, "sec_umrm_vic_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecUMRM "+uid, owner)
	testutil.AddChatMember(t, testDB, chatID, member, "member")
	testutil.AddChatMember(t, testDB, chatID, victim, "member")

	w := doPatch(t, newRouter(member), "/chats/"+chatID+"/members/"+victim,
		map[string]string{"role": "admin"})
	if w.Code != http.StatusForbidden {
		t.Errorf("regular member PATCH role: expected 403, got %d", w.Code)
	}
}

// ── IDOR: GET /chats/:id/pin ──────────────────────────────────────────────────

// TestGetPinnedMessage_NonMemberForbidden verifies that a non-member cannot
// read a chat's pinned message.
func TestGetPinnedMessage_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_gpin_own_"+uid, "sec_gpin_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_gpin_str_"+uid, "sec_gpin_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecGPIN "+uid, owner)

	w := doGet(t, newRouter(stranger), "/chats/"+chatID+"/pin")
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member GET pin: expected 403, got %d — IDOR possible!", w.Code)
	}
}

// ── IDOR: DELETE /chats/:id/pin ───────────────────────────────────────────────

// TestUnpinMessage_NonMemberForbidden verifies that a non-member cannot unpin
// a message from a chat they don't belong to.
func TestUnpinMessage_NonMemberForbidden(t *testing.T) {
	skipIfNoDB(t)
	uid := testutil.UniqueID()
	owner := testutil.SeedUser(t, testDB, "sec_upin_own_"+uid, "sec_upin_own_"+uid+"@x.com")
	stranger := testutil.SeedUser(t, testDB, "sec_upin_str_"+uid, "sec_upin_str_"+uid+"@x.com")
	chatID := testutil.CreateGroupChat(t, testDB, "SecUPIN "+uid, owner)
	msgID := testutil.CreateMessage(t, testDB, chatID, owner, "Pinned")
	doPost(t, newRouter(owner), "/chats/"+chatID+"/messages/"+msgID+"/pin",
		map[string]bool{"forAll": true})

	w := doDelete(t, newRouter(stranger), "/chats/"+chatID+"/pin?forAll=true")
	if w.Code != http.StatusForbidden {
		t.Errorf("non-member DELETE pin: expected 403, got %d — IDOR possible!", w.Code)
	}
}
