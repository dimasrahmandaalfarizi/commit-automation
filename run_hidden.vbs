Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & "d:\Xampp\htdocs\commit-automation-project\auto_daily.bat" & Chr(34), 0
Set WshShell = Nothing
