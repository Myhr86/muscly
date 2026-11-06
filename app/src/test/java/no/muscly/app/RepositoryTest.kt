package no.muscly.app

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {
    private val c = AppContainer(ApplicationProvider.getApplicationContext(), inMemory = true)
    private val repo = c.repository
    @After fun tearDown() { c.db.close(); Dates.clock = { LocalDate.now() } }

    private fun doWorkout(day: LocalDate, rating: Rating = Rating.PASSE, hitTarget: Boolean = true, pain: Set<String> = emptySet()) = runBlocking {
        Dates.clock = { day }
        val p = repo.profileNow()!!
        val plan = ProgramGenerator.weekPlan(p)
        val type = plan[Dates.dow(Dates.today())].let { if (it == DayType.HVILE) DayType.HELKROPP else it }
        val w = WorkoutBuilder.build(type, p, repo.statesNow(), repo.dao.sessionsNow().size, Dates.weekIndex(Dates.today()), Dates.today())
        val results = w.exercises.map { pe ->
            ExerciseResult(pe.exerciseId, pe.stageIndex, pe.target, pe.sets, List(pe.sets) { if (hitTarget) pe.target else (pe.target / 2) }, pe.weightKg, rating, pe.exerciseId in pain)
        }
        repo.saveSession(w, results, 0, 20 * 60_000L)
    }

    @Test fun `onboarding, økter, progresjon, rekorder og milepæler`() = runBlocking {
        val p = UserProfile(name = "Test", schedule = ScheduleType.TRE)
        repo.completeOnboarding(p, SelfAssessment(pushups = Ability.LITT, pullups = Ability.INGEN))
        assertTrue(repo.profileNow()!!.onboarded)
        val st0 = repo.statesNow()
        assertEquals(ExerciseLibrary.all.size, st0.size)
        assertEquals(2, st0.getValue("pushup").target)

        val mon = LocalDate.of(2026, 11, 9)
        val s1 = doWorkout(mon)
        assertEquals(s1.total, s1.completed)
        assertTrue(s1.newMilestones.any { it.id == "first" })
        assertTrue(s1.records.isEmpty())
        assertEquals(2, repo.statesNow().getValue("pushup").target) // én «passe» -> ikke økning ennå

        val s2 = doWorkout(mon.plusDays(4)) // fredag: helkropp med overkroppsfokus
        // pushup er med både mandag og fredag -> økt til 3 etter to mestringer
        val push = repo.statesNow().getValue("pushup")
        assertEquals(3, push.target)
        assertTrue(s2.decisions.any { it.kind == ChangeKind.UP })

        // Ny uke uten trening endrer ingenting
        Dates.clock = { mon.plusDays(20) }
        assertEquals(push, repo.statesNow().getValue("pushup"))

        // Smerte registreres og øvelsen tas ut
        doWorkout(mon.plusDays(21), pain = setOf("pushup"))
        val pained = repo.statesNow().getValue("pushup")
        assertTrue(pained.painUntilDay > Dates.today())
        assertEquals(3, pained.target)
        assertEquals(3, repo.dao.sessionsNow().size)
        assertTrue(repo.dao.milestonesNow().any { it.id == "first" })
    }

    @Test fun `personlig rekord oppdages`() = runBlocking {
        repo.completeOnboarding(UserProfile(schedule = ScheduleType.TRE), SelfAssessment(pushups = Ability.LITT))
        val d = LocalDate.of(2026, 11, 9)
        doWorkout(d, rating = Rating.LETT)
        val s = doWorkout(d.plusDays(4), rating = Rating.LETT)
        assertTrue(s.records.isNotEmpty())
    }

    @Test fun `ikke nådd mål senker ikke med en gang - hold, så juster`() = runBlocking {
        repo.completeOnboarding(UserProfile(schedule = ScheduleType.TRE), SelfAssessment(pushups = Ability.MYE))
        val start = repo.statesNow().getValue("squat")
        val d = LocalDate.of(2026, 11, 9)
        doWorkout(d, hitTarget = false, rating = Rating.VANSKELIG)
        val after1 = repo.statesNow().getValue("squat")
        // klarte halvparten -> justert ned, aldri under 1
        assertTrue(after1.target < start.target || after1.stage < start.stage)
        assertTrue(after1.target >= 1)
    }

    @Test fun `sletting fjerner alt`() = runBlocking {
        repo.completeOnboarding(UserProfile(), SelfAssessment())
        doWorkout(LocalDate.of(2026, 11, 9))
        repo.resetAll()
        assertNull(repo.profileNow()); assertTrue(repo.dao.sessionsNow().isEmpty()); assertTrue(repo.statesNow().isEmpty())
    }
}
