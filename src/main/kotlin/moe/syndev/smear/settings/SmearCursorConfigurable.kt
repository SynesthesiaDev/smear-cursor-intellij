package moe.syndev.smear.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ColorPanel
import com.intellij.ui.dsl.builder.*
import kotlin.math.roundToInt

class SmearCursorConfigurable : BoundConfigurable("Smear Cursor") {

    private val settings = SmearCursorSettings.getInstance()
    private val colorPanel = ColorPanel()

    override fun createPanel(): DialogPanel = panel {
        group("General Settings") {
            row { checkBox("Enable Smear Cursor").bindSelected(settings::enabled) }
            row { checkBox("Smear effect while typing").bindSelected(settings::smearWhileTyping) }
            row { checkBox("Smear between windows").bindSelected(settings::smearBetweenWindows) }
            row { checkBox("Smear between neighbor lines").bindSelected(settings::smearBetweenNeighborLines) }
            row { checkBox("Smear horizontally").bindSelected(settings::smearHorizontally) }
            row { checkBox("Smear vertically").bindSelected(settings::smearVertically) }
            row { checkBox("Smear diagonally").bindSelected(settings::smearDiagonally) }
        }

        group("Animation Settings") {
            row("Head Speed:") {
                slider(0, 100, 5, (settings.headSpeed * 100).roundToInt())
                    .bindValue(
                        getter = { (settings.headSpeed * 100).roundToInt() },
                        setter = { settings.headSpeed = it / 100.0 },
                    )
                    .comment("How quickly the leading edge of the cursor snaps to its new position.")
                    .resizableColumn()
            }
            row("Tail Speed:") {
                slider(0, 100, 5, (settings.tailSpeed * 100).roundToInt())
                    .bindValue(
                        getter = { (settings.tailSpeed * 100).roundToInt() },
                        setter = { settings.tailSpeed = it / 100.0 },
                    )
                    .comment("How quickly the trailing edge catches up. Lower values create a longer, more pronounced trail.")
                    .resizableColumn()
            }
            
            
            row("Damping:") {
                slider(0, 99, 5, (settings.damping * 100).roundToInt())
                    .bindValue(
                        getter = { (settings.damping * 100).roundToInt() },
                        setter = { settings.damping = it / 100.0 },
                    )
                    .comment("Controls how quickly the animation settles. Higher values mean the cursor stops more abruptly")
                    .resizableColumn()
            }
            row("Lag:") {
                slider(0, 50, 1, (settings.lag * 100).roundToInt())
                    .bindValue(
                        getter = { (settings.lag * 100).roundToInt() },
                        setter = { settings.lag = it / 100.0 },
                    )
                    .comment("How far the cursor shape kicks backward before snapping to its new position. Lower values produce bigger trails")
                    .resizableColumn()
            }
            row("Max Length:") {
                slider(1, 50, 1, settings.maxLength)
                    .bindValue(
                        getter = { settings.maxLength },
                        setter = { settings.maxLength = it },
                    )
                    .resizableColumn()
            }
        }

        group("Color Settings") {
            row { checkBox("Use editor cursor color").bindSelected(settings::useEditorCursorColor) }
            row("Cursor Color:") {
                cell(colorPanel)
            }
        }
    }

    override fun apply() {
        super.apply()
        colorPanel.selectedColor?.let { settings.setCursorColor(it) }
    }

    override fun reset() {
        super.reset()
        colorPanel.selectedColor = settings.getCursorColor()
    }

    override fun isModified(): Boolean {
        return super.isModified() || colorPanel.selectedColor?.rgb != settings.cursorColorRgb
    }
}