-- Migration 001: Convert all TIMESTAMP WITHOUT TIME ZONE to TIMESTAMPTZ.
-- Assumes existing data was stored in UTC (Docker default).
BEGIN;

ALTER TABLE users          ALTER COLUMN last_seen    TYPE TIMESTAMPTZ USING last_seen    AT TIME ZONE 'UTC';
ALTER TABLE users          ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

ALTER TABLE sessions       ALTER COLUMN expires_at   TYPE TIMESTAMPTZ USING expires_at   AT TIME ZONE 'UTC';
ALTER TABLE sessions       ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

ALTER TABLE chats          ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';
ALTER TABLE chats          ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC';

ALTER TABLE chat_members   ALTER COLUMN joined_at    TYPE TIMESTAMPTZ USING joined_at    AT TIME ZONE 'UTC';

ALTER TABLE messages       ALTER COLUMN pinned_at    TYPE TIMESTAMPTZ USING pinned_at    AT TIME ZONE 'UTC';
ALTER TABLE messages       ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

ALTER TABLE chat_pins      ALTER COLUMN pinned_at    TYPE TIMESTAMPTZ USING pinned_at    AT TIME ZONE 'UTC';

ALTER TABLE message_reactions ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC';

ALTER TABLE scheduled_posts ALTER COLUMN scheduled_at TYPE TIMESTAMPTZ USING scheduled_at AT TIME ZONE 'UTC';
ALTER TABLE scheduled_posts ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

ALTER TABLE call_history   ALTER COLUMN started_at   TYPE TIMESTAMPTZ USING started_at   AT TIME ZONE 'UTC';
ALTER TABLE call_history   ALTER COLUMN ended_at     TYPE TIMESTAMPTZ USING ended_at     AT TIME ZONE 'UTC';
ALTER TABLE call_history   ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

ALTER TABLE user_settings  ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC';

ALTER TABLE channel_settings ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

ALTER TABLE contacts       ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC';

COMMIT;
