package com.pingpong.league

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(db, onTeams = { screen = Screen.Teams })
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
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(db: AppDatabase, onTeams: () -> Unit) {
    val ctx = LocalContext.current
    val count by db.teamDao().observeCount().collectAsState(initial = 0)
    val soon = { Toast.makeText(ctx, "قريبًا", Toast.LENGTH_SHORT).show() }

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
            NeonButton("إنشاء بطولة", onClick = soon)
            Spacer(Modifier.height(14.dp))
            NeonButton("إنشاء فريق  $count", onClick = onTeams)
            Spacer(Modifier.height(14.dp))
            NeonButton("سجل الفائزين", onClick = soon)
        }
    }
}