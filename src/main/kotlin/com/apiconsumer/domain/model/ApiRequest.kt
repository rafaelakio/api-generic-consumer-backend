package com.apiconsumer.domain.model

data class ApiRequest(
    val url: String,
    val method: HttpMethod,
    val headers: List<RequestHeader>,
    val body: String?,
    val bypassSsl: Boolean,
    val certificate: String?,
    val credentialsSecretName: String?,
    val contentType: ContentType,
    val files: List<RequestFile>?,
    val formData: Map<String, String>?,
)

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }

enum class ContentType { JSON, RAW, FORM_DATA, X_WWW_FORM_URLENCODED }

data class RequestHeader(
    val key: String,
    val value: String,
    val enabled: Boolean,
)

data class RequestFile(
    val id: String,
    val name: String,
    val size: Long,
    val type: String,
    val content: String,
    val fieldName: String?,
)
