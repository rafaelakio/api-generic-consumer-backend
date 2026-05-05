# Guia de Contribuição

Obrigado por considerar contribuir com o API Generic Consumer Backend! 🎉

## 📋 Índice

- [Código de Conduta](#código-de-conduta)
- [Como Posso Contribuir?](#como-posso-contribuir)
- [Processo de Desenvolvimento](#processo-de-desenvolvimento)
- [Padrões de Código](#padrões-de-código)
- [Padrões de Commit](#padrões-de-commit)
- [Pull Requests](#pull-requests)
- [Testes](#testes)

## 📜 Código de Conduta

Este projeto adere ao [Código de Conduta](CODE_OF_CONDUCT.md). Ao participar, você concorda em manter este código.

## 🤝 Como Posso Contribuir?

### Reportando Bugs

Antes de criar um bug report, verifique se o problema já não foi reportado. Se você encontrar um bug:

1. Use o template de issue para bugs
2. Inclua título claro e descritivo
3. Descreva os passos para reproduzir
4. Forneça exemplos específicos
5. Descreva o comportamento esperado vs atual
6. Inclua screenshots se aplicável
7. Adicione informações do ambiente (OS, Java version, etc.)

### Sugerindo Melhorias

Para sugerir melhorias:

1. Use o template de issue para features
2. Explique claramente o problema que a feature resolve
3. Descreva a solução proposta
4. Liste alternativas consideradas
5. Adicione contexto adicional

### Contribuindo com Código

1. Fork o repositório
2. Crie uma branch para sua feature
3. Faça suas alterações
4. Adicione testes
5. Garanta que todos os testes passam
6. Faça commit das suas mudanças
7. Push para sua branch
8. Abra um Pull Request

## 🔄 Processo de Desenvolvimento

### Setup do Ambiente

```bash
# Clone seu fork
git clone https://github.com/seu-usuario/api-generic-consumer-backend.git
cd api-generic-consumer-backend

# Adicione o repositório upstream
git remote add upstream https://github.com/original-org/api-generic-consumer-backend.git

# Instale dependências
./gradlew build

# Execute testes
./gradlew test
```

### Workflow de Branches

Usamos o modelo de branching simplificado:

- `main`: Branch principal, sempre estável
- `feature/*`: Novas funcionalidades
- `fix/*`: Correções de bugs
- `docs/*`: Atualizações de documentação
- `refactor/*`: Refatorações de código

```bash
# Criar nova branch
git checkout -b feature/minha-feature

# Manter branch atualizada
git fetch upstream
git rebase upstream/main
```

## 📝 Padrões de Código

### Kotlin Style Guide

Seguimos as [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// ✅ BOM
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    fun createUser(name: String): User {
        require(name.isNotBlank()) { "Name cannot be blank" }
        return userRepository.save(User(name = name))
    }
}

// ❌ RUIM
class UserService(private val userRepository: UserRepository,private val emailService: EmailService){
    fun createUser(name:String):User{
        if(name.isBlank())throw Exception("Name cannot be blank")
        return userRepository.save(User(name=name))
    }
}
```

### Regras Gerais

- **Indentação**: 4 espaços (não tabs)
- **Linha máxima**: 120 caracteres
- **Nomenclatura**:
  - Classes: `PascalCase`
  - Funções/Variáveis: `camelCase`
  - Constantes: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Arquitetura Hexagonal

Mantenha a separação de camadas:

```
domain/          # Regras de negócio puras
├── model/       # Entidades e value objects
└── port/        # Interfaces (input/output)

application/     # Casos de uso
└── service/     # Implementação dos casos de uso

infrastructure/  # Detalhes técnicos
├── adapter/     # Implementação das portas
└── config/      # Configurações
```

### Injeção de Dependências

Use constructor injection:

```kotlin
// ✅ BOM
@Service
class ProxyService(
    private val httpClient: HttpClientPort,
    private val tokenProvider: TokenProviderPort
) : ExecuteProxyUseCase {
    // ...
}

// ❌ RUIM
@Service
class ProxyService {
    @Autowired
    private lateinit var httpClient: HttpClientPort
}
```

## 💬 Padrões de Commit

Usamos [Conventional Commits](https://www.conventionalcommits.org/):

### Formato

```
<tipo>(<escopo>): <descrição>

[corpo opcional]

[rodapé opcional]
```

### Tipos

- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação (não afeta código)
- `refactor`: Refatoração
- `test`: Adição/correção de testes
- `chore`: Manutenção/tarefas
- `perf`: Melhoria de performance
- `ci`: Mudanças em CI/CD

### Exemplos

```bash
# Feature
feat(proxy): add support for GraphQL endpoints

# Bug fix
fix(audit): correct timestamp format in DynamoDB

# Documentation
docs(readme): update installation instructions

# Refactoring
refactor(service): extract token validation logic

# Breaking change
feat(api)!: change proxy request format

BREAKING CHANGE: Request body now requires 'apiVersion' field
```

### Regras

- Use imperativo presente ("add" não "added")
- Primeira linha com no máximo 72 caracteres
- Corpo opcional para explicar o "porquê"
- Referencie issues quando aplicável

```bash
fix(auth): resolve token expiration issue

Token was not being refreshed correctly when expired.
Now checks expiration before each request.

Fixes #123
```

## 🔀 Pull Requests

### Checklist

Antes de submeter um PR, verifique:

- [ ] Código segue os padrões do projeto
- [ ] Testes adicionados/atualizados
- [ ] Todos os testes passam
- [ ] Documentação atualizada
- [ ] Commits seguem padrão conventional
- [ ] Branch está atualizada com main
- [ ] Sem conflitos de merge

### Template de PR

```markdown
## Descrição
Breve descrição das mudanças

## Tipo de Mudança
- [ ] Bug fix
- [ ] Nova feature
- [ ] Breaking change
- [ ] Documentação

## Como Testar
1. Passo 1
2. Passo 2
3. Resultado esperado

## Checklist
- [ ] Testes passando
- [ ] Documentação atualizada
- [ ] Code review solicitado

## Issues Relacionadas
Closes #123
```

### Processo de Review

1. Pelo menos 1 aprovação necessária
2. CI deve passar
3. Sem conflitos
4. Cobertura de testes mantida/melhorada

## 🧪 Testes

### Estrutura de Testes

```kotlin
@SpringBootTest
class ProxyServiceTest {
    
    @MockkBean
    private lateinit var httpClient: HttpClientPort
    
    @Autowired
    private lateinit var proxyService: ProxyService
    
    @Test
    fun `should execute proxy request successfully`() {
        // Given
        val request = ApiRequest(
            url = "https://api.example.com",
            method = "GET"
        )
        every { httpClient.execute(any()) } returns ApiResponse(200, "OK")
        
        // When
        val result = proxyService.execute(request)
        
        // Then
        assertThat(result.statusCode).isEqualTo(200)
        verify { httpClient.execute(request) }
    }
}
```

### Cobertura de Testes

- Mínimo: 80% de cobertura
- Testes unitários para lógica de negócio
- Testes de integração para adaptadores
- Testes E2E para fluxos críticos

### Executando Testes

```bash
# Todos os testes
./gradlew test

# Testes específicos
./gradlew test --tests ProxyServiceTest

# Com cobertura
./gradlew test jacocoTestReport

# Testes de integração
./gradlew integrationTest
```

## 📚 Recursos Adicionais

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)

## ❓ Dúvidas?

- Abra uma [Discussion](https://github.com/org/repo/discussions)
- Entre em contato via email: dev@example.com
- Consulte a [documentação](docs/)

## 🙏 Agradecimentos

Obrigado por contribuir! Sua ajuda torna este projeto melhor para todos. 🚀

---

**Lembre-se**: Código é lido muito mais vezes do que escrito. Escreva pensando em quem vai ler!