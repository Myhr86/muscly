package no.muscly.app

import no.muscly.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProgressionTest {
    private val profile = UserProfile(onboarded = true)
    private val push = ExerciseLibrary.get("pushup")

    private fun res(st: ExerciseState, vararg v: Int, r: Rating = Rating.PASSE, sets: Int = 2, pain: Boolean = false) =
        ExerciseResult(st.exerciseId, st.stage, st.target, sets, v.toList(), st.weightKg, r, pain)

    @Test fun `bruker som klarer 2 push-ups starter på 2`() {
        val s = ProgressionEngine.initialState(push, profile, SelfAssessment(pushups = Ability.LITT), 100)
        assertEquals(3, s.stage); assertEquals(2, s.target)
        assertEquals("Push-ups", push.stages[s.stage].name)
    }

    @Test fun `ukesprogresjon 2-3-4-5 når målet mestres`() {
        var s = ProgressionEngine.initialState(push, profile, SelfAssessment(pushups = Ability.LITT), 100)
        val targets = mutableListOf(s.target)
        var day = 100L
        repeat(4) { week ->
            // to økter per uke der målet nås og det kjennes «passe»
            repeat(2) {
                day++
                s = ProgressionEngine.evaluate(push, s, res(s, s.target, s.target), profile, day).newState
            }
            targets += s.target
            day += 5
        }
        assertEquals(listOf(2, 3, 4, 5, 6), targets)
    }

    @Test fun `øker aldri bare fordi tiden går`() {
        val s = ProgressionEngine.initialState(push, profile, SelfAssessment(pushups = Ability.LITT), 100)
        // Bygg økt flere uker senere uten ny evaluering – målet er det samme
        val states = mapOf("pushup" to s)
        for (week in 0L..10L) {
            val pe = WorkoutBuilder.plan("pushup", profile, states, 0, DayType.HELKROPP, 100 + week * 7)
            assertEquals(2, pe.target); assertEquals(3, pe.stageIndex)
        }
    }

    @Test fun `lett gir økning med en gang, vanskelig holder`() {
        val s = ExerciseState("pushup", 3, 5)
        val up = ProgressionEngine.evaluate(push, s, res(s, 5, 5, r = Rating.LETT), profile, 1)
        assertEquals(ChangeKind.UP, up.kind); assertEquals(6, up.newState.target)
        val hold = ProgressionEngine.evaluate(push, s, res(s, 5, 5, r = Rating.VANSKELIG), profile, 1)
        assertEquals(ChangeKind.HOLD, hold.kind); assertEquals(5, hold.newState.target)
    }

    @Test fun `bommer litt - holder først, justerer ned etter to ganger`() {
        var s = ExerciseState("pushup", 3, 6)
        val d1 = ProgressionEngine.evaluate(push, s, res(s, 6, 5), profile, 1)
        assertEquals(ChangeKind.HOLD, d1.kind); assertEquals(6, d1.newState.target)
        s = d1.newState
        val d2 = ProgressionEngine.evaluate(push, s, res(s, 5, 5), profile, 3)
        assertEquals(ChangeKind.DOWN, d2.kind); assertEquals(5, d2.newState.target)
    }

    @Test fun `bommer mye - går ned til det brukeren faktisk klarte, aldri straff under 1`() {
        val s = ExerciseState("pushup", 3, 8)
        val d = ProgressionEngine.evaluate(push, s, res(s, 3, 2), profile, 1)
        assertEquals(ChangeKind.DOWN, d.kind); assertEquals(3, d.newState.target)
        val z = ProgressionEngine.evaluate(push, ExerciseState("pushup", 3, 2), ExerciseResult("pushup", 3, 2, 2, listOf(0, 0), 0f, Rating.VANSKELIG), profile, 1)
        assertTrue(z.newState.target >= 1)
    }

    @Test fun `går ned et trinn på stigen når det er for tungt på laveste mål`() {
        val pull = ExerciseLibrary.get("pullup")
        val s = ExerciseState("pullup", 3, 1)
        val d = ProgressionEngine.evaluate(pull, s, ExerciseResult("pullup", 3, 1, 2, listOf(0, 0), 0f, Rating.VANSKELIG), profile, 1)
        // klarte 0 – går til negative pull-ups
        assertTrue(d.kind == ChangeKind.STAGE_DOWN || d.kind == ChangeKind.DOWN)
        assertEquals(2, d.newState.stage)
    }

    @Test fun `når toppen av trinnet nås går den til neste trinn`() {
        val pull = ExerciseLibrary.get("pullup")
        val s = ExerciseState("pullup", 0, 30) // heng 30 sek = maks
        val d = ProgressionEngine.evaluate(pull, s, ExerciseResult("pullup", 0, 30, 2, listOf(30, 30), 0f, Rating.LETT), profile, 1)
        assertEquals(ChangeKind.STAGE_UP, d.kind); assertEquals(1, d.newState.stage); assertEquals(3, d.newState.target)
    }

    @Test fun `curl øker reps først, deretter vekt forsiktig`() {
        val curl = ExerciseLibrary.get("curl")
        val s = ExerciseState("curl", 0, 15, weightKg = 3f)
        val d = ProgressionEngine.evaluate(curl, s, ExerciseResult("curl", 0, 15, 2, listOf(15, 15), 3f, Rating.LETT), profile, 1)
        assertEquals(ChangeKind.WEIGHT_UP, d.kind); assertEquals(4f, d.newState.weightKg); assertEquals(8, d.newState.target)
    }

    @Test fun `knebøy går til manual først når manualer finnes`() {
        val sq = ExerciseLibrary.get("squat")
        val s = ExerciseState("squat", 1, 20)
        val noDb = profile.copy(equipment = setOf(Equipment.KROPPSVEKT))
        val d = ProgressionEngine.evaluate(sq, s, ExerciseResult("squat", 1, 20, 2, listOf(20, 20), 0f, Rating.LETT), noDb, 1)
        assertEquals(ChangeKind.MAXED, d.kind); assertEquals(1, d.newState.stage)
        val d2 = ProgressionEngine.evaluate(sq, s, ExerciseResult("squat", 1, 20, 2, listOf(20, 20), 0f, Rating.LETT), profile, 1)
        assertEquals(ChangeKind.STAGE_UP, d2.kind); assertEquals(2, d2.newState.stage)
    }

    @Test fun `smerte gir pause og ingen økning`() {
        val s = ExerciseState("pushup", 3, 5)
        val d = ProgressionEngine.evaluate(push, s, res(s, 5, 5, r = Rating.LETT, pain = true), profile, 10)
        assertEquals(ChangeKind.PAIN, d.kind); assertEquals(5, d.newState.target); assertEquals(17, d.newState.painUntilDay)
        val w = WorkoutBuilder.build(DayType.HELKROPP, profile, mapOf("pushup" to d.newState), 3, 0, 12)
        assertTrue(w.exercises.none { it.exerciseId == "pushup" })
        val later = WorkoutBuilder.build(DayType.HELKROPP, profile, mapOf("pushup" to d.newState), 3, 0, 18)
        assertTrue(later.exercises.any { it.exerciseId == "pushup" })
    }

    @Test fun `samme øvelse to ganger samme dag gir bare én økning`() {
        val s = ExerciseState("pushup", 3, 5)
        val d1 = ProgressionEngine.evaluate(push, s, res(s, 5, 5, r = Rating.LETT), profile, 5)
        val d2 = ProgressionEngine.evaluate(push, d1.newState, res(d1.newState, 6, 6, r = Rating.LETT), profile, 5)
        assertEquals(6, d2.newState.target); assertEquals(ChangeKind.HOLD, d2.kind)
    }

    @Test fun `realistisk tempo over 8 uker - aldri mer enn én repetisjon per økt`() {
        var s = ProgressionEngine.initialState(push, profile, SelfAssessment(pushups = Ability.LITT), 0)
        var prev = s.target
        for (session in 1..24) {
            s = ProgressionEngine.evaluate(push, s, res(s, s.target, s.target, r = Rating.LETT), profile, session * 2L).newState
            if (s.stage == 3) assertTrue("hopp for stort", s.target - prev <= 1)
            prev = s.target
        }
        assertTrue(s.target <= 20)
    }

    @Test fun `alle øvelser har gyldige trinn og illustrasjoner`() {
        ExerciseLibrary.all.forEach { ex ->
            assertTrue(ex.id, ex.stages.isNotEmpty())
            ex.stages.forEach { st ->
                assertTrue("${ex.id}: ${st.poseKey}", Illustrations.all.containsKey(st.poseKey))
                assertTrue(ex.id, st.start in 1..st.max)
                assertTrue(ex.id, Illustrations.get(st.poseKey).frames.size >= 2)
            }
            assertTrue(ex.steps.isNotEmpty() && ex.mistakes.isNotEmpty() && ex.tips.isNotEmpty())
            ex.alternatives.forEach { assertNotNull("${ex.id} alt $it", ExerciseLibrary.find(it)) }
        }
    }

    @Test fun `ingen sit-ups i biblioteket`() {
        assertTrue(ExerciseLibrary.all.none { it.name.lowercase().contains("sit-up") || it.name.lowercase().contains("situp") })
    }
}
