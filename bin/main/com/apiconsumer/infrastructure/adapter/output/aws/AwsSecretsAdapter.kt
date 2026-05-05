package com.apiconsumer.infrastructure.adapter.output.aws

import com.apiconsumer.domain.model.ApiCredentials
import com.apiconsumer.domain.port.output.SecretsManagerPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

@Component
class AwsSecretsAdapter(
    private val secretsManagerClient: SecretsManagerClient,
    private val objectMapper: ObjectMapper,
) : SecretsManagerPort {

    override fun getApiCredentials(secretName: String): ApiCredentials {
        val request = GetSecretValueRequest.builder().secretId(secretName).build()
        val response = secretsManagerClient.getSecretValue(request)
        val node = objectMapper.readTree(response.secretString())
        return ApiCredentials(
            clientId = node["clientId"].asText(),
            clientSecret = node["clientSecret"].asText(),
            tokenUrl = node["tokenUrl"].asText(),
            scope = node["scope"]?.asText()?.ifBlank { null },
        )
    }
}
