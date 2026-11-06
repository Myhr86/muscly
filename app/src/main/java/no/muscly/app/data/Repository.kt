package no.muscly.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.muscly.app.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object Dates {
    var clock: () -> LocalDate = { LocalDate.now() }
    fun today(): Long = clock().toEpochDay()
    /** 0 = mandag */
    fun dow(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value - 1
    fun weekIndex(day: Long): Long = Math.floorDiv(day + 3, 7L)
    fun weekStart(day: Long): Long = day - dow(day)
    val dayNames = listOf("Mandag", "Tirsdag", "Onsdag", "Torsdag", "Fredag", "Lørdag", "Søndag")
    val dayShort = listOf("Man", "Tir", "Ons", "Tor", "Fre", "Lør", "Søn")
    private val months = listOf("jan", "feb", "mar", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "des")
    fun label(day: Long): String { val d = LocalDate.ofEpochDay(day); return "${d.dayOfMonth}. ${months[d.monthValue - 1]}" }
}

object ProfileJson {
    fun encode(p: UserProfile, sa: SelfAssessment = SelfAssessment()): String = JSONObject().apply {
        put("name", p.name); put("age", p.age); put("level", p.level.name); put("focus", p.focus.name)
        put("schedule", p.schedule.name); put("customDays", JSONArray(p.customDays.toList()))
        put("minutes", p.minutes); put("equipment", JSONArray(p.equipment.map { it.name }))
        put("liked", JSONArray(p.liked.toList())); put("avoided", JSONArray(p.avoided.toList()))
        put("injuries", JSONArray(p.injuries.map { it.name })); put("priorities", JSONArray(p.priorities.map { it.name }))
        put("remindersOn", p.remindersOn); put("reminderHour", p.reminderHour); put("reminderMinute", p.reminderMinute)
        put("followUpOn", p.followUpOn); put("weeklySummaryOn", p.weeklySummaryOn); put("theme", p.theme)
        put("aiEnabled", p.aiEnabled); put("coachUrl", p.coachUrl); put("parentPin", p.parentPin)
        put("maxHardPerWeek", p.maxHardPerWeek); put("onboarded", p.onboarded)
        put("sa", JSONObject().put("pushups", sa.pushups.name).put("pullups", sa.pullups.name).put("plank", sa.plank.name))
    }.toString()

    private fun JSONObject.strings(k: String): List<String> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
    private inline fun <reified T : Enum<T>> en(v: String?, d: T): T = runCatching { enumValueOf<T>(v ?: "") }.getOrDefault(d)
    private inline fun <reified T : Enum<T>> ens(l: List<String>): Set<T> = l.mapNotNull { runCatching { enumValueOf<T>(it) }.getOrNull() }.toSet()

    fun decode(s: String): UserProfile {
        val d = UserProfile()
        return runCatching {
            val o = JSONObject(s)
            UserProfile(
                name = o.optString("name", ""), age = o.optInt("age", 14),
                level = en(o.optString("level"), d.level), focus = en(o.optString("focus"), d.focus),
                schedule = en(o.optString("schedule"), d.schedule),
                customDays = o.optJSONArray("customDays")?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: d.customDays,
                minutes = o.optInt("minutes", 30), equipment = ens<Equipment>(o.strings("equipment")),
                liked = o.strings("liked").toSet(), avoided = o.strings("avoided").toSet(),
                injuries = ens(o.strings("injuries")), priorities = ens(o.strings("priorities")),
                remindersOn = o.optBoolean("remindersOn", true), reminderHour = o.optInt("reminderHour", 17),
                reminderMinute = o.optInt("reminderMinute", 0), followUpOn = o.optBoolean("followUpOn", true),
                weeklySummaryOn = o.optBoolean("weeklySummaryOn", true), theme = o.optString("theme", "dark"),
                aiEnabled = o.optBoolean("aiEnabled", true), coachUrl = o.optString("coachUrl", ""),
                parentPin = o.optString("parentPin", ""), maxHardPerWeek = o.optInt("maxHardPerWeek", 3),
                onboarded = o.optBoolean("onboarded", false),
            )
        }.getOrDefault(d)
    }

    fun decodeSa(s: String): SelfAssessment = runCatching {
        val o = JSONObject(s).getJSONObject("sa")
        SelfAssessment(enumValueOf(o.getString("pushups")), enumValueOf(o.getString("pullups")), enumValueOf(o.getString("plank")))
    }.getOrDefault(SelfAssessment())
}

data class PersonalRecord(val exerciseId: String, val stage: Int, val value: Int, val previous: Int)

data class SessionSummary(
    val sessionId: Long,
    val completed: Int,
    val total: Int,
    val decisions: List<Decision>,
    val records: List<PersonalRecord>,
    val newMilestones: List<Milestone>,
    val durationSec: Int,
)

fun ExerciseStateEntity.toDomain() = ExerciseState(exerciseId, stage, target, weightKg, successStreak, struggleStreak, lastUpdatedDay, painUntilDay, lastEvaluatedDay)
fun ExerciseState.toEntity() = ExerciseStateEntity(exerciseId, stage, target, weightKg, successStreak, struggleStreak, lastUpdatedDay, painUntilDay, lastEvaluatedDay)

class Repository(private val db: MusclyDatabase) {
    val dao = db.dao()

    val profile: Flow<UserProfile?> = dao.profile().map { it?.let { e -> ProfileJson.decode(e.json) } }
    val states: Flow<Map<String, ExerciseState>> = dao.states().map { l -> l.associate { it.exerciseId to it.toDomain() } }
    val sessions = dao.sessions()
    val logs = dao.logs()
    val milestones = dao.milestones()
    val chat = dao.chat()

    suspend fun profileNow(): UserProfile? = dao.profileNow()?.let { ProfileJson.decode(it.json) }

    suspend fun saveProfile(p: UserProfile) {
        val sa = dao.profileNow()?.let { ProfileJson.decodeSa(it.json) } ?: SelfAssessment()
        dao.saveProfile(ProfileEntity(json = ProfileJson.encode(p, sa)))
    }

    /** Første oppsett: lagrer profil og lager startnivå for alle øvelser. */
    suspend fun completeOnboarding(p: UserProfile, sa: SelfAssessment) = db.withTransaction {
        val today = Dates.today()
        dao.saveProfile(ProfileEntity(json = ProfileJson.encode(p.copy(onboarded = true), sa)))
        val existing = dao.statesNow().map { it.exerciseId }.toSet()
        val init = ExerciseLibrary.all.filter { it.id !in existing }.map { ProgressionEngine.initialState(it, p, sa, today).toEntity() }
        dao.saveStates(init)
    }

    suspend fun statesNow(): Map<String, ExerciseState> = dao.statesNow().associate { it.exerciseId to it.toDomain() }

    suspend fun updateState(s: ExerciseState) = dao.saveStates(listOf(s.toEntity()))

    suspend fun saveSession(
        workout: PlannedWorkout, results: List<ExerciseResult>, startedAt: Long, finishedAt: Long,
    ): SessionSummary = db.withTransaction {
        val today = Dates.today()
        val profile = profileNow() ?: UserProfile()
        val states = statesNow().toMutableMap()
        val oldLogs = dao.logsNow()
        val prevBest = oldLogs.groupBy { it.exerciseId to it.stage }.mapValues { (_, l) -> l.maxOf { it.values().maxOrNull() ?: 0 } }
        val oldMilestones = dao.milestonesNow().map { it.id }.toSet()

        val completed = results.count { r -> r.setValues.isNotEmpty() && !r.pain }
        val duration = ((finishedAt - startedAt) / 1000).toInt().coerceAtLeast(0)
        val sid = dao.insertSession(SessionEntity(day = today, dayType = workout.dayType.name, title = workout.title,
            startedAt = startedAt, finishedAt = finishedAt, completedExercises = completed, totalExercises = workout.exercises.size, durationSec = duration))

        val decisions = mutableListOf<Decision>()
        val records = mutableListOf<PersonalRecord>()
        val logs = results.filter { it.setValues.isNotEmpty() || it.pain }.map { r ->
            val ex = ExerciseLibrary.get(r.exerciseId)
            val st = states[r.exerciseId] ?: ProgressionEngine.initialState(ex, profile, SelfAssessment(), today)
            val d = ProgressionEngine.evaluate(ex, st, r, profile, today)
            states[r.exerciseId] = d.newState
            decisions += d
            val best = r.setValues.maxOrNull() ?: 0
            val prev = prevBest[r.exerciseId to r.stage]
            if (prev != null && best > prev && !r.pain) records += PersonalRecord(r.exerciseId, r.stage, best, prev)
            ExerciseLogEntity(sessionId = sid, day = today, exerciseId = r.exerciseId, stage = r.stage, target = r.target,
                setsPlanned = r.setsPlanned, sets = r.setValues.joinToString(","), weightKg = r.weightKg, rating = r.rating.name,
                pain = r.pain, decisionKind = d.kind.name, decisionText = d.message)
        }
        dao.insertLogs(logs)
        dao.saveStates(states.values.map { it.toEntity() })

        val snap = snapshot(dao.sessionsNow(), dao.logsNow(), today)
        val newly = Milestones.all.filter { it.id !in oldMilestones && it.check(snap) }
        dao.insertMilestones(newly.map { MilestoneEntity(it.id, today) })
        SessionSummary(sid, completed, workout.exercises.size, decisions, records, newly, duration)
    }

    suspend fun resetAll() = db.withTransaction {
        dao.clearSessions(); dao.clearLogs(); dao.clearStates(); dao.clearMilestones(); dao.clearChat(); dao.clearProfile()
    }

    companion object {
        fun cardioMinutes(l: ExerciseLogEntity): Int {
            val ex = ExerciseLibrary.find(l.exerciseId) ?: return 0
            if (ex.category != Category.KONDISJON) return 0
            val st = ex.stages[l.stage.coerceIn(0, ex.stages.lastIndex)]
            val total = l.values().sum()
            return when (st.measure) {
                Measure.MINUTES -> total
                Measure.ROUNDS -> if (st.workSec > 0) (total * (st.workSec + st.restSec)) / 60 else total
                Measure.SECONDS, Measure.SECONDS_PER_SIDE -> total / 60
                else -> 0
            }
        }

        /** Antall uker på rad (bakover fra denne/forrige uke) med minst 2 økter. */
        fun weeksInARow(sessions: List<SessionEntity>, today: Long): Int {
            val perWeek = sessions.groupingBy { Dates.weekIndex(it.day) }.eachCount()
            var w = Dates.weekIndex(today)
            if ((perWeek[w] ?: 0) < 2) w -= 1
            var n = 0
            while ((perWeek[w] ?: 0) >= 2) { n++; w-- }
            return n
        }

        fun snapshot(sessions: List<SessionEntity>, logs: List<ExerciseLogEntity>, today: Long): StatsSnapshot {
            val best = logs.filter { !it.pain }.groupBy { it.exerciseId to it.stage }.mapValues { (_, l) -> l.maxOf { it.values().maxOrNull() ?: 0 } }
            return StatsSnapshot(
                totalSessions = sessions.size, best = best, weeksInARow = weeksInARow(sessions, today),
                cardioMinutes = logs.sumOf { cardioMinutes(it) }, distinctExercises = logs.map { it.exerciseId }.distinct().size,
            )
        }
    }
}
