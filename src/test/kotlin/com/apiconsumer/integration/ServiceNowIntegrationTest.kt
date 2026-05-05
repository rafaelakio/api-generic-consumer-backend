package com.apiconsumer.integration

import com.apiconsumer.application.service.ServiceNowValidationService
import com.apiconsumer.domain.model.ChangeType
import com.apiconsumer.domain.model.ServiceNowValidationRequest
import com.apiconsumer.domain.port.output.HttpClientPort
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils

@SpringBootTest
@ActiveProfiles("test")
class ServiceNowIntegrationTest {

    @Autowired
    private lateinit var serviceNowValidationService: ServiceNowValidationService

    @Test
    fun `should validate ServiceNow integration end-to-end`() {
        // Setup test configuration
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowEnabled", true)
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowBaseUrl", "https://test.service-now.com")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowUsername", "test")
        ReflectionTestUtils.setField(serviceNowValidationService, "serviceNowPassword", "test")

        // Test CHG validation
        val chgRequest = ServiceNowValidationRequest(
            changeNumber = "CHG001234",
            apiUrl = "https://test.service-now.com/api/now/table/incident"
        )
        
        val chgResponse = serviceNowValidationService.validate(chgRequest)
        assert(chgResponse.isServiceNowApi)
        assert(chgResponse.changeType == ChangeType.CHG)
        assert(chgResponse.isValid)

        // Test INC validation
        val incRequest = ServiceNowValidationRequest(
            changeNumber = "INC001234",
            apiUrl = "https://api.example.com/test"
        )
        
        val incResponse = serviceNowValidationService.validate(incRequest)
        assert(!incResponse.isServiceNowApi)
        assert(incResponse.changeType == ChangeType.INC)
        assert(incResponse.isValid)
    }
}
