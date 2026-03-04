package org.teamtype.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.jetbrains.annotations.NonNls

@State(
   name = "org.teamtype.settings.AppSettings",
   storages = [Storage("TeamtypeSettingsPlugin.xml")]
)
class AppSettings : PersistentStateComponent<AppSettings.State> {

   data class State(
      @NonNls
      var teamtypeBinaryPath: String = "teamtype"
   )

   private var state: State = State()

   companion object {
      fun getInstance() : AppSettings {
         return ApplicationManager.getApplication().getService(AppSettings::class.java)
      }
   }

   override fun getState(): State {
      return state
   }

   override fun loadState(state: State) {
      this.state = state
   }
}