package no.muscly.app.ui

import androidx.compose.runtime.*
import no.muscly.app.domain.*

enum class Phase { WARMUP, EXERCISE, COOLDOWN }
enum class TimerKind { REST, WORK, INTERVAL, MINUTES }

data class IntervalPhase(val label: String, val seconds: Int, val work: Boolean)

data class RunningTimer(
    val kind: TimerKind, val ex: Int, val set: Int, val startedAt: Long, val durationMs: Long,
    val phases: List<IntervalPhase> = emptyList(), val label: String = "",
) {
    fun remainingMs(now: Long) = (durationMs - (now - startedAt)).coerceAtLeast(0)
    fun elapsedMs(now: Long) = (now - startedAt).coerceIn(0, durationMs)
    /** Hvilken intervallfase vi er i, og hvor mange sekunder som gjenstår av den. */
    fun phaseAt(now: Long): Triple<Int, IntervalPhase?, Int> {
        var t = elapsedMs(now) / 1000.0
        phases.forEachIndexed { i, p -> if (t < p.seconds) return Triple(i, p, (p.seconds - t).toInt() + 1); t -= p.seconds }
        return Triple(phases.size, null, 0)
    }
    fun workPhasesDone(now: Long): Int { val (i, _, _) = phaseAt(now); return phases.take(i).count { it.work } }
}

enum class Cue { TICK, DONE, SET_DONE }

/** Tilstand for én øvelse i en pågående økt. */
class ExerciseProgress(val planned: PlannedExercise) {
    val ex = ExerciseLibrary.get(planned.exerciseId)
    val stage = ex.stages[planned.stageIndex]
    val values = mutableStateListOf<Int>().apply { repeat(planned.sets) { add(planned.target) } }
    val done = mutableStateListOf<Boolean>().apply { repeat(planned.sets) { add(false) } }
    var weight by mutableFloatStateOf(planned.weightKg)
    var rating by mutableStateOf<Rating?>(null)
    var pain by mutableStateOf(false)
    var skipped by mutableStateOf(false)
    val allDone get() = done.all { it }
    val anyDone get() = done.any { it }
    fun toResult(): ExerciseResult = ExerciseResult(
        exerciseId = planned.exerciseId, stage = planned.stageIndex, target = planned.target, setsPlanned = planned.sets,
        setValues = values.filterIndexed { i, _ -> done[i] }, weightKg = if (stage.weighted) weight else 0f,
        rating = rating ?: Rating.PASSE, pain = pain,
    )
}

/** Tilstandsmaskin for treningsmodus. Ren logikk, testbar uten UI. */
class WorkoutSession(val workout: PlannedWorkout, private val clock: () -> Long = { System.currentTimeMillis() }) {
    val startedAt = clock()
    val items = workout.exercises.map { ExerciseProgress(it) }
    var phase by mutableStateOf(if (workout.warmup.isEmpty()) Phase.EXERCISE else Phase.WARMUP)
    var index by mutableIntStateOf(0)
    var timer by mutableStateOf<RunningTimer?>(null)
    val warmupDone = mutableStateListOf<Boolean>().apply { repeat(workout.warmup.size) { add(false) } }
    var onCue: (Cue) -> Unit = {}

    val current get() = items.getOrNull(index)
    val completedCount get() = items.count { it.anyDone && !it.pain }
    val progress: Float get() = if (items.isEmpty()) 1f else (items.count { it.allDone || it.skipped || it.pain } / items.size.toFloat())

    fun startExercises() { phase = Phase.EXERCISE; index = 0 }

    fun setValue(set: Int, v: Int) { current?.let { it.values[set] = v.coerceIn(0, 999) } }

    /** Markerer et sett som ferdig og starter pause. */
    fun completeSet(set: Int) {
        val c = current ?: return
        c.done[set] = true
        onCue(Cue.SET_DONE)
        val more = c.done.any { !it }
        timer = if (more && c.planned.restSec > 0) RunningTimer(TimerKind.REST, index, set, clock(), c.planned.restSec * 1000L, label = "Pause") else null
    }

    fun undoSet(set: Int) { current?.let { it.done[set] = false } }

    /** Starter nedtelling for tidsbaserte øvelser / intervaller / minutter. */
    fun startTimer(set: Int) {
        val c = current ?: return
        val st = c.stage
        timer = when (st.measure) {
            Measure.SECONDS -> RunningTimer(TimerKind.WORK, index, set, clock(), c.values[set] * 1000L, label = st.name)
            Measure.SECONDS_PER_SIDE -> {
                val ph = listOf(IntervalPhase("Venstre side", c.values[set], true), IntervalPhase("Bytt side", 5, false), IntervalPhase("Høyre side", c.values[set], true))
                RunningTimer(TimerKind.INTERVAL, index, set, clock(), ph.sumOf { it.seconds } * 1000L, ph, st.name)
            }
            Measure.MINUTES -> RunningTimer(TimerKind.MINUTES, index, set, clock(), c.values[set] * 60_000L, label = st.name)
            Measure.ROUNDS -> if (st.workSec > 0) {
                val ph = mutableListOf(IntervalPhase("Oppvarming – rolig", 120, false))
                repeat(c.values[set]) { r -> ph += IntervalPhase("HARDT! Runde ${r + 1}", st.workSec, true); ph += IntervalPhase("Rolig", st.restSec, false) }
                ph += IntervalPhase("Nedtrapping – rolig", 120, false)
                RunningTimer(TimerKind.INTERVAL, index, set, clock(), ph.sumOf { it.seconds } * 1000L, ph, st.name)
            } else null
            else -> null
        }
    }

    /** Kalles jevnlig fra UI. Fullfører timere automatisk. */
    fun tick(now: Long = clock()) {
        val t = timer ?: return
        if (t.remainingMs(now) > 0) {
            val rem = (t.remainingMs(now) / 1000).toInt()
            if (t.kind == TimerKind.REST && rem in 1..3) onCue(Cue.TICK)
            return
        }
        timer = null
        onCue(Cue.DONE)
        if (t.kind != TimerKind.REST) {
            val c = items[t.ex]
            if (c.stage.measure == Measure.ROUNDS) c.values[t.set] = t.phases.count { it.work }
            index = t.ex
            completeSet(t.set)
        }
    }

    /** Stopper en arbeidstimer tidlig og lagrer det som faktisk ble gjort. */
    fun stopTimerEarly(now: Long = clock()) {
        val t = timer ?: return
        timer = null
        if (t.kind == TimerKind.REST) return
        val c = items[t.ex]
        val v = when (t.kind) {
            TimerKind.WORK -> (t.elapsedMs(now) / 1000).toInt()
            TimerKind.MINUTES -> (t.elapsedMs(now) / 60000).toInt()
            TimerKind.INTERVAL -> if (c.stage.measure == Measure.ROUNDS) t.workPhasesDone(now) else (t.elapsedMs(now) / 2000).toInt()
            else -> 0
        }
        if (v > 0) { c.values[t.set] = v; completeSet(t.set); timer = null }
    }

    fun skipRest() { if (timer?.kind == TimerKind.REST) timer = null }
    fun addRest(sec: Int) { timer = timer?.let { if (it.kind == TimerKind.REST) it.copy(durationMs = it.durationMs + sec * 1000) else it } }

    fun rate(r: Rating) { current?.rating = r }
    fun reportPain() { current?.let { it.pain = true }; timer = null; next() }
    fun skip() { current?.let { if (!it.anyDone) it.skipped = true }; timer = null; next() }

    fun next() {
        timer = null
        if (index < items.lastIndex) index++ else phase = if (workout.cooldown.isEmpty()) Phase.COOLDOWN else Phase.COOLDOWN
    }
    fun prev() { timer = null; if (index > 0) index-- else if (workout.warmup.isNotEmpty()) phase = Phase.WARMUP }

    fun results(): List<ExerciseResult> = items.filter { it.anyDone || it.pain }.map { it.toResult() }
}
