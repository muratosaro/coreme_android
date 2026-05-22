package calls

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
	r.GET("/history", h.getHistory)
	r.DELETE("/history/:id", h.deleteRecord)
}

type CallRecord struct {
	ID              string     `json:"id"`
	CallID          string     `json:"call_id"`
	Type            string     `json:"type"`
	Status          string     `json:"status"`
	StartedAt       time.Time  `json:"started_at"`
	EndedAt         *time.Time `json:"ended_at"`
	DurationSeconds *int       `json:"duration_seconds"`
	CallerID        string     `json:"caller_id"`
	CalleeID        string     `json:"callee_id"`
	CallerName      string     `json:"caller_name"`
	CallerAvatar    *string    `json:"caller_avatar"`
	CalleeName      string     `json:"callee_name"`
	CalleeAvatar    *string    `json:"callee_avatar"`
}

func (h *handler) getHistory(c *gin.Context) {
	userId := c.GetString("userId")
	rows, err := h.db.Query(c.Request.Context(),
		`SELECT ch.id, ch.call_id, ch.type, ch.status,
		        ch.started_at, ch.ended_at, ch.duration_seconds,
		        ch.caller_id, ch.callee_id,
		        uc.display_name, uc.avatar_url,
		        ue.display_name, ue.avatar_url
		 FROM call_history ch
		 JOIN users uc ON ch.caller_id = uc.id
		 JOIN users ue ON ch.callee_id = ue.id
		 WHERE ch.caller_id=$1 OR ch.callee_id=$1
		 ORDER BY ch.started_at DESC
		 LIMIT 50`, userId,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "Internal server error"})
		return
	}
	defer rows.Close()

	result := []CallRecord{}
	for rows.Next() {
		var r CallRecord
		rows.Scan(&r.ID, &r.CallID, &r.Type, &r.Status,
			&r.StartedAt, &r.EndedAt, &r.DurationSeconds,
			&r.CallerID, &r.CalleeID,
			&r.CallerName, &r.CallerAvatar,
			&r.CalleeName, &r.CalleeAvatar)
		result = append(result, r)
	}
	c.JSON(http.StatusOK, result)
}

func (h *handler) deleteRecord(c *gin.Context) {
	userId := c.GetString("userId")
	if _, err := h.db.Exec(c.Request.Context(),
		"DELETE FROM call_history WHERE id=$1 AND (caller_id=$2 OR callee_id=$2)",
		c.Param("id"), userId,
	); err != nil {
		log.Printf("[calls] deleteRecord failed for user %s: %v", userId, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Delete record failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "Deleted"})
}
