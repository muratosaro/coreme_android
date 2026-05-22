const { v4: uuidv4 } = require('uuid');
const pool = require('../../config/database');

// Tracks active calls: callId → { callerId, calleeId, type, startedAt }
const activeCalls = new Map();

function register(io, socket) {
  socket.on('call:request', (data) => {
    const { to, callId, type, callerName, callerAvatar } = data;
    activeCalls.set(callId, {
      callerId: socket.userId,
      calleeId: to,
      type: type || 'audio',
      startedAt: new Date(),
    });
    io.to(`user:${to}`).emit('call:incoming', {
      callId, from: socket.userId, type, callerName, callerAvatar,
    });
  });

  socket.on('call:accept', ({ callId, to }) => {
    io.to(`user:${to}`).emit('call:accepted', { callId, from: socket.userId });
  });

  socket.on('call:reject', ({ callId, to }) => {
    const call = activeCalls.get(callId);
    if (call) {
      _saveCallRecord(callId, call, 'missed').catch(console.error);
      activeCalls.delete(callId);
    }
    io.to(`user:${to}`).emit('call:rejected', { callId, from: socket.userId });
  });

  socket.on('call:end', ({ callId, to }) => {
    const call = activeCalls.get(callId);
    if (call) {
      _saveCallRecord(callId, call, 'completed').catch(console.error);
      activeCalls.delete(callId);
    }
    io.to(`user:${to}`).emit('call:ended', { callId, from: socket.userId });
  });

  socket.on('call:offer', ({ callId, to, sdp }) => {
    io.to(`user:${to}`).emit('call:offer', { callId, from: socket.userId, sdp });
  });

  socket.on('call:answer', ({ callId, to, sdp }) => {
    io.to(`user:${to}`).emit('call:answer', { callId, from: socket.userId, sdp });
  });

  socket.on('call:ice', ({ callId, to, candidate }) => {
    io.to(`user:${to}`).emit('call:ice', { callId, from: socket.userId, candidate });
  });
}

async function _saveCallRecord(callId, call, status) {
  try {
    const endedAt = new Date();
    const duration = Math.round((endedAt - call.startedAt) / 1000);
    await pool.query(
      `INSERT INTO call_history (id, call_id, caller_id, callee_id, type, status, started_at, ended_at, duration_seconds)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       ON CONFLICT DO NOTHING`,
      [
        uuidv4(), callId,
        call.callerId, call.calleeId,
        call.type, status,
        call.startedAt, endedAt,
        status === 'completed' ? duration : 0,
      ],
    );
  } catch (err) {
    console.error('[call] _saveCallRecord error:', err);
  }
}

module.exports = { register };
