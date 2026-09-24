package moe.syndev.smear.render

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import moe.syndev.smear.animation.AnimationFrame
import moe.syndev.smear.settings.SmearCursorSettings
import moe.syndev.smear.util.ColorUtils
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

    private val quadPath = GeneralPath(Path2D.WIND_EVEN_ODD)

    private var cachedTailColor: Color? = null
    private var cachedOutlineColor: Color? = null
    private var cachedForCursorColor: Color? = null

    fun render(g: Graphics2D, frame: AnimationFrame, editor: Editor) {
        val cursorColor = getCursorColor(editor)
        updateColorCache(cursorColor)
        drawSmearQuad(g, frame, cursorColor)
    }

    private fun updateColorCache(cursorColor: Color) {
        if (cursorColor == cachedForCursorColor) return
        cachedTailColor = ColorUtils.withAlpha(cursorColor, 0.3)
        cachedOutlineColor = ColorUtils.withAlpha(cursorColor, 0.8)
        cachedForCursorColor = cursorColor
    }

    private fun drawSmearQuad(g: Graphics2D, frame: AnimationFrame, cursorColor: Color) {
        val corners = frame.corners

        quadPath.reset()
        quadPath.moveTo(corners[0].x, corners[0].y)
        quadPath.lineTo(corners[1].x, corners[1].y)
        quadPath.lineTo(corners[2].x, corners[2].y)
        quadPath.lineTo(corners[3].x, corners[3].y)
        quadPath.closePath()

        val head = corners[frame.headIndex]
        val tail = corners[frame.tailIndex]

        val gradient = GradientPaint(
            tail.x.toFloat(), tail.y.toFloat(), cachedTailColor!!,
            head.x.toFloat(), head.y.toFloat(), cursorColor,
        )

        val originalPaint = g.paint

        try {
            g.paint = gradient
            g.fill(quadPath)
        } finally {
            g.paint = originalPaint
        }
    }

    private fun getCursorColor(editor: Editor): Color {
        val settings = SmearCursorSettings.getInstance()
        return if (settings.useEditorCursorColor) {
            editor.colorsScheme.getColor(EditorColors.CARET_COLOR) ?: settings.getCursorColor()
        } else {
            settings.getCursorColor()
        }
    }
}
