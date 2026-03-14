package com.rancon.freelivetv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE isVisible = 1 ORDER BY priority DESC, name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels ORDER BY priority DESC, name ASC")
    suspend fun getAllChannelsSync(): List<Channel>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 AND isVisible = 1")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE healthStatus = 'ACTIVE' AND isVisible = 1 ORDER BY priority DESC")
    fun getActiveChannels(): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Update
    suspend fun update(channel: Channel)

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: String): Channel?
    
    @Query("DELETE FROM channels WHERE isFavorite = 0 AND healthStatus = 'INACTIVE' AND lastFailureTime < :cutoff")
    suspend fun cleanupOldChannels(cutoff: Long)
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE channelId = :channelId ORDER BY startTime ASC")
    fun getProgramsForChannel(channelId: String): Flow<List<Program>>

    @Query("SELECT * FROM programs WHERE channelId = :channelId AND :time BETWEEN startTime AND endTime LIMIT 1")
    suspend fun getCurrentProgram(channelId: String, time: Long): Program?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<Program>)

    @Query("DELETE FROM programs WHERE endTime < :cutoff")
    suspend fun cleanupOldPrograms(cutoff: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY startTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE programId = :programId")
    suspend fun deleteByProgramId(programId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM reminders WHERE programId = :programId)")
    suspend fun hasReminder(programId: Long): Boolean

    @Query("UPDATE reminders SET isNotified = 1 WHERE programId = :programId")
    suspend fun markAsNotified(programId: Long)
}

@Dao
interface InteractionDao {
    @Insert
    suspend fun insert(interaction: Interaction)

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractions(limit: Int): Flow<List<Interaction>>

    @Query("SELECT category, SUM(duration) as totalDuration FROM interactions WHERE actionType = 'WATCH' AND duration > 5000 GROUP BY category ORDER BY totalDuration DESC")
    fun getCategoryAffinities(): Flow<List<CategoryAffinity>>

    @Query("DELETE FROM interactions WHERE timestamp < :cutoff")
    suspend fun cleanupOldInteractions(cutoff: Long)
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY year DESC")
    fun getAllMovies(): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1")
    fun getFavoriteMovies(): Flow<List<Movie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<Movie>)

    @Query("SELECT * FROM movies WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC")
    fun getContinueWatching(): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: String): Movie?

    @Query("UPDATE movies SET lastPlayedPosition = :position, lastPlayedTime = :time WHERE id = :id")
    suspend fun updateProgress(id: String, position: Long, time: Long)
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series")
    fun getAllSeries(): Flow<List<Series>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<Series>)

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber, episodeNumber")
    fun getEpisodesForSeries(seriesId: String): Flow<List<Episode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<Episode>)

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getSeriesById(id: String): Series?

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: String): Episode?

    @Query("SELECT * FROM episodes WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC")
    fun getContinueWatchingEpisodes(): Flow<List<Episode>>

    @Query("UPDATE episodes SET lastPlayedPosition = :position, lastPlayedTime = :time WHERE id = :id")
    suspend fun updateEpisodeProgress(id: String, position: Long, time: Long)
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<SourceConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<SourceConfig>)
}

data class CategoryAffinity(
    val category: String,
    val totalDuration: Long
)

@Database(entities = [Channel::class, Program::class, Movie::class, Series::class, Episode::class, SourceConfig::class, Interaction::class, Reminder::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao
    abstract fun reminderDao(): ReminderDao
    abstract fun interactionDao(): InteractionDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN isVisible INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freelivetv_database"
                )
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
