package com.apiconsumer.domain.model

data class ServiceNowValidationRequest(
    val changeNumber: String,
    val apiUrl: String
)

data class ServiceNowValidationResponse(
    val isServiceNowApi: Boolean,
    val changeType: ChangeType,
    val isValid: Boolean,
    val message: String
)

enum class ChangeType {
    CHG,  // ServiceNow Change
    INC,  // Incident/Other
    UNKNOWN
}

data class ServiceNowConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val validateChgPattern: Boolean = true,
    val validateIncPattern: Boolean = true
)
