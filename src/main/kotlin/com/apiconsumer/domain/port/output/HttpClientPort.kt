package com.apiconsumer.domain.port.output

import com.apiconsumer.domain.model.ApiRequest
import com.apiconsumer.domain.model.ApiResponse

interface HttpClientPort {
    fun execute(request: ApiRequest, authorizationHeader: String?): ApiResponse
}
