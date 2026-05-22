package media_test

import (
	"bytes"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"

	"github.com/gin-gonic/gin"

	"github.com/muratosaro/coreme/api/internal/media"
)

func TestMain(m *testing.M) {
	gin.SetMode(gin.TestMode)
	os.Exit(m.Run())
}

func newRouter(t *testing.T) *gin.Engine {
	t.Helper()
	r := gin.New()
	media.RegisterRoutes(r.Group("/media"), t.TempDir())
	return r
}

// doUpload sends a multipart/form-data POST with a single "file" field.
func doUpload(t *testing.T, router *gin.Engine, filename string, content []byte) *httptest.ResponseRecorder {
	t.Helper()
	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)
	part, err := writer.CreateFormFile("file", filename)
	if err != nil {
		t.Fatalf("CreateFormFile: %v", err)
	}
	part.Write(content)
	writer.Close()

	req := httptest.NewRequest(http.MethodPost, "/media/upload", body)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)
	return w
}

func jsonBody(t *testing.T, w *httptest.ResponseRecorder) map[string]interface{} {
	t.Helper()
	var m map[string]interface{}
	if err := json.NewDecoder(w.Body).Decode(&m); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	return m
}

// ── POST /media/upload ────────────────────────────────────────────────────────

func TestUpload_NoFile(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "/media/upload", strings.NewReader(""))
	req.Header.Set("Content-Type", "multipart/form-data; boundary=boundary")
	w := httptest.NewRecorder()
	newRouter(t).ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d: %s", w.Code, w.Body.String())
	}
}

func TestUpload_DisallowedExtension(t *testing.T) {
	w := doUpload(t, newRouter(t), "script.exe", []byte("malicious"))
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400 for .exe, got %d: %s", w.Code, w.Body.String())
	}
}

func TestUpload_DisallowedExtension_PHP(t *testing.T) {
	w := doUpload(t, newRouter(t), "shell.php", []byte("<?php echo 'pwned'; ?>"))
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400 for .php, got %d: %s", w.Code, w.Body.String())
	}
}

func TestUpload_FileTooLarge(t *testing.T) {
	// 50 MB + 1 byte exceeds the 50 MB limit enforced by the handler.
	largeContent := make([]byte, 50*1024*1024+1)
	w := doUpload(t, newRouter(t), "big.jpg", largeContent)
	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400 for oversized file, got %d: %s", w.Code, w.Body.String())
	}
}

func TestUpload_Success_Image(t *testing.T) {
	w := doUpload(t, newRouter(t), "photo.jpg", []byte("fake-jpeg-content"))
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
	body := jsonBody(t, w)
	if body["url"] == nil {
		t.Error("expected url in response")
	}
	if body["filename"] != "photo.jpg" {
		t.Errorf("expected filename=photo.jpg, got %v", body["filename"])
	}
	if body["size"] == nil {
		t.Error("expected size in response")
	}
}

func TestUpload_Success_PDF(t *testing.T) {
	w := doUpload(t, newRouter(t), "doc.pdf", []byte("%PDF-1.4 fake"))
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
}

func TestUpload_Success_Video(t *testing.T) {
	w := doUpload(t, newRouter(t), "clip.mp4", []byte("fake-mp4"))
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d: %s", w.Code, w.Body.String())
	}
}
