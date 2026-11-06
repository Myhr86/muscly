package no.muscly.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import no.muscly.app.AppContainer
import no.muscly.app.coach.CoachContext
import no.muscly.app.data.*
import no.muscly.app.domain.*
import no.muscly.app.notify.Reminders

data class AppState(
    val loaded: Boolean = false,
    val profile: UserProfile? = null,
    val states: Map<String, ExerciseState> = emptyMap(),
    val sessions: List<SessionEntity> = emptyList(),
    val logs: List<ExerciseLogEntity> = emptyList(),
    val milestones: List<MilestoneEntity> = emptyList(),
    val today: Long = Dates.today(),
) {
    val weekPlan: List<DayType> get() = profile?.let { ProgramGenerator.weekPlan(it) } ?: List(7) { DayType.HVILE }
    val todayType: DayType get() = weekPlan[Dates.dow(today)]
    val doneToday: Boolean get() = sessions.any { it.day == today }
    val weekStart: Long get() = Dates.weekStart(today)
    val sessionsThisWeek: List<SessionEntity> get() = sessions.filter { it.day >= weekStart && it.day < weekStart + 7 }
    val plannedThisWeek: Int get() = weekPlan.count { it != DayType.HVILE }
    fun coachContext() = CoachContext(profile ?: UserProfile(), states, sessions, logs, weekPlan, today)
    fun buildWorkout(type: DayType, minutes: Int? = null): PlannedWorkout =
        WorkoutBuilder.build(type, profile ?: UserProfile(), states, sessions.size, Dates.weekIndex(today), today, minutes)
}

class AppViewModel(app: Application, val container: AppContainer) : AndroidViewModel(app) {
    private val repo = container.repository

    val state: StateFlow<AppState> = combine(
        repo.profile, repo.states, repo.sessions, repo.logs, repo.milestones,
    ) { p, s, ses, logs, ms -> AppState(true, p, s, ses, logs, ms, Dates.today()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    val chat = repo.chat.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _coachBusy = MutableStateFlow(false)
    val coachBusy: StateFlow<Boolean> = _coachBusy

    /** Sist fullførte økt – vises på oppsummeringssiden. */
    var lastSummary: SessionSummary? = null

    fun completeOnboarding(p: UserProfile, sa: SelfAssessment) = viewModelScope.launch {
        repo.completeOnboarding(p, sa)
        Reminders.schedule(getApplication(), p.copy(onboarded = true))
    }

    fun updateProfile(p: UserProfile) = viewModelScope.launch {
        repo.saveProfile(p)
        Reminders.schedule(getApplication(), p)
    }

    fun clearPain(id: String) = viewModelScope.launch {
        repo.statesNow()[id]?.let { repo.updateState(it.copy(painUntilDay = 0)) }
    }

    suspend fun saveWorkout(w: PlannedWorkout, results: List<ExerciseResult>, started: Long, finished: Long): SessionSummary {
        val s = repo.saveSession(w, results, started, finished)
        lastSummary = s
        return s
    }

    fun sendToCoach(text: String) = viewModelScope.launch {
        val t = text.trim(); if (t.isEmpty()) return@launch
        repo.dao.insertChat(ChatEntity(fromUser = true, text = t, time = System.currentTimeMillis()))
        _coachBusy.value = true
        try {
            val history = repo.dao.lastChat(12).reversed().map { it.fromUser to it.text }
            val reply = container.coach.reply(t, state.value.coachContext(), history.dropLast(1))
            repo.dao.insertChat(ChatEntity(fromUser = false, text = reply.text, time = System.currentTimeMillis(), action = reply.action, source = reply.source))
        } catch (e: Exception) {
            repo.dao.insertChat(ChatEntity(fromUser = false, text = "Oi, noe gikk galt. Prøv igjen!", time = System.currentTimeMillis()))
        } finally { _coachBusy.value = false }
    }

    /** Tar i bruk et programforslag fra treneren. Progresjonen per øvelse beholdes. */
    fun applyPlan(json: String) = viewModelScope.launch {
        val prop = no.muscly.app.coach.PlanProposal.fromJson(json) ?: return@launch
        val p = repo.profileNow() ?: return@launch
        val np = prop.apply(p)
        repo.saveProfile(np)
        Reminders.schedule(getApplication(), np)
        repo.dao.insertChat(ChatEntity(fromUser = false, time = System.currentTimeMillis(),
            text = "✅ Programmet er oppdatert! Fokus: ${np.focus.label}, ${np.schedule.label.lowercase()}, ${np.minutes} min. Se Trening → Mitt program. Du kan alltid justere under Profil."))
    }

    fun clearChat() = viewModelScope.launch { repo.dao.clearChat() }

    fun resetAll() = viewModelScope.launch {
        repo.resetAll()
        Reminders.schedule(getApplication(), UserProfile(onboarded = false))
    }
}
