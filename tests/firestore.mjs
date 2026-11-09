// Ejecutar exclusivamente con el emulador local de demo-mypet.
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const origin = "http://127.0.0.1:8088";
const project = "demo-mypet";
const root = `projects/${project}/databases/(default)/documents`;
const id = "a".repeat(32);
function token(uid) {
  const encode = value => Buffer.from(JSON.stringify(value)).toString("base64url");
  const now = Math.floor(Date.now() / 1000);
  return [
    encode({ alg: "none", typ: "JWT" }),
    encode({ sub: uid, user_id: uid, aud: project, iss: `https://securetoken.google.com/${project}`,
      iat: now, exp: now + 3600, firebase: { sign_in_provider: "password", identities: {} } }),
    ""
  ].join(".");
}
function fields(data) {
  return Object.fromEntries(Object.entries(data).map(([key, value]) => [
    key, valueType(value)
  ]));
}
function valueType(value) {
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return { integerValue: String(value) };
  if (typeof value === "object") return { mapValue: { fields: fields(value) } };
  return { stringValue: value };
}
const write = (path, data) => ({ update: { name: `${root}/${path}`, fields: fields(data) } });
const patch = (path, data) => ({ ...write(path, data), updateMask: { fieldPaths: Object.keys(data) } });
async function call(path, { uid, data, method = "GET" } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (uid) headers.Authorization = `Bearer ${token(uid)}`;
  return fetch(`${origin}/v1/${root}${path}`, {
    method, headers, body: data ? JSON.stringify(data) : undefined
  });
}
async function commit(writes, uid = "owner") {
  return call(":commit", { uid, method: "POST", data: { writes } });
}
const pet = {
  name: "Nube", photoUrl: "", breed: "Mestiza", sex: "Hembra", birthDate: "",
  color: "Canela", ownerName: "", phone: "", ownerEmail: "",
  vaccines: "", allergies: "", observations: "", status: "En casa",
  ownerId: "owner", publicId: "", publicBaseUrl: ""
};
const profile = {
  active: true, name: "Nube", breed: "Mestiza", sex: "Hembra", age: "3 años",
  traits: "Pelaje canela", medicalAlert: "", status: "lost", photoUrl: "", contactPhone: ""
};
const link = { ownerId: "owner", petId: "pet-one" };
const rules = await readFile(new URL("../firestore.rules.proposed", import.meta.url), "utf8");
const loaded = await fetch(`${origin}/emulator/v1/projects/${project}:securityRules`, {
  method: "PUT", headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ rules: { files: [{ name: "firestore.rules", content: rules }] } })
});
assert.equal(loaded.status, 200, "Las reglas deben compilar");

assert.equal((await commit([write("pets/pet-one", pet)])).status, 200);
assert.equal((await commit([write("pets/forged", { ...pet, ownerId: "intruder" })])).status, 403);
assert.equal((await commit([write("pets/anonymous", pet)], null)).status, 403);
assert.equal((await call("/pets/pet-one", { uid: "owner" })).status, 200);
assert.equal((await call("/pets/pet-one")).status, 403, "pets privado");
assert.equal((await call("/pets/pet-one", { uid: "intruder" })).status, 403);
assert.equal((await commit([patch("pets/pet-one", { name: "Nube revisada" })])).status, 200);
assert.equal((await commit([patch("pets/pet-one", { status: "Perdida" })])).status, 200);
assert.equal((await commit([patch("pets/pet-one", { status: "estado desconocido" })])).status, 403);
assert.equal((await commit([patch("pets/pet-one", { name: "Cambio ajeno" })], "intruder")).status, 403);
assert.equal((await commit([patch("pets/pet-one", { unknownField: "no permitido" })])).status, 403);
assert.equal((await commit([patch("pets/pet-one", { phone: 123 })])).status, 403);
assert.equal((await commit([write("pets/legacy", { ownerId: "owner", name: "Ficha anterior" })])).status, 200);
assert.equal((await commit([patch("pets/legacy", { color: "Canela" })])).status, 200);
for (const uid of ["owner", "intruder", null]) {
  assert.equal((await commit([{ delete: `${root}/pets/pet-one` }], uid)).status, 403);
}
const query = uid => ({
  structuredQuery: {
    from: [{ collectionId: "pets" }],
    where: { fieldFilter: { field: { fieldPath: "ownerId" }, op: "EQUAL", value: { stringValue: uid } } }
  }
});
for (const uid of ["owner", "intruder"]) {
  assert.equal((await call(":runQuery", { uid, method: "POST", data: query(uid) })).status, 200);
}
assert.equal((await call(":runQuery", { uid: "intruder", method: "POST", data: query("owner") })).status, 403);
assert.equal((await call(":runQuery", { method: "POST", data: query("owner") })).status, 403);
assert.equal((await call(":runQuery", {
  uid: "owner", method: "POST", data: { structuredQuery: { from: [{ collectionId: "pets" }] } }
})).status, 403);
assert.equal((await call(`/publicPets/${id}`)).status, 403, "Solo fichas activas");
assert.equal((await commit([write(`publicPets/${id}`, profile)])).status, 403);
const publish = [
  patch("pets/pet-one", { publicId: id, publicBaseUrl: "https://mypet-pip.web.app/" }),
  write(`publicPetLinks/${id}`, link),
  write(`publicPets/${id}`, profile)
];
assert.equal((await commit(publish, "intruder")).status, 403);
assert.equal((await commit(publish)).status, 200, "Publicación atómica del dueño");
for (const base of [
  "", "http://mypet-pip.web.app/", "https://example.invalid/",
  "https://mypet-pip.web.app.example.invalid/", "https://sub.mypet-pip.web.app/",
  "https://user@mypet-pip.web.app/", "https://mypet-pip.web.app:443/",
  "https://mypet-pip.web.app/path", "https://mypet-pip.web.app//",
  "https://mypet-pip.web.app/?", "https://mypet-pip.web.app/#",
  "https://mypet-pip.web.app/?p=otro", "https://mypet-pip.web.app/#demo"
]) {
  assert.equal((await commit([patch("pets/pet-one", { publicBaseUrl: base })])).status, 403, base);
}
assert.equal((await commit([patch("pets/pet-one", {
  publicBaseUrl: "https://mypet-pip.web.app"
})])).status, 200);
assert.equal((await call(`/publicPets/${id}`)).status, 200);
for (const uid of ["owner", "intruder", null]) {
  assert.equal((await call(`/publicPets/${id}`, { uid })).status, 200);
  assert.equal((await call("/publicPets", { uid })).status, 403, "No enumerar fichas");
  assert.equal((await call("/publicPetLinks", { uid })).status, 403);
}
assert.equal((await call(`/publicPetLinks/${id}`, { uid: "owner" })).status, 200);
assert.equal((await call(`/publicPetLinks/${id}`, { uid: "intruder" })).status, 403);
assert.equal((await call(`/publicPetLinks/${id}`)).status, 403, "Vínculo privado");
for (const field of [
  "ownerEmail", "ownerName", "ownerId", "petId", "phone", "publicId", "publicBaseUrl",
  "birthDate", "color", "vaccines", "allergies", "observations", "id"
]) {
  assert.equal((await commit([write(`publicPets/${id}`, { ...profile, [field]: "no autorizado" })])).status, 403, field);
}
for (const invalid of [
  { name: "   " }, { name: "N".repeat(101) }, { age: 3 }, { traits: { ownerId: "no autorizado" } },
  { status: "Perdida" }, { active: "true" }, { photoUrl: "http://example.invalid/photo" },
  { photoUrl: "https://user:password@example.invalid/photo" },
  { photoUrl: "javascript:alert(1)" }, { contactPhone: "123" },
  { medicalAlert: "x".repeat(501) }
]) {
  assert.equal((await commit([write(`publicPets/${id}`, { ...profile, ...invalid })])).status, 403);
}
assert.equal((await commit([write(`publicPets/${id}`, { ...profile, contactPhone: "+12025550123" })])).status, 200);
assert.equal((await commit([write(`publicPets/${id}`, {
  ...profile, photoUrl: "https://example.invalid/nube.webp?alt=media"
})])).status, 200, "Foto HTTPS autorizada");
assert.equal((await commit([write(`publicPets/${id}`, profile)], "intruder")).status, 403);
assert.equal((await commit([write(`publicPets/${id}`, profile)], null)).status, 403);
assert.equal((await commit([write(`publicPetLinks/${id}`, { ...link, ownerId: "intruder" })])).status, 403);
assert.equal((await commit([write(`publicPetLinks/${id}`, { ...link, petId: "legacy" })])).status, 403);
assert.equal((await commit([patch("pets/pet-one", { ownerId: "intruder" })])).status, 403);
assert.equal((await commit([write(`publicPets/${id}`, { ...profile, name: "Nube revisada" })])).status, 200);
assert.equal((await commit([write("pets/pet-one", pet)])).status, 403, "Revocación requiere marcador");
assert.equal((await commit([write(`publicPets/${id}`, { active: false })])).status, 403);
const revoke = [
  write(`publicPets/${id}`, { active: false }),
  patch("pets/pet-one", { publicId: "", publicBaseUrl: "" })
];
for (const uid of ["intruder", null]) {
  assert.equal((await commit(revoke, uid)).status, 403, "Revocación ajena o anónima");
  assert.equal((await call(`/publicPets/${id}`)).status, 200, "La ficha debe seguir activa");
  const unchanged = await (await call("/pets/pet-one", { uid: "owner" })).json();
  assert.equal(unchanged.fields.publicId.stringValue, id);
}
assert.equal((await commit(revoke, "owner")).status, 200, "Revocación completa del dueño");
// Token reservado de administración del EMULADOR. Solo se usa para leer datos ficticios locales.
const markerResponse = await fetch(`${origin}/v1/${root}/publicPets/${id}`, {
  headers: { Authorization: "Bearer owner" }
});
assert.equal(markerResponse.status, 200);
assert.deepEqual((await markerResponse.json()).fields, { active: { booleanValue: false } });
const revokedPet = await (await call("/pets/pet-one", { uid: "owner" })).json();
assert.equal(revokedPet.fields.publicId.stringValue, "");
assert.equal(revokedPet.fields.publicBaseUrl.stringValue, "");
for (const uid of ["owner", "intruder", null]) {
  assert.equal((await call(`/publicPets/${id}`, { uid })).status, 403, "Ficha inactiva denegada");
}
assert.equal((await commit(publish)).status, 403, "Identificador revocado no se reutiliza");
const nextId = "b".repeat(32);
assert.equal((await call(`/publicPetLinks/${nextId}`, { uid: "owner" })).status, 404, "Comprobación de colisión Android");
assert.equal((await commit([
  patch("pets/pet-one", { publicId: nextId, publicBaseUrl: "https://mypet-pip.web.app/" }),
  write(`publicPetLinks/${nextId}`, link), write(`publicPets/${nextId}`, profile)
])).status, 200, "Publicación con identificador nuevo");
assert.equal((await call(`/publicPets/${nextId}`)).status, 200);
assert.equal((await call(`/publicPets/${id}`)).status, 403);
assert.equal((await commit([{ delete: `${root}/publicPets/${nextId}` }])).status, 403);
assert.equal((await commit([write("unexpected/document", { name: "fuera del esquema" })])).status, 403);
console.log("Autorización estricta local aprobada: propietario, otra cuenta y visitante anónimo.");
