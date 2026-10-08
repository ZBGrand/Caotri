@echo off
setlocal
echo Building WeeklyGacha with Maven...
mvn clean package
if errorlevel 1 (
  echo Build failed. Check Java 21, Maven, and network access to Maven repositories.
  pause
  exit /b 1
)
echo Build successful: target\WeeklyGacha-1.0.0.jar
pause
