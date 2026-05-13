# Guia de Logging e Monitoramento

## 📋 Visão Geral

Este documento descreve a estratégia completa de logging implementada no sistema API Generic Consumer, cobrindo todas as camadas de comunicação: Frontend (Next.js) → API Gateway → Backend (Spring Boot) → External APIs.

## 🎯 Objetivos do Logging

1. **Rastreabilidade Completa**: Acompanhar cada requisição do início ao fim
2. **Troubleshooting**: Facilitar identificação e resolução de problemas
3. **Auditoria**: Manter histórico de todas as operações
4. **Performance**: Monitorar tempos de resposta e gargalos
5. **Segurança**: Detectar acessos não autorizados e comportamentos anômalos

## 🏗️ Arquitetura de Logging

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────────┐
│  Frontend   │────▶│ API Gateway  │────▶│   Backend   │────▶│ External API │
│  (Next.js)  │     │ (AWS/Local)  │     │(Spring Boot)│     │              │
└──────┬──────┘     └──────┬───────┘     └──────┬──────┘     └──────┬───────┘
       │                   │                    │                    │
       ▼                   ▼                    ▼                    ▼
  Browser Console    CloudWatch Logs      Application Logs     API Logs
  + Network Tab      + Access Logs        + File Logs          (External)
                                          + DynamoDB Audit
```

## 📝 Logging por Camada

### 1. Frontend Logging (Next.js)

#### Configuração

```typescript
// lib/logger.ts
export class Logger {
  private static formatLog(level: string, message: string, data?: any) {
    return {
      timestamp: new Date().toISOString(),
      level,
      message,
      ...data
    };
  }

  static info(message: string, data?: any) {
    console.log('[INFO]', this.formatLog('INFO', message, data));
  }

  static error(message: string, error?: Error, data?: any) {
    console.error('[ERROR]', this.formatLog('ERROR', message, {
      ...data,
      error: error?.message,
      stack: error?.stack
    }));
  }

  static debug(message: string, data?: any) {
    if (process.env.NODE_ENV === 'development') {
      console.debug('[DEBUG]', this.formatLog('DEBUG', message, data));
    }
  }
}
```

#### Pontos de Log

```typescript
// services/backend.service.ts
export class BackendService {
  async callProxy(request: ApiRequest): Promise<ApiResponse> {
    const startTime = Date.now();
    
    Logger.info('📤 Iniciando requisição para backend', {
      changeNumber: request.changeNumber,
      method: request.method,
      url: request.url
    });

    try {
      const response = await fetch('/api/proxy', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(request)
      });

      const duration = Date.now() - startTime;
      
      Logger.info('📥 Resposta recebida do backend', {
        changeNumber: request.changeNumber,
        status: response.status,
        duration: `${duration}ms`,
        size: response.headers.get('content-length')
      });

      return await response.json();
    } catch (error) {
      const duration = Date.now() - startTime;
      
      Logger.error('❌ Erro ao chamar backend', error as Error, {
        changeNumber: request.changeNumber,
        duration: `${duration}ms`
      });
      
      throw error;
    }
  }
}
```

### 2. API Gateway Logging (AWS/LocalStack)

#### Configuração CloudWatch

```yaml
# API Gateway Stage Settings
AccessLogSettings:
  DestinationArn: !GetAtt ApiGatewayLogGroup.Arn
  Format: >
    {
      "requestId": "$context.requestId",
      "ip": "$context.identity.sourceIp",
      "caller": "$context.identity.caller",
      "user": "$context.identity.user",
      "requestTime": "$context.requestTime",
      "httpMethod": "$context.httpMethod",
      "resourcePath": "$context.resourcePath",
      "status": "$context.status",
      "protocol": "$context.protocol",
      "responseLength": "$context.responseLength",
      "integrationLatency": "$context.integrationLatency",
      "responseLatency": "$context.responseLatency",
      "integrationStatus": "$context.integrationStatus",
      "error": "$context.error.message",
      "integrationError": "$context.integrationErrorMessage"
    }
```

#### Logs Capturados

- **Request ID**: Identificador único da requisição
- **Client IP**: Endereço IP do cliente
- **Timestamp**: Data/hora da requisição
- **HTTP Method**: GET, POST, PUT, DELETE, etc.
- **Resource Path**: Caminho do recurso acessado
- **Status Code**: Código de resposta HTTP
- **Latency**: Tempo de processamento
- **Errors**: Mensagens de erro, se houver

### 3. Backend Logging (Spring Boot)

#### Configuração application.yml

```yaml
logging:
  level:
    root: INFO
    com.apiconsumer: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    software.amazon.awssdk: INFO
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg - RequestId:%X{requestId} - ChangeNumber:%X{changeNumber}%n"
  file:
    name: logs/api-consumer.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

#### Logs por Componente

##### Controller Layer

```kotlin
@RestController
@RequestMapping("/api/proxy")
class ProxyController(
    private val proxyService: ProxyService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @PostMapping
    fun executeProxy(
        @RequestBody request: ApiRequest,
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse> {
        val requestId = UUID.randomUUID().toString()
        MDC.put("requestId", requestId)
        MDC.put("changeNumber", request.changeNumber)
        
        logger.info("📥 [CONTROLLER] Request recebido")
        logger.debug("📥 [CONTROLLER] Detalhes - Method: ${request.method}, URL: ${request.url}, Headers: ${request.headers.keys}")
        
        return try {
            val startTime = System.currentTimeMillis()
            val response = proxyService.execute(request)
            val duration = System.currentTimeMillis() - startTime
            
            logger.info("📤 [CONTROLLER] Response enviado - Status: ${response.statusCode}, Duration: ${duration}ms")
            logger.debug("📤 [CONTROLLER] Response Body Size: ${response.body?.length ?: 0} bytes")
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("❌ [CONTROLLER] Erro ao processar request", e)
            throw e
        } finally {
            MDC.clear()
        }
    }
}
```

##### Service Layer

```kotlin
@Service
class ProxyService(
    private val httpClient: HttpClientPort,
    private val tokenProvider: TokenProviderPort,
    private val secretsManager: SecretsManagerPort,
    private val auditService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun execute(request: ApiRequest): ApiResponse {
        logger.info("🔄 [SERVICE] Iniciando processamento")
        
        // 1. Buscar credenciais
        logger.debug("🔐 [SERVICE] Buscando credenciais do Secrets Manager")
        val credentials = secretsManager.getCredentials()
        logger.debug("✅ [SERVICE] Credenciais obtidas - ClientId: ${credentials.clientId}")
        
        // 2. Obter token OAuth
        logger.debug("🎫 [SERVICE] Obtendo token OAuth - TokenURL: ${credentials.tokenUrl}")
        val tokenStartTime = System.currentTimeMillis()
        val token = tokenProvider.getToken(credentials)
        val tokenDuration = System.currentTimeMillis() - tokenStartTime
        logger.info("✅ [SERVICE] Token OAuth obtido - Duration: ${tokenDuration}ms, Expira em: ${token.expiresIn}s")
        
        // 3. Executar requisição
        logger.info("🌐 [SERVICE] Executando requisição externa - URL: ${request.url}, Method: ${request.method}")
        val requestStartTime = System.currentTimeMillis()
        
        val response = try {
            httpClient.execute(request, token)
        } catch (e: Exception) {
            logger.error("❌ [SERVICE] Erro ao executar requisição externa", e)
            throw e
        }
        
        val requestDuration = System.currentTimeMillis() - requestStartTime
        logger.info("✅ [SERVICE] Requisição concluída - Status: ${response.statusCode}, Duration: ${requestDuration}ms")
        
        // 4. Gravar auditoria
        logger.debug("📝 [SERVICE] Gravando log de auditoria")
        try {
            auditService.write(createAuditLog(request, response, requestDuration))
            logger.debug("✅ [SERVICE] Auditoria gravada com sucesso")
        } catch (e: Exception) {
            logger.error("⚠️ [SERVICE] Erro ao gravar auditoria (não crítico)", e)
        }
        
        return response
    }
}
```

##### HTTP Client Adapter

```kotlin
@Component
class OkHttpClientAdapter(
    private val client: OkHttpClient
) : HttpClientPort {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun execute(request: ApiRequest, token: String): ApiResponse {
        logger.debug("🔌 [HTTP-CLIENT] Preparando requisição HTTP")
        logger.debug("🔌 [HTTP-CLIENT] URL: ${request.url}")
        logger.debug("🔌 [HTTP-CLIENT] Method: ${request.method}")
        logger.debug("🔌 [HTTP-CLIENT] Headers: ${request.headers.keys.joinToString(", ")}")
        
        val httpRequest = Request.Builder()
            .url(request.url)
            .method(request.method, createRequestBody(request))
            .header("Authorization", "Bearer $token")
            .apply {
                request.headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            .build()
        
        return try {
            logger.info("📡 [HTTP-CLIENT] Enviando requisição para API externa")
            val startTime = System.currentTimeMillis()
            
            val response = client.newCall(httpRequest).execute()
            val duration = System.currentTimeMillis() - startTime
            
            logger.info("📨 [HTTP-CLIENT] Resposta recebida - Status: ${response.code}, Duration: ${duration}ms")
            logger.debug("📨 [HTTP-CLIENT] Response Message: ${response.message}")
            logger.debug("📨 [HTTP-CLIENT] Response Headers: ${response.headers.names().joinToString(", ")}")
            logger.debug("📨 [HTTP-CLIENT] Response Body Size: ${response.body?.contentLength() ?: 0} bytes")
            
            convertToApiResponse(response)
        } catch (e: IOException) {
            logger.error("❌ [HTTP-CLIENT] Erro de rede ao executar requisição - URL: ${request.url}", e)
            throw ApiClientException("Erro de comunicação com API externa: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("❌ [HTTP-CLIENT] Erro inesperado ao executar requisição", e)
            throw e
        }
    }
}
```

##### DynamoDB Adapter

```kotlin
@Component
class DynamoDbAuditLogAdapter(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table-name}") private val tableName: String
) : AuditLogRepositoryPort {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun save(entry: AuditLogEntry) {
        logger.debug("💾 [DYNAMODB] Salvando entrada de auditoria")
        logger.debug("💾 [DYNAMODB] ChangeNumber: ${entry.changeNumber}, Level: ${entry.level}, Timestamp: ${entry.timestamp}")
        
        try {
            val item = mapOf(
                "changeNumber" to AttributeValue.builder().s(entry.changeNumber).build(),
                "timestamp" to AttributeValue.builder().s(entry.timestamp).build(),
                "level" to AttributeValue.builder().s(entry.level.name).build(),
                "message" to AttributeValue.builder().s(entry.message).build(),
                "ttl" to AttributeValue.builder().n(calculateTTL().toString()).build()
            )
            
            val request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()
            
            dynamoDbClient.putItem(request)
            
            logger.info("✅ [DYNAMODB] Auditoria salva com sucesso - ChangeNumber: ${entry.changeNumber}")
        } catch (e: DynamoDbException) {
            logger.error("❌ [DYNAMODB] Erro ao salvar auditoria - ChangeNumber: ${entry.changeNumber}, Error: ${e.awsErrorDetails().errorMessage()}", e)
            throw AuditLogException("Falha ao gravar log de auditoria no DynamoDB", e)
        }
    }
}
```

## 📊 Níveis de Log

| Nível | Quando Usar | Exemplo |
|-------|-------------|---------|
| **ERROR** | Erros que impedem a operação | Falha ao conectar com API externa, Erro ao salvar no DynamoDB |
| **WARN** | Situações anormais mas recuperáveis | Token OAuth próximo de expirar, Retry de requisição |
| **INFO** | Eventos importantes do fluxo | Request recebido, Response enviado, Operação concluída |
| **DEBUG** | Detalhes técnicos para troubleshooting | Headers, Payloads, Queries SQL, Parâmetros |
| **TRACE** | Informações muito detalhadas | Stack traces completos, Dumps de objetos |

## 🔍 MDC (Mapped Diagnostic Context)

O MDC permite adicionar contexto aos logs que persiste através de toda a thread:

```kotlin
// Adicionar contexto
MDC.put("requestId", UUID.randomUUID().toString())
MDC.put("changeNumber", request.changeNumber)
MDC.put("userId", authentication.name)

try {
    // Todas as operações terão esse contexto nos logs
    service.execute(request)
} finally {
    // Sempre limpar o MDC
    MDC.clear()
}
```

## 📈 Monitoramento e Alertas

### Métricas Importantes

1. **Taxa de Requisições**: Requests por minuto/hora
2. **Tempo de Resposta**: Média, P50, P95, P99
3. **Taxa de Erro**: Porcentagem de requisições com erro
4. **Disponibilidade**: Uptime do serviço
5. **Latência de Integrações**: Tempo de resposta de APIs externas

### Queries Úteis (CloudWatch Logs Insights)

#### Requisições com Erro
```sql
fields @timestamp, changeNumber, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100
```

#### Tempo de Resposta por Endpoint
```sql
fields @timestamp, changeNumber, duration
| filter @message like /Response enviado/
| parse @message /Duration: (?<duration>\d+)ms/
| stats avg(duration), max(duration), min(duration), pct(duration, 95) by bin(5m)
```

#### Taxa de Sucesso
```sql
fields @timestamp
| filter @message like /Response enviado/
| parse @message /Status: (?<status>\d+)/
| stats count() by status
| sort status
```

#### Top URLs Mais Lentas
```sql
fields @timestamp, url, duration
| filter @message like /Requisição concluída/
| parse @message /URL: (?<url>[^,]+).*Duration: (?<duration>\d+)ms/
| stats avg(duration) as avg_duration by url
| sort avg_duration desc
| limit 10
```

## 🚨 Alertas Recomendados

1. **Alta Taxa de Erro** (> 5% em 5 minutos)
2. **Latência Elevada** (P95 > 2 segundos)
3. **Falhas no DynamoDB** (> 10 erros em 5 minutos)
4. **Falhas de Autenticação** (> 20 em 1 minuto)
5. **API Externa Indisponível** (> 50% de falhas em 2 minutos)

## 📚 Boas Práticas

### ✅ Fazer

- Usar níveis de log apropriados
- Incluir contexto relevante (requestId, changeNumber)
- Logar início e fim de operações importantes
- Logar erros com stack trace
- Usar structured logging (JSON em produção)
- Sanitizar dados sensíveis (senhas, tokens)

### ❌ Evitar

- Logar informações sensíveis (senhas, tokens completos, PII)
- Usar System.out.println() em vez de logger
- Logar em excesso (DEBUG em produção)
- Logar objetos grandes sem necessidade
- Esquecer de limpar o MDC

## 🔒 Segurança e Compliance

### Dados Sensíveis

Nunca logar:
- Senhas
- Tokens completos (apenas primeiros/últimos caracteres)
- Números de cartão de crédito
- CPF/CNPJ completos
- Dados pessoais identificáveis (PII)

### Exemplo de Sanitização

```kotlin
fun sanitizeToken(token: String): String {
    return if (token.length > 10) {
        "${token.take(4)}...${token.takeLast(4)}"
    } else {
        "***"
    }
}

logger.debug("Token obtido: ${sanitizeToken(token)}")
```

## 📖 Referências

- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
- [SLF4J Documentation](http://www.slf4j.org/manual.html)
- [CloudWatch Logs Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AnalyzingLogData.html)
- [Structured Logging Best Practices](https://www.loggly.com/ultimate-guide/java-logging-basics/)

---

**Última atualização**: 2026-05-07