#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/.logs"
mkdir -p "$LOG_DIR"

# Colors
RED=$'\033[0;31m'
GREEN=$'\033[0;32m'
YELLOW=$'\033[1;33m'
BLUE=$'\033[0;34m'
MAGENTA=$'\033[0;35m'
CYAN=$'\033[0;36m'
BOLD=$'\033[1m'
NC=$'\033[0m'

# Load SDKMAN / NVM if available in home directory
if [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  set +u
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  set -u
fi

if [[ -s "$HOME/.nvm/nvm.sh" ]]; then
  set +u
  # shellcheck disable=SC1091
  source "$HOME/.nvm/nvm.sh"
  set -u
fi

PIDS=()

stop_ports() {
  local ports=(8080 8081 5173 5174)
  for port in "${ports[@]}"; do
    if command -v fuser >/dev/null 2>&1; then
      fuser -k "${port}/tcp" >/dev/null 2>&1 || true
    elif command -v lsof >/dev/null 2>&1; then
      local pids
      pids=$(lsof -ti :"${port}" 2>/dev/null || true)
      if [[ -n "$pids" ]]; then
        # shellcheck disable=SC2086
        kill -TERM $pids 2>/dev/null || true
      fi
    fi
  done
}

cleanup() {
  trap - SIGINT SIGTERM
  echo ""
  echo "${YELLOW}[INFO] Deteniendo todos los servicios de Dulce Mayor...${NC}"
  for pid in "${PIDS[@]:-}"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill -TERM "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
    fi
  done
  stop_ports
  echo "${GREEN}[OK] Todos los servicios fueron detenidos correctamente.${NC}"
  exit 0
}

usage() {
  echo "${BOLD}Uso:${NC} ./start.sh [opción]"
  echo ""
  echo "Opciones:"
  echo "  (sin argumentos)   Levanta el backend y las 3 aplicaciones frontend"
  echo "  --stop             Detiene cualquier instancia activa en los puertos 8080, 8081, 5173 y 5174"
  echo "  --help, -h         Muestra esta ayuda"
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "${1:-}" == "--stop" ]]; then
  echo "${YELLOW}[INFO] Liberando puertos 8080, 8081, 5173 y 5174...${NC}"
  stop_ports
  echo "${GREEN}[OK] Puertos liberados.${NC}"
  exit 0
fi

trap cleanup SIGINT SIGTERM

echo "${BOLD}Dulce Mayor - Iniciador del Ecosistema Completo${NC}"

# Verify dependencies
if ! command -v java >/dev/null 2>&1; then
  echo "${RED}[ERROR] 'java' no está instalado o no se encuentra en el PATH.${NC}"
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "${RED}[ERROR] 'npm' no está instalado o no se encuentra en el PATH.${NC}"
  exit 1
fi

# Ensure Maven wrappers are executable
chmod +x "$ROOT_DIR/server/mvnw" "$ROOT_DIR/client/landing/mvnw"

# Prepare frontend dependencies and env files
prepare_react_app() {
  local app_dir="$1"
  local app_name="$2"

  if [[ -f "$app_dir/.env.example" && ! -f "$app_dir/.env" ]]; then
    echo "${CYAN}[SETUP] Creando archivo .env por defecto en ${app_name}...${NC}"
    cp "$app_dir/.env.example" "$app_dir/.env"
  fi

  if [[ ! -d "$app_dir/node_modules" ]]; then
    echo "${CYAN}[SETUP] Instalando dependencias npm en ${app_name}...${NC}"
    (cd "$app_dir" && npm install)
  fi
}

prepare_react_app "$ROOT_DIR/client/central-management" "client/central-management"
prepare_react_app "$ROOT_DIR/client/order-management" "client/order-management"

# Free ports if already occupied from a previous session
stop_ports

# Helper to launch a service in its own process group with prefixed output and log file
start_service() {
  local name="$1"
  local color="$2"
  local workdir="$3"
  local logfile="$4"
  shift 4
  local prefix
  prefix=$(printf "%s[%-10s]%s" "$color" "$name" "$NC")

  : > "$logfile"
  setsid bash -c '
    workdir="$1"
    logfile="$2"
    prefix="$3"
    shift 3
    cd "$workdir" || exit 1
    "$@" 2>&1 | while IFS= read -r line; do
      printf "%s\n" "$line" >> "$logfile"
      printf "%s %s\n" "$prefix" "$line"
    done
  ' _ "$workdir" "$logfile" "$prefix" "$@" &
  PIDS+=($!)
}

# Build classpath from cached ~/.m2 repository if available (avoids JDK 27 javac + Lombok issue)
build_java_cmd() {
  local app_dir="$1"
  local main_class="$2"
  local cp_file="$app_dir/.mvn/runtime-classpath.txt"
  local m2_repo="${HOME}/.m2/repository"

  if [[ -d "$app_dir/target/classes" && -f "$cp_file" ]]; then
    local cp="$app_dir/target/classes"
    local all_found=true
    while IFS= read -r rel_jar; do
      [[ -z "$rel_jar" ]] && continue
      local full_jar="$m2_repo/$rel_jar"
      if [[ -f "$full_jar" ]]; then
        cp="${cp}:${full_jar}"
      else
        all_found=false
        break
      fi
    done < "$cp_file"

    if [[ "$all_found" == true ]]; then
      JAVA_CMD=(java -cp "$cp" "$main_class")
      return 0
    fi
  fi

  JAVA_CMD=(./mvnw spring-boot:run)
}

echo "${BLUE}[START] Iniciando Backend REST API (puerto 8080)...${NC}"
build_java_cmd "$ROOT_DIR/server" "ing.software.dulcemayor.DulceMayorApplication"
start_service "SERVER" "$BLUE" "$ROOT_DIR/server" "$LOG_DIR/server.log" "${JAVA_CMD[@]}"

echo "${MAGENTA}[START] Iniciando Landing Page Thymeleaf (puerto 8081)...${NC}"
build_java_cmd "$ROOT_DIR/client/landing" "ing.software.dulcemayor.landing.LandingApplication"
start_service "LANDING" "$MAGENTA" "$ROOT_DIR/client/landing" "$LOG_DIR/landing.log" "${JAVA_CMD[@]}"

echo "${GREEN}[START] Iniciando Central Management React (puerto 5173)...${NC}"
start_service "CENTRAL" "$GREEN" "$ROOT_DIR/client/central-management" "$LOG_DIR/central-management.log" npm run dev -- --port 5173 --strictPort

echo "${CYAN}[START] Iniciando Order Management React (puerto 5174)...${NC}"
start_service "ORDER" "$CYAN" "$ROOT_DIR/client/order-management" "$LOG_DIR/order-management.log" npm run dev -- --port 5174 --strictPort

echo ""
echo "${BOLD}------------------------------------------------------------${NC}"
echo "${BOLD}Servicios levantándose:${NC}"
echo "  • ${BLUE}Backend API:${NC}          http://localhost:8080/api"
echo "  • ${BLUE}Consola H2:${NC}           http://localhost:8080/h2-console"
echo "  • ${MAGENTA}Landing Page:${NC}         http://localhost:8081"
echo "  • ${GREEN}Central Management:${NC}   http://localhost:5173"
echo "  • ${CYAN}Order Management:${NC}     http://localhost:5174"
echo ""
echo "  Logs guardados en: ${BOLD}.logs/${NC}"
echo "  Presiona ${BOLD}Ctrl+C${NC} para detener todos los servicios."
echo "${BOLD}------------------------------------------------------------${NC}"
echo ""

wait
