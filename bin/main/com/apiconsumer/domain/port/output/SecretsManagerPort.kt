package com.apiconsumer.domain.port.output

import com.apiconsumer.domain.model.ApiCredentials

interface SecretsManagerPort {
    fun getApiCredentials(secretName: String): ApiCredentials
}
