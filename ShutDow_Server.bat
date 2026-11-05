@echo off
title DUNG TOAN BO SERVER NRO
color 0C
echo ====================================================
echo        DANG TAT TOAN BO SERVER VA DONG CAC CMD
echo ====================================================
echo.

echo [1/2] Dang tat cac tien trinh Java Server...
taskkill /F /IM java.exe /T 2>nul
taskkill /F /IM javaw.exe /T 2>nul

echo [2/2] Dang dong tat ca cac cua so CMD dang chay...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$myParent = (Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $PID)).ParentProcessId; Get-Process cmd -ErrorAction SilentlyContinue | Where-Object { $_.Id -ne $myParent } | ForEach-Object { taskkill /F /T /PID $_.Id 2>$null }" 2>nul

echo.
echo ====================================================
echo [XONG] Da tat sach Java va dong toan bo cac cua so CMD!
echo ====================================================
echo Cua so se tu dong dong sau 2 giay...
ping 127.0.0.1 -n 3 >nul
exit
