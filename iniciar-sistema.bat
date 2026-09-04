@echo off
title POS Licoreria - Inicio
echo ============================================
echo   SISTEMA POS LICORERIA
echo ============================================
echo.
echo Requisitos: MySQL corriendo, BD pos_licoreria creada
echo Usuario: admin / admin123
echo URL:     http://localhost:5173
echo.

cd /d "%~dp0"

echo [1/2] Iniciando BACKEND (puerto 8080)...
start "POS Backend" cmd /k "cd /d "%~dp0backend" && mvn spring-boot:run"

timeout /t 15 /nobreak >nul

echo [2/2] Iniciando FRONTEND (puerto 5173)...
start "POS Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo Listo. Abra el navegador en http://localhost:5173
echo Login: admin / admin123
echo.
pause
