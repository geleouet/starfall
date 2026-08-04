@echo off
rem Starfall - lanceur double-clic.
setlocal
cd /d "%~dp0"

echo Lancement de Starfall...
rem Les arguments supplementaires sont transmis au jeu, par exemple :
rem   run.bat --screenshot captures\m1 --size 1280x720
rem Gradle refuse un --args vide, donc on ne passe l'option que s'il y a quelque chose a transmettre.
rem Les deux branches sont ecrites en toutes lettres plutot que via une variable : une variable
rem reinjectee sans guillemets casserait sur un chemin ou un argument contenant des espaces.
if "%~1"=="" (
  call "%~dp0gradlew.bat" --quiet --console=plain :lwjgl3:run
) else (
  call "%~dp0gradlew.bat" --quiet --console=plain :lwjgl3:run --args="%*"
)
set EXITCODE=%ERRORLEVEL%

if not "%EXITCODE%"=="0" (
  echo.
  echo Echec du lancement ^(code %EXITCODE%^).
  pause
)
endlocal & exit /b %EXITCODE%
