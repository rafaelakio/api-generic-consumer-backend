package com.apiconsumer.domain.port.input

import com.apiconsumer.domain.model.ServiceNowValidationRequest
import com.apiconsumer.domain.model.ServiceNowValidationResponse

interface ValidateServiceNowUseCase {
    fun validate(request: ServiceNowValidationRequest): ServiceNowValidationResponse
}
