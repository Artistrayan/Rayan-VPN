import Fastify, { type FastifyInstance } from "fastify";
import helmet from "@fastify/helmet";
import rateLimit from "@fastify/rate-limit";
import { z } from "zod";
import { config } from "./config.js";
import { checkDatabase, database } from "./database.js";

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

  app.get("/api/v1/servers", async (request, reply) => {
    const querySchema = z.object({
      region: z.string().regex(/^[a-z0-9-]{2,32}$/i).optional()
    });
    const parsedQuery = querySchema.safeParse(request.query);
    if (!parsedQuery.success) {
      return reply.code(400).send({
        error: "invalid_query",
        message: "The region filter is invalid."
      });
    }

    try {
      const result = await database.query<{
        id: string;
        region_code: string;
        country_code: string;
        city: string;
        endpoint_host: string;
        endpoint_port: number;
        wireguard_public_key: string;
        capacity_mbps: number;
        current_load_percent: string;
        last_health_at: Date | null;
      }>(
        `SELECT id, region_code, country_code, city, endpoint_host,
                endpoint_port, wireguard_public_key, capacity_mbps,
                current_load_percent, last_health_at
           FROM gateways
          WHERE status = 'healthy'
            AND ($1::text IS NULL OR region_code = $1)
          ORDER BY current_load_percent ASC, region_code ASC
          LIMIT 100`,
        [parsedQuery.data.region ?? null]
      );

      reply.header("cache-control", "no-store");
      return {
        servers: result.rows.map((gateway) => ({
          id: gateway.id,
          region: gateway.region_code,
          countryCode: gateway.country_code,
          city: gateway.city,
          endpoint: { host: gateway.endpoint_host, port: gateway.endpoint_port },
          publicKey: gateway.wireguard_public_key,
          capacityMbps: gateway.capacity_mbps,
          loadPercent: Number(gateway.current_load_percent),
          lastHealthAt: gateway.last_health_at?.toISOString() ?? null
        }))
      };
    } catch (error) {
      requestLogFailure(app, error);
      return reply.code(503).send({
        error: "server_registry_unavailable",
        message: "The server registry is temporarily unavailable."
      });
    }
  });

  return app;
}

function requestLogFailure(app: FastifyInstance, error: unknown): void {
  // Error details stay in server logs; the public readiness response is intentionally generic.
  app.log.error({ err: error }, "Readiness check failed");
}
