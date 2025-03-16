package io.github.ethersync.ui

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import io.github.ethersync.StartEthersyncDaemonAction
import io.github.ethersync.StopEthersyncDaemonAction
import java.awt.BorderLayout
import javax.swing.JPanel

class ToolWindow(project: Project): JPanel() {

    val console: ConsoleView

    init {
        setLayout(BorderLayout())

        console = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console

        add(console.component, BorderLayout.CENTER)

        val actionToolBar = ActionManager.getInstance().createActionToolbar("", object : ActionGroup() {
            override fun getChildren(p0: AnActionEvent?): Array<AnAction> {
                return arrayOf(StartEthersyncDaemonAction(), StopEthersyncDaemonAction())
            }
        }, true)

        add(actionToolBar.component, BorderLayout.NORTH)
    }

    fun attachToProcess(processHandler: ProcessHandler) {
        console.clear()
        console.attachToProcess(processHandler)
    }
}