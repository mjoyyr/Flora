package com.floralorder.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws softly falling, swaying and spinning flower petals. Used as the
 * animated background of the splash / loading screen.
 */
class PetalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Petal(
        var x: Float,
        var y: Float,
        val size: Float,
        val fallSpeed: Float,   // px per second
        val swayAmount: Float,  // px
        val swaySpeed: Float,   // radians per second
        var swayPhase: Float,
        var rotation: Float,
        val rotationSpeed: Float, // degrees per second
        val color: Int
    )

    private val petals = mutableListOf<Petal>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val petalPath = Path()
    private var lastFrameTime = 0L
    private var running = false

    private val palette = intArrayOf(
        0xCCFFFFFF.toInt(),
        0xB3FDEAF1.toInt(),
        0x99FFD6E5.toInt(),
        0xB3FFFFFF.toInt(),
        0x99F8C8DC.toInt()
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        petals.clear()
        if (w == 0 || h == 0) return
        val density = resources.displayMetrics.density
        repeat(22) {
            petals.add(newPetal(w, h, density, startAnywhere = true))
        }
    }

    private fun newPetal(w: Int, h: Int, density: Float, startAnywhere: Boolean): Petal {
        val size = (7 + Random.nextFloat() * 11) * density
        return Petal(
            x = Random.nextFloat() * w,
            y = if (startAnywhere) Random.nextFloat() * h else -size * 2,
            size = size,
            fallSpeed = (28 + Random.nextFloat() * 50) * density,
            swayAmount = (14 + Random.nextFloat() * 30) * density,
            swaySpeed = 0.8f + Random.nextFloat() * 1.4f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 140f,
            color = palette[Random.nextInt(palette.size)]
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        lastFrameTime = 0L
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.nanoTime()
        val dt = if (lastFrameTime == 0L) 0f else ((now - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameTime = now

        val density = resources.displayMetrics.density
        for (i in petals.indices) {
            val p = petals[i]
            p.y += p.fallSpeed * dt
            p.swayPhase += p.swaySpeed * dt
            p.rotation += p.rotationSpeed * dt

            if (p.y - p.size > height) {
                petals[i] = newPetal(width, height, density, startAnywhere = false)
                continue
            }

            val drawX = p.x + sin(p.swayPhase) * p.swayAmount
            drawPetal(canvas, drawX, p.y, p.size, p.rotation, p.color)
        }

        if (running) postInvalidateOnAnimation()
    }

    private fun drawPetal(canvas: Canvas, cx: Float, cy: Float, s: Float, rotation: Float, color: Int) {
        petalPath.reset()
        petalPath.moveTo(0f, -s)
        petalPath.quadTo(s * 0.95f, -s * 0.25f, 0f, s)
        petalPath.quadTo(-s * 0.95f, -s * 0.25f, 0f, -s)
        petalPath.close()

        paint.color = color
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotation)
        canvas.drawPath(petalPath, paint)
        canvas.restore()
    }
}
