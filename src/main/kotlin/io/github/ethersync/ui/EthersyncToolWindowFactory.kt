package io.github.ethersync.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class EthersyncToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = io.github.ethersync.ui.ToolWindow(project)

        toolWindow.contentManager.addContent(ContentFactory.getInstance()
            .createContent(panel, "Daemon", true))
    }
}