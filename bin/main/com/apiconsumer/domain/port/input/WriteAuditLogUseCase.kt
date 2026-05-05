package com.apiconsumer.domain.port.input

import com.apiconsumer.domain.model.AuditLogEntry

interface WriteAuditLogUseCase {
    fun write(entry: AuditLogEntry)
}
