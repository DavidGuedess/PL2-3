# MiawGenda

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

# SCRUM-55 Gestão básica de utilizadores## Users API (Initial Setup)

Implemented basic user management:

- Seeded initial users (in-memory storage)
- Created endpoint to list users (GET /users)
- Created endpoint to create users (POST /users)
- Implemented user deactivation (PATCH /users/:id/deactivate)

Notes:
- Data is currently stored in-memory (temporary, resets on server restart)
- UUIDs are used for unique user identification