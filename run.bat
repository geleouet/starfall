@echo off
rem Starfall - lanceur double-clic.
setlocal
cd /d "%~dp0"

rem Le code du jeu voyage par ce fichier, et c'est ICI qu'il faut l'effacer -- pas dans la tache
rem Gradle. Si la compilation echoue, la tache de lancement ne demarre jamais, donc son doFirst non
rem plus : run.bat lisait alors le code de la fois d'avant et annoncait un diagnostic perime avec
rem aplomb. Mesure : compilation volontairement cassee apres une capture refusee, run.bat rendait 3
rem « capture non conforme » pour une erreur de compilation. Une mesure qui survit a ce qu'elle
rem mesure est pire que pas de mesure.
if exist "%~dp0lwjgl3\build\starfall-exit-code.txt" del "%~dp0lwjgl3\build\starfall-exit-code.txt"

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

rem Gradle sort 1 des qu'une tache echoue, quel que soit le code du processus : les trois codes du
rem jeu -- 1 plantage, 2 ligne de commande invalide, 3 capture non conforme a la taille demandee --
rem arrivaient tous ici transformes en 1. Ils existaient, ils etaient testes, ils etaient affiches,
rem et le seul appelant que des codes de sortie interessent, un script, ne pouvait pas les lire.
rem La tache de lancement les ecrit donc dans un fichier, efface plus haut avant tout appel a Gradle.
set GAMECODE=
if exist "%~dp0lwjgl3\build\starfall-exit-code.txt" set /p GAMECODE=<"%~dp0lwjgl3\build\starfall-exit-code.txt"
if not "%EXITCODE%"=="0" if defined GAMECODE set EXITCODE=%GAMECODE%

if not "%EXITCODE%"=="0" (
  echo.
  echo Echec du lancement ^(code %EXITCODE%^).
  pause
)
endlocal & exit /b %EXITCODE%
