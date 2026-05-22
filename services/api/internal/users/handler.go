package users

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
)

type handler struct{ db *pgxpool.Pool }

func RegisterRoutes(r *gin.RouterGroup, db *pgxpool.Pool) {
	h := &handler{db}
	r.GET("/me", h.getMe)
	r.PATCH("/me", h.updateMe)
	r.GET("/search", h.searchUsers)
	r.GET("/:id", h.getUserById)
}

type User struct {
	ID          string     `json:"id"`
	Username    string     `json:"username"`
	DisplayName string     `json:"display_name"`
	Email       *string    `json:"email,omitempty"`
	Bio         *string    `json:"bio"`
	AvatarURL   *string    `json:"avatar_url"`
	IsOnline    bool       `json:"is_online"`
	LastSeen    *time.Time `json:"last_seen,omitempty"`
	CreatedAt   *time.Time `json:"created_at,omitempty"`
}

func (h *handler) getMe(c *gin.Context) {
	userId := c.GetString("userId")
	var u User
	err := h.db.QueryRow(c.Request.Context(),
		`SELECT id, username, display_name, email, bio, avatar_url, is_online, last_seen, created_at
		 FROM users WHERE id=$1`, userId,
	).Scan(&u.ID, &u.Username, &u.DisplayName, &u.Email,
		&u.Bio, &u.AvatarURL, &u.IsOnline, &u.LastSeen, &u.CreatedAt)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"message": "User not found"})
		return
	}
	c.JSON(http.StatusOK, u)
}

func (h *handler) updateMe(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		DisplayName *string `json:"display_name"`
		Bio         *string `json:"bio"`
		Username    *string `json:"username"`
		AvatarURL   *string `json:"avatar_url"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}

	parts := []string{}
	values := []interface{}{}
	idx := 1

	if req.DisplayName != nil {
		parts = append(parts, "display_name=$"+strconv.Itoa(idx))
		values = append(values, *req.DisplayName)
		idx++
	}
	if req.Bio != nil {
		parts = append(parts, "bio=$"+strconv.Itoa(idx))
		values = append(values, *req.Bio)
		idx++
	}
	if req.Username != nil {
		parts = append(parts, "username=$"+strconv.Itoa(idx))
		values = append(values, *req.Username)
		idx++
	}
	if req.AvatarURL != nil {
		parts = append(parts, "avatar_url=$"+strconv.Itoa(idx))
		values = append(values, *req.AvatarURL)
		idx++
	}

	if len(parts) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Nothing to update"})
		return
	}

	values = append(values, userId)
	query := "UPDATE users SET " + strings.Join(parts, ", ") +
		" WHERE id=$" + strconv.Itoa(idx) +
		" RETURNING id, username, display_name, email, bio, avatar_url, is_online"

	var u User
	err := h.db.QueryRow(c.Request.Context(), query, values...).
		Scan(&u.ID, &u.Username, &u.DisplayName, &u.Email, &u.Bio, &u.AvatarURL, &u.IsOnline)
	if err != nil {
		if strings.Contains(err.Error(), "23505") || strings.Contains(err.Error(), "unique") {
			c.JSON(http.StatusConflict, gin.H{"message": "Username already taken"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	c.JSON(http.StatusOK, u)
}

func (h *handler) searchUsers(c *gin.Context) {
	q := c.Query("q")
	if len(q) < 2 {
		c.JSON(http.StatusOK, []User{})
		return
	}
	userId := c.GetString("userId")

	rows, err := h.db.Query(c.Request.Context(),
		`SELECT id, username, display_name, bio, avatar_url, is_online
		 FROM users
		 WHERE (username ILIKE $1 OR display_name ILIKE $1) AND id!=$2
		 LIMIT 20`,
		"%"+q+"%", userId,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	result := []User{}
	for rows.Next() {
		var u User
		rows.Scan(&u.ID, &u.Username, &u.DisplayName, &u.Bio, &u.AvatarURL, &u.IsOnline)
		result = append(result, u)
	}
	c.JSON(http.StatusOK, result)
}

func (h *handler) getUserById(c *gin.Context) {
	var u User
	err := h.db.QueryRow(c.Request.Context(),
		`SELECT id, username, display_name, bio, avatar_url, is_online, last_seen
		 FROM users WHERE id=$1`, c.Param("id"),
	).Scan(&u.ID, &u.Username, &u.DisplayName, &u.Bio, &u.AvatarURL, &u.IsOnline, &u.LastSeen)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"message": "User not found"})
		return
	}
	c.JSON(http.StatusOK, u)
}
