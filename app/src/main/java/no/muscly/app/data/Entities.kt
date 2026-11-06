package no.muscly.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profile")
data class ProfileEntity(@PrimaryKey val id: Int = 1, val json: String)

@Entity(tableName = "exercise_state")
data class ExerciseStateEntity(
    @PrimaryKey val exerciseId: String,
    val stage: Int, val target: Int, val weightKg: Float,
    val successStreak: Int, val struggleStreak: Int,
    val lastUpdatedDay: Long, val painUntilDay: Long, val lastEvaluatedDay: Long,
)

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long, val dayType: String, val title: String,
    val startedAt: Long, val finishedAt: Long,
    val completedExercises: Int, val totalExercises: Int, val durationSec: Int,
)

@Entity(tableName = "exercise_log", indices = [Index("sessionId"), Index("exerciseId")])
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long, val day: Long, val exerciseId: String,
    val stage: Int, val target: Int, val setsPlanned: Int,
    /** Kommaseparerte verdier per sett, f.eks. "5,5,4". */
    val sets: String, val weightKg: Float, val rating: String, val pain: Boolean,
    val decisionKind: String, val decisionText: String,
) {
    fun values(): List<Int> = if (sets.isBlank()) emptyList() else sets.split(",").mapNotNull { it.trim().toIntOrNull() }
}

@Entity(tableName = "milestone")
data class MilestoneEntity(@PrimaryKey val id: String, val day: Long)

@Entity(tableName = "chat")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUser: Boolean, val text: String, val time: Long,
    /** Valgfri handling, f.eks. "workout:HELKROPP:15" eller "exercise:pushup". */
    val action: String = "", val source: String = "lokal",
)

@Dao
interface MusclyDao {
    @Query("SELECT * FROM profile WHERE id = 1") fun profile(): Flow<ProfileEntity?>
    @Query("SELECT * FROM profile WHERE id = 1") suspend fun profileNow(): ProfileEntity?
    @Upsert suspend fun saveProfile(p: ProfileEntity)

    @Query("SELECT * FROM exercise_state") fun states(): Flow<List<ExerciseStateEntity>>
    @Query("SELECT * FROM exercise_state") suspend fun statesNow(): List<ExerciseStateEntity>
    @Upsert suspend fun saveStates(s: List<ExerciseStateEntity>)

    @Insert suspend fun insertSession(s: SessionEntity): Long
    @Insert suspend fun insertLogs(l: List<ExerciseLogEntity>)
    @Query("SELECT * FROM session ORDER BY finishedAt DESC") fun sessions(): Flow<List<SessionEntity>>
    @Query("SELECT * FROM session ORDER BY finishedAt DESC") suspend fun sessionsNow(): List<SessionEntity>
    @Query("SELECT * FROM session WHERE id = :id") suspend fun session(id: Long): SessionEntity?
    @Query("SELECT * FROM exercise_log ORDER BY day ASC, id ASC") fun logs(): Flow<List<ExerciseLogEntity>>
    @Query("SELECT * FROM exercise_log ORDER BY day ASC, id ASC") suspend fun logsNow(): List<ExerciseLogEntity>
    @Query("SELECT * FROM exercise_log WHERE sessionId = :id") suspend fun logsFor(id: Long): List<ExerciseLogEntity>
    @Query("SELECT COUNT(*) FROM session WHERE day = :day") suspend fun sessionsOnDay(day: Long): Int

    @Query("SELECT * FROM milestone") fun milestones(): Flow<List<MilestoneEntity>>
    @Query("SELECT * FROM milestone") suspend fun milestonesNow(): List<MilestoneEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMilestones(m: List<MilestoneEntity>)

    @Query("SELECT * FROM chat ORDER BY id ASC") fun chat(): Flow<List<ChatEntity>>
    @Query("SELECT * FROM chat ORDER BY id DESC LIMIT :n") suspend fun lastChat(n: Int): List<ChatEntity>
    @Insert suspend fun insertChat(c: ChatEntity): Long
    @Query("DELETE FROM chat") suspend fun clearChat()

    @Query("DELETE FROM session") suspend fun clearSessions()
    @Query("DELETE FROM exercise_log") suspend fun clearLogs()
    @Query("DELETE FROM exercise_state") suspend fun clearStates()
    @Query("DELETE FROM milestone") suspend fun clearMilestones()
    @Query("DELETE FROM profile") suspend fun clearProfile()
}

@Database(
    entities = [ProfileEntity::class, ExerciseStateEntity::class, SessionEntity::class, ExerciseLogEntity::class, MilestoneEntity::class, ChatEntity::class],
    version = 1, exportSchema = true,
)
abstract class MusclyDatabase : RoomDatabase() {
    abstract fun dao(): MusclyDao
}
