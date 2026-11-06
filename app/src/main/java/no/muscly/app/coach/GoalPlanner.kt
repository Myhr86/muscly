package no.muscly.app.coach

import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Et forslag til nytt treningsopplegg, laget fra en beskrivelse av målene. */
data class PlanProposal(
    val focus: Focus,
    val priorities: Set<Category>,
    val goals: Set<Goal>,
    val addInjuries: Set<Injury>,
    val schedule: ScheduleType?,
    val customDays: Set<Int>?,
    val minutes: Int?,
    val explanation: String,
) {
    fun apply(p: UserProfile): UserProfile = p.copy(
        focus = focus,
        priorities = priorities,
        goals = goals.filter { it != Goal.VEKTNEDGANG || p.adult }.toSet(),
        injuries = p.injuries + addInjuries,
        schedule = schedule ?: p.schedule,
        customDays = customDays ?: p.customDays,
        minutes = minutes ?: p.minutes,
    )

    fun toJson(): String = JSONObject().put("focus", focus.name).put("priorities", JSONArray(priorities.map { it.name }))
        .put("goals", JSONArray(goals.map { it.name })).put("injuries", JSONArray(addInjuries.map { it.name }))
        .put("schedule", schedule?.name ?: "").put("days", JSONArray((customDays ?: emptySet()).toList()))
        .put("minutes", minutes ?: 0).put("explanation", explanation).toString()

    companion object {
        private fun JSONObject.list(k: String) = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.get(it).toString() } } ?: emptyList()
        private inline fun <reified T : Enum<T>> ens(l: List<String>) = l.mapNotNull { runCatching { enumValueOf<T>(it) }.getOrNull() }.toSet()

        fun fromJson(s: String): PlanProposal? = runCatching {
            val o = JSONObject(s)
            PlanProposal(
                focus = enumValueOf(o.getString("focus")), priorities = ens(o.list("priorities")), goals = ens(o.list("goals")),
                addInjuries = ens(o.list("injuries")),
                schedule = o.optString("schedule").takeIf { it.isNotBlank() }?.let { enumValueOf<ScheduleType>(it) },
                customDays = o.list("days").mapNotNull { it.toIntOrNull() }.toSet().takeIf { it.isNotEmpty() },
                minutes = o.optInt("minutes", 0).takeIf { it in 10..90 }, explanation = o.optString("explanation", ""),
            )
        }.getOrNull()
    }
}

object GoalPlanner {
    private fun has(t: String, vararg w: String) = w.any { t.contains(it) }

    /** Ser teksten ut som et ønske om et nytt program basert på mål? */
    fun isPlanRequest(input: String): Boolean {
        val t = input.lowercase()
        val asksProgram = has(t, "program", "opplegg", "treningsplan", "sett opp", "sette opp")
        val statesGoal = has(t, "vil ", "ønsker", "mål", "trene opp", "større", "sterkere", "bedre", "få ", "bygge")
        return asksProgram && statesGoal || (has(t, "trene opp", "vil bli", "jeg vil") && has(t, "rygg", "biceps", "bein", "mage", "kondis", "armer", "ned i vekt"))
    }

    fun daysFor(n: Int): Pair<ScheduleType, Set<Int>?> = when (n) {
        1, 2 -> ScheduleType.EGEN to setOf(0, 3)
        3 -> ScheduleType.TRE to null
        4 -> ScheduleType.FIRE to null
        5 -> ScheduleType.EGEN to setOf(0, 1, 3, 4, 5)
        6 -> ScheduleType.EGEN to setOf(0, 1, 2, 3, 4, 5)
        else -> ScheduleType.HVER_DAG to null
    }

    fun focusFor(prio: Set<Category>, weightLoss: Boolean): Focus = when {
        weightLoss -> Focus.KOMBINASJON
        prio.size == 1 -> when (prio.first()) {
            Category.ARMER -> Focus.ARMER; Category.BEIN -> Focus.BEIN; Category.KJERNE -> Focus.KJERNE
            Category.OVERKROPP -> Focus.OVERKROPP; Category.KONDISJON -> Focus.KONDISJON; else -> Focus.AUTO
        }
        else -> Focus.AUTO
    }

    /** Innebygd tolkning av målene – virker uten internett. */
    fun local(input: String, p: UserProfile): PlanProposal {
        val t = input.lowercase()
        val prio = mutableSetOf<Category>()
        val goals = mutableSetOf<Goal>()
        val inj = mutableSetOf<Injury>()
        val notes = mutableListOf<String>()
        if (has(t, "biceps", "armer", "armene", "større arm")) prio += Category.ARMER
        if (has(t, "bein", "lår", "rumpe", "sete")) prio += Category.BEIN
        if (has(t, "mage", "kjerne", "sixpack", "six-pack")) prio += Category.KJERNE
        if (has(t, "bryst", "skuldre", "overkropp")) prio += Category.OVERKROPP
        if (has(t, "kondis", "utholdenhet", "løpe", "pust", "form")) prio += Category.KONDISJON
        if (has(t, "rygg")) {
            goals += Goal.RYGGHELSE
            notes += "Ryggvennlig: bird dog, dead bug og seteløft i hver økt – rolige øvelser som styrker muskulaturen rundt ryggen."
            if (has(t, "vond", "smerte", "prolaps", "skive", "isjias", "skade", "verk")) {
                inj += Injury.RYGG
                notes += "Siden ryggen gjør vondt, tar jeg ut øvelser som belaster den mest (roing foroverbøyd, skulderpress, superman). Få gjerne råd fra fysioterapeut eller lege, og stopp ved smerte."
            }
        }
        val wantsWeightLoss = has(t, "ned i vekt", "gå ned", "slank", "miste vekt", "vekttap", "fett")
        if (wantsWeightLoss && p.adult) {
            goals += Goal.VEKTNEDGANG; prio += Category.KONDISJON
            notes += "For vektnedgang: styrke + kondisjon, kortere pauser og hverdagsaktivitet. Kombiner med et moderat, varig kosthold – ingen kur."
        } else if (wantsWeightLoss) {
            notes += "Vi fokuserer på styrke, kondisjon og energi – ikke vekt. Snakk med helsesykepleier hvis du er bekymret for vekten."
        }
        val n = Regex("(\\d)\\s*(dager|ganger|økter)").find(t)?.groupValues?.get(1)?.toIntOrNull()
            ?: if (has(t, "hver dag", "daglig")) 7 else null
        val (sched, days) = n?.let { daysFor(it) } ?: (null to null)
        val minutes = Regex("(\\d{2})\\s*(min|minutter)").find(t)?.groupValues?.get(1)?.toIntOrNull()?.let { listOf(15, 20, 30, 45).minBy { m -> kotlin.math.abs(m - it) } }
        if (prio.contains(Category.ARMER)) notes += "Større biceps: chin-ups/pull-ups, bicepscurl og hammercurl – med progresjon når du mestrer målet."
        val focus = focusFor(prio, Goal.VEKTNEDGANG in goals)
        return PlanProposal(focus, prio, goals, inj, sched, days, minutes, notes.joinToString("\n"))
    }

    fun describe(prop: PlanProposal, p: UserProfile, viaAi: Boolean): String {
        val np = prop.apply(p)
        val plan = ProgramGenerator.weekPlan(np)
        return buildString {
            appendLine(if (viaAi) "Her er forslaget mitt 🎯" else "Her er et forslag basert på målene dine 🎯")
            appendLine()
            appendLine("• Fokus: ${np.focus.label}")
            if (np.priorities.isNotEmpty()) appendLine("• Ekstra prioritert: ${np.priorities.joinToString { it.label.lowercase() }}")
            np.goals.forEach { appendLine("• ${it.label}") }
            if (prop.addInjuries.isNotEmpty()) appendLine("• Skåner: ${prop.addInjuries.joinToString { it.label.lowercase() }}")
            appendLine("• ${np.schedule.label}, ${np.minutes} min per økt")
            appendLine()
            plan.forEachIndexed { i, d -> appendLine("${Dates.dayShort[i]}: ${d.emoji} ${d.title}") }
            if (prop.explanation.isNotBlank()) { appendLine(); append(prop.explanation) }
            appendLine(); appendLine()
            append("Progresjonen din beholdes – trykk «Bruk dette programmet» for å ta det i bruk.")
        }.trim()
    }
}
