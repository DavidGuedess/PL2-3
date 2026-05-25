#!/usr/bin/env bash
# MiauGenda — arranca o projeto completo e abre no browser

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DESKTOP="$ROOT/desktop"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m';  GRAY='\033[0;37m'
RED='\033[0;31m';   NC='\033[0m'

echo ""
echo -e "  ${CYAN}MiauGenda Desktop Launcher${NC}"
echo -e "  ${GRAY}---------------------------${NC}"
echo ""

# ── 1. Docker Desktop ─────────────────────────────────────────────────────────
echo -e "[1/4] ${YELLOW}A verificar Docker...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo "      A iniciar Docker Desktop..."
    start "" "C:/Program Files/Docker/Docker/Docker Desktop.exe" 2>/dev/null || true
    elapsed=0
    while ! docker info > /dev/null 2>&1; do
        sleep 3; elapsed=$((elapsed + 3))
        printf "      ... (%ds)\r" "$elapsed"
        if [ $elapsed -ge 90 ]; then
            echo -e "\n  ${RED}ERRO: Docker nao ficou pronto. Abre o Docker Desktop e tenta novamente.${NC}"
            exit 1
        fi
    done
    echo ""
fi
echo -e "      ${GREEN}Docker pronto.${NC}"

# ── 2. Build da imagem (so se necessario) ─────────────────────────────────────
echo -e "[2/4] ${YELLOW}A verificar backend...${NC}"
HASH_FILE="$ROOT/.backend-build-hash"
CURRENT_HASH=$(sha256sum \
    "$ROOT/backend/Dockerfile" \
    "$ROOT/backend/package.json" \
    "$ROOT/backend/package-lock.json" \
    "$ROOT/backend/prisma/schema.prisma" \
    2>/dev/null | sha256sum | cut -d' ' -f1)

if [ "$CURRENT_HASH" != "$(cat "$HASH_FILE" 2>/dev/null)" ]; then
    docker compose down
    docker compose build backend
    if [ $? -ne 0 ]; then
        echo -e "  ${RED}ERRO: Falha no build. Verifica a ligacao a internet e tenta novamente.${NC}"
        exit 1
    fi
    echo "$CURRENT_HASH" > "$HASH_FILE"
fi

docker compose up -d --remove-orphans
if [ $? -ne 0 ]; then
    echo -e "  ${RED}ERRO: Falha ao iniciar os containers.${NC}"
    exit 1
fi

# Verificar crash imediato
sleep 4
if [ "$(docker inspect miaugenda_backend --format='{{.State.Status}}' 2>/dev/null)" = "exited" ]; then
    echo -e "  ${RED}ERRO: O backend nao arrancou. Log de erro:${NC}"
    echo ""
    docker compose logs --tail=20 backend
    exit 1
fi

# Aguarda o backend ficar pronto (max 60s)
elapsed=0
while [ $elapsed -lt 60 ]; do
    curl -sf --max-time 2 http://localhost:3001/health > /dev/null 2>&1 && break
    sleep 2; elapsed=$((elapsed + 2))
    printf "      A aguardar backend... (%ds)\r" "$elapsed"
done
echo ""

if ! curl -sf --max-time 2 http://localhost:3001/health > /dev/null 2>&1; then
    echo -e "  ${RED}ERRO: Backend nao respondeu. Log de erro:${NC}"
    echo ""
    docker compose logs --tail=20 backend
    exit 1
fi
echo -e "      ${GREEN}Backend pronto!${NC}"

# ── 3. Seed (so se a base de dados estiver vazia) ─────────────────────────────
echo -e "[3/4] ${YELLOW}A verificar base de dados...${NC}"
USER_COUNT=$(docker compose exec -T db psql -U miaugenda -d miaugenda -t \
    -c 'SELECT COUNT(*) FROM "User";' 2>/dev/null | tr -d ' \n')

if [ -z "$USER_COUNT" ] || [ "$USER_COUNT" = "0" ]; then
    docker compose exec -T backend npm run db:seed
    if [ $? -ne 0 ]; then
        echo -e "  ${RED}ERRO: Falha ao carregar dados. Verifica os logs acima.${NC}"
        exit 1
    fi
    echo -e "      ${GREEN}Dados carregados!${NC}"
    echo ""
    echo -e "      ${GRAY}  Credenciais de acesso:${NC}"
    echo -e "      ${GRAY}  ADMIN:   EMP001 / password123${NC}"
    echo -e "      ${GRAY}  MANAGER: EMP003 / password123${NC}"
    echo -e "      ${GRAY}  MANAGER: EMP004 / password123${NC}"
    echo ""
else
    echo -e "      ${GREEN}Base de dados pronta ($USER_COUNT utilizadores).${NC}"
fi

# ── 4. Vite dev server ────────────────────────────────────────────────────────
echo -e "[4/4] ${YELLOW}A iniciar interface...${NC}"
cd "$DESKTOP"

if [ ! -d "node_modules" ]; then
    echo "      A instalar dependencias..."
    npm install
fi

echo ""
echo -e "  ${GREEN}Pronto! A abrir http://localhost:5173 no browser...${NC}"
echo -e "  ${GRAY}(fecha esta janela para terminar)${NC}"
echo ""
echo -e "  ${CYAN}Utilizadores disponíveis:${NC}"
echo -e "  ${GRAY}  EMP001 / password123  — Admin       (Administrador 1)${NC}"
echo -e "  ${GRAY}  EMP002 / password123  — Admin       (Administrador 2)${NC}"
echo -e "  ${GRAY}  EMP003 / password123  — Manager     (Gerente 1)${NC}"
echo -e "  ${GRAY}  EMP004 / password123  — Manager     (Gerente 2)${NC}"
echo ""

npm run dev -- --open
