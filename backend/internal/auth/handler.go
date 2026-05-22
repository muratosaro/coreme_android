package auth

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
)

type handler struct{ db *pgxpool.Pool }

func RegisterRoutes(r *gin.RouterGroup, db *pgxpool.Pool) {
	h := &handler{db}
	r.POST("/register", h.register)
	r.POST("/login", h.login)
	r.POST("/refresh", h.refresh)
	r.POST("/logout", h.logout)
}

func generateAccessToken(userId string) (string, error) {
	return jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": userId,
		"exp":    time.Now().Add(15 * time.Minute).Unix(),
	}).SignedString([]byte(os.Getenv("JWT_SECRET")))
}

func generateRefreshToken(userId string) (string, error) {
	return jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userId": userId,
		"jti":    uuid.NewString(),
		"exp":    time.Now().Add(30 * 24 * time.Hour).Unix(),
	}).SignedString([]byte(os.Getenv("JWT_REFRESH_SECRET")))
}

func (h *handler) saveRefreshToken(ctx context.Context, userId, token string) error {
	expiresAt := time.Now().Add(30 * 24 * time.Hour)
	_, err := h.db.Exec(ctx,
		"INSERT INTO sessions (id, user_id, refresh_token, expires_at) VALUES ($1, $2, $3, $4)",
		uuid.NewString(), userId, token, expiresAt,
	)
	return err
}

type User struct {
	ID          string     `json:"id"`
	Username    string     `json:"username"`
	DisplayName string     `json:"display_name"`
	Email       string     `json:"email"`
	Bio         *string    `json:"bio"`
	AvatarURL   *string    `json:"avatar_url"`
	IsOnline    bool       `json:"is_online"`
	LastSeen    *time.Time `json:"last_seen"`
	CreatedAt   time.Time  `json:"created_at"`
}

func (h *handler) register(c *gin.Context) {
	var req struct {
		Username    string `json:"username" binding:"required,min=3,max=50"`
		DisplayName string `json:"display_name" binding:"required,min=1,max=100"`
		Email       string `json:"email" binding:"required,email"`
		Password    string `json:"password" binding:"required,min=6"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}

	ctx := c.Request.Context()

	var count int
	h.db.QueryRow(ctx, "SELECT COUNT(*) FROM users WHERE username=$1 OR email=$2",
		req.Username, req.Email).Scan(&count)
	if count > 0 {
		c.JSON(http.StatusConflict, gin.H{"message": "Username or email already taken"})
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), 12)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}

	userId := uuid.NewString()
	var user User
	err = h.db.QueryRow(ctx,
		`INSERT INTO users (id, username, display_name, email, password_hash)
		 VALUES ($1, $2, $3, $4, $5)
		 RETURNING id, username, display_name, email, bio, avatar_url, is_online, last_seen, created_at`,
		userId, req.Username, req.DisplayName, req.Email, string(hash),
	).Scan(&user.ID, &user.Username, &user.DisplayName, &user.Email,
		&user.Bio, &user.AvatarURL, &user.IsOnline, &user.LastSeen, &user.CreatedAt)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}

	accessToken, _ := generateAccessToken(user.ID)
	refreshToken, _ := generateRefreshToken(user.ID)
	h.saveRefreshToken(ctx, user.ID, refreshToken)

	c.JSON(http.StatusCreated, gin.H{"accessToken": accessToken, "refreshToken": refreshToken, "user": user})
}

func (h *handler) login(c *gin.Context) {
	var req struct {
		Username string `json:"username" binding:"required"`
		Password string `json:"password" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"message": err.Error()})
		return
	}

	ctx := c.Request.Context()

	var user User
	var passwordHash string
	err := h.db.QueryRow(ctx,
		`SELECT id, username, display_name, email, bio, avatar_url, is_online, last_seen, created_at, password_hash
		 FROM users WHERE username=$1`, req.Username,
	).Scan(&user.ID, &user.Username, &user.DisplayName, &user.Email,
		&user.Bio, &user.AvatarURL, &user.IsOnline, &user.LastSeen, &user.CreatedAt, &passwordHash)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"message": "Invalid credentials"})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(req.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"message": "Invalid credentials"})
		return
	}

	if _, err := h.db.Exec(ctx, "UPDATE users SET is_online=true, last_seen=NOW() WHERE id=$1", user.ID); err != nil {
		log.Printf("[auth] update is_online on login failed for user %s: %v", user.ID, err)
	}
	user.IsOnline = true

	accessToken, _ := generateAccessToken(user.ID)
	refreshToken, _ := generateRefreshToken(user.ID)
	h.saveRefreshToken(ctx, user.ID, refreshToken)

	c.JSON(http.StatusOK, gin.H{"accessToken": accessToken, "refreshToken": refreshToken, "user": user})
}

func (h *handler) refresh(c *gin.Context) {
	var req struct {
		RefreshToken string `json:"refreshToken" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Refresh token required"})
		return
	}

	token, err := jwt.Parse(req.RefreshToken, func(t *jwt.Token) (interface{}, error) {
		return []byte(os.Getenv("JWT_REFRESH_SECRET")), nil
	})
	if err != nil || !token.Valid {
		c.JSON(http.StatusUnauthorized, gin.H{"message": "Refresh token invalid or expired"})
		return
	}

	claims, _ := token.Claims.(jwt.MapClaims)
	userId, _ := claims["userId"].(string)

	ctx := c.Request.Context()
	var sessionId string
	err = h.db.QueryRow(ctx,
		"SELECT id FROM sessions WHERE refresh_token=$1 AND expires_at>NOW()",
		req.RefreshToken,
	).Scan(&sessionId)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"message": "Refresh token invalid or expired"})
		return
	}

	newRefreshToken, err := generateRefreshToken(userId)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}

	newExpiry := time.Now().UTC().Add(30 * 24 * time.Hour)
	if _, err = h.db.Exec(ctx,
		"UPDATE sessions SET refresh_token=$1, expires_at=$2 WHERE id=$3",
		newRefreshToken, newExpiry, sessionId,
	); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}

	accessToken, _ := generateAccessToken(userId)
	c.JSON(http.StatusOK, gin.H{"accessToken": accessToken, "refreshToken": newRefreshToken})
}

func (h *handler) logout(c *gin.Context) {
	var req struct {
		RefreshToken string `json:"refreshToken" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "Refresh token required"})
		return
	}

	ctx := c.Request.Context()
	var userId string
	h.db.QueryRow(ctx,
		"DELETE FROM sessions WHERE refresh_token=$1 RETURNING user_id",
		req.RefreshToken,
	).Scan(&userId)

	if userId != "" {
		if _, err := h.db.Exec(ctx, "UPDATE users SET is_online=false, last_seen=NOW() WHERE id=$1", userId); err != nil {
			log.Printf("[auth] update is_online on logout failed for user %s: %v", userId, err)
		}
	}

	c.JSON(http.StatusOK, gin.H{"message": "Logged out successfully"})
}
