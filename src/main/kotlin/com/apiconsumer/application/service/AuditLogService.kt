package com.apiconsumer.application.service

import com.apiconsumer.domain.model.AuditLevel
import com.apiconsumer.domain.model.AuditLogEntry
import com.apiconsumer.domain.port.input.WriteAuditLogUseCase
import com.apiconsumer.domain.port.output.AuditLogRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepositoryPort
) : WriteAuditLogUseCase {

    private val log = LoggerFactory.getLogger(AuditLogService::class.java)

    override fun write(entry: AuditLogEntry) {
        // Log no console (mantém comportamento atual)
        logToConsole(entry)
        
        // Salva no DynamoDB
        try {
            auditLogRepository.save(entry)
        } catch (e: Exception) {
            log.error("Failed to save audit log to DynamoDB: changeNumber=${entry.changeNumber}", e)
            // Não propaga o erro para não quebrar o fluxo principal
        }
    }

    private fun logToConsole(entry: AuditLogEntry) {
        val prefix = "[${entry.changeNumber}] [${entry.timestamp}]"
        when (entry.level) {
            AuditLevel.SESSION_OPEN -> {
                log.info("\n${"=".repeat(80)}\n$prefix ${entry.message}\n${"=".repeat(80)}")
            }
            AuditLevel.CALL -> {
                log.info("$prefix ${entry.message}")
            }
            AuditLevel.SESSION_CLOSE -> {
                log.info("${"-".repeat(80)}\n$prefix ${entry.message}")
            }
            AuditLevel.SESSION_SUMMARY -> {
                entry.table?.takeIf { it.isNotEmpty() }?.let { table ->
                    val header = table.firstOrNull()?.keys?.joinToString(" | ") ?: ""
                    val rows = table.joinToString("\n") { row -> row.values.joinToString(" | ") }
                    log.info("$prefix URLs consumidas:\n$header\n$rows")
                }
                log.info("=".repeat(80))
            }
            AuditLevel.FULL_REPORT -> {
                log.info("\n${entry.message}\n")
            }
        }
    }
}
