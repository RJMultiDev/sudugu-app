#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PORT=3001
EXPO_PORT=8081

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
  echo -e "\n${YELLOW}停止服务...${NC}"
  [ -n "$BACKEND_PID" ] && kill $BACKEND_PID 2>/dev/null
  [ -n "$EXPO_PID" ] && kill $EXPO_PID 2>/dev/null
  exit 0
}
trap cleanup SIGINT SIGTERM

# Kill existing
lsof -ti:$BACKEND_PORT 2>/dev/null | xargs -r kill 2>/dev/null
sleep 0.5

echo -e "${GREEN}启动后端 (port $BACKEND_PORT)...${NC}"
cd "$PROJECT_DIR/server"
npx ts-node index.ts &
BACKEND_PID=$!
sleep 2

# Verify backend
if curl -sf http://localhost:$BACKEND_PORT/health > /dev/null 2>&1; then
  echo -e "${GREEN}✓ 后端就绪${NC}"
else
  echo -e "${RED}✗ 后端启动失败${NC}"
  exit 1
fi

echo -e "${GREEN}启动 Expo (port $EXPO_PORT)...${NC}"
cd "$PROJECT_DIR"
npx expo start --lan &
EXPO_PID=$!

echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}  速读谷 开发环境已启动${NC}"
echo -e "${GREEN}  后端 API:  http://localhost:$BACKEND_PORT${NC}"
echo -e "${GREEN}  Expo:      http://localhost:$EXPO_PORT${NC}"
echo -e "${GREEN}  手机 Expo Go 扫码即可预览${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}Ctrl+C 停止所有服务${NC}"

wait
