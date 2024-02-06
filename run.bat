@echo off
"cmd" /K "cd C:\data-transfer-automation && .\gradlew clean test && .\gradlew runAfterTest"
timeout /t 5 > nul