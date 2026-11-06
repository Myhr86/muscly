# Muscly AI-trener – backend

Liten Node-server som kobler appen til Claude. **API-nøkkelen ligger bare her – aldri i APK-en.**

```bash
cd backend
npm install
ANTHROPIC_API_KEY=sk-ant-... node server.js
```

Serveren lytter på port 8787 (`PORT` kan endres). Kjør den bak HTTPS (f.eks. Fly.io, Render, Railway eller en egen VPS med Caddy).

I appen: **Profil → AI-trener → Adresse til AI-server**, f.eks. `https://muscly.din-server.no`.
Alternativt bygges adressen inn ved bygging: `./gradlew assembleRelease -PcoachUrl=https://muscly.din-server.no`.

- Endepunkt: `POST /v1/coach` med `{ message, history: [{role, text}], context }` → `{ reply }`
- `GET /health` → `{ ok: true }`
- Modell: `claude-opus-5-5` (endres med `COACH_MODEL`)
- Ungdomsregler i systemprompten + filter på svarene (`safety.js`), i tillegg til filteret i appen.
- Enkel rate-begrensning per IP (30 forespørsler / 10 min).
