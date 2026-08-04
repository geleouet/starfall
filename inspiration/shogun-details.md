# Shogun Showdown — Guide complet des mécaniques

## 1. Présentation générale
Shogun Showdown est un jeu de combat au tour par tour avec des éléments rogue-like et de deck-building, développé par Roboatino (édité par Goblinz Publishing / Gamera Games), sorti en septembre 2024. Le but est de traverser une série de régions peuplées de troupes du Shogun, d'améliorer son "deck" de tuiles d'attaque, et d'affronter des boss de plus en plus puissants jusqu'au Shogun lui-même, puis un boss caché final. Il existe aussi une version gratuite, le *Prologue*, qui sert de démo avec ses propres boss.

## 2. Mécaniques fondamentales de combat

- **La grille de combat** : chaque niveau se déroule sur une rangée de cases (5, 7 ou 9 selon le niveau). Le héros et les ennemis occupent ces cases et se déplacent horizontalement.
- **Tour par tour** : le joueur agit, puis tous les ennemis exécutent leurs actions dans l'ordre après lui.
- **File d'attaque (Attack Queue)** : on peut empiler jusqu'à 3 tuiles dans une file avant de déclencher l'enchaînement ; les tuiles s'exécutent alors de la dernière ajoutée à la première.
- **Temps de recharge (Cooldown)** : chaque tuile a un nombre de "charges" à recharger (0 à 8) avant de pouvoir être rejouée ; la recharge avance d'un cran par tour, et retombe à zéro dès que la tuile est utilisée (qu'elle touche ou non).
- **Emplacements d'amélioration (Upgrade Slots)** : chaque tuile possède un nombre maximal d'améliorations qu'elle peut recevoir (jusqu'à 8).
- **Enchantements** : une tuile de héros ne peut recevoir qu'un seul enchantement parmi : onde de choc (dégâts aux ennemis adjacents), coup parfait (recharge si le coup achève exactement les PV restants), poison, gel, double frappe, malédiction, ou "Free-Play" (l'ajout de la tuile ne consomme pas de tour).
- **Statuts** : Poison (1 dégât par tour pendant 3 tours), Gel (immobilise 3 tours, sauf ennemis "Insensibles au gel"), Malédiction (double dégâts au prochain coup subi), Bouclier (annule la prochaine attaque reçue).
- **Combos** : tuer plusieurs ennemis dans le même tour déclenche un "combo kill", qui active certaines compétences spéciales (pièces bonus, soin, recharge, etc.).
- **Traits d'ennemis de base** : Agressif (se rapproche après avoir attaqué), Rapide (peut attaquer dès qu'une tuile est ajoutée à sa file), Explosif (explose à sa mort, 2 dégâts aux cases adjacentes), Fonceur/Dash (se déplace le plus loin possible), Insensible au gel (boss uniquement).
- **Élites** : à partir du Jour 2, des variantes élites des ennemis apparaissent avec un trait supplémentaire et plus de PV : Rapide, Lourd (ne peut être ni déplacé ni retourné), Frappe Double, Bouclier Réactif (gagne un bouclier après avoir été touché), et Corrompu (à partir du Jour 7, fait apparaître une Progéniture Corrompue à sa mort).
- **Jours (progression en boucle/NG+)** : chaque victoire complète contre le Shogun fait progresser un compteur de "Jour". Plus le Jour avance, plus les ennemis élites et les variantes "Corrompues" des boss apparaissent tôt et plus les PV des ennemis/boss augmentent (paliers notables : élites dès le Jour 2, variantes Corrompues des boss dès le Jour 4, PV augmentés dès le Jour 5, apparition du boss cachÉ final possible au Jour 7).

## 3. Personnages jouables

| Héros | Déblocage | Capacité spéciale |
|---|---|---|
| La Vagabonde (Wanderer) | Disponible dès le début | Échange sa place avec un ennemi devant elle en avançant |
| Le Ronin | Battre les Jumeaux (The Twins) | Repousse un ennemi devant lui en avançant |
| L'Ombre (Shadow) | Battre Nobunaga le Cruel | Traverse les ennemis devant elle en avançant ; passif : se déplace toujours le plus loin possible |
| La Jujitsuka | Battre le Shogun au Jour 2 | Projette un ennemi situé derrière elle en reculant |
| Le Maître des Chaînes | Battre le Shogun au Jour 3 | Échange les positions de la case devant et derrière lui |

Chaque héros possède deux jeux de tuiles de départ différents et une phrase de victoire propre lors de la défaite du Shogun au Jour 7.

## 4. Objets — Les Tuiles (armes/actions)

Les tuiles sont les objets centraux du jeu : armes, déplacements et effets spéciaux qui composent le "deck" du joueur. Il en existe 42 obtenables (plus des tuiles non-obtenables réservées aux ennemis/boss et quelques tuiles retirées du jeu).

### Tuiles obtenables (sélection résumée par catégorie)
- **Frappes simples** : Katana, Tetsubo, Sai (double dégâts si l'ennemi est sur le point d'attaquer), Blade of Patience (dégâts croissants tant qu'elle reste en file), Hookblade (avance et refrappe sur un coup fatal).
- **Frappes à effet de position** : Dragon Punch (repousse au maximum + dégâts de collision), Bo (retourne la cible), Back Strike (frappe derrière soi).
- **Frappes à zone/multiples** : Swirl (devant + derrière), Twin Tessen (devant + derrière avec repoussement), Spear (perce 2 cases devant), Nagiboku (perce 2 cases devant/derrière, ne peut pas tuer), Earth Impale (frappe à distance 2 devant et derrière).
- **Frappes à distance** : Shadow Kama, Meteor Hammer (rebondit derrière l'utilisateur), Blazing Suisei (explosion sur coup fatal), Shuriken, Arrow, Kunai (plusieurs projectiles), Crossbow (perce 2 cibles, se recharge en la réutilisant), Lightning (frappe la cible la plus éloignée).
- **Tuiles de déplacement/mobilité** : Charge, Back Charge, Shadow Dash, Back Shadow Dash, Smoke Bomb (échange de position), Back Smoke Bomb, Grappling Hook (tire la cible vers soi), Dash.
- **Tuiles utilitaires/pièges** : Trap (piège au sol), Thorns (barrière d'1 PV), Scar Strike (frappe toutes les cibles non pleines en PV), Chakram (gagne en dégâts à chaque mort d'ennemi), Mon (dépense une pièce pour plus de dégâts), Ki Push, Tanegashima (recul mutuel).
- **Tuiles "Free-Play" spéciales** (n'utilisent pas de tour) : Curse, Sharp Turn, Signature Move (déclenche la capacité spéciale du héros), Swap Toss, Dash, Origin of Symmetry (téléportation au centre), Mirror (se téléporte sur la position de l'attaquant).

### Tuiles non-obtenables (réservées aux ennemis/boss)
Volley, Barricade, Summon (et sa variante Double Strike), Bomb, Shield, Ally Shield, Copycat Mirror (copie une tuile de héros), Maku (change d'acte, utilisé par Sato), Boss Swap, ainsi que les tuiles de corruption : Corrupted Barrage, Corrupted Wave (gauche/droite), Corrupted Explosion, Corrupted Pulse — ces dernières soignent les boss tout en endommageant le joueur.

### Système d'amélioration des tuiles
Après la plupart des niveaux, on choisit une amélioration : +Dégâts, +Dégâts/+Cooldown, -Cooldown, +Emplacement d'amélioration, sacrifice d'une tuile contre de l'or, ou un enchantement aléatoire. Après certains niveaux clés (fin de Bamboo Grove, avant Moonlit Port, etc.), l'amélioration est remplacée par un choix entre deux nouvelles tuiles. Des upgrades de tuiles sont aussi disponibles à prix d'or après chaque région, dans les boutiques.

## 5. Consommables

| Type | Effet |
|---|---|
| Edamame Brew | Restaure jusqu'à 3 PV |
| Kami Brew | Octroie un bouclier |
| Cool Up | Recharge toutes les tuiles et la capacité spéciale |
| Mass Curse | Maudit tous les ennemis présents |
| Mass Poison | Empoisonne tous les ennemis (3 tours) |
| Mass Ice | Gèle tous les ennemis (3 tours) |
| Rain of Mirrors | Retourne tous les ennemis |
| Lucky Die (D6) | Relance l'intention d'un ennemi en combat, ou une récompense/boutique hors combat |
| Pièces / Crânes | Monnaies (les crânes sont une monnaie de méta-progression, obtenue uniquement via les boss) |

Le joueur peut transporter 3 consommables par défaut (extensible à 6 via la compétence *Big Pockets*). Un consommable non ramassé en fin de combat est automatiquement converti en pièce s'il n'y a plus de place.

## 6. Régions et progression du monde

Il y a 13 régions (+ une zone finale cachée). Chaque région comprend plusieurs niveaux à vagues d'ennemis, puis un niveau de boss. Entre les niveaux, on choisit entre amélioration de tuile ou nouvelle tuile ; entre les régions se trouvent des boutiques.

**Ordre général du parcours** : Bamboo Grove → (Whispering Caves ou Hibiku Wastelands) → Moonlit Port → (Spirit Gateway ou Forsaken Grounds) → (Hot Springs ou Theatre of Illusions) → (Hideyoshi's Keep, Nobunaga's Fortress ou Ieyasu's Garden) → Château du Shogun → Sanctuaire Écarlate (boss caché).

**Vagues** : chaque niveau contient un nombre de cases (5, 7 ou 9) déterminant le nombre maximal d'ennemis simultanés. Trois patterns d'apparition existent : *Normal* (nouvelle vague quand les ennemis sont presque tous morts), *Rapide* (apparition toutes les 2 tours), *Mixte* (règles différentes sur tours pairs/impairs).

**Apparition dynamique** : un ennemi n'apparaît dans les régions suivantes que si le joueur l'a déjà rencontré au moins une fois (ex. les Yumi Snipers n'apparaîtront pas plus tard si on a toujours emprunté le chemin des Whispering Caves plutôt que celui des Hibiku Wastelands).

## 7. Ennemis

| Ennemi | Trait | Région d'apparition | Comportement résumé |
|---|---|---|---|
| Ashigaru | — | Bamboo Grove | Épéiste basique |
| Twin Tachi | — | Bamboo Grove | Frappe devant/derrière, même au risque de toucher un allié |
| Spike Charger | — | Bamboo Grove | Charge en ligne droite |
| Yari Master | — | Whispering Caves | Attaque à la lance sur 2 cases |
| Yumi Sniper | — | Hibiku Wastelands | Tire à distance depuis l'arrière |
| Guardian | Agressif | Moonlit Port | Avance sans relâche à l'épée |
| Ashigaru Archer | — | Moonlit Port | Attend en retrait puis tire |
| Grappler | — | Spirit Gateway | Tire les cibles vers lui |
| Shinobi | Rapide | Spirit Gateway | Frappe vite sans prévenir |
| Shielder | — | Hot Springs | Protège les alliés |
| Warden | Explosif | Forsaken Grounds | Explose à sa mort |
| Strider | Fonceur | Forsaken Grounds | Se déplace au maximum en une fois |
| Kabukai | — | Theatre of Illusions | Copie les mouvements du joueur |
| Shadow Charger | Rapide | Hideyoshi's Keep / Nobunaga's Fortress / Ieyasu's Garden | Fonce silencieusement |

**Ennemis non-standards** : Dummy (mannequin d'entraînement du tutoriel), Barricade (obstacle statique), Summoner (statue invoquant des ennemis), Blight Charger (variante rapide et explosive du Strider), Swapper (ennemi à 30 PV lié à Ieyasu), Corrupted Progeny (invoquée par les ennemis "Corrompus" à leur mort, Jour 7), Thorns (barrière avec riposte).

## 8. Boss et leur comportement

Chaque boss dispose d'une variante **Corrompue**, apparaissant à partir du Jour 4, généralement plus rapide (trait Rapide ajouté) et dotée de tuiles supplémentaires plus dangereuses, en particulier des tuiles de corruption qui soignent le boss.

- **Daisuke the Dasher / Rei the Ruthless** (Bamboo Grove) — Ce sont deux boss alternatifs (Rei remplace Daisuke après une première victoire sur le Shogun). Daisuke alterne frappes basiques et invocations de Spike Chargers, avec une attaque de charge dangereuse à esquiver via mobilité. Rei privilégie le Dragon Punch et invoque des Twin Tachi qu'elle peut projeter sur le joueur pour infliger des dégâts de collision. Version corrompue : les deux gagnent le trait Rapide et invoquent/chargent plus souvent.

- **Iwao the Impaler** (Whispering Caves) — Combine des frappes à plusieurs cases (Earth Impale, Spear, Swirl) et invoque surtout des Yari Masters. Corrompu : gagne le trait Rapide et une charge combinée à la lance, rendant l'esquive plus délicate.

- **Baru the Barricader** (Hibiku Wastelands) — Alterne entre invocations, tirs à distance (Volley) et construction de barricades derrière lesquelles il se protège ; il faut détruire rapidement sa barricade pour l'exposer. Corrompu : gagne une lance et une vague de corruption qui le soigne.

- **The Twins** (Moonlit Port) — Deux boss identiques positionnés aux extrémités du terrain ; agressifs, ils encerclent le joueur qui doit rester à un bord du terrain pour n'affronter qu'un seul côté à la fois. Corrompus : gagnent le trait Rapide et un Meteor Hammer à ricochet dangereux.

- **The Statue** (Spirit Gateway) — Ne bouge jamais, empile ses tuiles avant de frapper, accompagné d'un Invocateur (Summoner) qui fait apparaître des ennemis. Corrompu : perd son handicap de vitesse et, sous 25 % de PV, enchaîne une attaque de téléportation quasiment inévitable qu'il faut vaincre rapidement.

- **Kowa the Coward** (Hot Springs) — Fuit constamment le joueur, se protège d'un bouclier puis s'échappe via Smoke Bomb ; le vrai danger vient des ennemis invoqués autour de lui plutôt que de ses propres attaques. Corrompu : son Smoke Bomb frappe deux fois et il peut invoquer des Grapplers.

- **Fumiko the Fallen** (Forsaken Grounds) — Fonce, frappe (lance ou zone), recule, et invoque des Blight Chargers ; peut aussi maudire le joueur. Corrompue : gagne une attaque à distance explosive (Blazing Suisei) et invoque plus d'ennemis.

- **Sato the Stagemaster** (Theatre of Illusions) — Fonctionne par "Actes" : il copie les tuiles du joueur (Copycat Mirror) puis laisse place à des vagues d'ennemis différentes selon l'acte tiré, avant un affrontement final. Corrompu : ajoute un acte supplémentaire et une version à double frappe de sa copie.

- **Hideyoshi the Cunning** (Hideyoshi's Keep) — Rapide et insensible au gel, il enchaîne des combos fixes de 3 tuiles (téléportation, miroir, bombe, etc.) sans jamais répéter le même consécutivement, et invoque des renforts. Corrompu : deux combos supplémentaires plus dangereux (malédiction, marteau-météore).

- **Nobunaga the Wicked** (Nobunaga's Fortress) — Rapide et insensible au gel ; mécanique unique de "spots lumineux" — il n'est vulnérable que lorsqu'il se trouve sur une case éclairée, le nombre de projecteurs diminuant à mesure que ses PV chutent. Corrompu : devient également Agressif, ce qui facilite paradoxalement son attraction vers la lumière.

- **Ieyasu the Patient** (Ieyasu's Garden) — Boss au comportement le plus simple : recharge une attaque à un coup (Blade of Patience) puis fonce dessus dès qu'il a la ligne de vue, sinon invoque des renforts. Corrompu : obtient une tuile perçante lui permettant de frapper à travers ses propres alliés.

- **The Shogun** (Château du Shogun) — Boss final en deux phases. Phase 1 : comportement proche d'un ennemi normal (approche, frappe, recul, charge à la lance occasionnelle) épaulé par des Invocateurs. Phase 2 : ouvre par une attaque de zone en expansion (Corrupted Barrage), puis enchaîne des combos de déplacement/frappe fixes sans répétition consécutive.

- **Corrupted Soul** (boss caché du Sanctuaire Écarlate, uniquement Jour 7) — Le véritable boss final. Se déplace verticalement au-dessus de la grille, invulnérable tant qu'il est "en l'air" ; alterne invocations massives et une attaque de zone (Tainted Triad), puis déclenche une impulsion de corruption à 50 % de PV. C'est le boss avec le plus de PV du jeu après avoir été rendu vulnérable.

*(Le Prologue gratuit possède son propre boss final, Ume the Unrelenting, rapide et agressif, dont la principale menace est un enchaînement de shurikens à esquiver par échange de position.)*

## 9. Boutiques et compétences (Skills)

Après chaque région, le joueur visite une boutique parmi 4 spécialisations (+ une boutique spéciale de l'Astronome) :
- **Guerrier (Warring)** : compétences offensives (bonus de dégâts selon la position, la distance, les tuiles à un seul élément, etc.).
- **Combo** : compétences liées aux combos (pièces bonus, soin, recharge de tuiles, malédiction/poison/gel sur combo).
- **Garde (Guarding)** : compétences défensives (PV bonus, bouclier de départ, dégâts renvoyés, réduction de dégâts).
- **Danseur (Dancer)** : compétences de mobilité (capacité spéciale bidirectionnelle, dégâts sur déplacement, effets de statut via la capacité spéciale).
- **Astronome (Moonlit Port)** : boutique unique regroupant toutes les compétences plus deux compétences exclusives (résurrection, premier reroll gratuit), payables en crânes.

Chaque compétence peut être achetée plusieurs fois pour monter de niveau (jusqu'au niveau 3), augmentant son effet. Les boutiques proposent aussi des "services de sang" (perdre des PV contre de l'or ou un rafraîchissement de la boutique) et la vente de consommables.

## 10. Quêtes et succès

Le jeu propose des quêtes optionnelles, certaines visibles dès le début (ex. vaincre un boss précis, tuer plusieurs ennemis en un seul tour, infliger de gros dégâts en une attaque) et d'autres cachées, révélées progressivement en accomplissant des quêtes précédentes (ex. terminer une run sans consommables, avec un deck limité à certains cooldowns, sans acheter de compétence, etc.), jusqu'à des défis ultimes couvrant tous les héros.



# Shogun Showdown — Détail des Tuiles et des Ennemis

## A. LES TUILES (objets d'attaque)

### Rappel des règles communes
Chaque tuile possède : des **dégâts** (nombre à gauche), un **cooldown** (0 à 8, décompté en "points" qui se rechargent de 1 par tour), un **coût en crânes** pour la débloquer en méta-progression (NA = déblocable autrement, souvent tuile de départ), et parfois un **enchantement possible** (Onde de choc, Coup Parfait, Poison, Gel, Double Frappe, Malédiction, Free-Play). Trois tuiles maximum peuvent être empilées dans la file d'attaque, exécutées de la dernière ajoutée à la première.

### 1. Tuiles obtenables (42 au total)

| Tuile | Dégâts | Cooldown | Coût (crânes) | Effet |
|---|---|---|---|---|
| Katana | 2 | 0 | Tuile de base | Frappe la case devant soi |
| Tetsubo | 4 | 7 | 3 | Frappe la case devant soi (gros dégâts, long cooldown) |
| Sai | 2 | 5 | 15 | Frappe devant ; dégâts doublés si la cible s'apprête à attaquer ce tour |
| Blade of Patience | 0 | 6 | 10 | Frappe devant ; les dégâts augmentent de 1 par tour passé dans la file |
| Hookblade | 2 | 5 | 30 | Frappe devant ; sur coup fatal, avance d'une case et refrappe pour 1 dégât de moins |
| Dragon Punch | 1 | 4 | 1 | Frappe devant et repousse la cible au maximum ; dégâts de collision si un autre ennemi est sur la trajectoire |
| Bo | 1 | 5 | 25 | Frappe devant et retourne la cible |
| Back Strike | 3 | 3 | 15 | Frappe la case derrière soi |
| Swirl | 2 | 3 | Tuile de base | Frappe simultanément devant et derrière |
| Twin Tessen | 1 | 6 | 20 | Frappe devant et derrière en repoussant les cibles au maximum (+ collision) |
| Spear | 2 | 5 | Tuile de base | Perce les 2 cases devant soi |
| Shadow Kama | 3 | 3 | 6 | Frappe la case à distance 2 devant soi (ignore la case intermédiaire) |
| Nagiboku | 2 | 5 | 30 | Perce 2 cases devant et 2 derrière ; ne peut pas tuer (laisse 1 PV minimum) |
| Earth Impale | 2 | 4 | 20 | Frappe les cases à distance 2, devant et derrière |
| Meteor Hammer | 2 | 5 | 25 | Frappe la 1ère cible jusqu'à 3 cases devant ; rebondit ensuite derrière soi |
| Blazing Suisei | 2 | 4 | 30 | Frappe à distance (3 cases) ; sur coup fatal, explosion (2 dégâts aux cases adjacentes) |
| Shuriken | 1 | 3 | Tuile de base | Frappe la 1ère cible devant soi |
| Arrow | 2 | 5 | Tuile de base | Frappe la 1ère cible devant soi |
| Kunai | 2 | 7 | 20 | Lance autant de kunai que la valeur d'attaque, 1 dégât chacun, sur la 1ère cible |
| Mon | 5 | 7 | 20 | Grosse frappe à distance ; dépense une pièce |
| Crossbow | 3 | 5 | 15 | Tire un carreau perçant sur les 2 premières cibles ; retombe à 0 après usage, se recharge en la réutilisant |
| Grappling Hook | 1 | 4 | 3 | Tire la 1ère cible jusqu'à la case juste devant soi |
| Ki Push | 0 | 6 | 25 | Frappe et repousse la 1ère cible au maximum (+ collision) |
| Tanegashima | 4 | 7 | 25 | Frappe la 1ère cible ; attaquant et cible reculent d'1 case chacun (collision possible) |
| Chakram | 0 | 7 | 30 | Frappe devant et derrière ; gagne +1 dégât à chaque mort d'ennemi, remis à 0 après usage |
| Lightning | 2 | 5 | 3 | Frappe la cible la plus éloignée devant soi |
| Scar Strike | 1 | 5 | 10 | Frappe toutes les cibles qui ne sont pas à PV pleins |
| Trap | 3 | 4 | 6 | Pose un piège devant soi, se déclenche quand une cible marche dessus |
| Thorns | 1 | 4 | 30 | Crée une barrière d'1 PV devant soi |
| Charge | 1 | 4 | Tuile de base | Fonce en avant et frappe la 1ère cible |
| Shadow Dash | 1 | 5 | Tuile de base | Traverse les cibles devant soi, s'arrête à la 1ère case vide derrière elles |
| Smoke Bomb | 1 | 5 | 1 | Échange sa position avec la 1ère cible devant soi |
| Back Charge | 1 | 3 | Tuile de base | Fonce en arrière et frappe la 1ère cible derrière soi |
| Back Shadow Dash | 1 | 5 | 20 | Version arrière du Shadow Dash |
| Back Smoke Bomb | 1 | 5 | 25 | Échange sa position avec la 1ère cible derrière soi |
| Curse (Free-Play) | — | 7 | 1 | Maudit la 1ère cible devant soi (double dégâts au prochain coup) |
| Sharp Turn (Free-Play) | 1 | 7 | 3 | Se retourne et frappe les cases autour de soi |
| Signature Move (Free-Play) | — | 6 | 25 | Exécute la capacité spéciale du héros |
| Swap Toss (Free-Play) | — | 7 | 25 | Échange le contenu des cases devant et derrière soi |
| Dash (Free-Play) | — | 6 | 10 | Fonce en avant le plus loin possible |
| Origin of Symmetry (Free-Play) | — | 6 | 30 | Téléporte au centre de la grille, échange avec l'occupant si présent |
| Mirror (Free-Play) | — | 6 | 20 | Se téléporte sur la position de l'attaquant |

> Les tuiles marquées **Free-Play** ne consomment pas de tour lorsqu'on les ajoute à la file (sauf mention contraire, propriété normalement réservée à l'enchantement du même nom).

### 2. Tuiles non-obtenables (réservées aux ennemis/boss)

| Tuile | Dégâts | Effet |
|---|---|---|
| Volley | 2 | Vise la case actuelle du héros au moment de la déclaration, puis frappe cette case |
| Barricade | — | Construit une barricade devant soi |
| Summon | — | Invoque un ennemi sur une case aléatoire |
| Summon (Double Strike) | — | Invoque deux fois de suite |
| Bomb | 3 | Pose une bombe devant soi, explose après 2 tours |
| Shield | — | Gagne un bouclier annulant la prochaine attaque |
| Ally Shield | — | Donne un bouclier au premier allié sans bouclier devant soi |
| Copycat Mirror | — | Se transforme en version basique d'une tuile aléatoire du héros (hors tuiles déjà en file) |
| Copycat Mirror (Double Strike) | — | Idem, en frappant deux fois |
| Maku | — | Baisse le rideau, passe à l'acte suivant (utilisé par Sato) |
| Boss Swap | — | Échange sa position avec le boss |
| Corrupted Barrage | 3 | Anneau de corruption qui s'étend d'une case chaque tour ; blesse les unités, soigne les boss |
| Corrupted Wave (gauche/droite) | 1 | Vague de corruption traversant le terrain d'un bord à l'autre ; blesse, soigne les boss |
| Corrupted Explosion | 1 | Explosion corrompue touchant toutes les cases |
| Corrupted Pulse | 3 | Rayons de corruption alternant cases impaires/paires à chaque tour |

### 3. Tuiles retirées du jeu

| Tuile | Info |
|---|---|
| Turn Around | Fusionnée avec Kaitenryuken pour former Sharp Turn |
| Kaitenryuken | Fusionnée avec Turn Around pour former Sharp Turn |
| Shurikens (devant/derrière) | Retravaillée pour devenir Chakram |
| War Fan | Certaines propriétés reprises dans Tanegashima |

### 4. Système d'amélioration
- **Entre les niveaux** : +1 Dégât, +1 Dégât/+1 Cooldown, +2 Dégâts/+3 Cooldown, -1/-2/-4 Cooldown (avec parfois -1 Dégât), +1 Emplacement d'amélioration, sacrifice d'une tuile contre 40 pièces, ou un enchantement aléatoire (avec un coût en cooldown additionnel selon l'enchantement).
- **Après une région, en boutique** : Amélioration de Dégâts (+1, 20 pièces), Amélioration de Cooldown (-2, 20 pièces), Amélioration de Niveau Max (+1, 10 pièces), Amélioration d'Enchantement (aléatoire + cooldown, 20 pièces), Sacrifice (gratuit, 40 pièces gagnées).

---

## B. LES ENNEMIS

### Traits de base
- **Agressif** : se rapproche du joueur après avoir attaqué (les ennemis sans ce trait reculent généralement).
- **Rapide** : peut déclarer son attaque dès qu'une tuile est ajoutée à sa file (pas besoin d'attendre un tour).
- **Explosif** : explose à sa mort, infligeant 2 dégâts aux unités adjacentes.
- **Fonceur (Dash)** : se déplace toujours le plus loin possible dans la direction choisie.
- **Insensible au gel** : immunité totale au statut Gelé (réservé aux boss).

### 1. Ennemis normaux (apparaissent dans les vagues)

| Ennemi | PV | PV (Jour 5+) | Trait | Première apparition | Comportement |
|---|---|---|---|---|---|
| Ashigaru | 3 | 4 | — | Bamboo Grove | Épéiste de base, attaque au corps à corps sans particularité |
| Twin Tachi | 2 | 3 | — | Bamboo Grove | Frappe devant/derrière lui, même si cela touche un allié sur la trajectoire |
| Spike Charger | 1 | 2 | — | Bamboo Grove | Charge en ligne droite, utilise son armure comme arme |
| Yari Master | 4 | 5 | — | Whispering Caves | Frappe à la lance sur 2 cases de portée |
| Yumi Sniper | 3 | 4 | — | Hibiku Wastelands | Tire à distance, efficace même depuis l'arrière du groupe |
| Guardian | 5 | 7 | Agressif | Moonlit Port | Avance sans relâche à l'épée après chaque attaque |
| Ashigaru Archer | 2 | 3 | — | Moonlit Port | Reste en retrait et attend le bon moment pour tirer |
| Grappler | 5 | 6 | — | Spirit Gateway | Attire le joueur vers lui |
| Shinobi | 4 | 5 | Rapide | Spirit Gateway | Frappe très vite, laisse peu de temps pour réagir |
| Shielder | 4 | 5 | — | Hot Springs | Protège ses alliés (bouclier) |
| Warden | 1 | 2 | Explosif | Forsaken Grounds | Faible mais dangereux à sa mort (explosion) |
| Strider | 4 | 5 | Fonceur | Forsaken Grounds | Se déplace d'un bloc sur toute la distance possible |
| Kabukai | 5 | 6 | — | Theatre of Illusions | Copie et retourne les mouvements du joueur contre lui |
| Shadow Charger | 5 | 7 | Rapide | Hideyoshi's Keep / Nobunaga's Fortress / Ieyasu's Garden | Fonce silencieusement, apparu après "la Balafrure" (Scarring) |

### 2. Ennemis non-standards (n'apparaissent pas via les vagues classiques)

| Ennemi | PV | Trait | Origine | Comportement |
|---|---|---|---|---|
| Dummy | 4 | — | Tutoriel (invoqué par l'Astronome) | Simple mannequin d'entraînement, passif |
| Barricade | 5 | — | Hibiku Wastelands | Obstacle statique sans attaque |
| Summoner | Variable | Lourd | Présent dès le début de certains combats de boss | Invoque des ennemis pour protéger le boss associé |
| Blight Charger | 1 (2 au Jour 5+) | Rapide | Forsaken Grounds | Charge rapidement, accumule vite les dégâts |
| Swapper | 30 | — | Ieyasu's Garden | Créature mécanisée liée à un fragment de "Shard" |
| Corrupted Progeny | 1 | Rapide | Apparaît quand un ennemi "Corrompu" meurt (Jour 7) | Expérience ratée, agit comme une menace secondaire explosive |
| Thorns | 1 | — | Tuile posée par le joueur ou un ennemi | Barrière avec effet de riposte |

### 3. Traits Élites (variantes plus fortes, dès le Jour 2)

| Trait Élite | Effet | Changement visuel/statistique |
|---|---|---|
| Rapide (Quick) | Peut attaquer dès l'ajout d'une tuile à sa file | Aura jaune, +1 PV |
| Lourd (Heavy) | Ne peut être ni déplacé ni retourné | Aura grise, +1 PV |
| Frappe Double (Double-Striker) | Effectue son attaque deux fois (double frappe uniquement sur la dernière tuile si plusieurs) | Aura rouge sombre, +2 PV |
| Bouclier Réactif | Gagne automatiquement un bouclier après avoir subi des dégâts | Aura bleue, +1 PV |
| Corrompu | Fait apparaître une Progéniture Corrompue à sa mort (Jour 7 uniquement) | Palette rose, aura de corruption |

**Notes utiles** :
- Un Warden ou un Shielder ne peuvent jamais être élites (incompatibilité de traits).
- Les Twin Tachi et Yari Master déclenchent leur attaque même si elle touche un allié sur la trajectoire — une façon indirecte d'éliminer des ennemis sans les attaquer soi-même.
- Si un Smoke Bomb tue un Warden, celui-ci échange d'abord sa position avec l'attaquant avant de mourir et d'exploser à son ancien emplacement.



# Shogun Showdown — Détail complet des Boss et des Compétences

## A. LES BOSS

### Règles générales
- Chaque boss possède une variante **Corrompue**, qui ne peut apparaître qu'à partir du **Jour 4**. Elle ajoute généralement le trait **Rapide**, de nouvelles tuiles (souvent des tuiles de corruption qui soignent le boss en blessant le joueur), et parfois d'autres traits (Agressif, Fonceur supplémentaire...).
- Les PV indiqués "Jour 5+" correspondent à l'augmentation générale de PV appliquée à tous les ennemis/boss à partir du 5ᵉ jour de boucle.
- Vaincre un boss pour la première fois débloque un succès et l'accès à la ou les région(s) suivante(s).

---

### 1. Daisuke the Dasher — *Bamboo Grove*
- **PV** : 15 (20 au Jour 5+) — **Traits** : aucun (Corrompu : Rapide)
- **Terrain** : 5 cases, Daisuke sur la 4ᵉ, joueur sur la 2ᵉ.
- **Comportement** : agit comme un ennemi commun amélioré. Alterne Katana/Swirl en s'approchant du joueur, invoque occasionnellement un Spike Charger (à tuer vite avant qu'il ne s'accumule), et représente surtout une menace via un combo **Charge + Katana**, difficile à éviter sans mobilité ou capacité spéciale.
- **Version Corrompue** : gagne la tuile **Tetsubo** et utilise beaucoup plus souvent le combo Charge + frappe ainsi que l'invocation. Il faut rester proche de lui pour raccourcir sa charge, et privilégier l'élimination des Spike Chargers seulement quand Daisuke ne prépare pas une attaque lourde.

### 2. Rei the Ruthless — *Bamboo Grove (boss alternatif)*
- **PV** : 13 (18 au Jour 5+) — **Traits** : aucun (Corrompu : Rapide)
- **Terrain** : 7 cases, Rei sur la 7ᵉ, joueur sur la 4ᵉ.
- Remplace Daisuke après une première victoire contre le Shogun.
- **Comportement** : son attaque principale est le **Dragon Punch**, facile à esquiver. Elle invoque des Twin Tachi (moins dangereux que les Spike Chargers de Daisuke), qu'elle peut ensuite frapper elle-même au Dragon Punch pour infliger des dégâts de collision sur le joueur. Elle utilise aussi **Back Charge + Summon** pour se replacer en sécurité, et **Charge + Dragon Punch** comme combo de rapprochement.
- **Version Corrompue** : invoque plus souvent des Twin Tachi et utilise plus fréquemment le Dragon Punch ; stratégie globalement identique à la version normale.

### 3. Iwao the Impaler — *Whispering Caves*
- **PV** : 25 (28 au Jour 5+) — **Traits** : aucun (Corrompu : Rapide)
- **Terrain** : 7 cases, Iwao sur la 5ᵉ, joueur sur la 3ᵉ.
- **Comportement** : privilégie les attaques à plusieurs cases : **Earth Impale**, souvent précédé d'un **Swirl** en préparation, et parfois **Spear** seul. Invoque surtout des Yari Masters (et occasionnellement des Spike Chargers). Aucune menace à distance en dehors de ces attaques, donc il est sûr de nettoyer les invocations sans crainte de représailles à distance.
- **Version Corrompue** : gagne le trait Rapide et un combo **Fonce + Spear**, rendant l'esquive par simple recul insuffisante (il faut se placer derrière lui ou bloquer avec une autre unité) ; utilise aussi occasionnellement une **Corrupted Wave** après ses attaques.

### 4. Baru the Barricader — *Hibiku Wastelands*
- **PV** : 20 (25 au Jour 5+) — **Traits** : aucun (Corrompu : Rapide)
- **Terrain** : 7 cases, Baru sur la 5ᵉ, joueur sur la 3ᵉ.
- **Comportement** : attaque au **Swirl**, puis recule et se protège derrière une **Barricade** (via Back Charge + Barricade, ou Dragon Punch + Barricade s'il est coincé). Alterne ensuite invocations et tirs de **Volley**, jusqu'à un maximum de 3 unités sur le terrain (barricade comprise), après quoi il ne fait plus que tirer. Détruire rapidement la barricade est la clé : cela le force à ressortir au Swirl, l'exposant aux dégâts. Les armes perforantes (Spear, Crossbow) ou qui ignorent les obstacles (Lightning, Shadow Kama) sont particulièrement efficaces.
- **Version Corrompue** : gagne le trait Rapide (cycle d'invocation/tir plus rapide) et la tuile **Spear** qui remplace parfois le Swirl, rendant les frappes de mêlée plus risquées. Il obtient aussi **Corrupted Wave**, qui le soigne significativement puisqu'il reste souvent immobile — d'où l'importance de dégâts de burst plutôt que de dégâts progressifs.

### 5. The Twins — *Moonlit Port*
- **PV** : 35 (43 au Jour 5+) — **Traits** : Agressif (Corrompu : Agressif + Rapide)
- **Terrain** : 7 cases, un Twin à chaque extrémité, joueur au centre (4ᵉ case).
- **Comportement** : identiques et agressifs, ils avancent après chaque coup (Spear, Tetsubo, Swirl), risquant d'enfermer le joueur entre eux. La stratégie consiste à rester à une extrémité du terrain pour n'affronter qu'un seul Twin de face ; le second, resté en retrait, passe alors en **Spear**, tandis que celui de devant utilise **Shadow Dash** ou **Dash + Swap Toss** pour retenter de recentrer le joueur entre eux deux.
- **Version Corrompue** : gagne le trait Rapide et la tuile **Meteor Hammer** (le ricochet peut désormais toucher le joueur même en jouant un Twin contre l'autre) ainsi qu'une **Corrupted Wave** qui les soigne deux fois.

### 6. The Statue — *Spirit Gateway*
- **PV** : 30 (40 au Jour 5+) — **Traits** : Lourd (Corrompu : Lourd)
- Accompagné d'un **Invocateur (Summoner)** à 15 PV, également Lourd.
- **Terrain** : 7 cases ; Statue sur la 7ᵉ, Invocateur sur la 1ʳᵉ, joueur sur la 4ᵉ.
- **Comportement** : ni la Statue ni son Invocateur ne bougent jamais. La Statue empile des tuiles aléatoires (avec un handicap de vitesse — un tour de préparation en plus) jusqu'à 3 tuiles en file, la première pouvant être une tuile à distance. L'Invocateur fait apparaître un ennemi aléatoire en synchronisation avec la première tuile de la Statue. Il n'est généralement pas conseillé de tuer l'Invocateur, ses invocations pouvant bloquer les attaques à distance de la Statue ; il vaut mieux foncer sur la Statue elle-même pendant qu'elle prépare son enchaînement.
- **Version Corrompue** : gagne 4 nouvelles tuiles et perd son handicap de vitesse (cycle plus rapide). Sous 25 % de PV, elle déclenche un enchaînement **Origin of Symmetry + Swirl + Twin Tessen** pour se téléporter au centre, puis répète indéfiniment **Arrow + Sharp Turn + Arrow**, une attaque quasi inévitable qu'il faut conclure au plus vite.

### 7. Kowa the Coward — *Hot Springs*
- **PV** : 30 (40 au Jour 5+) — **Traits** : aucun (Corrompu : aucun, mais Smoke Bomb à Double Frappe)
- **Terrain** : 7 cases, Kowa sur la 5ᵉ, joueur sur la 3ᵉ.
- **Comportement** : fuit constamment le joueur. Deux ennemis apparaissent naturellement au fil du combat (pas via Summon) pour se placer entre lui et le joueur. Une fois acculé à un bord, il se protège d'un **Shield**, puis enchaîne des **Smoke Bomb** pour s'échapper tant qu'il n'a rien entre lui et le joueur ; il régénère un bouclier dès qu'il n'en a plus. Sa seule attaque (Smoke Bomb) est faible et facilement esquivable ; le vrai danger vient des ennemis alentour. Les dégâts progressifs (poison notamment, qui perce son bouclier) et les armes qui ignorent les obstacles sont très efficaces.
- **Version Corrompue** : ne gagne pas le trait Rapide (fait rare parmi les boss), mais son Smoke Bomb devient à Double Frappe et des Grapplers rejoignent sa liste d'invocations ; il utilise aussi occasionnellement une **Corrupted Wave** à la place du Smoke Bomb.

### 8. Fumiko the Fallen — *Forsaken Grounds*
- **PV** : 28 (35 au Jour 5+) — **Traits** : Fonceur (Corrompu : Fonceur + Rapide)
- **Terrain** : 7 cases, Fumiko sur la 6ᵉ, joueur sur la 2ᵉ.
- **Comportement** : fonce sur le joueur avec **Spear** ou **Swirl**, frappe puis se replace en reculant. Peut aussi jeter une **Curse** à distance, ou invoquer deux Blight Chargers. Se joue comme un Strider en plus rapide ; son Spear nécessite souvent la capacité spéciale pour être esquivé, et il faut prioriser l'élimination des Blight Chargers (rapides et cumulant vite les dégâts).
- **Version Corrompue** : gagne **Blazing Suisei** (nécessitant de garder une option d'esquive même à 2 cases de distance) et **Corrupted Wave**, et invoque 3 Blight Chargers au lieu de 2.

### 9. Sato the Stagemaster — *Theatre of Illusions*
- **PV** : 38 (45 au Jour 5+) — **Traits** : aucun
- Combat structuré en **Actes** plutôt qu'en vagues classiques.
- **Acte 1 & 3** : Sato utilise **Copycat Mirror** pour copier et réutiliser une tuile du joueur contre lui, avant de faire tomber le rideau (**Maku**) et de passer à l'acte suivant.
- **Actes 2 & 4** : une variante de terrain est tirée au sort parmi 4 ("Mirror Clash", "The Ambush", "The Great Wave", "Storming the Walls"), chacune avec sa propre composition d'ennemis (Striders/Kabukai, Archers/Yari Masters, groupes mixtes avec Spike Chargers, etc.) ; l'Acte 4 ne peut pas répéter la variante déjà vue en Acte 2.
- **Acte 5 (Final)** : dernier affrontement avec deux compositions possibles d'ennemis ; tuer Sato élimine instantanément tous ses alliés restants.
- Sato est le seul boss à n'avoir aucune tuile d'attaque propre, se reposant entièrement sur la copie des tuiles du joueur.
- **Version Corrompue** : son Copycat Mirror devient à Double Frappe, et un 5ᵉ variante d'Acte 2/4 s'ajoute (vague de Wardens explosifs), plus une variante supplémentaire pour l'Acte final.

### 10. Hideyoshi the Cunning — *Hideyoshi's Keep*
- **PV** : 45 (55 au Jour 5+) — **Traits** : Rapide, Insensible au gel
- **Terrain** : 7 cases, Hideyoshi sur la 5ᵉ, joueur sur la 3ᵉ.
- **Comportement** : n'agit que par **combos fixes de 3 tuiles**, qui servent aussi de déplacement (pas de recul "normal"). Il ne répète jamais le même combo deux fois de suite. Combos observés : *Origin + Swirl + Earth Impale*, *Back Charge + Mirror + Spear*, *Swirl + Mirror + Swirl*, *Dash + Bomb + Back Charge*. Il invoque aussi des renforts, qu'il vaut mieux éliminer rapidement car ils modifient l'endroit où ses tuiles de déplacement l'amènent. "Camper" un coin est une stratégie efficace, beaucoup de ses combos reposant sur Mirror/Back Charge qui l'y ramènent aussi.
- **Version Corrompue** : ajoute deux combos plus dangereux — *Curse + Turn Around + Curse* (gérable, éviter d'être touché pour ne pas subir le double dégât) et *Meteor Hammer + Turn Around + Meteor Hammer* (très large zone, nécessite de garder ses distances ou de se cacher derrière un ennemi). Peut aussi invoquer des Grapplers.

### 11. Nobunaga the Wicked — *Nobunaga's Fortress*
- **PV** : 40 (50 au Jour 5+) — **Traits** : Rapide, Insensible au gel (Corrompu : + Agressif)
- **Terrain** : 7 cases, Nobunaga sur la 5ᵉ, joueur sur la 3ᵉ.
- **Comportement** : agit globalement comme un ennemi normal (avance, frappe, recule), avec occasionnellement une invocation de 2 ennemis ou un Shadow Dash à travers le joueur. Sa mécanique unique est le **système de projecteurs** : 3 cases sont éclairées en début de combat, et Nobunaga n'est vulnérable que lorsqu'il se trouve sous une lumière. Après un coup réussi sous la lumière, les projecteurs se redistribuent aléatoirement ; leur nombre diminue à 2 puis 1 quand ses PV passent sous les 2/3 puis 1/3. Les dégâts de burst (plutôt que progressifs) et les tuiles qui déplacent l'ennemi ou le héros (Dragon Punch, Grappling Hook, Smoke Bomb, Mirror...) sont essentiels pour l'amener sous la lumière.
- **Version Corrompue** : gagne **Twin Tessen** et **Corrupted Wave**, ainsi qu'une **Curse** occasionnelle ; peut désormais invoquer aussi des Kabukai et des Shinobi. Le combat démarre avec un projecteur en moins, mais le seuil de réduction ne s'applique qu'à 33 % de PV. Le trait **Agressif** ajouté le fait au contraire se rapprocher plus volontiers des zones éclairées, ce qui facilite paradoxalement le combat.

### 12. Ieyasu the Patient — *Ieyasu's Garden*
- **PV** : 40 (50 au Jour 5+) — **Traits** : Insensible au gel (Corrompu : + Rapide)
- Accompagné d'un **Swapper** (30 PV).
- **Terrain** : 9 cases, Ieyasu sur la 7ᵉ, Swapper sur la 5ᵉ, joueur sur la 3ᵉ.
- **Comportement** : le combat le plus "simple" en apparence mais potentiellement le plus punitif — il recharge en continu **Dash + Blade of Patience** (dont les dégâts augmentent tant qu'elle reste en file) et ne la déclenche que lorsqu'il a une ligne de vue directe sur le joueur, sinon il invoque des renforts ou, plus rarement, utilise **Origin of Symmetry** pour se repositionner. Il faut absolument disposer d'un moyen d'esquiver son attaque à un coup (capacité spéciale idéalement préparée à l'avance).
- **Version Corrompue** : gagne la tuile perçante **Nagiboku**, lui permettant de frapper à travers ses invocations ou le Swapper.

### 13. The Shogun — *Château du Shogun*
- **PV** : 40 (50 au Jour 5+) — **Traits** : Rapide, Insensible au gel — n'a pas de variante Corrompue.
- Accompagné de deux **Invocateurs** (30 PV, Lourd) qui piochent d'abord dans une liste "facile", puis basculent sur une liste "difficile" si l'un des deux meurt.
- **Terrain** : 7 cases, le Shogun sur la 5ᵉ, joueur sur la 3ᵉ.
- **Phase 1** : comportement proche d'un ennemi classique (approche, frappe, recul), avec occasionnellement **Mirror** pour se replacer et une charge à la lance. Rester à 2 cases de distance permet à la fois d'esquiver la lance et de réagir à un éventuel combo Charge + Spear.
- **Phase 2** (déclenchée automatiquement, tous les ennemis restants meurent à la transition) : ouvre systématiquement par **Corrupted Barrage** (en se téléportant d'abord au centre si nécessaire), puis alterne des combos fixes sans répétition consécutive : *Charge + Swirl*, *Back Charge + Mirror puis Shuriken au tour suivant*, ou *Origin* (utilisé seul pour changer de combo). Se tenir à 2 cases du centre après une téléportation permet de réagir à n'importe lequel des enchaînements.

### 14. Corrupted Soul — *Sanctuaire Écarlate (boss caché, Jour 7 uniquement)*
- **PV** : 70 — **Traits** : Insensible au gel (Corrompu : + Rapide)
- **Terrain** : 9 cases, joueur et Corrupted Soul démarrent sur la même case (5ᵉ), le boss étant positionné au-dessus de la grille.
- **Comportement** : véritable boss final, débloqué juste après avoir vaincu le Shogun. Sa particularité est de se déplacer sur un **axe vertical** : tant qu'il "flotte" au-dessus du terrain, il est totalement invulnérable. Il alterne deux invocations successives (4 ennemis au total, la deuxième étant annulée si les 4 premiers meurent assez vite), puis prépare **Tainted Triad** en descendant attaquer le joueur ; s'il atterrit sur une unité, il remonte aussitôt, sinon il reste au sol un tour avant de reprendre son cycle (Invoquer → Invoquer → Attaquer → Attaquer). À 50 % de PV, il remonte immédiatement après l'attaque du joueur et déclenche un **Corrupted Pulse** avant de reprendre son cycle normal.
- **Stratégie** : prioriser l'élimination des ennemis invoqués les plus dangereux (Grapplers, Shadow Chargers, Kabukai, Striders, Corrupted Progeny, Yumi Snipers), garder ses consommables (potions de bouclier, Mass Curse) pour cette phase, et concentrer les gros dégâts sur le boss uniquement quand il est au sol.

---

## B. LES COMPÉTENCES DE BOUTIQUE (Skills)

### Principe général
Après chaque région vaincue, le joueur visite une boutique et peut acheter des compétences avec de l'or. Il existe **36 compétences** (7 par catégorie + 1 disponible partout + 2 exclusives à la boutique de l'Astronome). Acheter plusieurs fois la même compétence en augmente le niveau (jusqu'au niveau 3), renforçant son effet ; certaines compétences ne peuvent toutefois pas être cumulées. Le nombre de compétences achetables simultanément dans chaque boutique doit être débloqué avec des crânes (monnaie de méta-progression).

### Compétence disponible dans toutes les boutiques
| Compétence | Coût | Effet |
|---|---|---|
| Big Pockets | 10 | +1 emplacement de consommable (jusqu'à 6 au total) |
| Rogue Retail | 10 | Permet de vendre des consommables en dehors des boutiques, à +1 pièce de valeur |

### Boutique Guerrier (Warring) — compétences offensives
| Compétence | Coût | Effet |
|---|---|---|
| Back Stabber | 15 | +1 dégât en attaquant un ennemi par derrière |
| Sniper | 15 | +1 dégât en attaquant à distance de 4 cases ou plus |
| Unfriendly Fire | 10 | Les ennemis s'infligent +1 dégât entre eux |
| Mindfulness | 20 | En attendant (action "wait"), recharge un point de cooldown supplémentaire |
| Monomancer | 20 | +1 dégât lorsqu'on attaque avec une seule tuile en file (non cumulable) |
| Close Combat | 20 | +1 dégât en attaquant un ennemi adjacent |
| Central Dominion | 10 | +1 dégât en attaquant depuis le centre du terrain |
| Odd Curse | 10 | À chaque vague impaire, un ennemi est automatiquement maudit |

### Boutique Combo — compétences liées aux combos
| Compétence | Coût | Effet |
|---|---|---|
| Combo Coin | 15 | Les ennemis tués en combo lâchent une pièce supplémentaire |
| Combo Recharge | 20 | Une tuile ayant réalisé un combo kill recharge 4 points de cooldown (8 au niveau 2) |
| Triple-Combo Heal | 20 | Soigne 1 PV en cas de triple combo kill (3 ennemis en un tour) |
| Combo Curse | 15 | Un combo kill maudit un ennemi aléatoire |
| Combo Deal | 10 | Utiliser deux consommables le même tour donne un nouveau consommable aléatoire |
| Kobushi Combo | 15 | Un combo kill recharge toutes les tuiles en main |
| Combo Boon | 15 | Un combo kill a 1 chance sur 5 de faire tomber une carte (Mass Curse/Poison/Ice/Rain of Mirrors) |
| Chilling Combo | 20 | Un combo kill gèle un ennemi aléatoire |

### Boutique Garde (Guarding) — compétences défensives
| Compétence | Coût | Effet |
|---|---|---|
| Healthy | 15 | +2 PV Max |
| Fortress | 20 | Commence les combats avec un bouclier, mais -2 PV Max |
| Reactive Shield | 20 | Gagne automatiquement un bouclier après avoir subi des dégâts |
| Shield Retention | 15 | Les boucliers persistent d'un combat à l'autre |
| Karma | 10 | Renvoie les dégâts subis à l'ennemi qui les a infligés |
| Chilling Blood | 15 | Gèle tous les ennemis dès qu'on subit des dégâts |
| Iron Skin | 20 | Réduit les dégâts subis de 1 (jamais à 0), mais -3 PV Max |
| Overflow Guard | 15 | Le soin qui dépasse les PV max se transforme en bouclier |

### Boutique Danseur (Dancer) — compétences de mobilité
| Compétence | Coût | Effet |
|---|---|---|
| Two-Way Move | 15 | Permet d'utiliser la capacité spéciale dans les deux directions |
| Quick Recovery | 15 | -1 cooldown pour la capacité spéciale (minimum 1) |
| Damaging Move | 15 | La capacité spéciale inflige 1 dégât |
| Dynamic Boost | 15 | +1 dégât si l'on s'est déplacé grâce à une tuile ce tour |
| Cursing Move | 15 | La capacité spéciale applique une malédiction |
| Chikara Crush | 15 | Les ennemis subissent +1 dégât de collision |
| Mamushi Move | 20 | La capacité spéciale empoisonne (+1 cooldown) |
| Two-Faced Dancer | 20 | Se retourner devient une action gratuite ; se retourner deux fois de suite s'auto-maudit |

### Boutique de l'Astronome (Moonlit Port Shop)
Regroupe toutes les compétences ci-dessus, payables en crânes, plus deux compétences exclusives :
| Compétence | Coût | Effet |
|---|---|---|
| Fenghuang's Feather | 20 | Ressuscite avec la moitié des PV max actuels lors d'un coup normalement fatal |
| Seiryu's Scale | 20 | Le premier reroll de chaque récompense est gratuit |

### Autres mécaniques de boutique
- Chaque boutique offre parfois gratuitement une **Edamame Brew** (potion de soin) ou une **Kami Brew** (potion de bouclier) à l'arrivée du joueur.
- **Services de sang** : perdre 1 PV pour rafraîchir entièrement la boutique, ou perdre 2 PV pour gagner 5 pièces. Le Dé Chanceux (Lucky Die) permet d'obtenir un rafraîchissement de boutique sans perdre de PV.
- Le prix des rerolls et des upgrades augmente à chaque utilisation successive dans une même boutique/région.

