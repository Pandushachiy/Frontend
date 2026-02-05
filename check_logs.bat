@echo off
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" devices > "%~dp0adb_devices.txt" 2>&1
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat -d -t 200 > "%~dp0adb_logs.txt" 2>&1
echo Done
