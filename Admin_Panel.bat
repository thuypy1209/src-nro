@echo off
cd /d "%~dp0Server"
title NRO ADMIN PANEL - SRC NRO NEW 01
color 0B
echo ====================================================
echo      DANG MO PANEL QUAN TRI SERVER (JFRAME)
echo ====================================================
echo.
start javaw -cp Server.jar nro.panel.AdminPanelFrame
echo [XONG] Da mo giao dien Admin Panel JFrame thanh cong!
echo.
pause
