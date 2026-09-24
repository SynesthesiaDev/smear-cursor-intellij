package moe.syndev.smear.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.ui.Gray
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

/**
 * Persistent settings for the Smear Cursor plugin.
 */
@State(
    name = "SmearCursorSettings",
    storages = [Storage("SmearCursorSettings.xml")]
)
@Service(Service.Level.APP)
class SmearCursorSettings : PersistentStateComponent<SmearCursorSettings> {

    // General configuration
    var enabled: Boolean = true
    var smearWhileTyping: Boolean = false
    var smearBetweenWindows: Boolean = true
    var smearBetweenNeighborLines: Boolean = true
    var minHorizontalDistanceSmear: Int = 0
    var minVerticalDistanceSmear: Int = 0
    var smearHorizontally: Boolean = true
    var smearVertically: Boolean = true
    var smearDiagonally: Boolean = true

    
    // Animation timing
    var timeInterval: Int = 16 // milliseconds (approximately 60 FPS)

    
    // Smear dynamics configuration
    var headSpeed: Double = 0.6 // How fast the smear's head moves towards target (0-1)
    var tailSpeed: Double = 0.45 // How fast the smear's tail moves towards target (0-1)
    var lag: Double = 0.2 // Initial velocity factor opposite to target
    var damping: Double = 0.99 // Velocity reduction over time (0-1)
        set(value) { field = value.coerceIn(0.0, 0.999) } // clamp to prevent NaN in physics calculations
    var trailingExponent: Double = 3.0 // Controls middle points closer to head or tail
    var distanceStopAnimating: Double = 0.1 // Stop when within this distance

    var maxLength: Int = 25 // Maximum smear length

    // Color settings (stored as RGB integers)
    var cursorColorRgb: Int = Gray._208.rgb // Default cursor color
    var useEditorCursorColor: Boolean = true // Use the editor's cursor color

    companion object {
        
        @JvmStatic
        fun getInstance(): SmearCursorSettings {
            return ApplicationManager.getApplication().getService(SmearCursorSettings::class.java)
        }
    }

    override fun getState(): SmearCursorSettings = this

    override fun loadState(state: SmearCursorSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getCursorColor(): Color = Color(cursorColorRgb)

    fun setCursorColor(color: Color) {
        cursorColorRgb = color.rgb
    }
}
