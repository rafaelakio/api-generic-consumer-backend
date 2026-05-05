package com.apiconsumer.application.service

import com.apiconsumer.domain.model.ApiRequest
import com.apiconsumer.domain.model.ApiResponse
import com.apiconsumer.domain.model.ServiceNowValidationRequest
import com.apiconsumer.domain.port.input.ExecuteProxyUseCase
import com.apiconsumer.domain.port.input.ValidateServiceNowUseCase
import com.apiconsumer.domain.port.output.HttpClientPort
import com.apiconsumer.domain.port.output.SecretsManagerPort
import com.apiconsumer.domain.port.output.TokenProviderPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProxyService(
    private val httpClient: HttpClientPort,
    private val secretsManager: SecretsManagerPort,
    private val tokenProvider: TokenProviderPort,
    private val serviceNowValidator: ValidateServiceNowUseCase?,
    @Value("\${app.secrets.api-credentials-name}") private val defaultSecretName: String,
) : ExecuteProxyUseCase {

    private val log = LoggerFactory.getLogger(ProxyService::class.java)

    override fun execute(request: ApiRequest): ApiResponse {
        // Validate ServiceNow if change number is provided
        request.changeNumber?.let { changeNumber ->
            validateServiceNowChange(changeNumber, request.url)
        }
        
        val authHeader = resolveAuthorizationHeader(request)
        return httpClient.execute(request, authHeader)
    }
    
    private fun validateServiceNowChange(changeNumber: String, apiUrl: String) {
        serviceNowValidator?.let { validator ->
            val validationRequest = ServiceNowValidationRequest(
                changeNumber = changeNumber,
                apiUrl = apiUrl
            )
            val validation = validator.validate(validationRequest)
            
            log.info("ServiceNow validation for $changeNumber: ${validation.message}")
            
            if (!validation.isValid) {
                log.warn("ServiceNow validation failed: ${validation.message}")
                throw IllegalStateException("ServiceNow validation failed: ${validation.message}")
            }
        }
    }

    private fun resolveAuthorizationHeader(request: ApiRequest): String? {
        val secretName = request.credentialsSecretName ?: defaultSecretName
        return try {
            val credentials = secretsManager.getApiCredentials(secretName)
            val token = tokenProvider.getToken(credentials, secretName)
            "Bearer $token"
        } catch (ex: Exception) {
            if (request.credentialsSecretName != null) {
                // Caller explicitly requested a named secret — hard fail
                throw IllegalStateException(
                    "Failed to retrieve credentials secret \"$secretName\": ${ex.message}", ex
                )
            }
            // Default secret not found or not configured — proceed without auth
            log.debug("Default credentials secret not available, proceeding without Authorization header")
            null
        }
    }
}
