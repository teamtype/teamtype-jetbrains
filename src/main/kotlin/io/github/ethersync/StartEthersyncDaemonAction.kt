package io.github.ethersync

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class StartEthersyncDaemonAction : AnAction("Connect to peer", "Connect to a running ethersync daemon or start a new peer",
   AllIcons.CodeWithMe.CwmInvite) {

   override fun actionPerformed(e: AnActionEvent) {
      val project = e.project ?: return
      val address = Messages.showInputDialog(
         project,
         "Provide ethersync peer address. Leave empty if you want to host a new session.",
         "Peer Address",
         Icons.ToolbarIcon
      )

      if (address != null) {
         val service = project.service<EthersyncService>()

         service.start(address)
      }
   }
}
