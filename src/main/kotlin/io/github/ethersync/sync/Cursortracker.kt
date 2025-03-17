package io.github.ethersync.sync

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.withUiContext
import com.intellij.ui.JBColor
import com.intellij.util.io.await
import io.github.ethersync.protocol.CursorEvent
import io.github.ethersync.protocol.CursorRequest
import io.github.ethersync.protocol.RemoteEthersyncClientProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import java.awt.Graphics
import java.awt.Graphics2D
import java.util.*
import kotlin.collections.HashMap

class Cursortracker(
   private val project: Project,
   private val cs: CoroutineScope,
) : CaretListener {

   private data class Key(val documentUri: String, val user: String)
   private val highlighter = HashMap<Key, List<RangeHighlighter>>()

   var remoteProxy: RemoteEthersyncClientProtocol? = null

   fun handleRemoteCursorEvent(cursorEvent: CursorEvent) {
      val fileEditor = FileEditorManager.getInstance(project)
         .allEditors
         .filterIsInstance<TextEditor>()
         .filter { editor -> editor.file.canonicalFile != null }
         .firstOrNull { editor -> editor.file.canonicalFile!!.url == cursorEvent.documentUri } ?: return

      val key = Key(cursorEvent.documentUri, cursorEvent.userId)
      val editor = fileEditor.editor

      cs.launch {
         withUiContext {
            synchronized(highlighter) {
               val markupModel = editor.markupModel

               val previous = highlighter.remove(key)
               if (previous != null) {
                  for (hl in previous) {
                     markupModel.removeHighlighter(hl)
                  }
               }

               val newHighlighter = LinkedList<RangeHighlighter>()
               for((i, range) in cursorEvent.ranges.withIndex()) {
                  val startPosition = editor.logicalPositionToOffset(LogicalPosition(range.start.line, range.start.character))
                  val endPosition = editor.logicalPositionToOffset(LogicalPosition(range.end.line, range.end.character))

                  val textAttributes = TextAttributes().apply {
                     effectType = EffectType.ROUNDED_BOX
                     effectColor = JBColor(JBColor.YELLOW, JBColor.DARK_GRAY)
                  }
                  val hl = markupModel.addRangeHighlighter(
                     startPosition,
                     endPosition,
                     HighlighterLayer.ADDITIONAL_SYNTAX,
                     textAttributes,
                     HighlighterTargetArea.EXACT_RANGE
                  ).apply {
                     customRenderer = object : CustomHighlighterRenderer {
                        override fun paint(editor: Editor, rl: RangeHighlighter, g: Graphics) {
                           if (i > 0) {
                              return
                           }

                           if (g is Graphics2D) {
                              val position = editor.offsetToVisualPosition(rl.endOffset)
                              val endOfLineOffset = editor.document.getLineEndOffset(position.line)
                              val endOfLinePosition = editor.offsetToVisualPosition(endOfLineOffset)
                              val endOfLineVisualPosition = editor.visualPositionToXY(endOfLinePosition)

                              val font = editor.colorsScheme.getFont(EditorFontType.PLAIN);
                              g.font = font

                              val metrics = g.getFontMetrics(font)
                              val bounds = metrics.getStringBounds(cursorEvent.name, g)

                              g.drawString(cursorEvent.name, endOfLineVisualPosition.x + 10f, endOfLineVisualPosition.y + bounds.height.toFloat())
                           }
                        }
                     }
                  }

                  newHighlighter.add(hl)
               }
               highlighter[key] = newHighlighter
            }
         }
      }
   }

   override fun caretPositionChanged(event: CaretEvent) {
      val canonicalFile = event.editor.virtualFile?.canonicalFile ?: return
      val uri = canonicalFile.url

      val ranges = event.editor.caretModel
         .allCarets
         .map {caret ->
            val pos = Position(caret.logicalPosition.line, caret.logicalPosition.column)
            Range(pos, pos)
         }

      launchCursorRequest(CursorRequest(uri, ranges))
   }

   private fun launchCursorRequest(cursorRequest: CursorRequest) {
      val remoteProxy = remoteProxy ?: return
      cs.launch {
         try {
            remoteProxy.cursor(cursorRequest).await()
         } catch (e: ResponseErrorException) {
            TODO("not yet implemented: notify about an protocol error")
         }
      }
   }

   suspend fun clear() {
      remoteProxy = null
      withUiContext {
         synchronized(highlighter) {
            for (entry in highlighter) {
               val fileEditor = FileEditorManager.getInstance(project)
                  .allEditors
                  .filterIsInstance<TextEditor>()
                  .filter { editor -> editor.file.canonicalFile != null }
                  .firstOrNull { editor -> editor.file.canonicalFile!!.url == entry.key.documentUri } ?: continue

               for (rangeHighlighter in entry.value) {
                  fileEditor.editor.markupModel.removeHighlighter(rangeHighlighter)
               }
            }

            highlighter.clear()
         }
      }
   }
}
