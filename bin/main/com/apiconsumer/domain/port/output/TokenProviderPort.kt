package com.apiconsumer.domain.port.output

import com.apiconsumer.domain.model.ApiCredentials

interface TokenProviderPort {
    fun getToken(credentials: ApiCredentials, cacheKey: String): String
}
