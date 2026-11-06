package no.muscly.app

import kotlinx.coroutines.runBlocking
import no.muscly.app.coach.*
import no.muscly.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoalAndAgeTest {
    private val adult = UserProfile(age = 40, onboarded = true)
    private val teen = UserProfile(age = 14, onboarded = true)
    private fun ctx(p: UserProfile) = CoachContext(p, emptyMap(), emptyList(), emptyList(), ProgramGenerator.weekPlan(p), 20000)

    @Test fun `dårlig rygg og større biceps blir et program`() {
        val q = "Jeg vil trene opp en dårlig rygg og få større biceps, 3 dager i uka, 30 min"
        assertTrue(GoalPlanner.isPlanRequest(q))
        val prop = GoalPlanner.local(q, adult)
        assertEquals(Focus.ARMER, prop.focus)
        assertEquals(setOf(Category.ARMER), prop.priorities)
        assertEquals(setOf(Goal.RYGGHELSE), prop.goals)
        assertTrue(prop.addInjuries.isEmpty())
        assertEquals(ScheduleType.TRE, prop.schedule); assertEquals(30, prop.minutes)
        val np = prop.apply(adult)
        // Ryggvennlige øvelser kommer i styrkeøktene, og curl er med på overkroppsdager
        for (d in ProgramGenerator.weekPlan(np).filter { it.intensity == Intensity.HARD }) {
            val ids = WorkoutBuilder.build(d, np, emptyMap(), 10, 0, 0).exercises.map { it.exerciseId }
            assertTrue("$d $ids", ids.any { it == "birddog" || it == "deadbug" })
            if (BodyArea.OVER in d.loads) assertTrue("$d $ids", "curl" in ids)
        }
        // Svaret fra treneren har knappen
        val r = OfflineCoach.answer(q, ctx(adult))
        assertTrue(r.action.startsWith("plan:"))
        assertEquals(prop, PlanProposal.fromJson(r.action.removePrefix("plan:")))
    }

    @Test fun `vond rygg gir skånsomt program, også for ungdom`() = runBlocking {
        val q = "Jeg har vondt i ryggen og vil trene den opp, kan du sette opp et program?"
        val prop = GoalPlanner.local(q, teen)
        assertTrue(Injury.RYGG in prop.addInjuries)
        val np = prop.apply(teen)
        val all = DayType.values().flatMap { WorkoutBuilder.build(it, np, emptyMap(), 0, 0, 0).exercises.map { e -> e.exerciseId } }
        assertTrue(all.none { it in listOf("row", "press", "superman") })
        // Ikke stoppet av sikkerhetsfilteret, men inneholder råd
        val r = CoachService("").reply(q, ctx(teen), emptyList())
        assertTrue(r.action.startsWith("plan:")); assertTrue(r.text.contains("fysioterapeut"))
        // Vanlig smerte uten programønske får fortsatt stoppskiltet
        assertEquals("sikkerhet", CoachService("").reply("Det gjør vondt i ryggen", ctx(teen), emptyList()).source)
    }

    @Test fun `vektnedgang bare for voksne`() {
        val q = "Jeg vil gå ned i vekt og bli sterkere, lag et program for 4 dager"
        val a = GoalPlanner.local(q, adult)
        assertTrue(Goal.VEKTNEDGANG in a.goals); assertEquals(Focus.KOMBINASJON, a.focus); assertEquals(ScheduleType.FIRE, a.schedule)
        val t = GoalPlanner.local(q, teen)
        assertFalse(Goal.VEKTNEDGANG in t.goals)
        // Selv om et forslag inneholder vektnedgang, filtreres det bort for ungdom
        assertFalse(Goal.VEKTNEDGANG in a.apply(teen).goals)
    }

    @Test fun `voksne får vanlige råd om kosthold og tilskudd, ungdom får ungdomsregler`() = runBlocking {
        assertNull(Safety.classify("Bør jeg ta kreatin?", adult = true))
        assertEquals(Safety.Topic.SUPPLEMENT, Safety.classify("Bør jeg ta kreatin?", adult = false))
        assertEquals(Safety.Topic.SUPPLEMENT, Safety.classify("Hva med steroider?", adult = true))
        assertEquals(Safety.Topic.EXTREME, Safety.classify("Skal jeg trene gjennom smerten?", adult = true))
        assertEquals(Safety.Topic.CRISIS, Safety.classify("Jeg vil ta livet mitt", adult = true))
        val r = CoachService("").reply("Bør jeg ta kreatin?", ctx(adult), emptyList())
        assertTrue(r.text.contains("Kreatin")); assertEquals("lokal", r.source)
        assertTrue(CoachService("").reply("Hvordan går jeg ned i vekt?", ctx(adult), emptyList()).text.contains("0,5 kg"))
        assertTrue(Safety.outputOk("Kreatin er fint", adult = true)); assertFalse(Safety.outputOk("Kreatin er fint", adult = false))
        assertFalse(Safety.outputOk("Prøv steroider", adult = true))
        assertTrue(Safety.answer(Safety.Topic.PAIN, adult = true).contains("fysioterapeut"))
    }

    @Test fun `vanlige økter og spørsmål virker fortsatt`() {
        assertFalse(GoalPlanner.isPlanRequest("Kan du lage en kort treningsøkt for i dag?"))
        assertTrue(OfflineCoach.answer("Kan du lage en kort treningsøkt for i dag?", ctx(adult)).action.startsWith("workout:"))
        assertTrue(OfflineCoach.answer("Hvordan får jeg større biceps?", ctx(teen)).text.contains("Chin-ups"))
    }
}
