package com.apiconsumer.application.service

import com.apiconsumer.domain.model.ChangeType
import com.apiconsumer.domain.model.ServiceNowValidationRequest
import com.apiconsumer.domain.model.ServiceNowValidationResponse
import com.apiconsumer.domain.port.input.ValidateServiceNowUseCase
import com.apiconsumer.domain.port.output.HttpClientPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class ServiceNowValidationService(
    private val httpClient: HttpClientPort,
    @Value("\${app.servicenow.base-url:}") private val serviceNowBaseUrl: String,
    @Value("\${app.servicenow.username:}") private val serviceNowUsername: String,
    @Value("\${app.servicenow.password:}") private val serviceNowPassword: String,
    @Value("\${app.servicenow.enabled:false}") private val serviceNowEnabled: Boolean
) : ValidateServiceNowUseCase {

    private val log = LoggerFactory.getLogger(ServiceNowValidationService::class.java)
    
    // Patterns for ServiceNow change numbers
    private val chgPattern = Pattern.compile("^CHG\\d{6,}$", Pattern.CASE_INSENSITIVE)
    private val incPattern = Pattern.compile("^INC\\d{6,}$", Pattern.CASE_INSENSITIVE)
    
    override fun validate(request: ServiceNowValidationRequest): ServiceNowValidationResponse {
        log.debug("Validating ServiceNow API for change number: ${request.changeNumber}")
        
        // If ServiceNow validation is disabled, return default response
        if (!serviceNowEnabled) {
            return ServiceNowValidationResponse(
                isServiceNowApi = false,
                changeType = ChangeType.UNKNOWN,
                isValid = true,
                message = "ServiceNow validation is disabled"
            )
        }
        
        val changeType = determineChangeType(request.changeNumber)
        
        return when (changeType) {
            ChangeType.CHG -> {
                log.info("CHG detected: ${request.changeNumber} - Validating as ServiceNow API")
                validateServiceNowApi(request, ChangeType.CHG)
            }
            ChangeType.INC -> {
                log.info("INC detected: ${request.changeNumber} - Not a ServiceNow API")
                ServiceNowValidationResponse(
                    isServiceNowApi = false,
                    changeType = ChangeType.INC,
                    isValid = true,
                    message = "INC change number detected - Not a ServiceNow API change"
                )
            }
            ChangeType.UNKNOWN -> {
                log.warn("Unknown change number format: ${request.changeNumber}")
                ServiceNowValidationResponse(
                    isServiceNowApi = false,
                    changeType = ChangeType.UNKNOWN,
                    isValid = false,
                    message = "Invalid change number format. Expected CHG or INC prefix."
                )
            }
        }
    }
    
    private fun determineChangeType(changeNumber: String): ChangeType {
        return when {
            chgPattern.matcher(changeNumber).matches() -> ChangeType.CHG
            incPattern.matcher(changeNumber).matches() -> ChangeType.INC
            else -> ChangeType.UNKNOWN
        }
    }
    
    private fun validateServiceNowApi(request: ServiceNowValidationRequest, changeType: ChangeType): ServiceNowValidationResponse {
        if (serviceNowBaseUrl.isBlank() || serviceNowUsername.isBlank() || serviceNowPassword.isBlank()) {
            log.warn("ServiceNow configuration is incomplete")
            return ServiceNowValidationResponse(
                isServiceNowApi = true,
                changeType = changeType,
                isValid = false,
                message = "ServiceNow configuration is incomplete"
            )
        }
        
        return try {
            // Validate if the API URL is accessible and follows ServiceNow patterns
            val isValidServiceNowUrl = isValidServiceNowUrl(request.apiUrl)
            
            if (isValidServiceNowUrl) {
                ServiceNowValidationResponse(
                    isServiceNowApi = true,
                    changeType = changeType,
                    isValid = true,
                    message = "Valid ServiceNow API for CHG ${request.changeNumber}"
                )
            } else {
                ServiceNowValidationResponse(
                    isServiceNowApi = true,
                    changeType = changeType,
                    isValid = false,
                    message = "URL does not appear to be a valid ServiceNow API endpoint"
                )
            }
        } catch (ex: Exception) {
            log.error("Error validating ServiceNow API", ex)
            ServiceNowValidationResponse(
                isServiceNowApi = true,
                changeType = changeType,
                isValid = false,
                message = "Error validating ServiceNow API: ${ex.message}"
            )
        }
    }
    
    private fun isValidServiceNowUrl(apiUrl: String): Boolean {
        // Check if URL matches ServiceNow API patterns
        val serviceNowUrlPatterns = listOf(
            ".*/api/now/.*",           // ServiceNow Table API
            ".*/api/itsm/.*",          // ServiceNow ITSM API
            ".*/sys_script.*",         // ServiceNow scripts
            ".*/sys_attachment.*",     // ServiceNow attachments
            ".*/incident.*",           // ServiceNow incidents
            ".*/change_request.*",     // ServiceNow change requests
            ".*/task.*"                // ServiceNow tasks
        )
        
        return serviceNowUrlPatterns.any { pattern ->
            Regex(pattern).matches(apiUrl)
        }
    }
}
