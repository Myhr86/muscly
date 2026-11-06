package no.muscly.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.muscly.app.coach.CoachService
import no.muscly.app.data.MusclyDatabase
import no.muscly.app.data.Repository
import no.muscly.app.notify.Reminders

/** Enkel manuell avhengighetsinjeksjon. */
class AppContainer(context: Context, inMemory: Boolean = false) {
    val db: MusclyDatabase = if (inMemory) Room.inMemoryDatabaseBuilder(context, MusclyDatabase::class.java).allowMainThreadQueries().build()
                             else Room.databaseBuilder(context, MusclyDatabase::class.java, "muscly.db").build()
    val repository = Repository(db)
    val coach = CoachService(BuildConfig.COACH_URL)
}

class MusclyApp : Application() {
    lateinit var container: AppContainer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
        scope.launch {
            runCatching { container.repository.profileNow()?.let { Reminders.schedule(this@MusclyApp, it) } }
        }
    }
}
