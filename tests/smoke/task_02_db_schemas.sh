#!/usr/bin/env bash
echo "Testing Task 2: Database schema — tables, indexes, constraints"
PASS=0; FAIL=0

check() {
  local desc="$1"; local cmd="$2"
  if eval "$cmd" &>/dev/null; then
    echo "  ✅ $desc"; ((PASS++))
  else
    echo "  ❌ $desc"; ((FAIL++))
  fi
}

SCHEMA="$(dirname "$0")/../../src/db/schema.sql"

check "schema.sql exists" "[ -f '$SCHEMA' ]"

# Required tables
for tbl in users sessions chats chat_members messages message_reactions scheduled_posts call_history user_settings contacts channel_settings; do
  check "table '$tbl' defined" "grep -q 'CREATE TABLE IF NOT EXISTS $tbl' '$SCHEMA'"
done

# Key columns
check "users.fcm_token column" "grep -q 'fcm_token' '$SCHEMA'"
check "messages.reply_to_id column" "grep -q 'reply_to_id' '$SCHEMA'"
check "messages.is_deleted column" "grep -q 'is_deleted' '$SCHEMA'"
check "messages.pinned_at column" "grep -q 'pinned_at' '$SCHEMA'"
check "scheduled_posts.scheduled_at column" "grep -q 'scheduled_at' '$SCHEMA'"
check "call_history.duration_seconds column" "grep -q 'duration_seconds' '$SCHEMA'"
check "user_settings.two_factor_enabled column" "grep -q 'two_factor_enabled' '$SCHEMA'"

# Indexes
check "idx_messages_chat_id index" "grep -q 'idx_messages_chat_id' '$SCHEMA'"
check "idx_messages_sender_id index" "grep -q 'idx_messages_sender_id' '$SCHEMA'"
check "idx_sessions_refresh_token index" "grep -q 'idx_sessions_refresh_token' '$SCHEMA'"
check "idx_scheduled_posts_due index" "grep -q 'idx_scheduled_posts_due' '$SCHEMA'"
check "idx_call_history_caller index" "grep -q 'idx_call_history_caller' '$SCHEMA'"
check "idx_call_history_callee index" "grep -q 'idx_call_history_callee' '$SCHEMA'"

# Constraints
check "messages.type CHECK enum" "grep -q \"'text','image','video','audio','file','video_circle','sticker'\" '$SCHEMA'"
check "chats.type CHECK enum" "grep -q \"'direct','group','channel'\" '$SCHEMA'"
check "call_history.status CHECK enum" "grep -q \"'missed','completed','rejected'\" '$SCHEMA'"

# Live DB check
PG_RUNNING=$(docker inspect coreme_postgres --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$PG_RUNNING" = "running" ]; then
  PG_CMD="docker exec coreme_postgres psql -U ${DB_USER:-postgres} -d ${DB_NAME:-coreme} -t -c"

  for tbl in users sessions chats chat_members messages message_reactions scheduled_posts call_history user_settings contacts channel_settings; do
    result=$($PG_CMD "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='$tbl');" 2>/dev/null | tr -d ' ')
    if [ "$result" = "t" ]; then
      echo "  ✅ DB table '$tbl' exists in live DB"; ((PASS++))
    else
      echo "  ❌ DB table '$tbl' NOT found in live DB"; ((FAIL++))
    fi
  done
else
  echo "  ⚠️  postgres container not running — skipping live DB checks (status: $PG_RUNNING)"
fi

echo "  Task 2: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
