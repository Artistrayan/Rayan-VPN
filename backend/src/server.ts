import { buildApp } from "./app.js";
import { config } from "./config.js";
import { database } from "./database.js";

const app = await buildApp();

const shutdown = async (signal: string): Promise<void> => {
  app.log.info({ signal }, "Shutting down control plane");
  try {
    await app.close();
    await database.end();
    process.exitCode = 0;
  } catch (error) {
    app.log.error({ err: error }, "Graceful shutdown failed");
    process.exitCode = 1;
  }
};

process.once("SIGINT", () => void shutdown("SIGINT"));
process.once("SIGTERM", () => void shutdown("SIGTERM"));

try {
  await app.listen({ host: config.host, port: config.port });
} catch (error) {
  app.log.error({ err: error }, "Unable to start control plane");
  await database.end();
  process.exitCode = 1;
}
