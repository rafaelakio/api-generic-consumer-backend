package com.apiconsumer.infrastructure.adapter.input.web

import com.apiconsumer.domain.model.ApiResponse
import com.apiconsumer.domain.port.input.ExecuteProxyUseCase
import com.apiconsumer.infrastructure.adapter.input.web.dto.ProxyRequestDto
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/proxy")
class ProxyController(private val executeProxy: ExecuteProxyUseCase) {

    private val log = LoggerFactory.getLogger(ProxyController::class.java)

    @PostMapping
    fun proxy(@Valid @RequestBody dto: ProxyRequestDto): ResponseEntity<Map<String, ApiResponse>> {
        return try {
            val response = executeProxy.execute(dto.toDomain())
            ResponseEntity.ok(mapOf("data" to response))
        } catch (ex: IllegalStateException) {
            log.warn("Proxy credential error: {}", ex.message)
            ResponseEntity.badRequest().body(
                mapOf("data" to ApiResponse(
                    statusCode = 400,
                    statusText = "Bad Request",
                    headers = emptyMap(),
                    body = mapOf("error" to (ex.message ?: "Credential error")),
                    responseTimeMs = 0,
                    error = ex.message,
                ))
            )
        } catch (ex: Exception) {
            log.error("Proxy execution failed", ex)
            ResponseEntity.internalServerError().body(
                mapOf("data" to ApiResponse(
                    statusCode = 502,
                    statusText = "Bad Gateway",
                    headers = emptyMap(),
                    body = mapOf("error" to "Request execution failed"),
                    responseTimeMs = 0,
                    error = ex.message,
                ))
            )
        }
    }
}
