#!/usr/bin/env bash
# Dev helper to start local environment (Linux/macOS)
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# First run: create .env from the template with a fresh random JWT secret.
if [ ! -f .env ]; then
  cp .env.example .env
  SECRET="$(openssl rand -base64 48 | tr -d '\n')"
  # '|' as sed delimiter: base64 can contain '/'
  sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=${SECRET}|" .env
  sed -i.bak "s|^DEMO_MODE=.*|DEMO_MODE=true|" .env   # local machine only: enables the demo account
  rm -f .env.bak
  echo ".env created with a new random JWT_SECRET (demo mode enabled for local use). It is git-ignored: never commit it."
fi

echo "Starting DevPulse local environment with Docker Compose..."

docker compose up -d --build

echo "Waiting for backend to be healthy (approx 10s)"
sleep 10

printf 'To run backend locally without containers:\n  cd backend && export JWT_SECRET=... (see .env) && mvn -DskipTests spring-boot:run\n'
