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

describe("anonymous device enrollment", () => {
  it("rejects malformed installation identifiers before database access", async () => {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/devices/enroll",
      payload: {
        installationId: "not-a-random-installation-id",
        platform: "android",
        wireGuardPublicKey: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toMatchObject({
      error: "invalid_enrollment"
    });
  });

  it("rejects invalid WireGuard public keys before database access", async () => {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/devices/enroll",
      payload: {
        installationId: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        platform: "android",
        wireGuardPublicKey: "not-a-wireguard-key"
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toMatchObject({
      error: "invalid_wireguard_key"
    });
  });

  it("rejects unexpected enrollment fields", async () => {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/devices/enroll",
      payload: {
        installationId: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        platform: "android",
        wireGuardPublicKey: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        privateKey: "must-never-be-sent"
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toMatchObject({
      error: "invalid_enrollment"
    });
  });
});
