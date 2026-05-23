@echo off
title GitHell Auto-Commit

REM Paksa batch membaca PATH terbaru (supaya javac dikenali meski VS Code belum direstart)
for /f "delims=" %%i in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [System.Environment]::GetEnvironmentVariable('Path','User')"') do set "PATH=%%i"

echo.
echo [1] Mengkompilasi GitHell.java...
javac GitHell.java
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Gagal mengkompilasi GitHell.java! Pastikan Java terinstall.
    pause
    exit /b
)

echo.
echo [2] Menjalankan GitHell... (Program akan berjalan otomatis setiap 5 menit)
echo [INFO] Untuk MENGHENTIKAN program ini, cukup TUTUP JENDELA HITAM ini (klik tombol X di pojok kanan atas)
echo ------------------------------------------------------------------------------------------------------
echo.

java GitHell

pause
