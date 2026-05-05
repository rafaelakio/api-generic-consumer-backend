# API Reference

Documentação completa dos endpoints da API Generic Consumer Backend.

## 📋 Base URL

- **Local**: `http://localhost:8080`
- **Development**: `https://api-dev.example.com`
- **Production**: `https://api.example.com`

## 🔐 Autenticação

A API suporta dois métodos de autenticação:

### 1. Azure AD JWT (Produção)

```http
Authorization: Bearer <azure-ad-token>
```

### 2. Admin Token (Desenvolvimento/Testes)

Obtenha um token via endpoint `/auth/token`:

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

## 📡 Endpoints

### Authentication

#### POST /auth/token

Gera um token JWT para autenticação admin (apenas desenvolvimento).

**Request**:
```json
{
  "username": "admin",
  "password": "admin"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

**Errors**:
- `401 Unauthorized`: Credenciais inválidas

---

### Proxy

#### POST /api/proxy

Executa uma requisição HTTP para uma API externa através do proxy.

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "url": "https://api.example.com/endpoint",
  "method": "GET|POST|PUT|DELETE|PATCH",
  "headers": {
    "Custom-Header": "value"
  },
  "body": {
    "key": "value"
  },
  "useOAuth": true,
  "secretName": "api-consumer/api-credentials"
}
```

**Request Parameters**:

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `url` | string | Sim | URL completa da API externa |
| `method` | string | Sim | Método HTTP (GET, POST, PUT, DELETE, PATCH) |
| `headers` | object | Não | Headers customizados para a requisição |
| `body` | object | Não | Corpo da requisição (para POST/PUT/PATCH) |
| `useOAuth` | boolean | Não | Se deve usar OAuth2 (default: true) |
| `secretName` | string | Não | Nome do secret no Secrets Manager |

**Response** (200 OK):
```json
{
  "statusCode": 200,
  "headers": {
    "content-type": "application/json",
    "date": "Mon, 05 May 2024 10:00:00 GMT"
  },
  "body": {
    "id": 1,
    "name": "Example"
  }
}
```

**Response Fields**:

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `statusCode` | number | Status HTTP da resposta |
| `headers` | object | Headers da resposta |
| `body` | any | Corpo da resposta (JSON ou string) |

**Examples**:

**GET Request**:
```bash
curl -X POST http://localhost:8080/api/proxy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://jsonplaceholder.typicode.com/posts/1",
    "method": "GET"
  }'
```

**POST Request with Body**:
```bash
curl -X POST http://localhost:8080/api/proxy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://jsonplaceholder.typicode.com/posts",
    "method": "POST",
    "body": {
      "title": "New Post",
      "body": "Content",
      "userId": 1
    }
  }'
```

**With Custom Headers**:
```bash
curl -X POST http://localhost:8080/api/proxy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://api.example.com/data",
    "method": "GET",
    "headers": {
      "X-Custom-Header": "value",
      "Accept-Language": "pt-BR"
    }
  }'
```

**Errors**:
- `400 Bad Request`: Parâmetros inválidos
- `401 Unauthorized`: Token inválido ou ausente
- `500 Internal Server Error`: Erro ao executar requisição
- `502 Bad Gateway`: API externa retornou erro

---

### Audit Log

#### POST /api/audit-log

Registra uma entrada de audit log no DynamoDB.

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "changeNumber": "CHG001",
  "level": "CALL",
  "message": "API call executed successfully",
  "timestamp": "2024-05-05T10:00:00Z",
  "table": [
    {
      "url": "https://api.example.com/endpoint",
      "method": "GET",
      "status": "200"
    }
  ]
}
```

**Request Parameters**:

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `changeNumber` | string | Sim | Identificador da sessão/mudança |
| `level` | string | Sim | Nível do log (SESSION_OPEN, CALL, SESSION_CLOSE, SESSION_SUMMARY, FULL_REPORT) |
| `message` | string | Sim | Mensagem descritiva |
| `timestamp` | string | Sim | Timestamp ISO 8601 |
| `table` | array | Não | Dados tabulares (para SUMMARY) |

**Audit Levels**:

| Level | Descrição | Uso |
|-------|-----------|-----|
| `SESSION_OPEN` | Abertura de sessão | Início de uma sequência de chamadas |
| `CALL` | Chamada individual | Cada requisição proxy |
| `SESSION_CLOSE` | Fechamento de sessão | Fim da sequência |
| `SESSION_SUMMARY` | Resumo da sessão | Estatísticas agregadas |
| `FULL_REPORT` | Relatório completo | Relatório detalhado final |

**Response** (200 OK):
```json
{
  "message": "Audit log saved successfully"
}
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/audit-log \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "changeNumber": "CHG001",
    "level": "SESSION_OPEN",
    "message": "Starting API consumption session",
    "timestamp": "2024-05-05T10:00:00Z"
  }'
```

**Errors**:
- `400 Bad Request`: Parâmetros inválidos
- `401 Unauthorized`: Token inválido ou ausente
- `500 Internal Server Error`: Erro ao salvar no DynamoDB

---

### Health & Monitoring

#### GET /actuator/health

Verifica o status de saúde da aplicação.

**Response** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Example**:
```bash
curl http://localhost:8080/actuator/health
```

---

#### GET /actuator/info

Retorna informações sobre a aplicação.

**Response** (200 OK):
```json
{
  "app": {
    "name": "api-generic-consumer-backend",
    "version": "0.0.1-SNAPSHOT",
    "description": "Generic API Consumer with OAuth2 and Audit"
  }
}
```

---

#### GET /actuator/metrics

Lista todas as métricas disponíveis.

**Response** (200 OK):
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "http.server.requests",
    "system.cpu.usage"
  ]
}
```

---

#### GET /actuator/metrics/{metricName}

Obtém uma métrica específica.

**Example**:
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**Response** (200 OK):
```json
{
  "name": "jvm.memory.used",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 123456789
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    }
  ]
}
```

---

## 🔒 Security Headers

Todas as respostas incluem headers de segurança:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

## 🚨 Error Responses

### Formato Padrão

```json
{
  "timestamp": "2024-05-05T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "path": "/api/proxy"
}
```

### Códigos de Status

| Código | Descrição |
|--------|-----------|
| 200 | Sucesso |
| 400 | Requisição inválida |
| 401 | Não autenticado |
| 403 | Não autorizado |
| 404 | Recurso não encontrado |
| 500 | Erro interno do servidor |
| 502 | Erro na API externa |
| 503 | Serviço indisponível |

## 📊 Rate Limiting

**Limites** (por IP):
- 100 requisições por minuto
- 1000 requisições por hora

**Headers de Rate Limit**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1620000000
```

## 🔄 CORS

**Origens Permitidas** (configurável):
- Local: `http://localhost:3000`
- Dev: `https://dev.example.com`
- Prod: `https://example.com`

**Métodos Permitidos**:
- GET, POST, PUT, DELETE, PATCH, OPTIONS

**Headers Permitidos**:
- Authorization, Content-Type, X-Requested-With

## 📝 Request/Response Examples

### Complete Proxy Flow

```bash
# 1. Get admin token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  | jq -r '.token')

# 2. Execute proxy request
curl -X POST http://localhost:8080/api/proxy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://jsonplaceholder.typicode.com/posts/1",
    "method": "GET"
  }' | jq

# 3. Log audit entry
curl -X POST http://localhost:8080/api/audit-log \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "changeNumber": "CHG001",
    "level": "CALL",
    "message": "Retrieved post #1",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }'
```

## 🧪 Testing with Postman

Importe a collection: [Postman Collection](../postman/api-consumer.postman_collection.json)

## 📚 Additional Resources

- [Architecture](ARCHITECTURE.md)
- [Local Setup](LOCAL_SETUP.md)
- [Testing Guide](TESTING.md)

---

**API Version**: 1.0.0  
**Last Updated**: 2024-05-05