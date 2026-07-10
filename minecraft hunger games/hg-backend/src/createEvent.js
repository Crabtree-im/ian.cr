import dotenv from "dotenv";
import { pool, query } from "./db.js";

dotenv.config();

function parseArgs(argv) {
  const out = {};
  for (const arg of argv) {
    if (!arg.startsWith("--")) {
      continue;
    }
    const [k, ...rest] = arg.slice(2).split("=");
    out[k] = rest.join("=");
  }
  return out;
}

async function run() {
  const args = parseArgs(process.argv.slice(2));
  const code = String(args.code || "").trim();
  const name = String(args.name || "").trim();
  const state = String(args.state || "planned").trim();
  const startsAt = args.startsAt ? String(args.startsAt).trim() : null;

  if (!code || !name) {
    throw new Error("Usage: npm run seed:event -- --code=hg-qualifier --name='HG Qualifier' [--state=planned] [--startsAt=2026-07-25T18:00:00Z]");
  }

  const result = await query(
    `
    insert into events (code, name, state, starts_at)
    values ($1, $2, $3, $4)
    on conflict (code)
    do update set
      name = excluded.name,
      state = excluded.state,
      starts_at = excluded.starts_at
    returning id, code, name, state, starts_at, created_at
    `,
    [code, name, state, startsAt]
  );

  console.log("Event upserted:", result.rows[0]);
  await pool.end();
}

run().catch(async (err) => {
  console.error(err.message || err);
  await pool.end();
  process.exit(1);
});
