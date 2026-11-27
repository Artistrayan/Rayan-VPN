import { Pool } from "pg";
import { config } from "./config.js";

export const database = new Pool({
  connectionString: config.databaseUrl,
  ssl: config.databaseSsl ? { rejectUnauthorized: true } : undefined,
  max: 10,
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 5_000,
  application_name: "rayan-vpn-control-plane"
});

database.on("error", (error: Error) => {
  // Do not log query parameters or connection strings.
  process.stderr.write(`[database] idle client error: ${error.name}: ${error.message}\n`);
});

export async function checkDatabase(): Promise<void> {
  const client = await database.connect();
  try {
    await client.query("SELECT 1");
  } finally {
    client.release();
  }
}
