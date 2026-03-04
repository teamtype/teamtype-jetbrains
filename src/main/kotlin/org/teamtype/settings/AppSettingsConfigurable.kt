package org.teamtype.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class AppSettingsConfigurable : Configurable {

   private var compoment: AppSettingsComponent? = null

   override fun createComponent(): JComponent {
      compoment = AppSettingsComponent()
      return compoment!!.panel
   }

   override fun isModified(): Boolean {
      val state = AppSettings.getInstance().state
      return state.teamtypeBinaryPath != compoment!!.teamtypeBinary
   }

   override fun apply() {
      val state = AppSettings.getInstance().state
      state.teamtypeBinaryPath = compoment!!.teamtypeBinary
   }

   @Nls(capitalization = Nls.Capitalization.Title)
   override fun getDisplayName(): String {
      return "Teamtype"
   }

   override fun reset() {
      val state = AppSettings.getInstance().state
      compoment!!.teamtypeBinary = state.teamtypeBinaryPath
   }

   override fun disposeUIResources() {
      compoment = null
   }
}