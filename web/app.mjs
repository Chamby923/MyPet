import { getProfile, phoneLink, safePhoto, statuses } from "./profiles.mjs";
import { createProfileQr } from "./share.mjs";

const byId = id => document.getElementById(id);
const placeholder = "./pet-placeholder.svg";
const messages = {
  unavailable: "Este enlace es inválido o la ficha ya no está disponible.",
  unconfigured: "La consulta de fichas reales todavía no está configurada.",
  error: "No pudimos consultar la ficha. Comprueba tu conexión e inténtalo otra vez."
};
const photo = byId("photo");
let qrBlobUrl;
let loadGeneration = 0;
photo.addEventListener("error", () => {
  if (photo.getAttribute("src") !== placeholder) {
    photo.src = placeholder;
    photo.alt = "Ilustración de un perro";
  }
});

function showProfile(pet, demo) {
  byId("demo").hidden = !demo;
  byId("pet-name").textContent = pet.name;
  const badge = byId("pet-status");
  badge.hidden = !Object.hasOwn(statuses, pet.status);
  badge.textContent = badge.hidden ? "" : statuses[pet.status];
  badge.dataset.status = pet.status ?? "";

  const facts = byId("facts");
  facts.replaceChildren();
  for (const [label, value] of [["Raza", pet.breed], ["Sexo", pet.sex], ["Edad", pet.age]]) {
    if (!value) continue;
    const item = document.createElement("div");
    const term = document.createElement("dt");
    const detail = document.createElement("dd");
    term.textContent = label;
    detail.textContent = value;
    item.append(term, detail);
    facts.append(item);
  }
  for (const [id, value] of [["traits", pet.traits], ["medical", pet.medicalAlert]]) {
    const section = byId(id);
    section.hidden = !value;
    section.querySelector("p").textContent = value ?? "";
  }

  const photoUrl = safePhoto(pet.photoUrl);
  photo.src = photoUrl || placeholder;
  photo.alt = photoUrl ? `Foto de ${pet.name}` : "Ilustración de un perro";
  const href = phoneLink(pet.contactPhone);
  const contact = byId("contact");
  contact.hidden = !href;
  if (href) contact.href = href;
  else contact.removeAttribute("href");
  byId("no-contact").hidden = Boolean(href);
  byId("profile").hidden = false;
  byId("share").hidden = false;
  try {
    const result = createProfileQr(location.href);
    const svg = new DOMParser().parseFromString(result.svg, "image/svg+xml").documentElement;
    byId("qr").replaceChildren(svg);
    if (qrBlobUrl) URL.revokeObjectURL(qrBlobUrl);
    qrBlobUrl = URL.createObjectURL(new Blob([result.svg], { type: "image/svg+xml" }));
    byId("download-qr").href = qrBlobUrl;
    byId("local-warning").hidden = !["localhost", "127.0.0.1", "[::1]"].includes(location.hostname);
  } catch {
    byId("download-qr").hidden = true;
    byId("qr-error").hidden = false;
  }
}

async function load() {
  const generation = ++loadGeneration;
  const message = byId("message");
  message.textContent = "Cargando la ficha…";
  message.hidden = false;
  byId("retry").hidden = true;
  byId("profile").hidden = true;
  byId("share").hidden = true;
  const result = await getProfile(new URLSearchParams(location.search).get("p"));
  if (generation !== loadGeneration) return;
  if (result.kind === "profile") {
    showProfile(result.profile, result.demo);
    message.hidden = true;
  } else {
    message.textContent = messages[result.kind];
    byId("retry").hidden = result.kind !== "error";
  }
}

byId("retry").addEventListener("click", load);
window.addEventListener("pageshow", event => {
  if (event.persisted) load();
});
document.addEventListener("visibilitychange", () => {
  if (!document.hidden) load();
});
load();
