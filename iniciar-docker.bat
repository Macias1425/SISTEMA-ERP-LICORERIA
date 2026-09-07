@echo off
title POS Licoreria - Docker
cd /d "%~dp0"

if not exist ".env" (
  echo Creando .env desde .env.example...
  copy /Y ".env.example" ".env" >nul
)

echo Levantando MySQL + Backend + Frontend...
docker compose up --build
pause
