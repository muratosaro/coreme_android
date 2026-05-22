// Sentry must be initialized before any other requires
require('dotenv').config();
const Sentry = require('@sentry/node');

if (process.env.SENTRY_DSN) {
  Sentry.init({
    dsn: process.env.SENTRY_DSN,
    environment: process.env.APP_ENV || 'production',
    tracesSampleRate: 0.2,
    beforeSend(event) {
      if (event.request) {
        if (event.request.headers) {
          delete event.request.headers.authorization;
          delete event.request.headers.cookie;
        }
        const url = event.request.url || '';
        if (url.includes('/auth/') || url.includes('/messages')) {
          event.request.data = '[redacted]';
        }
      }
      return event;
    },
  });
}

const http = require('http');
const { Server } = require('socket.io');
const pool = require('./config/database');
const redisClient = require('./config/redis');
const { connectNats, startConsumer, closeNats } = require('./config/nats');
const initSocket = require('./socket/socket');
const { setIo } = require('./socket/ioInstance');
const { processDuePosts } = require('./scheduler');

const PORT = process.env.REALTIME_PORT || 3002;

let isReady = false;

const server = http.createServer(async (req, res) => {
  if (req.url === '/health') {
    try {
      await pool.query('SELECT 1');
      await redisClient.ping();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'ok', service: 'realtime' }));
    } catch (err) {
      Sentry.captureException(err);
      res.writeHead(503, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'error', service: 'realtime', error: err.message }));
    }
  } else if (req.url === '/ready') {
    if (isReady) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'ready' }));
    } else {
      res.writeHead(503, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'not ready' }));
    }
  } else {
    res.writeHead(404);
    res.end();
  }
});

const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] },
});

async function shutdown() {
  isReady = false;
  console.log('[realtime] SIGTERM received, shutting down...');

  const forceExit = setTimeout(() => {
    console.error('[realtime] Forced shutdown after 30s');
    process.exit(1);
  }, 30_000);
  forceExit.unref();

  io.close(() => console.log('[realtime] Socket.IO closed'));

  server.close(async () => {
    console.log('[realtime] HTTP server closed');
    try {
      await closeNats();
      await pool.end();
      await redisClient.quit();
      await Sentry.close(2000);
    } catch (err) {
      console.error('[realtime] Cleanup error:', err);
    }
    clearTimeout(forceExit);
    process.exit(0);
  });
}

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

process.on('uncaughtException', (err) => {
  Sentry.captureException(err);
  console.error('[realtime] Uncaught exception:', err);
  Sentry.close(2000).then(() => process.exit(1));
});

process.on('unhandledRejection', (reason) => {
  Sentry.captureException(reason);
  console.error('[realtime] Unhandled rejection:', reason);
});

async function start() {
  try {
    await pool.query('SELECT 1');
    console.log('[realtime] PostgreSQL connected');

    await redisClient.connect();
    console.log('[realtime] Redis connected');

    // Connect NATS JetStream
    await connectNats();

    // NATS consumer: relay messages from JetStream to Socket.IO clients
    await startConsumer((subject, msg) => {
      const chatId = subject.replace('messages.', '');
      io.to(`chat:${chatId}`).emit('new_message', msg);
    });

    initSocket(io);
    setIo(io);

    server.listen(PORT, '0.0.0.0', () => {
      isReady = true;
      console.log(`[realtime] Listening on :${PORT}`);
    });

    // Auto-post scheduler: check every minute
    setInterval(processDuePosts, 60 * 1000);
  } catch (err) {
    Sentry.captureException(err);
    console.error('[realtime] Startup error:', err);
    process.exit(1);
  }
}

start();
