// Idempotently creates the `socket` table the service depends on.
// Run once at startup (see start.sh). Uses the same libpq env vars (PGHOST,
// PGUSER, ...) as the app's pg Pool, loaded from .env via dotenv.
const fs = require("fs");
const path = require("path");
const { Pool } = require("pg");
require("dotenv").config();

(async () => {
  const pool = new Pool();
  try {
    const sql = fs.readFileSync(path.join(__dirname, "schema.sql"), "utf8");
    await pool.query(sql);
    console.log("Schema ensured (table 'socket').");
  } finally {
    await pool.end();
  }
})().catch((err) => {
  console.error("Schema bootstrap failed:", err.message);
  process.exit(1);
});
