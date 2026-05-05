#!/bin/bash

echo "=========================================="
echo "Initializing LocalStack resources..."
echo "=========================================="

# Aguardar LocalStack estar pronto
echo "Waiting for LocalStack to be ready..."
sleep 5

# Criar tabela DynamoDB para audit logs
echo "Creating DynamoDB table: audit-logs"
awslocal dynamodb create-table \
    --table-name audit-logs \
    --attribute-definitions \
        AttributeName=changeNumber,AttributeType=S \
        AttributeName=timestamp,AttributeType=S \
    --key-schema \
        AttributeName=changeNumber,KeyType=HASH \
        AttributeName=timestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region us-east-1

# Habilitar TTL na tabela
echo "Enabling TTL on audit-logs table..."
awslocal dynamodb update-time-to-live \
    --table-name audit-logs \
    --time-to-live-specification "Enabled=true, AttributeName=ttl" \
    --region us-east-1

# Criar secret para credenciais de API
echo "Creating secret: api-consumer/api-credentials"
awslocal secretsmanager create-secret \
    --name api-consumer/api-credentials \
    --secret-string '{
        "clientId": "test-client-id",
        "clientSecret": "test-client-secret",
        "tokenUrl": "http://mock-oauth/token",
        "scope": "api.read api.write"
    }' \
    --region us-east-1

# Verificar recursos criados
echo ""
echo "=========================================="
echo "Verifying created resources..."
echo "=========================================="

echo "DynamoDB Tables:"
awslocal dynamodb list-tables --region us-east-1

echo ""
echo "Secrets Manager Secrets:"
awslocal secretsmanager list-secrets --region us-east-1

echo ""
echo "=========================================="
echo "LocalStack initialization complete!"
echo "=========================================="

# Made with Bob
