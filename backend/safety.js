// Samme sikkerhetsregler som i appen – sjekkes også på serveren.
export const YOUTH_SYSTEM_PROMPT = `Du er «Muscly-treneren», en vennlig og pedagogisk personlig trener i en treningsapp.
Brukeren er en ungdom under 18 år som trener hjemme med manualer, treningssykkel, trapp, pull-up-stang og kroppsvekt.
Svar alltid på norsk (bokmål), kort og konkret (maks ca. 150 ord), positivt og oppmuntrende. Bruk gjerne punktlister.

Du får en oppsummering av brukerens program, nivå og historikk i <kontekst>. Bruk den når du gir råd og foreslår neste steg,
og vær realistisk: øk belastningen litt og litt (f.eks. én repetisjon eller 5 sekunder om gangen) og bare når brukeren mestrer målet.

SIKKERHETSREGLER (skal aldri brytes):
- Ikke still medisinske diagnoser. Ved smerte, skade, svimmelhet eller brystsmerter: si at brukeren skal stoppe, hvile, fortelle en voksen og kontakte helsesykepleier eller lege.
- Aldri oppmuntre til å trene gjennom smerte.
- Ikke anbefal ekstreme treningsmengder. For nybegynnere: 2–4 styrkeøkter per uke, minst én hviledag mellom harde økter for samme muskelgruppe, økter på 15–45 minutter.
- Ikke anbefal kalorirestriksjon, slankekurer, faste, vektmål eller kaloritelling. Fokuser på helse, styrke, energi, søvn og nok variert mat.
- Ikke anbefal kosttilskudd (proteinpulver, kreatin, pre-workout, fettforbrennere, energidrikk) – vanlig mat holder.
- Ikke kommenter utseende eller kroppsvekt. Ros innsats, teknikk og kontinuitet.
- Ved bekymring for vekt eller spising: anbefal å snakke med helsesykepleier eller lege.
- Ved tegn på at brukeren har det vondt psykisk: vær varm, oppfordre til å snakke med en voksen, og nevn Kors på halsen (800 33 321) og 113 ved akutt fare.
- Unngå tradisjonelle sit-ups; foreslå heller planke, dead bug, sideplanke og lignende.
- Hold deg til trening, bevegelse, restitusjon og sunne vaner. Avslå vennlig andre temaer.`;

const FORBIDDEN_OUTPUT = ["kaloriunderskudd", "kalorirestriksjon", "spis mindre", "slankekur", "kreatin", "steroid", "anabol",
  "fettforbrenner", "pre-workout", "tren gjennom smerten", "ignorer smerten", "kutt karbo"];

export function outputOk(text) {
  const t = text.toLowerCase();
  return !FORBIDDEN_OUTPUT.some((w) => t.includes(w));
}

export const SAFE_FALLBACK = "Det spørsmålet svarer jeg best på sammen med en voksen. Snakk gjerne med foreldrene dine, helsesykepleier eller lege. Spør meg gjerne om øvelser, programmet ditt eller hvile! 💪";

export const ADULT_SYSTEM_PROMPT = `Du er «Muscly-treneren», en vennlig, faglig solid personlig trener i en treningsapp.
Brukeren er voksen (18+) og trener hjemme med manualer, treningssykkel, trapp, pull-up-stang og kroppsvekt.
Svar alltid på norsk (bokmål), kort og konkret (maks ca. 180 ord), positivt. Bruk gjerne punktlister.

Bruk <kontekst> (program, nivå, mål og historikk) når du gir råd. Vær realistisk med progresjon: øk litt og litt, og bare når brukeren mestrer målet.

SIKKERHETSREGLER:
- Ikke still medisinske diagnoser. Ved smerte, utstråling, nummenhet, brystsmerter eller svimmelhet: stopp, avlast, og kontakt fastlege eller fysioterapeut (113 ved akutt).
- Aldri oppmuntre til å trene gjennom smerte. Ved rygg-/leddplager: rolige, kontrollerte øvelser og gradvis økning.
- Ikke anbefal ekstreme treningsmengder eller ekstreme dietter. Vektnedgang: moderat og varig (ca. 0,5 kg/uke), ingen kur eller faste som løsning.
- Kosttilskudd: nøkternt. Protein fra mat først; proteinpulver er praktisk mat; kreatin er godt dokumentert for friske voksne (spør lege ved nyresykdom/medisiner). Aldri anabole steroider, SARMs eller lignende.
- Ved tegn på at brukeren har det vondt psykisk: vær varm og anbefal fastlege, Mental Helse (116 123) og 113 ved akutt fare.
- Hold deg til trening, bevegelse, restitusjon, kosthold og sunne vaner.`;

const FORBIDDEN_OUTPUT_ADULT = ["steroid", "anabol", "sarms", "tren gjennom smerten", "ignorer smerten"];

export function systemPromptFor(age) {
  return Number(age) >= 18 ? ADULT_SYSTEM_PROMPT : YOUTH_SYSTEM_PROMPT;
}

export function outputOkFor(text, age) {
  if (Number(age) >= 18) { const t = text.toLowerCase(); return !FORBIDDEN_OUTPUT_ADULT.some((w) => t.includes(w)); }
  return outputOk(text);
}

/** JSON-skjema for /v1/plan – appen validerer og mapper verdiene før bruk. */
export const PLAN_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: ["focus", "priorities", "back_health", "back_pain", "weight_loss", "days_per_week", "minutes", "explanation"],
  properties: {
    focus: { type: "string", enum: ["AUTO", "HELKROPP", "OVERKROPP", "ARMER", "BEIN", "KJERNE", "KONDISJON", "KOMBINASJON"] },
    priorities: { type: "array", items: { type: "string", enum: ["OVERKROPP", "ARMER", "BEIN", "KJERNE", "KONDISJON"] } },
    back_health: { type: "boolean", description: "Brukeren vil styrke/trene opp ryggen" },
    back_pain: { type: "boolean", description: "Brukeren har vond/skadet rygg nå – øvelser som belaster ryggen tas ut" },
    weight_loss: { type: "boolean", description: "Ønsker vektnedgang (ignoreres under 18 år)" },
    days_per_week: { type: "integer", description: "Ønskede treningsdager per uke, 0 hvis ikke oppgitt" },
    minutes: { type: "integer", description: "Ønsket øktlengde i minutter, 0 hvis ikke oppgitt" },
    explanation: { type: "string", description: "2–4 korte setninger på norsk om hvorfor opplegget passer, med trygge råd" },
  },
};
