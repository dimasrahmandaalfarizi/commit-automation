# ================================================================
# setup_scheduler.ps1 - GitHell v2.0
# Daftarkan GitHell ke Windows Task Scheduler
# Jalankan 1x saja sebagai Administrator!
# ================================================================

$TaskName   = "GitHell_DailyAutoCommit"
$ScriptPath = "d:\Xampp\htdocs\commit-automation-project\auto_daily.bat"
$RunTime    = "09:00"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  GitHell v2.0 - Setup Windows Task Scheduler" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $ScriptPath)) {
    Write-Host "[ERROR] File tidak ditemukan: $ScriptPath" -ForegroundColor Red
    exit 1
}

# Hapus task lama jika ada
$existingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-Host "[INFO] Task lama ditemukan. Menghapus..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
}

$Action = New-ScheduledTaskAction `
    -Execute "cmd.exe" `
    -Argument "/c `"$ScriptPath`"" `
    -WorkingDirectory "d:\Xampp\htdocs\commit-automation-project"

$Trigger = New-ScheduledTaskTrigger -Daily -At $RunTime

$Settings = New-ScheduledTaskSettingsSet `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2) `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 10) `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false `      # FIX: jalan meski network belum ready
    -DisallowStartIfOnBatteries:$false `     # FIX: jalan meski pakai baterai
    -StopIfGoingOnBatteries:$false `         # FIX: tidak berhenti saat baterai
    -WakeToRun:$false                        # Tidak perlu wake PC dari sleep

$Principal = New-ScheduledTaskPrincipal `
    -UserId $env:USERNAME `
    -LogonType InteractiveToken `            # Lebih reliable dari S4U
    -RunLevel Highest                        # Run as admin

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $Action `
    -Trigger $Trigger `
    -Settings $Settings `
    -Principal $Principal `
    -Description "GitHell v2.0: Auto commit ke GitHub setiap hari jam $RunTime" `
    -Force

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host "  BERHASIL! Task Scheduler sudah diperbarui." -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Detail:" -ForegroundColor White
Write-Host "     Nama Task     : $TaskName" -ForegroundColor White
Write-Host "     Jadwal        : Setiap hari jam $RunTime" -ForegroundColor White
Write-Host "     Script        : $ScriptPath" -ForegroundColor White
Write-Host ""
Write-Host "  Perbaikan v2.0:" -ForegroundColor Cyan
Write-Host "     [+] Jalan meski laptop pakai BATERAI" -ForegroundColor Cyan
Write-Host "     [+] Jalan meski network belum konek saat jam $RunTime" -ForegroundColor Cyan
Write-Host "     [+] StartWhenAvailable: jalan begitu PC menyala" -ForegroundColor Cyan
Write-Host "     [+] Restart otomatis 3x jika gagal" -ForegroundColor Cyan
Write-Host ""

# Verifikasi
Write-Host "  Verifikasi:" -ForegroundColor Yellow
$info = Get-ScheduledTask -TaskName $TaskName | Get-ScheduledTaskInfo
Write-Host "     Next Run : $($info.NextRunTime)" -ForegroundColor Yellow
Write-Host "     State    : $((Get-ScheduledTask -TaskName $TaskName).State)" -ForegroundColor Yellow
Write-Host ""
