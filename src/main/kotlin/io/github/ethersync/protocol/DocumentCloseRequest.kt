package io.github.ethersync.protocol

import com.google.gson.annotations.SerializedName

data class DocumentCloseRequest(
   @SerializedName("uri")
   val documentUri: String,
)