# Propositions d'interface — Starfall

Quatre écrans clés en SVG (source, éditable) + PNG (rendu via Chrome headless).
Fidèles au ruban `STYLE.md` / `combat-design.md` / `STORY.md`. Échelle 1× = résolution
shipped (960×720). À ouvrir dans un navigateur pour le SVG (vectoriel, zoomable).

> Note : les SVG sont des **propositions de design**, pas des assets finaux. Les
> filtres `feTurbulence`/`feDisplacementMap` approchent l'encre mais ne remplacent
> pas les shaders du moteur ; l'intention est de communiquer la composition, la
> hiérarchie et l'alphabet.

---

## `01-planning.svg` — frame de planification (map, densifiée)
Reprend ma recommandation **BP5** (audit) : contester le verdict « carte sobre ».
- Hero tenu à ~0.45 fh (au lieu de ~0.11), duel lisible au lieu de « diagramme sur fond ».
- Brumes colorées denses (rouille/coral/violet) façon famille C — densité à bas
  contraste, pas du vide.
- Disque solaire chaud derrière l'action (**BP7**).
- Lane 11 tuiles en marques de lavis, intensifiées près des figures.
- **Strikethrough vermillon** (télégraphe ennemi) en wash mouillé sur 2 tuiles.
- **Strophe** (colonne droite) : 5 slots, base-anchored, 3 banked, la clause du haut
  la plus humide/foncée (résout d'abord, LIFO).
- **Santé** = rangée de traits qui sèchent en tête de strophe.
- **Cooldowns** (marge gauche) : ticks ochre comptables sous chaque tuile tenue.
- Sceaux ochre aux deux têtes de marge (un auteur, une feuille).
- Motes jewel (cyan/magenta/ambre) + foxing des marges.

## `02-stanza.svg` — détail de la strophe d'encre
Zoom sur la colonne pour rendre lisible la mécanique LIFO.
- Base-anchored : 1re tuile en bas, newest en haut.
- Gradient de séchage top→base (le top = clause qui résout ensuite = la plus humide).
- Au-dessus de la pile : impressions « nib » fantômes ; la ligne d'atterrissage
  suivante plus forte (la colonne énonce sa propre capacité).
- Flèche de lecture = sens d'exécution (vers le bas).

## `03-execution.svg` — frame d'exécution (push-in intime)
- Figures grandes (~0.6 fh), lames croisées près du centre.
- **Clash star** (`#FFF6E2`) sans trail (correction de la « voile » pale de s4-p5,
  proposition **B3**), 5 rayons mous + étincelles amber qui **flottent** (pas du
  grinder).
- **Fumées d'encre qui bouclent** (**BP6**) le long des silhouettes — le mécanisme
  d'escapee cheveux appliqué au vêtement.
- Cheveux jetés en avant (vocabulaire carry/knockback, §7.2).
- **Souffle retenu** : anneau lumineux très faint autour du clash (§7.3, pas de freeze).
- UI **récedée** en ghost (strophe faint en coin haut-gauche).

## `04-alphabet.svg` — alphabet des 9 tuiles
Chaque glyphe diffère par la **forme**, lisible à froid sans légende (cold-read
acceptance, proposition **L1**). Reprend les fix de `system5-debt §4` :
- **COUPER** : trait vertical de base.
- **PERCER** : trait long + trait en croix (« un corps dedans » — aucun mouvement ne croise).
- **PARER** : deux traits parallèles sur un tiers (déviation, pas collision).
- **BALAYER** : arc devant+derrière (seul autoréfléchi).
- **TIRER** : ligne de force entre deux figures, crochet.
- **PAS** : masse qui pointe vers l'ennemi, extrémité franche.
- **PAS ARRIÈRE** : le bruit est le poids qui se retire (derrière, bas) — **jamais** de
  pointe lame-like (le défaut qui faisait lire BACK_STEP comme une attaque).
- **TOURNER** : spirale + traîne (corps s'enroule, tissu/cheveux derniers).
- **FEINTE** : geste commit + fantôme sec décalé (le geste **et** le geste non fait ;
  plein en forme, vide de corps).

---

## Pour générer les PNG
```powershell
$chrome = "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
& $chrome --headless --disable-gpu --window-size=960,720 --screenshot="tmp\ui-proposals\01-planning.png" "file:///C:/homeware/perso/spaces/starfall/tmp/ui-proposals/01-planning.svg"
```
