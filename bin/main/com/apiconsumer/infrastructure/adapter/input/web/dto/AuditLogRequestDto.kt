package com.apiconsumer.infrastructure.adapter.input.web.dto

import com.apiconsumer.domain.model.AuditLevel
import com.apiconsumer.domain.model.AuditLogEntry
import jakarta.validation.constraints.NotBlank

data class AuditLogRequestDto(
    @field:NotBlank val changeNumber: String = "",
    val level: String = "CALL",
    @field:NotBlank val message: String = "",
    @field:NotBlank val timestamp: String = "",
    val table: List<Map<String, Any>>? = null,
) {
    fun toDomain() = AuditLogEntry(
        changeNumber = changeNumber,
        level = AuditLevel.valueOf(level.uppercase()),
        message = message,
        timestamp = timestamp,
        table = table,
    )
}
