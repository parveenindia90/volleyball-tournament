package com.volleyball.tournament.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volleyball.tournament.data.MatchEntity
import com.volleyball.tournament.data.TournamentEntity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TournamentViewModel = viewModel()
                    val tournament by viewModel.activeTournament.collectAsStateWithLifecycle()
                    val matches by viewModel.activeMatches.collectAsStateWithLifecycle()

                    if (tournament == null) {
                        TournamentSetupScreen(onStart = { name, teams, target ->
                            viewModel.startNewTournament(name, teams, target)
                        })
                    } else {
                        TournamentDashboardScreen(
                            tournament = tournament!!,
                            matches = matches,
                            onUpdateScore = { match, a, b -> viewModel.updateScore(match, a, b) },
                            onReset = { viewModel.resetTournament() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentSetupScreen(onStart: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("Volleyball Championship") }
    var targetPoints by remember { mutableStateOf("15") }
    var teamsInput by remember {
        mutableStateOf((1..20).joinToString("\n") { "Team $it" })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Create Tournament", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tournament Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = targetPoints,
            onValueChange = { targetPoints = it.filter { char -> char.isDigit() } },
            label = { Text("Target Points to Win") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = teamsInput,
            onValueChange = { teamsInput = it },
            label = { Text("Teams (One per line)") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Button(
            onClick = {
                val pts = targetPoints.toIntOrNull() ?: 15
                onStart(name, teamsInput, pts)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Generate Bracket & Start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDashboardScreen(
    tournament: TournamentEntity,
    matches: List<MatchEntity>,
    onUpdateScore: (MatchEntity, Int, Int) -> Unit,
    onReset: () -> Unit
) {
    var selectedRound by remember(tournament.currentRound) { mutableIntStateOf(tournament.currentRound) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tournament.name, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Tournament")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (tournament.isCompleted && tournament.winnerTeamName != null) {
                ChampionBanner(winnerName = tournament.winnerTeamName)
                Spacer(modifier = Modifier.height(12.dp))
            }

            val totalExistingRounds = matches.map { it.roundIndex }.distinct().maxOrNull() ?: 1
            ScrollableTabRow(
                selectedTabIndex = (selectedRound - 1).coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..totalExistingRounds).forEach { r ->
                    Tab(
                        selected = selectedRound == r,
                        onClick = { selectedRound = r },
                        text = {
                            Text(if (r == tournament.totalRounds) "Finals" else "Round $r")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val roundMatches = matches.filter { it.roundIndex == selectedRound }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(roundMatches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        targetPoints = tournament.targetPoints,
                        onUpdateScore = { a, b -> onUpdateScore(match, a, b) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChampionBanner(winnerName: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text("Tournament Champion!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(winnerName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun MatchCard(
    match: MatchEntity,
    targetPoints: Int,
    onUpdateScore: (Int, Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (match.isBye) {
                Text(
                    text = "${match.teamAName} received a Round 1 Bye",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Advanced directly to Round 2",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Match #${match.matchNumber} (First to $targetPoints pts)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (match.isCompleted) {
                        Text(
                            "Winner: ${match.winnerName}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TeamScoreRow(
                    teamName = match.teamAName,
                    score = match.scoreA,
                    isWinner = match.winnerName == match.teamAName,
                    isCompleted = match.isCompleted,
                    onScoreChange = { newScore -> onUpdateScore(newScore, match.scoreB) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                TeamScoreRow(
                    teamName = match.teamBName ?: "TBD",
                    score = match.scoreB,
                    isWinner = match.winnerName == match.teamBName,
                    isCompleted = match.isCompleted,
                    onScoreChange = { newScore -> onUpdateScore(match.scoreA, newScore) }
                )
            }
        }
    }
}

@Composable
fun TeamScoreRow(
    teamName: String,
    score: Int,
    isWinner: Boolean,
    isCompleted: Boolean,
    onScoreChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = teamName,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isCompleted) {
                FilledIconButton(
                    onClick = { if (score > 0) onScoreChange(score - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = score.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (!isCompleted) {
                FilledIconButton(
                    onClick = { onScoreChange(score + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
