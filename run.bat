@echo off
rem Starfall - double-click launcher.
setlocal
cd /d "%~dp0"

echo Lancement de Starfall...
rem Any extra arguments are forwarded to the game, e.g.
rem   run.bat --screenshot captures\m1 --size 1280x720
call "%~dp0gradlew.bat" --quiet --console=plain :lwjgl3:run --args="%*"
set EXITCODE=%ERRORLEVEL%

if not "%EXITCODE%"=="0" (
  echo.
  echo Echec du lancement ^(code %EXITCODE%^).
  pause
)
endlocal & exit /b %EXITCODE%
