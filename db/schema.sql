-- CoreMe database schema
-- Logical sections mirror microservice boundaries:
--   auth   → users, sessions
--   chat   → chats, chat_members, messages, message_reactions, scheduled_posts
--   calls  → call_history
--   media  → (files stored on disk; metadata on messages.content / media table future)
--   settings → user_settings
--
-- All tables live in the default `public` schema so existing queries need no
-- change. The per-service read-only roles below enforce least-privilege access.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Auth ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  username      VARCHAR(50)  UNIQUE NOT NULL,
  display_name  VARCHAR(100) NOT NULL,
  password_hash TEXT         NOT NULL,
  email         VARCHAR(255) UNIQUE NOT NULL,
  bio           TEXT,
  avatar_url    TEXT,
  is_online     BOOLEAN      NOT NULL DEFAULT false,
  last_seen     TIMESTAMPTZ,
  fcm_token     TEXT,
  created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sessions (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  refresh_token TEXT UNIQUE NOT NULL,
  expires_at    TIMESTAMPTZ NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id       ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_refresh_token ON sessions(refresh_token);

-- ─── Chat ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS chats (
  id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  type             VARCHAR(20)  NOT NULL DEFAULT 'direct' CHECK (type IN ('direct','group','channel')),
  type_ext         VARCHAR(20),
  name             VARCHAR(100),
  description      TEXT,
  avatar_url       TEXT,
  username_handle  TEXT UNIQUE,
  subscriber_count INTEGER      NOT NULL DEFAULT 0,
  created_by       UUID         REFERENCES users(id) ON DELETE SET NULL,
  created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_members (
  chat_id    UUID        NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
  user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role       VARCHAR(20) NOT NULL DEFAULT 'member' CHECK (role IN ('owner','admin','member','superadmin')),
  joined_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_members_user_id ON chat_members(user_id);

CREATE TABLE IF NOT EXISTS messages (
  id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  chat_id               UUID         NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
  sender_id             UUID         REFERENCES users(id) ON DELETE SET NULL,
  type                  VARCHAR(20)  NOT NULL DEFAULT 'text'
                          CHECK (type IN ('text','image','video','audio','file','video_circle','sticker')),
  content               TEXT,
  caption               TEXT,
  duration              INTEGER,
  file_name             TEXT,
  file_size             INTEGER,
  is_read               BOOLEAN      NOT NULL DEFAULT false,
  is_edited             BOOLEAN      NOT NULL DEFAULT false,
  is_deleted            BOOLEAN      NOT NULL DEFAULT false,
  reply_to_id           UUID         REFERENCES messages(id) ON DELETE SET NULL,
  reply_to_content      TEXT,
  reply_to_sender_name  TEXT,
  pinned_at             TIMESTAMPTZ,
  created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS message_deletions (
  message_id  UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  user_id     UUID NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
  PRIMARY KEY (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_id    ON messages(chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id  ON messages(sender_id);

CREATE TABLE IF NOT EXISTS chat_pins (
  chat_id    UUID NOT NULL REFERENCES chats(id)    ON DELETE CASCADE,
  scope      VARCHAR(36) NOT NULL,
  message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  pinned_by  UUID NOT NULL REFERENCES users(id),
  pinned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (chat_id, scope)
);

CREATE TABLE IF NOT EXISTS message_reactions (
  message_id  UUID        NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  user_id     UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
  emoji       VARCHAR(10) NOT NULL,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  PRIMARY KEY (message_id, user_id)
);

CREATE TABLE IF NOT EXISTS scheduled_posts (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  chat_id      UUID         NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
  created_by   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type         VARCHAR(20)  NOT NULL DEFAULT 'text',
  content      TEXT         NOT NULL,
  scheduled_at TIMESTAMPTZ    NOT NULL,
  sent         BOOLEAN      NOT NULL DEFAULT false,
  created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scheduled_posts_due ON scheduled_posts(scheduled_at) WHERE sent = false;

-- ─── Calls ───────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS call_history (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  call_id           TEXT        NOT NULL,
  caller_id         UUID        REFERENCES users(id) ON DELETE SET NULL,
  callee_id         UUID        REFERENCES users(id) ON DELETE SET NULL,
  type              VARCHAR(10) NOT NULL DEFAULT 'audio' CHECK (type IN ('audio','video')),
  status            VARCHAR(20) NOT NULL DEFAULT 'missed' CHECK (status IN ('missed','completed','rejected')),
  started_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  ended_at          TIMESTAMPTZ,
  duration_seconds  INTEGER     NOT NULL DEFAULT 0,
  created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_call_history_caller ON call_history(caller_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_call_history_callee ON call_history(callee_id, started_at DESC);

-- ─── Settings ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_settings (
  user_id                  UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  notifications_enabled    BOOLEAN     NOT NULL DEFAULT true,
  notification_sound       VARCHAR(50) NOT NULL DEFAULT 'default',
  sound_enabled            BOOLEAN     NOT NULL DEFAULT true,
  show_read_receipts       BOOLEAN     NOT NULL DEFAULT true,
  last_seen_visible        BOOLEAN     NOT NULL DEFAULT true,
  theme                    VARCHAR(20) NOT NULL DEFAULT 'system' CHECK (theme IN ('light','dark','system')),
  font_size                VARCHAR(20) NOT NULL DEFAULT 'medium',
  chat_bg                  TEXT,
  language                 VARCHAR(10) NOT NULL DEFAULT 'uk',
  auto_reply_enabled       BOOLEAN     NOT NULL DEFAULT false,
  auto_reply_message       TEXT,
  two_factor_enabled       BOOLEAN     NOT NULL DEFAULT false,
  updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ─── Contacts ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS contacts (
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  contact_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  nickname    TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, contact_id)
);

-- ─── Channels ────────────────────────────────────────────────────────────────
-- Channels are a specialisation of chats (type='channel').
-- The channel_settings table stores channel-specific metadata.

CREATE TABLE IF NOT EXISTS channel_settings (
  chat_id       UUID    PRIMARY KEY REFERENCES chats(id) ON DELETE CASCADE,
  description   TEXT,
  is_public     BOOLEAN NOT NULL DEFAULT true,
  subscriber_count INTEGER NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── Per-service read-only roles ─────────────────────────────────────────────
-- Grant these to the respective service's DB user once DB_USER_API and
-- DB_USER_REALTIME are configured in production.

-- DO $$
-- BEGIN
--   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'coreme_api') THEN
--     CREATE ROLE coreme_api LOGIN PASSWORD 'CHANGE_ME';
--   END IF;
--   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'coreme_realtime') THEN
--     CREATE ROLE coreme_realtime LOGIN PASSWORD 'CHANGE_ME';
--   END IF;
-- END $$;

-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO coreme_api;
-- GRANT SELECT, INSERT, UPDATE ON messages, message_reactions, chat_members, chats TO coreme_realtime;
-- GRANT SELECT ON users TO coreme_realtime;
-- GRANT SELECT, INSERT ON call_history TO coreme_realtime;
-- GRANT SELECT, UPDATE ON user_settings TO coreme_realtime;
-- GRANT SELECT, UPDATE ON scheduled_posts TO coreme_realtime;
