package com.apiconsumer.infrastructure.adapter.output.dynamodb

import com.apiconsumer.domain.model.AuditLevel
import com.apiconsumer.domain.model.AuditLogEntry
import com.apiconsumer.domain.port.output.AuditLogRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import java.time.Instant

@Component
class DynamoDbAuditLogAdapter(
    private val dynamoDbClient: DynamoDbEnhancedClient,
    @Value("\${aws.dynamodb.table-name:audit-logs}")
    private val tableName: String
) : AuditLogRepositoryPort {

    private val log = LoggerFactory.getLogger(DynamoDbAuditLogAdapter::class.java)

    private val table: DynamoDbTable<AuditLogItem> by lazy {
        dynamoDbClient.table(tableName, TableSchema.fromBean(AuditLogItem::class.java))
    }

    override fun save(entry: AuditLogEntry) {
        try {
            val item = AuditLogItem(
                changeNumber = entry.changeNumber,
                timestamp = entry.timestamp,
                level = entry.level.name,
                message = entry.message,
                tableData = entry.table?.let { convertTableToJson(it) },
                ttl = calculateTtl()
            )
            
            table.putItem(item)
            log.debug("Audit log saved to DynamoDB: changeNumber={}, level={}", 
                entry.changeNumber, entry.level)
        } catch (e: Exception) {
            log.error("Failed to save audit log to DynamoDB", e)
            throw e
        }
    }

    override fun findByChangeNumber(changeNumber: String): List<AuditLogEntry> {
        try {
            val queryConditional = QueryConditional.keyEqualTo(
                Key.builder()
                    .partitionValue(changeNumber)
                    .build()
            )

            val items = table.query(queryConditional)
                .items()
                .toList()

            return items.map { item ->
                AuditLogEntry(
                    changeNumber = item.changeNumber,
                    timestamp = item.timestamp,
                    level = AuditLevel.valueOf(item.level),
                    message = item.message,
                    table = item.tableData?.let { parseJsonToTable(it) }
                )
            }
        } catch (e: Exception) {
            log.error("Failed to query audit logs from DynamoDB", e)
            return emptyList()
        }
    }

    private fun calculateTtl(): Long {
        // TTL de 30 dias a partir de agora
        return Instant.now().plusSeconds(30L * 24 * 60 * 60).epochSecond
    }

    private fun convertTableToJson(table: List<Map<String, Any>>): String {
        return try {
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(table)
        } catch (e: Exception) {
            log.warn("Failed to convert table to JSON", e)
            "[]"
        }
    }

    private fun parseJsonToTable(json: String): List<Map<String, Any>>? {
        return try {
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .readValue(json, List::class.java) as? List<Map<String, Any>>
        } catch (e: Exception) {
            log.warn("Failed to parse JSON to table", e)
            null
        }
    }
}

@DynamoDbBean
data class AuditLogItem(
    @get:DynamoDbPartitionKey
    var changeNumber: String = "",
    
    @get:DynamoDbSortKey
    var timestamp: String = "",
    
    var level: String = "",
    var message: String = "",
    var tableData: String? = null,
    var ttl: Long = 0
)

// Made with Bob
