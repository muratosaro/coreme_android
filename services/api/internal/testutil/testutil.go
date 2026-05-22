// Package testutil provides helpers shared across integration test suites.
// It connects to the real PostgreSQL database used in tests and exposes
// convenience functions for seeding and tearing down test data.
package testutil

import (
	"context"
	"fmt"
	"os"
	"sync/atomic"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
)

// dsn returns the connection string for the test database.
// Override with DATABASE_URL env var when needed.
func dsn() string {
	if v := os.Getenv("DATABASE_URL"); v != "" {
		return v
	}
	host := envOrDefault("DB_HOST", "localhost")
	port := envOrDefault("DB_PORT", "5432")
	name := envOrDefault("DB_NAME", "coreme")
	user := envOrDefault("DB_USER", "postgres")
	pass := envOrDefault("DB_PASSWORD", "postgres")
	return fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable", user, pass, host, port, name)
}

func envOrDefault(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

// Connect opens a pgxpool connection to the test database.
// If the database is unreachable the test is skipped (not failed).
func Connect(t *testing.T) *pgxpool.Pool {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	pool, err := pgxpool.New(ctx, dsn())
	if err != nil {
		t.Skipf("testutil: cannot connect to test DB (skip): %v", err)
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		t.Skipf("testutil: cannot ping test DB (skip): %v", err)
	}
	t.Cleanup(pool.Close)
	return pool
}

// counter is used to generate unique suffixes so parallel tests don't collide.
var counter int64

// UniqueID returns a short unique string safe to embed in names/emails.
// It is goroutine-safe.
func UniqueID() string {
	n := atomic.AddInt64(&counter, 1)
	return fmt.Sprintf("%d%d", n, time.Now().UnixNano()%1_000_000)
}

// CreateUser inserts a test user with a bcrypt-hashed password and returns
// the user's UUID.  The password argument is expected to be the plain-text
// password you want to use in tests; pass an already-hashed value via
// passwordHash if you need to bypass bcrypt overhead (see RegisterHash).
//
// t.Cleanup is registered to delete the user (and cascaded sessions/members)
// when the test finishes.
func CreateUser(t *testing.T, pool *pgxpool.Pool, username, email, passwordHash string) string {
	t.Helper()
	ctx := context.Background()

	var id string
	err := pool.QueryRow(ctx,
		`INSERT INTO users (id, username, display_name, email, password_hash)
		 VALUES (gen_random_uuid(), $1, $1, $2, $3)
		 RETURNING id`,
		username, email, passwordHash,
	).Scan(&id)
	if err != nil {
		t.Fatalf("testutil.CreateUser: %v", err)
	}

	t.Cleanup(func() {
		ctx2 := context.Background()
		pool.Exec(ctx2, "DELETE FROM sessions WHERE user_id=$1", id)
		pool.Exec(ctx2, "DELETE FROM chat_members WHERE user_id=$1", id)
		pool.Exec(ctx2, "DELETE FROM users WHERE id=$1", id)
	})

	return id
}

// CreateSession inserts a refresh-token session row and returns the session ID.
// t.Cleanup removes the row when the test finishes.
func CreateSession(t *testing.T, pool *pgxpool.Pool, userID, refreshToken string, expiresAt time.Time) string {
	t.Helper()
	ctx := context.Background()

	var id string
	err := pool.QueryRow(ctx,
		`INSERT INTO sessions (id, user_id, refresh_token, expires_at)
		 VALUES (gen_random_uuid(), $1, $2, $3)
		 RETURNING id`,
		userID, refreshToken, expiresAt,
	).Scan(&id)
	if err != nil {
		t.Fatalf("testutil.CreateSession: %v", err)
	}

	t.Cleanup(func() {
		pool.Exec(context.Background(), "DELETE FROM sessions WHERE id=$1", id)
	})

	return id
}

// DeleteUser forcibly removes a user by ID, ignoring errors.
// Useful in t.Cleanup closures where the user may already have been deleted.
func DeleteUser(pool *pgxpool.Pool, id string) {
	ctx := context.Background()
	pool.Exec(ctx, "DELETE FROM sessions WHERE user_id=$1", id)
	pool.Exec(ctx, "DELETE FROM chat_members WHERE user_id=$1", id)
	pool.Exec(ctx, "DELETE FROM users WHERE id=$1", id)
}

// InitTestDB opens a shared *pgxpool.Pool for use in TestMain.
// Returns nil (instead of failing) when the database is not reachable so that
// individual tests can skip themselves via skipIfNoDB.
func InitTestDB() *pgxpool.Pool {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	pool, err := pgxpool.New(ctx, dsn())
	if err != nil {
		return nil
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil
	}
	return pool
}

// SeedUser creates a user with a bcrypt MinCost hash (faster than production cost).
// Use for tests that do not exercise the login/register flow.
func SeedUser(t *testing.T, pool *pgxpool.Pool, username, email string) string {
	t.Helper()
	hash, err := bcrypt.GenerateFromPassword([]byte("testpass"), bcrypt.MinCost)
	if err != nil {
		t.Fatalf("testutil.SeedUser bcrypt: %v", err)
	}
	return CreateUser(t, pool, username, email, string(hash))
}

// CreateDirectChat inserts a direct chat between userA and userB and returns the chat ID.
func CreateDirectChat(t *testing.T, pool *pgxpool.Pool, userA, userB string) string {
	t.Helper()
	ctx := context.Background()
	var id string
	if err := pool.QueryRow(ctx,
		`INSERT INTO chats (id, type, created_by) VALUES (gen_random_uuid(), 'direct', $1) RETURNING id`,
		userA,
	).Scan(&id); err != nil {
		t.Fatalf("testutil.CreateDirectChat: %v", err)
	}
	pool.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id) VALUES ($1,$2)", id, userA)
	pool.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id) VALUES ($1,$2)", id, userB)
	t.Cleanup(func() { cleanupChat(pool, id) })
	return id
}

// CreateGroupChat inserts a group chat with createdBy as superadmin and returns the chat ID.
func CreateGroupChat(t *testing.T, pool *pgxpool.Pool, name, createdBy string) string {
	t.Helper()
	ctx := context.Background()
	var id string
	if err := pool.QueryRow(ctx,
		`INSERT INTO chats (id, type, name, created_by) VALUES (gen_random_uuid(), 'group', $1, $2) RETURNING id`,
		name, createdBy,
	).Scan(&id); err != nil {
		t.Fatalf("testutil.CreateGroupChat: %v", err)
	}
	pool.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,'superadmin')", id, createdBy)
	t.Cleanup(func() { cleanupChat(pool, id) })
	return id
}

// AddChatMember adds userID to chatID with the given role.
func AddChatMember(t *testing.T, pool *pgxpool.Pool, chatID, userID, role string) {
	t.Helper()
	if _, err := pool.Exec(context.Background(),
		`INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,$3) ON CONFLICT DO NOTHING`,
		chatID, userID, role,
	); err != nil {
		t.Fatalf("testutil.AddChatMember: %v", err)
	}
}

// CreateMessage inserts a text message into chatID sent by senderID and returns the message ID.
func CreateMessage(t *testing.T, pool *pgxpool.Pool, chatID, senderID, content string) string {
	t.Helper()
	ctx := context.Background()
	var id string
	if err := pool.QueryRow(ctx,
		`INSERT INTO messages (id, chat_id, sender_id, type, content)
		 VALUES (gen_random_uuid(), $1, $2, 'text', $3) RETURNING id`,
		chatID, senderID, content,
	).Scan(&id); err != nil {
		t.Fatalf("testutil.CreateMessage: %v", err)
	}
	t.Cleanup(func() {
		c := context.Background()
		pool.Exec(c, "DELETE FROM message_reactions WHERE message_id=$1", id)
		pool.Exec(c, "DELETE FROM message_deletions WHERE message_id=$1", id)
		pool.Exec(c, "DELETE FROM messages WHERE id=$1", id)
	})
	return id
}

// CreateCallRecord inserts a completed voice call and returns the record ID.
func CreateCallRecord(t *testing.T, pool *pgxpool.Pool, callerID, calleeID string) string {
	t.Helper()
	ctx := context.Background()
	var id string
	if err := pool.QueryRow(ctx,
		`INSERT INTO call_history (id, call_id, type, status, started_at, caller_id, callee_id)
		 VALUES (gen_random_uuid(), gen_random_uuid(), 'audio', 'completed', NOW(), $1, $2) RETURNING id`,
		callerID, calleeID,
	).Scan(&id); err != nil {
		t.Fatalf("testutil.CreateCallRecord: %v", err)
	}
	t.Cleanup(func() {
		pool.Exec(context.Background(), "DELETE FROM call_history WHERE id=$1", id)
	})
	return id
}

// CreateChannel inserts a channel (group+type_ext=channel) with createdBy as superadmin and returns the chat ID.
func CreateChannel(t *testing.T, pool *pgxpool.Pool, name, createdBy string) string {
	t.Helper()
	ctx := context.Background()
	var id string
	if err := pool.QueryRow(ctx,
		`INSERT INTO chats (id, type, type_ext, name, created_by)
		 VALUES (gen_random_uuid(), 'group', 'channel', $1, $2) RETURNING id`,
		name, createdBy,
	).Scan(&id); err != nil {
		t.Fatalf("testutil.CreateChannel: %v", err)
	}
	pool.Exec(ctx, "INSERT INTO chat_members (chat_id, user_id, role) VALUES ($1,$2,'superadmin')", id, createdBy)
	t.Cleanup(func() {
		pool.Exec(context.Background(), "DELETE FROM scheduled_posts WHERE chat_id=$1", id)
		cleanupChat(pool, id)
	})
	return id
}

// cleanupChat removes all DB rows associated with a chat in FK-safe order.
func cleanupChat(pool *pgxpool.Pool, chatID string) {
	ctx := context.Background()
	pool.Exec(ctx, "DELETE FROM message_reactions WHERE message_id IN (SELECT id FROM messages WHERE chat_id=$1)", chatID)
	pool.Exec(ctx, "DELETE FROM message_deletions WHERE message_id IN (SELECT id FROM messages WHERE chat_id=$1)", chatID)
	pool.Exec(ctx, "DELETE FROM chat_pins WHERE chat_id=$1", chatID)
	pool.Exec(ctx, "DELETE FROM messages WHERE chat_id=$1", chatID)
	pool.Exec(ctx, "DELETE FROM chat_members WHERE chat_id=$1", chatID)
	pool.Exec(ctx, "DELETE FROM chats WHERE id=$1", chatID)
}
