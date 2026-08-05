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

rem Le dossier du script, capture UNE FOIS. Il ne peut pas etre relu par %~dp0 dans le sous-programme
rem :expect, parce que « shift » decale aussi %0 : apres deux shift, %~dp0 vaut le repertoire courant
rem et non celui du script. Mesure : SANS-SHIFT donne le dossier du script, APRES-2-SHIFT donne
rem C:\Windows\. Ca marchait par accident, grace au « cd /d » ci-dessus -- l'ecriture disait une
rem chose et faisait l'autre.
set "HERE=%~dp0"

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

rem La vitrine n'existe qu'en mode capture : son scenario n'est rejoue que la, et sans
rem --screenshot elle ouvrirait une partie ordinaire en pretendant montrer autre chose. Le refus
rem etait verifie A LA MAIN, ce que le preambule de ce fichier qualifie lui-meme de "verification
rem qu'on finit par ne plus faire".
rem
rem LE CAS DE LA VITRINE A ETE RETIRE D'ICI, et c'est un aveu utile.
rem
rem Il ne pouvait echouer que de deux facons, toutes deux mauvaises. En dynamique : si le refus
rem disparait, run.bat lance une vraie partie, ouvre une fenetre et attend -- mesure a 75 secondes
rem sans retour, processus tues a la main. Dans une boucle automatisee, un garde-fou qui SUSPEND est
rem pire qu'un garde-fou absent.
rem
rem J'ai donc ajoute un controle statique qui epinglait d'abord le MESSAGE de l'exception, puis --
rem apres une review -- la ligne de garde elle-meme. Une seconde review a montre que la deuxieme
rem version rougit A TORT : couper la condition en deux lignes, renommer une variable ou inverser
rem deux operandes purs suffit. Et la structure « si le statique echoue, on saute le dynamique »
rem faisait qu'un faux rouge supprimait la seule preuve que la garde marche encore.
rem
rem Or ShowcaseScriptTest couvre deja la disparition reelle du refus, en JUnit, sans ecran et sans
rem risque de suspension -- verifie par mutation. Un controle fragile qui double un controle solide
rem n'apporte qu'un risque net. Retire.

rem Ce cas-ci ne se produit pas en usage normal, et c'est justement pourquoi il faut le
rem provoquer : un jeu sorti PROPREMENT pendant qu'une etape ulterieure de la construction echoue.
rem run.bat rendait alors 0 -- un code frais servi par-dessus un echec, symetrique exact du code
rem perime servi apres un echec. Sans cette couture, la regression reviendrait en silence.
set STARFALL_FAIL_AFTER_RUN=1
call :expect 1 "echec apres un jeu sorti en 0" --screenshot "%SHOTS%" --size 640x360 --frames 1
set STARFALL_FAIL_AFTER_RUN=

rem Le septieme cas est celui du DOUBLE-CLIC : run.bat sans aucun argument. Les six autres en
rem passent tous, si bien que la branche « if defined GAMEARGS goto :with_arguments » n'etait jamais
rem evaluee a faux -- le mode d'emploi principal du fichier, annonce des sa premiere ligne, n'etait
rem garde par rien. Mesure : en remplacant :lwjgl3:run par une tache inexistante dans cette branche,
rem les six cas passaient encore.
rem
rem On le provoque par un echec avant lancement, sinon le jeu ouvrirait une fenetre et attendrait.
rem CE QUE CE CAS COUVRE, ET CE QU'IL NE COUVRE PAS. Il eprouve la branche sans argument jusqu'au
rem code de sortie : la propriete -P, l'effacement du fichier, la lecture du code. Il ne peut PAS
rem verifier qu'elle lance la bonne tache, parce qu'une tache inexistante et un echec force rendent
rem tous deux 1 -- mesure faite, la mutation passait. Jouer cette branche jusqu'au bout demanderait
rem que le jeu se termine seul, or sans argument il ouvre une fenetre et attend. Le controle
rem statique ci-dessous prend le relais sur ce point precis.
set STARFALL_FAIL_BEFORE_RUN=1
call :expect 1 "double-clic, sans aucun argument"
set STARFALL_FAIL_BEFORE_RUN=

rem Les deux branches doivent appeler LA MEME tache. C'est ce que le cas dynamique ne peut pas voir.
set BRANCHES=0
for /f %%A in ('findstr /C:":lwjgl3:run" "%HERE%run.bat" ^| find /c /v ""') do set BRANCHES=%%A
if not "!BRANCHES!"=="2" (
  echo   ECHEC run.bat n'appelle plus :lwjgl3:run sur ses deux branches ^(!BRANCHES! trouvee^(s^)^)
  set /a FAILURES+=1
) else (
  echo   OK   les deux branches appellent :lwjgl3:run ^(controle statique^)
)

rem Et un controle STATIQUE, faute de mieux, sur la garde qui protege le double-clic du blocage.
rem Le « pause » de fin ne doit s'executer QUE sans argument : lance par un script, il suspendait
rem indefiniment celui a qui on venait de rendre le code lisible. Ce harnais ne peut pas l'eprouver
rem en marchant, parce qu'il pose lui-meme STARFALL_NO_PAUSE pour ne pas se suspendre -- la soupape
rem masque la garde. Et un blocage ne se chronometre pas en batch. On verifie donc que la ligne
rem existe telle quelle, ce qui est faible mais honnete, et bien mieux que rien.
findstr /C:"if \"%%~1\"==\"\" if not defined STARFALL_NO_PAUSE pause" "%HERE%run.bat" >nul
if errorlevel 1 (
  echo   ECHEC la garde du « pause » a disparu de run.bat : un script pourrait s'y suspendre
  set /a FAILURES+=1
) else (
  echo   OK   la garde du « pause » est en place ^(controle statique^)
)

echo.
if %FAILURES%==0 (
  echo [Starfall] lanceur conforme : sept chemins et deux controles statiques sont conformes.
) else (
  echo [Starfall] lanceur NON conforme : %FAILURES% controle^(s^) en echec sur 9.
)
endlocal & exit /b %FAILURES%

:expect
setlocal enabledelayedexpansion
set EXPECTED=%1
set "LABEL=%~2"
shift
shift
rem Annonce AVANT de lancer. Sans cette ligne, ce harnais est muet pendant les minutes que
rem prend chaque chemin, et une suspension ne dit pas ou elle a lieu : mesure, vingt minutes
rem a chercher sur quel controle il etait bloque, pour un blocage qui s'est avere venir d'une
rem contention de demon Gradle et non de run.bat. Le preambule de ce fichier dit qu'un
rem garde-fou qui suspend est pire qu'un garde-fou absent ; un garde-fou qui suspend SANS DIRE
rem OU est pire encore.
echo   ...  %LABEL%
rem Les arguments restants sont reinjectes tels quels, guillemets compris. HERE vient du niveau
rem au-dessus : apres deux shift, %~dp0 ne designe plus le dossier de ce script.
call "%HERE%run.bat" %1 %2 %3 %4 %5 %6 %7 %8 >nul 2>&1
set ACTUAL=!ERRORLEVEL!
if "!ACTUAL!"=="%EXPECTED%" (
  echo   OK   %LABEL% : code !ACTUAL!
  endlocal & exit /b 0
)
echo   ECHEC %LABEL% : code !ACTUAL! au lieu de %EXPECTED%
endlocal & set /a FAILURES+=1
exit /b 1
