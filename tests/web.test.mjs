import test from "node:test";
import assert from "node:assert/strict";
import { getProfile, phoneLink, safePhoto } from "../web/profiles.mjs";
import { createProfileQr } from "../web/share.mjs";

const id = "a".repeat(32);
const projectId = "demo-mypet";
function request(status, fields) {
  return async (url, options) => {
    assert.match(url, /\/documents\/publicPets\/[a-z0-9]+$/);
    assert.equal(options.credentials, "omit");
    return { status, ok: status === 200, json: async () => ({ fields }) };
  };
}
test("La demostración funciona sin hacer solicitudes", async () => {
  const noNetwork = () => assert.fail("La demo debe ser local");
  assert.equal((await getProfile("demo", { request: noNetwork })).demo, true);
  assert.equal((await getProfile("demo-disabled", { request: noNetwork })).kind, "unavailable");
  assert.equal((await getProfile("demo-missing", { request: noNetwork })).kind, "unavailable");
});
test("La lectura pública acepta solo campos previstos", async () => {
  const result = await getProfile(id, { projectId, request: request(200, {
    active: { booleanValue: true }, name: { stringValue: "Nube" },
    contactPhone: { stringValue: "+12025550123" }, ownerId: { stringValue: "privado" }
  }) });
  assert.equal(result.kind, "profile");
  assert.equal(result.profile.name, "Nube");
  assert.equal(result.profile.ownerId, undefined);
  assert.equal(phoneLink(result.profile.contactPhone), "tel:+12025550123");
});
test("Unifica fichas no disponibles y conserva los errores de conexión", async () => {
  for (const [status, fields, expected] of [
    [200, { active: { booleanValue: false } }, "unavailable"],
    [404, {}, "unavailable"], [403, {}, "unavailable"], [500, {}, "error"],
    [200, { active: { booleanValue: true } }, "error"]
  ]) {
    assert.equal((await getProfile(id, { projectId, request: request(status, fields) })).kind, expected);
  }
  assert.equal((await getProfile(id, {
    projectId, request: async () => { throw new Error("offline"); }
  })).kind, "error");
  assert.equal((await getProfile(id, { projectId: "" })).kind, "unconfigured");
});
test("El diagnóstico de permisos no incluye ID ni datos del documento", async () => {
  const original = console.warn;
  const entries = [];
  console.warn = (...args) => entries.push(args);
  try {
    await getProfile(id, { projectId, request: request(403, { privateField: "no registrar" }) });
    assert.deepEqual(entries, [[
      "MyPet: consulta pública denegada.", { operation: "publicPets.get", httpStatus: 403 }
    ]]);
  } finally {
    console.warn = original;
  }
});
test("Bloquea protocolos y teléfonos no válidos", () => {
  assert.equal(phoneLink("javascript:alert(1)"), "");
  assert.equal(safePhoto("javascript:alert(1)"), "");
  assert.equal(safePhoto("https://user:password@example.invalid/"), "");
});
test("El QR codifica el enlace público y conserva el identificador", () => {
  const result = createProfileQr(`https://mypet.example/?p=${id}`);
  assert.equal(result.url, `https://mypet.example/?p=${id}`);
  assert.match(result.svg, /<svg/);
  assert.ok(result.qr.getModuleCount() >= 21);
  assert.equal(new URL(createProfileQr("http://127.0.0.1:8000/").url).searchParams.get("p"), "demo");
  assert.throws(() => createProfileQr("javascript:alert(1)"));
});
