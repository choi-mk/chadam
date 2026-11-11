package com.example.pleosstarter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** A top-down vehicle marker for the relative-position picker. */
internal class CarMarker(private val bodyColor: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val checkpoint = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 48f, bounds.height() / 64f)

        paint.color = Color.rgb(9, 18, 29)
        for (y in listOf(12f, 43f)) {
            canvas.drawRoundRect(5f, y, 13f, y + 12f, 3f, 3f, paint)
            canvas.drawRoundRect(35f, y, 43f, y + 12f, 3f, 3f, paint)
        }
        paint.color = bodyColor
        canvas.drawRoundRect(10f, 3f, 38f, 61f, 10f, 10f, paint)
        paint.color = Color.rgb(29, 52, 69)
        canvas.drawRoundRect(14f, 16f, 34f, 29f, 4f, 4f, paint)
        canvas.drawRoundRect(15f, 41f, 33f, 49f, 3f, 3f, paint)
        paint.color = Color.rgb(240, 250, 255)
        canvas.drawRoundRect(13f, 8f, 20f, 12f, 2f, 2f, paint)
        canvas.drawRoundRect(28f, 8f, 35f, 12f, 2f, 2f, paint)
        paint.color = Color.rgb(251, 111, 120)
        canvas.drawRoundRect(13f, 53f, 20f, 57f, 2f, 2f, paint)
        canvas.drawRoundRect(28f, 53f, 35f, 57f, 2f, 2f, paint)
        canvas.restoreToCount(checkpoint)
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Required by Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
