package com.apiconsumer.infrastructure.adapter.input.web

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * Admin login endpoint — issues a self-signed JWT for local/dev access.
 * Secured by ADMIN_USERNAME / ADMIN_PASSWORD env vars.
 * In production, disable this controller via app.admin.enabled=false
 * and rely solely on Azure AD tokens.
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    @Value("\${app.admin.username}") private val adminUsername: String,
    @Value("\${app.admin.password}") private val adminPassword: String,
    @Value("\${app.admin.jwt-secret}") private val jwtSecret: String,
    @Value("\${app.admin.jwt-expiration-ms}") private val jwtExpirationMs: Long,
) {

    data class LoginRequest(val username: String = "", val password: String = "")
    data class TokenResponse(val token: String, val type: String = "Bearer")

    @PostMapping("/token")
    fun login(@RequestBody body: LoginRequest): ResponseEntity<Any> {
        if (body.username != adminUsername || body.password != adminPassword) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid credentials"))
        }

        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        val token = Jwts.builder()
            .subject(body.username)
            .claim("name", body.username)
            .claim("email", "${body.username}@local")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key)
            .compact()

        return ResponseEntity.ok(TokenResponse(token))
    }
}
