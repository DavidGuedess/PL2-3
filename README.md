# PawSchedule
Sistema de Gestao de Turnos para Clinica Veterinaria
LES 2025/26 - Tema T9 | Equipa PL2-4

---

## Como arrancar o projeto

**Pre-requisitos:** Docker Desktop e Git instalados.

```bash
# 1. Clonar o repositorio
git clone https://github.com/DavidGuedess/PL2-4
cd PL2-4

# 2. Configurar variaveis de ambiente
cp .env.example .env

# 3. Arrancar os servicos
docker-compose up --build
```

Num segundo terminal, quando os servicos estiverem a correr:
```bash
# 4. Criar tabelas e dados iniciais (so na primeira vez)
docker-compose exec backend npx prisma migrate dev --name init
docker-compose exec backend npm run db:seed
```

Abre http://localhost:5173 e faz login com `ADM001` / `admin123`.

---

## Como desenvolver

O fluxo de trabalho da equipa deve ser o seguinte:

- **main** — codigo estavel, so recebe Pull Requests aprovados
- **develop** — branch de integracao da equipa
- **feature/us-XX-nome** — uma branch por User Story

### Comecar uma nova tarefa
```bash
# Partir sempre de develop atualizado
git checkout develop
git pull origin develop
git checkout -b feature/us-XX-nome
```

### Guardar e partilhar o trabalho
```bash
git add .
git commit -m "feat: descricao do que fizeste"
git push origin feature/us-XX-nome
```

Depois abre um Pull Request no GitHub de `feature/us-XX-nome` para `develop`.
O PR precisa de pelo menos 1 aprovacao de um colega antes de fazer merge.

### Comandos uteis
```bash
# Parar os servicos
docker-compose down

# Ver logs do backend
docker-compose logs -f backend

# Nova migracao apos alterar o schema.prisma
docker-compose exec backend npx prisma migrate dev --name nome-da-migracao

# Ver base de dados visualmente
docker-compose exec backend npx prisma studio
```
