// Muscly AI-trener – backend-proxy.
// Kjør: ANTHROPIC_API_KEY=... node server.js   (PORT valgfri, standard 8787)
// Appen kaller POST /v1/coach med { message, history: [{role, text}], context }.
import http from "node:http";
import Anthropic from "@anthropic-ai/sdk";
import { YOUTH_SYSTEM_PROMPT, outputOk, SAFE_FALLBACK } from "./safety.js";

const PORT = Number(process.env.PORT || 8787);
const MODEL = process.env.COACH_MODEL || "claude-opus-5-5";
const client = new Anthropic(); // leser ANTHROPIC_API_KEY fra miljøet

// Enkel begrensning: maks 30 forespørsler per IP per 10 minutter.
const WINDOW_MS = 10 * 60 * 1000, LIMIT = 30;
const hits = new Map();
function rateLimited(ip) {
  const now = Date.now();
  const arr = (hits.get(ip) || []).filter((t) => now - t < WINDOW_MS);
  arr.push(now); hits.set(ip, arr);
  return arr.length > LIMIT;
}

function readJson(req, max = 32_000) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", (c) => { body += c; if (body.length > max) { reject(new Error("too large")); req.destroy(); } });
    req.on("end", () => { try { resolve(JSON.parse(body || "{}")); } catch (e) { reject(e); } });
    req.on("error", reject);
  });
}

function send(res, code, obj) {
  res.writeHead(code, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(obj));
}

export async function coachReply({ message, history = [], context = "" }) {
  const msgs = [];
  for (const h of history.slice(-10)) {
    if (!h || typeof h.text !== "string") continue;
    const role = h.role === "user" ? "user" : "assistant";
    // Meldinger må veksle mellom bruker og assistent, og starte med bruker.
    if (msgs.length === 0 && role !== "user") continue;
    if (msgs.length && msgs[msgs.length - 1].role === role) msgs[msgs.length - 1].content += "\n" + h.text.slice(0, 2000);
    else msgs.push({ role, content: h.text.slice(0, 2000) });
  }
  if (msgs.length && msgs[msgs.length - 1].role === "user") msgs.pop();
  msgs.push({ role: "user", content: `<kontekst>\n${String(context).slice(0, 4000)}\n</kontekst>\n\n${String(message).slice(0, 1000)}` });

  const response = await client.beta.messages.create({
    model: MODEL,
    max_tokens: 2000,
    system: YOUTH_SYSTEM_PROMPT,
    output_config: { effort: "low" },
    betas: ["server-side-fallback-2026-07-01"],
    fallbacks: "default",
    messages: msgs,
  });
  if (response.stop_reason === "refusal") return SAFE_FALLBACK;
  const text = response.content.filter((b) => b.type === "text").map((b) => b.text).join("\n").trim();
  if (!text || !outputOk(text)) return SAFE_FALLBACK;
  return text;
}

const server = http.createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") return send(res, 200, { ok: true });
  if (req.method !== "POST" || req.url !== "/v1/coach") return send(res, 404, { error: "not found" });
  const ip = req.headers["x-forwarded-for"]?.toString().split(",")[0].trim() || req.socket.remoteAddress || "?";
  if (rateLimited(ip)) return send(res, 429, { error: "for mange forespørsler" });
  let body;
  try { body = await readJson(req); } catch { return send(res, 400, { error: "ugyldig forespørsel" }); }
  if (typeof body.message !== "string" || !body.message.trim()) return send(res, 400, { error: "mangler message" });
  try {
    const reply = await coachReply(body);
    send(res, 200, { reply });
  } catch (err) {
    if (err instanceof Anthropic.RateLimitError) return send(res, 503, { error: "opptatt, prøv igjen" });
    if (err instanceof Anthropic.APIConnectionError) return send(res, 502, { error: "ingen kontakt med AI" });
    if (err instanceof Anthropic.APIError) { console.error("API-feil", err.status, err.message); return send(res, 502, { error: "AI-feil" }); }
    console.error(err);
    send(res, 500, { error: "intern feil" });
  }
});

if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split("/").pop())) {
  server.listen(PORT, "0.0.0.0", () => console.log(`Muscly coach backend på port ${PORT} (modell ${MODEL})`));
}
