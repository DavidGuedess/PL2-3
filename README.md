# MiauGenda

Sistema de Gestão de Turnos para Clínica Veterinária  
LES 2025/26 — Tema T9 | Equipa PL2-4

---

## Stack

| Camada | Tecnologia |
|---|---|
| App Mobile | Android (Kotlin + Jetpack Compose) |
| App Desktop | React + TypeScript + Vite |
| Backend | Node.js + Express + TypeScript |
| ORM | Prisma |
| Base de dados | PostgreSQL 16 |
| Containers | Docker + docker-compose |
| CI/CD | GitHub Actions |

---

## Pré-requisitos

- [Node.js 20+](https://nodejs.org)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Git

> A rede da universidade bloqueia o Docker Hub. Usa hotspot ou faz o setup em casa.

---

## Setup inicial (só na primeira vez)

```bash
# 1. Clonar o repositório
git clone https://github.com/DavidGuedess/PL2-4.git
cd PL2-4

# 2. Copiar variáveis de ambiente
cp .env.example .env

# 3. Arrancar a base de dados
docker-compose up -d db

# 4. Instalar dependências do backend
cd backend && npm install && cd ..

# 5. Instalar dependências do desktop
cd desktop && npm install && cd ..

# 6. Correr migrações e seed
cd backend
npx prisma migrate deploy
npm run db:seed
cd ..
```

---

## Arrancar o projeto

### Script automático

**Git Bash:**
```bash
./start.sh
```

**PowerShell:**
```powershell
.\start.ps1
```

Abre duas janelas separadas (backend + desktop) e mostra os URLs.

### Manualmente (dois terminais)

**Terminal 1 — Backend:**
```bash
cd backend
npm run dev
```

**Terminal 2 — Desktop:**
```bash
cd desktop
npm run dev
```

### URLs

| Serviço | URL |
|---|---|
| Desktop (browser) | http://localhost:5173 |
| Backend API | http://localhost:3001 |
| Swagger UI | http://localhost:3001/api-docs |

---

## Credenciais de login

Todos os utilizadores têm a password `password123`.

| Número | Nome | Role |
|---|---|---|
| `EMP001` | Administrador 1 | ADMIN |
| `EMP002` | Administrador 2 | ADMIN |
| `EMP003` | Gerente 1 | MANAGER |
| `EMP004` | Gerente 2 | MANAGER |
| `EMP005` | Funcionario 1 | EMPLOYEE |
| `EMP006` | Funcionario 2 | EMPLOYEE |

> Para re-criar os utilizadores: `cd backend && npm run db:seed`

---

## API Reference

Base URL: `http://localhost:3001`

Endpoints protegidos requerem header `Authorization: Bearer <token>`.  
O token é obtido via `POST /api/auth/login`.

### Autenticação

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Login com `employeeNumber` + `password` |
| POST | `/api/auth/refresh` | Público | Renovar access token com `refreshToken` |
| POST | `/api/auth/logout` | Autenticado | Invalidar refresh token |

**Exemplo de login:**
```bash
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"employeeNumber":"EMP001","password":"password123"}'
```

### Utilizadores

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/users` | ADMIN, MANAGER | Listar todos os utilizadores |
| GET | `/users/me` | Todos | Perfil do utilizador autenticado |
| GET | `/users/:id` | ADMIN | Perfil de um utilizador |
| POST | `/users` | ADMIN | Criar utilizador |
| PATCH | `/users/me` | Todos | Editar próprio perfil |
| PATCH | `/users/:id/activate` | ADMIN | Reativar utilizador |
| PATCH | `/users/:id/deactivate` | ADMIN | Desativar utilizador |

### Turnos

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/shifts?week=YYYY-MM-DD` | Todos | Escala semanal |
| GET | `/shifts?month=YYYY-MM` | Todos | Escala mensal |
| GET | `/shifts/me` | Todos | Turnos do utilizador autenticado |
| POST | `/shifts` | ADMIN, MANAGER | Criar turno |
| PATCH | `/shifts/:id` | ADMIN, MANAGER | Editar turno |
| DELETE | `/shifts/:id` | ADMIN, MANAGER | Eliminar turno |

### Tipos de Turno

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/shift-types` | Todos | Listar tipos de turno |
| POST | `/shift-types` | ADMIN, MANAGER | Criar tipo de turno |
| PATCH | `/shift-types/:id` | ADMIN, MANAGER | Editar tipo de turno |
| DELETE | `/shift-types/:id` | ADMIN, MANAGER | Eliminar tipo de turno |

### Registos de Ponto

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| POST | `/attendance` | Todos | Registar entrada (IN) ou saída (OUT) |
| GET | `/attendance/me` | Todos | Histórico próprio (`?from=&to=`) |
| GET | `/attendance/active` | ADMIN, MANAGER | Funcionários em turno agora |
| GET | `/attendance/report` | ADMIN, MANAGER | Relatório de presenças |
| GET | `/attendance/stats` | ADMIN, MANAGER | Estatísticas |
| GET | `/attendance` | ADMIN, MANAGER | Histórico de um funcionário (`?userId=`) |
| PATCH | `/attendance/:id` | ADMIN | Editar registo |
| DELETE | `/attendance/:id` | ADMIN | Eliminar registo |

### Pedidos de Folga

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/time-off-requests` | Todos | Listar pedidos |
| POST | `/time-off-requests` | Todos | Criar pedido |
| PATCH | `/time-off-requests/:id/status` | ADMIN, MANAGER | Aprovar/rejeitar |
| DELETE | `/time-off-requests/:id` | Todos | Cancelar pedido |

### Trocas de Turno

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/shift-swap-requests` | Todos | Listar pedidos |
| POST | `/shift-swap-requests` | Todos | Criar pedido |
| PATCH | `/shift-swap-requests/:id/target-response` | Todos | Resposta do visado |
| PATCH | `/shift-swap-requests/:id/status` | ADMIN, MANAGER | Aprovação final |
| DELETE | `/shift-swap-requests/:id` | Todos | Cancelar pedido |

### Disponibilidade

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/availability` | Todos | Consultar disponibilidades |
| POST | `/availability` | Todos | Registar disponibilidade |
| DELETE | `/availability/:id` | Todos | Remover disponibilidade |

### Canais de Mensagens

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/channels` | Todos | Listar canais |
| POST | `/channels` | Todos | Criar canal |
| DELETE | `/channels/:id` | Todos | Apagar canal |
| GET | `/channels/:id/messages` | Todos | Mensagens de um canal |
| POST | `/channels/:id/messages` | Todos | Enviar mensagem |

### Atribuições Semanais

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/week-assignments` | Todos | Listar atribuições |
| POST | `/week-assignments` | ADMIN, MANAGER | Criar atribuição |
| PATCH | `/week-assignments/:id` | ADMIN, MANAGER | Editar atribuição |
| DELETE | `/week-assignments/:id` | ADMIN, MANAGER | Eliminar atribuição |

---

## Workflow de desenvolvimento

```
main        ← apenas via Pull Request aprovado
  └── develop   ← branch de integração
        └── feature/us-XX-nome  ← uma branch por User Story
```

```bash
# Partir sempre de develop atualizado
git checkout develop
git pull origin develop
git checkout -b feature/us-XX-nome

# Guardar trabalho
git add .
git commit -m "feat: descrição do que fizeste"
git push origin feature/us-XX-nome
```

Abre um Pull Request de `feature/us-XX-nome` → `develop`.  
Necessita de 1 aprovação antes de fazer merge.

---

## Comandos úteis

```bash
# Parar a base de dados
docker-compose down

# Nova migração após alterar o schema.prisma
cd backend && npx prisma migrate dev --name nome-da-migracao

# Abrir Prisma Studio (interface visual da BD)
cd backend && npx prisma studio

# Correr testes
cd backend && npm test
```
