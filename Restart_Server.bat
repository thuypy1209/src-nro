@echo off
chcp 65001 >nul
cd /d "%~dp0"
title NRO RESTART SERVER
color 0E

echo ====================================================
echo        TIEN HANH KHOI DONG LAI HE THONG NRO
echo ====================================================
echo.

echo [1/4] Dang tat cac tien trinh Server cu (Login, Game Server, Java)...
taskkill /F /IM java.exe /T 2>nul
taskkill /F /IM javaw.exe /T 2>nul

echo [2/4] Cho giai phong cong mang va bo nho (3 giay)...
timeout /t 3 /nobreak >nul

echo [3/4] Dang khoi dong NRO Login Server...
start "NRO LOGIN SERVER" /d "%~dp0Login" cmd /k run.bat

timeout /t 2 /nobreak >nul

echo [4/4] Dang khoi dong NRO Game Server (RAM Toi Da: 4096 MB)...
start "NRO GAME SERVER" /d "%~dp0Server" cmd /k run.bat

echo.
echo ====================================================
echo [THANH CONG] Da khoi dong lai toan bo Server!
echo Web Admin: http://localhost:8181/
echo RAM Server: 1024 MB - 4096 MB (4 GB)
echo ====================================================
echo Cua so nay se tu dong dong sau 5 giay...
timeout /t 5 >nul
exit
