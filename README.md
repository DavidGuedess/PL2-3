# MiauGenda

Sistema de Gestão de Turnos para Clínica Veterinária
LES 2025/26 - Tema T9 | Equipa PL2-4

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| App Mobile | Android (Kotlin + Jetpack Compose) |
| Backend | Node.js + Express + TypeScript |
| ORM | Prisma |
| Base de dados | PostgreSQL 16 |
| Containers | Docker + docker-compose |
| CI/CD | GitHub Actions |

---

## Como arrancar o backend

**Pré-requisitos:** Docker Desktop e Git instalados.

> A rede da universidade bloqueia o Docker Hub. Usa hotspot ou faz o setup em casa.
```bash
# 1. Clonar o repositório
git clone https://github.com/teu-username/PL2-4.git
cd PL2-4

# 2. Configurar variáveis de ambiente
cp .env.example .env

# 3. Arrancar os serviços
docker-compose up --build
```

Num segundo terminal, quando os serviços estiverem a correr:
```bash
# 4. Criar tabelas e dados iniciais (só na primeira vez)
docker-compose exec backend npx prisma migrate dev --name init
docker-compose exec backend npm run db:seed
```

O backend fica disponível em `http://localhost:3001`.

A documentação interativa da API (Swagger UI) está disponível em `http://localhost:3001/api-docs`.

---

## Como desenvolver
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
git commit -m "feat: descricao do que fizeste"
git push origin feature/us-XX-nome
```

Abre um Pull Request no GitHub de `feature/us-XX-nome` para `develop`.
Necessita de 1 aprovação antes de fazer merge.

---

## Comandos úteis
```bash
# Parar os serviços
docker-compose down

# Ver logs do backend
docker-compose logs -f backend

# Nova migração após alterar o schema.prisma
docker-compose exec backend npx prisma migrate dev --name nome-da-migracao

# Ver base de dados visualmente
docker-compose exec backend npx prisma studio
```

---

## Histórico de alterações

### SCRUM-55 — Gestão básica de utilizadores (Sprint 1)

- Dados iniciais em memória (in-memory storage)
- Endpoint GET /users — listar utilizadores
- Endpoint POST /users — criar utilizador
- Endpoint PATCH /users/:id/deactivate — desativar utilizador

> Nota: dados em memória são temporários e reiniciam com o servidor.

---

### SCRUM-97 — Atualização do modelo de dados de utilizadores (Sprint 2, subtask 1.1)

Alinhamento dos tipos TypeScript com o schema Prisma:

- `UserRole`: `ADMIN | MANAGER | EMPLOYEE`
- `UserCategory` (novo): `VETERINARIAN | NURSE | OPERATIONAL | ADMINISTRATIVE`
- Campos renomeados para coincidir com o schema: `fullName → name`, `isActive → active`
- `id` alterado de UUID (string) para inteiro com autoincrement
- Adicionados campos `passwordHash`, `updatedAt`
- Novo tipo `UserPublic` (omite `passwordHash`) usado nas respostas da API
- Validação do `POST /users` atualizada para incluir `category`

**Modelo atual da tabela `User`:**

| Campo | Tipo | Notas |
|---|---|---|
| id | Int | autoincrement, PK |
| employeeNumber | String | único |
| name | String | |
| email | String | único |
| passwordHash | String | nunca exposto na API |
| role | Role | ADMIN, MANAGER, EMPLOYEE |
| category | Category | VETERINARIAN, NURSE, OPERATIONAL, ADMINISTRATIVE |
| active | Boolean | default true |
| createdAt | DateTime | auto |
| updatedAt | DateTime | auto |

---

### SCRUM-98 — Migration Prisma + Seed inicial (Sprint 2, subtask 1.2)

- Migration `20260313114044_init` já incluía os campos `role` e `category` — verificada e confirmada
- Criado `backend/src/prisma/seed.ts` com 3 utilizadores iniciais (Admin, Manager, Employee)
- Passwords encriptadas com bcrypt
- Seed usa `upsert` — pode ser re-executado sem criar duplicados

Notes:
- Data is currently stored in-memory (temporary, resets on server restart)
- UUIDs are used for unique user identification

novas instalações
- npm install uuid@8

- npm install --save-dev jest ts-jest @types/jest supertest @types/supertest
- npm install --save-dev @types/jest
Para correr o seed:
```bash
docker-compose exec backend npm run db:seed
```

---

### SCRUM-67 — Modelo de dados de turnos (Sprint 2)

- Criado modelo Prisma `ShiftType` (nome, hora início, hora fim)
- Criado modelo Prisma `Shift` (userId, shiftTypeId, data)
- Migration `20260322000000_shifts` gerada com as duas tabelas
- Constraint única `userId + date` para impedir turnos sobrepostos

**Modelos adicionados:**

| Modelo | Campos principais |
|---|---|
| ShiftType | id, name, startTime, endTime |
| Shift | id, userId, shiftTypeId, date — unique(userId, date) |

---

### SCRUM-68 — API de gestão e atribuição de turnos (Sprint 2)

Novos ficheiros: `backend/src/routes/shiftTypes.ts`, `backend/src/routes/shifts.ts`

**Endpoints de tipos de turno:**
- `GET /shift-types` — listar todos os tipos de turno
- `POST /shift-types` — criar tipo de turno (name, startTime, endTime)
- `PATCH /shift-types/:id` — atualizar tipo de turno
- `DELETE /shift-types/:id` — eliminar tipo de turno

**Endpoints de turnos:**
- `POST /shifts` — atribuir turno a funcionário (userId, shiftTypeId, date)
- `GET /shifts?week=2026-03-23` — escala semanal (segunda a domingo)
- `GET /shifts?month=2026-03` — escala mensal

---

### SCRUM-61 — CI, Swagger e documentação de endpoints (Sprint 2)

- Pipeline CI (GitHub Actions) configurado para correr em qualquer branch (push e pull request)
- Instalado `swagger-jsdoc` + `swagger-ui-express`
- Documentação interativa disponível em `http://localhost:3001/api-docs`
- Todos os endpoints documentados com OpenAPI 3.0:
  - `GET /health`
  - `GET /users`
  - `POST /users`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`

---

### SCRUM-63 — Controlo de acesso por papel - RBAC (Sprint 2)

- Campo `role` adicionado ao payload do JWT (access token inclui agora `userId`, `email` e `role`)
- Criado middleware `requireRole(...roles)` em `middleware/auth.ts` — retorna 403 se o papel do utilizador não estiver na lista permitida
- Adicionado `authenticate` aos routers de `/shifts` e `/shift-types` (estavam sem autenticação)
- RBAC aplicado a todos os endpoints existentes:

| Endpoint | Papéis permitidos |
|---|---|
| `GET /users` | ADMIN, MANAGER |
| `GET /users/me` | todos |
| `GET /users/:id` | ADMIN |
| `POST /users` | ADMIN |
| `PATCH /users/me` | todos |
| `PATCH /users/:id/deactivate` | ADMIN |
| `GET /shifts` | todos |
| `POST /shifts` | ADMIN, MANAGER |
| `GET /shift-types` | todos |
| `POST /shift-types` | ADMIN, MANAGER |
| `PATCH /shift-types/:id` | ADMIN, MANAGER |
| `DELETE /shift-types/:id` | ADMIN, MANAGER |

- Documentação Swagger atualizada com `security: bearerAuth` e respostas `401`/`403` em todos os endpoints protegidos
- Criado `tests/rbac.test.ts` com testes de autorização (403 para papéis incorretos, acesso confirmado para papéis corretos)


# SCRUM-115 "Registo de ponto"
- Implementado endpoint POST /attendance para registo de entradas/saídas.
- Implementado endpoint GET /attendance/me para consulta do histórico do utilizador autenticado.
- Adicionada validação da sequência IN/OUT para garantir consistência dos registos.
- Integração com Prisma/PostgreSQL e validação através de testes manuais (Postman).