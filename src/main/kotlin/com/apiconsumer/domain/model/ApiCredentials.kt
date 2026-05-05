package com.apiconsumer.domain.model

data class ApiCredentials(
    val clientId: String,
    val clientSecret: String,
    val tokenUrl: String,
    val scope: String?,
)
