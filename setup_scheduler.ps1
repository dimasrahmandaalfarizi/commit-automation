# ================================================================
# setup_scheduler.ps1
# Daftarkan GitHell ke Windows Task Scheduler
# Jalankan 1x saja sebagai Administrator!
# ================================================================

$TaskName    = "GitHell_DailyAutoCommit"
$ScriptPath  = "d:\Xampp\htdocs\commit-automation-project\auto_daily.bat"
$RunTime     = "09:00"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  GitHell - Setup Windows Task Scheduler" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $ScriptPath)) {
    Write-Host "[ERROR] File tidak ditemukan: $ScriptPath" -ForegroundColor Red
    exit 1
}

$existingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-Host "[INFO] Task sudah ada. Menghapus versi lama..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
}

$Action = New-ScheduledTaskAction `
    -Execute "cmd.exe" `
    -Argument "/c `"$ScriptPath`"" `
    -WorkingDirectory "d:\Xampp\htdocs\commit-automation-project"

$Trigger = New-ScheduledTaskTrigger -Daily -At $RunTime

$Settings = New-ScheduledTaskSettingsSet `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 5) `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable

$Principal = New-ScheduledTaskPrincipal `
    -UserId $env:USERNAME `
    -LogonType S4U `
    -RunLevel Limited

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $Action `
    -Trigger $Trigger `
    -Settings $Settings `
    -Principal $Principal `
    -Description "GitHell: Auto 20 commit ke GitHub setiap hari jam $RunTime"

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host "  BERHASIL! Task Scheduler sudah diatur." -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Detail:" -ForegroundColor White
Write-Host "     Nama Task  : $TaskName" -ForegroundColor White
Write-Host "     Jadwal     : Setiap hari jam $RunTime" -ForegroundColor White
Write-Host "     Script     : $ScriptPath" -ForegroundColor White
Write-Host "     Log Folder : d:\Xampp\htdocs\commit-automation-project\logs\" -ForegroundColor White
Write-Host ""
Write-Host "  Tips:" -ForegroundColor Yellow
Write-Host "     - Pastikan PC/laptop NYALA jam $RunTime" -ForegroundColor Yellow
Write-Host "     - Kalau mau ganti jam, edit variabel RunTime di file ini lalu run ulang" -ForegroundColor Yellow
Write-Host "     - Cek log di folder logs kalau ada masalah" -ForegroundColor Yellow
Write-Host "     - Buka Task Scheduler > Task Scheduler Library untuk lihat status" -ForegroundColor Yellow
Write-Host ""
