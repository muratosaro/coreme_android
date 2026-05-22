package channels

import (
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type handler struct{ db *pgxpool.Pool }

func RegisterRoutes(r *gin.RouterGroup, db *pgxpool.Pool) {
	h := &handler{db}
	r.GET("", h.getChannels)
	r.POST("", h.createChannel)
	r.GET("/:id/scheduled", h.getScheduledPosts)
	r.POST("/:id/scheduled", h.createScheduledPost)
	r.DELETE("/:id/scheduled/:postId", h.deleteScheduledPost)
}

type Channel struct {
	ID              string      `json:"id"`
	Name            string      `json:"name"`
	Description     *string     `json:"description"`
	AvatarURL       *string     `json:"avatar_url"`
	CreatedAt       time.Time   `json:"created_at"`
	UsernameHandle  *string     `json:"username_handle"`
	SubscriberCount int         `json:"subscriber_count"`
	Role            string      `json:"role"`
	LastPost        interface{} `json:"last_post"`
}

type ScheduledPost struct {
	ID          string    `json:"id"`
	ChatID      string    `json:"chat_id"`
	CreatedBy   string    `json:"created_by"`
	Content     string    `json:"content"`
	Type        string    `json:"type"`
	ScheduledAt time.Time `json:"scheduled_at"`
	Sent        bool      `json:"sent"`
}

func (h *handler) getChannels(c *gin.Context) {
	userId := c.GetString("userId")
	rows, err := h.db.Query(c.Request.Context(),
		`SELECT c.id, c.name, c.description, c.avatar_url, c.created_at,
		        c.username_handle, COALESCE(c.subscriber_count, 0),
		        cm.role
		 FROM chats c
		 JOIN chat_members cm ON c.id=cm.chat_id AND cm.user_id=$1
		 WHERE c.type='group' AND c.type_ext='channel'
		 ORDER BY c.created_at DESC`, userId,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	result := []Channel{}
	for rows.Next() {
		var ch Channel
		rows.Scan(&ch.ID, &ch.Name, &ch.Description, &ch.AvatarURL, &ch.CreatedAt,
			&ch.UsernameHandle, &ch.SubscriberCount, &ch.Role)
		result = append(result, ch)
	}
	c.JSON(http.StatusOK, result)
}

func (h *handler) createChannel(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		Name           string  `json:"name" binding:"required"`
		Description    *string `json:"description"`
		UsernameHandle *string `json:"username_handle"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "name required"})
		return
	}

	ctx := c.Request.Context()
	if req.UsernameHandle != nil {
		var existing int
		h.db.QueryRow(ctx, "SELECT COUNT(*) FROM chats WHERE username_handle=$1", *req.UsernameHandle).Scan(&existing)
		if existing > 0 {
			c.JSON(http.StatusConflict, gin.H{"message": "Username handle already taken"})
			return
		}
	}

	chatId := uuid.NewString()
	if _, err := h.db.Exec(ctx,
		`INSERT INTO chats (id, type, type_ext, name, description, username_handle, created_by)
		 VALUES ($1,'group','channel',$2,$3,$4,$5)`,
		chatId, req.Name, req.Description, req.UsernameHandle, userId,
	); err != nil {
		log.Printf("[channels] createChannel failed for user %s: %v", userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Create channel failed"})
		return
	}
	if _, err := h.db.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,'superadmin')", chatId, userId); err != nil {
		log.Printf("[channels] addSuperadmin failed for channel %s user %s: %v", chatId, userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Create channel failed"})
		return
	}

	var ch Channel
	h.db.QueryRow(ctx,
		`SELECT id, name, description, avatar_url, created_at, username_handle, COALESCE(subscriber_count,0)
		 FROM chats WHERE id=$1`, chatId,
	).Scan(&ch.ID, &ch.Name, &ch.Description, &ch.AvatarURL, &ch.CreatedAt, &ch.UsernameHandle, &ch.SubscriberCount)
	ch.Role = "superadmin"
	c.JSON(http.StatusCreated, ch)
}

func (h *handler) getScheduledPosts(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	ctx := c.Request.Context()

	var role string
	h.db.QueryRow(ctx, "SELECT role FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatId, userId).Scan(&role)
	if role == "" || role == "member" {
		c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
		return
	}

	rows, err := h.db.Query(ctx,
		"SELECT id, chat_id, created_by, content, type, scheduled_at, sent FROM scheduled_posts WHERE chat_id=$1 AND sent=false ORDER BY scheduled_at ASC",
		chatId,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	result := []ScheduledPost{}
	for rows.Next() {
		var p ScheduledPost
		rows.Scan(&p.ID, &p.ChatID, &p.CreatedBy, &p.Content, &p.Type, &p.ScheduledAt, &p.Sent)
		result = append(result, p)
	}
	c.JSON(http.StatusOK, result)
}

func (h *handler) createScheduledPost(c *gin.Context) {
	chatId := c.Param("id")
	userId := c.GetString("userId")
	var req struct {
		Content     string    `json:"content" binding:"required"`
		Type        string    `json:"type"`
		ScheduledAt time.Time `json:"scheduled_at" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "content and scheduled_at required"})
		return
	}
	if req.Type == "" {
		req.Type = "text"
	}

	ctx := c.Request.Context()
	var role string
	h.db.QueryRow(ctx, "SELECT role FROM chat_members WHERE chat_id=$1 AND user_id=$2", chatId, userId).Scan(&role)
	if role == "" || role == "member" {
		c.JSON(http.StatusForbidden, gin.H{"message": "No permission"})
		return
	}

	var p ScheduledPost
	err := h.db.QueryRow(ctx,
		`INSERT INTO scheduled_posts (chat_id, created_by, content, type, scheduled_at)
		 VALUES ($1,$2,$3,$4,$5) RETURNING id, chat_id, created_by, content, type, scheduled_at, sent`,
		chatId, userId, req.Content, req.Type, req.ScheduledAt,
	).Scan(&p.ID, &p.ChatID, &p.CreatedBy, &p.Content, &p.Type, &p.ScheduledAt, &p.Sent)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	c.JSON(http.StatusCreated, p)
}

func (h *handler) deleteScheduledPost(c *gin.Context) {
	if _, err := h.db.Exec(c.Request.Context(),
		"DELETE FROM scheduled_posts WHERE id=$1 AND chat_id=$2 AND created_by=$3",
		c.Param("postId"), c.Param("id"), c.GetString("userId"),
	); err != nil {
		log.Printf("[channels] deleteScheduledPost failed: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Delete scheduled post failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Deleted"})
}
