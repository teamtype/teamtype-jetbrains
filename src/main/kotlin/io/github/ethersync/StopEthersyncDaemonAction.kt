package io.github.ethersync

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class StopEthersyncDaemonAction : AnAction("Stpo daemon", "Stop running ethersync daemon", AllIcons.Run.Stop) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val service = project.service<EthersyncService>()
        service.shutdown()
    }
}