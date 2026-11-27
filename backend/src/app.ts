import Fastify, { type FastifyInstance } from "fastify";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import { config } from "./config.js";
import { checkDatabase } from "./database.js";

export async function buildApp(): Promise<FastifyInstance> {
  const app = Fastify({
    logger: {
      level: config.logLevel,
      redact: {
        paths: [
          "req.headers.authorization",
          "req.headers.cookie",
          "res.headers.set-cookie",
          "*.privateKey",
          "*.accessToken",
          "*.refreshToken",
          "*.wireguardConfig"
        ],
        censor: "[REDACTED]"
      }
    },
    trustProxy: false,
    bodyLimit: 64 * 1024,
    requestTimeout: 10_000
  });

  await app.register(helmet);
  await app.register(rateLimit, {
    max: 120,
    timeWindow: "1 minute",
    keyGenerator: (request) => request.ip
  });

  app.get("/health/live", async () => ({
    status: "ok",
    service: "rayan-vpn-control-plane"
  }));

  app.get("/health/ready", async (_request, reply) => {
    try {
      await checkDatabase();
      return {
        status: "ready",
        dependencies: { postgres: "ok" }
      };
    } catch (error) {
      requestLogFailure(app, error);
      return reply.code(503).send({
        status: "not_ready",
        dependencies: { postgres: "unavailable" }
      });
    }
  });

  app.get("/api/v1", async () => ({
    name: "Rayan VPN Control Plane",
    apiVersion: "v1"
  }));

  return app;
}

function requestLogFailure(app: FastifyInstance, error: unknown): void {
  // Error details stay in server logs; the public readiness response is intentionally generic.
  app.log.error({ err: error }, "Readiness check failed");
}
