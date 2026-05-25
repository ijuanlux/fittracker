package com.juan.fittracker.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PdfReporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    private val palette = listOf(
        "#FCD116", "#003893", "#CE1126", "#D9A86C",
        "#7BD389", "#FFC58A", "#8B5A2B", "#2E1F14",
    )

    suspend fun generate(context: Context, stats: WorkoutStats): Uri =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            drawBackground(canvas)
            drawHeader(canvas, stats)
            drawStatsGrid(canvas, stats)
            drawPieChart(canvas, stats)
            drawTopRoutines(canvas, stats)
            drawFooter(canvas)

            doc.finishPage(page)

            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "reports",
            ).apply { mkdirs() }
            val rangeSlug = stats.rangeLabel.replace(Regex("[^a-zA-Z0-9]"), "_")
            val file = File(dir, "galleto-informe-${rangeSlug}-${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }

    private fun drawBackground(canvas: Canvas) {
        // soft cream background
        val bg = Paint().apply { color = Color.parseColor("#FFF8E7") }
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), bg)
    }

    private fun drawHeader(canvas: Canvas, stats: WorkoutStats) {
        // Brand strip on left
        val accent = Paint().apply { color = Color.parseColor("#FFC58A") }
        canvas.drawRect(0f, 0f, 8f, PAGE_H.toFloat(), accent)
        // Colombian flag mini stripe
        val yellow = Paint().apply { color = Color.parseColor("#FCD116") }
        val blue = Paint().apply { color = Color.parseColor("#003893") }
        val red = Paint().apply { color = Color.parseColor("#CE1126") }
        canvas.drawRect(8f, 0f, 16f, PAGE_H * 0.5f, yellow)
        canvas.drawRect(8f, PAGE_H * 0.5f, 16f, PAGE_H * 0.75f, blue)
        canvas.drawRect(8f, PAGE_H * 0.75f, 16f, PAGE_H.toFloat(), red)

        val title = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#15100B")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Galleto informe", MARGIN, 60f, title)

        val subtitle = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8B5A2B")
            textSize = 14f
        }
        canvas.drawText("Galleta FitTracker · ${stats.rangeLabel}", MARGIN, 84f, subtitle)

        val sep = Paint().apply { color = Color.parseColor("#FFC58A"); strokeWidth = 2f }
        canvas.drawLine(MARGIN, 100f, PAGE_W - MARGIN, 100f, sep)
    }

    private fun drawStatsGrid(canvas: Canvas, stats: WorkoutStats) {
        val boxes = listOf(
            "ENTRENOS" to stats.totalWorkouts.toString(),
            "SERIES" to stats.totalSets.toString(),
            "REPS" to stats.totalReps.toString(),
            "VOLUMEN" to "${stats.totalVolumeKg.toInt()} kg",
            "DÍAS ACTIVOS" to stats.totalDays.toString(),
            "RACHA" to "${stats.currentStreak} días",
        )
        val cellW = (PAGE_W - 2 * MARGIN - 20f) / 3f
        val cellH = 70f
        val startX = MARGIN
        val startY = 120f
        boxes.forEachIndexed { i, (label, value) ->
            val col = i % 3
            val row = i / 3
            val x = startX + col * (cellW + 10f)
            val y = startY + row * (cellH + 10f)
            val bg = Paint().apply { color = Color.parseColor("#FAF1E0"); isAntiAlias = true }
            canvas.drawRoundRect(x, y, x + cellW, y + cellH, 12f, 12f, bg)
            val border = Paint().apply {
                color = Color.parseColor("#FFC58A")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(x, y, x + cellW, y + cellH, 12f, 12f, border)

            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#8B5A2B")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, x + 12f, y + 20f, labelPaint)

            val valuePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#2E1F14")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(value, x + 12f, y + 52f, valuePaint)
        }
    }

    private fun drawPieChart(canvas: Canvas, stats: WorkoutStats) {
        val sectionY = 290f
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2E1F14")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Quesito de ejercicios top", MARGIN, sectionY, titlePaint)

        if (stats.topExercises.isEmpty()) {
            val noData = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#8B6F47")
                textSize = 12f
            }
            canvas.drawText("Sin datos en este rango", MARGIN, sectionY + 24f, noData)
            return
        }

        val cx = MARGIN + 110f
        val cy = sectionY + 130f
        val r = 90f
        val total = stats.topExercises.sumOf { it.second }.toFloat()
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        var start = -90f
        stats.topExercises.forEachIndexed { i, (_, count) ->
            val sweep = (count / total) * 360f
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor(palette[i % palette.size])
                style = Paint.Style.FILL
            }
            canvas.drawArc(rect, start, sweep, true, paint)
            // separator stroke
            val sep = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#FFF8E7")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawArc(rect, start, sweep, true, sep)
            start += sweep
        }
        // inner circle (donut)
        val innerR = r * 0.42f
        val inner = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFF8E7")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, innerR, inner)

        // Legend
        val legendX = cx + r + 40f
        var legendY = cy - r + 16f
        stats.topExercises.forEachIndexed { i, (name, count) ->
            val swatch = Paint().apply { color = Color.parseColor(palette[i % palette.size]); isAntiAlias = true }
            canvas.drawRoundRect(legendX, legendY - 10f, legendX + 16f, legendY + 4f, 3f, 3f, swatch)
            val txt = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#2E1F14")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val pct = ((count / total) * 100f).toInt()
            canvas.drawText("$name", legendX + 24f, legendY, txt)
            val small = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#8B6F47")
                textSize = 11f
            }
            canvas.drawText("$count series · $pct%", legendX + 24f, legendY + 14f, small)
            legendY += 36f
        }
    }

    private fun drawTopRoutines(canvas: Canvas, stats: WorkoutStats) {
        val sectionY = 580f
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2E1F14")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Rutinas top", MARGIN, sectionY, titlePaint)

        if (stats.topRoutines.isEmpty()) {
            val noData = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#8B6F47")
                textSize = 12f
            }
            canvas.drawText("Sin rutinas guiadas en este rango", MARGIN, sectionY + 24f, noData)
            return
        }
        val max = stats.topRoutines.maxOf { it.second }.toFloat()
        val barAreaW = PAGE_W - 2 * MARGIN
        val barH = 22f
        val gap = 8f
        var y = sectionY + 20f
        stats.topRoutines.forEach { (name, count) ->
            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#2E1F14")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(name, MARGIN, y, labelPaint)
            val countPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#8B5A2B")
                textSize = 12f
            }
            canvas.drawText("× $count", PAGE_W - MARGIN - 40f, y, countPaint)
            y += 6f
            val track = Paint().apply { color = Color.parseColor("#EAD6B0"); isAntiAlias = true }
            canvas.drawRoundRect(MARGIN, y, MARGIN + barAreaW, y + barH, 11f, 11f, track)
            val fillW = (count / max) * barAreaW
            val fill = Paint().apply { color = Color.parseColor("#FFC58A"); isAntiAlias = true }
            canvas.drawRoundRect(MARGIN, y, MARGIN + fillW, y + barH, 11f, 11f, fill)
            y += barH + gap + 14f
        }
    }

    private fun drawFooter(canvas: Canvas) {
        val date = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy 'a las' HH:mm", Locale("es", "ES")),
        )
        val footer = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8B6F47")
            textSize = 10f
        }
        canvas.drawText("Generado el $date · Galleta FitTracker", MARGIN, 815f, footer)
        canvas.drawText("🍪", PAGE_W - MARGIN - 16f, 815f, footer)
    }
}
