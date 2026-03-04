package org.teamtype.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class EthersyncToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = org.teamtype.ui.ToolWindow(project)

        toolWindow.contentManager.addContent(ContentFactory.getInstance()
            .createContent(panel, "Daemon", true))
    }
}