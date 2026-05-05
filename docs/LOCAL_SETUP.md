# Setup Local com LocalStack

Este guia detalha como configurar e executar o ambiente de desenvolvimento local usando LocalStack para simular serviços AWS.

## 📋 Pré-requisitos

### Software Necessário

- **Docker Desktop** 20.10+
- **Docker Compose** 2.0+
- **Java** 21+
- **Gradle** 8+ (ou use o wrapper `./gradlew`)
- **AWS CLI** 2.0+ (opcional, para testes manuais)

### Verificar Instalações

```bash
# Docker
docker --version
docker-compose --version

# Java
java -version

# Gradle
./gradlew --version

# AWS CLI (opcional)
aws --version
```

## 🚀 Quick Start

### Opção 1: Docker Compose (Recomendado)

```bash
# 1. Clone o repositório
git clone <repository-url>
cd api-generic-consumer-backend

# 2. Inicie todos os serviços
docker-compose up -d

# 3. Verifique os logs
docker-compose logs -f

# 4. Acesse a aplicação
curl http://localhost:8080/actuator/health
```

### Opção 2: Desenvolvimento Local (sem Docker para Backend)

```bash
# 1. Inicie apenas o LocalStack
docker-compose up -d localstack

# 2. Configure variáveis de ambiente
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DYNAMODB_ENDPOINT=http://localhost:4566
export AWS_SECRETS_ENDPOINT=http://localhost:4566
export SPRING_PROFILES_ACTIVE=local

# 3. Execute o backend
./gradlew bootRun

# 4. Em outro terminal, verifique
curl http://localhost:8080/actuator/health
```

## 🐳 LocalStack

### Serviços Disponíveis

O LocalStack simula os seguintes serviços AWS:

- **DynamoDB**: Armazenamento de audit logs
- **Secrets Manager**: Gerenciamento de credenciais
- **API Gateway**: Roteamento de requisições (para testes E2E)

### Endpoints LocalStack

| Serviço | Endpoint |
|---------|----------|
| DynamoDB | http://localhost:4566 |
| Secrets Manager | http://localhost:4566 |
| API Gateway | http://localhost:4566 |
| Health Check | http://localhost:4566/_localstack/health |

### Verificar Serviços

```bash
# Health check geral
curl http://localhost:4566/_localstack/health

# Listar tabelas DynamoDB
awslocal dynamodb list-tables

# Listar secrets
awslocal secretsmanager list-secrets

# Verificar API Gateway
awslocal apigateway get-rest-apis
```

## 🔧 Configuração Detalhada

### 1. Inicialização do LocalStack

Os scripts de inicialização estão em `localstack-init/`:

```bash
localstack-init/
├── 01-setup.sh           # Cria DynamoDB e Secrets
└── 02-apigateway.sh      # Configura API Gateway
```

Esses scripts são executados automaticamente quando o LocalStack inicia.

### 2. Tabela DynamoDB

**Nome**: `audit-logs`

**Schema**:
```
Partition Key: changeNumber (String)
Sort Key: timestamp (String)
TTL Attribute: ttl (Number)
```

**Criar manualmente** (se necessário):
```bash
awslocal dynamodb create-table \
    --table-name audit-logs \
    --attribute-definitions \
        AttributeName=changeNumber,AttributeType=S \
        AttributeName=timestamp,AttributeType=S \
    --key-schema \
        AttributeName=changeNumber,KeyType=HASH \
        AttributeName=timestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST
```

### 3. Secrets Manager

**Nome do Secret**: `api-consumer/api-credentials`

**Estrutura**:
```json
{
  "clientId": "test-client-id",
  "clientSecret": "test-client-secret",
  "tokenUrl": "http://mock-oauth/token",
  "scope": "api.read api.write"
}
```

**Criar manualmente**:
```bash
awslocal secretsmanager create-secret \
    --name api-consumer/api-credentials \
    --secret-string '{
        "clientId": "test-client-id",
        "clientSecret": "test-client-secret",
        "tokenUrl": "http://mock-oauth/token"
    }'
```

**Recuperar secret**:
```bash
awslocal secretsmanager get-secret-value \
    --secret-id api-consumer/api-credentials \
    --query SecretString \
    --output text
```

## 🧪 Testando a Aplicação

### 1. Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Resposta esperada**:
```json
{
  "status": "UP"
}
```

### 2. Obter Token Admin

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

**Resposta**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. Executar Proxy Request

```bash
TOKEN="<seu-token-aqui>"

curl -X POST http://localhost:8080/api/proxy \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://jsonplaceholder.typicode.com/posts/1",
    "method": "GET",
    "headers": {}
  }'
```

### 4. Verificar Audit Logs no DynamoDB

```bash
# Listar todos os logs
awslocal dynamodb scan --table-name audit-logs

# Query por changeNumber
awslocal dynamodb query \
    --table-name audit-logs \
    --key-condition-expression "changeNumber = :cn" \
    --expression-attribute-values '{":cn":{"S":"CHG001"}}'
```

## 🔍 Troubleshooting

### LocalStack não inicia

**Problema**: Container não sobe ou fica reiniciando

**Solução**:
```bash
# Verificar logs
docker-compose logs localstack

# Limpar volumes e reiniciar
docker-compose down -v
docker-compose up -d localstack
```

### Backend não conecta ao LocalStack

**Problema**: Erro de conexão com DynamoDB/Secrets Manager

**Verificar**:
1. LocalStack está rodando: `docker ps`
2. Variáveis de ambiente corretas
3. Endpoints configurados

**Solução**:
```bash
# Verificar variáveis
echo $AWS_DYNAMODB_ENDPOINT
echo $AWS_SECRETS_ENDPOINT

# Testar conectividade
curl http://localhost:4566/_localstack/health
```

### Tabela DynamoDB não existe

**Problema**: `ResourceNotFoundException: Table not found`

**Solução**:
```bash
# Verificar tabelas
awslocal dynamodb list-tables

# Recriar tabela
docker-compose restart localstack
# Aguardar inicialização (30s)
awslocal dynamodb list-tables
```

### Secret não encontrado

**Problema**: `ResourceNotFoundException: Secret not found`

**Solução**:
```bash
# Listar secrets
awslocal secretsmanager list-secrets

# Recriar secret
awslocal secretsmanager create-secret \
    --name api-consumer/api-credentials \
    --secret-string '{"clientId":"test","clientSecret":"test","tokenUrl":"http://mock/token"}'
```

### Porta já em uso

**Problema**: `Port 4566 is already allocated`

**Solução**:
```bash
# Encontrar processo usando a porta
# Windows
netstat -ano | findstr :4566

# Linux/Mac
lsof -i :4566

# Parar container conflitante
docker ps
docker stop <container-id>
```

## 🔄 Comandos Úteis

### Docker Compose

```bash
# Iniciar todos os serviços
docker-compose up -d

# Parar todos os serviços
docker-compose down

# Parar e remover volumes
docker-compose down -v

# Ver logs em tempo real
docker-compose logs -f

# Ver logs de um serviço específico
docker-compose logs -f backend

# Reiniciar um serviço
docker-compose restart backend

# Reconstruir imagens
docker-compose build --no-cache
```

### AWS CLI Local (awslocal)

```bash
# Instalar awslocal
pip install awscli-local

# DynamoDB
awslocal dynamodb list-tables
awslocal dynamodb scan --table-name audit-logs
awslocal dynamodb describe-table --table-name audit-logs

# Secrets Manager
awslocal secretsmanager list-secrets
awslocal secretsmanager get-secret-value --secret-id api-consumer/api-credentials

# API Gateway
awslocal apigateway get-rest-apis
awslocal apigateway get-resources --rest-api-id <api-id>
```

### Gradle

```bash
# Compilar
./gradlew build

# Executar testes
./gradlew test

# Executar aplicação
./gradlew bootRun

# Limpar build
./gradlew clean

# Ver dependências
./gradlew dependencies
```

## 📊 Monitoramento

### Logs da Aplicação

```bash
# Via Docker
docker-compose logs -f backend

# Via arquivo (se rodando local)
tail -f logs/application.log
```

### Métricas (Actuator)

```bash
# Health
curl http://localhost:8080/actuator/health

# Info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics

# Metric específica
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## 🧹 Limpeza

### Remover tudo

```bash
# Parar e remover containers, networks e volumes
docker-compose down -v

# Remover imagens
docker rmi api-consumer-backend

# Limpar build do Gradle
./gradlew clean
```

### Reset completo

```bash
# Parar tudo
docker-compose down -v

# Remover dados do LocalStack
rm -rf .localstack

# Reconstruir e iniciar
docker-compose build --no-cache
docker-compose up -d
```

## 📚 Próximos Passos

Após configurar o ambiente local:

1. Leia a [Documentação da API](API.md)
2. Execute os [Testes](TESTING.md)
3. Veja a [Arquitetura](ARCHITECTURE.md)
4. Configure o [Frontend](../../api-generic-consumer-frontend/docs/LOCAL_SETUP.md)

## 🆘 Suporte

Se encontrar problemas:

1. Verifique os logs: `docker-compose logs`
2. Consulte o [Troubleshooting](#troubleshooting)
3. Abra uma [Issue](https://github.com/org/repo/issues)
4. Entre em contato: dev@example.com

---

**Última atualização**: 2024-05-05