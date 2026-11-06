package no.muscly.app

import no.muscly.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProgramTest {
    private fun all(): List<UserProfile> = buildList {
        for (s in ScheduleType.values()) for (f in Focus.values()) for (l in Level.values()) for (mh in 1..4) {
            add(UserProfile(schedule = s, focus = f, level = l, maxHardPerWeek = mh))
            if (s == ScheduleType.EGEN) {
                add(UserProfile(schedule = s, focus = f, level = l, customDays = setOf(0, 1, 2), maxHardPerWeek = mh))
                add(UserProfile(schedule = s, focus = f, level = l, customDays = setOf(5, 6, 0), maxHardPerWeek = mh))
                add(UserProfile(schedule = s, focus = f, level = l, customDays = setOf(0, 1, 2, 3, 4, 5), maxHardPerWeek = mh))
            }
        }
    }

    @Test fun `samme muskler belastes aldri hardt to dager på rad - også over ukeskiftet`() {
        for (p in all()) {
            val plan = ProgramGenerator.weekPlan(p)
            for (i in 0 until 7) {
                val a = plan[i]; val b = plan[(i + 1) % 7]
                assertFalse("$p: ${plan}", ProgramGenerator.overlaps(a, b))
            }
        }
    }

    @Test fun `antall harde økter holder seg innen grensen`() {
        for (p in all()) {
            val hard = ProgramGenerator.weekPlan(p).count { it.intensity == Intensity.HARD }
            assertTrue("$p -> $hard", hard <= ProgramGenerator.maxHard(p))
            if (p.level == Level.NYBEGYNNER) assertTrue(hard <= 3)
        }
    }

    @Test fun `trening hver dag har lette dager`() {
        for (f in Focus.values()) {
            val plan = ProgramGenerator.weekPlan(UserProfile(schedule = ScheduleType.HVER_DAG, focus = f))
            assertTrue(plan.toString(), plan.count { it.intensity == Intensity.LETT } >= 2)
            assertTrue(plan.none { it == DayType.HVILE })
        }
    }

    @Test fun `treningsdager følger valgt oppsett`() {
        val p3 = ProgramGenerator.weekPlan(UserProfile(schedule = ScheduleType.TRE))
        assertEquals(listOf(0, 2, 4), p3.indices.filter { p3[it] != DayType.HVILE })
        assertTrue(p3.filter { it != DayType.HVILE }.all { it.intensity == Intensity.HARD })
        val p4 = ProgramGenerator.weekPlan(UserProfile(schedule = ScheduleType.FIRE))
        assertEquals(4, p4.count { it != DayType.HVILE })
        val c = ProgramGenerator.weekPlan(UserProfile(schedule = ScheduleType.EGEN, customDays = setOf(1, 5)))
        assertEquals(listOf(1, 5), c.indices.filter { c[it] != DayType.HVILE })
        println("Hver dag (auto): " + ProgramGenerator.weekPlan(UserProfile(schedule = ScheduleType.HVER_DAG)).map { it.title })
        println("4 dager (armer): " + ProgramGenerator.weekPlan(UserProfile(schedule = ScheduleType.FIRE, focus = Focus.ARMER)).map { it.title })
    }

    @Test fun `økter holder seg innen valgt tid og passer nybegynnere`() {
        val states = emptyMap<String, ExerciseState>()
        for (m in listOf(15, 20, 30, 45)) for (t in DayType.values().filter { it != DayType.HVILE }) {
            val p = UserProfile(minutes = m)
            val w = WorkoutBuilder.build(t, p, states, 0, 0, 0)
            assertTrue("$t $m: ${w.estimatedMinutes}", w.estimatedMinutes <= m + 8)
            assertTrue("$t $m tom", w.exercises.isNotEmpty())
            w.exercises.forEach { pe ->
                val ex = ExerciseLibrary.get(pe.exerciseId)
                if (ex.category != Category.KONDISJON && ex.category != Category.BEVEGELIGHET) assertEquals("nybegynner skal ha 2 sett", 2, pe.sets)
            }
            assertEquals(w.exercises.size, w.exercises.map { it.exerciseId }.distinct().size)
        }
    }

    @Test fun `utstyr og skader respekteres`() {
        val p = UserProfile(equipment = setOf(Equipment.KROPPSVEKT, Equipment.TRAPP), injuries = setOf(Injury.KNE))
        for (t in DayType.values()) {
            val w = WorkoutBuilder.build(t, p, emptyMap(), 0, 0, 0)
            w.exercises.forEach { pe ->
                val ex = ExerciseLibrary.get(pe.exerciseId)
                assertFalse("${ex.id} krever utstyr", Equipment.STANG in ex.equipment || Equipment.MANUALER in ex.equipment || Equipment.SYKKEL in ex.equipment)
                assertFalse("${ex.id} belaster kne", Injury.KNE in ex.avoidWith)
            }
        }
        val avoid = UserProfile(avoided = setOf("pushup"))
        assertTrue(WorkoutBuilder.build(DayType.OVERKROPP, avoid, emptyMap(), 0, 0, 0).exercises.none { it.exerciseId == "pushup" })
    }

    @Test fun `favoritter og prioriteringer tas med`() {
        val p = UserProfile(priorities = setOf(Category.ARMER), minutes = 45)
        assertTrue(WorkoutBuilder.build(DayType.HELKROPP, p, emptyMap(), 0, 0, 0).exercises.any { it.exerciseId == "curl" })
        val liked = UserProfile(liked = setOf("stepup"))
        assertTrue(WorkoutBuilder.build(DayType.HELKROPP, liked, emptyMap(), 0, 0, 0).exercises.any { it.exerciseId == "stepup" })
    }

    @Test fun `forslag for i dag tar hensyn til gårsdagen`() {
        assertEquals(DayType.BEIN_KJERNE, WorkoutBuilder.suggestForToday(listOf(9L to DayType.OVERKROPP), 10))
        assertEquals(DayType.OVERKROPP, WorkoutBuilder.suggestForToday(listOf(9L to DayType.BEIN_KJERNE), 10))
        assertEquals(DayType.KJERNE_KONDIS, WorkoutBuilder.suggestForToday(listOf(9L to DayType.HELKROPP), 10))
        assertEquals(DayType.LETT, WorkoutBuilder.suggestForToday(listOf(9L to DayType.HELKROPP, 8L to DayType.OVERKROPP), 10))
        assertEquals(DayType.HELKROPP, WorkoutBuilder.suggestForToday(emptyList(), 10))
    }
}
