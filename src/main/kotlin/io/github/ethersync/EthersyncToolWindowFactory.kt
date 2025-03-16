package io.github.ethersync

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel


class EthersyncToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()
        panel.setLayout(BorderLayout())

        val console = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console

        panel.add(console.component, BorderLayout.CENTER)

        val actionToolBar = ActionManager.getInstance().createActionToolbar("", object : ActionGroup() {
            override fun getChildren(p0: AnActionEvent?): Array<AnAction> {
               return arrayOf(StartEthersyncDaemonAction(), StopEthersyncDaemonAction())
            }
        }, true)

        panel.add(
            actionToolBar.component, BorderLayout.NORTH)

        toolWindow.contentManager.addContent(ContentFactory.getInstance()
            .createContent(panel, "Daemon", true))
    }
}