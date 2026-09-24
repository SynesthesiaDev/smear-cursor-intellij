package moe.syndev.smear.render

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import moe.syndev.smear.animation.AnimationFrame
import moe.syndev.smear.settings.SmearCursorSettings
import moe.syndev.smear.util.ColorUtils
import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D

/**
 * Renderer for the smear cursor effect.
 * Draws the animated cursor trail and particles on the editor.
 */
class SmearCursorRenderer {

    private var cachedGradient: List<Color>? = null
    private var lastCursorColor: Color? = null
    private var lastBackgroundColor: Color? = null
    
    // Cache stroke for performance
    private val outlineStroke = BasicStroke(1.0f)

    /**
     * Render the animation frame to the graphics context.
     */
    fun render(
        g: Graphics2D,
        frame: AnimationFrame,
        editor: Editor
    ) {
        val settings = SmearCursorSettings.getInstance()

        // Get colors
        val backgroundColor = getBackgroundColor(editor)
        val cursorColor = getCursorColor(editor)

        // Update gradient cache if colors changed
        if (cursorColor != lastCursorColor || backgroundColor != lastBackgroundColor) {
            cachedGradient = ColorUtils.generateGradient(
                backgroundColor,
                cursorColor,
                settings.colorLevels,
                settings.gamma
            )
            lastCursorColor = cursorColor
            lastBackgroundColor = backgroundColor
        }

        // Draw the smear quad with gradient
        drawSmearQuad(g, frame, cursorColor)

    }

    /**
     * Draw the main smear quad shape with gradient coloring.
     */
    private fun drawSmearQuad(
        g: Graphics2D,
        frame: AnimationFrame,
        cursorColor: Color
    ) {
        val corners = frame.corners

        // Create the quad path
        val path = GeneralPath(Path2D.WIND_EVEN_ODD)
        path.moveTo(corners[0].x, corners[0].y)
        path.lineTo(corners[1].x, corners[1].y)
        path.lineTo(corners[2].x, corners[2].y)
        path.lineTo(corners[3].x, corners[3].y)
        path.closePath()

        // Calculate gradient paint
        val headCorner = corners[frame.headIndex]
        val tailCorner = corners[frame.tailIndex]

        val gradient = GradientPaint(
            tailCorner.x.toFloat(), tailCorner.y.toFloat(), 
            ColorUtils.withAlpha(cursorColor, 0.3),
            headCorner.x.toFloat(), headCorner.y.toFloat(), 
            cursorColor
        )

        // Fill with gradient
        val originalPaint = g.paint
        val originalColor = g.color
        val originalStroke = g.stroke
        try {
            g.paint = gradient
            g.fill(path)

            g.color = ColorUtils.withAlpha(cursorColor, 0.8)
            g.stroke = outlineStroke
            g.draw(path)
        } finally {
            g.paint = originalPaint
            g.color = originalColor
            g.stroke = originalStroke
        }
    }

    /**
     * Get the editor's background color.
     */
    private fun getBackgroundColor(editor: Editor): Color {
        return editor.colorsScheme.defaultBackground
    }

    /**
     * Get the cursor color based on settings.
     */
    private fun getCursorColor(editor: Editor): Color {
        val settings = SmearCursorSettings.getInstance()
        return if (settings.useEditorCursorColor) {
            editor.colorsScheme.getColor(EditorColors.CARET_COLOR)
                ?: settings.getCursorColor()
        } else {
            settings.getCursorColor()
        }
    }

    /**
     * Clear the cached gradient (call when colors change).
     */
    fun clearCache() {
        cachedGradient = null
        lastCursorColor = null
        lastBackgroundColor = null
    }
}
