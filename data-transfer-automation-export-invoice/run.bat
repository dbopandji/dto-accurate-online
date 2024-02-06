@echo off
"cmd" /K "cd C:\git\data-transfer-automation-export-invoice && .\gradlew clean test && .\gradlew runAfterTest"
timeout /t 5 > nul