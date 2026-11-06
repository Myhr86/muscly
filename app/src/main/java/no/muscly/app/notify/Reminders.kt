package no.muscly.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import no.muscly.app.MainActivity
import no.muscly.app.MusclyApp
import no.muscly.app.R
import no.muscly.app.data.Dates
import no.muscly.app.domain.DayType
import no.muscly.app.domain.ProgramGenerator
import no.muscly.app.domain.UserProfile
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

enum class ReminderKind { DAILY, FOLLOW_UP, WEEKLY }

object ReminderTimes {
    /** Neste tidspunkt etter [now] på en av [days] (0 = mandag) kl. hour:minute. */
    fun next(now: LocalDateTime, hour: Int, minute: Int, days: Set<Int>): LocalDateTime? {
        if (days.isEmpty()) return null
        for (i in 0..7) {
            val d = now.toLocalDate().plusDays(i.toLong())
            val t = LocalDateTime.of(d, LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
            if (t.isAfter(now) && (d.dayOfWeek.value - 1) in days) return t
        }
        return null
    }

    fun followUpTime(hour: Int, minute: Int): Pair<Int, Int> {
        val total = (hour * 60 + minute + 180).coerceAtMost(20 * 60 + 30)
        return total / 60 to total % 60
    }

    fun trainingDays(p: UserProfile): Set<Int> =
        ProgramGenerator.weekPlan(p).mapIndexedNotNull { i, d -> if (d != DayType.HVILE) i else null }.toSet()
}

object Reminders {
    const val CHANNEL = "trening"

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Treningspåminnelser", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Vennlige påminnelser om dagens trening" }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Planlegger (eller avbestiller) alle påminnelser ut fra profilen. Kalles ved oppstart og ved endringer. */
    fun schedule(ctx: Context, p: UserProfile, now: LocalDateTime = LocalDateTime.now()) {
        try { scheduleInternal(ctx, p, now) } catch (e: Exception) {
            android.util.Log.w("Muscly", "Kunne ikke planlegge påminnelser", e)
        }
    }

    private fun scheduleInternal(ctx: Context, p: UserProfile, now: LocalDateTime) {
        val wm = WorkManager.getInstance(ctx)
        val days = ReminderTimes.trainingDays(p)
        fun enqueue(kind: ReminderKind, at: LocalDateTime?) {
            val name = "reminder_${kind.name}"
            if (at == null) { wm.cancelUniqueWork(name); return }
            val delay = Duration.between(now, at).toMillis().coerceAtLeast(0)
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("kind" to kind.name))
                .addTag("muscly_reminder").build()
            wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, req)
        }
        if (!p.onboarded || !p.remindersOn) {
            ReminderKind.values().forEach { enqueue(it, null) }; return
        }
        enqueue(ReminderKind.DAILY, ReminderTimes.next(now, p.reminderHour, p.reminderMinute, days))
        val (fh, fm) = ReminderTimes.followUpTime(p.reminderHour, p.reminderMinute)
        enqueue(ReminderKind.FOLLOW_UP, if (p.followUpOn) ReminderTimes.next(now, fh, fm, days) else null)
        enqueue(ReminderKind.WEEKLY, if (p.weeklySummaryOn) ReminderTimes.next(now, 18, 37, setOf(6)) else null)
    }

    fun post(ctx: Context, id: Int, title: String, text: String) {
        if (!hasPermission(ctx)) return
        val intent = PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent).setAutoCancel(true).setColor(0xFFB4F43C.toInt()).build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }

    /** Positive tekster – aldri dårlig samvittighet. */
    object Texts {
        fun daily(day: DayType, seed: Int): Pair<String, String> {
            val titles = listOf("Klar for dagens treningsøkt? 💪", "Tid for trening! 🔥", "Dagens økt venter 🚀")
            val body = when (day.intensity) {
                no.muscly.app.domain.Intensity.LETT -> "I dag er det en lett dag: ${day.title.lowercase()}. Rolig og deilig!"
                else -> "I dag står det ${day.title.lowercase()} på planen!"
            }
            return titles[Math.floorMod(seed, titles.size)] to body
        }
        fun followUp(seed: Int): Pair<String, String> = listOf(
            "Fortsatt tid til en økt 🙂" to "Selv 10 minutter teller. Du klarer det!",
            "Har du tid til en kort økt? ⏱️" to "En liten økt i dag holder vanen i gang. Ingen stress hvis ikke!",
        )[Math.floorMod(seed, 2)]
        fun weekly(sessions: Int): Pair<String, String> = if (sessions > 0)
            "Bra jobbet med treningen denne uken! 🎉" to "Du har gjennomført $sessions ${if (sessions == 1) "økt" else "økter"}. Hvil godt – ny uke i morgen!"
        else "Ny uke, nye muligheter 🌱" to "Planen for neste uke ligger klar. Første økt blir bra!"
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? MusclyApp ?: return Result.success()
        val repo = app.container.repository
        val p = repo.profileNow() ?: return Result.success()
        val kind = runCatching { ReminderKind.valueOf(inputData.getString("kind") ?: "") }.getOrNull() ?: return Result.success()
        val today = Dates.today()
        val doneToday = repo.dao.sessionsOnDay(today) > 0
        val plan = ProgramGenerator.weekPlan(p)
        val todayType = plan[Dates.dow(today)]
        if (p.remindersOn) when (kind) {
            ReminderKind.DAILY -> if (!doneToday && todayType != DayType.HVILE) {
                val (t, b) = Reminders.Texts.daily(todayType, today.toInt()); Reminders.post(applicationContext, 1, t, b)
            }
            ReminderKind.FOLLOW_UP -> if (!doneToday && todayType != DayType.HVILE && p.followUpOn) {
                val (t, b) = Reminders.Texts.followUp(today.toInt()); Reminders.post(applicationContext, 2, t, b)
            }
            ReminderKind.WEEKLY -> if (p.weeklySummaryOn) {
                val start = Dates.weekStart(today)
                val n = repo.dao.sessionsNow().count { it.day >= start }
                val (t, b) = Reminders.Texts.weekly(n); Reminders.post(applicationContext, 3, t, b)
            }
        }
        // Planlegg neste runde (litt frem i tid, så samme påminnelse ikke kommer to ganger)
        Reminders.schedule(applicationContext, p, LocalDateTime.now().plusMinutes(1))
        return Result.success()
    }
}
