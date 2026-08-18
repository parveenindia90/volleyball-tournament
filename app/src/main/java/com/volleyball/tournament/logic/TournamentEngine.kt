package com.volleyball.tournament.logic

import com.volleyball.tournament.data.MatchEntity
import com.volleyball.tournament.data.TeamEntity
import com.volleyball.tournament.data.TournamentEntity
import java.util.UUID

object TournamentEngine {

    fun generateInitialBracket(
        tournament: TournamentEntity,
        teams: List<TeamEntity>
    ): Pair<TournamentEntity, List<MatchEntity>> {
        val n = teams.size
        require(n >= 2) { "At least 2 teams required to generate a bracket." }

        var bracketSize = 1
        var rounds = 0
        while (bracketSize < n) {
            bracketSize = bracketSize shl 1
            rounds++
        }

        val numByes = bracketSize - n
        val shuffledTeams = teams.map { it.name }.shuffled()

        val byeTeams = shuffledTeams.take(numByes)
        val playingTeams = shuffledTeams.drop(numByes)

        val matches = mutableListOf<MatchEntity>()
        var matchNumber = 1

        // Regular Round 1 matches
        for (i in playingTeams.indices step 2) {
            matches.add(
                MatchEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournament.id,
                    roundIndex = 1,
                    matchNumber = matchNumber++,
                    teamAName = playingTeams[i],
                    teamBName = playingTeams[i + 1],
                    isStarted = false,
                    isCompleted = false,
                    isBye = false
                )
            )
        }

        // Round 1 Byes
        for (byeTeam in byeTeams) {
            matches.add(
                MatchEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournament.id,
                    roundIndex = 1,
                    matchNumber = matchNumber++,
                    teamAName = byeTeam,
                    teamBName = null,
                    winnerName = byeTeam,
                    isStarted = true,
                    isCompleted = true,
                    isBye = true
                )
            )
        }

        val updatedTournament = tournament.copy(
            isStarted = true,
            totalRounds = rounds,
            currentRound = 1
        )

        return Pair(updatedTournament, matches)
    }

    fun generateNextRoundMatches(
        tournament: TournamentEntity,
        currentRoundMatches: List<MatchEntity>
    ): List<MatchEntity> {
        val nextRound = tournament.currentRound + 1
        val winners = currentRoundMatches.mapNotNull { it.winnerName }

        if (winners.size <= 1) return emptyList()

        val matches = mutableListOf<MatchEntity>()
        var matchNumber = 1

        for (i in winners.indices step 2) {
            val teamA = winners[i]
            val teamB = winners.getOrNull(i + 1)

            matches.add(
                MatchEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournament.id,
                    roundIndex = nextRound,
                    matchNumber = matchNumber++,
                    teamAName = teamA,
                    teamBName = teamB,
                    winnerName = if (teamB == null) teamA else null,
                    isStarted = teamB == null,
                    isCompleted = teamB == null,
                    isBye = teamB == null
                )
            )
        }
        return matches
    }
}