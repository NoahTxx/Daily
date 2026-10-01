package com.example.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object WidgetBitmapUtils {
    fun createColoredCircleBitmap(sizePx: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
        return bitmap
    }

    fun createRingBitmap(sizePx: Int, color: Int, strokeWidthPx: Float = 3f): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidthPx
        }
        val radius = (sizePx - strokeWidthPx) / 2f
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, paint)
        return bitmap
    }

    fun createCircularProgressBitmap(
        sizePx: Int,
        progress: Float,
        strokeWidthPx: Float,
        accentColor: Int,
        trackColor: Int = 0xFF26262B.toInt()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val halfStroke = strokeWidthPx / 2f
        val rect = RectF(halfStroke, halfStroke, sizePx - halfStroke, sizePx - halfStroke)

        // Background track circle
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
        }
        canvas.drawOval(rect, trackPaint)

        // Progress arc
        if (progress > 0.005f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = if (progress >= 0.999f) Paint.Cap.BUTT else Paint.Cap.ROUND
            }
            val sweepAngle = (progress.coerceIn(0f, 1f) * 360f)
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
        }

        return bitmap
    }

    fun createProgressBarBitmap(
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        accentColor: Int,
        trackColor: Int = 0xFF26262B.toInt()
    ): Bitmap {
        val safeW = widthPx.coerceAtLeast(10)
        val safeH = heightPx.coerceAtLeast(4)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cornerRadius = safeH / 2f

        // Track background
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.FILL
        }
        val trackRect = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        // Filled progress portion
        if (progress > 0.01f) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            val fillW = (safeW.toFloat() * progress.coerceIn(0f, 1f)).coerceAtLeast(cornerRadius * 2)
            val fillRect = RectF(0f, 0f, fillW, safeH.toFloat())
            canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)
        }

        return bitmap
    }
}
