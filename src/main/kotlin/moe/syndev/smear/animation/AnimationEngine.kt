package moe.syndev.smear.animation

import moe.syndev.smear.settings.SmearCursorSettings
import moe.syndev.smear.util.MutableVector2
import kotlin.math.*

/**
 * Animation engine implementing spring physics for cursor movement.
 * This is the core animation system. It originally mirrored animation.lua from the <a href="https://github.com/sphamba/smear-cursor.nvim">neovim plugin</a>  
 * but was altered to perform better on the jvm
 */
class AnimationEngine {

    companion object {

        private const val BASE_TIME_INTERVAL = 17.0 // Base timing in milliseconds (60 FPS)

    }

    val frame = AnimationFrame(
        corners = Corners(),
        targetPosition = MutableVector2(),
        isAnimating = false,
        headIndex = 0,
        tailIndex = 0,
        gradientOrigin = MutableVector2(),
        gradientDirection = MutableVector2(),
    )

    // Animation state
    var animating = false
        private set
    
    private var previousTime = 0L

    // Cursor position tracking (in pixel coordinates)
    private val targetPosition = MutableVector2()

    // Quad corners: represents the smear shape
    // Corner indices: 0=top-left, 1=top-right, 2=bottom-right, 3=bottom-left
    private val currentCorners = Corners()
    private val targetCorners = Corners()
    private val velocityCorners = Corners()
    private val stiffnesses = doubleArrayOf(0.0, 0.0, 0.0, 0.0)

    // Cursor dimensions (in pixels)
    private var cursorWidth = 8.0
    private var cursorHeight = 16.0
    
    private val targetCenter: MutableVector2 = MutableVector2()


    /**
     * Initialized the animation engine with cursor dimensions
     * @param width cursor width
     * @param height cursor height
     */
    fun initialize(width: Double, height: Double) {
        cursorWidth = width
        cursorHeight = height
    }

    
    /**
     * Sets corners based on cursor's position and dimensions.
     * @param corners Corners
     * @param x Cursor's x position
     * @param y Cursor's y position
     */
    private fun setCorners(corners: Corners, x: Double, y: Double) {
        corners.topLeft.set(x, y)
        corners.topRight.set(x + cursorWidth, y)
        corners.bottomRight.set(x + cursorWidth, y + cursorHeight)
        corners.bottomLeft.set(x, y + cursorHeight)
    }

    private fun resetVelocity() {
        velocityCorners.reset()
    }

    /**
     * Set initial velocity based on anticipation (opposite to movement direction).
     */
    private fun setInitialVelocity() {
        val settings = SmearCursorSettings.getInstance()
        for (i in 0..3) {
            velocityCorners[i].x = (currentCorners[i].x - targetCorners[i].x) * settings.lag
            velocityCorners[i].y = (currentCorners[i].y - targetCorners[i].y) * settings.lag
        }
    }

    /**
     * Get the center point of a set of corners.
     */
    private fun getCenter(corners: Corners, out: MutableVector2) {
        out.set(
            (corners.topLeft.x + corners.topRight.x + corners.bottomRight.x + corners.bottomLeft.x) / 4.0,
            (corners.topLeft.y + corners.topRight.y + corners.bottomRight.y + corners.bottomLeft.y) / 4.0
        )
    }

    /**
     * Jump cursor immediately to new position without animation.
     */
    fun jump(x: Double, y: Double) {
        targetPosition.set(x, y)
        setCorners(targetCorners, x, y)
        setCorners(currentCorners, x, y)
        resetVelocity()
        animating = false
        previousTime = 0L
    }

    /**
     * Start animation towards a new target position.
     */
    fun animateTo(x: Double, y: Double) {
        val settings = SmearCursorSettings.getInstance()

        // Check minimum distances
        val currentX = currentCorners[0].x
        val currentY = currentCorners[0].y
        val dx = abs(x - currentX)
        val dy = abs(y - currentY)

        if (dy < settings.minVerticalDistanceSmear * cursorHeight &&
            dx < settings.minHorizontalDistanceSmear * cursorWidth
        ) {
            jump(x, y)
            return
        }

        // Check direction restrictions
        if (!settings.smearHorizontally && dy <= cursorHeight / 2) {
            jump(x, y)
            return
        }
        if (!settings.smearVertically && dx <= cursorWidth / 2) {
            jump(x, y)
            return
        }
        if (!settings.smearDiagonally && dy > cursorHeight / 2 && dx > cursorWidth / 2) {
            jump(x, y)
            return
        }

        targetPosition.set(x, y)
        setCorners(targetCorners, x, y)
        setHeadSpeed()

        if (!animating) {
            setInitialVelocity()
        }

        animating = true
    }

    /**
     * Calculate stiffness values for each corner based on distance from target.
     */
    private fun setHeadSpeed() {
        val settings = SmearCursorSettings.getInstance()
        getCenter(targetCorners, targetCenter)
        val distances = DoubleArray(4)
        var minDistance = Double.MAX_VALUE
        var maxDistance = 0.0

        val headStiffness = settings.headSpeed
        val trailingStiffness = settings.tailSpeed
        val trailingExponent = settings.trailingExponent

        for (i in 0..3) {
            val x = (currentCorners[i].x - targetCenter.x)
            val y = (currentCorners[i].y - targetCenter.y)
            val distance = sqrt(x * x + y * y)
            minDistance = min(minDistance, distance)
            maxDistance = max(maxDistance, distance)
            distances[i] = distance
        }

        if (maxDistance == minDistance) {
            for (i in 0..3) {
                stiffnesses[i] = headStiffness
            }
            return
        }

        for (i in 0..3) {
            val x = (distances[i] - minDistance) / (maxDistance - minDistance)
            val stiffness = headStiffness + (trailingStiffness - headStiffness) * x.pow(trailingExponent)
            stiffnesses[i] = min(1.0, stiffness)
        }
    }

    /**
     * Perform one animation update step.
     * @return Current animation frame data for rendering, null if not animating
     */
    fun update(settings: SmearCursorSettings): AnimationFrame? {
        if (!animating) return null
        
        val currentTime = System.nanoTime() / 1_000_000L

        val timeInterval = if (previousTime == 0L) {
            previousTime = currentTime
            BASE_TIME_INTERVAL
        } else {
            val elapsed = (currentTime - previousTime).toDouble()
            previousTime = currentTime
            if (elapsed < 1.0) BASE_TIME_INTERVAL else elapsed
        }

        // Calculate physics
        val speedCorrection = timeInterval / BASE_TIME_INTERVAL
        val damping = settings.damping
        val velocityConservationFactor = exp(ln(1.0 - damping) * speedCorrection)
        val dampingCorrectionFactor = 1.0 / (1.0 + 2.5 * velocityConservationFactor)

        var distanceHeadToTargetSquared = Double.MAX_VALUE
        var distanceTailToTargetSquared = 0.0
        var indexHead = 0
        var indexTail = 0

        // Update each corner
        for (i in 0..3) {
            val x = (currentCorners[i].x - targetCorners[i].x)
            val y = (currentCorners[i].y - targetCorners[i].y)
            val distanceSquared = x * x + y * y

            // fast path for 1.0 to avoid exp and ln calculation
            val stiffness = if (speedCorrection == 1.0) stiffnesses[i] * dampingCorrectionFactor
            else 1.0 - exp(ln(1.0 - stiffnesses[i] * dampingCorrectionFactor) * speedCorrection)

            if (distanceSquared < distanceHeadToTargetSquared) {
                distanceHeadToTargetSquared = distanceSquared
                indexHead = i
            }
            if (distanceSquared > distanceTailToTargetSquared) {
                distanceTailToTargetSquared = distanceSquared
                indexTail = i
            }

            velocityCorners[i].x += (targetCorners[i].x - currentCorners[i].x) * stiffness
            currentCorners[i].x += velocityCorners[i].x
            velocityCorners[i].x *= velocityConservationFactor

            velocityCorners[i].y += (targetCorners[i].y - currentCorners[i].y) * stiffness
            currentCorners[i].y += velocityCorners[i].y
            velocityCorners[i].y *= velocityConservationFactor
        }

        // Limit smear length
        var smearLength = 0.0
        for (i in 0..3) {
            if (i != indexHead) {
                val x = (currentCorners[i].x - currentCorners[indexHead].x)
                val y = (currentCorners[i].y - currentCorners[indexHead].y)
                val distance = sqrt(x * x + y * y)
                smearLength = max(smearLength, distance)
            }
        }

        val maxLength = settings.maxLength * cursorWidth
        if (smearLength > maxLength) {
            val factor = maxLength / smearLength
            for (i in 0..3) {
                if (i != indexHead) {
                    currentCorners[i].x = currentCorners[indexHead].x +
                            (currentCorners[i].x - currentCorners[indexHead].x) * factor

                    currentCorners[i].y = currentCorners[indexHead].y +
                            (currentCorners[i].y - currentCorners[indexHead].y) * factor
                }
            }
        }

        // Check if animation should stop
        var maxDistance = 0.0
        var maxVelocity = 0.0
        for (i in 0..3) {
            val distX = (currentCorners[i].x - targetCorners[i].x) 
            val distY = (currentCorners[i].y - targetCorners[i].y)
            val velX = velocityCorners[i].x
            val velY = velocityCorners[i].y
            
            val distance = sqrt(distX * distX + distY * distY)
            val velocity = sqrt(velX * velX + velY * velY)
            maxDistance = max(maxDistance, distance)
            maxVelocity = max(maxVelocity, velocity)
        }

        val stopThreshold = settings.distanceStopAnimating * cursorWidth
        if (maxDistance <= stopThreshold && maxVelocity <= stopThreshold) {
            setCorners(currentCorners, targetPosition.x, targetPosition.y)
            resetVelocity()
            stopAnimation()
        }

        // Calculate gradient direction
        frame.gradientOrigin.set(currentCorners[indexHead].x, currentCorners[indexHead].y)
        frame.gradientDirection.set(currentCorners[indexTail].x - currentCorners[indexHead].x, currentCorners[indexTail].y - currentCorners[indexHead].y)

        val gradientLengthSquared = frame.gradientDirection.x * frame.gradientDirection.x + frame.gradientDirection.y * frame.gradientDirection.y
        if (gradientLengthSquared > 1e-6) {
            val magnitude = sqrt(gradientLengthSquared)
            frame.gradientDirection.x /= magnitude
            frame.gradientDirection.y /= magnitude
        } else {
            frame.gradientDirection.x = 0.0
            frame.gradientDirection.y = 0.0
        }

        if (currentCorners[0].x.isNaN()) {
            //whoopsie!!
            stopAnimation()
            return null
        }

        for (i in 0..3) {
            frame.corners[i].set(currentCorners[i].x, currentCorners[i].y)
        }
        frame.targetPosition.set(targetPosition)
        frame.isAnimating = animating
        frame.headIndex = indexHead
        frame.tailIndex = indexTail

        return frame
    }

    /**
     * Stop the current animation immediately.
     */
    fun stopAnimation() {
        animating = false
        previousTime = 0L
    }
}
