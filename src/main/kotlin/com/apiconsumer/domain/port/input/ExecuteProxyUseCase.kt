package com.apiconsumer.domain.port.input

import com.apiconsumer.domain.model.ApiRequest
import com.apiconsumer.domain.model.ApiResponse

interface ExecuteProxyUseCase {
    fun execute(request: ApiRequest): ApiResponse
}
