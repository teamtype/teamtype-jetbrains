package io.github.ethersync.protocol

import com.google.gson.annotations.SerializedName

data class DocumentOpenRequest(
   @SerializedName("uri")
   val documentUri: String,
   val content: String
)