package com.smearcursor.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.smearcursor.SmearCursorService

/**
 * Action to toggle the smear cursor effect on/off.
 */
class ToggleSmearCursorAction : AnAction(), Toggleable {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        SmearCursorService.getInstance().toggle()
    }

    override fun update(e: AnActionEvent) {
        val isEnabled = SmearCursorService.getInstance().isEnabled()
        Toggleable.setSelected(e.presentation, isEnabled)
        
        val text = if (isEnabled) "Disable Smear Cursor" else "Enable Smear Cursor"
        e.presentation.text = text
    }
}
