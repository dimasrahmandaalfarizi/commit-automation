@echo off
title GitHell v2.0 - Daily Auto Commit (Task Scheduler)

REM Paksa ambil PATH terbaru termasuk Java
for /f "delims=" %%i in ('powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [System.Environment]::GetEnvironmentVariable('Path','User')"') do set "PATH=%%i"

REM Pindah ke folder project
cd /d "d:\Xampp\htdocs\commit-automation-project"

REM === FIX: Format tanggal YYYY-MM-DD (bukan YYYY-DD-MM yang buggy) ===
for /f "tokens=1-3 delims=/" %%a in ('powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd'"') do set "DATESTR=%%a-%%b-%%c"
for /f %%a in ('powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd'"') do set "DATESTR=%%a"

REM Set output log dengan tanggal yang benar
set LOGFILE=d:\Xampp\htdocs\commit-automation-project\logs\daily_%DATESTR%.log

REM Buat folder logs jika belum ada
if not exist "d:\Xampp\htdocs\commit-automation-project\logs" (
    mkdir "d:\Xampp\htdocs\commit-automation-project\logs"
)

echo [%date% %time%] ===== GitHell v2.0 Daily Run START ===== >> "%LOGFILE%"

REM Compile JavaFile
echo [%date% %time%] Compiling GitHell.java... >> "%LOGFILE%"
javac GitHell.java >> "%LOGFILE%" 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [%date% %time%] ERROR: Compile gagal! >> "%LOGFILE%"
    exit /b 1
)

REM Jalankan dalam mode --daily (20 commit lalu exit)
echo [%date% %time%] Running daily mode... >> "%LOGFILE%"
java GitHell --daily >> "%LOGFILE%" 2>&1

echo [%date% %time%] ===== GitHell v2.0 Daily Run END ===== >> "%LOGFILE%"
echo. >> "%LOGFILE%"
