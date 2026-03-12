package org.teamtype

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class StartTeamtypeDaemonAction : AnAction("Connect to peer", "Connect to a running teamtype daemon or start a new peer",
   AllIcons.CodeWithMe.CwmInvite) {

   override fun actionPerformed(e: AnActionEvent) {
      val project = e.project ?: return
      val joinCode = Messages.showInputDialog(
         project,
         "Provide teamtype join code. Leave empty if you want to host a new session.",
         "Join Code",
         Icons.ToolbarIcon
      )

      if (joinCode != null) {
         val service = project.service<TeamtypeService>()

         service.start(joinCode)
      }
   }
}
