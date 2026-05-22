package handler

import (
	"context"
	"encoding/json"
	"log"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/muratosaro/coreme/realtime/internal/hub"
)

type Handler struct {
	Pool *pgxpool.Pool
	Hub  *hub.Hub
}

type inMsg struct {
	Type         string          `json:"type"`
	ChatID       string          `json:"chatId"`
	MessageID    string          `json:"messageId"`
	Content      string          `json:"content"`
	MsgType      string          `json:"msgType"`
	ReplyToID    *string         `json:"replyToId"`
	ClientMsgID  string          `json:"clientMsgId"`
	Emoji        string          `json:"emoji"`
	Message      json.RawMessage `json:"message"`
	TargetUserID string          `json:"targetUserId"`
	CallID       string          `json:"callId"`
	CallType     string          `json:"callType"`
}

type activeCall struct {
	CallerID  string
	CalleeID  string
	Type      string
	StartedAt time.Time
}

var (
	activeCalls   = map[string]*activeCall{}
	activeCallsMu sync.RWMutex
)

func (h *Handler) Handle(c *hub.Client, raw []byte) {
	var msg inMsg
	if err := json.Unmarshal(raw, &msg); err != nil {
		return
	}
	ctx := context.Background()
	switch msg.Type {
	case "join_chat":
		h.joinChat(ctx, c, msg.ChatID)
	case "send_message":
		h.sendMessage(ctx, c, msg)
	case "reaction_add":
		h.reactionAdd(ctx, c, msg)
	case "reaction_remove":
		h.reactionRemove(ctx, c, msg)
	case "message_edited":
		if msg.ChatID != "" && msg.Message != nil {
			h.Hub.BroadcastRoomExcept(msg.ChatID, c.UserID, encode(map[string]any{
				"type": "message_edited", "chatId": msg.ChatID, "message": msg.Message,
			}))
		}
	case "message_deleted":
		if msg.ChatID != "" && msg.MessageID != "" {
			h.Hub.BroadcastRoomExcept(msg.ChatID, c.UserID, encode(map[string]any{
				"type": "message_deleted", "chatId": msg.ChatID, "messageId": msg.MessageID,
			}))
		}
	case "typing_start":
		h.Hub.BroadcastRoomExcept(msg.ChatID, c.UserID, encode(map[string]any{
			"type": "user_typing", "chatId": msg.ChatID, "userId": c.UserID, "isTyping": true,
		}))
	case "typing_stop":
		h.Hub.BroadcastRoomExcept(msg.ChatID, c.UserID, encode(map[string]any{
			"type": "user_typing", "chatId": msg.ChatID, "userId": c.UserID, "isTyping": false,
		}))
	case "mark_read":
		h.markRead(ctx, c, msg.ChatID)
	case "initiate_call":
		h.initiateCall(ctx, c, msg)
	case "accept_call":
		h.acceptCall(c, msg.CallID)
	case "reject_call":
		h.rejectCall(ctx, c, msg.CallID)
	case "end_call":
		h.endCall(ctx, c, msg.CallID)
	}
}

func (h *Handler) joinChat(ctx context.Context, c *hub.Client, chatID string) {
	var exists bool
	err := h.Pool.QueryRow(ctx,
		`SELECT EXISTS(SELECT 1 FROM chat_members WHERE chat_id=$1 AND user_id=$2)`,
		chatID, c.UserID,
	).Scan(&exists)
	if err != nil || !exists {
		return
	}
	h.Hub.JoinRoom(c, chatID)
}

func (h *Handler) sendMessage(ctx context.Context, c *hub.Client, msg inMsg) {
	var isMember bool
	if err := h.Pool.QueryRow(ctx,
		`SELECT EXISTS(SELECT 1 FROM chat_members WHERE chat_id=$1 AND user_id=$2)`,
		msg.ChatID, c.UserID,
	).Scan(&isMember); err != nil || !isMember {
		return
	}

	msgType := msg.MsgType
	if msgType == "" {
		msgType = "text"
	}

	var replyContent, replySenderName *string
	if msg.ReplyToID != nil && *msg.ReplyToID != "" {
		row := h.Pool.QueryRow(ctx,
			`SELECT m.content, u.display_name FROM messages m
			 JOIN users u ON m.sender_id = u.id WHERE m.id=$1`,
			*msg.ReplyToID,
		)
		var rc, rsn string
		if err := row.Scan(&rc, &rsn); err == nil {
			replyContent = &rc
			replySenderName = &rsn
		}
	}

	var result struct {
		ID                 string    `json:"id"`
		ChatID             string    `json:"chat_id"`
		SenderID           string    `json:"sender_id"`
		Type               string    `json:"type"`
		Content            string    `json:"content"`
		IsRead             bool      `json:"is_read"`
		CreatedAt          time.Time `json:"created_at"`
		IsEdited           bool      `json:"is_edited"`
		IsDeleted          bool      `json:"is_deleted"`
		ReplyToID          *string   `json:"reply_to_id"`
		ReplyToContent     *string   `json:"reply_to_content"`
		ReplyToSenderName  *string   `json:"reply_to_sender_name"`
	}

	err := h.Pool.QueryRow(ctx,
		`INSERT INTO messages
		   (chat_id, sender_id, type, content, reply_to_id, reply_to_content, reply_to_sender_name)
		 VALUES ($1,$2,$3,$4,$5,$6,$7)
		 RETURNING id, chat_id, sender_id, type, content, is_read, created_at,
		           is_edited, is_deleted, reply_to_id, reply_to_content, reply_to_sender_name`,
		msg.ChatID, c.UserID, msgType, msg.Content, msg.ReplyToID, replyContent, replySenderName,
	).Scan(
		&result.ID, &result.ChatID, &result.SenderID, &result.Type, &result.Content,
		&result.IsRead, &result.CreatedAt, &result.IsEdited, &result.IsDeleted,
		&result.ReplyToID, &result.ReplyToContent, &result.ReplyToSenderName,
	)
	if err != nil {
		log.Printf("[handler] sendMessage insert: %v", err)
		return
	}

	var senderName string
	h.Pool.QueryRow(ctx, `SELECT display_name FROM users WHERE id=$1`, c.UserID).Scan(&senderName)

	payload := encode(map[string]any{
		"type":                  "new_message",
		"id":                    result.ID,
		"chat_id":               result.ChatID,
		"sender_id":             result.SenderID,
		"message_type":          result.Type,
		"content":               result.Content,
		"is_read":               result.IsRead,
		"created_at":            result.CreatedAt,
		"is_edited":             result.IsEdited,
		"is_deleted":            result.IsDeleted,
		"reply_to_id":           result.ReplyToID,
		"reply_to_content":      result.ReplyToContent,
		"reply_to_sender_name":  result.ReplyToSenderName,
		"sender_name":           senderName,
		"client_msg_id":         msg.ClientMsgID,
	})
	h.Hub.BroadcastRoom(msg.ChatID, payload)

	go h.handleAutoReply(msg.ChatID, result.ID, c.UserID)
}

func (h *Handler) handleAutoReply(chatID, originalMsgID, senderID string) {
	ctx := context.Background()
	rows, err := h.Pool.Query(ctx,
		`SELECT cm.user_id, us.auto_reply_message, u.display_name
		 FROM chat_members cm
		 LEFT JOIN user_settings us ON cm.user_id = us.user_id
		 LEFT JOIN users u ON cm.user_id = u.id
		 WHERE cm.chat_id=$1 AND cm.user_id!=$2
		   AND us.auto_reply_enabled=true AND u.is_online=false`,
		chatID, senderID,
	)
	if err != nil {
		return
	}
	defer rows.Close()

	type member struct {
		UserID      string
		ReplyMsg    *string
		DisplayName string
	}
	var members []member
	for rows.Next() {
		var m member
		if err := rows.Scan(&m.UserID, &m.ReplyMsg, &m.DisplayName); err == nil {
			members = append(members, m)
		}
	}

	for _, m := range members {
		autoMsg := "Зараз недоступний. Відповім пізніше."
		if m.ReplyMsg != nil && *m.ReplyMsg != "" {
			autoMsg = *m.ReplyMsg
		}
		var result struct {
			ID        string    `json:"id"`
			ChatID    string    `json:"chat_id"`
			SenderID  string    `json:"sender_id"`
			Type      string    `json:"type"`
			Content   string    `json:"content"`
			IsRead    bool      `json:"is_read"`
			CreatedAt time.Time `json:"created_at"`
		}
		err := h.Pool.QueryRow(ctx,
			`INSERT INTO messages (chat_id, sender_id, type, content)
			 VALUES ($1,$2,'text',$3)
			 RETURNING id, chat_id, sender_id, type, content, is_read, created_at`,
			chatID, m.UserID, autoMsg,
		).Scan(&result.ID, &result.ChatID, &result.SenderID, &result.Type, &result.Content, &result.IsRead, &result.CreatedAt)
		if err != nil {
			continue
		}
		h.Hub.BroadcastRoom(chatID, encode(map[string]any{
			"type":        "new_message",
			"id":          result.ID,
			"chat_id":     result.ChatID,
			"sender_id":   result.SenderID,
			"message_type": result.Type,
			"content":     result.Content,
			"is_read":     result.IsRead,
			"created_at":  result.CreatedAt,
			"sender_name": m.DisplayName,
		}))
	}
}

func (h *Handler) reactionAdd(ctx context.Context, c *hub.Client, msg inMsg) {
	if _, err := h.Pool.Exec(ctx,
		`INSERT INTO message_reactions (message_id, user_id, emoji)
		 VALUES ($1,$2,$3) ON CONFLICT (message_id, user_id) DO UPDATE SET emoji=$3`,
		msg.MessageID, c.UserID, msg.Emoji,
	); err != nil {
		return
	}
	h.broadcastReactions(ctx, msg.ChatID, msg.MessageID)
}

func (h *Handler) reactionRemove(ctx context.Context, c *hub.Client, msg inMsg) {
	if _, err := h.Pool.Exec(ctx,
		`DELETE FROM message_reactions WHERE message_id=$1 AND user_id=$2`,
		msg.MessageID, c.UserID,
	); err != nil {
		return
	}
	h.broadcastReactions(ctx, msg.ChatID, msg.MessageID)
}

func (h *Handler) broadcastReactions(ctx context.Context, chatID, messageID string) {
	rows, err := h.Pool.Query(ctx,
		`SELECT emoji, COUNT(*)::int, array_agg(user_id::text)
		 FROM message_reactions WHERE message_id=$1 GROUP BY emoji`,
		messageID,
	)
	if err != nil {
		return
	}
	defer rows.Close()

	type reaction struct {
		Emoji   string   `json:"emoji"`
		Count   int      `json:"count"`
		UserIDs []string `json:"user_ids"`
	}
	var reactions []reaction
	for rows.Next() {
		var r reaction
		if err := rows.Scan(&r.Emoji, &r.Count, &r.UserIDs); err == nil {
			reactions = append(reactions, r)
		}
	}
	h.Hub.BroadcastRoom(chatID, encode(map[string]any{
		"type": "reaction_updated", "messageId": messageID, "chatId": chatID, "reactions": reactions,
	}))
}

func (h *Handler) markRead(ctx context.Context, c *hub.Client, chatID string) {
	rows, err := h.Pool.Query(ctx,
		`SELECT DISTINCT sender_id FROM messages
		 WHERE chat_id=$1 AND sender_id!=$2 AND is_read=false`,
		chatID, c.UserID,
	)
	if err != nil {
		return
	}
	defer rows.Close()

	var senderIDs []string
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err == nil {
			senderIDs = append(senderIDs, id)
		}
	}
	if len(senderIDs) == 0 {
		return
	}

	h.Pool.Exec(ctx,
		`UPDATE messages SET is_read=true WHERE chat_id=$1 AND sender_id!=$2`,
		chatID, c.UserID,
	)

	payload := encode(map[string]any{
		"type": "message_read", "chatId": chatID, "userId": c.UserID,
	})
	h.Hub.BroadcastRoomExcept(chatID, c.UserID, payload)
	for _, sid := range senderIDs {
		h.Hub.SendToUser(sid, payload)
	}
}

func (h *Handler) initiateCall(ctx context.Context, c *hub.Client, msg inMsg) {
	if msg.TargetUserID == "" {
		return
	}
	callType := msg.CallType
	if callType == "" {
		callType = "audio"
	}

	var callerName, avatarURL string
	h.Pool.QueryRow(ctx, `SELECT display_name, COALESCE(avatar_url,'') FROM users WHERE id=$1`, c.UserID).
		Scan(&callerName, &avatarURL)

	// find or skip chatId (direct chat between users)
	var chatID string
	h.Pool.QueryRow(ctx,
		`SELECT c.id FROM chats c
		 JOIN chat_members m1 ON m1.chat_id=c.id AND m1.user_id=$1
		 JOIN chat_members m2 ON m2.chat_id=c.id AND m2.user_id=$2
		 WHERE c.type='direct' LIMIT 1`,
		c.UserID, msg.TargetUserID,
	).Scan(&chatID)

	callID := uuid.NewString()

	activeCallsMu.Lock()
	activeCalls[callID] = &activeCall{
		CallerID:  c.UserID,
		CalleeID:  msg.TargetUserID,
		Type:      callType,
		StartedAt: time.Now(),
	}
	activeCallsMu.Unlock()

	h.Hub.SendToUser(msg.TargetUserID, encode(map[string]any{
		"type":           "incoming_call",
		"callId":         callID,
		"chatId":         chatID,
		"callType":       callType,
		"callerName":     callerName,
		"callerAvatarUrl": avatarURL,
	}))
}

func (h *Handler) acceptCall(c *hub.Client, callID string) {
	activeCallsMu.RLock()
	call, ok := activeCalls[callID]
	activeCallsMu.RUnlock()
	if !ok {
		return
	}
	h.Hub.SendToUser(call.CallerID, encode(map[string]any{
		"type": "call_accepted", "callId": callID,
	}))
}

func (h *Handler) rejectCall(ctx context.Context, c *hub.Client, callID string) {
	activeCallsMu.Lock()
	call, ok := activeCalls[callID]
	if ok {
		delete(activeCalls, callID)
	}
	activeCallsMu.Unlock()
	if !ok {
		return
	}
	h.saveCallRecord(ctx, callID, call, "missed")
	h.Hub.SendToUser(call.CallerID, encode(map[string]any{
		"type": "call_rejected", "callId": callID,
	}))
}

func (h *Handler) endCall(ctx context.Context, c *hub.Client, callID string) {
	activeCallsMu.Lock()
	call, ok := activeCalls[callID]
	if ok {
		delete(activeCalls, callID)
	}
	activeCallsMu.Unlock()
	if !ok {
		return
	}
	h.saveCallRecord(ctx, callID, call, "completed")
	otherID := call.CalleeID
	if c.UserID == call.CalleeID {
		otherID = call.CallerID
	}
	h.Hub.SendToUser(otherID, encode(map[string]any{
		"type": "call_ended", "callId": callID,
	}))
}

func (h *Handler) saveCallRecord(ctx context.Context, callID string, call *activeCall, status string) {
	endedAt := time.Now()
	duration := 0
	if status == "completed" {
		duration = int(endedAt.Sub(call.StartedAt).Seconds())
	}
	h.Pool.Exec(ctx,
		`INSERT INTO call_history (id, call_id, caller_id, callee_id, type, status, started_at, ended_at, duration_seconds)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) ON CONFLICT DO NOTHING`,
		uuid.NewString(), callID,
		call.CallerID, call.CalleeID,
		call.Type, status,
		call.StartedAt, endedAt, duration,
	)
}

// OnConnect notifies all others that a user came online and updates DB.
func (h *Handler) OnConnect(ctx context.Context, c *hub.Client) {
	h.Pool.Exec(ctx, `UPDATE users SET is_online=true, last_seen=NOW() WHERE id=$1`, c.UserID)
	h.Hub.BroadcastAllExcept(c.UserID, encode(map[string]any{
		"type": "user_online", "userId": c.UserID,
	}))
}

// OnDisconnect notifies all others that a user went offline and updates DB.
func (h *Handler) OnDisconnect(ctx context.Context, c *hub.Client) {
	h.Pool.Exec(ctx, `UPDATE users SET is_online=false, last_seen=NOW() WHERE id=$1`, c.UserID)
	h.Hub.BroadcastAllExcept(c.UserID, encode(map[string]any{
		"type": "user_offline", "userId": c.UserID,
	}))
}

// RunScheduler checks for due scheduled posts every minute.
func (h *Handler) RunScheduler() {
	ticker := time.NewTicker(time.Minute)
	defer ticker.Stop()
	for range ticker.C {
		h.processDuePosts()
	}
}

func (h *Handler) processDuePosts() {
	ctx := context.Background()
	rows, err := h.Pool.Query(ctx,
		`SELECT id, chat_id, created_by, type, content FROM scheduled_posts
		 WHERE sent=false AND scheduled_at<=NOW() ORDER BY scheduled_at ASC LIMIT 50`,
	)
	if err != nil {
		return
	}
	defer rows.Close()

	type post struct {
		ID        string
		ChatID    string
		CreatedBy string
		Type      string
		Content   string
	}
	var posts []post
	for rows.Next() {
		var p post
		if err := rows.Scan(&p.ID, &p.ChatID, &p.CreatedBy, &p.Type, &p.Content); err == nil {
			posts = append(posts, p)
		}
	}

	for _, p := range posts {
		var msgID, chatID, senderID, mType, content string
		var createdAt time.Time
		err := h.Pool.QueryRow(ctx,
			`INSERT INTO messages (chat_id, sender_id, type, content)
			 VALUES ($1,$2,$3,$4) RETURNING id, chat_id, sender_id, type, content, created_at`,
			p.ChatID, p.CreatedBy, p.Type, p.Content,
		).Scan(&msgID, &chatID, &senderID, &mType, &content, &createdAt)
		if err != nil {
			continue
		}
		h.Pool.Exec(ctx, `UPDATE scheduled_posts SET sent=true WHERE id=$1`, p.ID)
		h.Hub.BroadcastRoom(chatID, encode(map[string]any{
			"type":         "new_message",
			"id":           msgID,
			"chat_id":      chatID,
			"sender_id":    senderID,
			"message_type": mType,
			"content":      content,
			"created_at":   createdAt,
		}))
	}
}

func encode(v any) []byte {
	b, _ := json.Marshal(v)
	return b
}
