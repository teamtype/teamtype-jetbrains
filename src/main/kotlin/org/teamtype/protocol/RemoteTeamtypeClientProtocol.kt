package org.teamtype.protocol

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

interface RemoteTeamtypeClientProtocol {
    @JsonRequest
    fun cursor(cursorRequest: CursorRequest): CompletableFuture<Void>

    @JsonRequest
    fun open(documentRequest: DocumentOpenRequest): CompletableFuture<Void>

    @JsonRequest
    fun edit(editRequest: EditRequest): CompletableFuture<Void>

    @JsonNotification
    fun close(documentRequest: DocumentCloseRequest)
}