package io.github.ethersync

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.utils.vfs.createFile
import com.intellij.util.application
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.Optional
import kotlin.io.path.Path

class EthersyncServiceImplTest : HeavyPlatformTestCase() {

   var daemon: Process? = null
   var joinCode: String? = null
   var daemonDir: Path? = null

   override fun setUp() {
      super.setUp()

      daemonDir = Files.createTempDirectory("remote-project")
      val permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
      Files.createDirectory(Path(daemonDir!!.toString(), ".teamtype"), permissions);

      daemon = ProcessBuilder()
         .command("teamtype", "share")
         .directory(daemonDir!!.toFile())
         .start()

      val reader = BufferedReader(InputStreamReader(daemon!!.inputStream))
      reader.use {
         var line: String? = null
         do {
            line = reader.readLine()

            if(line != null) {
               line = line.trim()
               if (line.startsWith("teamtype join ")) {
                  joinCode = line.substring(14)
                  break
               }
            }
         } while (line != null)
      }
   }

   fun testIntention() {
      val dir = orCreateProjectBaseDir

      val service = project.service<EthersyncService>()
      runInEdtAndWait {
         service.start(joinCode)
      }

      Thread.sleep(5_000)


      application.runWriteAction {
         dir.createFile("file.txt")
      }

      runInEdtAndWait {
         val file = dir.findOrCreateFile("file.txt")
         FileEditorManager.getInstance(project).openFile(file)
         val editor = FileEditorManager.getInstance(project)
            .allEditors
            .filterIsInstance<TextEditor>()
            .firstOrNull { editor -> editor.file == file }!!

         Thread.sleep(5_000)

         val document = editor.editor.document
         WriteCommandAction.runWriteCommandAction(project, {
            document.insertString(0, "Hello")
         })

         Thread.sleep(5_000)
      }

      val firstLine = Files.lines(Path(daemonDir!!.toString(), "file.txt"))
         .findFirst()
      assertEquals(Optional.of("Hello"), firstLine)

      runInEdtAndWait {
         service.shutdown()
      }
   }

   override fun tearDown() {
      daemon!!.destroy()
      daemon!!.waitFor()
      daemon = null
      daemonDir = null
      joinCode = null
   }
}
