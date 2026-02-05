$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$outFile = "$PSScriptRoot\adb_result.txt"

"=== ADB DEVICES ===" | Out-File $outFile
& $adbPath devices 2>&1 | Out-File $outFile -Append

"" | Out-File $outFile -Append
"=== LOGCAT ERRORS ===" | Out-File $outFile -Append
& $adbPath logcat -d -t 300 2>&1 | Where-Object { $_ -match "Upload|Document|Error|Exception|health.companion" } | Out-File $outFile -Append

Write-Host "Done! Check adb_result.txt"
