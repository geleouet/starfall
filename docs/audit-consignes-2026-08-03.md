# Audit des consignes — Starfall (3 août 2026)

**Objet.** Revue indépendante des fichiers de consignes (`projet-jeu-brief.md`,
`STORY.md`, `STYLE.md`, `MEASUREMENT.md`, `combat-design.md`, `shogun-details.md`,
`inspiration.md`, et les `docs/*.md` de dette/review) au regard de quatre objectifs
explicitement demandés : **(1)** un jeu visuellement très beau et poétique,
**(2)** une interface et des actions lisibles, **(3)** carte blanche sur le jeu, le
rendu, la jouabilité et la faisabilité.

**Cadrage.** Un audit exhaustif existe déjà (`docs/proposals-2026-08-03.md`, même
date), excellent sur le rendu et l'épistémologie de la mesure. Le présent rapport ne
le duplique pas : il se concentre sur ce que celui-ci couvre peu ou pas — la poésie
*vécue* comme gameplay, le design du jeu complet, la faisabilité mobile, et la
*santé documentaire* (les consignes qui se contredisent ou ont vieilli). Les items
qui recoupent l'audit existant sont cités par référence (B1, B2, P1…).

**Aucun fichier existant n'a été modifié pour cet audit.**

---

## 0. Verdict en une phrase

Les consignes sont d'une qualité rare pour le *rendu* et la *rigueur de revue*, mais
elles décrivent un **moteur de combat poétique sans encore de jeu** : la poésie y est
presque entièrement une affaire de shader et de squelette, très peu une affaire de
*systèmes* ; et le brief fondateur est **stale**, décrivant un autre jeu
(shogounien, choix moteur ouvert) que celui qui est en cours de construction.

---

## 1. Forces — ce que les consignes font exceptionnellement bien

À créditer avant toute critique, parce que ce sont des acquis rares :

- **Un ruban visuel cohérent et falsifiable** (`STYLE.md` + `MEASUREMENT.md`). La
  bible visuelle est ancrée sur 8 images de référence avec une division du travail
  claire (famille A/B/C), une palette chiffrée, et une doctrine de mesure qui force
  chaque assertion à être reproductible. C'est l'instrument le plus solide du projet.
- **Une décision de design courageuse et tenue** : pas de chrome, UI = marques d'encre
  sur papier (`STYLE.md §8`, `combat-design.md §3`). Le cold-read a confirmé que
  l'interface sans texte survit — résultat non trivial.
- **Une mémoire de conception vivante** : chaque dettes `systemN-debt.md`, chaque
  review, trace ce qui a échoué et pourquoi. La règle « un guard doit avoir été vu
  rouge » (`MEASUREMENT.md §11.2b(f)`) est une discipline de test rare.
- **Un moteur de règles déjà éprouvé** (`combat-design.md §3d`) : 96 tests, invariant
  du télégraphe prouvé, la phase-lock comprise et corrigée. Le socle tactical est sain.
- **Un pitch narratif fort** (`STORY.md`) : *L'Atlas des Songes Éteints*, le Pèlerin
  de la Nuit, la file d'actions comme « strophe d'encre ». Le cadre est évocateur et
  distinctif.

---

## 2. Le problème de structure — la poésie est peinte, pas jouée

C'est ma conclusion centrale et elle conditionne tout le reste.

**La poésie du projet est aujourd'hui presque entièrement portée par le rendu** :
edge dissolve, Verlet de cheveux, bloom d'encre à l'impact, lame comme seul trait
dur. Le *gameplay* lui-même (`combat-design.md`) est un retro-spec fidèle de
*Shogun Showdown* — file LIFO, cooldowns 0–8, charges, traits — avec des termes
renommés (Seeping, Stillness, Marked). Or `combat-design.md §0` énonce lui-même le
filtre qui devrait tout décider :

> *A mechanic earns its place if it produces a beat of choreography.*

…et liste explicitement comme « ne gagne pas sa place » : *flat damage modifiers,
pure cooldown arithmetic, numeric aura buffs, off-screen effects*. Or le système de
**cooldowns 0–8 qui se rechargent de 1 par tour** (`§1.2`) est exactement de
l'arithmétique de cooldown pure. C'est une tension interne non résolue : le document
de design dit « le jeu est la chorégraphie du contact » puis hérite d'un système dont
l'économie centrale est une horloge abstraite.

`STORY.md §1.2` fait un travail magnifique pour *nommer* poétiquement la file
(L'Ancrage, la Rature, la Strophe) — mais ce sont des **habits** sur une mécanique de
deckbuilder. La poésie est lexicale, pas systémique. Un joueur qui échange « Seeping »
contre « Poison » ressent la même chose.

**Conséquence :** le projet risque de produire « un très beau moteur de rendu d'un
combat tactique correct » plutôt que « un jeu poétique ». La beauté est une couche ;
elle n'est pas le jeu.

---

## 3. Axe BEAUTÉ & POÉSIE — propositions

### BP1. Faire du « rêve qui s'éteint » une mécanique, pas un lore
`STORY.md` pose une catastrophe cosmique (le Rêveur s'éveille, les étoiles tombent,
le monde se dissout) qui n'existe **nulle part** dans les systèmes. Proposition :
qu'une ressource de **Lumière / Clarté** décroisse au fil du combat ou de la run —
chaque action éteint un peu le monde — et que cela ait des conséquences visuelles
*et* mécaniques (l'encre devient plus sombre, les Ombres plus nombreuses ou plus
cassées, les tuiles se rechargent plus lentement). C'est la version jouable du
*Starfall*. Le projet a déjà toute la palette pour le rendre (`#EAF2F8` la lame
d'étoile comme dernière étincelle). Coût : structurel, mais c'est *le jeu*.

### BP2. Adopter les propositions B1/B5/B6/B7 de l'audit existant
Je confirme de façon indépendante, après lecture des captures et des références :
- **B1 (part count comme acceptance, planche de fittings)** est l'item de beauté au
  meilleur coût — les mains, prise, garde, pieds, plis manquent depuis 7 systèmes et
  n'ont jamais été briefés. C'est le trou visuel n°1.
- **B5 (ratio d'encre-plancher dérivé du sol)** : le sol de 12.2 mesuré sur la
  famille B contre un floor absolu qui imprime à 27.9 est le plus gros gain de
  contraste disponible pour une demi-journée.
- **B6 (fumées d'encre qui bouclent)** et **B7 (disque solaire chaud derrière
  l'action)** : les deux ajouts qui transformeraient le décor de « dégradé correct »
  à « tableau composé ».

### BP3. Le son est entièrement absent des consignes — gap majeur
Aucun fichier ne mentionne l'audio. Pour un jeu qui se définit comme *onirique* et
*poétique*, c'est la moitié manquante. Proposition d'ajouter une section audio à
`STYLE.md` :
- **Esthétique** : pas de musique mélodique par défaut ; un *drone* de parchemin,
  souffle de papier, gratte de pinceau ; un thème minimal en pentatonique (référence
  implicite aux peintures, sans être japonais — `STORY.md` a pris soin de décoller
  l'iconographie).
- **Tous les SFX dérivent du geste d'encre** : contact lame = tint sec + sourd
  étouffé (pas de bruit métallique hollywoodien) ; impact = le *woosh* d'un pinceau
  sur papier mouillé ; parry = deux traits qui se croisent, frottement sec.
- **L'audio confirme la lecture** : un cooldown prêt fait un petit *tic* d'encre qui
  sèche ; une Rature (télégraphe ennemi) a un grondement grave. Le joueur *entend*
  la stanche se construire.
- **Pas de hitstop visuel** (`STYLE.md §7.1`) mais un *hold* audio subliminal
  (~80 ms de silence scoop) qui fait le travail du freeze sans le dessiner.

Coût : moyen, mais le retour émotionnel par heure est imbattable. Et c'est
intrinsèquement lié à la lisibilité (§4).

### BP4. Mort et résolution comme moments poétiques
Aucune consigne ne décrit ce qui se passe quand une Ombre meurt ou quand le Pèlerin
tombe. Pour un jeu onirique, c'est l'instant le plus chargé. Proposition :
- **Mort d'Ombre** : pas de flash. Le corps se résorbe en une mare d'encre qui
  s'étale lentement (la même matière que les stains `§3b.0`), puis se fige en une
  rature permanente sur la page — le terrain *garde la mémoire* des combats. C'est
  free visuellement (réutilise le système de stains) et chargé de sens.
- **Mort du Pèlerin** : l'encre de la lame d'étoile se disperse, l'écran se vide en
  papier nu, et le rebond est doux — un repos, pas un game over punitif. Le ton
  onirique l'autorise et le différencie.

### BP5. Le frame « map » est une erreur — appuyer B2 plus fort
L'owner a explicitement tranché que le frame de planification est une carte, pas un
tableau (`STYLE.md §9`), et que « la moitié du temps de jeu ne cherche pas à être
belle ». Je trouve ce verdict coûteux pour le produit, pour trois raisons qui ne sont
pas dans l'audit existant :
1. **C'est l'écran le plus regardé.** Accepter qu'il soit « sobre par décision »
   condamne 50%+ du ressenti joueur à la médiocrité scénique.
2. **Le corpus (famille C) ne montre pas cela.** Les images 6/7/8 ne sont pas vides —
   elles sont *denses à bas contraste* : bancs de brume colorés (rouille, corail,
   violet), herbes, motes, figure de premier plan à 60% de la hauteur. La « carte »
   peut être une carte *peinte*, pas un schéma.
3. **La lisibilité et la beauté ne s'opposent pas ici, elles s'alignent.** Plus le
   frame est vide, plus les quelques marques (strophes, télégraphes) flottent sans
   ancrage et deviennent illisibles (cf. cold-read : strophes illisibles à 1×).

**Ma recommandation** (plus tranchée que B2) : revenir sur le verdict « map ». Tenir
le héros à ≥0.45 de hauteur de frame même en planification, énoncer hors-champ par
des marques (bord de frame, ratures), denses brumes colorées. Si l'owner maintient la
décision de carte, alors **au moins** exiger une carte *peinte* (famille C dense),
pas un schéma sur un dégradé.

### BP6. Variance esthétique par rencontre
`shogun-details.md` décrit 13 régions au décor très varié (Bamboo Grove, Whispering
Caves, Hot Springs, Theatre of Illusions…). `combat-design.md §4` les met
explicitement hors-scope pour la V1, à juste titre. Mais **le STYLE actuel ne
prévoit qu'un seul décor** (dusk duel). Quand le jeu grandira, chaque rencontre
devrait avoir sa propre couleur-script : Bamboo = famille A (papier chaud, encre
explosée) ; Caves = palette froide, lavis ruisselants ; Hot Springs = brume et eau
miroir. Cela multiplie la valeur du ruban existant sans le réinventer.

---

## 4. Axe LISIBILITÉ — interface & actions possibles

### L1. Confirmer et compléter l'alphabet (cold read comme acceptance)
L'audit existant (L1–L6) est juste : codifier le cold read en protocole, réauthorer
`BACK_STEP`/`THRUST`/`FEINT` (déjà fait en partie dans `system5-debt §4`), relever le
télégraphe. J'ajoute trois items indépendants.

### L2. Hiérarchie d'information inexistante
`STYLE.md §8` énumère les éléments d'UI (queue, santé, télégraphe, lane) mais **sans
hiérarchie de priorité**. Or un joueur ne peut pas tout lire d'un coup. Proposition
d'ajouter à `§8` une doctrine de hiérarchie par moment :

| Moment | Lecture dominante | Tout le reste s'efface |
|---|---|---|
| Planification (file vide) | **télégraphe ennemi** (où frappe-t-on ?) + tuiles disponibles | Santé en arrière-plan |
| Banking (file en construction) | **la strophe** en cours + prochain slot | Télégraphes se maintiennent |
| Exécution | **l'échange** (corps + lames) | Toute l'UI s'éteint (déjà partiellement fait) |
| Mort / résolution | **le geste** | Silence UI complet |

Cela donne une licence pour *réduire* l'UI à certains moments — sans relitiger `§8`.

### L3. PV ennemis non dessinés — le plus gros trou de lisibilité
`system5-debt §8` le nomme : *« Enemy hit points are still not drawn at all. The
largest omission. »* Sur un jeu tactique tour-par-tour, ne pas voir la santé ennemie
empêche toute planification. C'est un bloqueur de jouabilité, pas un détail esthétique.
Proposition : un nombre ou une jauge de marks d'encre au-dessus de chaque Ombre —
*aligné avec le style* (rangée de petits traits d'encre qui sèchent, comme la santé
héros). À traiter comme priorité 1 de lisibilité.

### L4. Le vermillon sur-utilisé devient du bruit
`system5-debt §7` mesure que le budget vermillon est passé à **0.504%** du frame (le
double du pass-2), uniquement à cause du framing resserré. `STYLE.md §2.2` dit que le
vermillon est « un budget » et doit toujours *signifier* quelque chose (danger,
intention, sang, sceau). Si tout est vermillon (télégraphes + sceaux + santé qui
baisse), le signal se dilue. Proposition : **différencier par valeur/saturation, pas
seulement par forme** — télégraphe = vermillon lavé et mouillé ; sceau/santé basse =
vermillon sec plus profond. Garder le vermillon pur pour l'unique urgence (PV
critique, attaque imminente ce tour).

### L5. Lisibilité du LIFO sur 5 slots
`combat-design.md §3` choisit une colonne verticale base-anchored, newest-en-haut.
C'est juste. Mais avec **5 slots** (`§1.1a`), la lecture « le haut résout d'abord »
doit être *immédiate*. Actuellement la colonne « sèche vers le bas » (§H/G guards),
ce qui est élégant mais peut obscurcir l'ordre. Proposition : un fin gradient de
*chaleur* (le slot top = le plus frais / humide, le slot base = le plus sec), et un
micro-mouvement de « prêt à résoudre » sur le top slot uniquement. Le joueur lit
l'ordre par l'humidité, sans texte.

### L6. Affordance du « free-play » et du coût d'un tour
`combat-design.md §1.3` : ajouter une tuile coûte un tour, sauf *free-play*. C'est
central au jeu. Mais visuellement, comment le joueur sait-il qu'une tuile donnée est
free-play avant de la poser ? Et comment sait-il combien de tours il vient de
dépenser ? Proposition : chaque tuile porte une petite marque marginale (un point pour
free-play, rien sinon), et le nombre de « tours passés exposé » pourrait être
représenté par l'intensité grandissante des ratures ennemies — l'information est déjà
là, il faut la *rendre saliente*.

---

## 5. Axe JOUABILITÉ & DESIGN DE JEU

### J1. Le scope « un seul combat excellent » est juste — mais le design du jeu complet est vide
`combat-design.md` reporte à plus tard régions, boutiques, skills, jours, NG+ (§4).
C'est la bonne discipline pour la V1. **Mais aucune consigne ne décrit la boucle de
jeu au-dessus du combat**, alors que c'est elle qui donne son sens au combat. Sans
métajeu, le projet reste une démo technique. Proposition d'ajouter un document
`PROGRESSION.md` (léger, V1-light) qui réponde à :
- Qu'est-ce qu'une *run* ? Combien de combats ? Quel fil ?
- Que gagne-t-on entre deux combats ? (une tuile ? un enchantement ? un PV ?)
- Qu'est-ce qui fait *rejouer* ? (la question rogue-like centrale)
- Quelle est la courbe de difficulté et l'arc émotionnel visé ?

Même une version « 1 page » change la donne pour orienter les décisions de combat.

### J2. Trois questions de design ouvertes bloquent — et la réponse est « joue »
`combat-design.md §3d.3` liste deux items (cooldowns 0–8 quasi-inertes à 5 slots ;
facing gratuit pour les ennemis) et dit pour chacun *« needs a playable fight to
settle »*. À cela s'ajoutent la paire lane/queue (`§1.1a`) et l'économie de combo.
**Six systèmes de rendu ont été bâtis sans qu'aucun humain n'ait jamais joué le
jeu.** Proposition P1 de l'audit existant : construire la boucle d'input **avant**
tout nouveau pass de rendu. Je souscris totalement et ajoute : un prototype jouable
répondra aussi à des questions que personne n'a encore posées (la pause échappe-t-elle
au joueur ? la lecture LIFO résiste-t-elle à la pression ? le tempo est-il supportable ?).

### J3. Le tempo interactif — seam non dessiné
L'audit existant P2 le souligne : un stanza de 5 tuiles contre 3 ennemis à
`STYLE.md §7.1` (40% wind-up / 15% contact / 45% recovery, settles 0.3–0.6 s, camera
floor 0.25 s, pas de hitstop) peut produire une séquence **cinématique
ininterrompable de plusieurs secondes à chaque tour**. Pour un poème, peut-être
souhaitable. Pour un jeu qu'on rejoue 50 fois, probablement pas. Proposition : une
section `combat-design.md §5` qui fixe (a) planning non chronométré, (b) exécution ≤ N
secondes wall-clock (N choisi au proto), (c) un fast-forward dès le jour 1, (d) le
joueur peut toujours interrompre la *caméra*, jamais la *résolution*. À décider avant
le proto, pas découvert au premier playtest.

### J4. Le système hérité de Shogun ne sert pas toujours le filtre
`combat-design.md §0` dit qu'une mécanique gagne sa place si elle produit un beat de
chorégraphie. Mettons ce filtre sur les systèmes hérités :
- **Cooldowns 0–8** : arithmétique pure. *Ne gagne pas sa place tel quel.*
  Alternative poétique : une tuile se « recharge » quand le joueur accomplit un geste
  en résonance (parer avec une autre lame, traverser une ombre…). Ou : simplifier à
  « utilisable / épuisée » (binaire), le rechargement étant visuel (l'encre revient).
- **Charges + upgrades** : bruit numérique. À simplifier radicalement en V1.
- **Enchantements** (7 types) : probablement trop pour la V1 ; un seul (« parfait » =
  recharge si kill exact) est déjà un bon système.
- **File LIFO + retrait gratuit** : *gagne sa place* — c'est le cœur chorégraphique.
- **Facing comme ressource** : *gagne sa place* — verbe corporel, lit spatial.

Proposition : un audit par le filtre de chaque système hérité, et élaguer ce qui
n'est que de l'arithmétique. Le combat n'en sera que plus lisible et plus poétique.

### J5. Deux héros → un seul pour le premier jalon
Concorde avec P3 : la paire Warden/Pilgrim existe pour prouver la généricité de la
couche d'interaction. Mais le *premier* combattant n'a toujours pas de mains (B1).
Garder le **Pilgrim** (swap = le test le plus dur et la signature nommée), parquer le
Warden. C'est un changement de séquencement, pas de design.

### J6. Boss / encounter design : zéro consigne
`shogun-details.md` consacre 200 lignes aux boss de quelqu'un d'autre. `combat-design.md`
n'a aucun guidage sur *nos* encounters. Au minimum pour la V1 élargie : 3–5
archétypes d'Ombres avec une identité chorégraphique claire (pas seulement un trait),
chacun forçant un type de contact différent. Le filtre de `§0` est la bonne
boussole : chaque ennemi devrait *forcer un geste*.

---

## 6. Axe FAISABILITÉ

### F1. Le brief fondateur est stale — problème de santé documentaire
`projet-jeu-brief.md` est incohérent avec l'état réel du projet :
- **Choix moteur « en cours d'arbitrage » libGDX vs Godot** — alors que le code est
  manifestement déjà libGDX/Java (gradle, `gw`, shaders GLSL, `dev.starfall.*`).
  Cette section induit en erreur tout nouvel acteur.
- **Concept « shogun/samouraï »** comme cadre — alors que `STORY.md §2` a
  explicitement retiré ce cadrage et `combat-design.md §3b` l'entérine (« re-skin,
  not a redesign »).
- **Spine/DragonBones écartés, système custom** — partie toujours d'actualité, OK.
- **MCP « à mettre en place »** — alors que `feedback-loop.md` montre qu'il est
  opérationnel (`mcp/starfall-mcp.mjs`).

Proposition : **réécrire `projet-jeu-brief.md`** pour refléter l'état réel
(libGDX choisi, univers Atlas, boucle de feedback en place, V1 scope = un combat).
Le laisser tel quel est un piège à lecture.

### F2. Cible mobile non adressée dans les consignes
`projet-jeu-brief.md` nomme « Mobile (Android/iOS) + Desktop » comme cibles. Mais
**aucune consigne de design ou de style** ne traite les implications mobile :
- **Input tactile** : aucune mention de toucher/algorithme de tap sur la lane,
  drag pour réordonner la strophe, etc. Or l'UI sans chrome *exige* un input soigné.
- **Taille d'écran / portrait vs landscape** : la lane est horizontale (5–15 tuiles) ;
  en portrait mobile, une lane de 15 cases est illisible. Décision à prendre.
- **Performance** : la stack shader (multi-octave noise, Verlet cheveux, cloth sim,
  skinning GPU, plusieurs passes de dissolve) est lourde. **Aucune frame n'a jamais
  été rendue à taux interactif** (audit P4). Sur mobile mid-range, le risque est réel.
- **Batterie / thermals** : un shader dense qui tient 60 fps sur desktop peut
  throttle à 20 fps sur téléphone en 3 minutes.

Proposition : ajouter une section `MOBILE.md` (ou dans `STYLE.md`) qui fixe (a) un
budget de draw calls / temps de frame cible sur un device de référence, (b) la
decision portrait/landscape, (c) le plan d'input tactile, (d) les dégradations
acceptées sur mobile (octaves en moins, cheveux simplifiés, etc.). À faire **avant**
d'investir dans plus de classes de matériau (`§3b`).

### F3. Un gate de performance interactive avant tout nouvel octave shader
Rejoint P4. Avant `3c` (material classes avec per-class octave stacks de `§3b.3`),
lancer le duel scene live à 60 Hz sur desktop et *enregistrer le frame time*. Si ça
rate, c'est un fait que le matériau doit contourner (octaves baked en texture,
fields précomputés). L'apprendre après `3c` est la voie coûteuse.

### F4. Géométrie de scène à figer (P5)
`TILE_WIDTH` vs `FIGURE_HEIGHT` vs `BODY_HALF` (dette 8, nommée par 3 passes)
bloque trois choses à la fois : la séparation de base (1.16–1.29× le corpus), le
framing planification (BP5/B2), et l'arithmétique du push-in. Une décision de
`combat-design.md` (à quelle distance deux combattants sur tuiles adjacentes se
tiennent-ils ?) débloque trois systèmes. Décision petite, effet structurel.

### F5. Coupler la sortie d'input avec un gate d'« jouabilité »
Une fois le proto jouable (J2), ajouter au pipeline de review un *playtest report*
court : un humain a-t-il pu (a) comprendre les règles sans doc, (b) planifier une
strophe de 3+ tuiles, (c) ressentir le moment de contact, (d) vouloir rejouer.
Aujourd'hui la rubrique note des images ; aucune note ne juge le *jeu*. À ajouter à
`MEASUREMENT.md` quand l'input existera.

---

## 7. Tensions et contradictions entre documents

### T1. `projet-jeu-brief.md` vs tout le reste
Le brief décrit un autre jeu (shogounien, moteur ouvert, MCP à venir). À réécrire.
C'est la tension n°1 et la plus simple à corriger.

### T2. `inspiration.md` vs `combat-design.md §0`
`inspiration.md` célèbre l'économie d'action de Shogun comme digne d'étude —
*« builds 0 CD », « builds High Damage », « seuil critique de 5 dégâts »*, etc.
`combat-design.md §0` dit explicitement que ce genre d'arithmétique *ne gagne pas sa
place*. Ces deux documents pointent dans des directions opposées sans le nommer. Si
`inspiration.md` reste, il devrait être reformulé comme « analyse de ce qu'il faut
*comprendre* de Shogun, puis *filtrer* » plutôt que « ce qu'il faut reproduire ».

### T3. `shogun-details.md` — 469 lignes sur le jeu de quelqu'un d'autre
C'est le plus gros fichier de consignes et il décrit intégralement *Shogun Showdown*
(42 tuiles, 14 boss, 13 régions, 36 skills…). Utile comme référence, mais son poids
risque d'ancrer le design au source plutôt qu'au but poétique. Proposition : le
déplacer dans `docs/reference/` et l'en-têter par *« ceci est un retro-spec du jeu
source, à filtrer par `combat-design.md §0`, pas un cahier des charges »*. Pareil pour
`inspiration.md`.

### T4. `STYLE.md §9` (map) vs `§0` (one-sentence test)
`§0` dit : *« cette frame pourrait-elle être extraite d'une des 8 images de
référence ? »* `§9` dit que le frame planification est une *carte* et *« n'essaie pas
d'être belle »*. Ces deux lignes sont en tension directe : le frame le plus regardé
est explicitement exempté du test qui fonde le projet. Soit lever l'exemption (BP5),
soit admettre que `§0` ne s'applique pas partout — mais alors le dire explicitement
et définir *à quoi* on grade le frame planification.

### T5. `STORY.md` en français, tout le reste en anglais
Noté par l'audit existant (R6). Les agents citent les termes à travers les fichiers ;
la table lexique existe déjà. Une jumelle anglaise (ou une table bilingue formelle)
supprimerait la couture à l'interface UI/naming. Faible priorité mais coût faible.

### T6. STYLE.md est devenu un traité de mesure
Concorde avec R1/R2 de l'audit existant : ~40% du fichier est de l'épistémologie de
garde, pas de l'encre. Le split est *déjà parti* (`MEASUREMENT.md` existe) mais
`STYLE.md` charrie encore `§7.1` sur ~100 lignes d'archéologie de statistique cloth,
les amendements narratifs dans `§2.2`/`§8`, etc. Un builder qui ouvre le fichier pour
apprendre ce qu'est un hem doit traverser l'épistémologie des assertions. Poursuivre
le split (history → changelog, current rule only dans le corps) libérerait le
document pour redevenir une bible d'encre.

---

## 8. Recommandations priorisées

Classées par *valeur débloquée / coût*.

| # | Recommandation | Coût | Débloque |
|---|---|---|---|
| **1** | **Réécrire `projet-jeu-brief.md`** pour refléter l'état réel (F1) | *cheap* | Tous les nouveaux lecteurs du projet |
| **2** | **Planche de fittings** (B1) : mains, prise, garde, plis, pieds, 2e lame | *structural* | Le plus grand trou visuel, depuis 7 systèmes |
| **3** | **PV ennemis dessinés** (L3) | *medium* | Jouabilité basique du combat tactique |
| **4** | **Ratio d'encre-plancher dérivé** (B5) | *cheap* | Contraste, le plus gros gain visuel / heure |
| **5** | **Boucle d'input jouable** (J2/P1) + tempo (J3/P2) | *medium* | 3 questions de design bloquées ; tout test de lisibilité sous pression |
| **6** | **Géométrie de scène figée** (F4/P5) | *petit* | Framing, séparation, push-in |
| **7** | **Section audio** (BP3) | *medium* | La moitié manquante de la poésie |
| **8** | **Gate perf interactive** (F3/P4) | *cheap* | Évite un redesign matériel coûteux |
| **9** | **Revoir le verdict « frame map »** (BP5) | *décision* | Le frame le plus regardé |
| **10** | **Audit des systèmes hérités par le filtre** (J4) | *cheap* | Clarté du design, élagage |
| **11** | **`PROGRESSION.md` léger** (J1) | *cheap* | Sens du combat ; cap pour les décisions |
| **12** | **Finir le split STYLE.md** (T6/R1) | *cheap* | Builder lit une bible d'encre |
| **13** | **Fumées d'encre + disque solaire** (B6/B7) | *medium* | Décor → tableau composé |
| **14** | **Hiérarchie d'info par moment** (L2) | *cheap* | Réduction de clutter UI |
| **15** | **Le rêve qui s'éteint comme mécanique** (BP1) | *structural* | *Le jeu*, pas une démo technique |

---

## 9. Décisions à trancher d'abord

Ces décisions sont peu coûteuses à *prendre* et coûteuses à *laisser ouvertes*.

1. **Frame planification : carte ou tableau ?** (BP5, T4). Tout pend à cette phrase.
   Ma recommandation : tableau dense famille C, tenir le héros ≥0.45 fh.
2. **Le jeu complet : quelle run, quelle progression ?** (J1). Sans réponse, le combat
   est orphelin.
3. **Stack mobile : quel device cible, portrait/landscape, budget frame ?** (F2).
   Avant tout nouvel octave.
4. **Le registre de l'Ombre pâle : corpus-sombre ou wraith canonique ?** (B4 de
   l'audit existant). Littéralement une phrase à écrire.
5. **Systèmes hérités : que garde-t-on de Shogun ?** (J4). Le filtre de `§0` appliqué
   honnêtement élaguerait la moitié du retro-spec.
6. **Le tempo interactif** (J3). Un stanza de 5 doit-il être un cinematic de 5 s ou un
   enchaînement de 2 s avec fast-forward ?

---

## 10. Note de méthode

Cet audit a été produit sans modifier aucun fichier existant, à la lecture croisée de
tous les `.md` de consignes et des `docs/*.md` de dette/review, ainsi que de
l'audit `docs/proposals-2026-08-03.md` dont il est délibérément complémentaire. Les
propositions qui s'y recoupent (B1, B2, B5, B6, B7, P1, P2, P3, P4, P5, R1, R6) sont
citées par référence et confirmées indépendamment ; les items BP/L/J/F/T ci-dessus
sont des ajouts propres, centrés sur la poésie comme gameplay, le design global du
jeu, la faisabilité mobile, et la cohérence des consignes entre elles.
