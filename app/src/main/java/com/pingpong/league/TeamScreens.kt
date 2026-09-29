package com.pingpong.league

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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

private data class Resolution(
    val finalists: List<Long>?,
    val qualified: List<Long>,
    val tieCandidates: List<Long>,
    val tieRound: Int,
    val tieSlots: Int
)

private fun resolve(ids: List<Long>, league: List<MatchEntity>, ties: List<MatchEntity>): Resolution {
    val qualified = mutableListOf<Long>()
    var slots = 2
    var pool: List<Long> = ids
    var level = 0
    for (guard in 0 until 200) {
        val lv = level
        val groups = pool
            .groupBy { id ->
                if (lv == 0) league.count { it.winnerId == id }
                else ties.count { it.round == lv && it.winnerId == id }
            }
            .toList()
            .sortedByDescending { it.first }
        var next: List<Long>? = null
        for (entry in groups) {
            val g = entry.second
            if (g.size <= slots) {
                qualified.addAll(g)
                slots -= g.size
                if (slots == 0) break
            } else {
                next = g
                break
            }
        }
        if (next == null) {
            return Resolution(qualified.toList(), qualified.toList(), emptyList(), 0, 0)
        }
        val r = lv + 1
        val pairs = next.size * (next.size - 1) / 2
        val played = ties.count { it.round == r }
        if (played < pairs) {
            return Resolution(null, qualified.toList(), next, r, slots)
        }
        pool = next
        level = r
    }
    return Resolution(qualified.toList(), qualified.toList(), emptyList(), 0, 0)
}

private fun samePair(m: MatchEntity, x: Long, y: Long): Boolean {
    return (m.team1Id == x && m.team2Id == y) || (m.team1Id == y && m.team2Id == x)
}

private fun matchKey(m: MatchEntity): Int {
    val p = when (m.phase) {
        "LEAGUE" -> 0
        "TIE" -> 1
        else -> 2
    }
    return p * 1000 + m.round
}

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
                    items(teams, key = { "team_${it.id}" }) { team ->
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
private fun Celebration(
    tournamentName: String,
    champ: TournamentTeamEntity,
    rows: List<TeamRow>,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "celebrate")
    val pulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    val glow by inf.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )
    val gold = Color(0xFFFBBF24)
    val names = champ.players.split("\n").filter { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("رجوع", color = NeonCyan) }
        }
        Text("✨ ⭐ ✨", fontSize = 26.sp, modifier = Modifier.alpha(glow))
        Text("تهانينا", color = gold, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("🏆", fontSize = 110.sp, modifier = Modifier.scale(pulse))
        Spacer(Modifier.height(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .background(
                    Brush.radialGradient(listOf(gold.copy(alpha = glow * 0.6f), Color.Transparent)),
                    CircleShape
                )
        ) {
            TeamLogo(champ.logoPath, 150.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            champ.name,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        names.forEach {
            Text(it, color = Color(0xEEFFFFFF), fontSize = 18.sp)
        }
        Spacer(Modifier.height(24.dp))
        PosterButton(makeBitmap = {
            buildPosterBitmap(
                tournamentName = tournamentName,
                championName = champ.name,
                championLogoPath = champ.logoPath,
                championPlayers = names,
                rows = rows.mapIndexed { i, r -> PosterRow(i + 1, r.team.name, r.team.logoPath, r.wins) }
            )
        })
        Spacer(Modifier.height(12.dp))
        NeonButton("إنهاء البطولة", onClick = onFinish)
        TextButton(onClick = onUndo) {
            Text("إلغاء نتيجة النهائي ×", color = Color(0xFFF87171))
        }
    }
}

@Composable
fun TournamentScreen(db: AppDatabase, onHome: () -> Unit) {
    val scope = rememberCoroutineScope()
    val active by db.tournamentDao().observeActive().collectAsState(initial = null)
    val current = active
    if (current == null) {
        NeonBackground { }
    } else {
        TournamentContent(
            db = db,
            t = current,
            onHome = onHome,
            onFinish = { winnerId ->
                scope.launch {
                    db.tournamentDao().finish(current.id, winnerId)
                    onHome()
                }
            },
            onDelete = {
                scope.launch {
                    db.tournamentDao().deleteAll(current.id)
                    onHome()
                }
            }
        )
    }
}

@Composable
private fun TournamentContent(
    db: AppDatabase,
    t: TournamentEntity,
    onHome: () -> Unit,
    onFinish: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val teams by remember(t.id) { db.tournamentDao().observeTeams(t.id) }.collectAsState(initial = emptyList())
    val matches by remember(t.id) { db.tournamentDao().observeMatches(t.id) }.collectAsState(initial = emptyList())

    var first by remember { mutableStateOf<Long?>(null) }
    var second by remember { mutableStateOf<Long?>(null) }
    var pendingWinner by remember { mutableStateOf<Long?>(null) }
    var undoMatch by remember { mutableStateOf<MatchEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var introShown by remember(t.id) { mutableStateOf(false) }

    val league = matches.filter { it.phase == "LEAGUE" }
    val ties = matches.filter { it.phase == "TIE" }
    val finals = matches.filter { it.phase == "FINAL" }
    val rows = teams.map { tm ->
        val played = league.filter { it.team1Id == tm.id || it.team2Id == tm.id }
        TeamRow(tm, played.size, played.count { it.winnerId == tm.id })
    }.sortedByDescending { it.wins }
    val n = teams.size
    val total = n * (n - 1) / 2
    val done = league.size
    val leagueDone = n >= 2 && done >= total
    val res = if (leagueDone) resolve(teams.map { it.id }, league, ties) else null
    val finalists = res?.finalists
    val finalMatch = finals.firstOrNull()
    val champion = finalMatch?.winnerId?.let { wid -> teams.firstOrNull { it.id == wid } }

    val mode = when {
        champion != null -> "WIN"
        finalists != null -> "FINAL"
        res != null -> "TIE"
        else -> "LEAGUE"
    }

    fun canPick(id: Long): Boolean {
        return when (mode) {
            "TIE" -> res != null && res.tieCandidates.contains(id)
            "FINAL" -> finalists != null && finalists.contains(id)
            else -> true
        }
    }

    fun onTap(id: Long) {
        if (!canPick(id)) {
            Toast.makeText(ctx, "هذا الفريق غير مشارك في هذه المرحلة", Toast.LENGTH_SHORT).show()
            return
        }
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

    val a = teams.firstOrNull { it.id == first }
    val b = teams.firstOrNull { it.id == second }
    val already = when {
        a == null || b == null -> false
        mode == "LEAGUE" -> league.any { samePair(it, a.id, b.id) }
        mode == "TIE" && res != null -> ties.any { it.round == res.tieRound && samePair(it, a.id, b.id) }
        else -> false
    }

    val finalistA = finalists?.getOrNull(0)?.let { id -> teams.firstOrNull { it.id == id } }
    val finalistB = finalists?.getOrNull(1)?.let { id -> teams.firstOrNull { it.id == id } }
    val showIntro = mode == "FINAL" && finals.isEmpty() && !introShown && finalistA != null && finalistB != null

    NeonBackground {
        if (champion != null && finalMatch != null) {
            Celebration(
                tournamentName = t.name,
                champ = champion,
                rows = rows,
                onBack = onHome,
                onUndo = { undoMatch = finalMatch },
                onFinish = { onFinish(champion.id) }
            )
        } else if (showIntro && finalistA != null && finalistB != null) {
            FinalShowdownIntro(
                a = finalistA,
                b = finalistB,
                onDismiss = { introShown = true }
            )
        } else {
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

                if (mode == "LEAGUE") {
                    Text(
                        "مرحلة الدوري • $done من $total",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                if (mode == "TIE" && res != null) {
                    val c = res.tieCandidates.size
                    val pairs = c * (c - 1) / 2
                    val played = ties.count { it.round == res.tieRound }
                    val qNames = teams.filter { res.qualified.contains(it.id) }.joinToString("، ") { it.name }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x66F59E0B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "مباراة فاصلة للتأهل إلى النهائي",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$c فرق تتنافس على ${res.tieSlots} مقعد • $played من $pairs",
                                color = Color(0xEEFFFFFF),
                                fontSize = 13.sp
                            )
                            if (qNames.isNotEmpty()) {
                                Text(
                                    "متأهل مؤقتًا: $qNames",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                if (mode == "FINAL") {
                    Text(
                        "🏆 النهائي",
                        color = Color(0xFFFBBF24),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

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
                    item(key = "header") {
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
                    items(rows, key = { "team_${it.team.id}" }) { row ->
                        val id = row.team.id
                        val sel = id == first || id == second
                        val rank = rows.indexOfFirst { it.wins == row.wins } + 1
                        val names = row.team.players.split("\n").filter { it.isNotBlank() }.joinToString("، ")
                        val status = when {
                            mode == "FINAL" && finalists != null && finalists.contains(id) -> "🏅 في النهائي"
                            mode == "TIE" && res != null && res.qualified.contains(id) -> "✅ متأهل"
                            mode == "TIE" && res != null && res.tieCandidates.contains(id) -> "⚔️ فاصلة"
                            else -> ""
                        }
                        val borderStroke: BorderStroke? = when {
                            sel -> BorderStroke(3.dp, NeonCyan)
                            status.startsWith("⚔") -> BorderStroke(2.dp, Color(0xFFF59E0B))
                            status.startsWith("🏅") -> BorderStroke(2.dp, Color(0xFFFBBF24))
                            else -> null
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (canPick(id)) 1f else 0.4f)
                                .clickable { onTap(id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sel) Color(0x668B5CF6) else CardBg
                            ),
                            border = borderStroke
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
                                    if (status.isNotEmpty()) {
                                        Text(status, color = Color(0xFFFBBF24), fontSize = 12.sp)
                                    }
                                }
                                StatCell("${row.played}")
                                StatCell("${row.wins}")
                                StatCell("${row.played - row.wins}")
                                StatCell("${row.wins}", NeonCyan, true, 18)
                            }
                        }
                    }

                    if (matches.isNotEmpty()) {
                        item(key = "results_title") {
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
                                    Text(label, color = Color(0xFFFBBF24), fontSize = 12.sp)
                                    TextButton(onClick = { undoMatch = m }) {
                                        Text("×", color = Color(0xFFF87171), fontSize = 26.sp)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "delete") {
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
                    val phase = when (mode) {
                        "TIE" -> "TIE"
                        "FINAL" -> "FINAL"
                        else -> "LEAGUE"
                    }
                    val round = if (mode == "TIE" && res != null) res.tieRound else 0
                    scope.launch {
                        db.tournamentDao().insertMatch(
                            MatchEntity(
                                tournamentId = t.id,
                                phase = phase,
                                round = round,
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
        val later = matches.filter { matchKey(it) > matchKey(um) }
        AlertDialog(
            onDismissRequest = { undoMatch = null },
            title = { Text("حذف هذه النتيجة؟") },
            text = {
                if (later.isNotEmpty()) {
                    Text("سيتم أيضًا حذف نتائج المراحل اللاحقة (الفاصلة/النهائي) لأنها تعتمد على هذه النتيجة.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    undoMatch = null
                    first = null
                    second = null
                    scope.launch {
                        later.forEach { db.tournamentDao().deleteMatch(it.id) }
                        db.tournamentDao().deleteMatch(um.id)
                    }
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
                    onDelete()
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            }
        )
    }
}