const { Pool, types } = require('pg');

// TIMESTAMP WITHOUT TIME ZONE → append 'Z' so pg returns UTC-marked string
// instead of local-time shifting on non-UTC machines.
types.setTypeParser(1114, (str) => (str ? `${str}Z` : null));

const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT, 10),
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  max: 10,
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 5_000,
});

pool.on('error', (err) => {
  console.error('[db] Unexpected PostgreSQL client error:', err);
});

module.exports = pool;
