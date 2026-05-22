const { connect, StringCodec } = require('nats');

let nc = null;
let js = null;
const sc = StringCodec();

const STREAM_NAME = 'MESSAGES';
const CONSUMER_NAME = 'realtime-consumer';

async function connectNats() {
  const servers = process.env.NATS_URL || 'nats://nats:4222';
  nc = await connect({ servers });
  js = nc.jetstream();

  // Ensure the stream exists (idempotent)
  const jsm = await nc.jetstreamManager();
  try {
    await jsm.streams.add({
      name: STREAM_NAME,
      subjects: ['messages.>'],
      // Retain messages until ACK'd (work-queue semantics)
      retention: 'workqueue',
      // Keep messages up to 24 h so offline users can catch up
      max_age: 24 * 60 * 60 * 1_000_000_000, // nanoseconds
      storage: 'file',
      num_replicas: 1,
    });
    console.log(`[nats] Stream '${STREAM_NAME}' created`);
  } catch (err) {
    if (/already in use|already exists/i.test(err.message)) {
      console.log(`[nats] Stream '${STREAM_NAME}' already exists`);
    } else {
      throw err;
    }
  }

  console.log(`[nats] Connected to ${servers}`);
  return { nc, js };
}

async function publish(subject, data) {
  if (!js) throw new Error('[nats] Not connected');
  const encoded = sc.encode(JSON.stringify(data));
  await js.publish(subject, encoded);
}

// Start a durable push consumer that delivers messages to the handler callback.
async function startConsumer(handler) {
  if (!js || !nc) throw new Error('[nats] Not connected');

  const jsm = await nc.jetstreamManager();

  try {
    await jsm.consumers.add(STREAM_NAME, {
      durable_name: CONSUMER_NAME,
      ack_policy: 'explicit',
      filter_subject: 'messages.>',
      deliver_policy: 'all', // workqueue streams require deliver_all policy
      ack_wait: 30_000_000_000, // 30 s in nanoseconds
    });
  } catch (err) {
    if (!/already in use|already exists/i.test(err.message)) throw err;
  }

  const consumer = await js.consumers.get(STREAM_NAME, CONSUMER_NAME);
  const subscription = await consumer.consume();

  (async () => {
    for await (const msg of subscription) {
      try {
        const data = JSON.parse(sc.decode(msg.data));
        handler(msg.subject, data);
        msg.ack();
      } catch (err) {
        console.error('[nats] Consumer handler error:', err);
        msg.nak();
      }
    }
  })();

  console.log(`[nats] Consumer '${CONSUMER_NAME}' started`);
}

async function closeNats() {
  if (nc) {
    await nc.drain();
    await nc.close();
  }
}

module.exports = { connectNats, publish, startConsumer, closeNats };
