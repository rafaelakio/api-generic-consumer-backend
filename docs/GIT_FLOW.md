# Git Flow Workflow

Este projeto utiliza o modelo Git Flow para gerenciamento de branches e releases.

## 🌳 Estrutura de Branches

```
main (produção)
├── develop (desenvolvimento)
│   ├── feature/nova-funcionalidade
│   ├── feature/validacao-servicenow
│   └── feature/outra-feature
├── release/v1.0.0
└── hotfix/correcao-urgente
```

## 📋 Branches Principais

### `main`
- **Propósito**: Código em produção
- **Proteção**: Apenas merges de `release/*` e `hotfix/*`
- **Tags**: Cada release é taggado (ex: `v1.0.0`)

### `develop`
- **Propósito**: Integração contínua de features
- **Base**: Todas as novas features partem daqui
- **Destino**: Merge para `main` via `release/*`

## 🚀 Branches de Suporte

### `feature/*`
- **Propósito**: Desenvolvimento de novas funcionalidades
- **Criação**: `git checkout -b feature/nome-feature develop`
- **Merge**: `git checkout develop && git merge feature/nome-feature`
- **Delete**: `git branch -d feature/nome-feature`

### `release/*`
- **Propósito**: Preparação para lançamento
- **Criação**: `git checkout -b release/v1.0.0 develop`
- **Conteúdo**: Bug fixes, documentação, ajustes finos
- **Merge**: Para `main` e `develop`

### `hotfix/*`
- **Propósito**: Correções urgentes em produção
- **Criação**: `git checkout -b hotfix/correcao main`
- **Merge**: Para `main` e `develop`

## 🔄 Fluxo de Trabalho

### 1. Nova Funcionalidade
```bash
# Criar feature branch
git checkout develop
git pull origin develop
git checkout -b feature/nova-funcionalidade

# Desenvolver...
git add .
git commit -m "feat: implementar nova funcionalidade"

# Merge para develop
git checkout develop
git merge feature/nova-funcionalidade
git push origin develop
git branch -d feature/nova-funcionalidade
```

### 2. Preparar Release
```bash
# Criar release branch
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0

# Ajustes finos, version bump, docs...
git commit -m "chore: preparar release v1.0.0"

# Merge para main
git checkout main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin main --tags

# Merge para develop
git checkout develop
git merge --no-ff release/v1.0.0
git push origin develop
git branch -d release/v1.0.0
```

### 3. Hotfix Urgente
```bash
# Criar hotfix branch
git checkout main
git pull origin main
git checkout -b hotfix/correcao-urgente

# Corrigir...
git commit -m "fix: corrigir bug crítico"

# Merge para main
git checkout main
git merge --no-ff hotfix/correcao-urgente
git tag -a v1.0.1 -m "Hotfix v1.0.1"
git push origin main --tags

# Merge para develop
git checkout develop
git merge --no-ff hotfix/correcao-urgente
git push origin develop
git branch -d hotfix/correcao-urgente
```

## 📝 Convenções de Commits

Utilizamos [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>[escopo opcional]: <descrição>

[corpo opcional]

[rodapé opcional]
```

### Tipos
- `feat`: Nova funcionalidade
- `fix`: Bug fix
- `docs`: Documentação
- `style`: Formatação, semântica
- `refactor`: Refatoração
- `test`: Testes
- `chore`: Build, dependências

### Exemplos
```bash
feat(api): adicionar validação ServiceNow
fix(auth): corrigir token expiration
docs(readme): atualizar instruções de setup
```

## 🔧 Configuração Local

### Instalar git-flow (opcional)
```bash
# macOS
brew install git-flow-avh

# Windows
# Baixar e instalar git-flow-avh

# Linux
sudo apt-get install git-flow
```

### Inicializar Git Flow
```bash
git flow init
# Configurar branch names:
# Production branch: main
# Development branch: develop
```

## 📋 Pull Requests

### Título
Seguir convenção de commits:
- `feat: adicionar validação ServiceNow`
- `fix: corrigir bug de autenticação`

### Descrição
- **O que**: Descrição clara da mudança
- **Por que**: Motivo da mudança
- **Como**: Implementação (se relevante)
- **Testes**: Como testar

### Reviewers
- Mínimo 1 reviewer técnico
- Aprovação obrigatória antes do merge

## 🚀 Deploy

### Desenvolvimento
- Branch: `develop`
- Ambiente: Homologação
- Trigger: Auto após merge em `develop`

### Produção
- Branch: `main`
- Ambiente: Produção
- Trigger: Auto após merge em `main`

## 📊 ServiceNow Integration

Para mudanças relacionadas à integração com ServiceNow:

1. **Feature Branch**: `feature/servicenow-chg-validation`
2. **Validação**: Testar CHG vs INC
3. **Documentação**: Atualizar API docs
4. **Testes**: Incluir testes unitários e integração

## 📞 Suporte

- Dúvidas sobre Git Flow: [Git Flow Documentation](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)
- Issues: [GitHub Issues](https://github.com/rafaelakio/api-generic-consumer-backend/issues)
