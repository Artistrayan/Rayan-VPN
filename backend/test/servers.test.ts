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

describe("gateway discovery", () => {
  it("rejects malformed region filters before querying the registry", async () => {
    const response = await app.inject({
      method: "GET",
      url: "/api/v1/servers?region=../../invalid"
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toMatchObject({
      error: "invalid_query"
    });
  });
});
