package com.volleyball.tournament.logic

import com.volleyball.tournament.data.MatchEntity
import com.volleyball.tournament.data.TournamentEntity
import java.util.UUID

object TournamentEngine {

    fun createTournament(name: String, teams: List<String>, targetScore: Int): Pair<TournamentEntity, List<MatchEntity>> {
        val n = teams.size
        require(n >= 2) { "At least 2 teams required" }

        var bracketSize = 1
        var rounds = 0
        while (bracketSize < n) {
            bracketSize = bracketSize shl 1
            rounds++
        }

        val tournamentId = UUID.randomUUID().toString()
        val numByes = bracketSize - n
        val shuffledTeams = teams.shuffled()

        val byeTeams = shuffledTeams.take(numByes)
        val round1PlayingTeams = shuffledTeams.drop(numByes)

        val matches = mutableListOf<MatchEntity>()
        var matchNumber = 1

        for (i in round1PlayingTeams.indices step 2) {
            matches.add(
                MatchEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournamentId,
                    roundIndex = 1,
                    matchNumber = matchNumber++,
                    teamAName = round1PlayingTeams[i],
                    teamBName = round1PlayingTeams[i + 1],
                    isCompleted = false,
                    isBye = false
                )
            )
        }

        for (byeTeam in byeTeams) {
            matches.add(
                MatchEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournamentId,
                    roundIndex = 1,
                    matchNumber = matchNumber++,
                    teamAName = byeTeam,
                    teamBName = null,
                    winnerName = byeTeam,
                    isCompleted = true,
                    isBye = true
                )
            )
        }

        val tournament = TournamentEntity(
            id = tournamentId,
            name = name,
            targetPoints = targetScore,
            currentRound = 1,
            totalRounds = rounds,
            isCompleted = false
        )

        return Pair(tournament, matches)
    }

    fun generateNextRoundMatches(
        tournament: TournamentEntity,
        completedRoundMatches: List<MatchEntity>
    ): List<MatchEntity> {
        val nextRound = tournament.currentRound + 1
        val qualifiedTeams = completedRoundMatches.mapNotNull { it.winnerName }

        if (qualifiedTeams.size <= 1) return emptyList()

        val matches = mutableListOf<MatchEntity>()
        var matchNumber = 1

        for (i in qualifiedTeams.indices step 2) {
            val teamA = qualifiedTeams[i]
            val teamB = qualifiedTeams.getOrNull(i + 1)

            matches.add(
                MatchEntity(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournament.id,
                    roundIndex = nextRound,
                    matchNumber = matchNumber++,
                    teamAName = teamA,
                    teamBName = teamB,
                    winnerName = if (teamB == null) teamA else null,
                    isCompleted = teamB == null,
                    isBye = teamB == null
                )
            )
        }
        return matches
    }
}
