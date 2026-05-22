package media

import (
	"fmt"
	"math/rand"
	"net/http"
	"path/filepath"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

var allowedExt = map[string]bool{
	".jpg": true, ".jpeg": true, ".png": true, ".gif": true, ".webp": true,
	".mp4": true, ".mov": true, ".avi": true, ".m4v": true,
	".m4a": true, ".aac": true, ".mp3": true, ".ogg": true, ".wav": true,
	".pdf": true, ".doc": true, ".docx": true,
	".xls": true, ".xlsx": true, ".txt": true, ".zip": true,
}

func RegisterRoutes(r *gin.RouterGroup, uploadsDir string) {
	r.POST("/upload", func(c *gin.Context) {
		file, err := c.FormFile("file")
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"message": "No file uploaded"})
			return
		}

		ext := strings.ToLower(filepath.Ext(file.Filename))
		if !allowedExt[ext] {
			c.JSON(http.StatusBadRequest, gin.H{"message": "File type not allowed"})
			return
		}
		if file.Size > 50*1024*1024 {
			c.JSON(http.StatusBadRequest, gin.H{"message": "File too large (max 50MB)"})
			return
		}

		filename := fmt.Sprintf("%d-%d%s", time.Now().UnixMilli(), rand.Int63n(1e9), ext)
		dst := filepath.Join(uploadsDir, filename)
		if err := c.SaveUploadedFile(file, dst); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"message": "Failed to save file"})
			return
		}

		scheme := c.GetHeader("X-Forwarded-Proto")
		if scheme == "" {
			if c.Request.TLS != nil {
				scheme = "https"
			} else {
				scheme = "http"
			}
		}
		host := c.GetHeader("X-Forwarded-Host")
		if host == "" {
			host = c.Request.Host
		}
		url := fmt.Sprintf("%s://%s/uploads/%s", scheme, host, filename)
		c.JSON(http.StatusOK, gin.H{
			"url":      url,
			"filename": file.Filename,
			"size":     file.Size,
		})
	})
}
