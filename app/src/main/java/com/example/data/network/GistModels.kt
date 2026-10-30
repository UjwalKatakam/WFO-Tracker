package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GistFile(
    val filename: String? = null,
    val type: String? = null,
    val language: String? = null,
    val raw_url: String? = null,
    val size: Long? = null,
    val truncated: Boolean? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GistResponse(
    val id: String,
    val description: String? = null,
    val public: Boolean = false,
    val files: Map<String, GistFile> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GistFileContent(
    val content: String
)

@JsonClass(generateAdapter = true)
data class GistUpdateRequest(
    val description: String? = null,
    val files: Map<String, GistFileContent>
)
