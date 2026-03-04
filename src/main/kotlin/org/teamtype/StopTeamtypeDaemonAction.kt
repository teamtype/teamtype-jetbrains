package org.teamtype

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class StopTeamtypeDaemonAction : AnAction("Stpo daemon", "Stop running teamtype daemon", AllIcons.Run.Stop) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val service = project.service<TeamtypeService>()
        service.shutdown()
    }
}