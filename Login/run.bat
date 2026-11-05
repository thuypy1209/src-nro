@echo off
chcp 65001 >nul
cd /d "%~dp0"
title NRO LOGIN SERVER
color 0B
java -server -Dfile.encoding=UTF-8 -jar Login.jar
echo.
pause
