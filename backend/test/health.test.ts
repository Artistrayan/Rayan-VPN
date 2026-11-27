import { afterAll, beforeAll, describe, expect, it } from "vitest";

process.env.NODE_ENV = "test";
process.env.DATABASE_URL = "postgresql://test:test@127.0.0.1:5432/rayan_vpn_test";

let app: Awaited<ReturnType<typeof import("../src/app.js").buildApp>>;

beforeAll(async () => {
  const { buildApp } = await import("../src/app.js");
  app = await buildApp();
});

afterAll(async () => {
  await app.close();
  const { database } = await import("../src/database.js");
  await database.end();
});

describe("health endpoints", () => {
  it("reports process liveness without requiring the database", async () => {
    const response = await app.inject({ method: "GET", url: "/health/live" });
    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      status: "ok",
      service: "rayan-vpn-control-plane"
    });
  });
});
