import { firebaseWeb } from "./firebase-config.mjs";

export const statuses = { lost: "Perdida", home: "En casa", found: "Encontrada" };
const demo = {
  active: true, name: "Nube", breed: "Mestiza", sex: "Hembra", age: "3 años",
  traits: "Pelaje canela, pecho blanco y una mancha oscura en la oreja izquierda.",
  medicalAlert: "Ejemplo autorizado: no ofrecer alimentos con pollo.",
  status: "lost"
};
const publicFields = ["name", "breed", "sex", "age", "traits", "medicalAlert", "status", "photoUrl", "contactPhone"];

export async function getProfile(id, { projectId = firebaseWeb.projectId, request = fetch } = {}) {
  if (id === null || id === "demo") return { kind: "profile", profile: demo, demo: true };
  if (id === "demo-missing" || id === "demo-disabled") return { kind: "unavailable" };
  if (id === "demo-error") return { kind: "error" };
  if (!/^[A-Za-z0-9_-]{22,64}$/.test(id)) return { kind: "unavailable" };
  if (!/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(projectId)) return { kind: "unconfigured" };

  try {
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/publicPets/${id}`;
    const response = await request(url, {
      cache: "no-store", credentials: "omit", referrerPolicy: "no-referrer",
      signal: AbortSignal.timeout(8000)
    });
    if (response.status === 404) return { kind: "unavailable" };
    if (response.status === 401 || response.status === 403) {
      console.warn("MyPet: consulta pública denegada.", {
        operation: "publicPets.get", httpStatus: response.status
      });
      return { kind: "unavailable" };
    }
    if (!response.ok) {
      console.warn("MyPet: fallo en consulta pública.", {
        operation: "publicPets.get", httpStatus: response.status
      });
      return { kind: "error" };
    }
    const { fields = {} } = await response.json();
    if (fields.active?.booleanValue === false) return { kind: "unavailable" };
    if (fields.active?.booleanValue !== true || !fields.name?.stringValue?.trim()) return { kind: "error" };
    const profile = Object.fromEntries(publicFields.map(field => [field, fields[field]?.stringValue?.trim()]));
    return { kind: "profile", profile, demo: false };
  } catch {
    console.warn("MyPet: consulta pública no completada.", {
      operation: "publicPets.get", category: "network-or-response"
    });
    return { kind: "error" };
  }
}

export function phoneLink(phone) {
  return typeof phone === "string" && /^\+[1-9][0-9]{7,14}$/.test(phone) ? `tel:${phone}` : "";
}

export function safePhoto(url) {
  try {
    const photo = new URL(url);
    return photo.protocol === "https:" && !photo.username && !photo.password ? photo.href : "";
  } catch {
    return "";
  }
}
