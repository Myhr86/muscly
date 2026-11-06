package no.muscly.app

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import no.muscly.app.data.Dates
import no.muscly.app.domain.*
import no.muscly.app.notify.ReminderWorker
import no.muscly.app.notify.Reminders
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderWorkTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before fun setup() { WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().build()) }

    private fun works(name: String) = WorkManager.getInstance(ctx).getWorkInfosForUniqueWork(name).get()

    @Test fun `påminnelser planlegges og kan slås av`() {
        val p = UserProfile(onboarded = true, remindersOn = true)
        Reminders.schedule(ctx, p, LocalDateTime.of(2026, 11, 6, 12, 0))
        listOf("DAILY", "FOLLOW_UP", "WEEKLY").forEach { assertEquals(WorkInfo.State.ENQUEUED, works("reminder_$it").single().state) }
        Reminders.schedule(ctx, p.copy(followUpOn = false), LocalDateTime.of(2026, 11, 6, 12, 0))
        assertTrue(works("reminder_FOLLOW_UP").all { it.state == WorkInfo.State.CANCELLED })
        Reminders.schedule(ctx, p.copy(remindersOn = false))
        listOf("DAILY", "WEEKLY").forEach { n -> assertTrue(works("reminder_$n").all { it.state == WorkInfo.State.CANCELLED }) }
    }

    @Test fun `varsel sendes på treningsdag uten økt, ikke når økten er gjort`() = runBlocking {
        val app = ctx as MusclyApp
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val repo = app.container.repository
        repo.resetAll()
        Dates.clock = { LocalDate.of(2026, 11, 9) } // mandag
        repo.completeOnboarding(UserProfile(schedule = ScheduleType.TRE), SelfAssessment())
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val worker = TestListenableWorkerBuilder<ReminderWorker>(ctx).setInputData(workDataOf("kind" to "DAILY")).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        val n = shadowOf(nm).allNotifications
        assertEquals(1, n.size)
        val text = shadowOf(n[0]).contentText.toString()
        assertTrue(text, text.contains("på planen"))
        // Etter fullført økt: ingen ny påminnelse
        nm.cancelAll()
        val w = WorkoutBuilder.build(DayType.HELKROPP, repo.profileNow()!!, repo.statesNow(), 0, 0, Dates.today())
        repo.saveSession(w, emptyList(), 0, 1000)
        TestListenableWorkerBuilder<ReminderWorker>(ctx).setInputData(workDataOf("kind" to "FOLLOW_UP")).build().doWork()
        assertEquals(0, shadowOf(nm).allNotifications.size)
        // Ukeoppsummering er positiv
        TestListenableWorkerBuilder<ReminderWorker>(ctx).setInputData(workDataOf("kind" to "WEEKLY")).build().doWork()
        assertTrue(shadowOf(shadowOf(nm).allNotifications.single()).contentTitle.toString().contains("Bra jobbet"))
        Dates.clock = { LocalDate.now() }
    }

    @Test fun `tekstene er alltid positive`() {
        val bad = listOf("skuffet", "dårlig", "lat", "glemte", "skam")
        val all = DayType.values().map { Reminders.Texts.daily(it, 1) } + (0..3).map { Reminders.Texts.followUp(it) } + listOf(Reminders.Texts.weekly(0), Reminders.Texts.weekly(3))
        all.forEach { (t, b) -> bad.forEach { w -> assertFalse("$t $b", (t + b).lowercase().contains(w)) } }
    }
}
