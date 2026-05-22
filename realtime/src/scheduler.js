const { v4: uuidv4 } = require('uuid');
const pool = require('./config/database');
const { getIo } = require('./socket/ioInstance');

async function processDuePosts() {
  try {
    const { rows } = await pool.query(
      `SELECT * FROM scheduled_posts
       WHERE sent = false AND scheduled_at <= NOW()
       ORDER BY scheduled_at ASC
       LIMIT 50`,
    );

    for (const post of rows) {
      try {
        const msgId = uuidv4();
        const { rows: msgs } = await pool.query(
          `INSERT INTO messages (id, chat_id, sender_id, type, content)
           VALUES ($1, $2, $3, $4, $5)
           RETURNING *`,
          [msgId, post.chat_id, post.created_by, post.type, post.content],
        );

        await pool.query(
          'UPDATE scheduled_posts SET sent = true WHERE id = $1',
          [post.id],
        );

        getIo()?.to(`chat:${post.chat_id}`).emit('new_message', msgs[0]);
      } catch (err) {
        console.error('[scheduler] Failed to process post', post.id, err.message);
      }
    }
  } catch (err) {
    console.error('[scheduler] processDuePosts error:', err.message);
  }
}

module.exports = { processDuePosts };
