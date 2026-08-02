# STORY.md — Lore, Vision & Mémoire de Conception

Ce document constitue la référence narrative et conceptuelle du projet **Starfall**. Il définit l'univers du jeu, la fusion entre les mécaniques tactiques et l'univers poétique, et conserve la mémoire des pistes explorées lors de la conception.

---

## 1. Vision Retenue : *L'Atlas des Songes Éteints*

### 1.1 Pitch & Cosmologie

> *« Le ciel n'a jamais été fait de matière, mais de l'encre dont sont peints les rêves du monde. Quand une étoile tombe, c'est un songe géant qui s'éteint et vient s'écraser sur le réveil. »*

Dans cet univers universel et onirique, le monde réel n'existe pas. Il n'existe qu'un vaste **Cosmo-Atlas** dessiné à l'encre par une entité mystérieuse : **Le Rêveur Oublié**. Chaque continent est une page de parchemin chaud, chaque rivière une coulee d'encre indigo, et chaque étoile au firmament une pensée maintenue éveillée pour préserver la nuit du vide.

Le **Starfall** désigne la grande catastrophe cosmique : le Rêveur commence à s'éveiller. Les constellations se détachent du ciel et s'effondrent sous forme de **pluies d'encre stellaire incandescente**. En percutant la surface du parchment, cette encre dissout les contours du monde et matérialise des **Ombres Cartographiées** — des chimères nées des rêves brisés et des ratures oubliées.

Le joueur incarne le **Pèlerin de la Nuit**, une silhouette d'encre épurée armée d'une lame forgée dans le dernier éclat d'une étoile tombée. Il parcourt le **Pli du Monde** (la grille de combat) pour affronter les Ombres et préserver l'ultime étincelle de lumière avant l'évanouissement total du songe.

---

### 1.2 La File d'Actions : La Mécanique comme Philosophie

Dans *L'Atlas des Songes Éteints*, la file d'actions (limitée à 3 tuiles) n'est pas une simple contrainte d'interface : elle est le pilier philosophique et narratif du combat.

```
 [ GÈLE L'INSTANT ]  ──>  [ ANCRE LES GLYPHES ]  ──>  [ DÉCLENCHE LA STROPHE ]
   (Pause tactique)        (Queue : 1 à 3 tuiles)       (Exécution ininterrompue)
```

1. **L'Ancrage des Glyphes (Préparation / Ajout de tuile) :**
   Dans le monde des rêves, aucun geste n'est impulsif. Ajouter une tuile à la file (consommer un tour) revient à tremper son pinceau dans l'encre stellaire et à imprimer son intention sur le parchemin du terrain.
2. **La Rature et la Correction (Ajustement gratuit à 0 coût) :**
   Tant que la séquence n'est pas déclenchée, le Pèlerin possède la lucidité du rêveur : il peut effacer du bout du doigt un tracé d'encre (retirer ou réordonner une tuile) sans dépenser de temps. C'est l'acte de "débugger sa strophe" en temps réel.
3. **Le Tracé Ininterrompu (L'Exécution "Tout-ou-Rien") :**
   Lorsque l'exécution est lancée, l'encre sèche instantanément. Le Pèlerin glisse le long du terrain d'un seul mouvement chorégraphié. Si la séquence est exacte, la lame d'étoile tranche l'Ombre ; si la trajectoire manque sa cible, le Pèlerin poursuit son tracé jusqu'au bout de son encre, restant exposé.
4. **La Lecture des Ratures (Indicateurs d'Intention Adverses) :**
   Les attaques adverses apparaissent sur la grille sous forme de traînées d'encre vermillon (`#C8382E`). Anticiper le coup ennemi revient à déchiffrer les ratures que l'Ombre s'apprête à porter sur la carte.

---

### 1.3 Ancrage Visuel & Matière

La vision narrative s'articule directement avec le ruban visuel du projet ([`STYLE.md`](file:///C:/homeware/perso/spaces/starfall/STYLE.md)) :

* **Le Papier Ground (`#EDE4D3`) :** Le terrain est la surface d'une page de l'Atlas.
* **La Dissolution d'Encre (*Edge Dissolve*) :** Les vêtements du Pèlerin et des Ombres se frangent en nuages d'encre mouillée aux extrémités, illustrant la frontière poreuse entre le corps et le songe.
* **La Lame d'Étoile (`#EAF2F8`) :** L'unique trait dur et d'une netteté parfaite à l'écran, laissant un ruban de lumière bleue-blanche.
* **Les Poussières de Rêve :** Particules bokehs cyan (`#5FD8E8`) et magenta (`#E06BA8`) flottant au gré de la brume (`#D6D2CE`).
* **L'Impact Poétique :** Aucun violent shockwave ou hitstop. Les collisions libèrent une floraison d'encre dorée (`#FFF6E2`) et une envolée d'étincelles d'ambre (`#FF9A4D`).

---

## 2. Mémoire de Conception : Alternatives Envisagées & Écartées

Au cours de la genèse du projet, plusieurs orientations narratives et thématiques ont été soumises à l'analyse puis écartées au profit de *L'Atlas des Songes Éteints*. Ce chapitre en conserve la trace.

### Alternative A : *L'Inspiration Shogun / Samouraï (Brief Initial)*
* **Concept original :** Duel féodal japonais à l'encre de Chine (*Shogun Showdown*), avec guerriers en hakama, haori et katanas.
* **Raison du retrait :** Bien que graphiquement très fort, le cadre samouraï restreignait l'univers à un ancrage culturel précis. L'équipe a préféré pivoter vers un imaginaire plus universel, poétique et conté, offrant une liberté créative totale sur le chara-design et le lore.

### Alternative B : *Le Grimoire du Somnambule (La Ligne d'Encre)*
* **Concept :** Un duel littéraire sur les marges d'un livre d'heures en train de s'effacer.
* **Raison du retrait :** Très proche du concept retenu, mais trop centré sur la métaphore du livre et du texte. Il lui manquait la dimension spatiale et géographique qu'apporte la notion **d'Atlas** pour justifier le déplacement tactique sur la grille de 5 à 15 cases.

### Alternative C : *La Mer d'Encre Nébuleuse*
* **Concept :** Une marée d'encre céleste inondant le monde et noyant les souvenirs. Les personnages s'affrontent sur des digues de pierre suspendues au-dessus de l'océan obscur.
* **Raison du retrait :** Thème visuel marin séduisant, mais moins directement aligné avec le rendu "papier chaud / lavis d'encre" et la métaphore du tracé à la plume de la file d'actions.

### Alternative D : *La Forge des Lames d'Étoile*
* **Concept :** Focus axé sur la fabrication de 8 armes célestes légendaires forgées dans une météore tombée du ciel.
* **Raison du retrait :** Trop orienté vers un récit d'armes et de chevalerie classique, négligeant le côté onirique, l'encre et la mécanique de programmation temporelle.

---

## 3. Lexique & Vocabulaire du Jeu

Pour nourrir l'interface, les tuiles et le bestiaire :

| Terme In-Game | Concept Gameplay / Lore |
| :--- | :--- |
| **Pèlerin de la Nuit** | Le personnage joueur |
| **Ombre Cartographiée** | Ennemi / Chimère née du songe |
| **Plis du Monde** | La grille de combat (5 à 15 cases) |
| **Strophe d'Encre** | La file d'actions de 3 emplacements |
| **Rature** | Indicateur d'intention adverse (zone menacée) |
| **Glissement de Compas** | Tuile de déplacement |
| **Trait d'Étoile** | Tuile d'attaque lourde (5 dégâts) |
| **Rature Éclatante** | Tuile d'action gratuite (*Free-play*) |
| **Buvard d'Ombre** | Altération d'état (Malédiction / *Curse*) |
