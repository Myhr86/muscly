# Muscly – personlig AI-trener for Android

Treningsapp for ungdom og voksne (10–80 år; egne sikkerhetsregler under 18) som trener hjemme med manualer, treningssykkel, trapp, pull-up-stang og kroppsvekt.
Ingen innlogging, ingen reklame, all data lagres lokalt på telefonen.

## Installere APK-en
1. Kopier `Muscly-1.1.0.apk` til telefonen (USB, Google Drive, e-post e.l.).
2. Åpne filen på telefonen. Android spør om du vil tillate installasjon fra denne kilden → **Tillat** (Innstillinger → Apper → Spesiell tilgang → Installer ukjente apper).
3. Trykk **Installer**. Krever Android 8.0 eller nyere.
4. Første gang appen åpnes: gå gjennom introduksjonen og tillat varsler når du blir spurt.

## Funksjoner
| Område | Hva |
|---|---|
| Introduksjon | Alder, nivå, enkel selvvurdering (ingen tester), fokus, prioriterte muskelgrupper, dager, tid per økt, utstyr, skader, favoritt-/unngå-øvelser, påminnelser |
| Program | 3 dager, 4 dager, annenhver dag, hver dag (varierende intensitet) eller egne dager. Samme muskelgruppe belastes aldri hardt to dager på rad (også over ukeskiftet). Maks 3 harde økter/uke for nybegynnere |
| Øvelser | 27 øvelser med animert 2D-illustrasjon (start/slutt), startposisjon, utførelse, muskler, vanlige feil, tips, sett/reps/pause, alternativer. Ingen sit-ups |
| Progresjon | Progresjonsstiger (f.eks. vegg → trapp → kne → push-ups; heng → assistert → negative → pull-up). Øker bare når målet er nådd i alle sett og det kjentes lett/passe – aldri fordi tiden går. Holder ved bom, justerer forsiktig ned ved gjentatt bom, går et trinn ned ved behov. Vekt økes forsiktig etter at reps er mestret |
| Treningsmodus | Illustrasjon, sett/reps/hviletid, registrering av reps og manualvekt per sett, pausetimer med vibrasjon/lyd, nedtelling for planke o.l., intervalltimer for sykkel, lett/passe/vanskelig, «Gjør vondt» (pause fra øvelsen i 7 dager), oppsummering med rekorder og milepæler |
| Fremgang | Økter per uke, push-ups, planke, pull-ups, bicepscurl (kg×reps), knebøy, kondisjonsminutter, 23 milepæler |
| AI-trener | Innebygd trener som virker offline og bruker historikken din + valgfri språkmodell via eget backend. Sikkerhetsregler for ungdom i appen og på serveren |
| Påminnelser | Valgfritt tidspunkt, kun på treningsdager, vennlig oppfølging senere samme dag om økten ikke er gjort, positiv ukeoppsummering søndag. WorkManager + Android-varsler med tillatelseshåndtering |
| Foreldredel | PIN-beskyttet: program, planlagte dager, gjennomførte økter, utvikling, milepæler, meldt smerte, påminnelser, maks harde økter/uke, AI av/på |
| Sunne vaner | Daglig vanetips, kosthold/søvn/hvile via treneren. Ingen kaloritelling eller vektmål |

## Det som må konfigureres: AI-tilkobling
Appen fungerer fullt ut uten dette – da brukes den innebygde treneren.
For svar fra en språkmodell (Claude):
1. Sett opp serveren i `backend/` (se `backend/README.md`) med din egen `ANTHROPIC_API_KEY`, bak HTTPS.
2. I appen: **Profil → AI-trener → Adresse til AI-server** → lim inn adressen.
API-nøkkelen ligger kun på serveren, aldri i APK-en.

## Teknisk
Kotlin · Jetpack Compose · Material 3 · Room · WorkManager · Navigation Compose. Manuell DI (`AppContainer`).
```
app/src/main/java/no/muscly/app/
  domain/   Modeller, øvelsesbibliotek, illustrasjoner (2D-skjelett m/ invers kinematikk), programgenerator, øktbygger, progresjonsmotor, milepæler
  data/     Room-entiteter, DAO, Repository (lagring av økter, progresjon, rekorder, milepæler)
  coach/    Sikkerhetsfilter, offline-trener, klient mot AI-backend
  notify/   Påminnelser (WorkManager)
  ui/       Tema, komponenter (animasjon, grafer), skjermer
backend/    Node-proxy mot Claude (nøkkel kun her)
```
Bygg: `./gradlew assembleRelease` (krever Android SDK). Test: `./gradlew testDebugUnitTest`.
Skjermbilder fra testene ligger i `screenshots/`.
