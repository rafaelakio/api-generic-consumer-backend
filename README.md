# API Generic Consumer Backend

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-green.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)

Proxy service genérico para consumo de APIs externas com autenticação OAuth2, gerenciamento de credenciais via AWS Secrets Manager e auditoria em DynamoDB.

## 🚀 Features

- ✅ Proxy genérico para APIs REST
- ✅ Autenticação OAuth2 automática
- ✅ Gerenciamento seguro de credenciais (AWS Secrets Manager)
- ✅ Logs de auditoria em DynamoDB com TTL
- ✅ Suporte a Azure AD JWT
- ✅ Arquitetura hexagonal (ports & adapters)
- ✅ Testes locais com LocalStack
- ✅ Health checks e métricas (Actuator)
- ✅ CORS configurável
- ✅ Docker ready

## 📋 Pré-requisitos

- Java 21+
- Gradle 8+
- Docker & Docker Compose
- AWS CLI (opcional, para testes locais)

## 🏃 Quick Start

### Desenvolvimento Local com LocalStack

1. **Clone o repositório**
```bash
git clone <repository-url>
cd api-generic-consumer-backend
```

2. **Inicie o ambiente completo**
```bash
docker-compose up -d
```

3. **Verifique os serviços**
```bash
# Health check do backend
curl http://localhost:8080/actuator/health

# Verificar LocalStack
curl http://localhost:4566/_localstack/health
```

4. **Acesse a aplicação**
- Backend API: http://localhost:8080
- LocalStack: http://localhost:4566

### Desenvolvimento Local (sem Docker)

1. **Configure as variáveis de ambiente**
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DYNAMODB_ENDPOINT=http://localhost:4566
export SPRING_PROFILES_ACTIVE=local
```

2. **Execute a aplicação**
```bash
./gradlew bootRun
```

## 🧪 Testes

### Testes Unitários
```bash
./gradlew test
```

### Testes de Integração
```bash
./gradlew integrationTest
```

### Testes End-to-End
```bash
docker-compose -f docker-compose.e2e.yml up
```

## 📖 Documentação

- [Arquitetura](docs/ARCHITECTURE.md) - Visão geral da arquitetura e componentes
- [API Reference](docs/API.md) - Documentação completa dos endpoints
- [Setup Local](docs/LOCAL_SETUP.md) - Guia detalhado de configuração local
- [Testes](docs/TESTING.md) - Estratégias e guias de teste
- [Guia de Contribuição](CONTRIBUTING.md) - Como contribuir com o projeto

## 🔧 Configuração

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `AWS_REGION` | Região AWS | `us-east-1` |
| `AWS_DYNAMODB_ENDPOINT` | Endpoint DynamoDB (local) | - |
| `AWS_SECRET_NAME_API_CREDENTIALS` | Nome do secret | `api-consumer/api-credentials` |
| `AZURE_TENANT_ID` | Tenant ID do Azure AD | `common` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas CORS | `http://localhost:3000` |
| `ADMIN_USERNAME` | Username admin | `admin` |
| `ADMIN_PASSWORD` | Password admin | `admin` |

### application.yml

Veja [application.yml](src/main/resources/application.yml) para configurações completas.

## 🏗️ Arquitetura

```
┌─────────────────┐
│   Frontend      │
│   (Next.js)     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  API Gateway    │
│   (AWS/Local)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│   Backend       │─────▶│  DynamoDB    │
│ (Spring Boot)   │      │ (Audit Logs) │
└────────┬────────┘      └──────────────┘
         │
         ▼
┌─────────────────┐
│ Secrets Manager │
│  (Credentials)  │
└─────────────────┘
```

### Camadas

- **Domain**: Modelos e portas (interfaces)
- **Application**: Casos de uso e serviços
- **Infrastructure**: Adaptadores (HTTP, AWS, OAuth)

## 🐳 Docker

### Build da imagem
```bash
docker build -t api-consumer-backend .
```

### Executar container
```bash
docker run -p 8080:8080 \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=test \
  -e AWS_SECRET_ACCESS_KEY=test \
  api-consumer-backend
```

## 📊 Endpoints Principais

### Proxy
```bash
POST /api/proxy
Authorization: Bearer <token>
Content-Type: application/json

{
  "url": "https://api.example.com/endpoint",
  "method": "GET",
  "headers": {},
  "body": {}
}
```

### Audit Log
```bash
POST /api/audit-log
Authorization: Bearer <token>
Content-Type: application/json

{
  "changeNumber": "CHG001",
  "level": "CALL",
  "message": "API call executed",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### Health Check
```bash
GET /actuator/health
```

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor, leia nosso [Guia de Contribuição](CONTRIBUTING.md) para detalhes sobre nosso código de conduta e processo de submissão de pull requests.

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🙏 Agradecimentos

- Spring Boot Team
- AWS SDK Team
- Kotlin Community
- LocalStack Team

## 📞 Suporte

- 📧 Email: support@example.com
- 🐛 Issues: [GitHub Issues](https://github.com/your-org/api-generic-consumer-backend/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/your-org/api-generic-consumer-backend/discussions)

## 🗺️ Roadmap

- [ ] Suporte a GraphQL
- [ ] Cache de tokens OAuth2
- [ ] Métricas customizadas
- [ ] Rate limiting
- [ ] Circuit breaker
- [ ] Retry policies

---

**Desenvolvido com ❤️ usando Kotlin e Spring Boot**