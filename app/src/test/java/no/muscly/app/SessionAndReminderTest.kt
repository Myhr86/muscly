package no.muscly.app

import no.muscly.app.domain.*
import no.muscly.app.notify.ReminderTimes
import no.muscly.app.ui.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class SessionAndReminderTest {
    @Test fun `neste påminnelse lander på en treningsdag i fremtiden`() {
        val fri = LocalDateTime.of(2026, 11, 6, 18, 0) // fredag
        assertEquals(LocalDateTime.of(2026, 11, 9, 17, 0), ReminderTimes.next(fri, 17, 0, setOf(0, 2, 4)))
        val friMorning = LocalDateTime.of(2026, 11, 6, 8, 0)
        assertEquals(LocalDateTime.of(2026, 11, 6, 17, 0), ReminderTimes.next(friMorning, 17, 0, setOf(4)))
        assertNull(ReminderTimes.next(fri, 17, 0, emptySet()))
        assertEquals(20 to 0, ReminderTimes.followUpTime(17, 0))
        assertEquals(20 to 30, ReminderTimes.followUpTime(19, 0)) // aldri sent på kvelden
        assertEquals(setOf(0, 2, 4), ReminderTimes.trainingDays(UserProfile(schedule = ScheduleType.TRE)))
    }

    @Test fun `treningsmodus - sett, pause, timer og resultat`() {
        var now = 0L
        val w = WorkoutBuilder.build(DayType.HELKROPP, UserProfile(), emptyMap(), 0, 0, 0)
        val s = WorkoutSession(w) { now }
        assertEquals(Phase.WARMUP, s.phase)
        s.startExercises()
        val first = s.current!!
        s.setValue(0, first.planned.target - 1)
        s.completeSet(0)
        assertEquals(TimerKind.REST, s.timer?.kind)
        now += first.planned.restSec * 1000L + 10
        s.tick(now)
        assertNull(s.timer)
        s.completeSet(1)
        assertNull("ingen pause etter siste sett", s.timer)
        s.rate(Rating.PASSE)
        s.next()
        // Finn planken og test tidtaker
        while (s.current?.stage?.measure != Measure.SECONDS && s.index < s.items.lastIndex) s.next()
        val plank = s.current!!
        assertEquals(Measure.SECONDS, plank.stage.measure)
        s.startTimer(0)
        assertEquals(TimerKind.WORK, s.timer?.kind)
        now += plank.values[0] * 1000L + 1
        s.tick(now)
        assertTrue(plank.done[0])
        // Stopp tidlig på sett 2 (etter pausen)
        s.skipRest(); s.startTimer(1); now += 7000; s.stopTimerEarly(now)
        assertEquals(7, plank.values[1]); assertTrue(plank.done[1])
        val r = s.results()
        assertEquals(2, r.size)
        assertEquals(listOf(first.planned.target - 1, first.planned.target), r[0].setValues)
        assertEquals(Rating.PASSE, r[1].rating) // standard når ikke valgt
    }

    @Test fun `intervalltimer teller runder`() {
        var now = 0L
        val w = WorkoutBuilder.build(DayType.KONDISJON, UserProfile(), emptyMap(), 0, 0, 0)
        val s = WorkoutSession(w) { now }
        s.startExercises()
        val bike = s.items.first { it.ex.id == "bike_interval" }
        s.index = s.items.indexOf(bike)
        s.startTimer(0)
        val t = s.timer!!
        assertEquals(bike.values[0], t.phases.count { it.work })
        // Stopp etter oppvarming + 2 runder
        now = (120 + 2 * (bike.stage.workSec + bike.stage.restSec)) * 1000L + 500
        s.stopTimerEarly(now)
        assertEquals(2, bike.values[0])
    }

    @Test fun `smerte i treningsmodus registreres`() {
        val w = WorkoutBuilder.build(DayType.HELKROPP, UserProfile(), emptyMap(), 0, 0, 0)
        val s = WorkoutSession(w)
        s.startExercises()
        val id = s.current!!.ex.id
        s.reportPain()
        val r = s.results()
        assertTrue(r.single { it.exerciseId == id }.pain)
        assertEquals(1, s.index)
    }

    @Test fun `animasjonstidslinje gir gyldige poser`() {
        Illustrations.all.forEach { (k, ill) ->
            val total = no.muscly.app.ui.components.AnimTimeline.total(ill)
            for (t in 0..total step 50) {
                val (pose, cap) = no.muscly.app.ui.components.AnimTimeline.poseAt(ill, t)
                val sk = Kinematics.skeleton(pose)
                listOf(sk.head, sk.handF, sk.ankleF, sk.toeB).forEach { p -> assertTrue("$k $p", p.x.isFinite() && p.y.isFinite()) }
                assertTrue(cap in 0..ill.caption.lastIndex.coerceAtLeast(0))
            }
        }
    }
}
