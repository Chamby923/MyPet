import qrcode from "./vendor/qrcode.mjs";

export function createProfileQr(href) {
  const url = new URL(href);
  if (!["https:", "http:"].includes(url.protocol) || url.username || url.password) {
    throw new Error("Enlace no válido");
  }
  if (!url.searchParams.has("p")) url.searchParams.set("p", "demo");
  const qr = qrcode(0, "M");
  qr.addData(url.href);
  qr.make();
  return {
    url: url.href,
    svg: qr.createSvgTag({ cellSize: 6, margin: 24, scalable: true }),
    qr
  };
}
