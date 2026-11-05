@echo off
chcp 65001 >nul
cd /d "%~dp0"
title NRO LAUNCHER

echo ====================================================
echo        KHOI DONG HE THONG MAY CHU NRO
echo ====================================================
echo.


echo [1/2] Dang khoi dong Login Server...
start "NRO LOGIN SERVER" /d "%~dp0Login" cmd /k run.bat

timeout /t 2 /nobreak >nul

echo [2/2] Dang khoi dong Game Server...
start "NRO GAME SERVER" /d "%~dp0Server" cmd /k run.bat

echo.
echo ====================================================
echo Ca hai Server va MySQL da duoc khoi dong thanh cong!
echo ====================================================
pause
