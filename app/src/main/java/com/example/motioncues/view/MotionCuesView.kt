package com.example.motioncues.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Choreographer
import android.view.View
import com.example.motioncues.prefs.DotCount
import com.example.motioncues.prefs.DotPattern
import com.example.motioncues.prefs.PrefsManager
import com.example.motioncues.sensor.MotionVector
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MotionCuesView(context: Context) : View(context), Choreographer.FrameCallback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dotRadius = 5f
    private var dotColor = 0xFF4A90D9.toInt()
    private var pattern = DotPattern.REGULAR
    private var maxVisibleDots = 40
    private var targetDotCount = 40

    private var isAnimating = false
    private var isFrameCallbackPosted = false
    private var lastFrameTime = 0L

    private val choreographer = Choreographer.getInstance()
    private var prefs: PrefsManager? = null

    private var screenWidth = 0f
    private var screenHeight = 0f

    private var smoothLateralInput = 0f
    private var committedDirection = 1f

    private var timeAccumulator = 0f
    private var spawnAccumulator = 0f
    private var initialBuildUpPhase = true
    private var buildUpTimer = 0f

    private val dots = mutableListOf<TraversingDot>()

    private val edgeInsetY = 12f

    private val twoPi = (2f * PI).toFloat()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setPrefsManager(prefsManager: PrefsManager) {
        prefs = prefsManager
        updateAppearance()
    }

    fun updateAppearance() {
        prefs?.let { p ->
            pattern = p.pattern
            dotRadius = p.dotSize
            dotColor = if (p.getSelectedColor().androidColor != 0) {
                p.getSelectedColor().androidColor
            } else {
                val isNight = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNight) 0xFF888888.toInt() else 0xFF666666.toInt()
            }
            maxVisibleDots = when (p.dotCount) {
                DotCount.NORMAL -> 80
                DotCount.MORE -> 120
            }
            targetDotCount = maxVisibleDots
        }
    }

    fun updateMotion(motion: MotionVector) {
        val smoothed = smoothLateralInput
        val target = motion.lateral.coerceIn(-5f, 5f) / 5f
        smoothLateralInput += (target - smoothed) * 0.06f
    }

    fun setAnimating(animating: Boolean) {
        isAnimating = animating
        if (animating) {
            lastFrameTime = 0L
            spawnAccumulator = 0f
            timeAccumulator = 0f
            smoothLateralInput = 0f
            committedDirection = 1f
            targetDotCount = maxVisibleDots
            dots.clear()
            initialBuildUpPhase = true
            buildUpTimer = 0f
            spawnAccumulator = 0f
            postFrameCallback()
        } else {
            removeFrameCallback()
            dots.clear()
            initialBuildUpPhase = false
        }
    }

    private fun postFrameCallback() {
        if (!isFrameCallbackPosted) {
            isFrameCallbackPosted = true
            choreographer.postFrameCallback(this)
        }
    }

    private fun removeFrameCallback() {
        isFrameCallbackPosted = false
        choreographer.removeFrameCallback(this)
    }

    private fun spawnDot(xProgress: Float, y: Float) {
        val dot = TraversingDot(
            x = xProgress * screenWidth,
            y = y,
            xProgress = xProgress,
            wanderAngle = Random.nextFloat() * twoPi,
            wanderRate = 0.5f + Random.nextFloat() * 1.5f,
            wanderPhase = Random.nextFloat() * twoPi,
            wanderAmplitude = 0.8f + Random.nextFloat() * 1.5f,
            speedVariation = 0.85f + Random.nextFloat() * 0.3f
        )
        dots.add(dot)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAnimating) {
            isFrameCallbackPosted = false
            return
        }

        val dt = if (lastFrameTime == 0L) {
            0.016f
        } else {
            ((frameTimeNanos - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
        }
        lastFrameTime = frameTimeNanos

        timeAccumulator += dt
        if (timeAccumulator > 1000f) {
            timeAccumulator -= 1000f
        }

        updateDots(dt)

        postInvalidateOnAnimation()
        choreographer.postFrameCallback(this)
    }

    private fun updateDots(dt: Float) {
        if (screenWidth <= 0f || screenHeight <= 0f) return

        val deadZone = 0.15f
        val absInput = abs(smoothLateralInput)

        if (absInput > deadZone) {
            committedDirection = if (smoothLateralInput > 0f) 1f else -1f
        }

        val baseSpeed = 200f
        val direction = committedDirection
        val motionStrength = absInput.coerceIn(deadZone, 1f)
        val speed = baseSpeed * (0.6f + motionStrength * 0.4f)

        for (dot in dots) {
            dot.xProgress += (speed * direction * dot.speedVariation / screenWidth) * dt
            dot.wanderAngle += dot.wanderRate * dt
            if (dot.wanderAngle >= twoPi) dot.wanderAngle -= twoPi
            dot.wanderPhase += dot.wanderRate * 0.7f * dt
            if (dot.wanderPhase >= twoPi) dot.wanderPhase -= twoPi
            dot.x = dot.xProgress * screenWidth
        }

        dots.removeAll { it.xProgress < -0.2f || it.xProgress > 1.2f }

        if (initialBuildUpPhase) {
            buildUpTimer += dt
            val buildUpDuration = 1.5f
            val buildUpProgress = (buildUpTimer / buildUpDuration).coerceAtMost(1f)
            val currentTarget = (buildUpProgress * targetDotCount).toInt()

            val spawnRate = if (buildUpProgress < 0.3f) 0.012f else 0.025f
            spawnAccumulator += dt

            while (spawnAccumulator >= spawnRate && dots.size < currentTarget) {
                spawnAccumulator -= spawnRate
                val usableHeight = screenHeight - 2f * edgeInsetY
                val y = edgeInsetY + Random.nextFloat() * usableHeight
                val xProgress = Random.nextFloat()
                spawnDot(xProgress, y)
            }

            if (buildUpProgress >= 1f && dots.size >= targetDotCount) {
                initialBuildUpPhase = false
                spawnAccumulator = 0f
            }
        } else {
            val spawnInterval = 0.06f
            spawnAccumulator += dt

            while (spawnAccumulator >= spawnInterval && dots.size < targetDotCount) {
                spawnAccumulator -= spawnInterval

                val usableHeight = screenHeight - 2f * edgeInsetY
                val y = edgeInsetY + Random.nextFloat() * usableHeight
                val xProgress = if (committedDirection > 0f) -0.05f else 1.05f
                spawnDot(xProgress, y)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isAnimating) {
            if (dots.isEmpty() && screenHeight > 0f) {
                initialBuildUpPhase = true
                buildUpTimer = 0f
                spawnAccumulator = 0f
            }
            postFrameCallback()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeFrameCallback()
        dots.clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        paint.color = dotColor

        for (dot in dots) {
            val alpha = computeAlpha(dot.xProgress)
            if (alpha <= 0f) continue

            val wanderY = sin(dot.wanderAngle) * dot.wanderAmplitude
            val wanderX = cos(dot.wanderPhase) * dot.wanderAmplitude * 0.4f

            val sizeMod = if (pattern == DotPattern.DYNAMIC) {
                0.85f + 0.15f * sin(timeAccumulator * 2.5f + dot.wanderAngle).toFloat()
            } else {
                1f
            }

            paint.alpha = (255f * alpha).toInt()

            canvas.drawCircle(
                dot.x + wanderX,
                dot.y + wanderY,
                dotRadius * sizeMod,
                paint
            )
        }
    }

    private fun computeAlpha(progress: Float): Float {
        val edgeZone = 0.25f
        val fadeRange = 0.15f

        val distanceToLeft = progress
        val distanceToRight = 1f - progress
        val nearestEdge = minOf(distanceToLeft, distanceToRight)

        return when {
            nearestEdge <= edgeZone -> 1f
            nearestEdge <= edgeZone + fadeRange -> {
                1f - ((nearestEdge - edgeZone) / fadeRange)
            }
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    private class TraversingDot(
        var x: Float,
        var y: Float,
        var xProgress: Float,
        var wanderAngle: Float,
        var wanderRate: Float,
        var wanderPhase: Float,
        var wanderAmplitude: Float,
        var speedVariation: Float = 1f
    )
}
