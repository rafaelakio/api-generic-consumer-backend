#!/bin/bash

echo "=========================================="
echo "Setting up API Gateway..."
echo "=========================================="

# Aguardar um pouco mais para garantir que o LocalStack está pronto
sleep 10

# Criar API REST
echo "Creating REST API..."
API_ID=$(awslocal apigateway create-rest-api \
    --name "api-consumer-gateway" \
    --description "Local API Gateway for API Generic Consumer" \
    --endpoint-configuration types=REGIONAL \
    --region us-east-1 \
    --query 'id' \
    --output text)

echo "API ID: $API_ID"

# Salvar API ID em arquivo para referência
echo "$API_ID" > /tmp/api-gateway-id.txt

# Obter root resource ID
ROOT_ID=$(awslocal apigateway get-resources \
    --rest-api-id $API_ID \
    --region us-east-1 \
    --query 'items[0].id' \
    --output text)

echo "Root Resource ID: $ROOT_ID"

# Criar resource /api
echo "Creating /api resource..."
API_RESOURCE=$(awslocal apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $ROOT_ID \
    --path-part "api" \
    --region us-east-1 \
    --query 'id' \
    --output text)

echo "API Resource ID: $API_RESOURCE"

# Criar resource /api/{proxy+}
echo "Creating /api/{proxy+} resource..."
PROXY_RESOURCE=$(awslocal apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $API_RESOURCE \
    --path-part "{proxy+}" \
    --region us-east-1 \
    --query 'id' \
    --output text)

echo "Proxy Resource ID: $PROXY_RESOURCE"

# Configurar método ANY no proxy resource
echo "Setting up ANY method..."
awslocal apigateway put-method \
    --rest-api-id $API_ID \
    --resource-id $PROXY_RESOURCE \
    --http-method ANY \
    --authorization-type NONE \
    --region us-east-1

# Configurar integração HTTP_PROXY
echo "Setting up HTTP_PROXY integration..."
awslocal apigateway put-integration \
    --rest-api-id $API_ID \
    --resource-id $PROXY_RESOURCE \
    --http-method ANY \
    --type HTTP_PROXY \
    --integration-http-method ANY \
    --uri "http://backend:8080/api/{proxy}" \
    --request-parameters '{"integration.request.path.proxy":"method.request.path.proxy"}' \
    --region us-east-1

# Configurar method response
echo "Setting up method response..."
awslocal apigateway put-method-response \
    --rest-api-id $API_ID \
    --resource-id $PROXY_RESOURCE \
    --http-method ANY \
    --status-code 200 \
    --region us-east-1

# Configurar integration response
echo "Setting up integration response..."
awslocal apigateway put-integration-response \
    --rest-api-id $API_ID \
    --resource-id $PROXY_RESOURCE \
    --http-method ANY \
    --status-code 200 \
    --region us-east-1

# Deploy da API
echo "Deploying API to 'local' stage..."
awslocal apigateway create-deployment \
    --rest-api-id $API_ID \
    --stage-name local \
    --stage-description "Local development stage" \
    --description "Initial deployment" \
    --region us-east-1

# Exibir URL da API
echo ""
echo "=========================================="
echo "API Gateway setup complete!"
echo "=========================================="
echo ""
echo "API Gateway URL:"
echo "http://localhost:4566/restapis/$API_ID/local/_user_request_/api"
echo ""
echo "Example usage:"
echo "curl http://localhost:4566/restapis/$API_ID/local/_user_request_/api/proxy"
echo ""
echo "API ID saved to: /tmp/api-gateway-id.txt"
echo "=========================================="

# Made with Bob
