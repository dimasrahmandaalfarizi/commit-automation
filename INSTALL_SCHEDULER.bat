@echo off
:: Minta UAC Admin elevation otomatis
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Meminta izin Administrator...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"%~f0\"' -Verb RunAs -Wait"
    exit /b
)

echo.
echo =====================================================
echo   GitHell v2.0 - Setup Task Scheduler (Admin)
echo =====================================================
echo.

:: Jalankan setup_scheduler.ps1 sebagai admin
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$TaskName='GitHell_DailyAutoCommit';" ^
    "$ScriptPath='d:\Xampp\htdocs\commit-automation-project\auto_daily.bat';" ^
    "$Action=New-ScheduledTaskAction -Execute 'cmd.exe' -Argument ('/c \"'+$ScriptPath+'\"') -WorkingDirectory 'd:\Xampp\htdocs\commit-automation-project';" ^
    "$Trigger=New-ScheduledTaskTrigger -Daily -At '09:00';" ^
    "$Settings=New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Hours 2) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 10) -StartWhenAvailable;" ^
    "$Principal=New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Highest;" ^
    "Register-ScheduledTask -TaskName $TaskName -Action $Action -Trigger $Trigger -Settings $Settings -Principal $Principal -Description 'GitHell v2.0 Auto Commit' -Force | Out-Null;" ^
    "$taskPath = 'C:\Windows\System32\Tasks\' + $TaskName;" ^
    "[xml]$xml = Get-Content $taskPath;" ^
    "$xml.Task.Settings.DisallowStartIfOnBatteries = 'false';" ^
    "$xml.Task.Settings.StopIfGoingOnBatteries = 'false';" ^
    "$xml.Task.Settings.RunOnlyIfNetworkAvailable = 'false';" ^
    "$xml.Save($taskPath);" ^
    "Write-Host '=== SUKSES! Task terdaftar dengan benar ===' -ForegroundColor Green;" ^
    "$info = Get-ScheduledTask -TaskName $TaskName | Get-ScheduledTaskInfo;" ^
    "Write-Host ('Next Run: ' + $info.NextRunTime) -ForegroundColor Cyan;" ^
    "$s=(Get-ScheduledTask -TaskName $TaskName).Settings;" ^
    "Write-Host ('DisallowBattery : ' + $s.DisallowStartIfOnBatteries) -ForegroundColor Cyan;" ^
    "Write-Host ('NetworkRequired : ' + $s.RunOnlyIfNetworkAvailable) -ForegroundColor Cyan;"

echo.
pause
