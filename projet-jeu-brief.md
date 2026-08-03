# Brief projet — Starfall

> **Ce document décrit l'état réel du projet.** Il a été réécrit le 3 août 2026 parce
> qu'il décrivait encore le projet tel qu'il était *envisagé* : choix de moteur ouvert,
> univers shogun, boucle de feedback « à mettre en place ». Trois de ces points étaient
> tranchés depuis longtemps et un quatrième était livré. Un audit l'a qualifié de
> **piège à lecture** pour tout nouvel arrivant — humain ou agent — et il avait raison.
>
> La version d'origine est dans l'historique git. Ce qui a changé et pourquoi est en
> annexe, parce que dans ce projet les décisions se lisent avec leur raison.

---

## Concept

**Starfall** — duel tactique 2D au tour par tour, sur une lane linéaire de 5 à 15 cases,
rendu comme une peinture à l'encre à demi rêvée.

**Priorité absolue, inchangée depuis le premier jour** : un rendu et une animation
distinctement poétiques et oniriques. Coups de pinceau à l'encre qui coulent, lumière
douce et peinte, personnages et lames se mouvant comme dessinés dans un rêve à demi
souvenu. Ni hyper-réaliste, ni sec, ni orienté impact. Les réactions aux coups doivent se
lire comme des **temps poétiques**, pas comme des chocs.

Les animations d'**interaction entre personnages** — parades, contacts d'armes, reculs —
sont procédurales, pas des cycles pré-calculés joués en boucle.

## Univers

*L'Atlas des Songes Éteints* (voir `STORY.md`). Le monde est un cosmo-atlas dessiné à
l'encre ; le joueur est le **Pèlerin de la Nuit**, portant une lame forgée dans une
étoile tombée, affrontant des **Ombres Cartographiées** à travers le **Pli du Monde**.

**Le cadrage shogun/samouraï a été retiré** comme trop culturellement situé
(`STORY.md §2`, `combat-design.md §3b` : *re-skin, not a redesign*). Les huit images de
référence restent japonaises et restent la vérité terrain — mais **pour la matière, la
valeur, l'atmosphère et le mouvement, pas pour l'iconographie**. Ne jamais exiger un
tsuba ou un hakama ; exiger la silhouette qu'ils enseignent.

## Plateformes

Desktop d'abord, **mobile-safe** : limites GLES 3.0, budget d'os et de particules
respectés dès le départ (32 os à l'origine, 36 aujourd'hui).

**Point ouvert et honnête** : rien n'a jamais tourné en interactif sur un appareil. La
cible mobile est une contrainte de conception respectée, pas une contrainte *vérifiée*.
Un gate de performance interactive est dû avant tout nouvel empilement d'octaves shader.

## Moteur — **tranché : libGDX / Java 21**

Godot et Unity ont été écartés. Le contrôle bas niveau, l'accès direct aux shaders GLSL
et l'absence d'abstraction gênante au-dessus d'un système d'animation entièrement custom
l'ont emporté.

Gradle 8.10.2 épinglé via le wrapper `./gw`, parce que le JAVA_HOME de la machine pointe
sur un JDK 24 que ce Gradle ne sait pas exécuter. **Ne jamais invoquer gradle
directement, ne jamais modifier JAVA_HOME.**

## Animation — système custom, construit

Spine et DragonBones écartés. Tout est recodé, et **le pipeline Blender → glTF envisagé
au départ n'existe pas** : l'art est **entièrement procédural, défini dans le code**, ce
qui supprime toute dépendance à un asset externe et permet à la boucle
itération → capture → revue de tourner à pleine vitesse.

| # | Système | État |
|---|---|---|
| 1 | Hiérarchie d'os + skinning GPU | fermé au plafond de 5 passes, dette consignée |
| 2 | IK (2 os analytique + FABRIK) | **passé** |
| 3 | Verlet cheveux + tissu | fermé sur un **négatif démontré** |
| 3b | Visages (expression + variété) | passe 2 livrée, revue à faire |
| 3c | Classes de matière et texture | pas commencé |
| 4 | Couche d'interaction procédurale | fermé au plafond, document d'héritage |
| 5 | Combat + file d'actions | moteur fait ; couche visuelle en passe 3 |
| 6 | Harnais de capture + serveur MCP | **fait** |

## Méthode de développement

**Construire → capturer → faire relire par un agent indépendant → recommencer**, avec un
plafond de cinq passes par système. Ce qui n'est pas résolu au plafond est consigné comme
dette plutôt que de bloquer le projet.

**Ce qui construit ne note jamais son propre travail** : la revue est un sous-agent
distinct, à contexte neuf, sans intérêt dans les décisions prises. C'est la règle la plus
rentable du projet — les revues ont trouvé un garde incapable d'échouer, un ennemi hors
champ sur 69 % des plateaux, une palette de portrait employée sur un ciel crépusculaire,
et six assertions qui ne s'exécutaient pour personne d'autre que leur auteur.

Les barèmes sont `STYLE.md` (ce que le jeu doit être) et `MEASUREMENT.md` (ce qui compte
comme preuve). `progress.html` est la fenêtre du propriétaire sur le travail.

## Boucle de feedback — **opérationnelle**

Capture hors écran à pas de temps fixe, planches-contact, manifestes `capture.txt`
portant la commande qui reproduit la capture. Outil d'analyse en ligne de commande,
`Rehearsal` qui rejoue un duel entier sans contexte GL, serveur MCP (`mcp/starfall-mcp.mjs`)
et `tools/sfctl.mjs`.

## Ce qui n'existe pas encore, et qui compte

- **Aucune gestion d'entrée. Personne n'a jamais joué à ce jeu.** Sept systèmes de rendu
  ont été construits sans qu'un humain ait jamais tenu une manette, et trois questions de
  design ouvertes attendent explicitement « un combat jouable » pour être tranchées.
- **Les PV ennemis ne sont pas dessinés du tout** — bloqueur de jouabilité sur un jeu
  tactique.
- **Aucun métajeu** : ni run, ni progression, ni raison de rejouer.
- **Aucun son.** L'audit note que c'est la moitié manquante de la poésie.
- **Le compte de parties lisibles** plafonne sous la cible de 18 (`STYLE.md §11.4`) ; les
  garnitures — mains, prise, garde, plis, pieds — n'ont jamais figuré sur l'ordre de
  travail d'une passe en sept systèmes.

---

## Annexe — ce que cette réécriture a corrigé

| Le document disait | La réalité |
|---|---|
| Choix moteur « en cours d'arbitrage », libGDX vs Godot | libGDX/Java tranché, et tout le code l'est |
| Univers shogun/samouraï | *L'Atlas des Songes Éteints* ; cadrage shogun retiré |
| Boucle de feedback « à mettre en place » | livrée, et load-bearing pour chaque revue |
| Serveur MCP « étape suivante envisagée » | opérationnel |
| Pipeline Blender → glTF | jamais construit ; l'art est procédural |
| « Combats sur une quinzaine de cases » | lane de 5 à 15, la longueur est un cadran de composition |

Rien de ce qui était juste n'a été retiré : la priorité au rendu poétique, le rejet de
Spine/DragonBones, les quatre composants du système d'animation et la discipline de
capture sur les changements brusques viennent tous du brief d'origine et tiennent encore.
