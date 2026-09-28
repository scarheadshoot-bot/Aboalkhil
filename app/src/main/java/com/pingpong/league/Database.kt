package com.pingpong.league

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val logoPath: String?
)

@Entity(
    tableName = "players",
    foreignKeys = [ForeignKey(
        entity = TeamEntity::class,
        parentColumns = ["id"],
        childColumns = ["teamId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("teamId")]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamId: Long,
    val name: String
)

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val status: String,
    val createdAt: Long,
    val stage: String,
    val winnerTeamId: Long?
)

@Entity(tableName = "tournament_teams")
data class TournamentTeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val originalTeamId: Long,
    val name: String,
    val logoPath: String?,
    val players: String
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val phase: String,
    val round: Int,
    val team1Id: Long,
    val team2Id: Long,
    val winnerId: Long?
)

@Dao
abstract class TeamDao {
    @Query("SELECT * FROM teams ORDER BY id DESC")
    abstract fun observeTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM players ORDER BY id ASC")
    abstract fun observePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT COUNT(*) FROM teams")
    abstract fun observeCount(): Flow<Int>

    @Query("SELECT * FROM teams WHERE id = :id")
    abstract suspend fun getTeam(id: Long): TeamEntity?

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY id ASC")
    abstract suspend fun playersOf(teamId: Long): List<PlayerEntity>

    @Insert
    abstract suspend fun insertTeam(team: TeamEntity): Long

    @Update
    abstract suspend fun updateTeam(team: TeamEntity)

    @Insert
    abstract suspend fun insertPlayers(players: List<PlayerEntity>)

    @Query("DELETE FROM players WHERE teamId = :teamId")
    abstract suspend fun deletePlayersOf(teamId: Long)

    @Query("DELETE FROM teams WHERE id = :id")
    abstract suspend fun deleteTeam(id: Long)

    @Transaction
    open suspend fun saveTeam(team: TeamEntity, names: List<String>) {
        val id = if (team.id == 0L) {
            insertTeam(team)
        } else {
            updateTeam(team)
            team.id
        }
        deletePlayersOf(id)
        insertPlayers(names.map { PlayerEntity(teamId = id, name = it) })
    }
}

@Dao
abstract class TournamentDao {
    @Query("SELECT * FROM tournaments WHERE status != 'ARCHIVED' LIMIT 1")
    abstract fun observeActive(): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE status = 'ARCHIVED' ORDER BY createdAt DESC")
    abstract fun observeArchived(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournament_teams")
    abstract fun observeAllTeams(): Flow<List<TournamentTeamEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    abstract suspend fun getTournament(id: Long): TournamentEntity?

    @Query("SELECT * FROM tournament_teams WHERE tournamentId = :tid ORDER BY id ASC")
    abstract fun observeTeams(tid: Long): Flow<List<TournamentTeamEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tid ORDER BY id ASC")
    abstract fun observeMatches(tid: Long): Flow<List<MatchEntity>>

    @Query("UPDATE tournaments SET status = 'ARCHIVED', stage = 'COMPLETED', winnerTeamId = :winnerId WHERE id = :id")
    abstract suspend fun finish(id: Long, winnerId: Long)

    @Insert
    abstract suspend fun insertTournament(t: TournamentEntity): Long

    @Insert
    abstract suspend fun insertTeams(list: List<TournamentTeamEntity>)

    @Insert
    abstract suspend fun insertMatch(m: MatchEntity): Long

    @Query("DELETE FROM matches WHERE id = :id")
    abstract suspend fun deleteMatch(id: Long)

    @Query("DELETE FROM matches WHERE tournamentId = :tid")
    abstract suspend fun deleteMatchesOf(tid: Long)

    @Query("DELETE FROM tournament_teams WHERE tournamentId = :tid")
    abstract suspend fun deleteTeamsOf(tid: Long)

    @Query("DELETE FROM tournaments WHERE id = :tid")
    abstract suspend fun deleteTournament(tid: Long)

    @Transaction
    open suspend fun create(t: TournamentEntity, teams: List<TournamentTeamEntity>) {
        val id = insertTournament(t)
        insertTeams(teams.map { it.copy(tournamentId = id) })
    }

    @Transaction
    open suspend fun deleteAll(tid: Long) {
        deleteMatchesOf(tid)
        deleteTeamsOf(tid)
        deleteTournament(tid)
    }
}

@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        TournamentEntity::class,
        TournamentTeamEntity::class,
        MatchEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun tournamentDao(): TournamentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pingpong.db"
                ).build().also { instance = it }
            }
        }
    }
}