@echo off
rem Starfall - lanceur double-clic, et point d'entree des captures automatisees.
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem Le fichier par lequel le code de sortie du jeu voyage. Le chemin est ecrit ICI et transmis a
rem Gradle : il vivait auparavant a deux endroits et dans deux langages, si bien qu'un repertoire de
rem build deplace aurait fait retomber ce script sur le code de Gradle sans un mot.
set "EXITFILE=%~dp0lwjgl3\build\starfall-exit-code.txt"

rem Efface AVANT tout appel a Gradle, et c'est ici qu'il faut le faire -- pas dans la tache. Si la
rem compilation echoue, la tache de lancement ne demarre jamais, donc son doFirst non plus : ce
rem script lisait alors le code de la fois d'avant. Mesure : compilation volontairement cassee apres
rem une capture refusee, run.bat rendait 3 « capture non conforme » pour une erreur de compilation.
rem Une mesure qui survit a ce qu'elle mesure est pire que pas de mesure.
if exist "%EXITFILE%" del "%EXITFILE%"

echo Lancement de Starfall...
rem Les arguments supplementaires sont transmis au jeu, par exemple :
rem   run.bat --screenshot captures\m1 --size 1280x720
rem Gradle refuse un --args vide, donc on ne passe l'option que s'il y a quelque chose a transmettre.
rem
rem Les guillemets de %* sont ECHAPPES avant d'entrer dans --args="...". Sans cela, un chemin
rem contenant une espace -- le cas le plus banal sous Windows -- coupait la chaine en deux et Gradle
rem cherchait une tache nommee d'apres la fin du chemin. Mesure : run.bat --screenshot "C:\mes
rem shots" --size 640x360 rendait « Task 'shots --size 640x360' not found ». Le commentaire qui
rem occupait cette place affirmait au contraire que les deux branches ecrites en toutes lettres
rem protegeaient des espaces ; elles n'en protegeaient pas.
rem L'echappement vit HORS de tout bloc « ( ) », et ce n'est pas cosmetique : la substitution
rem contient un nombre impair de guillemets, ce qui suffit a casser l'analyse du bloc qui
rem l'entourait. Mesure : Gradle recevait alors une propriete VIDE et refusait de creer la tache
rem (« Cannot convert '' to File »), sur les quatre chemins a la fois.
set "GAMEARGS=%*"
if defined GAMEARGS set "GAMEARGS=!GAMEARGS:"=\"!"

if defined GAMEARGS goto :avec_arguments
call "%~dp0gradlew.bat" --quiet --console=plain :lwjgl3:run "-Pstarfall.exitCodeFile=%EXITFILE%"
goto :lance
:avec_arguments
call "%~dp0gradlew.bat" --quiet --console=plain :lwjgl3:run "-Pstarfall.exitCodeFile=%EXITFILE%" --args="!GAMEARGS!"
:lance
set EXITCODE=%ERRORLEVEL%

rem Gradle sort 1 des qu'une tache echoue, quel que soit le code du processus : les trois codes du
rem jeu -- 1 plantage, 2 ligne de commande invalide, 3 capture non conforme a la taille demandee --
rem arrivaient tous ici transformes en 1. Ils existaient, ils etaient testes, ils etaient affiches,
rem et le seul appelant que des codes de sortie interessent, un script, ne pouvait pas les lire.
rem
rem Le code lu ne remplace celui de Gradle QUE s'il est lui-meme non nul. Sans cette condition, un
rem jeu sorti proprement pendant qu'une etape ULTERIEURE de la construction echouait faisait rendre
rem 0 a ce script : le symetrique exact du defaut ci-dessus -- un code frais servi par-dessus un
rem echec au lieu d'un code perime servi apres un echec.
set GAMECODE=
if exist "%EXITFILE%" set /p GAMECODE=<"%EXITFILE%"
if not "%EXITCODE%"=="0" if defined GAMECODE if not "!GAMECODE!"=="0" set EXITCODE=!GAMECODE!

if not "%EXITCODE%"=="0" (
  echo.
  echo Echec du lancement ^(code %EXITCODE%^).
  rem La pause n'a de sens que pour un double-clic, ou la fenetre se refermerait avant qu'on ait lu
  rem le message. Lancee avec des arguments -- c'est-a-dire par un script, le seul appelant que ces
  rem codes de sortie interessent -- elle suspendait indefiniment celui a qui on venait justement de
  rem rendre le code lisible. STARFALL_NO_PAUSE la desactive aussi dans le cas sans argument.
  if "%~1"=="" if not defined STARFALL_NO_PAUSE pause
)
endlocal & exit /b %EXITCODE%
