package com.apiconsumer.infrastructure.adapter.input.web

import com.apiconsumer.domain.port.input.WriteAuditLogUseCase
import com.apiconsumer.infrastructure.adapter.input.web.dto.AuditLogRequestDto
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/audit-log")
class AuditLogController(private val writeAuditLog: WriteAuditLogUseCase) {

    @PostMapping
    fun log(@Valid @RequestBody dto: AuditLogRequestDto): ResponseEntity<Map<String, Boolean>> {
        writeAuditLog.write(dto.toDomain())
        return ResponseEntity.ok(mapOf("ok" to true))
    }
}
