@echo off
rem Le garde-fou du lanceur : run.bat rend-il encore les bons codes de sortie ?
rem
rem POURQUOI UN SCRIPT ET PAS UNE TACHE GRADLE. run.bat appelle gradlew ; une tache Gradle qui le
rem lancerait imbriquerait une construction dans une autre, sur le meme projet et les memes verrous.
rem Ce controle vit donc a cote, comme verifyRender vit hors de « test » : il demande un ecran, et il
rem se lance a la main.
rem
rem POURQUOI IL EXISTE. Les trois codes du jeu -- 1 plantage, 2 ligne de commande invalide, 3 capture
rem non conforme -- n'ont d'interet que pour un script. Ils ont mis quatre reprises a arriver
rem jusqu'a lui, et chacune a casse quelque chose que la precedente avait etabli : un code perime
rem servi apres un echec de compilation, un zero frais servi par-dessus un echec, un chemin
rem contenant une espace coupe en deux, et une substitution de guillemets qui cassait l'analyse du
rem bloc qui l'entourait -- celle-la mettant les quatre chemins par terre d'un coup. Chaque fois,
rem seule une verification a la main l'a vu. Une verification faite a la main est une verification
rem qu'on finit par ne plus faire.

setlocal enabledelayedexpansion
cd /d "%~dp0"

rem Sans cela, un echec attendu suspendrait ce script sur « Appuyez sur une touche ».
set STARFALL_NO_PAUSE=1
set "SHOTS=%TEMP%\starfall-verify-launcher"
set "SPACED=%TEMP%\starfall verify launcher"
set FAILURES=0

call :expect 2 "ligne de commande invalide" --grid 99
call :expect 3 "capture non conforme a la taille demandee" --screenshot "%SHOTS%" --size 6000x4000
rem Immediatement apres le cas precedent, et l'ordre est le fond du controle : le fichier de code
rem contient maintenant 3. Si la construction echoue AVANT que la tache demarre -- la forme exacte
rem d'une compilation cassee, ou ni doFirst ni doLast ne s'executent --, run.bat doit rendre 1 et
rem non ce 3 perime. C'est ce que son effacement prealable garantit, et rien d'autre.
set STARFALL_FAIL_BEFORE_RUN=1
call :expect 1 "echec avant le lancement, code perime a portee" --screenshot "%SHOTS%" --size 640x360
set STARFALL_FAIL_BEFORE_RUN=

call :expect 0 "capture reussie" --screenshot "%SHOTS%" --size 640x360 --frames 1
call :expect 0 "chemin contenant une espace" --screenshot "%SPACED%" --size 640x360 --frames 1

rem Le cinquieme cas ne se produit pas en usage normal, et c'est justement pourquoi il faut le
rem provoquer : un jeu sorti PROPREMENT pendant qu'une etape ulterieure de la construction echoue.
rem run.bat rendait alors 0 -- un code frais servi par-dessus un echec, symetrique exact du code
rem perime servi apres un echec. Sans cette couture, la regression reviendrait en silence.
set STARFALL_FAIL_AFTER_RUN=1
call :expect 1 "echec apres un jeu sorti en 0" --screenshot "%SHOTS%" --size 640x360 --frames 1
set STARFALL_FAIL_AFTER_RUN=

echo.
if %FAILURES%==0 (
  echo [Starfall] lanceur conforme : les six chemins rendent le code attendu.
) else (
  echo [Starfall] lanceur NON conforme : %FAILURES% chemin^(s^) sur 6.
)
endlocal & exit /b %FAILURES%

:expect
setlocal enabledelayedexpansion
set EXPECTED=%1
set "LABEL=%~2"
shift
shift
rem Les arguments restants sont reinjectes tels quels, guillemets compris.
call "%~dp0run.bat" %1 %2 %3 %4 %5 %6 %7 %8 >nul 2>&1
set ACTUAL=!ERRORLEVEL!
if "!ACTUAL!"=="%EXPECTED%" (
  echo   OK   %LABEL% : code !ACTUAL!
  endlocal & exit /b 0
)
echo   ECHEC %LABEL% : code !ACTUAL! au lieu de %EXPECTED%
endlocal & set /a FAILURES+=1
exit /b 1
