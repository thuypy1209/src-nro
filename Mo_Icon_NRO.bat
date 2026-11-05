@echo off
chcp 65001 >nul
title NRO - COPY ID & MỞ FILE ẢNH ICON TRONG THƯ MỤC
color 0B

:: 1. Tự động tìm thư mục chứa icon trên máy tính
set "ICON_DIR="
if exist "C:\Users\ADMIN\Desktop\SRCNRO\SRC NRO\data\icon\x4" (
    set "ICON_DIR=C:\Users\ADMIN\Desktop\SRCNRO\SRC NRO\data\icon\x4"
) else if exist "%~dp0SRCNRO\SRC NRO\data\icon\x4" (
    set "ICON_DIR=%~dp0SRCNRO\SRC NRO\data\icon\x4"
) else if exist "C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\resources\normal\image\2\icon" (
    set "ICON_DIR=C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\resources\normal\image\2\icon"
) else if exist "%~dp0SRC NRO NEW 01\Server\resources\normal\image\2\icon" (
    set "ICON_DIR=%~dp0SRC NRO NEW 01\Server\resources\normal\image\2\icon"
)

:loop
cls
echo ======================================================================
echo          CÔNG CỤ COPY ID VÀ MỞ VỊ TRÍ FILE ICON TRONG SRC NRO
echo ======================================================================
echo Thư mục Icon hiện tại:
echo %ICON_DIR%
echo ----------------------------------------------------------------------
echo.
set "ICON_ID="
set /p "ICON_ID=>> Nhập mã ID Icon cần mở (hoặc gõ 'q' để thoát): "

if /i "%ICON_ID%"=="q" exit /b
if "%ICON_ID%"=="" goto loop

:: Loại bỏ đuôi .png nếu người dùng quen tay nhập cả đuôi
set "ICON_ID=%ICON_ID:.png=%"
set "ICON_ID=%ICON_ID: =%"

:: 1. Tự động Copy ID vào Clipboard (Bộ nhớ tạm)
<nul set /p="%ICON_ID%" | clip
echo.
echo [OK] DA COPY MA ICON ID: %ICON_ID% VAO CLIPBOARD (BO NHO TAM)!

:: 2. Kiểm tra và mở file trong Windows Explorer
if exist "%ICON_DIR%\%ICON_ID%.png" (
    echo [OK] Đã tìm thấy file "%ICON_ID%.png". Đang mở và chọn đúng file...
    start "" explorer.exe /select,"%ICON_DIR%\%ICON_ID%.png"
) else (
    echo [CHÚ Ý] File "%ICON_ID%.png" chưa có trong thư mục!
    echo Đang mở thư mục Icon để bạn có thể thêm file mới vào...
    start "" explorer.exe "%ICON_DIR%"
)

echo.
echo ======================================================================
echo Bạn có thể nhấn Ctrl + V để dán mã ID này vào bất kỳ đâu.
echo Nhấn phím bất kỳ để tiếp tục nhập ID khác...
pause >nul
goto loop
