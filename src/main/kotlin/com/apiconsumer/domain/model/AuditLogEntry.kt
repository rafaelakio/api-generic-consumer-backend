package com.apiconsumer.domain.model

data class AuditLogEntry(
    val changeNumber: String,
    val level: AuditLevel,
    val message: String,
    val timestamp: String,
    val table: List<Map<String, Any>>? = null,
)

enum class AuditLevel {
    SESSION_OPEN, CALL, SESSION_CLOSE, SESSION_SUMMARY, FULL_REPORT
}
