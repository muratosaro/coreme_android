package chats

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type handler struct{ db *pgxpool.Pool }

// memberRole returns the requester's role in a chat, or "" if not a member.
func (h *handler) memberRole(ctx context.Context, chatId, userId string) string {
	var role string
	h.db.QueryRow(ctx, "SELECT role FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatId, userId).Scan(&role)
	return role
}

func RegisterRoutes(r *gin.RouterGroup, db *pgxpool.Pool) {
	h := &handler{db}
	r.GET("", h.getChats)
	r.POST("", h.createChat)
	r.POST("/group", h.createGroup)
	r.GET("/:id", h.getChatById)
	r.PATCH("/:id", h.updateGroup)
	r.DELETE("/:id/leave", h.leaveGroup)
	r.GET("/:id/members", h.getGroupMembers)
	r.POST("/:id/members", h.addMember)
	r.DELETE("/:id/members/:userId", h.removeMember)
	r.PATCH("/:id/members/:userId", h.updateMemberRole)
	r.GET("/:id/messages", h.getMessages)
	r.POST("/:id/messages", h.sendMessage)
	r.PATCH("/:id/messages/:msgId", h.editMessage)
	r.DELETE("/:id/messages/:msgId", h.deleteMessage)
	r.GET("/:id/pin", h.getPinnedMessage)
	r.POST("/:id/messages/:msgId/pin", h.pinMessage)
	r.DELETE("/:id/pin", h.unpinMessage)
}

// ── Models ────────────────────────────────────────────────────────────────────

type OtherUser struct {
	ID          string  `json:"id"`
	Username    string  `json:"username"`
	DisplayName string  `json:"display_name"`
	AvatarURL   *string `json:"avatar_url"`
	IsOnline    bool    `json:"is_online"`
}

type LastMessage struct {
	ID         string    `json:"id"`
	ChatID     string    `json:"chat_id"`
	SenderID   string    `json:"sender_id"`
	Type       string    `json:"type"`
	Content    string    `json:"content"`
	IsRead     bool      `json:"is_read"`
	CreatedAt  time.Time `json:"created_at"`
	IsEdited   bool      `json:"is_edited"`
	IsDeleted  bool      `json:"is_deleted"`
	SenderName *string   `json:"sender_name"`
}

type Chat struct {
	ID             string       `json:"id"`
	Type           string       `json:"type"`
	Name           *string      `json:"name"`
	Description    *string      `json:"description"`
	GroupAvatarURL *string      `json:"group_avatar_url"`
	CreatedAt      time.Time    `json:"created_at"`
	LastMessage    *LastMessage `json:"last_message"`
	UnreadCount    int          `json:"unread_count"`
	MemberCount    int          `json:"member_count"`
	OtherUser      *OtherUser   `json:"other_user"`
}

type Message struct {
	ID                string    `json:"id"`
	ChatID            string    `json:"chat_id"`
	SenderID          string    `json:"sender_id"`
	Type              string    `json:"type"`
	Content           string    `json:"content"`
	IsRead            bool      `json:"is_read"`
	CreatedAt         time.Time `json:"created_at"`
	IsEdited          bool      `json:"is_edited"`
	IsDeleted         bool      `json:"is_deleted"`
	ReplyToID         *string   `json:"reply_to_id"`
	ReplyToContent    *string   `json:"reply_to_content"`
	ReplyToSenderName *string   `json:"reply_to_sender_name"`
	Duration          *int      `json:"duration,omitempty"`
	FileName          *string   `json:"file_name,omitempty"`
	FileSize          *int64    `json:"file_size,omitempty"`
	Caption           *string   `json:"caption,omitempty"`
	Reactions         []Reaction `json:"reactions,omitempty"`
	SenderName        *string   `json:"sender_name,omitempty"`
}

type Reaction struct {
	Emoji  string `json:"emoji"`
	UserID string `json:"user_id"`
}

type Member struct {
	UserID      string    `json:"user_id"`
	Username    string    `json:"username"`
	DisplayName string    `json:"display_name"`
	AvatarURL   *string   `json:"avatar_url"`
	IsOnline    bool      `json:"is_online"`
	Role        string    `json:"role"`
	JoinedAt    time.Time `json:"joined_at"`
}

// ── List chats ────────────────────────────────────────────────────────────────

func (h *handler) getChats(c *gin.Context) {
	userId := c.GetString("userId")
	rows, err := h.db.Query(c.Request.Context(), chatSelectQuery(userId))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	chats := []Chat{}
	for rows.Next() {
		ch, err := scanChat(rows)
		if err == nil {
			chats = append(chats, ch)
		}
	}
	c.JSON(http.StatusOK, chats)
}

func chatSelectQuery(userId string) string {
	return fmt.Sprintf(`
		SELECT
			c.id, c.type, c.name, c.description, c.avatar_url, c.created_at,
			(SELECT row_to_json(x) FROM (
				SELECT m.id, m.chat_id, m.sender_id, m.type, m.content,
				       m.is_read, m.created_at, m.is_edited, m.is_deleted, u2.display_name AS sender_name
				FROM messages m JOIN users u2 ON m.sender_id=u2.id
				WHERE m.chat_id=c.id AND m.is_deleted=false ORDER BY m.created_at DESC LIMIT 1
			) x) AS last_message,
			(SELECT COUNT(*)::int FROM messages m
			 WHERE m.chat_id=c.id AND m.sender_id!='%s' AND m.is_read=false) AS unread_count,
			(SELECT COUNT(*)::int FROM chat_members WHERE chat_id=c.id) AS member_count,
			CASE WHEN c.type='direct' THEN (
				SELECT row_to_json(x) FROM (
					SELECT u.id, u.username, u.display_name, u.avatar_url, u.is_online
					FROM users u JOIN chat_members cm2 ON u.id=cm2.user_id
					WHERE cm2.chat_id=c.id AND cm2.user_id!='%s' LIMIT 1
				) x
			) END AS other_user
		FROM chats c
		JOIN chat_members cm ON c.id=cm.chat_id AND cm.user_id='%s'
		ORDER BY COALESCE(
			(SELECT created_at FROM messages WHERE chat_id=c.id ORDER BY created_at DESC LIMIT 1),
			c.created_at
		) DESC`, userId, userId, userId)
}

func scanChat(rows interface{ Scan(...interface{}) error }) (Chat, error) {
	var ch Chat
	var lastMsgJSON, otherUserJSON []byte
	err := rows.Scan(
		&ch.ID, &ch.Type, &ch.Name, &ch.Description, &ch.GroupAvatarURL, &ch.CreatedAt,
		&lastMsgJSON, &ch.UnreadCount, &ch.MemberCount, &otherUserJSON,
	)
	if err != nil {
		return ch, err
	}
	if lastMsgJSON != nil {
		ch.LastMessage = &LastMessage{}
		json.Unmarshal(lastMsgJSON, ch.LastMessage)
	}
	if otherUserJSON != nil {
		ch.OtherUser = &OtherUser{}
		json.Unmarshal(otherUserJSON, ch.OtherUser)
	}
	return ch, nil
}

// ── Create chat ───────────────────────────────────────────────────────────────

func (h *handler) createChat(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		Type      string   `json:"type" binding:"required,oneof=direct group"`
		MemberIDs []string `json:"member_ids" binding:"required,min=1"`
		Name      *string  `json:"name"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}

	ctx := c.Request.Context()
	if req.Type == "direct" && len(req.MemberIDs) > 0 {
		otherId := req.MemberIDs[0]
		var existingId string
		h.db.QueryRow(ctx,
			`SELECT c.id FROM chats c
			 JOIN chat_members cm1 ON c.id=cm1.chat_id AND cm1.user_id=$1
			 JOIN chat_members cm2 ON c.id=cm2.chat_id AND cm2.user_id=$2
			 WHERE c.type='direct' LIMIT 1`, userId, otherId,
		).Scan(&existingId)
		if existingId != "" {
			c.Params = append(c.Params, gin.Param{Key: "id", Value: existingId})
			h.getChatById(c)
			return
		}
	}

	chatId := uuid.NewString()
	_, err := h.db.Exec(ctx,
		"INSERT INTO chats (id, type, name, created_by) VALUES ($1, $2, $3, $4)",
		chatId, req.Type, req.Name, userId,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}

	allMembers := uniqueStr(append([]string{userId}, req.MemberIDs...))
	for _, memberId := range allMembers {
		if _, err := h.db.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2)", chatId, memberId); err != nil {
			log.Printf("[chats] addMember to direct chat %s failed for user %s: %v", chatId, memberId, err)
		}
	}

	c.Params = append(c.Params, gin.Param{Key: "id", Value: chatId})
	h.getChatById(c)
}

func (h *handler) createGroup(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		Name        string   `json:"name" binding:"required"`
		Description *string  `json:"description"`
		AvatarURL   *string  `json:"avatar_url"`
		MemberIDs   []string `json:"member_ids"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}

	ctx := c.Request.Context()
	chatId := uuid.NewString()
	h.db.Exec(ctx,
		"INSERT INTO chats (id, type, name, description, avatar_url, created_by) VALUES ($1,'group',$2,$3,$4,$5)",
		chatId, req.Name, req.Description, req.AvatarURL, userId,
	)
	if _, err := h.db.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,'superadmin')", chatId, userId); err != nil {
		log.Printf("[chats] addSuperadmin to group %s failed for user %s: %v", chatId, userId, err)
	}
	for _, memberId := range req.MemberIDs {
		if memberId != userId {
			if _, err := h.db.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,'member')", chatId, memberId); err != nil {
				log.Printf("[chats] addMember to group %s failed for user %s: %v", chatId, memberId, err)
			}
		}
	}

	c.Params = append(c.Params, gin.Param{Key: "id", Value: chatId})
	h.getChatById(c)
}

func (h *handler) getChatById(c *gin.Context) {
	userId := c.GetString("userId")
	chatId := c.Param("id")
	query := fmt.Sprintf(`%s WHERE c.id='%s'`, chatSelectQuery(userId)[:len(chatSelectQuery(userId))-len("ORDER BY COALESCE(\n\t\t(SELECT created_at FROM messages WHERE chat_id=c.id ORDER BY created_at DESC LIMIT 1),\n\t\t\tc.created_at\n\t\t) DESC")], chatId)

	// Simpler direct query
	q := fmt.Sprintf(`
		SELECT
			c.id, c.type, c.name, c.description, c.avatar_url, c.created_at,
			(SELECT row_to_json(x) FROM (
				SELECT m.id, m.chat_id, m.sender_id, m.type, m.content,
				       m.is_read, m.created_at, m.is_edited, m.is_deleted, u2.display_name AS sender_name
				FROM messages m JOIN users u2 ON m.sender_id=u2.id
				WHERE m.chat_id=c.id AND m.is_deleted=false ORDER BY m.created_at DESC LIMIT 1
			) x) AS last_message,
			(SELECT COUNT(*)::int FROM messages m
			 WHERE m.chat_id=c.id AND m.sender_id!='%s' AND m.is_read=false) AS unread_count,
			(SELECT COUNT(*)::int FROM chat_members WHERE chat_id=c.id) AS member_count,
			CASE WHEN c.type='direct' THEN (
				SELECT row_to_json(x) FROM (
					SELECT u.id, u.username, u.display_name, u.avatar_url, u.is_online
					FROM users u JOIN chat_members cm2 ON u.id=cm2.user_id
					WHERE cm2.chat_id=c.id AND cm2.user_id!='%s' LIMIT 1
				) x
			) END AS other_user
		FROM chats c
		JOIN chat_members cm_auth ON c.id=cm_auth.chat_id AND cm_auth.user_id='%s'
		WHERE c.id='%s'`, userId, userId, userId, chatId)
	_ = query

	var ch Chat
	var lastMsgJSON, otherUserJSON []byte
	err := h.db.QueryRow(c.Request.Context(), q).Scan(
		&ch.ID, &ch.Type, &ch.Name, &ch.Description, &ch.GroupAvatarURL, &ch.CreatedAt,
		&lastMsgJSON, &ch.UnreadCount, &ch.MemberCount, &otherUserJSON,
	)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"message": "Chat not found"})
		return
	}
	if lastMsgJSON != nil {
		ch.LastMessage = &LastMessage{}
		json.Unmarshal(lastMsgJSON, ch.LastMessage)
	}
	if otherUserJSON != nil {
		ch.OtherUser = &OtherUser{}
		json.Unmarshal(otherUserJSON, ch.OtherUser)
	}
	c.JSON(http.StatusOK, ch)
}

// ── Group management ──────────────────────────────────────────────────────────

func (h *handler) updateGroup(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	ctx := c.Request.Context()
	if role := h.memberRole(ctx, chatId, userId); role != "admin" && role != "superadmin" {
		c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
		return
	}
	var req struct {
		Name        *string `json:"name"`
		Description *string `json:"description"`
		AvatarURL   *string `json:"avatar_url"`
	}
	c.ShouldBindJSON(&req)

	fields := []string{}
	values := []interface{}{}
	idx := 1
	if req.Name != nil {
		fields = append(fields, fmt.Sprintf("name=$%d", idx)); values = append(values, *req.Name); idx++
	}
	if req.Description != nil {
		fields = append(fields, fmt.Sprintf("description=$%d", idx)); values = append(values, *req.Description); idx++
	}
	if req.AvatarURL != nil {
		fields = append(fields, fmt.Sprintf("avatar_url=$%d", idx)); values = append(values, *req.AvatarURL); idx++
	}
	if len(fields) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Nothing to update"})
		return
	}
	values = append(values, chatId)
	q := fmt.Sprintf("UPDATE chats SET %s WHERE id=$%d", joinStr(fields, ", "), idx)
	if _, err := h.db.Exec(ctx, q, values...); err != nil {
		log.Printf("[chats] updateGroup failed for chat %s: %v", chatId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Update group failed"})
		return
	}
	h.getChatById(c)
}

func (h *handler) leaveGroup(c *gin.Context) {
	userId := c.GetString("userId")
	chatId := c.Param("id")
	h.db.Exec(c.Request.Context(),
		"DELETE FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatId, userId)
	c.JSON(http.StatusOK, gin.H{"message": "Left"})
}

func (h *handler) getGroupMembers(c *gin.Context) {
	ctx := c.Request.Context()
	if h.memberRole(ctx, c.Param("id"), c.GetString("userId")) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}
	rows, err := h.db.Query(ctx,
		`SELECT u.id, u.username, u.display_name, u.avatar_url, u.is_online, cm.role, cm.joined_at
		 FROM chat_members cm JOIN users u ON cm.user_id=u.id
		 WHERE cm.chat_id=$1`, c.Param("id"),
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()
	members := []Member{}
	for rows.Next() {
		var m Member
		rows.Scan(&m.UserID, &m.Username, &m.DisplayName, &m.AvatarURL, &m.IsOnline, &m.Role, &m.JoinedAt)
		members = append(members, m)
	}
	c.JSON(http.StatusOK, members)
}

func (h *handler) addMember(c *gin.Context) {
	chatId := c.Param("id")
	ctx := c.Request.Context()
	if role := h.memberRole(ctx, chatId, c.GetString("userId")); role != "admin" && role != "superadmin" {
		c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
		return
	}
	var req struct {
		UserID string `json:"user_id" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "user_id required"})
		return
	}
	if _, err := h.db.Exec(ctx,
		"INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,'member') ON CONFLICT DO NOTHING",
		chatId, req.UserID,
	); err != nil {
		log.Printf("[chats] addMember failed for chat %s user %s: %v", chatId, req.UserID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Add member failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Added"})
}

func (h *handler) removeMember(c *gin.Context) {
	chatId := c.Param("id")
	ctx := c.Request.Context()
	if role := h.memberRole(ctx, chatId, c.GetString("userId")); role != "admin" && role != "superadmin" {
		c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
		return
	}
	if _, err := h.db.Exec(ctx,
		"DELETE FROM chat_members WHERE chat_id=$1 AND user_id=$2",
		chatId, c.Param("userId"),
	); err != nil {
		log.Printf("[chats] removeMember failed for chat %s user %s: %v", chatId, c.Param("userId"), err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Remove member failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Removed"})
}

func (h *handler) updateMemberRole(c *gin.Context) {
	chatId := c.Param("id")
	ctx := c.Request.Context()
	if role := h.memberRole(ctx, chatId, c.GetString("userId")); role != "admin" && role != "superadmin" {
		c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
		return
	}
	var req struct {
		Role string `json:"role" binding:"required,oneof=admin member"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": err.Error()})
		return
	}
	if _, err := h.db.Exec(ctx,
		"UPDATE chat_members SET role=$1 WHERE chat_id=$2 AND user_id=$3",
		req.Role, chatId, c.Param("userId"),
	); err != nil {
		log.Printf("[chats] updateMemberRole failed for chat %s user %s: %v", chatId, c.Param("userId"), err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Update role failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Updated"})
}

// ── Messages ──────────────────────────────────────────────────────────────────

func (h *handler) getMessages(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	ctx := c.Request.Context()
	if h.memberRole(ctx, chatId, userId) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}

	limit := 50
	if v, err := strconv.Atoi(c.Query("limit")); err == nil && v > 0 && v <= 100 {
		limit = v
	}
	offset := 0
	if v, err := strconv.Atoi(c.Query("offset")); err == nil && v >= 0 {
		offset = v
	}
	before := c.Query("before")
	around := c.Query("around")

	var rows interface {
		Next() bool
		Scan(...interface{}) error
		Close()
	}
	var err error

	baseQ := `
		SELECT m.id, m.chat_id, m.sender_id, m.type, m.content, m.is_read,
		       m.created_at, m.is_edited, m.is_deleted,
		       m.reply_to_id, m.reply_to_content, m.reply_to_sender_name,
		       m.duration, m.file_name, m.file_size, m.caption,
		       u.display_name AS sender_name,
		       COALESCE(
		         (SELECT json_agg(json_build_object('emoji', r.emoji, 'user_id', r.user_id))
		          FROM message_reactions r WHERE r.message_id=m.id),
		         '[]'::json
		       ) AS reactions
		FROM messages m
		JOIN users u ON m.sender_id=u.id
		LEFT JOIN message_deletions md ON md.message_id=m.id AND md.user_id=$1
		WHERE m.chat_id=$2 AND md.message_id IS NULL`

	switch {
	case around != "":
		rows, err = h.db.Query(c.Request.Context(),
			baseQ+` AND m.created_at <= (SELECT created_at FROM messages WHERE id=$3)
			ORDER BY m.created_at DESC LIMIT $4 OFFSET 0`,
			userId, chatId, around, limit)
	case before != "":
		rows, err = h.db.Query(c.Request.Context(),
			baseQ+` AND m.created_at < $3 ORDER BY m.created_at DESC LIMIT $4`,
			userId, chatId, before, limit)
	default:
		rows, err = h.db.Query(c.Request.Context(),
			baseQ+` ORDER BY m.created_at DESC LIMIT $3 OFFSET $4`,
			userId, chatId, limit, offset)
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	msgs := []Message{}
	for rows.Next() {
		var m Message
		var reactionsJSON []byte
		rows.Scan(&m.ID, &m.ChatID, &m.SenderID, &m.Type, &m.Content, &m.IsRead,
			&m.CreatedAt, &m.IsEdited, &m.IsDeleted,
			&m.ReplyToID, &m.ReplyToContent, &m.ReplyToSenderName,
			&m.Duration, &m.FileName, &m.FileSize, &m.Caption,
			&m.SenderName, &reactionsJSON)
		if reactionsJSON != nil {
			json.Unmarshal(reactionsJSON, &m.Reactions)
		}
		msgs = append(msgs, m)
	}
	c.JSON(http.StatusOK, msgs)
}

func (h *handler) sendMessage(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	ctx := c.Request.Context()
	if h.memberRole(ctx, chatId, userId) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}
	var req struct {
		Content   string  `json:"content" binding:"required"`
		Type      string  `json:"type"`
		ReplyToID *string `json:"reply_to_id"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}
	if req.Type == "" {
		req.Type = "text"
	}
	var replyContent, replySenderName *string
	if req.ReplyToID != nil {
		h.db.QueryRow(ctx,
			`SELECT m.content, u.display_name FROM messages m
			 JOIN users u ON m.sender_id=u.id WHERE m.id=$1`, *req.ReplyToID,
		).Scan(&replyContent, &replySenderName)
	}

	var msg Message
	err := h.db.QueryRow(ctx,
		`INSERT INTO messages (id, chat_id, sender_id, type, content, reply_to_id, reply_to_content, reply_to_sender_name)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
		 RETURNING id, chat_id, sender_id, type, content, is_read, created_at, is_edited, is_deleted, reply_to_id, reply_to_content, reply_to_sender_name`,
		uuid.NewString(), chatId, userId, req.Type, req.Content, req.ReplyToID, replyContent, replySenderName,
	).Scan(&msg.ID, &msg.ChatID, &msg.SenderID, &msg.Type, &msg.Content, &msg.IsRead,
		&msg.CreatedAt, &msg.IsEdited, &msg.IsDeleted,
		&msg.ReplyToID, &msg.ReplyToContent, &msg.ReplyToSenderName)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	c.JSON(http.StatusCreated, msg)
}

func (h *handler) editMessage(c *gin.Context) {
	chatId := c.Param("id")
	msgId := c.Param("msgId")
	userId := c.GetString("userId")
	var req struct {
		Content string `json:"content" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}

	ctx := c.Request.Context()
	if h.memberRole(ctx, chatId, userId) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}
	var senderId, msgType string
	h.db.QueryRow(ctx, "SELECT sender_id, type FROM messages WHERE id=$1 AND chat_id=$2", msgId, chatId).
		Scan(&senderId, &msgType)
	if senderId == "" {
		c.JSON(http.StatusNotFound, gin.H{"message": "Message not found"})
		return
	}
	if senderId != userId {
		c.JSON(http.StatusForbidden, gin.H{"message": "Not your message"})
		return
	}
	if msgType != "text" {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Only text messages can be edited"})
		return
	}

	var msg Message
	scanErr := h.db.QueryRow(ctx,
		`UPDATE messages SET content=$1, is_edited=true WHERE id=$2
		 RETURNING id, chat_id, sender_id, type, content, is_read, created_at, is_edited, is_deleted, reply_to_id, reply_to_content, reply_to_sender_name`,
		req.Content, msgId,
	).Scan(&msg.ID, &msg.ChatID, &msg.SenderID, &msg.Type, &msg.Content, &msg.IsRead,
		&msg.CreatedAt, &msg.IsEdited, &msg.IsDeleted,
		&msg.ReplyToID, &msg.ReplyToContent, &msg.ReplyToSenderName)
	if scanErr != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	var displayName string
	h.db.QueryRow(ctx, "SELECT display_name FROM users WHERE id=$1", msg.SenderID).Scan(&displayName)
	msg.SenderName = &displayName
	c.JSON(http.StatusOK, msg)
}

func (h *handler) deleteMessage(c *gin.Context) {
	chatId := c.Param("id")
	msgId := c.Param("msgId")
	userId := c.GetString("userId")
	forAll := c.Query("forAll") == "true"
	ctx := c.Request.Context()
	if h.memberRole(ctx, chatId, userId) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}

	if !forAll {
		if _, err := h.db.Exec(ctx,
			"INSERT INTO message_deletions (message_id, user_id) VALUES ($1,$2) ON CONFLICT DO NOTHING",
			msgId, userId); err != nil {
			log.Printf("[chats] deleteMessage (self) failed for msg %s user %s: %v", msgId, userId, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Delete message failed"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"message": "Deleted for me"})
		return
	}

	var senderId string
	h.db.QueryRow(ctx, "SELECT sender_id FROM messages WHERE id=$1 AND chat_id=$2", msgId, chatId).Scan(&senderId)
	if senderId == "" {
		c.JSON(http.StatusNotFound, gin.H{"message": "Message not found"})
		return
	}
	if senderId != userId {
		var role string
		h.db.QueryRow(ctx, "SELECT role FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatId, userId).Scan(&role)
		if role == "" || role == "member" {
			c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
			return
		}
	}

	if _, err := h.db.Exec(ctx, "DELETE FROM messages WHERE id=$1", msgId); err != nil {
		log.Printf("[chats] deleteMessage (all) failed for msg %s: %v", msgId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Delete message failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Deleted"})
}

// ── Pinned messages ───────────────────────────────────────────────────────────

func (h *handler) getPinnedMessage(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	ctx := c.Request.Context()
	if h.memberRole(ctx, chatId, userId) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}

	type PinResult struct {
		MessageID  string `json:"messageId"`
		Content    string `json:"content"`
		Type       string `json:"type"`
		SenderName string `json:"senderName"`
		ForAll     bool   `json:"forAll"`
	}

	scanPin := func(scope string) (*PinResult, error) {
		var p PinResult
		err := h.db.QueryRow(ctx,
			`SELECT p.message_id, m.content, m.type, u.display_name, p.scope='all'
			 FROM chat_pins p JOIN messages m ON p.message_id=m.id JOIN users u ON m.sender_id=u.id
			 WHERE p.chat_id=$1 AND p.scope=$2 LIMIT 1`,
			chatId, scope,
		).Scan(&p.MessageID, &p.Content, &p.Type, &p.SenderName, &p.ForAll)
		if err != nil {
			return nil, err
		}
		return &p, nil
	}

	pin, err := scanPin(userId)
	if err != nil {
		pin, err = scanPin("all")
	}
	if err != nil {
		c.JSON(http.StatusOK, nil)
		return
	}
	c.JSON(http.StatusOK, pin)
}

func (h *handler) pinMessage(c *gin.Context) {
	chatId := c.Param("id")
	msgId := c.Param("msgId")
	userId := c.GetString("userId")
	var req struct {
		ForAll bool `json:"forAll"`
	}
	c.ShouldBindJSON(&req)
	ctx := c.Request.Context()

	var role string
	h.db.QueryRow(ctx, "SELECT role FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatId, userId).Scan(&role)
	if role == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Not a member"})
		return
	}

	var chatType string
	h.db.QueryRow(ctx, "SELECT type FROM chats WHERE id=$1", chatId).Scan(&chatType)
	isGroup := chatType == "group"

	if isGroup && role == "member" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Only admins can pin in groups"})
		return
	}

	scope := userId
	if isGroup || req.ForAll {
		scope = "all"
	}

	if _, err := h.db.Exec(ctx,
		`INSERT INTO chat_pins (chat_id, scope, message_id, pinned_by) VALUES ($1,$2,$3,$4)
		 ON CONFLICT (chat_id, scope) DO UPDATE SET message_id=$3, pinned_by=$4, pinned_at=NOW()`,
		chatId, scope, msgId, userId); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to pin message"})
		return
	}

	type PinResponse struct {
		MessageID  string `json:"messageId"`
		Content    string `json:"content"`
		Type       string `json:"type"`
		SenderName string `json:"senderName"`
		ForAll     bool   `json:"forAll"`
	}
	var resp PinResponse
	resp.MessageID = msgId
	resp.ForAll = (scope == "all")
	h.db.QueryRow(ctx,
		`SELECT m.content, m.type, u.display_name
		 FROM messages m JOIN users u ON m.sender_id=u.id WHERE m.id=$1`, msgId,
	).Scan(&resp.Content, &resp.Type, &resp.SenderName)
	c.JSON(http.StatusOK, resp)
}

func (h *handler) unpinMessage(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	forAll := c.Query("forAll") == "true"
	ctx := c.Request.Context()
	if h.memberRole(ctx, chatId, userId) == "" {
		c.JSON(http.StatusForbidden, gin.H{"message": "Access denied"})
		return
	}

	if forAll {
		if _, err := h.db.Exec(ctx, "DELETE FROM chat_pins WHERE chat_id=$1 AND scope='all'", chatId); err != nil {
			log.Printf("[chats] unpinMessage (all) failed for chat %s: %v", chatId, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Unpin message failed"})
			return
		}
	} else {
		if _, err := h.db.Exec(ctx, "DELETE FROM chat_pins WHERE chat_id=$1 AND scope=$2", chatId, userId); err != nil {
			log.Printf("[chats] unpinMessage failed for chat %s user %s: %v", chatId, userId, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Unpin message failed"})
			return
		}
	}
	c.JSON(http.StatusOK, gin.H{"message": "Unpinned"})
}

// ── Helpers ───────────────────────────────────────────────────────────────────

func uniqueStr(s []string) []string {
	seen := map[string]bool{}
	result := []string{}
	for _, v := range s {
		if !seen[v] {
			seen[v] = true
			result = append(result, v)
		}
	}
	return result
}

func joinStr(s []string, sep string) string {
	r := ""
	for i, v := range s {
		if i > 0 {
			r += sep
		}
		r += v
	}
	return r
}
