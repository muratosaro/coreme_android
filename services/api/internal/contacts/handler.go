package contacts

import (
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
)

type handler struct{ db *pgxpool.Pool }

func RegisterRoutes(r *gin.RouterGroup, db *pgxpool.Pool) {
	h := &handler{db}
	r.GET("", h.getContacts)
	r.POST("", h.addContact)
	r.PATCH("/:id", h.updateNickname)
	r.DELETE("/:id", h.removeContact)
}

type Contact struct {
	ID          string     `json:"id"`
	Nickname    *string    `json:"nickname"`
	CreatedAt   time.Time  `json:"created_at"`
	Username    string     `json:"username"`
	DisplayName string     `json:"display_name"`
	AvatarURL   *string    `json:"avatar_url"`
	IsOnline    bool       `json:"is_online"`
	LastSeen    *time.Time `json:"last_seen"`
	Bio         *string    `json:"bio"`
}

func (h *handler) getContacts(c *gin.Context) {
	userId := c.GetString("userId")
	rows, err := h.db.Query(c.Request.Context(),
		`SELECT c.contact_id, c.nickname, c.created_at,
		        u.username, u.display_name, u.avatar_url, u.is_online, u.last_seen, u.bio
		 FROM contacts c
		 JOIN users u ON c.contact_id = u.id
		 WHERE c.user_id=$1
		 ORDER BY COALESCE(c.nickname, u.display_name)`, userId,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	result := []Contact{}
	for rows.Next() {
		var ct Contact
		rows.Scan(&ct.ID, &ct.Nickname, &ct.CreatedAt,
			&ct.Username, &ct.DisplayName, &ct.AvatarURL, &ct.IsOnline, &ct.LastSeen, &ct.Bio)
		result = append(result, ct)
	}
	c.JSON(http.StatusOK, result)
}

func (h *handler) addContact(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		ContactID string  `json:"contact_id" binding:"required"`
		Nickname  *string `json:"nickname"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "contact_id required"})
		return
	}
	if req.ContactID == userId {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Cannot add yourself"})
		return
	}

	ctx := c.Request.Context()
	_, err := h.db.Exec(ctx,
		`INSERT INTO contacts (user_id, contact_id, nickname)
		 VALUES ($1, $2, $3)
		 ON CONFLICT (user_id, contact_id) DO UPDATE SET nickname = EXCLUDED.nickname`,
		userId, req.ContactID, req.Nickname,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}

	var ct Contact
	h.db.QueryRow(ctx,
		`SELECT c.contact_id, c.nickname, c.created_at,
		        u.username, u.display_name, u.avatar_url, u.is_online, u.last_seen, u.bio
		 FROM contacts c JOIN users u ON c.contact_id = u.id
		 WHERE c.user_id=$1 AND c.contact_id=$2`,
		userId, req.ContactID,
	).Scan(&ct.ID, &ct.Nickname, &ct.CreatedAt,
		&ct.Username, &ct.DisplayName, &ct.AvatarURL, &ct.IsOnline, &ct.LastSeen, &ct.Bio)

	c.JSON(http.StatusCreated, ct)
}

func (h *handler) updateNickname(c *gin.Context) {
	userId := c.GetString("userId")
	contactId := c.Param("id")
	var req struct {
		Nickname *string `json:"nickname"`
	}
	c.ShouldBindJSON(&req)
	if _, err := h.db.Exec(c.Request.Context(),
		"UPDATE contacts SET nickname=$1 WHERE user_id=$2 AND contact_id=$3",
		req.Nickname, userId, contactId,
	); err != nil {
		log.Printf("[contacts] updateNickname failed for user %s contact %s: %v", userId, contactId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Update nickname failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Updated"})
}

func (h *handler) removeContact(c *gin.Context) {
	userId := c.GetString("userId")
	if _, err := h.db.Exec(c.Request.Context(),
		"DELETE FROM contacts WHERE user_id=$1 AND contact_id=$2",
		userId, c.Param("id"),
	); err != nil {
		log.Printf("[contacts] removeContact failed for user %s: %v", userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Remove contact failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Removed"})
}
