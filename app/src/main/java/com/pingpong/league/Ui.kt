package com.pingpong.league

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

val NeonPurple = Color(0xFF8B5CF6)
val NeonCyan = Color(0xFF22D3EE)
val CardBg = Color(0x44FFFFFF)

@Composable
fun NeonBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0B2E), Color(0xFF2E1065), Color(0xFF0E3A5F))
                )
            ),
        content = content
    )
}

@Composable
fun TeamLogo(path: String?, size: Dp) {
    val bmp = remember(path) {
        path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(2.dp, NeonCyan, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("🏓", fontSize = (size.value / 2).sp)
        }
    }
}

@Composable
fun NeonButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonPurple,
            contentColor = Color.White
        )
    ) {
        Text(text, fontSize = 18.sp)
    }
}

fun saveLogo(context: Context, uri: Uri): String? {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val src = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        val min = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - min) / 2, (src.height - min) / 2, min, min)
        val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
        val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.png")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.absolutePath
    } catch (e: Exception) {
        return null
    }
}