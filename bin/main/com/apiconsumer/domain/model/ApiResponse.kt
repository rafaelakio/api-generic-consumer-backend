package com.apiconsumer.domain.model

data class ApiResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, String>,
    val body: Any?,
    val responseTimeMs: Long,
    val error: String? = null,
)
