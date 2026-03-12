package org.teamtype.sync

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer
import com.intellij.openapi.vfs.VirtualFile

class FileDocumentSynchronizationAlwaysVetoer: FileDocumentSynchronizationVetoer() {

   private val filesToVetoOn = HashSet<String>();

   fun block(fileUrl: String) {
      filesToVetoOn.add(fileUrl)
   }

   fun unblock(fileUrl: String) {
      filesToVetoOn.remove(fileUrl)
   }

   override fun mayReloadFileContent(file: VirtualFile, document: Document): Boolean {
      return !filesToVetoOn.contains(file.url)
   }

   override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {
      val file = FileDocumentManager.getInstance().getFile(document) ?: return true;
      return !filesToVetoOn.contains(file.url)
   }
}