@echo off
title DUNG TOAN BO SERVER NRO
color 0C

:: Header
echo ====================================================
echo        DANG TAT TOAN BO SERVER VA DONG CAC CMD
echo ====================================================

:: Kill Java processes (project specific)
taskkill /F /IM java.exe /T 2>nul
taskkill /F /IM javaw.exe /T 2>nul

:: Kill only cmd windows that were started from this project
powershell -NoProfile -ExecutionPolicy Bypass -Command "$myPid = $PID; Get-CimInstance Win32_Process -Filter \"Name = 'cmd.exe'\" | Where-Object { $_.ProcessId -ne $myPid -and $_.CommandLine -match 'SRC NRO' } | ForEach-Object { taskkill /F /T /PID $_.ProcessId 2>$null }" 2>nul

:: Finish
echo.
echo ====================================================
echo [XONG] Da tat sach Java va dong toan bo cac cua so CMD cua project!
echo ====================================================
echo Cua so se tu dong dong sau 2 giay...
ping 127.0.0.1 -n 3 >nul
exit
