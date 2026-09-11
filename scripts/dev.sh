#!/usr/bin/env bash
# Dev helper to start local environment (Linux/macOS)
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "Starting DevPulse local environment with Docker Compose..."

docker compose up -d --build

echo "Waiting for backend to be healthy (approx 10s)"
sleep 10

echo "To run backend locally without containers:\n  cd backend && mvn -DskipTests spring-boot:run"
