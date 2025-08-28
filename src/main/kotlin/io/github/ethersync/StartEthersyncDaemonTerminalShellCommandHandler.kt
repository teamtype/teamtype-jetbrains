package io.github.ethersync

import com.intellij.execution.Executor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.terminal.TerminalShellCommandHandler
import io.github.ethersync.settings.AppSettings

class StartEthersyncDaemonTerminalShellCommandHandler : TerminalShellCommandHandler {
    override fun execute(
        project: Project,
        workingDirectory: String?,
        localSession: Boolean,
        command: String,
        executor: Executor
    ): Boolean {
        val ethersyncService = project.service<EthersyncService>()
        ethersyncService.startWithCustomCommandLine(command)
        return true
    }

    override fun matches(project: Project, workingDirectory: String?, localSession: Boolean, command: String): Boolean {
        if (workingDirectory == null) {
           return false
        }
        else if (project.basePath != workingDirectory) {
           return false
        }

        val ethersyncBinary = AppSettings.getInstance().state.ethersyncBinaryPath

        if (!command.startsWith(ethersyncBinary)) {
            return false
        }

        val rest = command.substring(ethersyncBinary.length).trim()

        return rest.startsWith("share") || rest.startsWith("join")
    }
}
