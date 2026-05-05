# Guia de Testes

Este documento descreve as estratégias de teste, ferramentas e práticas recomendadas para o projeto.

## 📋 Índice

- [Estratégia de Testes](#estratégia-de-testes)
- [Testes Unitários](#testes-unitários)
- [Testes de Integração](#testes-de-integração)
- [Testes End-to-End](#testes-end-to-end)
- [Cobertura de Código](#cobertura-de-código)
- [Executando Testes](#executando-testes)

## 🎯 Estratégia de Testes

### Pirâmide de Testes

```
        /\
       /  \      E2E Tests (5%)
      /____\     - Fluxos completos
     /      \    - LocalStack
    /________\   Integration Tests (15%)
   /          \  - Controllers + Services
  /____________\ - Adapters + External
 /              \ Unit Tests (80%)
/________________\ - Domain Logic
                   - Services
                   - Utilities
```

### Cobertura Alvo

| Camada | Cobertura Mínima | Cobertura Ideal |
|--------|------------------|-----------------|
| Domain | 100% | 100% |
| Application | 90% | 95% |
| Infrastructure | 70% | 80% |
| **Total** | **80%** | **90%** |

## 🧪 Testes Unitários

### Ferramentas

- **JUnit 5**: Framework de testes
- **MockK**: Mocking para Kotlin
- **AssertJ**: Assertions fluentes
- **SpringMockK**: Integração Spring + MockK

### Estrutura

```kotlin
@Test
fun `should execute proxy request successfully`() {
    // Given (Arrange)
    val request = ApiRequest(
        url = "https://api.example.com",
        method = "GET"
    )
    every { httpClient.execute(any()) } returns ApiResponse(200, "OK")
    
    // When (Act)
    val result = proxyService.execute(request)
    
    // Then (Assert)
    assertThat(result.statusCode).isEqualTo(200)
    verify { httpClient.execute(request) }
}
```

### Exemplos

#### Domain Layer

```kotlin
package com.apiconsumer.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ApiRequestTest {

    @Test
    fun `should create valid API request`() {
        // Given
        val url = "https://api.example.com"
        val method = "GET"
        
        // When
        val request = ApiRequest(url = url, method = method)
        
        // Then
        assertThat(request.url).isEqualTo(url)
        assertThat(request.method).isEqualTo(method)
    }

    @Test
    fun `should validate URL format`() {
        // Given
        val invalidUrl = "not-a-url"
        
        // When/Then
        assertThrows<IllegalArgumentException> {
            ApiRequest(url = invalidUrl, method = "GET")
        }
    }
}
```

#### Application Layer

```kotlin
package com.apiconsumer.application.service

import com.apiconsumer.domain.model.*
import com.apiconsumer.domain.port.output.*
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProxyServiceTest {

    private lateinit var httpClient: HttpClientPort
    private lateinit var tokenProvider: TokenProviderPort
    private lateinit var secretsManager: SecretsManagerPort
    private lateinit var proxyService: ProxyService

    @BeforeEach
    fun setup() {
        httpClient = mockk()
        tokenProvider = mockk()
        secretsManager = mockk()
        proxyService = ProxyService(httpClient, tokenProvider, secretsManager)
    }

    @Test
    fun `should execute GET request without OAuth`() {
        // Given
        val request = ApiRequest(
            url = "https://api.example.com/data",
            method = "GET",
            useOAuth = false
        )
        val expectedResponse = ApiResponse(200, mapOf(), """{"data":"value"}""")
        
        every { httpClient.execute(any()) } returns expectedResponse
        
        // When
        val result = proxyService.execute(request)
        
        // Then
        assertThat(result.statusCode).isEqualTo(200)
        verify(exactly = 0) { tokenProvider.getToken(any()) }
        verify(exactly = 1) { httpClient.execute(request) }
    }

    @Test
    fun `should execute POST request with OAuth token`() {
        // Given
        val credentials = ApiCredentials(
            clientId = "client-id",
            clientSecret = "secret",
            tokenUrl = "https://oauth.example.com/token"
        )
        val token = "access-token-123"
        val request = ApiRequest(
            url = "https://api.example.com/data",
            method = "POST",
            body = mapOf("key" to "value"),
            useOAuth = true
        )
        
        every { secretsManager.getCredentials(any()) } returns credentials
        every { tokenProvider.getToken(credentials) } returns token
        every { httpClient.execute(any()) } returns ApiResponse(201, mapOf(), "")
        
        // When
        val result = proxyService.execute(request)
        
        // Then
        assertThat(result.statusCode).isEqualTo(201)
        verify { secretsManager.getCredentials(any()) }
        verify { tokenProvider.getToken(credentials) }
        verify { httpClient.execute(match { it.headers["Authorization"] == "Bearer $token" }) }
    }
}
```

#### Infrastructure Layer

```kotlin
package com.apiconsumer.infrastructure.adapter.output.dynamodb

import com.apiconsumer.domain.model.AuditLevel
import com.apiconsumer.domain.model.AuditLogEntry
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable

class DynamoDbAuditLogAdapterTest {

    private lateinit var dynamoDbClient: DynamoDbEnhancedClient
    private lateinit var table: DynamoDbTable<AuditLogItem>
    private lateinit var adapter: DynamoDbAuditLogAdapter

    @BeforeEach
    fun setup() {
        dynamoDbClient = mockk()
        table = mockk()
        every { dynamoDbClient.table(any(), any<TableSchema<AuditLogItem>>()) } returns table
        adapter = DynamoDbAuditLogAdapter(dynamoDbClient, "audit-logs")
    }

    @Test
    fun `should save audit log entry`() {
        // Given
        val entry = AuditLogEntry(
            changeNumber = "CHG001",
            level = AuditLevel.CALL,
            message = "Test message",
            timestamp = "2024-05-05T10:00:00Z"
        )
        
        every { table.putItem(any<AuditLogItem>()) } just Runs
        
        // When
        adapter.save(entry)
        
        // Then
        verify { table.putItem(match { 
            it.changeNumber == "CHG001" && 
            it.level == "CALL" 
        }) }
    }
}
```

### Executar Testes Unitários

```bash
# Todos os testes unitários
./gradlew test

# Testes de um pacote específico
./gradlew test --tests "com.apiconsumer.domain.*"

# Teste específico
./gradlew test --tests "ProxyServiceTest"

# Com relatório detalhado
./gradlew test --info
```

## 🔗 Testes de Integração

### Configuração

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProxyControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var proxyService: ProxyService

    @Test
    fun `should execute proxy request via REST endpoint`() {
        // Given
        val request = """
            {
                "url": "https://api.example.com",
                "method": "GET"
            }
        """.trimIndent()
        
        val response = ApiResponse(200, mapOf(), """{"data":"value"}""")
        every { proxyService.execute(any()) } returns response
        
        // When/Then
        mockMvc.perform(
            post("/api/proxy")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .content(request)
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.statusCode").value(200))
    }
}
```

### Testes com LocalStack

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
class DynamoDbIntegrationTest {

    companion object {
        @Container
        val localstack = LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.DYNAMODB)
    }

    @Autowired
    private lateinit var auditLogAdapter: DynamoDbAuditLogAdapter

    @Test
    fun `should save and retrieve audit log from DynamoDB`() {
        // Given
        val entry = AuditLogEntry(
            changeNumber = "CHG001",
            level = AuditLevel.CALL,
            message = "Integration test",
            timestamp = "2024-05-05T10:00:00Z"
        )
        
        // When
        auditLogAdapter.save(entry)
        val retrieved = auditLogAdapter.findByChangeNumber("CHG001")
        
        // Then
        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].message).isEqualTo("Integration test")
    }
}
```

### Executar Testes de Integração

```bash
# Testes de integração
./gradlew integrationTest

# Com LocalStack
docker-compose -f docker-compose.test.yml up -d
./gradlew integrationTest
docker-compose -f docker-compose.test.yml down
```

## 🌐 Testes End-to-End

### Setup

```bash
# Iniciar ambiente completo
docker-compose -f docker-compose.e2e.yml up -d

# Aguardar serviços ficarem prontos
./scripts/wait-for-services.sh
```

### Cenários de Teste

#### 1. Fluxo Completo de Proxy

```bash
#!/bin/bash

# 1. Obter token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# 2. Executar proxy request
RESPONSE=$(curl -s -X POST http://localhost:8080/api/proxy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://jsonplaceholder.typicode.com/posts/1",
    "method": "GET"
  }')

echo "Response: $RESPONSE"

# 3. Verificar status code
STATUS=$(echo $RESPONSE | jq -r '.statusCode')
if [ "$STATUS" -eq 200 ]; then
    echo "✅ Test passed"
else
    echo "❌ Test failed: Expected 200, got $STATUS"
    exit 1
fi
```

#### 2. Verificar Audit Logs no DynamoDB

```bash
#!/bin/bash

# Executar algumas requisições
for i in {1..5}; do
    curl -s -X POST http://localhost:8080/api/audit-log \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"changeNumber\": \"CHG001\",
        \"level\": \"CALL\",
        \"message\": \"Test call $i\",
        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
      }" > /dev/null
done

# Verificar logs no DynamoDB
COUNT=$(awslocal dynamodb query \
    --table-name audit-logs \
    --key-condition-expression "changeNumber = :cn" \
    --expression-attribute-values '{":cn":{"S":"CHG001"}}' \
    --endpoint-url http://localhost:4566 \
    | jq '.Count')

if [ "$COUNT" -ge 5 ]; then
    echo "✅ Audit logs saved correctly"
else
    echo "❌ Expected at least 5 logs, found $COUNT"
    exit 1
fi
```

### Executar Testes E2E

```bash
# Executar todos os testes E2E
./scripts/run-e2e-tests.sh

# Ou manualmente
docker-compose -f docker-compose.e2e.yml up -d
sleep 30  # Aguardar inicialização
./tests/e2e/test-proxy-flow.sh
./tests/e2e/test-audit-logs.sh
docker-compose -f docker-compose.e2e.yml down
```

## 📊 Cobertura de Código

### Gerar Relatório

```bash
# Executar testes com cobertura
./gradlew test jacocoTestReport

# Abrir relatório
open build/reports/jacoco/test/html/index.html
```

### Verificar Cobertura Mínima

```bash
# Falha se cobertura < 80%
./gradlew test jacocoTestCoverageVerification
```

### Configuração Jacoco

```kotlin
// build.gradle.kts
jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
```

## 🎭 Mocking Best Practices

### Use MockK

```kotlin
// ✅ BOM
val httpClient = mockk<HttpClientPort>()
every { httpClient.execute(any()) } returns ApiResponse(200, mapOf(), "")

// ❌ RUIM - Não use Mockito em Kotlin
val httpClient = mock(HttpClientPort::class.java)
```

### Verify Interactions

```kotlin
// Verificar chamada exata
verify(exactly = 1) { httpClient.execute(request) }

// Verificar que não foi chamado
verify(exactly = 0) { tokenProvider.getToken(any()) }

// Verificar ordem
verifyOrder {
    secretsManager.getCredentials(any())
    tokenProvider.getToken(any())
    httpClient.execute(any())
}
```

### Capture Arguments

```kotlin
val slot = slot<ApiRequest>()
every { httpClient.execute(capture(slot)) } returns ApiResponse(200, mapOf(), "")

proxyService.execute(request)

assertThat(slot.captured.url).isEqualTo("https://api.example.com")
```

## 🐛 Debugging Tests

### IntelliJ IDEA

1. Clique com botão direito no teste
2. Selecione "Debug 'TestName'"
3. Adicione breakpoints

### Logs Detalhados

```kotlin
@Test
fun `test with detailed logging`() {
    // Habilitar logs
    val logger = LoggerFactory.getLogger(ProxyService::class.java)
    (logger as ch.qos.logback.classic.Logger).level = Level.DEBUG
    
    // Executar teste
    proxyService.execute(request)
}
```

### Print Stack Traces

```bash
# Executar com stack traces
./gradlew test --stacktrace

# Ou com mais detalhes
./gradlew test --debug
```

## 📝 Convenções de Nomenclatura

### Testes

```kotlin
// ✅ BOM - Descritivo e legível
@Test
fun `should return 200 when proxy request succeeds`()

@Test
fun `should throw exception when URL is invalid`()

@Test
fun `should save audit log to DynamoDB`()

// ❌ RUIM - Não descritivo
@Test
fun testProxy()

@Test
fun test1()
```

### Arquivos

```
src/test/kotlin/
├── com/apiconsumer/
│   ├── domain/
│   │   └── model/
│   │       └── ApiRequestTest.kt
│   ├── application/
│   │   └── service/
│   │       └── ProxyServiceTest.kt
│   └── infrastructure/
│       └── adapter/
│           └── DynamoDbAuditLogAdapterTest.kt
```

## 🚀 CI/CD Integration

### GitHub Actions

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run tests
        run: ./gradlew test
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

## 📚 Recursos Adicionais

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [MockK Documentation](https://mockk.io/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

---

**Última atualização**: 2024-05-05