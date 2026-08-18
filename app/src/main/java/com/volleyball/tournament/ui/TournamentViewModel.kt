package com.volleyball.tournament.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.volleyball.tournament.data.*
import com.volleyball.tournament.logic.TournamentEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).tournamentDao()

    val allTournaments: StateFlow<List<TournamentEntity>> = dao.getAllTournaments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedTournamentId = MutableStateFlow<String?>(null)
    val selectedTournamentId: StateFlow<String?> = _selectedTournamentId.asStateFlow()

    val currentTournament: StateFlow<TournamentEntity?> = _selectedTournamentId.flatMapLatest { id ->
        if (id != null) dao.getTournamentById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentTeams: StateFlow<List<TeamEntity>> = _selectedTournamentId.flatMapLatest { id ->
        if (id != null) dao.getTeamsForTournament(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentMatches: StateFlow<List<MatchEntity>> = _selectedTournamentId.flatMapLatest { id ->
        if (id != null) dao.getMatchesForTournament(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Admin State
    var isAdminLoggedIn = MutableStateFlow(false)
        private set

    fun loginAdmin(user: String, pass: String): Boolean {
        return if (user.trim().equals("admin", ignoreCase = true) && pass.trim() == "admin") {
            isAdminLoggedIn.value = true
            true
        } else {
            false
        }
    }

    fun logoutAdmin() {
        isAdminLoggedIn.value = false
    }

    fun selectTournament(id: String?) {
        _selectedTournamentId.value = id
    }

    fun createTournament(name: String, targetPoints: Int) {
        val newTournament = TournamentEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            targetPoints = targetPoints,
            isStarted = false,
            isCompleted = false
        )
        viewModelScope.launch {
            dao.insertTournament(newTournament)
            _selectedTournamentId.value = newTournament.id
        }
    }

    fun addTeam(teamName: String) {
        val tourneyId = _selectedTournamentId.value ?: return
        val trimmed = teamName.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val count = currentTeams.value.size
            dao.insertTeam(
                TeamEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tourneyId,
                    name = trimmed,
                    seedNumber = count + 1
                )
            )
        }
    }

    fun startTournamentMatches() {
        val tourney = currentTournament.value ?: return
        val teams = currentTeams.value
        if (teams.size < 2) return

        viewModelScope.launch {
            val (updatedTourney, matches) = TournamentEngine.generateInitialBracket(tourney, teams)
            dao.updateTournament(updatedTourney)
            dao.insertMatches(matches)
        }
    }

    fun startMatch(match: MatchEntity) {
        viewModelScope.launch {
            dao.updateMatch(match.copy(isStarted = true))
        }
    }

    fun setScore(match: MatchEntity, scoreA: Int, scoreB: Int) {
        val tourney = currentTournament.value ?: return
        val target = tourney.targetPoints

        val isDone = scoreA >= target || scoreB >= target
        val winner = when {
            scoreA >= target -> match.teamAName
            scoreB >= target -> match.teamBName
            else -> null
        }

        val updated = match.copy(
            scoreA = scoreA,
            scoreB = scoreB,
            winnerName = winner,
            isCompleted = isDone,
            isStarted = true
        )

        viewModelScope.launch {
            dao.updateMatch(updated)
            checkAndAdvanceRound()
        }
    }

    private suspend fun checkAndAdvanceRound() {
        val tourney = currentTournament.value ?: return
        val matches = currentMatches.value
        val roundMatches = matches.filter { it.roundIndex == tourney.currentRound }

        if (roundMatches.isNotEmpty() && roundMatches.all { it.isCompleted }) {
            val winners = roundMatches.mapNotNull { it.winnerName }
            if (winners.size == 1) {
                dao.updateTournament(
                    tourney.copy(
                        isCompleted = true,
                        winnerTeamName = winners.first()
                    )
                )
            } else {
                val nextRoundMatches = TournamentEngine.generateNextRoundMatches(tourney, roundMatches)
                dao.insertMatches(nextRoundMatches)
                dao.updateTournament(tourney.copy(currentRound = tourney.currentRound + 1))
            }
        }
    }

    fun closeTournament() {
        val tourney = currentTournament.value ?: return
        viewModelScope.launch {
            dao.updateTournament(tourney.copy(isCompleted = true))
        }
    }
}