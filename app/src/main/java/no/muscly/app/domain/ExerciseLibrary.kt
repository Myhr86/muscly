package no.muscly.app.domain

import no.muscly.app.domain.Category.*
import no.muscly.app.domain.Equipment.*
import no.muscly.app.domain.Measure.*
import no.muscly.app.domain.Muscle.*

object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        // ======================= OVERKROPP =======================
        Exercise(
            id = "pushup", name = "Push-ups", category = OVERKROPP,
            primary = listOf(BRYST, TRICEPS), secondary = listOf(SKULDRE, MAGE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Hendene litt bredere enn skuldrene, fingrene peker fremover. Kroppen er rett som en planke fra hode til hæl.",
            steps = listOf(
                "Stram magen og setet så kroppen holder seg rett.",
                "Bøy albuene og senk brystet kontrollert (ca. 2 sekunder).",
                "Albuene peker litt bakover – ikke rett ut til siden.",
                "Press deg opp igjen til armene er nesten strake."
            ),
            mistakes = listOf("Hoftene henger ned mot gulvet", "Rumpa stikker i været", "Albuene peker rett ut til siden", "Bare halve bevegelsen"),
            tips = listOf("Heller færre gode repetisjoner enn mange dårlige.", "Se ned i gulvet litt foran hendene.", "Klarer du ikke flere? Fortsett på knærne!"),
            stages = listOf(
                Stage("Push-ups mot vegg", "push_wall", REPS, 8, 15, 1, "Jo lenger unna veggen du står, jo tyngre blir det."),
                Stage("Push-ups på trappetrinn", "push_incline", REPS, 5, 12, 1, "Hendene på et trinn. Lavere trinn = tyngre."),
                Stage("Knestående push-ups", "push_knee", REPS, 4, 12, 1, "Knærne i gulvet, rett linje fra kne til hode."),
                Stage("Push-ups", "push", REPS, 2, 20, 1, "Full push-up fra tærne."),
            ),
            restSec = 75, avoidWith = setOf(Injury.HANDLEDD, Injury.SKULDER),
            alternatives = listOf("dips", "press"), keywords = listOf("push", "armhev", "push-up", "pushup", "bryst")
        ),
        Exercise(
            id = "pullup", name = "Pull-ups", category = OVERKROPP,
            primary = listOf(RYGG, BICEPS), secondary = listOf(UNDERARM, SKULDRE, MAGE),
            equipment = setOf(STANG),
            startPosition = "Grip stanga i skulderbredde. Underhåndsgrep (håndflatene mot deg, «chin-up») er litt lettere og trener biceps ekstra godt.",
            steps = listOf(
                "Heng med nesten strake armer og stram magen.",
                "Trekk skuldrene ned og bak før du trekker deg opp.",
                "Trekk albuene ned mot sidene til haka er over stanga.",
                "Senk deg kontrollert ned igjen."
            ),
            mistakes = listOf("Sparker og svinger med beina", "Bare halve bevegelsen", "Skuldrene trekkes opp mot ørene", "Slipper seg fort ned"),
            tips = listOf("Alle starter med null pull-ups – det er helt normalt!", "Negative pull-ups er den raskeste veien til din første pull-up.", "Sjekk alltid at stanga sitter godt fast før du starter."),
            stages = listOf(
                Stage("Heng i stanga", "hang", SECONDS, 10, 30, 5, "Bare heng og hold grepet. Bygger grep og skuldre."),
                Stage("Assisterte pull-ups", "pull_assisted", REPS, 3, 8, 1, "Én fot på en stol bak deg. Bruk foten så lite som mulig."),
                Stage("Negative pull-ups", "pull_negative", REPS, 2, 6, 1, "Hopp/stig opp så haka er over stanga, og senk deg så sakte du klarer (3–5 sek)."),
                Stage("Pull-ups", "pull", REPS, 1, 10, 1, "Full pull-up – du har klart det!"),
            ),
            restSec = 90, avoidWith = setOf(Injury.SKULDER),
            alternatives = listOf("row", "curl"), keywords = listOf("pull", "pull-up", "pullup", "chin", "stang", "hang")
        ),
        Exercise(
            id = "row", name = "Manualroing", category = OVERKROPP,
            primary = listOf(RYGG), secondary = listOf(BICEPS, SKULDRE),
            equipment = setOf(MANUALER, STOL),
            startPosition = "Støtt én hånd på en stol eller et trappetrinn. Rett rygg, nesten vannrett overkropp. Manualen henger rett ned.",
            steps = listOf(
                "Stram magen og hold ryggen rett.",
                "Trekk manualen opp mot hofta – albuen går bakover tett inntil kroppen.",
                "Klem skulderbladene sammen et lite øyeblikk.",
                "Senk manualen kontrollert ned. Bytt side etter settet."
            ),
            mistakes = listOf("Rund rygg", "Vrir overkroppen for å få opp vekta", "Rykker opp manualen"),
            tips = listOf("Tenk at du starter en gressklipper – rolig og kontrollert.", "Bruk en vekt du kan løfte 10 ganger med god teknikk."),
            stages = listOf(Stage("Manualroing", "row", REPS_PER_SIDE, 8, 15, 1, weighted = true)),
            restSec = 60, avoidWith = setOf(Injury.RYGG), alternatives = listOf("pullup", "superman"), keywords = listOf("roing", "row", "rygg")
        ),
        Exercise(
            id = "press", name = "Skulderpress", category = OVERKROPP,
            primary = listOf(SKULDRE), secondary = listOf(TRICEPS, MAGE),
            equipment = setOf(MANUALER),
            startPosition = "Stå med føttene i hoftebredde. Hold manualene ved skuldrene med albuene litt foran kroppen.",
            steps = listOf("Stram magen og setet.", "Press manualene rett opp til armene er nesten strake.", "Senk dem kontrollert ned til skuldrene igjen."),
            mistakes = listOf("Svaier i korsryggen", "For tung vekt", "Låser albuene hardt øverst"),
            tips = listOf("Start lett – skuldrene er små muskler.", "Pust ut når du presser opp."),
            stages = listOf(Stage("Skulderpress", "press", REPS, 8, 15, 1, weighted = true)),
            restSec = 60, avoidWith = setOf(Injury.SKULDER, Injury.RYGG), alternatives = listOf("pushup"), keywords = listOf("skulder", "press")
        ),
        Exercise(
            id = "dips", name = "Triceps-dips på trapp", category = ARMER,
            primary = listOf(TRICEPS), secondary = listOf(BRYST, SKULDRE),
            equipment = setOf(TRAPP),
            startPosition = "Sitt på kanten av et trappetrinn, hendene ved siden av hoftene. Skyv hoftene frem fra trinnet.",
            steps = listOf("Bøy albuene rett bakover og senk deg litt ned.", "Gå bare så langt ned at det kjennes greit i skuldrene.", "Press deg opp igjen med armene."),
            mistakes = listOf("Går for dypt ned", "Albuene peker ut til siden", "Skuldrene trekkes opp mot ørene"),
            tips = listOf("Bøy knærne for å gjøre det lettere, strekk beina for å gjøre det tyngre.", "Kjenner du ubehag foran i skulderen – hopp over øvelsen."),
            stages = listOf(Stage("Triceps-dips", "dips", REPS, 5, 15, 1)),
            restSec = 60, avoidWith = setOf(Injury.SKULDER, Injury.HANDLEDD), alternatives = listOf("pushup"), keywords = listOf("dips", "triceps")
        ),
        // ======================= ARMER =======================
        Exercise(
            id = "curl", name = "Bicepscurl", category = ARMER,
            primary = listOf(BICEPS), secondary = listOf(UNDERARM),
            equipment = setOf(MANUALER),
            startPosition = "Stå oppreist med en manual i hver hånd, armene rett ned og håndflatene fremover.",
            steps = listOf("Hold albuene inntil kroppen.", "Bøy armene og løft manualene opp mot skuldrene.", "Klem biceps et lite øyeblikk på toppen.", "Senk manualene SAKTE ned (ca. 2–3 sekunder)."),
            mistakes = listOf("Svinger med overkroppen", "Albuene glir frem", "Slipper vekta fort ned", "For tung manual"),
            tips = listOf("Den sakte veien ned er like viktig som veien opp.", "Øk antall repetisjoner først – vekta øker vi senere."),
            stages = listOf(Stage("Bicepscurl", "curl", REPS, 8, 15, 1, weighted = true)),
            restSec = 60, alternatives = listOf("hammer", "pullup"), keywords = listOf("biceps", "curl", "arm")
        ),
        Exercise(
            id = "hammer", name = "Hammercurl", category = ARMER,
            primary = listOf(BICEPS, UNDERARM), equipment = setOf(MANUALER),
            startPosition = "Stå oppreist med manualene langs siden, tomlene peker fremover (som når du holder en hammer).",
            steps = listOf("Hold albuene inntil kroppen.", "Løft manualene opp mot skuldrene uten å vri håndleddet.", "Senk kontrollert ned."),
            mistakes = listOf("Svinger med kroppen", "Løfter albuene frem", "For fort tempo"),
            tips = listOf("Trener både biceps og underarmene – bra for grepet i pull-ups!"),
            stages = listOf(Stage("Hammercurl", "hammer", REPS, 8, 15, 1, weighted = true)),
            restSec = 60, alternatives = listOf("curl"), keywords = listOf("hammer", "underarm", "grep")
        ),
        // ======================= BEIN =======================
        Exercise(
            id = "squat", name = "Knebøy", category = BEIN,
            primary = listOf(FORSIDE_LAR, SETE), secondary = listOf(BAKSIDE_LAR, MAGE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Stå med føttene i skulderbredde, tærne peker litt utover. Armene fremover for balanse.",
            steps = listOf("Skyv hoftene bakover som om du skal sette deg på en stol.", "Knærne følger retningen på tærne.", "Gå ned til lårene er omtrent parallelle med gulvet.", "Press deg opp gjennom hele foten."),
            mistakes = listOf("Knærne faller innover", "Hælene letter fra gulvet", "Rund rygg", "Går for fort ned"),
            tips = listOf("Se rett frem, brystet opp.", "Hælene skal hele tiden være i gulvet."),
            stages = listOf(
                Stage("Knebøy til stol", "squat_chair", REPS, 8, 15, 1, "Sett deg lett på stolen og reis deg igjen."),
                Stage("Knebøy", "squat", REPS, 10, 20, 1, "Uten stol."),
                Stage("Goblet-knebøy med manual", "squat_goblet", REPS, 8, 15, 1, "Hold én manual foran brystet.", weighted = true),
            ),
            restSec = 75, avoidWith = setOf(Injury.KNE), alternatives = listOf("wallsit", "bridge"), keywords = listOf("knebøy", "squat", "lår", "bein")
        ),
        Exercise(
            id = "lunge", name = "Bakoverutfall", category = BEIN,
            primary = listOf(FORSIDE_LAR, SETE), secondary = listOf(BAKSIDE_LAR, LEGG),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Stå oppreist med føttene i hoftebredde. Hendene på hoftene eller manualer langs siden.",
            steps = listOf("Ta et langt steg bakover med ett bein.", "Senk bakre kne rolig ned mot gulvet.", "Fremre kne er over ankelen.", "Press deg tilbake med fremre bein. Bytt side."),
            mistakes = listOf("Fremre kne faller innover", "Lener overkroppen langt frem", "For kort steg"),
            tips = listOf("Bakoverutfall er snillere mot knærne enn utfall fremover.", "Hold deg i en vegg om balansen er vanskelig."),
            stages = listOf(
                Stage("Bakoverutfall", "lunge", REPS_PER_SIDE, 6, 12, 1),
                Stage("Utfall med manualer", "lunge_db", REPS_PER_SIDE, 6, 12, 1, weighted = true),
            ),
            restSec = 75, avoidWith = setOf(Injury.KNE), alternatives = listOf("stepup", "squat"), keywords = listOf("utfall", "lunge")
        ),
        Exercise(
            id = "stepup", name = "Step-ups i trapp", category = BEIN,
            primary = listOf(FORSIDE_LAR, SETE), secondary = listOf(LEGG, HJERTE_LUNGE),
            equipment = setOf(TRAPP),
            startPosition = "Stå foran trappa. Sett hele den ene foten på et trinn (ett eller to trinn opp).",
            steps = listOf("Press deg opp med beinet som står på trinnet.", "Stå helt oppreist på toppen.", "Gå kontrollert ned igjen med samme bein.", "Gjør alle repetisjonene på én side, så bytt."),
            mistakes = listOf("Dytter fra med bakre bein", "Kneet faller innover", "Bare halve foten på trinnet"),
            tips = listOf("Hold i rekkverket hvis du trenger balanse.", "Ett trinn holder godt i starten."),
            stages = listOf(
                Stage("Step-ups", "stepup", REPS_PER_SIDE, 8, 15, 1),
                Stage("Step-ups med manualer", "stepup_db", REPS_PER_SIDE, 8, 12, 1, weighted = true),
            ),
            restSec = 60, avoidWith = setOf(Injury.KNE), alternatives = listOf("lunge", "squat"), keywords = listOf("step", "trapp", "steg")
        ),
        Exercise(
            id = "wallsit", name = "Wall sit", category = BEIN,
            primary = listOf(FORSIDE_LAR), secondary = listOf(SETE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Stå med ryggen mot en vegg, føttene ca. ett skritt fremfor deg.",
            steps = listOf("Skli ned langs veggen til knærne er ca. 90 grader.", "Knærne skal være over anklene.", "Hold stillingen og pust rolig."),
            mistakes = listOf("Knærne går forbi tærne", "Holder pusten", "Hendene presser på lårene"),
            tips = listOf("Gå litt mindre dypt ned hvis det er for tøft.", "Tell ned høyt – det hjelper!"),
            stages = listOf(Stage("Wall sit", "wallsit", SECONDS, 15, 60, 5)),
            restSec = 60, avoidWith = setOf(Injury.KNE), alternatives = listOf("squat", "bridge"), keywords = listOf("wall", "vegg", "sit")
        ),
        Exercise(
            id = "calf", name = "Tåhev i trapp", category = BEIN,
            primary = listOf(LEGG), equipment = setOf(TRAPP),
            startPosition = "Stå på tærne på kanten av et trinn, hælene stikker ut over kanten. Hold i rekkverket eller veggen.",
            steps = listOf("Senk hælene rolig litt under trinnkanten.", "Press deg helt opp på tærne.", "Hold et sekund på toppen.", "Senk rolig ned igjen."),
            mistakes = listOf("For fort tempo", "Bøyer knærne", "Bare halve bevegelsen"),
            tips = listOf("Rolig og kontrollert gir best effekt."),
            stages = listOf(
                Stage("Tåhev", "calf", REPS, 10, 25, 2),
                Stage("Ettbens tåhev", "calf_single", REPS_PER_SIDE, 8, 15, 1),
            ),
            restSec = 45, avoidWith = setOf(Injury.ANKEL), alternatives = listOf("stepup"), keywords = listOf("tåhev", "legg", "calf")
        ),
        Exercise(
            id = "bridge", name = "Seteløft", category = BEIN,
            primary = listOf(SETE, BAKSIDE_LAR), secondary = listOf(KORSRYGG, MAGE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Ligg på ryggen med bøyde knær og føttene i gulvet i hoftebredde. Armene langs siden.",
            steps = listOf("Press hælene i gulvet og løft hoftene.", "Stram setet hardt på toppen – rett linje fra knær til skuldre.", "Senk rolig ned."),
            mistakes = listOf("Svaier i korsryggen", "Presser med tærne i stedet for hælene"),
            tips = listOf("Snill øvelse som er bra for knær og rygg."),
            stages = listOf(
                Stage("Seteløft", "bridge", REPS, 10, 20, 2),
                Stage("Ettbens seteløft", "bridge_single", REPS_PER_SIDE, 6, 12, 1),
            ),
            restSec = 45, alternatives = listOf("squat", "superman"), keywords = listOf("sete", "bridge", "rumpe")
        ),
        // ======================= KJERNE =======================
        Exercise(
            id = "plank", name = "Planken", category = KJERNE,
            primary = listOf(MAGE), secondary = listOf(SKULDRE, SETE, KORSRYGG),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Ligg på underarmene med albuene rett under skuldrene. Tærne i gulvet.",
            steps = listOf("Løft kroppen så du er rett fra hode til hæl.", "Stram magen som om noen skal dytte deg i magen.", "Stram setet.", "Pust rolig og hold."),
            mistakes = listOf("Hoftene henger ned", "Rumpa i været", "Holder pusten", "Ser frem i stedet for ned"),
            tips = listOf("God teknikk er viktigere enn lang tid.", "Stopp når du ikke klarer å holde kroppen rett."),
            stages = listOf(
                Stage("Planke på knærne", "plank_knee", SECONDS, 10, 30, 5),
                Stage("Planke", "plank", SECONDS, 10, 60, 5),
            ),
            restSec = 45, alternatives = listOf("deadbug", "birddog"), keywords = listOf("planke", "plank", "mage")
        ),
        Exercise(
            id = "sideplank", name = "Sideplanke", category = KJERNE,
            primary = listOf(SKRA_MAGE), secondary = listOf(SKULDRE, HOFTE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Ligg på siden med albuen rett under skulderen. Beina oppå hverandre.",
            steps = listOf("Løft hoften så kroppen blir rett.", "Hold den øverste armen opp eller på hofta.", "Hold, og bytt side."),
            mistakes = listOf("Hoften synker", "Lener seg frem eller bak", "Albuen ikke under skulderen"),
            tips = listOf("Bøy knærne for å gjøre den lettere."),
            stages = listOf(
                Stage("Sideplanke på knærne", "side_plank_knee", SECONDS_PER_SIDE, 10, 30, 5),
                Stage("Sideplanke", "side_plank", SECONDS_PER_SIDE, 10, 45, 5),
            ),
            restSec = 30, avoidWith = setOf(Injury.SKULDER), alternatives = listOf("deadbug", "plank"), keywords = listOf("sideplanke", "skrå")
        ),
        Exercise(
            id = "deadbug", name = "Dead bug", category = KJERNE,
            primary = listOf(MAGE), secondary = listOf(HOFTE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Ligg på ryggen. Armene rett opp mot taket, knærne løftet i 90 grader.",
            steps = listOf("Press korsryggen mot gulvet.", "Strekk én arm bakover og motsatt bein fremover – sakte.", "Gå tilbake og bytt side."),
            mistakes = listOf("Korsryggen løfter seg fra gulvet", "Går for fort", "Holder pusten"),
            tips = listOf("Jo saktere, jo bedre. En av de beste mageøvelsene som finnes!"),
            stages = listOf(Stage("Dead bug", "deadbug", REPS_PER_SIDE, 5, 12, 1)),
            restSec = 30, alternatives = listOf("plank", "birddog"), keywords = listOf("dead bug", "deadbug", "mage")
        ),
        Exercise(
            id = "mountain", name = "Mountain climbers", category = KJERNE,
            primary = listOf(MAGE, HJERTE_LUNGE), secondary = listOf(SKULDRE, HOFTE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Stå i høy planke (som toppen av en push-up), hendene under skuldrene.",
            steps = listOf("Trekk ett kne frem mot brystet.", "Bytt bein i jevnt tempo.", "Hold hoftene lave og rolige."),
            mistakes = listOf("Rumpa går til værs", "Hendene foran skuldrene"),
            tips = listOf("Start rolig og øk tempoet når teknikken sitter."),
            stages = listOf(Stage("Mountain climbers", "mountain", SECONDS, 15, 40, 5)),
            restSec = 40, avoidWith = setOf(Injury.HANDLEDD), alternatives = listOf("deadbug", "jacks"), keywords = listOf("mountain", "climber", "klatrer")
        ),
        Exercise(
            id = "birddog", name = "Bird dog", category = KJERNE,
            primary = listOf(KORSRYGG, MAGE), secondary = listOf(SETE, SKULDRE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Stå på alle fire. Hendene under skuldrene, knærne under hoftene.",
            steps = listOf("Strekk én arm frem og motsatt bein bak.", "Hold i 2 sekunder – ryggen skal være helt rolig.", "Gå tilbake og bytt side."),
            mistakes = listOf("Svaier i ryggen", "Vipper hoften", "Går for fort"),
            tips = listOf("Tenk at du har et glass vann på ryggen som ikke skal velte."),
            stages = listOf(Stage("Bird dog", "birddog", REPS_PER_SIDE, 5, 12, 1)),
            restSec = 30, alternatives = listOf("deadbug", "superman"), keywords = listOf("bird", "fugl", "hund")
        ),
        Exercise(
            id = "superman", name = "Superman", category = KJERNE,
            primary = listOf(KORSRYGG, RYGG), secondary = listOf(SETE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Ligg på magen med armene strukket fremover.",
            steps = listOf("Løft armer, bryst og bein litt fra gulvet.", "Hold i 2 sekunder.", "Senk rolig ned."),
            mistakes = listOf("Bøyer nakken bakover", "Løfter for høyt med rykk"),
            tips = listOf("Se ned i gulvet. Små, kontrollerte løft er nok."),
            stages = listOf(Stage("Superman", "superman", REPS, 6, 15, 1)),
            restSec = 30, avoidWith = setOf(Injury.RYGG), alternatives = listOf("birddog", "bridge"), keywords = listOf("superman", "korsrygg")
        ),
        Exercise(
            id = "kneeraise", name = "Kneløft", category = KJERNE,
            primary = listOf(MAGE), secondary = listOf(HOFTE, UNDERARM),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Liggende: på ryggen med beina i 90°. Hengende: heng i stanga med strake armer.",
            steps = listOf("Stram magen.", "Løft/rull knærne opp mot brystet.", "Senk kontrollert ned uten å svinge."),
            mistakes = listOf("Svinger med kroppen", "Bruker fart i stedet for magen"),
            tips = listOf("Rolig tempo – magen skal gjøre jobben."),
            stages = listOf(
                Stage("Liggende kneløft", "knee_raise_lying", REPS, 6, 15, 1),
                Stage("Hengende kneløft", "knee_raise_hang", REPS, 4, 12, 1, "Krever pull-up-stang.", needs = STANG),
            ),
            restSec = 40, alternatives = listOf("deadbug", "plank"), keywords = listOf("kneløft", "knee raise", "hengende")
        ),
        // ======================= KONDISJON =======================
        Exercise(
            id = "bike_interval", name = "Sykkelintervaller", category = KONDISJON,
            primary = listOf(HJERTE_LUNGE), secondary = listOf(FORSIDE_LAR, LEGG),
            equipment = setOf(SYKKEL),
            startPosition = "Juster setet så beinet er nesten strakt i nederste pedalstilling. Sykle rolig i 3 minutter først.",
            steps = listOf("Tråkk hardt i arbeidsperioden – du skal bli andpusten.", "Sykle helt rolig i pausen.", "Gjenta antall runder.", "Avslutt med 2–3 minutter rolig sykling."),
            mistakes = listOf("Hopper over oppvarmingen", "Starter alt for hardt på første runde", "For lavt sete"),
            tips = listOf("Hardt betyr at du ikke klarer å snakke i hele setninger.", "Ha en vannflaske klar."),
            stages = listOf(
                Stage("Intervaller 20/40", "bike", ROUNDS, 4, 8, 1, "20 sek hardt, 40 sek rolig", workSec = 20, restSec = 40),
                Stage("Intervaller 30/60", "bike", ROUNDS, 5, 10, 1, "30 sek hardt, 60 sek rolig", workSec = 30, restSec = 60),
                Stage("Intervaller 40/40", "bike", ROUNDS, 5, 10, 1, "40 sek hardt, 40 sek rolig", workSec = 40, restSec = 40),
            ),
            restSec = 0, alternatives = listOf("stairs", "jacks"), keywords = listOf("sykkel", "intervall", "kondis")
        ),
        Exercise(
            id = "bike_easy", name = "Rolig sykling", category = KONDISJON,
            primary = listOf(HJERTE_LUNGE), secondary = listOf(FORSIDE_LAR),
            equipment = setOf(SYKKEL),
            startPosition = "Sett deg på sykkelen med rett rygg og avslappede skuldre.",
            steps = listOf("Sykle i et tempo der du kan prate.", "Hold jevn fart hele veien."),
            mistakes = listOf("Sykler for hardt på en lett dag"),
            tips = listOf("Rolig sykling hjelper musklene å restituere. Hør på musikk eller en podkast!"),
            stages = listOf(Stage("Rolig sykling", "bike", MINUTES, 10, 30, 2)),
            restSec = 0, alternatives = listOf("stairs"), keywords = listOf("rolig", "sykling", "sykle")
        ),
        Exercise(
            id = "stairs", name = "Trappeintervaller", category = KONDISJON,
            primary = listOf(HJERTE_LUNGE, FORSIDE_LAR), secondary = listOf(SETE, LEGG),
            equipment = setOf(TRAPP),
            startPosition = "Stå nederst i trappa. Sjekk at det ikke ligger noe i trappa.",
            steps = listOf("Gå eller jogg opp trappa i godt tempo.", "Gå rolig ned igjen – det er pausen.", "Gjenta antall runder."),
            mistakes = listOf("Løper ned trappa", "Ser ikke hvor du setter føttene"),
            tips = listOf("Gå alltid rolig ned – det er der uhellene skjer.", "Hold i rekkverket ved behov."),
            stages = listOf(
                Stage("Rask gange opp trappa", "stairs", ROUNDS, 3, 8, 1),
                Stage("Jogg opp trappa", "stairs", ROUNDS, 4, 10, 1),
            ),
            restSec = 0, avoidWith = setOf(Injury.KNE, Injury.ANKEL), alternatives = listOf("bike_interval", "jacks"), keywords = listOf("trapp", "trappeløp", "stairs")
        ),
        Exercise(
            id = "jacks", name = "Stjernehopp", category = KONDISJON,
            primary = listOf(HJERTE_LUNGE), secondary = listOf(LEGG, SKULDRE),
            equipment = setOf(KROPPSVEKT),
            startPosition = "Stå med samlede bein og armene langs siden.",
            steps = listOf("Hopp ut med beina og før armene over hodet.", "Hopp tilbake til start.", "Hold jevnt tempo."),
            mistakes = listOf("Lander på hælene", "Holder pusten"),
            tips = listOf("For skånsom variant: steg ut til siden i stedet for å hoppe."),
            stages = listOf(Stage("Stjernehopp", "jacks", SECONDS, 20, 45, 5)),
            restSec = 30, avoidWith = setOf(Injury.KNE, Injury.ANKEL), alternatives = listOf("mountain", "bike_interval"), keywords = listOf("stjernehopp", "jumping jacks", "jacks")
        ),
        // ======================= BEVEGELIGHET =======================
        Exercise(
            id = "stretch_quad", name = "Lårstrekk", category = BEVEGELIGHET,
            primary = listOf(FORSIDE_LAR), equipment = setOf(KROPPSVEKT),
            startPosition = "Stå på ett bein og hold deg i en vegg.",
            steps = listOf("Ta tak i foten bak deg og dra hælen mot rumpa.", "Hold knærne inntil hverandre.", "Hold, og bytt side."),
            mistakes = listOf("Svaier i ryggen", "Kneet glir fremover"),
            tips = listOf("Tøy rolig – det skal kjennes, men ikke gjøre vondt."),
            stages = listOf(Stage("Lårstrekk", "stretch_quad", SECONDS_PER_SIDE, 30, 30, 0)),
            restSec = 0, keywords = listOf("tøy", "strekk")
        ),
        Exercise(
            id = "stretch_hip", name = "Hoftestrekk", category = BEVEGELIGHET,
            primary = listOf(HOFTE), secondary = listOf(FORSIDE_LAR), equipment = setOf(KROPPSVEKT),
            startPosition = "Stå på ett kne med det andre beinet foran deg (som i et utfall).",
            steps = listOf("Skyv hoften rolig fremover.", "Hold overkroppen oppreist.", "Hold, og bytt side."),
            mistakes = listOf("Lener seg frem", "Svaier i korsryggen"),
            tips = listOf("Ha en pute under kneet."),
            stages = listOf(Stage("Hoftestrekk", "stretch_hip", SECONDS_PER_SIDE, 30, 30, 0)),
            restSec = 0, keywords = listOf("hofte", "tøy")
        ),
        Exercise(
            id = "stretch_ham", name = "Bakside lår-strekk", category = BEVEGELIGHET,
            primary = listOf(BAKSIDE_LAR), equipment = setOf(TRAPP),
            startPosition = "Sett hælen på et lavt trappetrinn med strakt bein.",
            steps = listOf("Len deg rolig fremover med rett rygg.", "Hold, og bytt side."),
            mistakes = listOf("Runder ryggen", "Bøyer kneet"),
            tips = listOf("Pust rolig ut og slapp av i strekken."),
            stages = listOf(Stage("Bakside lår-strekk", "stretch_ham", SECONDS_PER_SIDE, 30, 30, 0)),
            restSec = 0, keywords = listOf("bakside", "tøy")
        ),
        Exercise(
            id = "stretch_child", name = "Barnets stilling", category = BEVEGELIGHET,
            primary = listOf(KORSRYGG, RYGG), secondary = listOf(SKULDRE), equipment = setOf(KROPPSVEKT),
            startPosition = "Sitt på knærne og sett deg ned på hælene.",
            steps = listOf("Len overkroppen frem og strekk armene langt fremover på gulvet.", "Slapp av og pust rolig."),
            mistakes = listOf("Holder pusten"),
            tips = listOf("Fin avslutning på en hard økt."),
            stages = listOf(Stage("Barnets stilling", "stretch_child", SECONDS, 30, 30, 0)),
            restSec = 0, keywords = listOf("barnets", "rygg", "tøy")
        ),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Exercise = byId[id] ?: error("Ukjent øvelse $id")
    fun find(id: String): Exercise? = byId[id]

    fun available(profile: UserProfile, ex: Exercise): Boolean {
        if (ex.id in profile.avoided) return false
        if (ex.avoidWith.any { it in profile.injuries }) return false
        val needs = ex.equipment - Equipment.KROPPSVEKT - Equipment.STOL
        return needs.all { it in profile.equipment }
    }

    /** Finn øvelse ut fra fritekst (brukes av treneren). */
    fun search(text: String): Exercise? {
        val t = text.lowercase()
        return all.firstOrNull { e -> e.keywords.any { t.contains(it) } || t.contains(e.name.lowercase()) }
    }

    val warmup = listOf(
        "3 min rolig sykling eller marsj på stedet",
        "10 armsirkler forover og bakover",
        "10 rolige knebøy",
        "10 hoftesirkler hver vei",
        "20 sek stjernehopp eller rask gange i trappa"
    )
    val cooldown = listOf("2 min rolig gange eller sykling", "Lårstrekk 30 sek per side", "Barnets stilling 30 sek", "Drikk vann 💧")
}
