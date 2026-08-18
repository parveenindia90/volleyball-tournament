package com.volleyball.tournament.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetPoints: Int = 15,
    val currentRound: Int = 1,
    val totalRounds: Int = 1,
    val isCompleted: Boolean = false,
    val winnerTeamName: String? = null
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val tournamentId: String,
    val roundIndex: Int,
    val matchNumber: Int,
    val teamAName: String,
    val teamBName: String?,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val winnerName: String? = null,
    val isCompleted: Boolean = false,
    val isBye: Boolean = false
)

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments WHERE isCompleted = 0 LIMIT 1")
    fun getActiveTournament(): Flow<TournamentEntity?>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundIndex ASC, matchNumber ASC")
    fun getMatchesForTournament(tournamentId: String): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM tournaments")
    suspend fun clearAllTournaments()

    @Query("DELETE FROM matches")
    suspend fun clearAllMatches()
}

@Database(entities = [TournamentEntity::class, MatchEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "volleyball_tournament_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
