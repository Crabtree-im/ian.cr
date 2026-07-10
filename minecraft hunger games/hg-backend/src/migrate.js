import dotenv from "dotenv";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { pool } from "./db.js";

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const schemaPath = path.resolve(__dirname, "../schema.sql");

async function run() {
  const sql = await fs.readFile(schemaPath, "utf8");
  await pool.query(sql);
  await pool.end();
  console.log("Schema migration complete.");
}

run().catch(async (err) => {
  console.error(err);
  await pool.end();
  process.exit(1);
});
