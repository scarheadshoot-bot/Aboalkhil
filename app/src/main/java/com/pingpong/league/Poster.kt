package com.pingpong.league

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class PosterRow(val rank: Int, val name: String, val logoPath: String?, val points: Int)

fun buildPosterBitmap(
    tournamentName: String,
    championName: String,
    championLogoPath: String?,
    championPlayers: List<String>,
    rows: List<PosterRow>
): Bitmap {
    val w = 1080
    val h = 1920
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)

    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#1A0B2E"),
                Color.parseColor("#2E1065"),
                Color.parseColor("#0E3A5F")
            ),
            null, Shader.TileMode.CLAMP
        )
    }
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    val white = Color.WHITE
    val cyan = Color.parseColor("#22D3EE")
    val gold = Color.parseColor("#FBBF24")

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textSize = 56f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    c.drawText(tournamentName, w / 2f, 140f, titlePaint)

    val trophyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 220f
    }
    c.drawText("\uD83C\uDFC6", w / 2f, 420f, trophyPaint)

    val logoBmp = championLogoPath?.let { BitmapFactory.decodeFile(it) }
    val logoSize = 320
    val logoCx = w / 2f
    val logoCy = 620f
    if (logoBmp != null) {
        val circlePath = Path().apply {
            addCircle(logoCx, logoCy, logoSize / 2f, Path.Direction.CW)
        }
        c.save()
        c.clipPath(circlePath)
        val dstRect = RectF(
            logoCx - logoSize / 2f, logoCy - logoSize / 2f,
            logoCx + logoSize / 2f, logoCy + logoSize / 2f
        )
        c.drawBitmap(logoBmp, null, dstRect, null)
        c.restore()
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = cyan
        }
        c.drawCircle(logoCx, logoCy, logoSize / 2f, ringPaint)
    }

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textSize = 64f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    c.drawText(championName, w / 2f, 840f, namePaint)

    val playersPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEFFFFFF")
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    var py = 900f
    championPlayers.take(6).forEach {
        c.drawText(it, w / 2f, py, playersPaint)
        py += 44f
    }

    var ty = py + 60f
    val tableTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gold
        textSize = 44f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    c.drawText("الترتيب النهائي", w / 2f, ty, tableTitle)
    ty += 60f

    val rowNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textSize = 38f
        textAlign = Paint.Align.RIGHT
    }
    val rowPointsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cyan
        textSize = 38f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
    }
    val margin = 90f
    rows.take(8).forEach { row ->
        val top = ty
        val bottom = ty + 78f
        c.drawRoundRect(RectF(margin, top, w - margin, bottom), 20f, 20f, cardPaint)
        c.drawText("${row.name}  #${row.rank}", w - margin - 20f, top + 52f, rowNamePaint)
        c.drawText("${row.points}", margin + 30f, top + 52f, rowPointsPaint)
        ty = bottom + 16f
    }

    return bmp
}

fun savePosterToGallery(context: Context, bitmap: Bitmap): Boolean {
    val filename = "pingpong_${System.currentTimeMillis()}.png"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PingPongLeague")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "PingPongLeague"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            true
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun PosterButton(makeBitmap: () -> Bitmap) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    fun doSave() {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { savePosterToGallery(ctx, makeBitmap()) }
            Toast.makeText(
                ctx,
                if (ok) "تم الحفظ في المعرض" else "تعذر الحفظ",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            doSave()
        } else {
            Toast.makeText(ctx, "يلزم إذن الوصول للصور للحفظ", Toast.LENGTH_SHORT).show()
        }
    }

    NeonButton("حفظ كصورة", onClick = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) doSave() else permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doSave()
        }
    })
}