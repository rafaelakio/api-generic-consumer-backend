package com.apiconsumer.application.service

import com.apiconsumer.domain.model.ChangeType
import com.apiconsumer.domain.model.ServiceNowValidationRequest
import com.apiconsumer.domain.model.ServiceNowValidationResponse
import com.apiconsumer.domain.port.output.HttpClientPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class ServiceNowValidationServiceTest {

    @Mock
    private lateinit var httpClient: HttpClientPort

    @InjectMocks
    private lateinit var serviceNowValidationService: ServiceNowValidationService

    @Test
    fun `should return disabled response when ServiceNow is disabled`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", false)
        val request = ServiceNowValidationRequest(
            changeNumber = "CHG123456",
            apiUrl = "https://example.service-now.com/api/now/table/incident"
        )

        // When
        val response = serviceNowValidationService.validate(request)

        // Then
        assertFalse(response.isServiceNowApi)
        assertEquals(ChangeType.UNKNOWN, response.changeType)
        assertTrue(response.isValid)
        assertEquals("ServiceNow validation is disabled", response.message)
    }

    @Test
    fun `should validate CHG change number as ServiceNow API`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowBaseUrl", "https://example.service-now.com")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowUsername", "admin")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowPassword", "password")
        
        val request = ServiceNowValidationRequest(
            changeNumber = "CHG123456",
            apiUrl = "https://example.service-now.com/api/now/table/incident"
        )

        // When
        val response = serviceNowValidationService.validate(request)

        // Then
        assertTrue(response.isServiceNowApi)
        assertEquals(ChangeType.CHG, response.changeType)
        assertTrue(response.isValid)
        assertTrue(response.message.contains("Valid ServiceNow API for CHG"))
    }

    @Test
    fun `should validate INC change number as non-ServiceNow API`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        
        val request = ServiceNowValidationRequest(
            changeNumber = "INC123456",
            apiUrl = "https://api.example.com/endpoint"
        )

        // When
        val response = serviceNowValidationService.validate(request)

        // Then
        assertFalse(response.isServiceNowApi)
        assertEquals(ChangeType.INC, response.changeType)
        assertTrue(response.isValid)
        assertTrue(response.message.contains("Not a ServiceNow API change"))
    }

    @Test
    fun `should reject unknown change number format`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        
        val request = ServiceNowValidationRequest(
            changeNumber = "UNKNOWN123",
            apiUrl = "https://api.example.com/endpoint"
        )

        // When
        val response = serviceNowValidationService.validate(request)

        // Then
        assertFalse(response.isServiceNowApi)
        assertEquals(ChangeType.UNKNOWN, response.changeType)
        assertFalse(response.isValid)
        assertTrue(response.message.contains("Invalid change number format"))
    }

    @Test
    fun `should reject CHG with invalid ServiceNow URL`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowBaseUrl", "https://example.service-now.com")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowUsername", "admin")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowPassword", "password")
        
        val request = ServiceNowValidationRequest(
            changeNumber = "CHG123456",
            apiUrl = "https://api.example.com/not-servicenow"
        )

        // When
        val response = serviceNowValidationService.validate(request)

        // Then
        assertTrue(response.isServiceNowApi)
        assertEquals(ChangeType.CHG, response.changeType)
        assertFalse(response.isValid)
        assertTrue(response.message.contains("does not appear to be a valid ServiceNow API endpoint"))
    }

    @Test
    fun `should handle incomplete ServiceNow configuration`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowBaseUrl", "") // Empty
        
        val request = ServiceNowValidationRequest(
            changeNumber = "CHG123456",
            apiUrl = "https://example.service-now.com/api/now/table/incident"
        )

        // When
        val response = serviceNowValidationService.validate(request)

        // Then
        assertTrue(response.isServiceNowApi)
        assertEquals(ChangeType.CHG, response.changeType)
        assertFalse(response.isValid)
        assertTrue(response.message.contains("ServiceNow configuration is incomplete"))
    }

    @Test
    fun `should validate various ServiceNow URL patterns`() {
        // Given
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowBaseUrl", "https://example.service-now.com")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowUsername", "admin")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowPassword", "password")
        
        val serviceNowUrls = listOf(
            "https://example.service-now.com/api/now/table/incident",
            "https://example.service-now.com/api/itsm/change_request",
            "https://example.service-now.com/sys_script.do",
            "https://example.service-now.com/sys_attachment.do",
            "https://example.service-now.com/task.do"
        )

        serviceNowUrls.forEach { url ->
            val request = ServiceNowValidationRequest(
                changeNumber = "CHG123456",
                apiUrl = url
            )

            // When
            val response = serviceNowValidationService.validate(request)

            // Then
            assertTrue(response.isServiceNowApi, "URL $url should be recognized as ServiceNow API")
            assertEquals(ChangeType.CHG, response.changeType)
            assertTrue(response.isValid, "URL $url should be valid")
        }
    }
}
