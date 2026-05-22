const jwt = require('jsonwebtoken');
const chatHandler = require('./handlers/chat.handler');
const callHandler = require('./handlers/call.handler');

function initSocket(io) {
  io.use((socket, next) => {
    const token = socket.handshake.auth?.token;
    if (!token) return next(new Error('No token'));
    try {
      const payload = jwt.verify(token, process.env.JWT_SECRET);
      socket.userId = payload.userId;
      next();
    } catch {
      next(new Error('Invalid token'));
    }
  });

  io.on('connection', (socket) => {
    console.log(`[socket] Connected: ${socket.userId}`);

    socket.join(`user:${socket.userId}`);
    chatHandler.setOnline(socket.userId, true);
    socket.broadcast.emit('user_online', { userId: socket.userId });

    chatHandler.register(io, socket);
    callHandler.register(io, socket);

    socket.on('disconnect', () => {
      console.log(`[socket] Disconnected: ${socket.userId}`);
      chatHandler.setOnline(socket.userId, false);
      socket.broadcast.emit('user_offline', { userId: socket.userId });
    });
  });
}

module.exports = initSocket;
