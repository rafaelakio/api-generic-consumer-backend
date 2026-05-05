package com.apiconsumer.infrastructure.config

import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.SecretKey

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") private val jwksUri: String,
    @Value("\${app.admin.jwt-secret}") private val adminJwtSecret: String,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/token").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt -> jwt.decoder(jwtDecoder()) }
            }

        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val adminKey: SecretKey = Keys.hmacShaKeyFor(adminJwtSecret.toByteArray()) as SecretKey
        val adminDecoder = NimbusJwtDecoder.withSecretKey(adminKey).build()
        val azureDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build()

        // Try the local HMAC decoder first (no network call), then Azure AD JWKS.
        return JwtDecoder { token ->
            var decoded: Jwt? = null
            try { decoded = adminDecoder.decode(token) } catch (_: JwtException) {}
            decoded ?: azureDecoder.decode(token)
        }
    }
}
