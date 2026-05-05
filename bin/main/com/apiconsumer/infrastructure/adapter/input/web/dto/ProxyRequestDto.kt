package com.apiconsumer.infrastructure.adapter.input.web.dto

import com.apiconsumer.domain.model.ApiRequest
import com.apiconsumer.domain.model.ContentType
import com.apiconsumer.domain.model.HttpMethod
import com.apiconsumer.domain.model.RequestFile
import com.apiconsumer.domain.model.RequestHeader
import jakarta.validation.constraints.NotBlank

data class ProxyRequestDto(
    @field:NotBlank val url: String = "",
    val method: String = "GET",
    val headers: List<HeaderDto> = emptyList(),
    val body: String? = null,
    val bypassSsl: Boolean = false,
    val certificate: String? = null,
    val credentialsSecretName: String? = null,
    val contentType: String = "json",
    val files: List<FileDto>? = null,
    val formData: Map<String, String>? = null,
) {
    fun toDomain() = ApiRequest(
        url = url,
        method = HttpMethod.valueOf(method.uppercase()),
        headers = headers.map { it.toDomain() },
        body = body,
        bypassSsl = bypassSsl,
        certificate = certificate?.ifBlank { null },
        credentialsSecretName = credentialsSecretName?.ifBlank { null },
        contentType = when (contentType.lowercase()) {
            "form-data"             -> ContentType.FORM_DATA
            "x-www-form-urlencoded" -> ContentType.X_WWW_FORM_URLENCODED
            "raw"                   -> ContentType.RAW
            else                    -> ContentType.JSON
        },
        files = files?.map { it.toDomain() },
        formData = formData,
    )
}

data class HeaderDto(
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true,
) {
    fun toDomain() = RequestHeader(key, value, enabled)
}

data class FileDto(
    val id: String = "",
    val name: String = "",
    val size: Long = 0,
    val type: String = "",
    val content: String = "",
    val fieldName: String? = null,
) {
    fun toDomain() = RequestFile(id, name, size, type, content, fieldName)
}
