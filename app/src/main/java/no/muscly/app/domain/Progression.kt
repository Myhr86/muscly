package no.muscly.app.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** Lagret progresjonstilstand for én øvelse. */
data class ExerciseState(
    val exerciseId: String,
    val stage: Int,
    val target: Int,
    val weightKg: Float = 0f,
    val successStreak: Int = 0,
    val struggleStreak: Int = 0,
    val lastUpdatedDay: Long = 0,
    val painUntilDay: Long = 0,
    val lastEvaluatedDay: Long = 0,
)

/** Resultatet av én øvelse i én økt. */
data class ExerciseResult(
    val exerciseId: String,
    val stage: Int,
    val target: Int,
    val setsPlanned: Int,
    val setValues: List<Int>,
    val weightKg: Float,
    val rating: Rating,
    val pain: Boolean = false,
)

enum class ChangeKind { UP, STAGE_UP, WEIGHT_UP, HOLD, DOWN, STAGE_DOWN, PAIN, MAXED }

data class Decision(val newState: ExerciseState, val kind: ChangeKind, val message: String)

/** Selvvurdering fra introduksjonen – ingen krav om tester. */
enum class Ability { VET_IKKE, INGEN, LITT, MIDDELS, MYE }

data class SelfAssessment(
    val pushups: Ability = Ability.VET_IKKE,
    val pullups: Ability = Ability.VET_IKKE,
    val plank: Ability = Ability.VET_IKKE,
)

object ProgressionEngine {

    fun stageAllowed(stage: Stage, profile: UserProfile): Boolean {
        if (stage.weighted && Equipment.MANUALER !in profile.equipment) return false
        val n = stage.needs
        return n == null || n in profile.equipment
    }

    private fun defaultWeight(id: String): Float = when (id) {
        "curl", "hammer", "lunge", "stepup" -> 3f
        "row" -> 4f
        "press" -> 2f
        "squat" -> 6f
        else -> 0f
    }

    fun weightStep(current: Float): Float = if (current < 6f) 1f else 2f

    /** Startnivå – konservativt, basert på nivå og selvvurdering. */
    fun initialState(ex: Exercise, profile: UserProfile, sa: SelfAssessment, today: Long): ExerciseState {
        var stage = 0
        var target = ex.stages[0].start
        val lvl = profile.level
        when (ex.id) {
            "pushup" -> when (sa.pushups) {
                Ability.INGEN -> { stage = 0; target = 8 }
                Ability.LITT -> { stage = 3; target = 2 }
                Ability.MIDDELS -> { stage = 3; target = 4 }
                Ability.MYE -> { stage = 3; target = 8 }
                Ability.VET_IKKE -> if (lvl == Level.NYBEGYNNER) { stage = 1; target = 5 } else { stage = 2; target = 6 }
            }
            "pullup" -> when (sa.pullups) {
                Ability.INGEN -> { stage = 0; target = 10 }
                Ability.LITT -> { stage = 2; target = 2 }
                Ability.MIDDELS, Ability.MYE -> { stage = 3; target = if (sa.pullups == Ability.MYE) 4 else 2 }
                Ability.VET_IKKE -> if (lvl == Level.NYBEGYNNER) { stage = 0; target = 10 } else { stage = 1; target = 4 }
            }
            "plank" -> { stage = 1; target = when (sa.plank) { Ability.MIDDELS -> 20; Ability.MYE -> 35; else -> 10 } }
            "squat" -> if (lvl != Level.NYBEGYNNER) { stage = 1; target = 10 }
            "sideplank" -> if (lvl == Level.ERFAREN) { stage = 1; target = 10 }
        }
        // Ikke start på et trinn som krever utstyr brukeren ikke har
        while (stage > 0 && !stageAllowed(ex.stages[stage], profile)) { stage--; target = ex.stages[stage].start }
        return ExerciseState(ex.id, stage, target, defaultWeight(ex.id), lastUpdatedDay = today)
    }

    private fun minTarget(stage: Stage) = when (stage.measure) {
        Measure.SECONDS, Measure.SECONDS_PER_SIDE -> 5
        Measure.MINUTES -> 5
        else -> 1
    }

    /**
     * Kjernen i progresjonen. Endrer KUN på bakgrunn av faktisk prestasjon i en økt –
     * aldri fordi tiden går. Krever mestring (alle sett nådd) før økning.
     */
    fun evaluate(ex: Exercise, state: ExerciseState, r: ExerciseResult, profile: UserProfile, today: Long): Decision {
        val stage = ex.stages[state.stage.coerceIn(0, ex.stages.lastIndex)]
        val fixed = stage.step == 0 // tøying o.l. – ingen progresjon
        if (r.pain) {
            return Decision(
                state.copy(painUntilDay = today + 7, successStreak = 0, struggleStreak = 0, lastUpdatedDay = today, lastEvaluatedDay = today),
                ChangeKind.PAIN,
                "${ex.name}: Vi tar en pause fra øvelsen i en uke. Gjør det fortsatt vondt, snakk med en voksen."
            )
        }
        val newWeight = if (r.weightKg > 0f) r.weightKg else state.weightKg
        val base = state.copy(weightKg = newWeight, lastUpdatedDay = today, lastEvaluatedDay = today)
        if (fixed) return Decision(base, ChangeKind.HOLD, "${ex.name}: Bra!")
        // Én progresjon per øvelse per dag – hindrer hopp hvis samme øvelse gjøres to ganger.
        if (state.lastEvaluatedDay == today) {
            return Decision(base, ChangeKind.HOLD, "${ex.name}: Samme mål neste gang.")
        }
        val values = r.setValues
        val hit = values.size >= r.setsPlanned && values.isNotEmpty() && values.all { it >= r.target }
        val ratio = if (r.target > 0 && values.isNotEmpty()) values.average() / r.target else 0.0

        if (hit) {
            val ready = when (r.rating) {
                Rating.LETT -> true
                Rating.PASSE -> state.successStreak + 1 >= 2
                Rating.VANSKELIG -> false
            }
            if (!ready) {
                val streak = if (r.rating == Rating.VANSKELIG) 0 else state.successStreak + 1
                val msg = if (r.rating == Rating.VANSKELIG) "${ex.name}: Du klarte målet! Vi holder det samme til det kjennes lettere."
                          else "${ex.name}: Målet nådd! Klarer du det én gang til, øker vi."
                return Decision(base.copy(successStreak = streak, struggleStreak = 0), ChangeKind.HOLD, msg)
            }
            return progress(ex, base.copy(successStreak = 0, struggleStreak = 0), profile)
        }
        // Ikke nådd målet: aldri straff – hold eller juster litt ned.
        if (ratio >= 0.75 && r.rating != Rating.VANSKELIG) {
            val struggles = state.struggleStreak + 1
            if (struggles < 2) return Decision(base.copy(successStreak = 0, struggleStreak = struggles), ChangeKind.HOLD,
                "${ex.name}: Nesten! Vi prøver samme mål neste gang.")
        }
        if (ratio >= 0.75 && r.rating == Rating.VANSKELIG && state.struggleStreak == 0) {
            return Decision(base.copy(successStreak = 0, struggleStreak = 1), ChangeKind.HOLD,
                "${ex.name}: Tøft i dag – det er helt vanlig. Samme mål neste gang.")
        }
        return regress(ex, base.copy(successStreak = 0, struggleStreak = 0), profile, actualBest = values.maxOrNull() ?: 0)
    }

    private fun progress(ex: Exercise, s: ExerciseState, profile: UserProfile): Decision {
        val stage = ex.stages[s.stage]
        val next = s.target + stage.step
        if (next <= stage.max) {
            return Decision(s.copy(target = next), ChangeKind.UP, "${ex.name}: Neste gang $next ${stage.measure.unit} 🚀")
        }
        val nextIdx = s.stage + 1
        if (nextIdx <= ex.stages.lastIndex && stageAllowed(ex.stages[nextIdx], profile)) {
            val ns = ex.stages[nextIdx]
            return Decision(s.copy(stage = nextIdx, target = ns.start), ChangeKind.STAGE_UP,
                "Nytt nivå! Du er klar for «${ns.name}» – ${ns.start} ${ns.measure.unit} 🎉")
        }
        if (stage.weighted) {
            val w = s.weightKg + weightStep(s.weightKg)
            return Decision(s.copy(weightKg = w, target = stage.start), ChangeKind.WEIGHT_UP,
                "${ex.name}: Prøv ${fmtKg(w)} kg hvis du har det, med ${stage.start} reps. Har du ikke tyngre manualer, gjør repetisjonene saktere.")
        }
        return Decision(s.copy(target = stage.max), ChangeKind.MAXED, "${ex.name}: Du har nådd toppen av stigen – sterkt! 🏆")
    }

    private fun regress(ex: Exercise, s: ExerciseState, profile: UserProfile, actualBest: Int): Decision {
        val stage = ex.stages[s.stage]
        val min = minTarget(stage)
        val down = max(s.target - stage.step, min)
        // Bruk faktisk prestasjon som pekepinn, men aldri under minimum.
        val realistic = max(minOf(down, max(actualBest, min)), min)
        val belowStage = s.stage > 0 && (actualBest < stage.start || realistic < stage.start)
        if (!belowStage) {
            return Decision(s.copy(target = realistic), ChangeKind.DOWN,
                "${ex.name}: Vi justerer til $realistic ${stage.measure.unit} – så bygger vi deg opp igjen derfra.")
        }
        var prev = s.stage - 1
        while (prev > 0 && !stageAllowed(ex.stages[prev], profile)) prev--
        val ps = ex.stages[prev]
        val t = ((ps.start + ps.max) / 2.0).roundToInt()
        return Decision(s.copy(stage = prev, target = t), ChangeKind.STAGE_DOWN,
            "${ex.name}: Vi bygger mer styrke med «${ps.name}» først ($t ${ps.measure.unit}). Smart trening!")
    }

    fun fmtKg(w: Float): String = if (w % 1f == 0f) w.toInt().toString() else String.format("%.1f", w)
}
