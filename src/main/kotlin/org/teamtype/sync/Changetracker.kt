package org.teamtype.sync

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import org.teamtype.protocol.Delta
import org.teamtype.protocol.EditEvent
import org.teamtype.protocol.EditRequest
import org.teamtype.protocol.RemoteEthersyncClientProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private val LOG = logger<Changetracker>()

class Changetracker(
   private val project: Project,
   private val cs: CoroutineScope,
) : DocumentListener {

   // TODO: remove because that seems brittle…
   private val ignoreChangeEvent = AtomicBoolean(false)

   private data class FileRevision(
      // Number of operations the daemon has made.
      var daemon: UInt = 0u,
      // Number of operations we have made.
      var editor: UInt = 0u,
   )

   private val revisions: HashMap<String, FileRevision> = HashMap()

   var remoteProxy: RemoteEthersyncClientProtocol? = null

   override fun beforeDocumentChange(event: DocumentEvent) {
      if (ignoreChangeEvent.get()) {
         return
      }

      val file = FileDocumentManager.getInstance().getFile(event.document)!!
      val fileEditor = FileEditorManager.getInstance(project).getEditors(file)
         .filter { editor -> editor.file.canonicalFile != null }
         .filterIsInstance<TextEditor>()
         .firstOrNull() ?: return

      if (file.modificationStamp > event.document.modificationStamp) {
         LOG.warn("Document reloaded from file system: ${file.name}")
         return
      }

      val editor = fileEditor.editor
      val uri = file.canonicalFile!!.url

      val rev = revisions.computeIfAbsent(uri) { k -> FileRevision() }
      rev.editor += 1u

      val start = editor.offsetToLogicalPosition(event.offset)
      val end = editor.offsetToLogicalPosition(event.offset + event.oldLength)

      launchEditRequest(
         EditRequest(
            uri,
            rev.daemon,
            Collections.singletonList(
               Delta(
                  Range(
                     Position(start.line, start.column),
                     Position(end.line, end.column)
                  ),
                  // TODO: I remember UTF-16/32… did not test a none ASCII file yet
                  event.newFragment.toString()
               )
            )
         )
      )
   }

   fun handleRemoteEditEvent(editEvent: EditEvent) {
      val revision =
         revisions.computeIfAbsent(editEvent.documentUri) { k ->
            FileRevision(
               editEvent.editorRevision,
               editEvent.editorRevision
            )
         }

      // Check if operation is up-to-date to our content.
      // If it's not, ignore it! The daemon will send a transformed one later.
      if (editEvent.editorRevision == revision.editor) {

         val fileEditorManager = FileEditorManager.getInstance(project)

         val fileEditor = fileEditorManager.allEditors
            .filter { editor -> editor.file.canonicalFile != null }
            .filterIsInstance<TextEditor>()
            .firstOrNull { editor -> editor.file.canonicalFile!!.url == editEvent.documentUri } ?: return

         val editor = fileEditor.editor

         cs.launch {
            withContext(Dispatchers.EDT) {
               WriteCommandAction.runWriteCommandAction(project) {
                  ignoreChangeEvent.set(true)
                  for (delta in editEvent.delta) {
                     val start =
                        editor.logicalPositionToOffset(
                           LogicalPosition(
                              delta.range.start.line,
                              delta.range.start.character
                           )
                        )
                     val end =
                        editor.logicalPositionToOffset(LogicalPosition(delta.range.end.line, delta.range.end.character))

                     editor.document.replaceString(start, end, delta.replacement)
                  }
                  ignoreChangeEvent.set(false)
               }
            }
         }


         revision.daemon += 1u
      }
   }

   fun closeFile(fileUri: String) {
      revisions.remove(fileUri)
   }

   fun clear() {
      remoteProxy = null
      revisions.clear()
   }

   private fun launchEditRequest(editRequest: EditRequest) {
      val remoteProxy = remoteProxy ?: return
      cs.launch {
         try {
            remoteProxy.edit(editRequest).await()
         } catch (e: ResponseErrorException) {
            TODO("not yet implemented: notify about an protocol error")
         }
      }
   }
}
