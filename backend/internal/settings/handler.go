package settings

import (
	"log"
	"net/http"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
)

type handler struct{ db *pgxpool.Pool }

func RegisterRoutes(r *gin.RouterGroup, db *pgxpool.Pool) {
	h := &handler{db}
	r.GET("", h.getSettings)
	r.PATCH("", h.updateSettings)
	r.POST("/change-password", h.changePassword)
	r.POST("/fcm", h.saveFcmToken)
}

type Settings struct {
	UserID              string  `json:"user_id"`
	AutoReplyEnabled    bool    `json:"auto_reply_enabled"`
	AutoReplyMessage    *string `json:"auto_reply_message"`
	NotificationsEnabled bool   `json:"notifications_enabled"`
	SoundEnabled        bool    `json:"sound_enabled"`
	ShowReadReceipts    bool    `json:"show_read_receipts"`
	LastSeenVisible     bool    `json:"last_seen_visible"`
	Theme               string  `json:"theme"`
	FontSize            string  `json:"font_size"`
	ChatBg              *string `json:"chat_bg"`
}

func (h *handler) getSettings(c *gin.Context) {
	userId := c.GetString("userId")
	ctx := c.Request.Context()

	if _, err := h.db.Exec(ctx, "INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING", userId); err != nil {
		log.Printf("[settings] default settings upsert failed for user %s: %v", userId, err)
	}

	var s Settings
	err := h.db.QueryRow(ctx,
		`SELECT user_id, auto_reply_enabled, auto_reply_message,
		        notifications_enabled, sound_enabled,
		        show_read_receipts, last_seen_visible,
		        theme, font_size, chat_bg
		 FROM user_settings WHERE user_id=$1`, userId,
	).Scan(&s.UserID, &s.AutoReplyEnabled, &s.AutoReplyMessage,
		&s.NotificationsEnabled, &s.SoundEnabled,
		&s.ShowReadReceipts, &s.LastSeenVisible,
		&s.Theme, &s.FontSize, &s.ChatBg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	c.JSON(http.StatusOK, s)
}

func (h *handler) updateSettings(c *gin.Context) {
	userId := c.GetString("userId")
	var body map[string]interface{}
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Invalid body"})
		return
	}

	allowed := []string{
		"auto_reply_enabled", "auto_reply_message",
		"notifications_enabled", "sound_enabled",
		"show_read_receipts", "last_seen_visible",
		"theme", "font_size", "chat_bg",
	}

	fields := []string{}
	values := []interface{}{}
	idx := 1
	for _, key := range allowed {
		if val, ok := body[key]; ok {
			fields = append(fields, key+"=$"+strconv.Itoa(idx))
			values = append(values, val)
			idx++
		}
	}
	if len(fields) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Nothing to update"})
		return
	}
	fields = append(fields, "updated_at=NOW()")

	ctx := c.Request.Context()
	values = append(values, userId)
	if _, err := h.db.Exec(ctx, "INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING", userId); err != nil {
		log.Printf("[settings] default settings upsert failed for user %s: %v", userId, err)
	}
	if _, err := h.db.Exec(ctx, "UPDATE user_settings SET "+strings.Join(fields, ", ")+" WHERE user_id=$"+strconv.Itoa(idx), values...); err != nil {
		log.Printf("[settings] updateSettings failed for user %s: %v", userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Update settings failed"})
		return
	}

	h.getSettings(c)
}

func (h *handler) changePassword(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		CurrentPassword string `json:"current_password" binding:"required"`
		NewPassword     string `json:"new_password" binding:"required,min=6"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": err.Error()})
		return
	}

	ctx := c.Request.Context()
	var hash string
	err := h.db.QueryRow(ctx, "SELECT password_hash FROM users WHERE id=$1", userId).Scan(&hash)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"message": "User not found"})
		return
	}
	if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(req.CurrentPassword)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"message": "Current password is incorrect"})
		return
	}

	newHash, err := bcrypt.GenerateFromPassword([]byte(req.NewPassword), 12)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Password hashing failed"})
		return
	}
	if _, err = h.db.Exec(ctx, "UPDATE users SET password_hash=$1 WHERE id=$2", string(newHash), userId); err != nil {
		log.Printf("[settings] changePassword failed for user %s: %v", userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Change password failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Password changed successfully"})
}

func (h *handler) saveFcmToken(c *gin.Context) {
	userId := c.GetString("userId")
	var req struct {
		Token string `json:"token" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "token required"})
		return
	}
	if _, err := h.db.Exec(c.Request.Context(), "UPDATE users SET fcm_token=$1 WHERE id=$2", req.Token, userId); err != nil {
		log.Printf("[settings] saveFcmToken failed for user %s: %v", userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Save FCM token failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Token saved"})
}
