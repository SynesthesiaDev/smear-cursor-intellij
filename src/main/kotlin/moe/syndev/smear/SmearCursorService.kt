package moe.syndev.smear

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Disposer
import moe.syndev.smear.render.SmearCursorOverlay
import moe.syndev.smear.settings.SmearCursorSettings
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JRootPane
import javax.swing.SwingUtilities

/**
 * Main application service for managing smear cursor overlays across all editors.
 */
@Service(Service.Level.APP)
class SmearCursorService : Disposable {

    private val editorOverlays = ConcurrentHashMap<Editor, SmearCursorOverlay>()
    private val visibleAreaListeners = ConcurrentHashMap<Editor, VisibleAreaListener>()
    private var editorFactoryListener: EditorFactoryListener? = null
    private var initialized = false

    companion object {
        @JvmStatic
        fun getInstance(): SmearCursorService {
            return ApplicationManager.getApplication().getService(SmearCursorService::class.java)
        }
    }

    /**
     * Initialize the service and start listening for editor events.
     */
    fun initialize() {
        if (initialized) return
        initialized = true

        val settings = SmearCursorSettings.getInstance()
        if (!settings.enabled) return

        // Add overlay to existing editors
        EditorFactory.getInstance().allEditors.forEach { editor ->
            addOverlayToEditor(editor)
        }

        // Listen for new editors
        editorFactoryListener = object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                if (SmearCursorSettings.getInstance().enabled) {
                    SwingUtilities.invokeLater {
                        addOverlayToEditor(event.editor)
                    }
                }
            }

            override fun editorReleased(event: EditorFactoryEvent) {
                removeOverlayFromEditor(event.editor)
            }
        }

        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener!!, this)
    }

    /**
     * Add smear cursor overlay to an editor.
     */
    private fun addOverlayToEditor(editor: Editor) {
        if (editorOverlays.containsKey(editor)) return
        if (editor !is EditorEx) return

        val contentComponent = editor.contentComponent
        
        // Check if the component is ready (has a root pane)
        val rootPane = SwingUtilities.getRootPane(contentComponent)
        if (rootPane != null) {
            // Component is ready, add overlay immediately
            doAddOverlayToEditor(editor, contentComponent, rootPane)
        } else {
            // Component not ready yet (happens for restored tabs at startup)
            // Wait for it to become displayable using a HierarchyListener
            val hierarchyListener = object : HierarchyListener {
                override fun hierarchyChanged(e: HierarchyEvent) {
                    if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L) {
                        if (contentComponent.isShowing) {
                            contentComponent.removeHierarchyListener(this)
                            SwingUtilities.invokeLater {
                                val rp = SwingUtilities.getRootPane(contentComponent)
                                if (rp != null && !editorOverlays.containsKey(editor)) {
                                    doAddOverlayToEditor(editor, contentComponent, rp)
                                }
                            }
                        }
                    }
                }
            }
            contentComponent.addHierarchyListener(hierarchyListener)
        }
    }
    
    /**
     * Actually add the overlay to the editor (called when component is ready).
     */
    private fun doAddOverlayToEditor(editor: Editor, contentComponent: JComponent, rootPane: JRootPane) {
        if (editorOverlays.containsKey(editor)) return
        
        try {
            val overlay = SmearCursorOverlay(editor)
            val layeredPane = rootPane.layeredPane
            
            // Function to update overlay position to match content component screen location
            fun updateOverlayBounds() {
                if (!contentComponent.isShowing) return
                try {
                    val contentLocation = contentComponent.locationOnScreen
                    val layeredPaneLocation = layeredPane.locationOnScreen
                    overlay.setBounds(
                        contentLocation.x - layeredPaneLocation.x,
                        contentLocation.y - layeredPaneLocation.y,
                        contentComponent.width,
                        contentComponent.height
                    )
                } catch (e: Exception) {
                    // Component may not be showing yet
                }
            }
            
            // Add to drag layer (high z-order, doesn't interfere with content)
            layeredPane.add(overlay)
            layeredPane.setLayer(overlay, JLayeredPane.DRAG_LAYER)
            
            editorOverlays[editor] = overlay

            // Listen for visible area changes (scrolling)
            val visibleAreaListener = VisibleAreaListener { _ ->
                overlay.onScroll()
            }
            editor.scrollingModel.addVisibleAreaListener(visibleAreaListener)
            visibleAreaListeners[editor] = visibleAreaListener

            // Update overlay bounds when content component changes
            contentComponent.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    SwingUtilities.invokeLater { updateOverlayBounds() }
                }
                override fun componentMoved(e: ComponentEvent?) {
                    SwingUtilities.invokeLater { updateOverlayBounds() }
                }
                override fun componentShown(e: ComponentEvent?) {
                    SwingUtilities.invokeLater { updateOverlayBounds() }
                }
            })
            
            // Initial bounds update
            SwingUtilities.invokeLater { updateOverlayBounds() }
        } catch (e: Exception) {
            // Silently fail - editor may not be ready yet
        }
    }

    /**
     * Remove smear cursor overlay from an editor.
     */
    private fun removeOverlayFromEditor(editor: Editor) {
        val overlay = editorOverlays.remove(editor) ?: return
        
        try {
            overlay.dispose()
            overlay.parent?.remove(overlay)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove visible area listener
        visibleAreaListeners.remove(editor)?.let { listener ->
            editor.scrollingModel.removeVisibleAreaListener(listener)
        }
    }

    /**
     * Enable or disable smear cursor for all editors.
     */
    fun setEnabled(enabled: Boolean) {
        SmearCursorSettings.getInstance().enabled = enabled
        
        editorOverlays.values.forEach { overlay ->
            overlay.setEnabled(enabled)
        }

        if (enabled && !initialized) {
            initialize()
        }
    }

    /**
     * Toggle smear cursor on/off.
     */
    fun toggle() {
        setEnabled(!SmearCursorSettings.getInstance().enabled)
    }

    /**
     * Check if smear cursor is enabled.
     */
    fun isEnabled(): Boolean = SmearCursorSettings.getInstance().enabled

    override fun dispose() {
        editorOverlays.keys.toList().forEach { editor ->
            removeOverlayFromEditor(editor)
        }
        editorOverlays.clear()
        visibleAreaListeners.clear()
        initialized = false
    }
}
