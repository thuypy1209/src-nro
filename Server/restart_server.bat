@echo off
chcp 65001 >nul
cd /d "%~dp0"
title NRO SERVER RESTARTER
color 0E

echo ========================================================
echo         DANG KHOI DONG LAI NRO SERVER (4GB RAM)
echo ========================================================

echo [1/3] Dang dung tien trinh Server cu neu co...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :14445') do (
    taskkill /F /PID %%a >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8181') do (
    taskkill /F /PID %%a >nul 2>&1
)

timeout /t 2 /nobreak >nul

echo [2/3] Kiem tra port da duoc giai phong...
timeout /t 1 /nobreak >nul

echo [3/3] Dang khoi dong NRO Server moi voi bo nho 4096MB (4GB)...
start "NRO SERVER" java -server -Dfile.encoding=UTF-8 -Xms1024M -Xmx4096M -jar Server.jar

echo ========================================================
echo   Server da duoc restart thanh cong trong cua so moi!
echo ========================================================
timeout /t 3
exit
