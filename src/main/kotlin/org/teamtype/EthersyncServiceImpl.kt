package org.teamtype

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.io.await
import com.intellij.util.io.awaitExit
import com.intellij.util.io.readLineAsync
import org.teamtype.protocol.*
import org.teamtype.settings.AppSettings
import org.teamtype.sync.Changetracker
import org.teamtype.sync.Cursortracker
import org.teamtype.ui.ToolWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.Executors

private val LOG = logger<org.teamtype.EthersyncServiceImpl>()

@Service(Service.Level.PROJECT)
class EthersyncServiceImpl(
   private val project: Project,
   private val cs: CoroutineScope,
) : org.teamtype.EthersyncService {

   private var launcher: Launcher<org.teamtype.protocol.RemoteEthersyncClientProtocol>? = null
   private var daemonProcess: ColoredProcessHandler? = null
   private var clientProcess: Process? = null

   private val changetracker: org.teamtype.sync.Changetracker =
       _root_ide_package_.org.teamtype.sync.Changetracker(project, cs)
   private val cursortracker: org.teamtype.sync.Cursortracker =
       _root_ide_package_.org.teamtype.sync.Cursortracker(project, cs)

   /** test-only! */
   var attachDaemonOutputToUi: Boolean = true

   init {
      val bus = project.messageBus.connect()
      bus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
         override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            val canonicalFile = file.canonicalFile ?: return
            val content = LoadTextUtil.loadText(file).toString()
            launchDocumentOpenRequest(canonicalFile.url, content)
         }

         override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
            val canonicalFile = file.canonicalFile ?: return
            launchDocumentCloseNotification(canonicalFile.url)
         }
      })

      for (editor in FileEditorManager.getInstance(project).allEditors) {
         if (editor is TextEditor) {
            val file = editor.file ?: continue
            if (!file.exists()) {
               continue
            }
            editor.editor.caretModel.addCaretListener(cursortracker)
            editor.editor.document.addDocumentListener(changetracker)
         }
      }

      EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
         override fun editorCreated(event: EditorFactoryEvent) {
            val doc = event.editor.document
            val file = PsiDocumentManager.getInstance(project).getPsiFile(doc) ?: return
            LOG.debug("Starting to watch changes of ${file}")

            event.editor.caretModel.addCaretListener(cursortracker)
            event.editor.document.addDocumentListener(changetracker)
         }

         override fun editorReleased(event: EditorFactoryEvent) {
            val file = event.editor.virtualFile ?: return
            if (!file.exists()) {
               return
            }

            event.editor.caretModel.removeCaretListener(cursortracker)
            event.editor.document.removeDocumentListener(changetracker)
         }
      }, project)

      ProjectManager.getInstance().addProjectManagerListener(project, object : ProjectManagerListener {
         override fun projectClosingBeforeSave(project: Project) {
            shutdown()
         }
      })
   }

   override fun shutdown(): Job {
      return cs.launch {
         shutdownImpl()
      }
   }

   private suspend fun shutdownImpl() {
      clientProcess?.let {
         it.destroy()
         it.awaitExit()
         clientProcess = null
      }
      daemonProcess?.let {
         it.detachProcess()
         it.process.destroy()
         it.process.awaitExit()
         daemonProcess = null
      }
      changetracker.clear()
      cursortracker.clear()
   }

   override fun start(joinCode: String?): Job {
      val cmd = GeneralCommandLine(AppSettings.getInstance().state.ethersyncBinaryPath)

      if (joinCode == null || joinCode.trim().isEmpty()) {
         cmd.addParameter("share")
      } else {
         cmd.addParameter("join")
         cmd.addParameter(joinCode.trim())
      }

      return cs.launch {
         val channel = Channel<Unit>()
         launchDaemon(cmd, channel)
         channel.consumeEach { msg ->
            LOG.debug("Started: $msg")
         }
      }
   }

   override fun startWithCustomCommandLine(commandLine: String) {
      // TODO: splitting by " " is probably insufficient if there is an argument with spaces in it…
      val cmd = GeneralCommandLine(commandLine.split(" "))

      cs.launch {
         val channel = Channel<Unit>()
         launchDaemon(cmd, channel)
         channel.consumeEach { msg ->
            LOG.debug("Started: $msg")
         }
      }
   }

   private suspend fun launchDaemon(cmd: GeneralCommandLine, clientStarted: Channel<Unit>) {
      val projectDirectory = File(project.basePath!!)
      val ethersyncDirectory = File(projectDirectory, ".teamtype")
      cmd.workDirectory = projectDirectory

      shutdownImpl()

      if (!ethersyncDirectory.exists()) {
         LOG.debug("Creating teamtype directory")
         val permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
         withContext(Dispatchers.IO) {
            Files.createDirectory(ethersyncDirectory.toPath(), permissions)
         };
      }

      daemonProcess = object : ColoredProcessHandler(cmd) {
         override fun readerOptions(): BaseOutputReader.Options {
            return BaseOutputReader.Options.forMostlySilentProcess()
         }
      }

      daemonProcess!!.addProcessListener(object : ProcessListener {
         override fun startNotified(event: ProcessEvent) {
            cs.launch {
               val ethersyncSocket = File(ethersyncDirectory, "socket").toPath()
               while (!Files.exists(ethersyncSocket)) {
                  Thread.sleep(100)
               }
               launchEthersyncClient(projectDirectory, clientStarted)
            }
         }

         override fun processTerminated(event: ProcessEvent) {
            shutdown()
         }
      })


      attachDaemonToToolWindow()

      daemonProcess!!.startNotify()
   }

   private suspend fun attachDaemonToToolWindow() {
      if (!attachDaemonOutputToUi) {
         return
      }
      val process = daemonProcess ?: return

      withContext(Dispatchers.EDT) {
         val tw = ToolWindowManager.getInstance(project).getToolWindow("ethersync") ?: return@withContext

         val daemon = tw.contentManager.findContent("Daemon") ?: return@withContext
         val toolWindow = daemon.component
         if (toolWindow is ToolWindow) {
            toolWindow.attachToProcess(process)
         }

         tw.show()
      }
   }

   private fun createProtocolHandler(): EthersyncEditorProtocol {

      return object : EthersyncEditorProtocol {
         override fun cursor(cursorEvent: CursorEvent) {
            cursortracker.handleRemoteCursorEvent(cursorEvent)
         }

         override fun edit(editEvent: EditEvent) {
            changetracker.handleRemoteEditEvent(editEvent)
         }

      }
   }

   private suspend fun launchEthersyncClient(projectDirectory: File, clientStarted: Channel<Unit>) {
      if (clientProcess != null) {
         return
      }

      LOG.info("Starting teamtype client")
      // TODO: try catch not existing binary
      val clientProcessBuilder = ProcessBuilder(AppSettings.getInstance().state.ethersyncBinaryPath, "client")
         .directory(projectDirectory)
      clientProcess = clientProcessBuilder.start()
      val clientProcess = clientProcess!!

      val ethersyncEditorProtocol = createProtocolHandler()
      launcher = Launcher.createIoLauncher(
         ethersyncEditorProtocol,
         RemoteEthersyncClientProtocol::class.java,
         clientProcess.inputStream,
         clientProcess.outputStream,
         Executors.newCachedThreadPool(),
         { c -> c },
         { _ -> run {} }
      )

      val listening = launcher!!.startListening()
      cursortracker.remoteProxy = launcher!!.remoteProxy
      changetracker.remoteProxy = launcher!!.remoteProxy

      val fileEditorManager = FileEditorManager.getInstance(project)
      for (file in fileEditorManager.openFiles) {
         val content = LoadTextUtil.loadText(file).toString()
         launchDocumentOpenRequest(file.canonicalFile!!.url, content)
      }

      clientStarted.send(Unit)
      clientStarted.close()
      clientProcess.awaitExit()

      listening.cancel(true)
      listening.await()

      if (clientProcess.exitValue() != 0) {
         val stderr = BufferedReader(InputStreamReader(clientProcess.errorStream))
         stderr.use {
            while (true) {
               try {
                  val line = stderr.readLineAsync() ?: break;
                  LOG.trace(line)
               } catch (e: IOException) {
                  LOG.trace(e)
                  break
               }
            }
         }
      }
   }

   fun launchDocumentCloseNotification(fileUri: String) {
      val launcher = launcher ?: return
      cs.launch {
         launcher.remoteProxy.close(DocumentCloseRequest(fileUri))
         changetracker.closeFile(fileUri)
      }
   }

   fun launchDocumentOpenRequest(fileUri: String, content: String) {
      val launcher = launcher ?: return
      cs.launch {
         try {
            launcher.remoteProxy.open(DocumentOpenRequest(fileUri, content)).await()
         } catch (e: ResponseErrorException) {
            TODO("not yet implemented: notify about an protocol error")
         }
      }
   }

}
