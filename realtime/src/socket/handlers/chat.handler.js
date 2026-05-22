const pool = require('../../config/database');
const { publish } = require('../../config/nats');

async function setOnline(userId, isOnline) {
  await pool.query(
    'UPDATE users SET is_online = $1, last_seen = NOW() WHERE id = $2',
    [isOnline, userId],
  ).catch(console.error);
}

function register(io, socket) {
  socket.on('join_chat', async ({ chatId }) => {
    const { rows } = await pool.query(
      'SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2',
      [chatId, socket.userId],
    ).catch(() => ({ rows: [] }));
    if (rows.length) socket.join(`chat:${chatId}`);
  });

  socket.on('send_message', async ({ chatId, content, type = 'text', replyToId, clientMsgId }) => {
    try {
      const member = await pool.query(
        'SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2',
        [chatId, socket.userId],
      );
      if (!member.rows.length) return;

      let replyContent = null;
      let replySenderName = null;
      if (replyToId) {
        const orig = await pool.query(
          `SELECT m.content, m.type, u.display_name
           FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.id = $1`,
          [replyToId],
        );
        if (orig.rows.length) {
          replyContent = orig.rows[0].type === 'text' ? orig.rows[0].content : `[${orig.rows[0].type}]`;
          replySenderName = orig.rows[0].display_name;
        }
      }

      const { rows } = await pool.query(
        `INSERT INTO messages
           (chat_id, sender_id, type, content, reply_to_id, reply_to_content, reply_to_sender_name)
         VALUES ($1, $2, $3, $4, $5, $6, $7)
         RETURNING id, chat_id, sender_id, type, content, is_read, created_at,
                   is_edited, is_deleted, reply_to_id, reply_to_content, reply_to_sender_name`,
        [chatId, socket.userId, type, content, replyToId || null, replyContent, replySenderName],
      );

      const senderRow = await pool.query('SELECT display_name FROM users WHERE id = $1', [socket.userId]);
      const msg = { ...rows[0], sender_name: senderRow.rows[0]?.display_name, client_msg_id: clientMsgId || null };

      // Publish to NATS JetStream — all realtime instances will emit to clients
      await publish(`messages.${chatId}`, msg);

      // Auto-reply: check if any recipient has auto-reply enabled
      _handleAutoReply(chatId, msg).catch(console.error);
    } catch (err) {
      console.error('[chat] send_message error:', err);
    }
  });

  socket.on('reaction:add', async ({ messageId, chatId, emoji }) => {
    try {
      await pool.query(
        `INSERT INTO message_reactions (message_id, user_id, emoji)
         VALUES ($1, $2, $3)
         ON CONFLICT (message_id, user_id) DO UPDATE SET emoji = $3`,
        [messageId, socket.userId, emoji],
      );
      const { rows } = await pool.query(
        `SELECT emoji, COUNT(*)::int AS count, array_agg(user_id) AS user_ids
         FROM message_reactions WHERE message_id = $1 GROUP BY emoji`,
        [messageId],
      );
      io.to(`chat:${chatId}`).emit('reaction_updated', { messageId, chatId, reactions: rows });
    } catch (err) {
      console.error('[chat] reaction:add error:', err);
    }
  });

  socket.on('reaction:remove', async ({ messageId, chatId }) => {
    try {
      await pool.query(
        'DELETE FROM message_reactions WHERE message_id = $1 AND user_id = $2',
        [messageId, socket.userId],
      );
      const { rows } = await pool.query(
        `SELECT emoji, COUNT(*)::int AS count, array_agg(user_id) AS user_ids
         FROM message_reactions WHERE message_id = $1 GROUP BY emoji`,
        [messageId],
      );
      io.to(`chat:${chatId}`).emit('reaction_updated', { messageId, chatId, reactions: rows });
    } catch (err) {
      console.error('[chat] reaction:remove error:', err);
    }
  });

  socket.on('message:edited', ({ chatId, message }) => {
    if (chatId && message) {
      socket.to(`chat:${chatId}`).emit('message_edited', message);
    }
  });

  socket.on('message:deleted', ({ chatId, messageId }) => {
    if (chatId && messageId) {
      socket.to(`chat:${chatId}`).emit('message_deleted', { chatId, messageId });
    }
  });

  socket.on('typing_start', ({ chatId }) => {
    socket.to(`chat:${chatId}`).emit('user_typing', { chatId, userId: socket.userId, isTyping: true });
  });

  socket.on('typing_stop', ({ chatId }) => {
    socket.to(`chat:${chatId}`).emit('user_typing', { chatId, userId: socket.userId, isTyping: false });
  });

  socket.on('mark_read', async ({ chatId }) => {
    try {
      const { rows: senders } = await pool.query(
        'SELECT DISTINCT sender_id FROM messages WHERE chat_id = $1 AND sender_id != $2 AND is_read = false',
        [chatId, socket.userId],
      );
      if (!senders.length) return;

      await pool.query(
        'UPDATE messages SET is_read = true WHERE chat_id = $1 AND sender_id != $2',
        [chatId, socket.userId],
      );

      const payload = { chatId, userId: socket.userId };
      socket.to(`chat:${chatId}`).emit('message_read', payload);
      senders.forEach(({ sender_id }) => {
        io.to(`user:${sender_id}`).emit('message_read', payload);
      });
    } catch (err) {
      console.error('[chat] mark_read error:', err);
    }
  });
}

async function _handleAutoReply(chatId, originalMsg) {
  const { rows: members } = await pool.query(
    `SELECT cm.user_id, us.auto_reply_enabled, us.auto_reply_message, u.is_online, u.display_name
     FROM chat_members cm
     LEFT JOIN user_settings us ON cm.user_id = us.user_id
     LEFT JOIN users u ON cm.user_id = u.id
     WHERE cm.chat_id = $1 AND cm.user_id != $2`,
    [chatId, originalMsg.sender_id],
  );

  for (const member of members) {
    if (member.auto_reply_enabled && !member.is_online) {
      const autoMsg = member.auto_reply_message || 'Зараз недоступний. Відповім пізніше.';
      const { rows } = await pool.query(
        `INSERT INTO messages (chat_id, sender_id, type, content)
         VALUES ($1, $2, 'text', $3)
         RETURNING id, chat_id, sender_id, type, content, is_read, created_at, is_edited, is_deleted`,
        [chatId, member.user_id, autoMsg],
      );
      await publish(`messages.${chatId}`, { ...rows[0], sender_name: member.display_name });
    }
  }
}

module.exports = { register, setOnline };
