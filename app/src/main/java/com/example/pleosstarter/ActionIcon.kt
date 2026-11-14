package com.example.pleosstarter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

internal class ActionIcon(private val symbol: String, color: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun draw(canvas: Canvas) {
        val saved = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        val path = Path()
        when (symbol) {
            "send" -> {
                path.moveTo(3f, 10f)
                path.lineTo(21f, 3f)
                path.lineTo(14f, 21f)
                path.lineTo(10f, 14f)
                path.close()
                canvas.drawLine(10f, 14f, 21f, 3f, paint)
            }
            "check" -> {
                path.moveTo(4f, 12f)
                path.lineTo(9f, 17f)
                path.lineTo(20f, 6f)
            }
            "close" -> {
                canvas.drawLine(6f, 6f, 18f, 18f, paint)
                canvas.drawLine(18f, 6f, 6f, 18f, paint)
            }
            "bell" -> {
                path.moveTo(4f, 17f)
                path.cubicTo(6f, 15f, 6f, 13f, 6f, 9f)
                path.cubicTo(6f, 1f, 18f, 1f, 18f, 9f)
                path.cubicTo(18f, 13f, 18f, 15f, 20f, 17f)
                path.close()
                canvas.drawArc(9f, 17f, 15f, 22f, 0f, 180f, false, paint)
            }
            "mic" -> {
                canvas.drawRoundRect(9f, 2f, 15f, 14f, 3f, 3f, paint)
                canvas.drawArc(5f, 5f, 19f, 18f, 0f, 180f, false, paint)
                canvas.drawLine(12f, 18f, 12f, 22f, paint)
            }
        }
        canvas.drawPath(path, paint)
        canvas.restoreToCount(saved)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Required by Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

internal class RoadSurface : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dash = DashPathEffect(floatArrayOf(14f, 18f), 0f)

    override fun draw(canvas: Canvas) {
        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()
        val right = bounds.right.toFloat()
        val bottom = bounds.bottom.toFloat()
        val width = bounds.width().toFloat()
        paint.pathEffect = null
        paint.color = Color.rgb(16, 29, 42)
        canvas.drawRoundRect(left, top, right, bottom, 24f, 24f, paint)

        paint.color = Color.rgb(53, 73, 91)
        paint.strokeWidth = 2f
        paint.pathEffect = dash
        for (fraction in listOf(1f / 3f, 2f / 3f)) {
            val x = left + width * fraction
            canvas.drawLine(x, top + 16f, x, bottom - 16f, paint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Required by Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
