package com.apiconsumer.infrastructure.adapter.input.web

import com.apiconsumer.domain.model.ServiceNowValidationRequest
import com.apiconsumer.domain.model.ServiceNowValidationResponse
import com.apiconsumer.domain.port.input.ValidateServiceNowUseCase
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/servicenow")
class ServiceNowController(private val validateServiceNow: ValidateServiceNowUseCase) {

    private val log = LoggerFactory.getLogger(ServiceNowController::class.java)

    @PostMapping("/validate")
    fun validate(@Valid @RequestBody request: ServiceNowValidationRequest): ResponseEntity<ServiceNowValidationResponse> {
        return try {
            val response = validateServiceNow.validate(request)
            log.info("ServiceNow validation completed for ${request.changeNumber}: ${response.message}")
            ResponseEntity.ok(response)
        } catch (ex: Exception) {
            log.error("ServiceNow validation failed", ex)
            ResponseEntity.internalServerError().body(
                ServiceNowValidationResponse(
                    isServiceNowApi = false,
                    changeType = com.apiconsumer.domain.model.ChangeType.UNKNOWN,
                    isValid = false,
                    message = "Validation failed: ${ex.message}"
                )
            )
        }
    }
}
