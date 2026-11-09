// Ejecutar con Firebase Hosting Emulator en el puerto 5000.
import assert from "node:assert/strict";
const base = "http://127.0.0.1:5000/";
const resources = [
  ["", "text/html"], ["?p=" + "a".repeat(32), "text/html"],
  ["app.mjs", "javascript"], ["profiles.mjs", "javascript"],
  ["share.mjs", "javascript"], ["firebase-config.mjs", "javascript"],
  ["vendor/qrcode.mjs", "javascript"], ["styles.css", "text/css"],
  ["pet-placeholder.svg", "image/svg+xml"]
];
for (const [path, type] of resources) {
  const response = await fetch(base + path);
  assert.equal(response.status, 200, path);
  assert.ok(response.headers.get("content-type")?.includes(type), path);
  assert.equal(response.headers.get("referrer-policy"), "no-referrer");
  assert.equal(response.headers.get("x-content-type-options"), "nosniff");
  assert.ok(response.headers.get("cache-control")?.includes("no-cache"));
  await response.body?.cancel();
}
for (const path of ["app/google-services.json", ".firebaserc", "firebase.json", "docs/database.md"]) {
  const response = await fetch(base + path);
  assert.equal(response.status, 404, "Solo se debe servir web/");
  await response.body?.cancel();
}
console.log("Hosting local: rutas, módulos, cabeceras y separación de archivos correctos.");
process.env.MYPET_TEST_BASE_URL = base;
await import("./browser.mjs");
