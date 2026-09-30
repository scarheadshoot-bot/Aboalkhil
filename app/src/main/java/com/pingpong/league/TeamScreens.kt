package com.pingpong.league

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = Color(0x88FFFFFF),
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = Color(0xCCFFFFFF),
    cursorColor = NeonCyan
)

@Composable
fun TeamsScreen(db: AppDatabase, onBack: () -> Unit, onEdit: (Long?) -> Unit) {
    val teams by db.teamDao().observeTeams().collectAsState(initial = emptyList())
    val players by db.teamDao().observePlayers().collectAsState(initial = emptyList())
    val byTeam = players.groupBy { it.teamId }

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
                    "الفرق",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (teams.isEmpty()) {
                Text(
                    "لا توجد فرق بعد",
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(team.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TeamLogo(team.logoPath, 56.dp)
                                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        team.name,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val names = byTeam[team.id]?.joinToString("، ") { it.name } ?: ""
                                    if (names.isNotEmpty()) {
                                        Text(names, color = Color(0xCCFFFFFF), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            NeonButton("+ فريق جديد", onClick = { onEdit(null) })
        }
    }
}

@Composable
fun TeamEditorScreen(db: AppDatabase, teamId: Long?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var logoPath by remember { mutableStateOf<String?>(null) }
    val players = remember { mutableStateListOf<String>() }
    var newPlayer by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        if (teamId != null) {
            val t = db.teamDao().getTeam(teamId)
            if (t != null) {
                name = t.name
                logoPath = t.logoPath
            }
            players.clear()
            players.addAll(db.teamDao().playersOf(teamId).map { it.name })
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val p = withContext(Dispatchers.IO) { saveLogo(ctx, uri) }
                if (p != null) logoPath = p
            }
        }
    }

    fun addPlayer() {
        val n = newPlayer.trim()
        if (n.isNotEmpty()) {
            players.add(n)
            newPlayer = ""
        }
    }

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDone) { Text("رجوع", color = NeonCyan) }
                Text(
                    if (teamId == null) "فريق جديد" else "تعديل الفريق",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.clickable {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamLogo(logoPath, 110.dp)
                Text(
                    "اضغط لاختيار الشعار",
                    color = Color(0xCCFFFFFF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم الفريق") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "اللاعبون",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPlayer,
                    onValueChange = { newPlayer = it },
                    label = { Text("اسم اللاعب") },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { addPlayer() }) {
                    Text("+", color = NeonCyan, fontSize = 32.sp)
                }
            }
            players.forEachIndexed { index, p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { players.removeAt(index) }) {
                            Text("−", color = Color(0xFFF87171), fontSize = 26.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            NeonButton("حفظ", onClick = {
                if (name.isBlank()) {
                    Toast.makeText(ctx, "اكتب اسم الفريق", Toast.LENGTH_SHORT).show()
                } else {
                    val list = players.toMutableList()
                    if (newPlayer.isNotBlank()) list.add(newPlayer.trim())
                    scope.launch {
                        db.teamDao().saveTeam(
                            TeamEntity(id = teamId ?: 0, name = name.trim(), logoPath = logoPath),
                            list
                        )
                        onDone()
                    }
                }
            })
            if (teamId != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("حذف الفريق", color = Color(0xFFF87171))
                }
            }
        }
    }

    if (confirmDelete && teamId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف الفريق؟") },
            text = { Text("لن تتأثر البطولات القديمة.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.teamDao().deleteTeam(teamId)
                        onDone()
                    }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            }
        )
    }
}