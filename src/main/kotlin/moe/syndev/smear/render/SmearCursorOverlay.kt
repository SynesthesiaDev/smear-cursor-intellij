package moe.syndev.smear.render

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import moe.syndev.smear.animation.AnimationEngine
import moe.syndev.smear.settings.SmearCursorSettings
import moe.syndev.smear.util.MutableVector2
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.abs

/**
 * Overlay component that renders the smear cursor effect on top of the editor.
 * This component is added to the editor's layered pane and handles animation timing.
 */
class SmearCursorOverlay(private val editor: Editor) : JComponent(), CaretListener, DocumentListener, Disposable {

    private val animationEngine = AnimationEngine()
    private val renderer = SmearCursorRenderer()
    private var animationTimer: Timer? = null
    private val lastCaretPosition: MutableVector2 = MutableVector2()
    private var hasLastCaretPosition = false

    // Cursor dimensions
    private var cursorWidth = 2.0
    private var cursorHeight = 16.0
    
    val settings = SmearCursorSettings.getInstance()

    // Tracks whether the caret moved due to a document change (typing/deletion)
    private var documentJustChanged = false

    init {
        isOpaque = false
        
        // Make this component completely mouse-transparent
        // This allows clicks to pass through to the editor below
        isFocusable = false
        
        // Initialize cursor dimensions from editor
        updateCursorDimensions()
        
        // Initialize animation engine
        val caretPos = MutableVector2()
        if (getCaretScreenPosition(caretPos)) {
            animationEngine.initialize(cursorWidth, cursorHeight)
            animationEngine.jump(caretPos.x, caretPos.y)
            lastCaretPosition.set(caretPos)
            hasLastCaretPosition = true
        }

        // Add caret listener and document listener
        editor.caretModel.addCaretListener(this, this)
        editor.document.addDocumentListener(this, this)

        EditorUtil.disposeWithEditor(editor, this)

        // Create animation timer with coalescing for better performance
        animationTimer = Timer(SmearCursorSettings.getInstance().timeInterval) {
            if (isEnabled && animationEngine.animating) {
                repaint()
            }
        }
        animationTimer?.isRepeats = true
        animationTimer?.isCoalesce = true // Coalesce multiple pending events
    }

    /**
     * Update cursor dimensions based on editor font metrics.
     */
    private fun updateCursorDimensions() {
        val fontMetrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(null))
        cursorHeight = fontMetrics.height.toDouble()
        cursorWidth = maxOf(2.0, fontMetrics.charWidth('M').toDouble() / 4.0)
        animationEngine.initialize(cursorWidth, cursorHeight)
    }

    /**
     * Get the current caret position relative to the overlay's coordinate system.
     * In IntelliJ 2025+, visualPositionToXY returns viewport-relative coordinates,
     * so we don't need to subtract the scroll offset.
     */
    private fun getCaretScreenPosition(out: MutableVector2): Boolean {
        try {
            if (!editor.contentComponent.isShowing || !this.isShowing) return false

            val caret = editor.caretModel.currentCaret
            val point = editor.visualPositionToXY(caret.visualPosition)

            if (point.y !in 0..height || point.x < 0 || point.x > width) return false

            out.set(point.x.toDouble(), point.y.toDouble())
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // DocumentListener: detect when text is being typed/deleted
    override fun documentChanged(event: DocumentEvent) {
        documentJustChanged = true
    }

    override fun caretPositionChanged(event: CaretEvent) {
        if (!isEnabled) return

        if (!settings.enabled) return

        // Capture and reset the typing flag
        val isTypingChange = documentJustChanged
        documentJustChanged = false

        SwingUtilities.invokeLater {
            val newPosition = MutableVector2()
            if(!getCaretScreenPosition(newPosition)) return@invokeLater
            val oldPosition = lastCaretPosition

            // If smear-while-typing is disabled and this caret move was caused by a document change, skip animation
            val suppressForTyping = isTypingChange && !settings.smearWhileTyping

            if (hasLastCaretPosition && !suppressForTyping) {
                val dx = abs(newPosition.x - oldPosition.x)
                val dy = abs(newPosition.y - oldPosition.y)

                // Check if movement is significant enough
                if (dx > cursorWidth / 2 || dy > cursorHeight / 2) {
                    // Check direction restrictions
                    val shouldAnimate = when {
                        !settings.smearHorizontally && dy <= cursorHeight / 2 -> false
                        !settings.smearVertically && dx <= cursorWidth / 2 -> false
                        !settings.smearDiagonally && dy > cursorHeight / 2 && dx > cursorWidth / 2 -> false
                        !settings.smearBetweenNeighborLines && dy <= cursorHeight * 1.5 -> false
                        else -> true
                    }

                    if (shouldAnimate) {
                        animationEngine.animateTo(newPosition.x, newPosition.y)
                        startAnimation()
                    } else {
                        animationEngine.jump(newPosition.x, newPosition.y)
                    }
                } else {
                    // Small movement, just jump
                    animationEngine.jump(newPosition.x, newPosition.y)
                }
            } else {
                animationEngine.jump(newPosition.x, newPosition.y)
            }

            lastCaretPosition.set(newPosition)
            hasLastCaretPosition = true
            repaint() // Always repaint on caret move
        }
    }

    /**
     * Start the animation timer.
     */
    private fun startAnimation() {
        if (animationTimer?.isRunning != true) {
            animationTimer?.start()
        }
        repaint()
    }

    /**
     * Stop the animation timer.
     */
    private fun stopAnimation() {
        animationTimer?.stop()
        animationEngine.stopAnimation()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val settings = SmearCursorSettings.getInstance()

        if (!isEnabled || !settings.enabled) return

        val g2d = g.create() as Graphics2D
        try {
            // Set rendering hints for performance
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )
            g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED
            )
            
            // Update animation and get current frame
            val frame = animationEngine.update(settings)

            if (frame != null && frame.isAnimating) {
                renderer.render(g2d, frame, editor)
                
                // Update timer interval in case settings changed
                animationTimer?.delay = settings.timeInterval
            } else {
                // Animation finished
                animationTimer?.stop()
            }
        } finally {
            g2d.dispose()
        }
    }

    /**
     * Enable or disable the overlay.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            stopAnimation()
            repaint()
        }
    }

    /**
     * Make the overlay completely mouse-transparent.
     * By always returning false, all mouse events pass through to the editor below.
     * This ensures Cmd+Click (Go to Declaration) and other mouse interactions work normally.
     */
    override fun contains(x: Int, y: Int): Boolean = false

    /**
     * Clean up resources when the overlay is no longer needed.
     */

    override fun dispose() {
        animationTimer?.stop()
        animationTimer = null
    }

    /**
     * Handle editor scroll events by updating the caret position.
     */
    fun onScroll() {
        // When scrolling, the caret's screen position changes even if logical position doesn't
        // We need to jump (not animate) to the new position to avoid weird trails
        val newPosition = MutableVector2()
        if (getCaretScreenPosition(newPosition)) {
            animationEngine.jump(newPosition.x, newPosition.y)
            lastCaretPosition.set(newPosition)
            hasLastCaretPosition = true
        } else {
            // Caret not visible, stop animation
            animationEngine.stopAnimation()
            lastCaretPosition.reset()
            hasLastCaretPosition = false
        }
        repaint()
    }

    /**
     * Force refresh the cursor dimensions (call when font changes).
     */
    fun refreshDimensions() {
        updateCursorDimensions()
        val caretPos = MutableVector2()
        if (getCaretScreenPosition(caretPos)) {
            animationEngine.jump(caretPos.x, caretPos.y)
            lastCaretPosition.set(caretPos)
            hasLastCaretPosition = true
        }
    }
}
