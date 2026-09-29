package com.pingpong.league

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FinalShowdownIntro(a: TournamentTeamEntity, b: TournamentTeamEntity, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var clashed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        started = true
        delay(650)
        clashed = true
        delay(2200)
        onDismiss()
    }

    val slideA by animateDpAsState(
        targetValue = if (started) 0.dp else (-260).dp,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "slideA"
    )
    val slideB by animateDpAsState(
        targetValue = if (started) 0.dp else 260.dp,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "slideB"
    )
    val vsScale by animateFloatAsState(
        targetValue = if (clashed) 1f else 0.01f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "vsScale"
    )
    val inf = rememberInfiniteTransition(label = "bolt")
    val boltAlpha by inf.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "boltAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF3B0764), Color(0xFF0B0620))))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "⚡",
            fontSize = 40.sp,
            color = Color(0xFFFBBF24),
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp).alpha(boltAlpha)
        )
        Text(
            "⚡",
            fontSize = 40.sp,
            color = Color(0xFFFBBF24),
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).alpha(boltAlpha)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("النهائي", color = Color(0xFFFBBF24), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = slideA)
                ) {
                    TeamLogo(a.logoPath, 120.dp)
                    Text(a.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.padding(horizontal = 16.dp).scale(vsScale)) {
                    Text("VS", color = Color(0xFFF87171), fontSize = 46.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = slideB)
                ) {
                    TeamLogo(b.logoPath, 120.dp)
                    Text(b.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("اضغط للمتابعة", color = Color(0xCCFFFFFF), fontSize = 13.sp, modifier = Modifier.alpha(boltAlpha))
        }
    }
}