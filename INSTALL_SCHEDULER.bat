@echo off
title Setup GitHell Task Scheduler
echo Membuka PowerShell sebagai Administrator...
echo.
powershell -Command "Start-Process powershell -ArgumentList '-ExecutionPolicy Bypass -File \"%~dp0setup_scheduler.ps1\"' -Verb RunAs -Wait"
echo.
echo Selesai. Tekan tombol apapun untuk keluar.
pause > nul
