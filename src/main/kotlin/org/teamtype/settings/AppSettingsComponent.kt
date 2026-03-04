package org.teamtype.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class AppSettingsComponent {

   val panel: JPanel
   private val teamtypeBinaryTF: JBTextField = JBTextField()

   init {
       panel = FormBuilder.createFormBuilder()
          .addLabeledComponent(JBLabel("Teamtype binary:"), teamtypeBinaryTF, 1, false)
          .addComponentFillVertically(JPanel(), 0)
          .panel
   }

   var teamtypeBinary: String
      get() {
         return if (teamtypeBinaryTF.text.isNullOrBlank()) {
            "teamtype"
         } else {
            teamtypeBinaryTF.text.toString()
         }
      }
      set(value) {
         teamtypeBinaryTF.setText(value)
      }
}