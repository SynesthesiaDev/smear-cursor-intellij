package com.smearcursor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity


/**
 * Startup activity that initializes the smear cursor service when IDE starts.
 */
class SmearCursorStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            SmearCursorService.getInstance().initialize()
        }
    }
}
