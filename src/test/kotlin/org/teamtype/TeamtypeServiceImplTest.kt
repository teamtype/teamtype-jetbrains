package org.teamtype

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.utils.vfs.createFile
import com.intellij.testFramework.utils.vfs.getDocument
import com.intellij.util.application
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.*
import kotlin.io.path.Path

class TeamtypeServiceImplTest : HeavyPlatformTestCase() {


   /**
    * A teamtype daemon that is used to simulate a remote client
    */
   var remoteDaemon: Process? = null
   var joinCode: String? = null

   /**
    * This is the directory where the simulated remote project has been build.
    */
   var remoteProjectDir: Path? = null

   override fun setUp() {
      super.setUp()

      startRemoteTeamTypeSession()
      connectJetbrainsToRemoteProject()
   }

   fun startRemoteTeamTypeSession() {
      remoteProjectDir = Files.createTempDirectory("remote-project")
      val permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
      Files.createDirectory(Path(remoteProjectDir!!.toString(), ".teamtype"), permissions);

      remoteDaemon = ProcessBuilder()
         .command("teamtype", "share")
         .directory(remoteProjectDir!!.toFile())
         .start()

      val reader = BufferedReader(InputStreamReader(remoteDaemon!!.inputStream))
      reader.use {
         do {
            var line = reader.readLine() ?: break
            println("Output from daemon: $line")

            line = line.trim()
            if (line.startsWith("teamtype join ")) {
               joinCode = line.substring(14)
               break
            }
         } while (true)
      }

      assertNotNull(joinCode)
   }

   fun connectJetbrainsToRemoteProject() {
      assertNotNull(remoteDaemon)

      val dir = orCreateProjectBaseDir

      val service = project.service<TeamtypeServiceImpl>()
      service.attachDaemonOutputToUi = false
      val notifier = service.start(joinCode)

      runBlocking() {
         notifier.join()
      }

      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

   }

   fun testSyncChangesFromRemoteProjectToJetbrains() {
      val dir = orCreateProjectBaseDir

      application.runWriteAction {
         dir.createFile("jetbrains.txt")
      }

      var editor: TextEditor? = null
      runInEdtAndWait {
         val file = dir.findOrCreateFile("jetbrains.txt")
         FileEditorManager.getInstance(project).openFile(file)
         editor = FileEditorManager.getInstance(project)
            .allEditors
            .filterIsInstance<TextEditor>()
            .firstOrNull { editor -> editor.file == file }!!
      }

      // give teamtype and Intellij a bit time to perform the changes
      val remoteFile = Path(remoteProjectDir!!.toString(), "jetbrains.txt")
      while (!Files.exists(remoteFile)) {
         Thread.sleep(100)
         PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      }

      Files.write(remoteFile, ("Hello from remote project" + System.lineSeparator()).toByteArray(), StandardOpenOption.CREATE);

      // TODO: is there another way to get notified when Intellij got a message from the daemon?
      // give teamtype and Intellij a bit time to perform the changes
      Thread.sleep(2_000)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

      var text: String? = null
      runInEdtAndWait {
         text = editor!!.editor.virtualFile!!.getDocument().text
      }

      assertEquals("Hello from remote project" + System.lineSeparator(), text)
   }

   fun testSyncChangesFromJetbrainsToRemoteProject() {
      val dir = orCreateProjectBaseDir

      application.runWriteAction {
         dir.createFile("jetbrains-other.txt")
      }

      var editor: TextEditor? = null
      runInEdtAndWait {
         val file = dir.findOrCreateFile("jetbrains-other.txt")
         FileEditorManager.getInstance(project).openFile(file)
         editor = FileEditorManager.getInstance(project)
            .allEditors
            .filterIsInstance<TextEditor>()
            .firstOrNull { editor -> editor.file == file }!!

         val document = editor.editor.document
         WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(0, "Hello from Jetbrains" + System.lineSeparator())
         }
      }

      // give teamtype and Intellij a bit time to perform the changes
      val remoteFile = Path(remoteProjectDir!!.toString(), "jetbrains-other.txt")
      while (!Files.exists(remoteFile)) {
         Thread.sleep(100)
         PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      }

      val firstLine = Files.lines(remoteFile).findFirst()
      assertEquals(Optional.of("Hello from Jetbrains"), firstLine)
   }

   override fun tearDown() {
      val fem = FileEditorManager.getInstance(project)
      for (f in fem.openFiles) {
         application.runWriteAction {
            fem.closeFile(f)
         }
      }

      val service = project.service<TeamtypeService>()
      val notifier = service.shutdown()

      runBlocking() {
         notifier.join()
      }

      remoteDaemon!!.destroy()
      remoteDaemon!!.waitFor()
      remoteDaemon = null
      remoteProjectDir = null
      joinCode = null

      super.tearDown()
   }
}
