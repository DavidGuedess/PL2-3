# 🐾 PawSchedule

Sistema de Gestão de Turnos para Clínica Veterinária  
**LES 2025/26 — Tema T9 | Equipa PL5-4**

---

## Stack Tecnológico

| Camada | Tecnologia | Alternativa considerada | Justificação |
|--------|-----------|------------------------|--------------|
| Frontend | React 18 + TypeScript + Vite | Vue 3 | Ecossistema maior, melhor suporte TypeScript, curva de aprendizagem da equipa |
| Estilo | Tailwind CSS | CSS Modules / styled-components | Desenvolvimento rápido, classes utilitárias, sem conflitos de escopo |
| Estado | Zustand | Redux Toolkit | Muito mais simples para estado global de pequena/média escala |
| Fetching | TanStack Query + Axios | SWR | Cache automático, invalidation granular, melhor DX |
| Backend | Node.js + Express + TypeScript | FastAPI (Python) | Mesmo ecossistema JS/TS, partilha de tipos, familiaridade da equipa |
| ORM | Prisma | TypeORM / Sequelize | Tipo-seguro end-to-end, migrações integradas, excelente DX |
| Migrações | Prisma Migrate | Flyway | Integração nativa com o ORM, versionamento automático |
| Base de dados | PostgreSQL 16 | MySQL | Melhor suporte a constraints complexas, JSON, e extensibilidade |
| Containers | Docker + docker-compose | — | Reprodutibilidade obrigatória (enunciado), isolamento de ambiente |
| CI/CD | GitHub Actions | GitLab CI | Integração nativa com GitHub, YAML simples |

---

## Arquitetura

```
pawschedule/
├── backend/
│   ├── prisma/
│   │   └── schema.prisma          # Fonte de verdade do modelo de dados
│   └── src/
│       ├── api/routes/            # Express routes (auth, users, shifts)
│       ├── middleware/            # auth.ts, errorHandler.ts
│       ├── models/                # Prisma client singleton
│       ├── prisma/seed.ts         # Dados iniciais (admin, turnos)
│       ├── services/jwt.ts        # JWT sign/verify
│       ├── tests/                 # Jest tests
│       ├── app.ts                 # Express app + middleware
│       └── index.ts               # Servidor HTTP
├── frontend/
│   └── src/
│       ├── components/            # Layout, componentes reutilizáveis
│       ├── hooks/                 # useShifts.ts (React Query)
│       ├── pages/                 # Login, Dashboard, Escalas, Solicitações, Utilizadores
│       ├── services/api.ts        # Axios com interceptor JWT
│       ├── store/auth.ts          # Zustand auth store (persistido)
│       ├── types/index.ts         # Tipos TypeScript partilhados
│       └── App.tsx                # Router + rotas privadas
└── .github/workflows/ci.yml       # Pipeline CI (lint + test + build)
```

---

## Modelo de Dados

```
User ──┬── ShiftAssignment ── ShiftTemplate
       ├── ShiftRequest
       └── AttendanceRecord
```

- **User** — funcionários com roles: `ADMIN`, `MANAGER`, `EMPLOYEE`
- **ShiftTemplate** — tipos de turno (manhã/tarde/noite) com horários
- **ShiftAssignment** — atribuição de turno a funcionário numa data
- **ShiftRequest** — pedidos de troca, folga ou férias com fluxo de aprovação
- **AttendanceRecord** — registos de ponto (clock-in/out)

---

## Início Rápido

### Pré-requisitos
- Docker + Docker Compose
- Git

### 1. Clonar e configurar

```bash
git clone <url-do-repositorio>
cd pawschedule
cp .env.example .env
# Edita o .env se necessário
```

### 2. Arrancar

```bash
docker-compose up --build
```

| Serviço | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:3001 |
| Health check | http://localhost:3001/health |

### 3. Criar tabelas e seed

```bash
# Executar migrações
docker-compose exec backend npx prisma migrate dev --name init

# Popular com dados iniciais
docker-compose exec backend npm run db:seed
```

**Credenciais de teste:**

| Role | Nº Funcionário | Palavra-passe |
|------|----------------|---------------|
| Admin | `ADM001` | `admin123` |
| Manager | `GER001` | `manager123` |

### 4. Desenvolvimento local (sem Docker)

**Backend:**
```bash
cd backend
npm install
npx prisma generate
npx prisma migrate dev
npm run dev
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

---

## API Endpoints

| Método | Endpoint | Descrição | Role |
|--------|----------|-----------|------|
| `POST` | `/api/auth/login` | Login | Público |
| `GET` | `/api/auth/me` | Perfil atual | Autenticado |
| `GET` | `/api/users` | Listar utilizadores | Manager+ |
| `POST` | `/api/users` | Criar utilizador | Admin |
| `PATCH` | `/api/users/:id` | Editar utilizador | Admin |
| `DELETE` | `/api/users/:id` | Desativar utilizador | Admin |
| `GET` | `/api/shifts/templates` | Tipos de turno | Autenticado |
| `POST` | `/api/shifts/templates` | Criar tipo de turno | Admin |
| `GET` | `/api/shifts/assignments` | Ver escalas | Autenticado* |
| `POST` | `/api/shifts/assignments` | Atribuir turno | Manager+ |
| `DELETE` | `/api/shifts/assignments/:id` | Remover turno | Manager+ |
| `GET` | `/api/shifts/requests` | Ver solicitações | Autenticado* |
| `POST` | `/api/shifts/requests` | Criar solicitação | Autenticado |
| `PATCH` | `/api/shifts/requests/:id/review` | Aprovar/Rejeitar | Manager+ |
| `POST` | `/api/shifts/attendance/clock-in` | Entrada | Autenticado |
| `PATCH` | `/api/shifts/attendance/clock-out` | Saída | Autenticado |

*Employees veem apenas os seus próprios dados.

---

## Roles e Permissões

| Ação | Admin | Manager | Employee |
|------|:-----:|:-------:|:--------:|
| Criar/editar utilizadores | ✅ | ❌ | ❌ |
| Gerir tipos de turno | ✅ | ❌ | ❌ |
| Atribuir/remover turnos | ✅ | ✅ | ❌ |
| Ver todas as escalas | ✅ | ✅ | ❌ |
| Ver a própria escala | ✅ | ✅ | ✅ |
| Pedir troca/folga/férias | ✅ | ✅ | ✅ |
| Aprovar/rejeitar pedidos | ✅ | ✅ | ❌ |

---

## CI/CD

O pipeline (`.github/workflows/ci.yml`) executa em cada push/PR para `main` ou `develop`:

1. **Backend**: `npm run lint` (ESLint) + `npm test` (Jest)
2. **Frontend**: `npm run lint` (ESLint) + `npm run build` (Vite)

---

## Equipa

| Nome | Nº |
|------|----|
| | |
| | |
| | |
| | |
