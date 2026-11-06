package no.muscly.app.coach

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.muscly.app.data.Dates
import no.muscly.app.data.ExerciseLogEntity
import no.muscly.app.data.SessionEntity
import no.muscly.app.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Alt treneren trenger å vite om brukeren. Ingen navn eller personopplysninger sendes ut. */
data class CoachContext(
    val profile: UserProfile,
    val states: Map<String, ExerciseState>,
    val sessions: List<SessionEntity>,
    val logs: List<ExerciseLogEntity>,
    val weekPlan: List<DayType>,
    val today: Long,
) {
    fun stageText(id: String): String {
        val ex = ExerciseLibrary.find(id) ?: return ""
        val s = states[id] ?: return ex.name
        val st = ex.stages[s.stage.coerceIn(0, ex.stages.lastIndex)]
        val w = if (st.weighted) " med ${ProgressionEngine.fmtKg(s.weightKg)} kg" else ""
        return "${st.name}: ${s.target} ${st.measure.unit}$w (trinn ${s.stage + 1} av ${ex.stages.size})"
    }

    fun sessionsLast(days: Int) = sessions.count { it.day > today - days }

    /** Kort, anonym oppsummering som sendes til AI-backend. */
    fun summary(): String = buildString {
        appendLine("Alder: ${profile.age}. Nivå: ${profile.level.label}. Fokus: ${profile.focus.label}. Øktlengde: ${profile.minutes} min.")
        appendLine("Utstyr: ${profile.equipment.joinToString { it.label }}.")
        if (profile.injuries.isNotEmpty()) appendLine("Unngå belastning på: ${profile.injuries.joinToString { it.label }}.")
        appendLine("Ukeplan: " + weekPlan.mapIndexed { i, d -> "${Dates.dayShort[i]}=${d.title}" }.joinToString(", "))
        appendLine("Økter totalt: ${sessions.size}. Siste 7 dager: ${sessionsLast(7)}. Siste 28 dager: ${sessionsLast(28)}.")
        val last = sessions.take(5).joinToString("; ") { "${Dates.label(it.day)} ${it.title} (${it.completedExercises}/${it.totalExercises})" }
        if (last.isNotBlank()) appendLine("Siste økter: $last")
        appendLine("Nåværende nivå per øvelse:")
        listOf("pushup", "pullup", "curl", "squat", "plank", "bike_interval").forEach { appendLine("- ${stageText(it)}") }
        val pain = states.values.filter { it.painUntilDay > today }.mapNotNull { ExerciseLibrary.find(it.exerciseId)?.name }
        if (pain.isNotEmpty()) appendLine("Meldt smerte (pause): ${pain.joinToString()}")
    }
}

data class CoachReply(val text: String, val action: String = "", val source: String = "lokal")

/** Regelbasert trener som alltid virker – også uten internett. */
object OfflineCoach {

    private fun has(t: String, vararg words: String) = words.any { t.contains(it) }

    fun answer(input: String, c: CoachContext): CoachReply {
        val t = input.lowercase()
        val ex = ExerciseLibrary.search(t)
        return when {
            has(t, "lag", "sett opp", "foreslå", "gi meg") && has(t, "økt", "trening", "program") && !has(t, "uke") -> makeWorkout(t, c)
            has(t, "for vanskelig", "for tung", "klarer ikke", "får ikke til", "for hard") && ex != null -> easier(ex, c)
            has(t, "alternativ", "i stedet", "erstatt", "bytte ut", "noe annet enn") && ex != null -> alternatives(ex, c)
            has(t, "hvilke muskler", "hva trener", "hvilken muskel", "muskler trener") && ex != null -> muscles(ex)
            has(t, "hvordan gjør", "hvordan utfører", "forklar", "teknikk", "riktig måte") && ex != null -> howTo(ex, c)
            has(t, "biceps", "større armer", "store armer", "armmuskler") -> biceps(c)
            has(t, "push") || has(t, "armhev") -> pushups(c)
            has(t, "pull", "chin", "stanga") -> pullups(c)
            has(t, "i går", "trene i dag", "trene igjen", "kan jeg trene", "hver dag", "to dager på rad") -> trainToday(c)
            has(t, "fremgang", "analyse", "hvordan går det", "statistikk", "historikk", "neste steg", "bedre") -> analyse(c)
            has(t, "motivasjon", "gidder", "orker", "lei", "tregt", "kjedelig", "gir opp") -> motivate(c)
            has(t, "hvile", "restitusjon", "støl", "søvn", "sove", "trøtt") -> recovery()
            has(t, "mat", "spise", "protein", "frokost", "middag", "drikke", "vann") -> food()
            has(t, "oppvarming", "varme opp") -> CoachReply("God oppvarming gjør musklene klare og minsker risikoen for skader. Gjør dette før hver økt (ca. 5 min):\n\n" + ExerciseLibrary.warmup.joinToString("\n") { "• $it" })
            has(t, "program", "plan", "uke") -> plan(c)
            ex != null -> howTo(ex, c)
            has(t, "hei", "hallo", "heisann", "yo") -> CoachReply("Hei! 👋 Jeg er treneren din. Spør meg om øvelser, programmet ditt, hvile – eller be meg lage en kort økt for i dag!")
            else -> CoachReply("Det er jeg ikke helt sikker på, men jeg kan hjelpe deg med:\n• «Lag en kort økt for i dag»\n• «Hvordan blir jeg bedre i pull-ups?»\n• «Kan jeg trene i dag?»\n• «Hvilke muskler trener knebøy?»\n• «Hvordan går det med fremgangen min?»")
        }
    }

    private fun makeWorkout(t: String, c: CoachContext): CoachReply {
        val minutes = Regex("(\\d{2})").find(t)?.value?.toIntOrNull()?.coerceIn(10, 45) ?: 15
        val recent = c.sessions.map { it.day to (runCatching { DayType.valueOf(it.dayType) }.getOrDefault(DayType.HELKROPP)) }
        var type = WorkoutBuilder.suggestForToday(recent, c.today)
        when {
            has(t, "bein", "lår") -> if (recent.none { it.first == c.today - 1 && BodyArea.UNDER in it.second.loads && it.second.intensity == Intensity.HARD }) type = DayType.BEIN_KJERNE
            has(t, "arm", "biceps", "overkropp") -> if (recent.none { it.first == c.today - 1 && BodyArea.OVER in it.second.loads && it.second.intensity == Intensity.HARD }) type = DayType.ARMER
            has(t, "mage", "kjerne") -> type = DayType.KJERNE_KONDIS
            has(t, "kondis", "sykkel", "sykle") -> type = DayType.KONDISJON
            has(t, "lett", "rolig") -> type = DayType.LETT
        }
        val w = WorkoutBuilder.build(type, c.profile, c.states, c.sessions.size, Dates.weekIndex(c.today), c.today, minutes)
        val why = if (type == DayType.LETT) "Du har trent hardt de siste dagene, så i dag blir det en lett økt for restitusjon."
                  else "Jeg har valgt dette ut fra hva du har trent den siste tiden, så musklene får hvile."
        val list = w.exercises.joinToString("\n") { pe ->
            val e = ExerciseLibrary.get(pe.exerciseId); val st = e.stages[pe.stageIndex]
            "• ${st.name}: ${pe.sets} × ${pe.target} ${st.measure.unit}"
        }
        return CoachReply("Her er en økt på ca. ${w.estimatedMinutes} min: ${type.emoji} ${type.title}\n\n$list\n\n$why", action = "workout:${type.name}:$minutes")
    }

    private fun easier(ex: Exercise, c: CoachContext): CoachReply {
        val s = c.states[ex.id]
        val stage = s?.stage ?: 0
        val text = if (stage > 0) {
            val prev = ex.stages[stage - 1]
            "Helt greit – alle har øvelser som er tunge! 💪\n\nPrøv «${prev.name}» i stedet: ${prev.note.ifBlank { "en lettere variant" }}\n\n" +
                "I appen: Velg «Vanskelig» etter øvelsen, så justerer programmet seg automatisk. Klarer du færre enn målet to ganger, går vi et trinn ned."
        } else {
            "Helt greit – vi starter der du er! 💪\n\n• Gjør færre repetisjoner, men med god teknikk.\n• Ta lengre pause mellom settene.\n• Velg «Vanskelig» etter øvelsen, så tilpasser appen målet.\n\nTips: ${ex.tips.firstOrNull() ?: ""}"
        }
        return CoachReply(text, action = "exercise:${ex.id}")
    }

    private fun alternatives(ex: Exercise, c: CoachContext): CoachReply {
        val alts = ex.alternatives.mapNotNull { ExerciseLibrary.find(it) }.filter { ExerciseLibrary.available(c.profile, it) }
            .ifEmpty { ExerciseLibrary.all.filter { it.category == ex.category && it.id != ex.id && ExerciseLibrary.available(c.profile, it) }.take(3) }
        return CoachReply("Gode alternativer til ${ex.name}:\n\n" + alts.joinToString("\n") { "• ${it.name} – trener ${it.primary.joinToString { m -> m.label.lowercase() }}" } +
            "\n\nVil du at en øvelse aldri skal komme i programmet? Gå til Profil → Øvelser jeg vil unngå.", action = alts.firstOrNull()?.let { "exercise:${it.id}" } ?: "")
    }

    private fun muscles(ex: Exercise) = CoachReply(
        "${ex.name} trener først og fremst ${ex.primary.joinToString { it.label.lowercase() }}" +
            (if (ex.secondary.isNotEmpty()) ", og i tillegg ${ex.secondary.joinToString { it.label.lowercase() }}." else ".") +
            "\n\n${ex.tips.firstOrNull() ?: ""}", action = "exercise:${ex.id}")

    private fun howTo(ex: Exercise, c: CoachContext): CoachReply = CoachReply(
        "${ex.name} – slik gjør du det:\n\nStart: ${ex.startPosition}\n\n" + ex.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") +
            "\n\nVanlig feil: ${ex.mistakes.first()}.\nDitt nivå nå: ${c.stageText(ex.id)}", action = "exercise:${ex.id}")

    private fun biceps(c: CoachContext) = CoachReply(
        "Slik bygger du sterkere biceps 💪\n\n" +
            "1. Chin-ups/pull-ups er den beste biceps-øvelsen du har – underhåndsgrep gir mest biceps. Du er nå på: ${c.stageText("pullup")}\n" +
            "2. Bicepscurl og hammercurl med kontrollert, sakte nedfiring. Du er på: ${c.stageText("curl")}\n" +
            "3. Øk repetisjonene litt og litt – appen øker når du har klart målet med god teknikk.\n" +
            "4. Hvil minst én dag mellom hver armøkt – musklene vokser i hvilen.\n" +
            "5. Spis nok mat og sov 9–10 timer. Det er «hemmeligheten» mange glemmer.\n\n" +
            "Muskler bygges over måneder, ikke dager. Vær tålmodig – du kommer til å se fremgang i tallene først!", action = "exercise:curl")

    private fun pushups(c: CoachContext) = CoachReply(
        "Klarer du bare noen få push-ups? Det er en helt vanlig start! 🙌\n\n" +
            "Appen har en stige du klatrer oppover:\n" +
            "1. Push-ups mot vegg\n2. Push-ups på trappetrinn\n3. Knestående push-ups\n4. Vanlige push-ups\n\n" +
            "Du er nå på: ${c.stageText("pushup")}\n\n" +
            "Klarer du målet i alle sett (og det kjennes passe eller lett), øker vi med én repetisjon. Klarer du 2 nå, er 3 neste mål – ikke 10. " +
            "Tips: Ta noen ekstra knestående push-ups etter de vanlige for å bygge mer styrke.", action = "exercise:pushup")

    private fun pullups(c: CoachContext) = CoachReply(
        "Veien til pull-ups 🧗\n\n" +
            "1. Heng i stanga – bygger grep\n2. Assisterte pull-ups med fot på stol\n3. Negative pull-ups – start øverst og senk deg sakte (3–5 sek)\n4. Full pull-up!\n\n" +
            "Du er nå på: ${c.stageText("pullup")}\n\n" +
            "Negative pull-ups er det beste trikset – kroppen er sterkere på vei ned enn opp, så du bygger styrke raskt. Tren pull-ups 2–3 ganger i uka, ikke hver dag. Hammercurl og roing hjelper også.", action = "exercise:pullup")

    private fun trainToday(c: CoachContext): CoachReply {
        val y = c.sessions.filter { it.day == c.today - 1 }.mapNotNull { runCatching { DayType.valueOf(it.dayType) }.getOrNull() }
        val todayPlan = c.weekPlan[Dates.dow(c.today)]
        val doneToday = c.sessions.any { it.day == c.today }
        return when {
            doneToday -> CoachReply("Du har allerede trent i dag – sterkt! 🎉 Nå er det hvile, mat og søvn som gjør jobben. Litt rolig aktivitet som en gåtur er fint.")
            y.any { it.intensity == Intensity.HARD } -> {
                val area = y.filter { it.intensity == Intensity.HARD }.flatMap { it.loads }.toSet()
                val tip = when {
                    area.containsAll(listOf(BodyArea.OVER, BodyArea.UNDER)) -> "Du trente hele kroppen i går, så i dag passer en lett dag: rolig sykling, tøying eller en mageøkt."
                    BodyArea.OVER in area -> "Du trente overkroppen i går. Da kan du fint trene bein og mage i dag!"
                    else -> "Du trente bein i går. Da kan du trene overkropp og armer i dag!"
                }
                CoachReply("Ja, du kan trene – men ikke de samme musklene hardt to dager på rad. $tip\n\nI dag står det «${todayPlan.title}» i planen din.", action = "workout:${WorkoutBuilder.suggestForToday(c.sessions.map { it.day to (runCatching { DayType.valueOf(it.dayType) }.getOrDefault(DayType.HELKROPP)) }, c.today).name}:20")
            }
            else -> CoachReply("Ja! Planen din i dag er: ${todayPlan.emoji} ${todayPlan.title}. " +
                (if (todayPlan == DayType.HVILE) "Det er hviledag – men en gåtur eller rolig sykling er alltid lov." else "Kroppen er uthvilt, så kjør på! 💪"))
        }
    }

    private fun analyse(c: CoachContext): CoachReply {
        if (c.sessions.isEmpty()) return CoachReply("Du har ikke gjennomført noen økter ennå. Start med dagens økt – så kan jeg følge fremgangen din! 🚀")
        val w = c.sessionsLast(7); val m = c.sessionsLast(28)
        val ups = c.logs.filter { it.decisionKind in listOf("UP", "STAGE_UP", "WEIGHT_UP") }
        val stageUps = c.logs.filter { it.decisionKind == "STAGE_UP" }.mapNotNull { ExerciseLibrary.find(it.exerciseId)?.name }.distinct()
        val planned = c.weekPlan.count { it != DayType.HVILE }
        val sb = StringBuilder("Din fremgang 📈\n\n")
        sb.appendLine("• ${c.sessions.size} økter totalt, $w siste 7 dager, $m siste 4 uker.")
        sb.appendLine("• Du har økt målet ${ups.size} ganger fordi du mestret øvelsene.")
        if (stageUps.isNotEmpty()) sb.appendLine("• Nye nivåer i: ${stageUps.joinToString()}.")
        sb.appendLine("\nNå er du på:")
        listOf("pushup", "pullup", "squat", "plank").forEach { sb.appendLine("• ${c.stageText(it)}") }
        sb.appendLine()
        sb.append(when {
            w >= planned -> "Du følger planen helt – imponerende! Neste steg: fortsett slik, og fokuser på god teknikk i hver repetisjon."
            w > 0 -> "Bra innsats! Neste steg: prøv å få med alle $planned planlagte økter neste uke. Korte økter teller også!"
            else -> "Det er en stund siden sist – helt greit, det skjer alle. Neste steg: én kort økt på 15 minutter i dag eller i morgen."
        })
        return CoachReply(sb.toString())
    }

    private fun motivate(c: CoachContext): CoachReply {
        val total = c.sessions.size
        val msg = listOf(
            "Alle har dager der motivasjonen er lav – også proffene. Trikset er å gjøre noe lite: bare 10 minutter. Ofte får du lyst på mer når du først er i gang!",
            "Husk hvorfor du startet. Hver økt gjør deg litt sterkere, selv om du ikke ser det i speilet ennå.",
            "Prøv å trene sammen med en venn eller noen i familien – det er mye morsommere!",
        )[(total % 3)]
        return CoachReply("Det er helt normalt at det går litt tregt innimellom 💙\n\n$msg\n\n" +
            (if (total > 0) "Du har allerede gjennomført $total økter – det er $total ganger du valgte å trene. Det er ekte innsats!" else "Den første økten er den viktigste. Du klarer det!"),
            action = "workout:${DayType.KJERNE_KONDIS.name}:15")
    }

    private fun recovery() = CoachReply(
        "Hvile er en del av treningen! 😴\n\n" +
            "• Musklene blir sterkere mens du hviler – ikke mens du trener.\n" +
            "• Sov 9–10 timer. Under søvnen frigjøres veksthormon som bygger muskler.\n" +
            "• Stølhet 1–2 dager etter trening er normalt. Rolig sykling og tøying hjelper.\n" +
            "• Tren ikke samme muskelgruppe hardt to dager på rad.\n" +
            "• Er du veldig sliten eller syk: hvil. Du mister ingenting på noen dagers pause.")

    private fun food() = CoachReply(
        "Mat er drivstoffet ditt 🍽️\n\n" +
            "• Spis frokost – du trenger energi til skole og trening.\n" +
            "• 3 hovedmåltider + 1–2 mellommåltider hver dag.\n" +
            "• Protein i hvert måltid hjelper musklene: egg, melk, yoghurt, fisk, kylling, bønner.\n" +
            "• Grove kornprodukter, frukt og grønnsaker gir energi og vitaminer.\n" +
            "• Etter trening: et glass melk og en brødskive er perfekt.\n" +
            "• Drikk vann – før, under og etter trening.\n\n" +
            "Du trenger ikke tilskudd eller spesialmat – vanlig, variert mat er det beste!")

    private fun plan(c: CoachContext) = CoachReply("Ukeplanen din:\n\n" +
        c.weekPlan.mapIndexed { i, d -> "${Dates.dayNames[i]}: ${d.emoji} ${d.title}" }.joinToString("\n") +
        "\n\nPlanen sørger for at du aldri trener samme muskler hardt to dager på rad. Du kan endre dager og mål under Profil.")
}

/** Kobler til en språkmodell via eget backend. API-nøkkelen ligger KUN på serveren. */
class RemoteCoach(private val baseUrl: String) {
    suspend fun ask(message: String, history: List<Pair<Boolean, String>>, context: String): String = withContext(Dispatchers.IO) {
        val url = URL(baseUrl.trimEnd('/') + "/v1/coach")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 75000; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            val body = JSONObject().put("message", message).put("context", context).put("history",
                JSONArray(history.takeLast(10).map { JSONObject().put("role", if (it.first) "user" else "assistant").put("text", it.second) }))
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            val txt = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(txt).getString("reply")
        } finally { conn.disconnect() }
    }
}

/** Velger riktig kilde for svaret og sørger for at sikkerhetsreglene alltid gjelder. */
class CoachService(private val defaultUrl: String) {
    suspend fun reply(input: String, ctx: CoachContext, history: List<Pair<Boolean, String>>): CoachReply {
        Safety.classify(input)?.let { return CoachReply(Safety.answer(it), source = "sikkerhet") }
        val local = OfflineCoach.answer(input, ctx)
        val url = ctx.profile.coachUrl.ifBlank { defaultUrl }
        if (!ctx.profile.aiEnabled || url.isBlank()) return local
        // Handlinger som «lag en økt» løses best lokalt – de bruker appens egen progresjon.
        if (local.action.startsWith("workout:")) return local
        return try {
            val r = RemoteCoach(url).ask(input, history, ctx.summary())
            if (r.isBlank() || !Safety.outputOk(r)) local.copy(source = "lokal")
            else CoachReply(r.take(3000), action = local.action.takeIf { it.startsWith("exercise:") } ?: "", source = "ai")
        } catch (e: Exception) {
            local.copy(text = local.text + "\n\n(Svar fra den innebygde treneren – AI-tilkoblingen er ikke tilgjengelig akkurat nå.)", source = "lokal")
        }
    }
}
