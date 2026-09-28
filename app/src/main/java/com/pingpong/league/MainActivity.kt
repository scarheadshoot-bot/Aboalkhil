package com.pingpong.league

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface Screen {
    object Home : Screen
    object Teams : Screen
    data class TeamEditor(val teamId: Long?) : Screen
    object CreateTournament : Screen
    object Tournament : Screen
    object Winners : Screen
    data class Archive(val id: Long) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("crash", MODE_PRIVATE)
        val lastCrash = prefs.getString("trace", null)
        prefs.edit().remove("trace").commit()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                prefs.edit().putString("trace", Log.getStackTraceString(e)).commit()
            } catch (t: Throwable) {
            }
            previous?.uncaughtException(thread, e)
        }

        val db = AppDatabase.get(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    var crash by remember { mutableStateOf(lastCrash) }
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(
                            db,
                            onTeams = { screen = Screen.Teams },
                            onCreate = { screen = Screen.CreateTournament },
                            onContinue = { screen = Screen.Tournament },
                            onWinners = { screen = Screen.Winners }
                        )
                        Screen.Teams -> {
                            BackHandler { screen = Screen.Home }
                            TeamsScreen(
                                db,
                                onBack = { screen = Screen.Home },
                                onEdit = { screen = Screen.TeamEditor(it) }
                            )
                        }
                        is Screen.TeamEditor -> {
                            BackHandler { screen = Screen.Teams }
                            TeamEditorScreen(db, s.teamId, onDone = { screen = Screen.Teams })
                        }
                        Screen.CreateTournament -> {
                            BackHandler { screen = Screen.Home }
                            CreateTournamentScreen(
                                db,
                                onBack = { screen = Screen.Home },
                                onStarted = { screen = Screen.Tournament }
                            )
                        }
                        Screen.Tournament -> {
                            BackHandler { screen = Screen.Home }
                            TournamentScreen(db, onHome = { screen = Screen.Home })
                        }
                        Screen.Winners -> {
                            BackHandler { screen = Screen.Home }
                            WinnersScreen(
                                db,
                                onBack = { screen = Screen.Home },
                                onOpen = { screen = Screen.Archive(it) }
                            )
                        }
                        is Screen.Archive -> {
                            BackHandler { screen = Screen.Winners }
                            ArchiveDetailScreen(db, s.id, onBack = { screen = Screen.Winners })
                        }
                    }
                    val c = crash
                    if (c != null) {
                        AlertDialog(
                            onDismissRequest = { crash = null },
                            title = { Text("تقرير الخطأ") },
                            text = {
                                Text(
                                    c.take(2500),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .heightIn(max = 400.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { crash = null }) { Text("إغلاق") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    db: AppDatabase,
    onTeams: () -> Unit,
    onCreate: () -> Unit,
    onContinue: () -> Unit,
    onWinners: () -> Unit
) {
    val ctx = LocalContext.current
    val count by db.teamDao().observeCount().collectAsState(initial = 0)
    val active by db.tournamentDao().observeActive().collectAsState(initial = null)

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "بطولة بينغ بونغ",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(40.dp))
            if (active != null) {
                NeonButton("متابعة", onClick = onContinue)
                Spacer(Modifier.height(14.dp))
            }
            NeonButton("إنشاء بطولة", onClick = {
                if (active != null) {
                    Toast.makeText(ctx, "توجد بطولة قيد التنفيذ، اضغط متابعة", Toast.LENGTH_SHORT).show()
                } else if (count < 2) {
                    Toast.makeText(ctx, "أنشئ فريقين على الأقل أولًا", Toast.LENGTH_SHORT).show()
                } else {
                    onCreate()
                }
            })
            Spacer(Modifier.height(14.dp))
            NeonButton("إنشاء فريق  $count", onClick = onTeams)
            Spacer(Modifier.height(14.dp))
            NeonButton("سجل الفائزين", onClick = onWinners)
        }
    }
}