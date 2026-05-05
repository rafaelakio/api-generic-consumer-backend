package com.apiconsumer.infrastructure.adapter.output.oauth

import com.apiconsumer.domain.model.ApiCredentials
import com.apiconsumer.domain.port.output.TokenProviderPort
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.concurrent.ConcurrentHashMap

@Component
class OAuthTokenAdapter(private val webClient: WebClient) : TokenProviderPort {

    private data class CachedToken(val accessToken: String, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, CachedToken>()

    override fun getToken(credentials: ApiCredentials, cacheKey: String): String {
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() < cached.expiresAt - 60_000) {
            return cached.accessToken
        }

        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", credentials.clientId)
            add("client_secret", credentials.clientSecret)
            credentials.scope?.let { add("scope", it) }
        }

        val response = webClient.post()
            .uri(credentials.tokenUrl)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono<TokenResponse>()
            .block() ?: throw IllegalStateException("Empty token response from ${credentials.tokenUrl}")

        cache[cacheKey] = CachedToken(
            accessToken = response.accessToken,
            expiresAt = System.currentTimeMillis() + response.expiresIn * 1000L,
        )

        return response.accessToken
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TokenResponse(
        @JsonProperty("access_token") val accessToken: String,
        @JsonProperty("expires_in") val expiresIn: Long,
    )
}
