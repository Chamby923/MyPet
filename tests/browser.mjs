// Prueba local en Chromium. FIRESTORE se sustituye por respuestas ficticias.
import assert from "node:assert/strict";
import { createServer } from "node:http";
import { readFile, writeFile, mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";

const root = fileURLToPath(new URL("../web/", import.meta.url));
const types = { html: "text/html", mjs: "text/javascript", css: "text/css", svg: "image/svg+xml" };
const server = createServer(async (req, res) => {
  try {
    const path = new URL(req.url, "http://localhost").pathname;
    const file = resolve(root, "." + (path === "/" ? "/index.html" : path));
    if (!file.startsWith(resolve(root) + sep)) throw new Error("Ruta inválida");
    const body = await readFile(file);
    res.writeHead(200, { "Content-Type": types[file.split(".").pop()] ?? "text/plain" });
    res.end(body);
  } catch {
    res.writeHead(404);
    res.end();
  }
});
const externalBase = process.env.MYPET_TEST_BASE_URL;
if (!externalBase) await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
const base = externalBase ?? `http://127.0.0.1:${server.address().port}/`;
assert.ok(["127.0.0.1", "localhost", "[::1]"].includes(new URL(base).hostname));
const temporary = await mkdtemp(join(tmpdir(), "mypet-browser-"));
const binary = process.env.CHROME_BINARY ??
  join(process.env.ProgramFiles, "Google/Chrome/Application/chrome.exe");
const browser = spawn(binary, [
  "--headless", "--disable-background-networking", "--no-first-run",
  "--remote-debugging-port=0", "--user-data-dir=" + temporary, "about:blank"
], { stdio: "ignore", windowsHide: true });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws;
const errors = [];
let revoked = false;
try {
  let port;
  for (let attempt = 0; attempt < 60 && !port; attempt++) {
    try { port = (await readFile(join(temporary, "DevToolsActivePort"), "utf8")).split("\n")[0]; }
    catch { await delay(150); }
  }
  assert.ok(port, "Chromium debe iniciar");
  const tabs = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
  ws = new WebSocket(tabs.find(tab => tab.type === "page").webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject; });
  let id = 0;
  const pending = new Map();
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    pending.set(++id, [resolve, reject]);
    ws.send(JSON.stringify({ id, method, params }));
  });
  ws.onmessage = async event => {
    const message = JSON.parse(event.data);
    if (pending.has(message.id)) {
      const [resolve, reject] = pending.get(message.id);
      pending.delete(message.id);
      message.error ? reject(message.error) : resolve(message.result);
    }
    if (message.method === "Runtime.exceptionThrown") errors.push("Error JavaScript");
    if (message.method === "Fetch.requestPaused") {
      const { request, requestId } = message.params;
      if (Object.keys(request.headers).some(key => key.toLowerCase() === "authorization")) {
        errors.push("La consulta pública envió autorización");
      }
      const last = new URL(request.url).pathname.split("/").pop();
      let code = 200;
      let response = { fields: {
        active: { booleanValue: true }, name: { stringValue: "Nube <script>" },
        status: { stringValue: "lost" }, contactPhone: { stringValue: "+12025550123" }
      } };
      if (last.startsWith("b") || last.startsWith("a") && revoked) {
        response = { fields: { active: { booleanValue: false } } };
      }
      if (last.startsWith("c")) code = 404;
      if (last.startsWith("d")) code = 500;
      if (last.startsWith("e")) code = 403;
      await send("Fetch.fulfillRequest", {
        requestId, responseCode: code,
        responseHeaders: [
          { name: "Content-Type", value: "application/json" },
          { name: "Access-Control-Allow-Origin", value: "*" }
        ],
        body: Buffer.from(JSON.stringify(response)).toString("base64")
      });
    }
  };
  const evaluate = async expression => {
    const result = await send("Runtime.evaluate", { expression, returnByValue: true });
    assert.equal(result.exceptionDetails, undefined);
    return result.result.value;
  };
  await send("Runtime.enable");
  await send("Fetch.enable", { patterns: [{ urlPattern: "*firestore.googleapis.com/*" }] });
  async function open(query, text) {
    await send("Page.navigate", { url: base + query });
    for (let attempt = 0; attempt < 70; attempt++) {
      const ready = await evaluate(`location.href === ${JSON.stringify(base + query)} &&
        (document.querySelector('#pet-name')?.textContent ||
        document.querySelector('#message')?.textContent || '').includes(${JSON.stringify(text)})`);
      if (ready) return;
      await delay(100);
    }
    assert.fail("Estado esperado ausente");
  }
  for (const [width, height, columns] of [[320, 740, 1], [390, 844, 1], [1280, 900, 2]]) {
    await send("Emulation.setDeviceMetricsOverride", { width, height, deviceScaleFactor: 1, mobile: false });
    await open("", "Nube");
    assert.equal(await evaluate("document.querySelector('#photo').complete"), true);
    assert.equal(await evaluate("document.querySelector('#contact').hidden"), true);
    assert.equal(await evaluate("getComputedStyle(document.querySelector('#profile')).gridTemplateColumns.split(' ').length"), columns);
    await evaluate("document.querySelector('#share').open = true");
    assert.equal(await evaluate("document.documentElement.scrollWidth > innerWidth"), false);
    assert.equal(await evaluate("document.querySelectorAll('#qr svg').length"), 1);
    assert.equal(await evaluate("document.querySelector('#download-qr').href.startsWith('blob:')"), true);
    await evaluate("document.querySelector('#share').open = false");
    if (width !== 320) {
      const screenshot = await send("Page.captureScreenshot", { format: "png" });
      const path = join(temporary, `mypet-${width}.png`);
      await writeFile(path, Buffer.from(screenshot.data, "base64"));
      console.log("Captura " + path);
    }
    console.log(`Vista ${width}px y QR correctos`);
  }
  await open("?p=" + "a".repeat(32), "Nube <script>");
  assert.equal(await evaluate("document.querySelector('#contact').getAttribute('href')"), "tel:+12025550123");
  assert.equal(await evaluate("document.querySelectorAll('#pet-name script').length"), 0);
  await open("?p=" + "b".repeat(32), "ya no está disponible");
  const unavailableText = await evaluate("document.querySelector('#message').textContent");
  await open("?p=" + "c".repeat(32), "ya no está disponible");
  assert.equal(await evaluate("document.querySelector('#message').textContent"), unavailableText);
  await open("?p=" + "e".repeat(32), "ya no está disponible");
  assert.equal(await evaluate("document.querySelector('#message').textContent"), unavailableText);
  await open("?p=" + "d".repeat(32), "No pudimos");
  assert.equal(await evaluate("document.querySelector('#retry').hidden"), false);
  await evaluate("document.querySelector('#retry').click()");
  for (let attempt = 0; attempt < 50; attempt++) {
    if (await evaluate("!document.querySelector('#retry').hidden")) break;
    await delay(100);
  }
  assert.equal(await evaluate("document.querySelector('#retry').hidden"), false);
  await open("?p=" + "a".repeat(32), "Nube <script>");
  revoked = true;
  await evaluate("window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }))");
  for (let attempt = 0; attempt < 50; attempt++) {
    if (await evaluate("document.querySelector('#message').textContent.includes('ya no está disponible')")) break;
    await delay(100);
  }
  assert.equal(await evaluate("document.querySelector('#profile').hidden"), true);
  assert.equal(await evaluate("document.querySelector('#message').textContent"), unavailableText);
  assert.deepEqual(errors, []);
  console.log("Lectura anónima simulada, contacto, texto seguro y revocación al restaurar la página correctos");
} finally {
  ws?.close();
  browser.kill();
  if (server.listening) server.close();
}
