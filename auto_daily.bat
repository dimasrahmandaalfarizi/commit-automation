@echo off
title GitHell - Daily Auto Commit (Task Scheduler)

REM Paksa ambil PATH terbaru termasuk Java
for /f "delims=" %%i in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [System.Environment]::GetEnvironmentVariable('Path','User')"') do set "PATH=%%i"

REM Pindah ke folder project
cd /d "d:\Xampp\htdocs\commit-automation-project"

REM Set output log dengan tanggal
set LOGFILE=d:\Xampp\htdocs\commit-automation-project\logs\daily_%date:~-4,4%-%date:~-7,2%-%date:~-10,2%.log

REM Buat folder logs jika belum ada
if not exist "d:\Xampp\htdocs\commit-automation-project\logs" (
    mkdir "d:\Xampp\htdocs\commit-automation-project\logs"
)

echo [%date% %time%] ===== GitHell Daily Run START ===== >> "%LOGFILE%"

REM Compile JavaFile
echo [%date% %time%] Compiling... >> "%LOGFILE%"
javac GitHell.java >> "%LOGFILE%" 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [%date% %time%] ERROR: Compile gagal! >> "%LOGFILE%"
    exit /b 1
)

REM Jalankan dalam mode --daily (20 commit lalu exit)
echo [%date% %time%] Running daily mode... >> "%LOGFILE%"
java GitHell --daily >> "%LOGFILE%" 2>&1

echo [%date% %time%] ===== GitHell Daily Run END ===== >> "%LOGFILE%"
echo. >> "%LOGFILE%"
