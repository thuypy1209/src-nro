@echo off
chcp 65001 >nul
cd /d "%~dp0"
title NRO SERVER
color 0A
:: Bo nho khoi tao: 1024MB (1GB), Bo nho toi da: 4096MB (4GB)
java -server -Dfile.encoding=UTF-8 -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xms1024M -Xmx4096M -jar Server.jar
echo.
pause
