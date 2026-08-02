# Brief projet — Jeu de combat 2D tour par tour (inspiration shogun)

## Concept
Jeu 2D tour par tour sur grille, façon *Shogun Showdown* : combats tactiques sur une quinzaine de cases. Univers d'inspiration shogun/samouraï (épées, vêtements traditionnels).

**Priorité absolue** : un rendu visuel très soigné des combattants — mouvement, vêtements, cheveux — avec des **animations procédurales sur les interactions entre personnages** (parades, contacts d'armes, réactions à l'impact), pas seulement des animations pré-calculées jouées en boucle.

## Cibles plateformes
Mobile (Android/iOS) + Desktop.

## Choix moteur (en cours d'arbitrage)
Deux options envisagées, décision non tranchée :

- **libGDX (Java)** — contrôle bas niveau total, accès direct OpenGL/shaders, aucune abstraction gênante pour injecter un système d'animation custom. Tout le système de bones/skinning/IK est à construire from scratch.
- **Godot (GDScript/C#/GDExtension)** — dispose nativement d'un système de squelette 2D (Bone2D/Skeleton2D, IK type CCDIK/FABRIK) qui peut servir de base à étendre plutôt que tout reconstruire ; GDExtension (C++) permet un contrôle bas niveau équivalent. Implique d'abandonner Java.
- Unity écarté : historique de pricing instable (runtime fee 2023, tensions sur les frais Enterprise début 2026) malgré un tooling 2D/mobile solide.

**Critère de décision encore ouvert** : rester en Java est-il une contrainte forte, ou négociable si Godot sert mieux le projet ?

## Approche animation — décision prise : système custom
Spine et DragonBones ont été évalués et écartés au profit d'un système recodé sur mesure, pour permettre le contrôle nécessaire aux animations procédurales d'interaction.

**Composants du système à construire :**
1. **Hiérarchie d'os + skinning** — transforms locaux → globaux, linear blend skinning calculé via shader GPU.
2. **IK** — solution géométrique directe pour chaînes courtes (bras/avant-bras) ; FABRIK pour chaînes plus longues.
3. **Contraintes physiques (jiggle/cloth)** — système masse-ressort ou intégration de Verlet par segment, pour le sway naturel des cheveux/vêtements.
4. **Couche d'interactions procédurales** (logique de jeu, au-dessus du système d'animation) — détection de contact (hitboxes armes/cibles), pilotage dynamique des IK targets selon la position de l'adversaire, blending d'animations selon l'état de combat (paré/touché/déséquilibré), offsets procéduraux au runtime (knockback, recul).

**Pipeline de rigging envisagé** : Blender (gratuit, gère bien le rig 2D "cutout") → export glTF (porte squelette + animations + poids de skinning) → loader/runtime custom dans le moteur choisi.

## Méthode de développement
Vibe coding. Besoin d'une boucle de feedback graphique rapide pour que l'itération sur les rendus (fluidité, qualité du jiggle/IK) puisse être évaluée visuellement à chaque étape.

## Boucle de feedback graphique — à mettre en place

**Capture automatisée** (pas de screenshot manuel) :
- LibGDX : mode headless (LWJGL), extraction du framebuffer via `ScreenUtils.getFrameBufferPixmap()` → PNG.
- Godot : `get_viewport().get_texture().get_image().save_png(...)` en script, ou `--write-movie` pour une séquence complète.

**Contact sheets** : assembler 8-12 frames clés d'un cycle d'animation en une seule image grille (script Python/PIL ou ImageMagick), pour juger la trajectoire complète d'un coup d'œil plutôt que des frames isolées.

**Cas de test prioritaires** : privilégier les captures sur les moments de changement brusque (direction, vitesse, impact) — c'est là que les artefacts d'IK/physique (overshoot, instabilité, clipping) sont visibles. Les poses statiques en idle révèlent peu de choses.

**Comparaison** : conserver les contact sheets des itérations précédentes pour comparer ancien/nouveau côte à côte.

## Étape suivante envisagée : serveur MCP de contrôle
Objectif : permettre à Claude Code de piloter directement les tests visuels sans aller-retour manuel.

**Architecture prévue :**
1. Debug API côté jeu (socket local ou endpoint HTTP) recevant des commandes, ex. `POST /trigger {"action":"play_animation","character":"shogun1","state":"parry"}`.
2. Serveur MCP (Python ou Node, SDK MCP) exposant des tools :
   - `trigger_animation(character, state)` — déclenche une animation/interaction précise à tester
   - `capture_frame` / `capture_sequence(n)` — prend un ou plusieurs screenshots, génère la contact sheet
   - `set_camera(...)` — cadre la capture sur la zone d'intérêt
3. Le serveur retourne le chemin du screenshot/contact sheet généré pour analyse.

Cette boucle MCP ne fonctionne que dans un environnement agentique (Claude Code) — pas dans l'app de chat standard.

## Décisions ouvertes à trancher avec Claude Code
- Choix final du moteur (libGDX vs Godot) — dépend de l'importance de rester en Java.
- Point de départ du prototypage : fondation (debug API + MCP) en premier, ou premier prototype du système d'os/skinning/IK pour avoir un rendu à tester rapidement.
