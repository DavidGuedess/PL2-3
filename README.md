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
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop) — tem de estar instalado e em execução
- [Node.js 20+](https://nodejs.org)
- Git Bash (Windows) ou terminal Unix

> A rede da universidade pode bloquear o Docker Hub. Usa hotspot ou faz o setup em casa.

---

## Arrancar o projeto

O script `run-desktop.sh` trata de tudo automaticamente: verifica o Docker, faz build se necessário, corre as migrações, faz seed da base de dados e abre a app no browser.

```bash
./run-desktop.sh
```

Abrir em: **http://localhost:5173**

---

## Credenciais de login

Todos os utilizadores têm a password `password123`.

| Número | Nome | Role |
|---|---|---|
| `ADM001` | David Guedes | Administrador |
| `ADM002` | Xavier Bolotinha | Administrador |
| `GER001` | Leonardo Andronési | Gerente |
| `GER002` | Diogo Silva | Gerente |
| `EMP001` | Mariana Ferreira | Funcionário |
| `EMP002` | Carlos Rodrigues | Funcionário |

---

## Permissões por role

| Funcionalidade | Administrador | Gerente | Funcionário |
|---|---|---|---|
| Gerir utilizadores | ✓ | — | — |
| Gerir turnos | ✓ | ✓ | — |
| Ver escala | ✓ | ✓ | ✓ |
| Aprovar pedidos de folga | ✓ | ✓ | — |
| Aprovar trocas de turno | ✓ | ✓ | — |
| Registo de ponto | ✓ | ✓ | ✓ |
| Mensagens | ✓ | ✓ | ✓ |

---

## Comandos úteis

```bash
# Resetar e re-popular a base de dados
docker compose exec -T backend npm run db:seed

# Parar todos os containers
docker compose down

# Ver logs do backend
docker compose logs -f backend

# Nova migração após alterar o schema.prisma
cd backend && npx prisma migrate dev --name nome-da-migracao

# Abrir Prisma Studio (interface visual da BD)
cd backend && npx prisma studio
```

---

## URLs

| Serviço | URL |
|---|---|
| App Desktop | http://localhost:5173 |
| Backend API | http://localhost:3001 |
| Swagger (docs API) | http://localhost:3001/api-docs |

---

## Estrutura do projeto

```
PL2-4/
├── backend/          # API REST (Node.js + Express + Prisma)
│   ├── src/
│   │   ├── routes/   # Endpoints da API
│   │   ├── prisma/   # Schema e seed
│   │   └── middleware/
│   └── Dockerfile
├── desktop/          # App web (React + Vite)
│   └── src/
│       ├── screens/  # Ecrãs da aplicação
│       ├── api/      # Chamadas ao backend
│       └── components/
├── android/          # App móvel (Kotlin)
├── docker-compose.yml
└── run-desktop.sh    # Script de arranque
```
