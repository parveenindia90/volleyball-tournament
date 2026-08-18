package com.volleyball.tournament.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volleyball.tournament.data.MatchEntity
import com.volleyball.tournament.data.TeamEntity
import com.volleyball.tournament.data.TournamentEntity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: TournamentViewModel = viewModel()
                    val selectedTournamentId by vm.selectedTournamentId.collectAsStateWithLifecycle()

                    if (selectedTournamentId == null) {
                        HomeScreen(viewModel = vm)
                    } else {
                        TournamentDetailsScreen(viewModel = vm)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. HOME SCREEN (Tournament List & Start New Tournament)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TournamentViewModel) {
    val tournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏐 Volleyball Tournaments", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isAdmin) {
                        AssistChip(
                            onClick = { viewModel.logoutAdmin() },
                            label = { Text("Admin (Logout)") },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    } else {
                        IconButton(onClick = { showLoginDialog = true }) {
                            Icon(Icons.Default.Lock, contentDescription = "Admin Login")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Start a Tournament") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!isAdmin) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "You are in Viewer Mode. Log in as admin to create tournaments & score matches.",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Text("All Tournaments", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (tournaments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tournaments yet. Click 'Start a Tournament' to create one!", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tournaments) { tourney ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectTournament(tourney.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tourney.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    if (tourney.isCompleted) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Finished 🏆") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color(0xFFFFD700))
                                        )
                                    } else if (tourney.isStarted) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Live • Round ${tourney.currentRound}") }
                                        )
                                    } else {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Setup Phase") }
                                        )
                                    }
                                }
                                if (tourney.winnerTeamName != null) {
                                    Text(
                                        "Winner: ${tourney.winnerTeamName}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text("Target Points: ${tourney.targetPoints} pts", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTournamentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, points ->
                viewModel.createTournament(name, points)
                showCreateDialog = false
            }
        )
    }

    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = { u, p ->
                val ok = viewModel.loginAdmin(u, p)
                if (ok) showLoginDialog = false
                ok
            }
        )
    }
}

// -------------------------------------------------------------
// 2. TOURNAMENT DETAILS & DASHBOARD SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailsScreen(viewModel: TournamentViewModel) {
    val tournament by viewModel.currentTournament.collectAsStateWithLifecycle()
    val teams by viewModel.currentTeams.collectAsStateWithLifecycle()
    val matches by viewModel.currentMatches.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var scoringMatch by remember { mutableStateOf<MatchEntity?>(null) }
    var showCloseWarning by remember { mutableStateOf(false) }

    val tourney = tournament ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tourney.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectTournament(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin && !tourney.isCompleted) {
                        Button(
                            onClick = { showCloseWarning = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Close Tournament")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Champion Card
            if (tourney.isCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Tournament Concluded!", fontSize = 14.sp)
                            Text(
                                tourney.winnerTeamName ?: "No Winner Declared",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tab Navigation: 0 -> Matches/Rounds, 1 -> Teams
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Rounds & Matches") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Teams (${teams.size})") })
            }

            if (selectedTab == 0) {
                if (!tourney.isStarted) {
                    SetupTournamentView(
                        teams = teams,
                        isAdmin = isAdmin,
                        onStartMatches = { viewModel.startTournamentMatches() }
                    )
                } else {
                    RoundsMatchesView(
                        tournament = tourney,
                        matches = matches,
                        isAdmin = isAdmin,
                        onStartMatch = { viewModel.startMatch(it) },
                        onOpenScoring = { scoringMatch = it }
                    )
                }
            } else {
                TeamsListView(
                    teams = teams,
                    isAdmin = isAdmin && !tourney.isStarted,
                    onAddTeam = { viewModel.addTeam(it) }
                )
            }
        }
    }

    // Interactive 15-Point Scoring Modal Dialog
    if (scoringMatch != null) {
        val liveMatch = matches.find { it.id == scoringMatch!!.id } ?: scoringMatch!!
        ScoreboardDialog(
            match = liveMatch,
            targetPoints = tourney.targetPoints,
            isAdmin = isAdmin,
            onSetScore = { a, b -> viewModel.setScore(liveMatch, a, b) },
            onDismiss = { scoringMatch = null }
        )
    }

    // Close Tournament Warning Dialog
    if (showCloseWarning) {
        AlertDialog(
            onDismissRequest = { showCloseWarning = false },
            title = { Text("Close Tournament?") },
            text = {
                if (tourney.winnerTeamName == null) {
                    Text("⚠️ Warning: No team has won the finals yet. If you close now, the tournament will end without a champion.")
                } else {
                    Text("Are you sure you want to officially end this tournament?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeTournament()
                        showCloseWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseWarning = false }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// 3. ROUNDS & MATCHES SCREEN
// -------------------------------------------------------------
@Composable
fun RoundsMatchesView(
    tournament: TournamentEntity,
    matches: List<MatchEntity>,
    isAdmin: Boolean,
    onStartMatch: (MatchEntity) -> Unit,
    onOpenScoring: (MatchEntity) -> Unit
) {
    val totalRounds = matches.map { it.roundIndex }.distinct().maxOrNull() ?: 1
    var activeRoundTab by remember(tournament.currentRound) { mutableIntStateOf(tournament.currentRound) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScrollableTabRow(
            selectedTabIndex = (activeRoundTab - 1).coerceIn(0, totalRounds - 1),
            edgePadding = 0.dp
        ) {
            (1..totalRounds).forEach { r ->
                Tab(
                    selected = activeRoundTab == r,
                    onClick = { activeRoundTab = r },
                    text = { Text(if (r == tournament.totalRounds) "Finals" else "Round $r") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val roundMatches = matches.filter { it.roundIndex == activeRoundTab }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(roundMatches) { match ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (match.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (match.isBye) {
                            Text("✨ ${match.teamAName} received a Bye", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Text("Advanced automatically to next round", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Match #${match.matchNumber}", fontSize = 12.sp, color = Color.Gray)
                                if (match.isCompleted) {
                                    Text("Winner: ${match.winnerName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                } else if (match.isStarted) {
                                    Text("LIVE NOW", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                } else {
                                    Text("Scheduled", color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(match.teamAName, fontSize = 16.sp, fontWeight = if (match.winnerName == match.teamAName) FontWeight.Bold else FontWeight.Normal)
                                Text("${match.scoreA} pts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(match.teamBName ?: "TBD", fontSize = 16.sp, fontWeight = if (match.winnerName == match.teamBName) FontWeight.Bold else FontWeight.Normal)
                                Text("${match.scoreB} pts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                if (isAdmin && !match.isCompleted) {
                                    if (!match.isStarted) {
                                        Button(onClick = { onStartMatch(match) }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Start Match")
                                        }
                                    } else {
                                        Button(onClick = { onOpenScoring(match) }) {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open Scoreboard")
                                        }
                                    }
                                } else if (match.isStarted || match.isCompleted) {
                                    OutlinedButton(onClick = { onOpenScoring(match) }) {
                                        Text("View Points Breakdown")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. INTERACTIVE 15-POINT SCOREBOARD DIALOG
// -------------------------------------------------------------
@Composable
fun ScoreboardDialog(
    match: MatchEntity,
    targetPoints: Int,
    isAdmin: Boolean,
    onSetScore: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localScoreA by remember { mutableIntStateOf(match.scoreA) }
    var localScoreB by remember { mutableIntStateOf(match.scoreB) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Match #${match.matchNumber} Scoreboard", fontWeight = FontWeight.Bold)
                Text("First to $targetPoints points wins", fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Team A scoring section
                TeamPointGrid(
                    teamName = match.teamAName,
                    score = localScoreA,
                    targetPoints = targetPoints,
                    isEditable = isAdmin && !match.isCompleted,
                    onScoreSelected = {
                        localScoreA = it
                        onSetScore(localScoreA, localScoreB)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Team B scoring section
                TeamPointGrid(
                    teamName = match.teamBName ?: "Team B",
                    score = localScoreB,
                    targetPoints = targetPoints,
                    isEditable = isAdmin && !match.isCompleted,
                    onScoreSelected = {
                        localScoreB = it
                        onSetScore(localScoreA, localScoreB)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun TeamPointGrid(
    teamName: String,
    score: Int,
    targetPoints: Int,
    isEditable: Boolean,
    onScoreSelected: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(teamName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("$score / $targetPoints", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of 15 buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(130.dp)
        ) {
            items(targetPoints) { index ->
                val point = index + 1
                val isSelected = score >= point
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable(enabled = isEditable) {
                            // If tapped the same point, step down, otherwise set score to point
                            if (score == point) onScoreSelected(point - 1) else onScoreSelected(point)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$point",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. SETUP & TEAMS LIST VIEWS
// -------------------------------------------------------------
@Composable
fun SetupTournamentView(
    teams: List<TeamEntity>,
    isAdmin: Boolean,
    onStartMatches: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SportsVolleyball, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Setup Phase", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("${teams.size} Teams Added", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(20.dp))

        if (teams.size < 2) {
            Text("Add at least 2 teams in the 'Teams' tab to generate tournament rounds.", color = MaterialTheme.colorScheme.error)
        } else if (isAdmin) {
            Button(
                onClick = onStartMatches,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Generate Bracket & Start Match 1")
            }
        } else {
            Text("Waiting for Admin to start the tournament.")
        }
    }
}

@Composable
fun TeamsListView(
    teams: List<TeamEntity>,
    isAdmin: Boolean,
    onAddTeam: (String) -> Unit
) {
    var newTeamName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isAdmin) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTeamName,
                    onValueChange = { newTeamName = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newTeamName.isNotBlank()) {
                            onAddTeam(newTeamName)
                            newTeamName = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Add Team")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Registered Teams (${teams.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(teams) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${team.seedNumber}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(team.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. DIALOGS (Create Tournament & Admin Login)
// -------------------------------------------------------------
@Composable
fun CreateTournamentDialog(onDismiss: () -> Unit, onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Tournament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tournament Name") })
                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it.filter { c -> c.isDigit() } },
                    label = { Text("Target Win Points (e.g. 15)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onCreate(name, points.toIntOrNull() ?: 15)
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoginDialog(onDismiss: () -> Unit, onLogin: (String, String) -> Boolean) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Login") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Default Admin Credentials:\nUser: admin | Pass: admin", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val ok = onLogin(username, password)
                if (!ok) errorMsg = "Invalid credentials. Use admin / admin."
            }) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}