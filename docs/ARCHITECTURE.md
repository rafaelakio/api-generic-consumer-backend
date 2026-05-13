# Arquitetura do Sistema

## 📋 Visão Geral

O API Generic Consumer Backend é construído usando **Arquitetura Hexagonal** (Ports & Adapters), promovendo separação de responsabilidades, testabilidade e independência de frameworks.

## 🏗️ Diagrama de Arquitetura

```mermaid
graph TB
    subgraph "Frontend Layer"
        FE[Next.js Frontend]
    end
    
    subgraph "API Gateway Layer"
        AG[AWS API Gateway]
    end
    
    subgraph "Backend - Hexagonal Architecture"
        subgraph "Infrastructure Layer"
            WEB[Web Controllers]
            HTTP[HTTP Client Adapter]
            OAUTH[OAuth Token Adapter]
            SECRETS[AWS Secrets Adapter]
            DYNAMO[DynamoDB Adapter]
        end
        
        subgraph "Application Layer"
            PROXY[Proxy Service]
            AUDIT[Audit Log Service]
        end
        
        subgraph "Domain Layer"
            MODEL[Domain Models]
            PORTS[Ports/Interfaces]
        end
    end
    
    subgraph "External Services"
        API[External APIs]
        SM[AWS Secrets Manager]
        DB[DynamoDB]
    end
    
    FE -->|HTTPS| AG
    AG -->|HTTP| WEB
    WEB --> PROXY
    WEB --> AUDIT
    PROXY --> HTTP
    PROXY --> OAUTH
    PROXY --> SECRETS
    AUDIT --> DYNAMO
    HTTP -->|REST| API
    OAUTH -->|OAuth2| API
    SECRETS -->|SDK| SM
    DYNAMO -->|SDK| DB
    
    style FE fill:#61dafb
    style AG fill:#ff9900
    style WEB fill:#6db33f
    style PROXY fill:#6db33f
    style AUDIT fill:#6db33f
    style MODEL fill:#7c4dff
    style PORTS fill:#7c4dff
```

## 🎯 Arquitetura Hexagonal

### Camadas

#### 1. **Domain Layer** (Núcleo)
Contém a lógica de negócio pura, independente de frameworks e tecnologias.

```
domain/
├── model/              # Entidades e Value Objects
│   ├── ApiRequest.kt
│   ├── ApiResponse.kt
│   ├── ApiCredentials.kt
│   └── AuditLogEntry.kt
└── port/               # Interfaces (Contratos)
    ├── input/          # Casos de uso
    │   ├── ExecuteProxyUseCase.kt
    │   └── WriteAuditLogUseCase.kt
    └── output/         # Portas de saída
        ├── HttpClientPort.kt
        ├── TokenProviderPort.kt
        ├── SecretsManagerPort.kt
        └── AuditLogRepositoryPort.kt
```

**Características:**
- Sem dependências externas
- Apenas lógica de negócio
- Testável isoladamente
- Define contratos (ports)

#### 2. **Application Layer**
Implementa os casos de uso orquestrando o domínio.

```
application/
└── service/
    ├── ProxyService.kt      # Implementa ExecuteProxyUseCase
    └── AuditLogService.kt   # Implementa WriteAuditLogUseCase
```

**Responsabilidades:**
- Orquestrar chamadas ao domínio
- Coordenar múltiplas operações
- Gerenciar transações
- Aplicar regras de negócio

#### 3. **Infrastructure Layer**
Implementa os adaptadores para tecnologias específicas.

```
infrastructure/
├── adapter/
│   ├── input/
│   │   └── web/
│   │       ├── ProxyController.kt
│   │       ├── AuditLogController.kt
│   │       └── AuthController.kt
│   └── output/
│       ├── http/
│       │   └── OkHttpClientAdapter.kt
│       ├── oauth/
│       │   └── OAuthTokenAdapter.kt
│       ├── aws/
│       │   └── AwsSecretsAdapter.kt
│       └── dynamodb/
│           └── DynamoDbAuditLogAdapter.kt
└── config/
    ├── ApplicationConfig.kt
    ├── AwsConfig.kt
    ├── DynamoDbConfig.kt
    ├── SecurityConfig.kt
    └── WebConfig.kt
```

**Responsabilidades:**
- Implementar portas de entrada (controllers)
- Implementar portas de saída (adapters)
- Configurar frameworks
- Gerenciar dependências externas

## 🔄 Fluxo de Dados

### 1. Fluxo Completo com API Gateway

```mermaid
sequenceDiagram
    participant Frontend as Frontend<br/>(Next.js)
    participant APIGateway as API Gateway<br/>(AWS/LocalStack)
    participant Controller as Backend Controller<br/>(Spring Boot)
    participant ProxyService as Proxy Service
    participant SecretsAdapter as Secrets Adapter
    participant TokenAdapter as OAuth Token Adapter
    participant HttpAdapter as HTTP Client Adapter
    participant ExternalAPI as External API
    participant AuditService as Audit Service
    participant DynamoDB as DynamoDB

    Note over Frontend,DynamoDB: 📝 Logging em cada etapa

    Frontend->>Frontend: LOG: Iniciando requisição
    Frontend->>APIGateway: POST /api/proxy<br/>(HTTPS + Auth Token)
    Note right of Frontend: Log: Request enviado<br/>Timestamp, URL, Headers
    
    APIGateway->>APIGateway: LOG: Request recebido
    APIGateway->>APIGateway: Validar autenticação
    APIGateway->>Controller: Forward request
    Note right of APIGateway: Log: Request validado<br/>e encaminhado
    
    Controller->>Controller: LOG: Request recebido no backend
    Controller->>ProxyService: execute(request)
    Note right of Controller: Log: Iniciando processamento<br/>ChangeNumber, Method, URL
    
    ProxyService->>ProxyService: LOG: Buscando credenciais
    ProxyService->>SecretsAdapter: getCredentials()
    SecretsAdapter->>SecretsAdapter: LOG: Consultando Secrets Manager
    SecretsAdapter-->>ProxyService: credentials
    Note right of SecretsAdapter: Log: Credenciais obtidas
    
    ProxyService->>ProxyService: LOG: Obtendo token OAuth
    ProxyService->>TokenAdapter: getToken(credentials)
    TokenAdapter->>TokenAdapter: LOG: Requisitando token
    TokenAdapter->>ExternalAPI: POST /oauth/token
    ExternalAPI-->>TokenAdapter: access_token
    TokenAdapter-->>ProxyService: accessToken
    Note right of TokenAdapter: Log: Token obtido<br/>Expiration time
    
    ProxyService->>ProxyService: LOG: Executando requisição externa
    ProxyService->>HttpAdapter: execute(request + token)
    HttpAdapter->>HttpAdapter: LOG: Preparando HTTP request
    HttpAdapter->>ExternalAPI: HTTP Request<br/>(com Bearer Token)
    Note right of HttpAdapter: Log: Request enviado<br/>URL, Method, Headers
    
    ExternalAPI-->>HttpAdapter: HTTP Response
    HttpAdapter->>HttpAdapter: LOG: Response recebido
    HttpAdapter-->>ProxyService: response
    Note right of HttpAdapter: Log: Status, Body size,<br/>Response time
    
    ProxyService->>ProxyService: LOG: Gravando auditoria
    ProxyService->>AuditService: write(auditLog)
    AuditService->>AuditService: LOG: Preparando entrada de auditoria
    AuditService->>DynamoDB: save(log)
    DynamoDB-->>AuditService: success
    Note right of AuditService: Log: Auditoria gravada<br/>ChangeNumber, Timestamp
    
    ProxyService-->>Controller: response
    Controller->>Controller: LOG: Response preparado
    Controller-->>APIGateway: HTTP Response
    Note right of Controller: Log: Response enviado<br/>Status, Duration
    
    APIGateway->>APIGateway: LOG: Response processado
    APIGateway-->>Frontend: HTTP Response
    Note right of APIGateway: Log: Response retornado<br/>ao cliente
    
    Frontend->>Frontend: LOG: Response recebido
    Note right of Frontend: Log: Status, Data,<br/>Total duration
```

### 2. Requisição de Proxy (Detalhado)

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant ProxyService
    participant SecretsAdapter
    participant TokenAdapter
    participant HttpAdapter
    participant ExternalAPI
    participant AuditService
    participant DynamoDB

    Client->>Controller: POST /api/proxy
    Controller->>ProxyService: execute(request)
    ProxyService->>SecretsAdapter: getCredentials()
    SecretsAdapter-->>ProxyService: credentials
    ProxyService->>TokenAdapter: getToken(credentials)
    TokenAdapter-->>ProxyService: accessToken
    ProxyService->>HttpAdapter: execute(request + token)
    HttpAdapter->>ExternalAPI: HTTP Request
    ExternalAPI-->>HttpAdapter: HTTP Response
    HttpAdapter-->>ProxyService: response
    ProxyService->>AuditService: write(auditLog)
    AuditService->>DynamoDB: save(log)
    ProxyService-->>Controller: response
    Controller-->>Client: HTTP Response
```

### 2. Gravação de Audit Log

```mermaid
sequenceDiagram
    participant Service
    participant AuditService
    participant DynamoAdapter
    participant DynamoDB

    Service->>AuditService: write(entry)
    AuditService->>AuditService: logToConsole()
    AuditService->>DynamoAdapter: save(entry)
    DynamoAdapter->>DynamoAdapter: convertToItem()
    DynamoAdapter->>DynamoDB: putItem()
    DynamoDB-->>DynamoAdapter: success
    DynamoAdapter-->>AuditService: void
```

## 🔐 Segurança

### Autenticação e Autorização

```mermaid
graph LR
    A[Client Request] --> B{Has Token?}
    B -->|No| C[Return 401]
    B -->|Yes| D{Valid Token?}
    D -->|No| C
    D -->|Yes| E{Azure AD JWT?}
    E -->|Yes| F[Validate with JWKS]
    E -->|No| G{Admin Token?}
    G -->|Yes| H[Validate with Secret]
    G -->|No| C
    F --> I[Process Request]
    H --> I
```

### Fluxo de Credenciais

1. **Armazenamento**: Credenciais em AWS Secrets Manager
2. **Recuperação**: Adapter busca credenciais via SDK
3. **Cache**: Credenciais cacheadas em memória (opcional)
4. **Uso**: Token OAuth2 obtido e usado nas requisições
5. **Renovação**: Token renovado automaticamente quando expira

## 📊 Modelo de Dados

### DynamoDB - Audit Logs

```
Table: audit-logs
Partition Key: changeNumber (String)
Sort Key: timestamp (String)
TTL: ttl (Number) - 30 dias

Attributes:
- changeNumber: Identificador da sessão
- timestamp: ISO 8601 timestamp
- level: SESSION_OPEN | CALL | SESSION_CLOSE | SESSION_SUMMARY | FULL_REPORT
- message: Mensagem descritiva
- tableData: JSON string (opcional)
- ttl: Unix timestamp para expiração
```

### Secrets Manager - API Credentials

```json
{
  "clientId": "string",
  "clientSecret": "string",
  "tokenUrl": "string",
  "scope": "string (optional)"
}
```

## 🚀 Deployment

### Ambientes

#### Local (Development)
```
Frontend (localhost:3000)
    ↓
LocalStack API Gateway (localhost:4566)
    ↓
Backend (localhost:8080)
    ↓
LocalStack DynamoDB (localhost:4566)
```

#### AWS (Production)
```
CloudFront + S3 (Frontend)
    ↓
API Gateway
    ↓
EKS/ECS (Backend)
    ↓
DynamoDB
```

## 🔧 Configuração

### Variáveis de Ambiente por Ambiente

| Variável | Local | Dev | Prod |
|----------|-------|-----|------|
| `AWS_REGION` | us-east-1 | us-east-1 | us-east-1 |
| `AWS_DYNAMODB_ENDPOINT` | http://localhost:4566 | - | - |
| `SPRING_PROFILES_ACTIVE` | local | dev | prod |
| `CORS_ALLOWED_ORIGINS` | http://localhost:3000 | https://dev.example.com | https://example.com |

## 📈 Escalabilidade

### Horizontal Scaling
- Backend stateless
- Múltiplas instâncias via Kubernetes
- Load balancing via API Gateway

### Vertical Scaling
- Ajuste de recursos (CPU/Memory)
- JVM tuning
- Connection pooling

### Caching Strategy
- Token OAuth2 em memória
- Credenciais cacheadas (5 min)
- HTTP client connection pool

## 🔍 Observabilidade

### Logs
- Structured logging (JSON)
- Níveis: ERROR, WARN, INFO, DEBUG
- Contexto: requestId, changeNumber, userId

### Métricas (Actuator)
- Health checks
- JVM metrics
- HTTP metrics
- Custom business metrics

### Tracing
- Request ID propagation
- Distributed tracing ready
- Integration with CloudWatch/X-Ray

## 🧪 Testabilidade

### Estratégia de Testes

```
Unit Tests (80%)
├── Domain Layer: 100%
├── Application Layer: 90%
└── Infrastructure Layer: 70%

Integration Tests (15%)
├── Controller → Service
├── Adapter → External Service
└── Database operations

E2E Tests (5%)
└── Full flow with LocalStack
```

### Test Doubles

- **Mocks**: Para portas de saída
- **Stubs**: Para respostas fixas
- **Fakes**: Para implementações in-memory

## 🔄 Evolução da Arquitetura

### Próximos Passos

1. **Event-Driven**: Adicionar eventos de domínio
2. **CQRS**: Separar comandos de queries
3. **Saga Pattern**: Para transações distribuídas
4. **Circuit Breaker**: Resiliência em chamadas externas
5. **Rate Limiting**: Controle de taxa de requisições

## 📚 Referências

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

**Última atualização**: 2024-05-05