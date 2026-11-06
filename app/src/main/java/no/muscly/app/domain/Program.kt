package no.muscly.app.domain

import kotlin.math.max
import kotlin.math.min

object ProgramGenerator {

    fun trainingDays(p: UserProfile): Set<Int> = when (p.schedule) {
        ScheduleType.TRE -> setOf(0, 2, 4)
        ScheduleType.FIRE -> setOf(0, 1, 3, 5)
        ScheduleType.ANNENHVER -> setOf(0, 2, 4, 6)
        ScheduleType.HVER_DAG -> (0..6).toSet()
        ScheduleType.EGEN -> p.customDays.ifEmpty { setOf(0, 2, 4) }
    }

    /** Maks antall harde styrkeøkter per uke – trygt for ungdom/nybegynnere. */
    fun maxHard(p: UserProfile): Int {
        val byLevel = when (p.level) { Level.NYBEGYNNER -> 3; Level.LITT_ERFAREN -> 3; Level.ERFAREN -> 4 }
        return min(byLevel, p.maxHardPerWeek.coerceIn(1, 4))
    }

    private fun hardRotation(f: Focus): List<DayType> = when (f) {
        Focus.HELKROPP -> listOf(DayType.HELKROPP, DayType.HELKROPP_OVER, DayType.HELKROPP, DayType.HELKROPP_OVER)
        Focus.OVERKROPP -> listOf(DayType.OVERKROPP, DayType.BEIN_KJERNE, DayType.HELKROPP_OVER, DayType.OVERKROPP)
        Focus.ARMER -> listOf(DayType.ARMER, DayType.BEIN_KJERNE, DayType.HELKROPP_OVER, DayType.ARMER)
        Focus.BEIN -> listOf(DayType.BEIN_KJERNE, DayType.OVERKROPP, DayType.HELKROPP, DayType.BEIN_KJERNE)
        Focus.KJERNE -> listOf(DayType.HELKROPP, DayType.BEIN_KJERNE, DayType.HELKROPP_OVER, DayType.OVERKROPP)
        Focus.KONDISJON -> listOf(DayType.HELKROPP, DayType.HELKROPP_OVER, DayType.HELKROPP)
        Focus.AUTO, Focus.KOMBINASJON -> listOf(DayType.HELKROPP, DayType.BEIN_KJERNE, DayType.HELKROPP_OVER, DayType.OVERKROPP)
    }

    private fun fillers(f: Focus): List<DayType> = when (f) {
        Focus.KONDISJON -> listOf(DayType.KONDISJON, DayType.LETT, DayType.KONDISJON, DayType.KJERNE_KONDIS)
        Focus.KJERNE -> listOf(DayType.KJERNE_KONDIS, DayType.LETT, DayType.KONDISJON, DayType.LETT)
        else -> listOf(DayType.KONDISJON, DayType.LETT, DayType.KJERNE_KONDIS, DayType.LETT)
    }

    fun overlaps(a: DayType, b: DayType): Boolean =
        a.intensity == Intensity.HARD && b.intensity == Intensity.HARD && a.loads.intersect(b.loads).isNotEmpty()

    /** Lager en ukeplan (indeks 0 = mandag). Samme muskelgruppe belastes aldri hardt to dager på rad. */
    fun weekPlan(p: UserProfile): List<DayType> {
        val days = trainingDays(p).sorted()
        val n = days.size
        val hardTarget = when {
            p.focus == Focus.KONDISJON -> min(2, n)
            n <= 3 -> n
            n <= 5 -> 3
            else -> 4
        }.let { min(it, maxHard(p)) }

        // Velg hvilke dager som blir harde: maksimer avstand og unngå dager på rad.
        val hardDays = bestSpread(days, hardTarget)
        val plan = MutableList(7) { DayType.HVILE }
        val rot = hardRotation(p.focus)
        var ri = 0
        var fi = 0
        val fill = fillers(p.focus)
        for (d in days) {
            if (d in hardDays) {
                val prev = plan[(d + 6) % 7]
                var chosen: DayType? = null
                for (k in rot.indices) {
                    val cand = rot[(ri + k) % rot.size]
                    if (!overlaps(prev, cand)) { chosen = cand; ri = (ri + k + 1) % rot.size; break }
                }
                plan[d] = chosen ?: fill[fi++ % fill.size]
            } else {
                plan[d] = fill[fi++ % fill.size]
            }
        }
        // Sjekk søndag -> mandag (ny uke)
        if (overlaps(plan[6], plan[0])) plan[6] = DayType.LETT
        // Hver dag: sørg for minst én lett dag etter to harde/moderate dager på rad
        for (i in 0 until 7) {
            val a = plan[i]; val b = plan[(i + 1) % 7]; val c = plan[(i + 2) % 7]
            if (n == 7 && a.intensity != Intensity.LETT && b.intensity != Intensity.LETT && c.intensity != Intensity.LETT &&
                c.intensity != Intensity.HVILE && c.intensity != Intensity.HARD) plan[(i + 2) % 7] = DayType.LETT
        }
        return plan
    }

    private fun bestSpread(days: List<Int>, k: Int): Set<Int> {
        if (k <= 0) return emptySet()
        if (k >= days.size) return days.toSet()
        var best: List<Int> = days.take(k)
        var bestScore = Int.MIN_VALUE
        fun rec(start: Int, cur: List<Int>) {
            if (cur.size == k) {
                var score = 0
                for (i in cur.indices) {
                    val a = cur[i]; val b = cur[(i + 1) % cur.size]
                    val gap = if (i == cur.lastIndex) (b + 7 - a) else (b - a)
                    score += if (gap <= 1) -100 else min(gap, 3) * 10
                }
                // foretrekk tidlig i uka ved likhet
                score -= cur.sum()
                if (score > bestScore) { bestScore = score; best = cur }
                return
            }
            for (i in start until days.size) rec(i + 1, cur + days[i])
        }
        rec(0, emptyList())
        return best.toSet()
    }
}

/** Setter sammen innholdet i en økt ut fra dagstype, profil og progresjon. */
object WorkoutBuilder {

    private fun slots(t: DayType): List<List<String>> = when (t) {
        DayType.HELKROPP -> listOf(listOf("squat"), listOf("pushup"), listOf("pullup"), listOf("lunge", "stepup"), listOf("row", "curl"), listOf("plank"), listOf("bridge"))
        DayType.HELKROPP_OVER -> listOf(listOf("pushup"), listOf("pullup"), listOf("curl"), listOf("squat", "bridge"), listOf("press", "hammer"), listOf("sideplank", "deadbug"), listOf("row"))
        DayType.OVERKROPP -> listOf(listOf("pushup"), listOf("pullup"), listOf("row"), listOf("curl"), listOf("press"), listOf("plank"), listOf("dips", "hammer"))
        DayType.ARMER -> listOf(listOf("pullup"), listOf("curl"), listOf("pushup"), listOf("hammer"), listOf("dips"), listOf("plank"), listOf("row"))
        DayType.BEIN_KJERNE -> listOf(listOf("squat"), listOf("stepup", "lunge"), listOf("bridge"), listOf("plank", "deadbug"), listOf("calf", "wallsit"), listOf("sideplank", "kneeraise"), listOf("lunge", "stepup"))
        DayType.KJERNE_KONDIS -> listOf(listOf("bike_interval", "stairs"), listOf("plank"), listOf("deadbug", "birddog"), listOf("mountain"), listOf("sideplank", "kneeraise"), listOf("superman"))
        DayType.KONDISJON -> listOf(listOf("bike_interval", "stairs"), listOf("mountain", "jacks"), listOf("plank", "deadbug"), listOf("stairs", "bike_interval"))
        DayType.LETT -> listOf(listOf("bike_easy", "stairs"), listOf("birddog"), listOf("stretch_quad"), listOf("stretch_hip"), listOf("stretch_ham"), listOf("stretch_child"))
        DayType.HVILE -> emptyList()
    }

    fun setsFor(ex: Exercise, profile: UserProfile, completedSessions: Int, dayType: DayType): Int = when {
        ex.category == Category.KONDISJON || ex.category == Category.BEVEGELIGHET -> 1
        dayType == DayType.KJERNE_KONDIS || dayType == DayType.LETT || dayType == DayType.KONDISJON -> 2
        profile.level == Level.NYBEGYNNER && completedSessions < 8 -> 2
        profile.level == Level.ERFAREN -> 3
        completedSessions < 4 -> 2
        else -> 3
    }

    fun secondsPerSet(stage: Stage, target: Int): Int {
        val side = if (stage.measure.perSide) 2 else 1
        return when (stage.measure) {
            Measure.REPS, Measure.REPS_PER_SIDE -> (target * 3.5 * side).toInt() + 5
            Measure.SECONDS, Measure.SECONDS_PER_SIDE -> target * side + 8
            Measure.MINUTES -> target * 60
            Measure.ROUNDS -> if (stage.workSec > 0) target * (stage.workSec + stage.restSec) + 300 else target * 50
        }
    }

    fun estimateSeconds(list: List<PlannedExercise>): Int = list.sumOf { pe ->
        val ex = ExerciseLibrary.get(pe.exerciseId)
        val st = ex.stages[pe.stageIndex]
        pe.sets * secondsPerSet(st, pe.target) + (pe.sets - 1).coerceAtLeast(0) * pe.restSec + 30
    }

    fun restFor(ex: Exercise, profile: UserProfile): Int = when (profile.focus) {
        Focus.KOMBINASJON, Focus.KONDISJON -> (ex.restSec * 0.75).toInt()
        else -> ex.restSec
    }

    fun build(
        dayType: DayType, profile: UserProfile, states: Map<String, ExerciseState>,
        completedSessions: Int, weekIndex: Long, today: Long, minutesOverride: Int? = null,
    ): PlannedWorkout {
        val minutes = minutesOverride ?: profile.minutes
        if (dayType == DayType.HVILE) return PlannedWorkout(dayType, dayType.title, emptyList(), emptyList(), emptyList(), 0)
        val budget = (minutes - (if (dayType == DayType.LETT) 2 else 6)) * 60
        val chosen = mutableListOf<String>()
        val alt = (weekIndex % 2).toInt()
        var slotList = slots(dayType)
        // Prioriter muskelgrupper og favorittøvelser
        val extra = mutableListOf<List<String>>()
        if (Category.ARMER in profile.priorities && dayType.loads.contains(BodyArea.OVER)) extra += listOf("curl")
        if (Category.KJERNE in profile.priorities && dayType.intensity == Intensity.HARD) extra += listOf("deadbug", "plank")
        if (Category.BEIN in profile.priorities && dayType.loads.contains(BodyArea.UNDER)) extra += listOf("stepup", "squat")
        slotList = slotList.take(3) + extra + slotList.drop(3)

        fun usable(id: String): Boolean {
            val ex = ExerciseLibrary.find(id) ?: return false
            if (!ExerciseLibrary.available(profile, ex)) return false
            val st = states[id]
            if (st != null && st.painUntilDay > today) return false
            return id !in chosen
        }

        val planned = mutableListOf<PlannedExercise>()
        for (slot in slotList) {
            val ordered = if (slot.size > 1) slot.drop(alt % slot.size) + slot.take(alt % slot.size) else slot
            val liked = ordered.firstOrNull { it in profile.liked && usable(it) }
            var pick = liked ?: ordered.firstOrNull { usable(it) }
            if (pick == null) {
                // Prøv alternativer til første øvelse i slotten
                pick = ExerciseLibrary.find(ordered.first())?.alternatives?.firstOrNull { usable(it) }
            }
            if (pick == null) continue
            val pe0 = plan(pick, profile, states, completedSessions, dayType, today)
            val pe = if (minutes <= 20) pe0.copy(sets = minOf(pe0.sets, 2)) else pe0
            val est = estimateSeconds(planned + pe)
            if (planned.size >= 3 && est > budget) break
            chosen += pick
            planned += pe
        }
        // Kombinasjon: legg til kort kondisjonsavslutning på styrkedager
        if (profile.focus == Focus.KOMBINASJON && dayType.intensity == Intensity.HARD && minutes >= 30 && usable("jacks")) {
            val pe = plan("jacks", profile, states, completedSessions, dayType, today).copy(sets = 2)
            if (estimateSeconds(planned + pe) <= budget + 120) planned += pe
        }
        val warm = if (dayType == DayType.LETT) emptyList() else ExerciseLibrary.warmup
        val cool = if (dayType == DayType.LETT) emptyList() else ExerciseLibrary.cooldown
        val est = (estimateSeconds(planned) / 60) + (if (warm.isEmpty()) 0 else 6)
        return PlannedWorkout(dayType, dayType.title, planned, warm, cool, max(est, 5))
    }

    fun plan(id: String, profile: UserProfile, states: Map<String, ExerciseState>, completed: Int, dayType: DayType, today: Long): PlannedExercise {
        val ex = ExerciseLibrary.get(id)
        val st = states[id] ?: ProgressionEngine.initialState(ex, profile, SelfAssessment(), today)
        val stageIdx = st.stage.coerceIn(0, ex.stages.lastIndex)
        val stage = ex.stages[stageIdx]
        // Lette dager: litt lavere belastning
        val target = if (dayType == DayType.LETT && stage.step > 0 && stage.measure != Measure.MINUTES)
            max(stage.start, (st.target * 0.8).toInt()) else st.target
        return PlannedExercise(id, stageIdx, setsFor(ex, profile, completed, dayType), target,
            if (stage.weighted) st.weightKg else 0f, restFor(ex, profile))
    }

    /** Velg en passende økt for i dag ut fra hva som ble trent de siste dagene. */
    fun suggestForToday(recent: List<Pair<Long, DayType>>, today: Long): DayType {
        val yesterday = recent.filter { it.first == today - 1 }.map { it.second }
        val loadsY = yesterday.filter { it.intensity == Intensity.HARD }.flatMap { it.loads }.toSet()
        val hardLast3 = recent.count { it.first >= today - 2 && it.second.intensity == Intensity.HARD }
        return when {
            hardLast3 >= 2 -> DayType.LETT
            loadsY.containsAll(setOf(BodyArea.OVER, BodyArea.UNDER)) -> DayType.KJERNE_KONDIS
            BodyArea.OVER in loadsY -> DayType.BEIN_KJERNE
            BodyArea.UNDER in loadsY -> DayType.OVERKROPP
            else -> DayType.HELKROPP
        }
    }
}
