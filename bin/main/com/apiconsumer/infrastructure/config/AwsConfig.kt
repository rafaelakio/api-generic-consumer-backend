package com.apiconsumer.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Configuration
class AwsConfig(@Value("\${aws.region}") private val region: String) {

    @Bean
    fun secretsManagerClient(): SecretsManagerClient =
        SecretsManagerClient.builder()
            .region(Region.of(region))
            // In EKS, the pod's IRSA role is picked up automatically via
            // the default credentials provider chain (WebIdentityTokenFileCredentialsProvider).
            // No explicit credentials are needed here.
            .build()
}
