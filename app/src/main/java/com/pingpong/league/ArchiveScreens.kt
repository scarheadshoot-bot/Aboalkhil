package com.pingpong.league

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ArchiveRow(val team: TournamentTeamEntity, val played: Int, val wins: Int)

@Composable
fun WinnersScreen(db: AppDatabase, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val archived by db.tournamentDao().observeArchived().collectAsState(initial = emptyList())
    val allTeams by db.tournamentDao().observeAllTeams().collectAsState(initial = emptyList())
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("رجوع", color = NeonCyan) }
                Text(
                    "سجل الفائزين",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (archived.isEmpty()) {
                Text(
                    "لا توجد بطولات منتهية بعد",
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(archived, key = { "arch_${it.id}" }) { t ->
                        val winner = allTeams.firstOrNull { it.id == t.winnerTeamId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(t.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (winner != null) {
                                    TeamLogo(winner.logoPath, 56.dp)
                                } else {
                                    Text("🏆", fontSize = 40.sp)
                                }
                                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        "🏆 ${t.name}",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "الفائز: ${winner?.name ?: "-"}",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        fmt.format(Date(t.createdAt)),
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveDetailScreen(db: AppDatabase, tournamentId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<TournamentEntity?>(null) }
    LaunchedEffect(tournamentId) {
        loaded = db.tournamentDao().getTournament(tournamentId)
    }
    val teams by remember(tournamentId) { db.tournamentDao().observeTeams(tournamentId) }
        .collectAsState(initial = emptyList())
    val matches by remember(tournamentId) { db.tournamentDao().observeMatches(tournamentId) }
        .collectAsState(initial = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    val tour = loaded

    val league = matches.filter { it.phase == "LEAGUE" }
    val rows = teams.map { tm ->
        val played = league.filter { it.team1Id == tm.id || it.team2Id == tm.id }
        ArchiveRow(tm, played.size, played.count { it.winnerId == tm.id })
    }.sortedByDescending { it.wins }
    val champion = teams.firstOrNull { it.id == tour?.winnerTeamId }

    NeonBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "top") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("رجوع", color = NeonCyan) }
                    Text(
                        tour?.name ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            if (champion != null) {
                item(key = "champ") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x55FBBF24)),
                        border = BorderStroke(2.dp, Color(0xFFFBBF24))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆", fontSize = 60.sp)
                            TeamLogo(champion.logoPath, 100.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                champion.name,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            champion.players.split("\n").filter { it.isNotBlank() }.forEach {
                                Text(it, color = Color(0xEEFFFFFF), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            item(key = "rank_title") {
                Text(
                    "الترتيب",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            items(rows, key = { "team_${it.team.id}" }) { row ->
                val rank = rows.indexOfFirst { it.wins == row.wins } + 1
                val names = row.team.players.split("\n").filter { it.isNotBlank() }.joinToString("، ")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$rank",
                            color = Color(0xCCFFFFFF),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(24.dp)
                        )
                        TeamLogo(row.team.logoPath, 44.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                row.team.name,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (names.isNotEmpty()) {
                                Text(names, color = Color(0xCCFFFFFF), fontSize = 11.sp)
                            }
                        }
                        Text(
                            "${row.wins}",
                            color = NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            item(key = "res_title") {
                Text(
                    "النتائج",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            items(matches.reversed(), key = { "match_${it.id}" }) { m ->
                val w = teams.firstOrNull { it.id == m.winnerId }
                val loserId = if (m.winnerId == m.team1Id) m.team2Id else m.team1Id
                val l = teams.firstOrNull { it.id == loserId }
                val label = when (m.phase) {
                    "TIE" -> "فاصلة"
                    "FINAL" -> "النهائي"
                    else -> "مواجهة"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (w != null) TeamLogo(w.logoPath, 40.dp)
                        Text(
                            "+1",
                            color = Color(0xFF4ADE80),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        if (l != null) TeamLogo(l.logoPath, 40.dp)
                        Text(
                            "0",
                            color = Color(0xFFF87171),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(label, color = Color(0xFFFBBF24), fontSize = 12.sp)
                    }
                }
            }

            item(key = "delete") {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حذف هذه البطولة", color = Color(0xFFF87171))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("هل تريد حذف هذه البطولة نهائيًا؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        db.tournamentDao().deleteAll(tournamentId)
                        onBack()
                    }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            }
        )
    }
}