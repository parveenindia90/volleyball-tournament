package com.volleyball.tournament.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.volleyball.tournament.data.AppDatabase
import com.volleyball.tournament.data.MatchEntity
import com.volleyball.tournament.data.TournamentEntity
import com.volleyball.tournament.logic.TournamentEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).tournamentDao()

    val activeTournament: StateFlow<TournamentEntity?> = dao.getActiveTournament()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val activeMatches: StateFlow<List<MatchEntity>> = activeTournament.flatMapLatest { tournament ->
        if (tournament != null) dao.getMatchesForTournament(tournament.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun startNewTournament(name: String, teamsListRaw: String, targetPoints: Int) {
        val teams = teamsListRaw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (teams.size < 2) return

        viewModelScope.launch {
            dao.clearAllTournaments()
            dao.clearAllMatches()

            val (tournament, matches) = TournamentEngine.createTournament(name, teams, targetPoints)
            dao.insertTournament(tournament)
            dao.insertMatches(matches)
        }
    }

    fun updateScore(match: MatchEntity, scoreA: Int, scoreB: Int) {
        val tournament = activeTournament.value ?: return
        val target = tournament.targetPoints

        val isFinished = scoreA >= target || scoreB >= target
        val winner = when {
            scoreA >= target -> match.teamAName
            scoreB >= target -> match.teamBName
            else -> null
        }

        val updated = match.copy(
            scoreA = scoreA,
            scoreB = scoreB,
            winnerName = winner,
            isCompleted = isFinished
        )

        viewModelScope.launch {
            dao.updateMatch(updated)
            checkAndAdvanceRound()
        }
    }

    private suspend fun checkAndAdvanceRound() {
        val tournament = activeTournament.value ?: return
        val matches = activeMatches.value
        val currentRoundMatches = matches.filter { it.roundIndex == tournament.currentRound }

        if (currentRoundMatches.isNotEmpty() && currentRoundMatches.all { it.isCompleted }) {
            val winners = currentRoundMatches.mapNotNull { it.winnerName }

            if (winners.size == 1) {
                dao.updateTournament(
                    tournament.copy(
                        isCompleted = true,
                        winnerTeamName = winners.first()
                    )
                )
            } else {
                val nextRoundMatches = TournamentEngine.generateNextRoundMatches(tournament, currentRoundMatches)
                dao.insertMatches(nextRoundMatches)
                dao.updateTournament(tournament.copy(currentRound = tournament.currentRound + 1))
            }
        }
    }

    fun resetTournament() {
        viewModelScope.launch {
            dao.clearAllTournaments()
            dao.clearAllMatches()
        }
    }
}
