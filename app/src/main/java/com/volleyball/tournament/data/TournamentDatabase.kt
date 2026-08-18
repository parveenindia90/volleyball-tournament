package com.volleyball.tournament.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetPoints: Int = 15,
    val currentRound: Int = 1,
    val totalRounds: Int = 1,
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val winnerTeamName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val tournamentId: String,
    val name: String,
    val seedNumber: Int = 0
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
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val isBye: Boolean = false
)

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id LIMIT 1")
    fun getTournamentById(id: String): Flow<TournamentEntity?>

    @Query("SELECT * FROM teams WHERE tournamentId = :tournamentId ORDER BY seedNumber ASC")
    fun getTeamsForTournament(tournamentId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundIndex ASC, matchNumber ASC")
    fun getMatchesForTournament(tournamentId: String): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<TeamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM tournaments WHERE id = :tournamentId")
    suspend fun deleteTournament(tournamentId: String)
}

@Database(
    entities = [TournamentEntity::class, TeamEntity::class, MatchEntity::class],
    version = 2,
    exportSchema = false
)
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
                    "volleyball_tournament_v2_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}