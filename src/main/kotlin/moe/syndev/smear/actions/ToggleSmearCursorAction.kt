package moe.syndev.smear.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import moe.syndev.smear.SmearCursorService

/**
 * Action to toggle the smear cursor effect on/off.
 */
class ToggleSmearCursorAction : ToggleAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return SmearCursorService.getInstance().isEnabled()
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        SmearCursorService.getInstance().setEnabled(state)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val isEnabled = SmearCursorService.getInstance().isEnabled()
        val text = if (isEnabled) "Disable Smear Cursor" else "Enable Smear Cursor"
        e.presentation.text = text
    }
}
