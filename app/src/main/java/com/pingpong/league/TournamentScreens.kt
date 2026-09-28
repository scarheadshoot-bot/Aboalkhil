package com.pingpong.league

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch

private data class TeamRow(val team: TournamentTeamEntity, val played: Int, val wins: Int)

@Composable
private fun StatCell(text: String, color: Color = Color.White, bold: Boolean = false, size: Int = 15) {
    Text(
        text,
        color = color,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(44.dp)
    )
}

@Composable
fun CreateTournamentScreen(db: AppDatabase, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val teams by db.teamDao().observeTeams().collectAsState(initial = emptyList())
    val players by db.teamDao().observePlayers().collectAsState(initial = emptyList())
    val byTeam = players.groupBy { it.teamId }
    var name by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(2) }
    var starting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }

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
                    "بطولة جديدة",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم البطولة") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("عدد الفرق", color = Color.White, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (count > 2) {
                            count -= 1
                            while (selected.size > count) selected.removeAt(selected.size - 1)
                        }
                    }) { Text("−", color = NeonCyan, fontSize = 30.sp) }
                    Text(
                        "$count",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        if (count < teams.size) count += 1
                    }) { Text("+", color = NeonCyan, fontSize = 30.sp) }
                }
            }
            Text(
                "اختر ${selected.size} من $count",
                color = NeonCyan,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (teams.size < 2) {
                Text(
                    "أنشئ فريقين على الأقل أولًا",
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(teams, key = { it.id }) { team ->
                        val isSel = selected.contains(team.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isSel) 1f else 0.55f)
                                .clickable {
                                    if (isSel) {
                                        selected.remove(team.id)
                                    } else if (selected.size < count) {
                                        selected.add(team.id)
                                    } else {
                                        Toast
                                            .makeText(ctx, "اكتمل العدد، ألغِ فريقًا أولًا", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) Color(0x668B5CF6) else CardBg
                            ),
                            border = if (isSel) BorderStroke(3.dp, NeonCyan) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TeamLogo(team.logoPath, 52.dp)
                                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        team.name,
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val names = byTeam[team.id]?.joinToString("، ") { it.name } ?: ""
                                    if (names.isNotEmpty()) {
                                        Text(names, color = Color(0xCCFFFFFF), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            NeonButton("بدء البطولة", onClick = {
                if (selected.size != count) {
                    Toast.makeText(ctx, "اختر $count فرق", Toast.LENGTH_SHORT).show()
                } else if (!starting) {
                    starting = true
                    val list = selected.mapNotNull { id ->
                        val t = teams.firstOrNull { it.id == id } ?: return@mapNotNull null
                        TournamentTeamEntity(
                            tournamentId = 0,
                            originalTeamId = t.id,
                            name = t.name,
                            logoPath = t.logoPath,
                            players = (byTeam[t.id] ?: emptyList()).joinToString("\n") { it.name }
                        )
                    }
                    scope.launch {
                        db.tournamentDao().create(
                            TournamentEntity(
                                name = name.trim().ifEmpty { "بطولة جديدة" },
                                status = "ACTIVE",
                                createdAt = System.currentTimeMillis(),
                                stage = "LEAGUE",
                                winnerTeamId = null
                            ),
                            list
                        )
                        onStarted()
                    }
                }
            })
        }
    }
}

@Composable
fun TournamentScreen(db: AppDatabase, onHome: () -> Unit) {
    val scope = rememberCoroutineScope()
    val active by db.tournamentDao().observeActive().collectAsState(initial = null)
    val t = active
    if (t == null) {
        NeonBackground { }
        return
    }
    val teams by remember(t.id) { db.tournamentDao().observeTeams(t.id) }.collectAsState(initial = emptyList())
    val matches by remember(t.id) { db.tournamentDao().observeMatches(t.id) }.collectAsState(initial = emptyList())

    var first by remember { mutableStateOf<Long?>(null) }
    var second by remember { mutableStateOf<Long?>(null) }
    var pendingWinner by remember { mutableStateOf<Long?>(null) }
    var undoMatch by remember { mutableStateOf<MatchEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val league = matches.filter { it.phase == "LEAGUE" }
    val rows = teams.map { tm ->
        val played = league.filter { it.team1Id == tm.id || it.team2Id == tm.id }
        TeamRow(tm, played.size, played.count { it.winnerId == tm.id })
    }.sortedByDescending { it.wins }
    val n = teams.size
    val total = n * (n - 1) / 2
    val done = league.size
    val leagueDone = n >= 2 && done >= total

    val a = teams.firstOrNull { it.id == first }
    val b = teams.firstOrNull { it.id == second }
    val already = a != null && b != null && league.any {
        (it.team1Id == a.id && it.team2Id == b.id) || (it.team1Id == b.id && it.team2Id == a.id)
    }

    fun onTap(id: Long) {
        if (first == id) {
            first = second
            second = null
        } else if (second == id) {
            second = null
        } else if (first == null) {
            first = id
        } else {
            second = id
        }
    }

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onHome) { Text("رجوع", color = NeonCyan) }
                Text(
                    t.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Text(
                "مرحلة الدوري • $done من $total",
                color = NeonCyan,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (a != null && b != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.clickable(enabled = !already) { pendingWinner = a.id },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TeamLogo(a.logoPath, 84.dp)
                                Text(a.name, color = Color.White, fontSize = 14.sp)
                            }
                            Text(
                                "VS",
                                color = NeonCyan,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Column(
                                modifier = Modifier.clickable(enabled = !already) { pendingWinner = b.id },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TeamLogo(b.logoPath, 84.dp)
                                Text(b.name, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (already) {
                            Text("هذه المواجهة انتهت مسبقًا", color = Color(0xFFF87171), fontSize = 15.sp)
                        } else {
                            Text("اضغط على الفائز", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        StatCell("لعب", Color(0xCCFFFFFF), false, 12)
                        StatCell("فوز", Color(0xCCFFFFFF), false, 12)
                        StatCell("خسارة", Color(0xCCFFFFFF), false, 12)
                        StatCell("نقاط", Color(0xCCFFFFFF), false, 12)
                    }
                }
                items(rows, key = { it.team.id }) { row ->
                    val sel = row.team.id == first || row.team.id == second
                    val rank = rows.indexOfFirst { it.wins == row.wins } + 1
                    val names = row.team.players.split("\n").filter { it.isNotBlank() }.joinToString("، ")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTap(row.team.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sel) Color(0x668B5CF6) else CardBg
                        ),
                        border = if (sel) BorderStroke(3.dp, NeonCyan) else null
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
                            StatCell("${row.played}")
                            StatCell("${row.wins}")
                            StatCell("${row.played - row.wins}")
                            StatCell("${row.wins}", NeonCyan, true, 18)
                        }
                    }
                }

                if (leagueDone) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x6622C55E))
                        ) {
                            Text(
                                "اكتمل الدوري ✅",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                if (league.isNotEmpty()) {
                    item {
                        Text(
                            "النتائج",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    items(league.reversed(), key = { it.id }) { m ->
                        val w = teams.firstOrNull { it.id == m.winnerId }
                        val loserId = if (m.winnerId == m.team1Id) m.team2Id else m.team1Id
                        val l = teams.firstOrNull { it.id == loserId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                TextButton(onClick = { undoMatch = m }) {
                                    Text("×", color = Color(0xFFF87171), fontSize = 26.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حذف البطولة", color = Color(0xFFF87171))
                    }
                }
            }
        }
    }

    val w = pendingWinner
    if (w != null && a != null && b != null) {
        val winner = if (w == a.id) a else b
        AlertDialog(
            onDismissRequest = { pendingWinner = null },
            title = { Text("تسجيل فوز ${winner.name}؟") },
            confirmButton = {
                TextButton(onClick = {
                    pendingWinner = null
                    first = null
                    second = null
                    scope.launch {
                        db.tournamentDao().insertMatch(
                            MatchEntity(
                                tournamentId = t.id,
                                phase = "LEAGUE",
                                round = 0,
                                team1Id = a.id,
                                team2Id = b.id,
                                winnerId = winner.id
                            )
                        )
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { pendingWinner = null }) { Text("إلغاء") }
            }
        )
    }

    val um = undoMatch
    if (um != null) {
        AlertDialog(
            onDismissRequest = { undoMatch = null },
            title = { Text("حذف هذه النتيجة؟") },
            confirmButton = {
                TextButton(onClick = {
                    undoMatch = null
                    scope.launch { db.tournamentDao().deleteMatch(um.id) }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { undoMatch = null }) { Text("إلغاء") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف البطولة؟") },
            text = { Text("هذه البطولة قيد التنفيذ. حذفها سيؤدي إلى حذف نتائجها. هل أنت متأكد؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        db.tournamentDao().deleteAll(t.id)
                        onHome()
                    }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            }
        )
    }
}