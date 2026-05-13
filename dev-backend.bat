@echo off
REM Script para desenvolvimento apenas do backend

echo ⚙️ Iniciando Backend em modo desenvolvimento...
echo =============================================

cd /d "%~dp0"

REM Verificar se Java está disponível
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java não encontrado. Por favor, instale o Java 21+
    pause
    exit /b 1
)

REM Verificar se Gradle está disponível
gradle --version >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Gradle não encontrado. Tentando usar Gradle Wrapper...
    if not exist "gradlew.bat" (
        echo [ERROR] Gradle Wrapper não encontrado. Por favor, instale o Gradle.
        pause
        exit /b 1
    )
    set GRADLE_CMD=gradlew.bat
) else (
    set GRADLE_CMD=gradle
)

REM Copiar configuração de teste
if exist ".env.test" (
    echo [INFO] Configuração de teste encontrada
    echo [INFO] Variáveis de ambiente: AWS_REGION, AWS_ACCESS_KEY_ID, etc.
)

REM Iniciar LocalStack se não estiver rodando
echo [INFO] Verificando LocalStack...
curl -s http://localhost:4566/_localstack/health >nul 2>&1
if errorlevel 1 (
    echo [INFO] Iniciando LocalStack...
    docker-compose -f ..\api-generic-consumer-frontend\docker-compose.integrated.yml up -d localstack
    echo [INFO] Aguardando LocalStack iniciar...
    :wait_localstack
    timeout /t 2 >nul
    curl -s http://localhost:4566/_localstack/health >nul 2>&1
    if errorlevel 1 (
        echo .
        goto wait_localstack
    )
    echo [SUCCESS] LocalStack está pronto!
)

REM Configurar dados de teste
echo [INFO] Configurando dados de teste...
aws dynamodb create-table ^
    --table-name audit-logs ^
    --attribute-definitions AttributeName=changeNumber,AttributeType=S AttributeName=timestamp,AttributeType=S ^
    --key-schema AttributeName=changeNumber,KeyType=HASH AttributeName=timestamp,KeyType=RANGE ^
    --billing-mode PAY_PER_REQUEST ^
    --endpoint-url http://localhost:4566 ^
    --region us-east-1 >nul 2>&1

aws secretsmanager create-secret ^
    --name api-consumer/api-credentials ^
    --secret-string "{\"username\":\"test\",\"password\":\"test\",\"clientId\":\"test\",\"clientSecret\":\"test\",\"tokenUrl\":\"https://httpbin.org/post\"}" ^
    --endpoint-url http://localhost:4566 ^
    --region us-east-1 >nul 2>&1

REM Iniciar backend
echo [INFO] Iniciando backend...
echo [INFO] Backend estará disponível em http://localhost:8080
echo [INFO] Health check: http://localhost:8080/actuator/health
echo.

%GRADLE_CMD% bootRun

pause
