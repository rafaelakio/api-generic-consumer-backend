package com.apiconsumer.domain.port.output

import com.apiconsumer.domain.model.AuditLogEntry

interface AuditLogRepositoryPort {
    fun save(entry: AuditLogEntry)
    fun findByChangeNumber(changeNumber: String): List<AuditLogEntry>
}

// Made with Bob
