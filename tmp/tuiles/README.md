# Catalogue des Tuiles d'Action — STARFALL: L'Atlas des Songes Éteints

Catalogue officiel des tuiles d'attaque, de déplacement et d'action du jeu, adaptées des mécaniques de *Shogun Showdown* vers l'univers **L'Atlas des Songes Éteints**.

## Configuration de Génération (Forge Neo) :
- **Serveur** : `http://undefined.egaetan.me:7862/`
- **Modèle** : `krea2_turbo_nvfp4 [61527003b2]`
- **Modules** : `wan_2.1_vae.safetensors`, `qwen3VLInstruct4bHeretic_int8Convrot.safetensors`
- **Format** : `1024x1024` (Icônes carrées 1:1 pour cartes d'action)
- **Palette obtenables** : papier chaud (`#EDE4D3`), acier étoilé (`#EAF2F8`), ambre (`#FF9A4D`), vermillon (`#C8382E`).
- **Palette corruption** : rose (`#D96E9A`), magenta profond (`#B83A7A`), rose clair (`#E07AA8`).
- **Variante** = 3 illustrations candidate par tuile (seed + prompt distincts).
- **Scripts** : `generate_tuiles.py` (1-18 v1), `generate_tuiles_2.py` (19-39 v1), `generate_tuiles_3.py` (non-obtenables), `generate_tuiles_4.py` (obtenables v2/v3 + README canonique).

---

## Galerie des Tuiles Obtenables (1-39) — 3 variantes par tuile

### 1. Tracé de la Lame (Katana)
- **Type** : `Frappe simple (Attaque)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 0 (Tuile de base)`
- **Lore** : *Un trait d'encre fluide et direct gravé à la plume sur la case adjacente.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_01_trace_de_lame.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_01_trace_de_lame.png)
- **Prompt SD** : `Game action card tile icon, single glowing sword slash stroke in luminous blue-white ink (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), dark ink brush border, minimalist game UI icon design, high quality`

![Tracé de la Lame (Katana) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_01_trace_de_lame.png)

#### Variante 2 (seed 100001)
- **Fichier** : [tuile_01_trace_de_lame_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_01_trace_de_lame_v2.png)
- **Prompt SD** : `Game action card tile icon, diagonal falling katana slash with luminous blue-white ink spray (#EAF2F8), macro focus on the glowing blade edge, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), dark ink brush border, minimalist game UI icon`

![Tracé de la Lame (Katana) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_01_trace_de_lame_v2.png)

#### Variante 3 (seed 200001)
- **Fichier** : [tuile_01_trace_de_lame_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_01_trace_de_lame_v3.png)
- **Prompt SD** : `Game action card tile icon, twin parallel calligraphic sword strokes crossing in an X of luminous ink (#EAF2F8), expressive wet brush, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design`

![Tracé de la Lame (Katana) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_01_trace_de_lame_v3.png)

---

### 2. Marteau de Météore (Tetsubo)
- **Type** : `Frappe lourde (Attaque)`
- **Statistiques** : `Dégâts: 4 | Cooldown: 7 | Coût: 3 Crânes`
- **Lore** : *Un lourd stamp d'encre sombre écrasant la case sous la pression d'une masse céleste.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_02_marteau_de_meteore.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_02_marteau_de_meteore.png)
- **Prompt SD** : `Game action card tile icon, heavy warhammer head crashing down with floating amber embers (#FF9A4D), sumi-e ink wash painting on warm parchment paper ground (#EDE4D3), dark wet ink splatter, minimalist game UI icon design`

![Marteau de Météore (Tetsubo) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_02_marteau_de_meteore.png)

#### Variante 2 (seed 100002)
- **Fichier** : [tuile_02_marteau_de_meteore_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_02_marteau_de_meteore_v2.png)
- **Prompt SD** : `Game action card tile icon, side-swing heavy warhammer sweeping horizontally with an amber shockwave ring (#FF9A4D), motion arc, sumi-e ink wash painting on warm parchment paper ground (#EDE4D3), dark ink splatter, minimalist game UI icon`

![Marteau de Météore (Tetsubo) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_02_marteau_de_meteore_v2.png)

#### Variante 3 (seed 200002)
- **Fichier** : [tuile_02_marteau_de_meteore_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_02_marteau_de_meteore_v3.png)
- **Prompt SD** : `Game action card tile icon, overhead warhammer impact crater with radiating amber cracks (#FF9A4D) and ink debris, sumi-e ink wash painting on warm parchment paper ground (#EDE4D3), minimalist game UI icon design`

![Marteau de Météore (Tetsubo) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_02_marteau_de_meteore_v3.png)

---

### 3. Pointe du Méridien (Spear)
- **Type** : `Frappe perçante (Attaque à zone)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 5 (Tuile de base)`
- **Lore** : *Une aiguille d'encre effilée transperçant deux cases consécutives sur la ligne de boussole.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_03_pointe_du_meridien.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_03_pointe_du_meridien.png)
- **Prompt SD** : `Game action card tile icon, long sharp spear thrust piercing forward through two glowing ink nodes, sumi-e ink wash style on warm paper ground (#EDE4D3), luminous blade tip (#EAF2F8), dark brush strokes, game icon UI`

![Pointe du Méridien (Spear) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_03_pointe_du_meridien.png)

#### Variante 2 (seed 100003)
- **Fichier** : [tuile_03_pointe_du_meridien_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_03_pointe_du_meridien_v2.png)
- **Prompt SD** : `Game action card tile icon, spear thrust drawn in profile with motion streaks impaling two glowing ink nodes (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), dark brush strokes, game icon UI`

![Pointe du Méridien (Spear) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_03_pointe_du_meridien_v2.png)

#### Variante 3 (seed 200003)
- **Fichier** : [tuile_03_pointe_du_meridien_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_03_pointe_du_meridien_v3.png)
- **Prompt SD** : `Game action card tile icon, close-up of a glowing spear tip with a trailing ink ribbon and two pierced node markers, sumi-e ink wash style on warm paper ground (#EDE4D3), luminous blade tip (#EAF2F8), game icon UI`

![Pointe du Méridien (Spear) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_03_pointe_du_meridien_v3.png)

---

### 4. Vortex de Pinceau (Swirl)
- **Type** : `Frappe circulaire (Devant & Derrière)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 3 (Tuile de base)`
- **Lore** : *Une rotation de la plume libérant un halo d'encre devant et derrière soi simultanément.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_04_vortex_de_pinceau.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_04_vortex_de_pinceau.png)
- **Prompt SD** : `Game action card tile icon, circular 360-degree brush swirl of black ink and glowing blue starlight (#EAF2F8), sumi-e wash style on warm cream paper ground (#EDE4D3), elegant minimalist UI icon`

![Vortex de Pinceau (Swirl) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_04_vortex_de_pinceau.png)

#### Variante 2 (seed 100004)
- **Fichier** : [tuile_04_vortex_de_pinceau_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_04_vortex_de_pinceau_v2.png)
- **Prompt SD** : `Game action card tile icon, asymmetric double-arc brush swirl, larger front arc and smaller rear arc of black ink and glowing starlight (#EAF2F8), sumi-e wash style on warm cream paper ground (#EDE4D3), elegant minimalist UI icon`

![Vortex de Pinceau (Swirl) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_04_vortex_de_pinceau_v2.png)

#### Variante 3 (seed 200004)
- **Fichier** : [tuile_04_vortex_de_pinceau_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_04_vortex_de_pinceau_v3.png)
- **Prompt SD** : `Game action card tile icon, spiral galaxy-like ink swirl with orbiting stardust flecks (#EAF2F8), sumi-e wash style on warm cream paper ground (#EDE4D3), elegant minimalist UI icon`

![Vortex de Pinceau (Swirl) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_04_vortex_de_pinceau_v3.png)

---

### 5. Flèche d'Étoile (Shuriken / Arrow)
- **Type** : `Frappe à distance (Projectile)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 5 (Tuile de base)`
- **Lore** : *Un projectile de lumière stellaire taillé dans le verre de météore, frappant à distance.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_05_fleche_d_etoile.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_05_fleche_d_etoile.png)
- **Prompt SD** : `Game action card tile icon, glowing starlight arrow projectile flying through dark ink mist, sumi-e ink wash painting on warm paper ground (#EDE4D3), radiant blue-white core (#EAF2F8), floating amber embers, game icon`

![Flèche d'Étoile (Shuriken / Arrow) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_05_fleche_d_etoile.png)

#### Variante 2 (seed 100005)
- **Fichier** : [tuile_05_fleche_d_etoile_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_05_fleche_d_etoile_v2.png)
- **Prompt SD** : `Game action card tile icon, three glowing shuriken stars flying in sequence through dark ink mist (#EAF2F8), amber sparks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game icon`

![Flèche d'Étoile (Shuriken / Arrow) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_05_fleche_d_etoile_v2.png)

#### Variante 3 (seed 200005)
- **Fichier** : [tuile_05_fleche_d_etoile_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_05_fleche_d_etoile_v3.png)
- **Prompt SD** : `Game action card tile icon, single starlight arrow with feathered ink fletching and a long glowing trail (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), floating amber embers, game icon`

![Flèche d'Étoile (Shuriken / Arrow) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_05_fleche_d_etoile_v3.png)

---

### 6. Éclair du Firmament (Lightning)
- **Type** : `Frappe à distance éloignée`
- **Statistiques** : `Dégâts: 2 | Cooldown: 5 | Coût: 3 Crânes`
- **Lore** : *Un trait de foudre céleste frappant directement la cible la plus distante de la carte.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_06_eclair_du_firmament.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_06_eclair_du_firmament.png)
- **Prompt SD** : `Game action card tile icon, zig-zag lightning bolt of pure cyan starlight (#5FD8E8) striking down from an ink storm cloud, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game icon design`

![Éclair du Firmament (Lightning) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_06_eclair_du_firmament.png)

#### Variante 2 (seed 100006)
- **Fichier** : [tuile_06_eclair_du_firmament_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_06_eclair_du_firmament_v2.png)
- **Prompt SD** : `Game action card tile icon, forked twin cyan lightning bolts (#5FD8E8) striking down simultaneously from an ink storm cloud, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game icon design`

![Éclair du Firmament (Lightning) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_06_eclair_du_firmament_v2.png)

#### Variante 3 (seed 200006)
- **Fichier** : [tuile_06_eclair_du_firmament_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_06_eclair_du_firmament_v3.png)
- **Prompt SD** : `Game action card tile icon, crackling ball lightning sphere of cyan starlight (#5FD8E8) with radiating sparks, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game icon design`

![Éclair du Firmament (Lightning) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_06_eclair_du_firmament_v3.png)

---

### 7. Onde de Compas (Dragon Punch / Ki Push)
- **Type** : `Frappe à repoussement (Mobilité)`
- **Statistiques** : `Dégâts: 1 | Cooldown: 4 | Coût: 1 Crâne`
- **Lore** : *Une impulsion de force cartographique repoussant l'ennemi jusqu'au bord du parchemin.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_07_onde_de_compas.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_07_onde_de_compas.png)
- **Prompt SD** : `Game action card tile icon, glowing shockwave burst expanding outward with directional push arrow in ink, sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist clean UI icon design`

![Onde de Compas (Dragon Punch / Ki Push) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_07_onde_de_compas.png)

#### Variante 2 (seed 100007)
- **Fichier** : [tuile_07_onde_de_compas_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_07_onde_de_compas_v2.png)
- **Prompt SD** : `Game action card tile icon, forward glowing fist impact with concentric push rings and a directional knockback arrow (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist clean UI icon design`

![Onde de Compas (Dragon Punch / Ki Push) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_07_onde_de_compas_v2.png)

#### Variante 3 (seed 200007)
- **Fichier** : [tuile_07_onde_de_compas_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_07_onde_de_compas_v3.png)
- **Prompt SD** : `Game action card tile icon, open telekinetic palm emitting a directional gust of ink wind with push arrows, amber dust (#FF9A4D), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist clean UI icon design`

![Onde de Compas (Dragon Punch / Ki Push) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_07_onde_de_compas_v3.png)

---

### 8. Brouillard de Songe (Smoke Bomb)
- **Type** : `Téléportation / Transposition`
- **Statistiques** : `Dégâts: 1 | Cooldown: 5 | Coût: 1 Crâne`
- **Lore** : *Une bouffée d'encre et de brume transposant la position du Pèlerin avec celle de son adversaire.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_08_brouillard_de_songe.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_08_brouillard_de_songe.png)
- **Prompt SD** : `Game action card tile icon, swirling cloud of dense black ink smoke and white fog with two intersecting swap arrows, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon`

![Brouillard de Songe (Smoke Bomb) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_08_brouillard_de_songe.png)

#### Variante 2 (seed 100008)
- **Fichier** : [tuile_08_brouillard_de_songe_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_08_brouillard_de_songe_v2.png)
- **Prompt SD** : `Game action card tile icon, ink smoke bomb pellet bursting mid-air into swirling black and white fog with swap arrows, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon`

![Brouillard de Songe (Smoke Bomb) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_08_brouillard_de_songe_v2.png)

#### Variante 3 (seed 200008)
- **Fichier** : [tuile_08_brouillard_de_songe_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_08_brouillard_de_songe_v3.png)
- **Prompt SD** : `Game action card tile icon, two half-dissolved ink silhouettes swapping positions through dense fog, intersecting arrows (#EAF2F8), sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon`

![Brouillard de Songe (Smoke Bomb) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_08_brouillard_de_songe_v3.png)

---

### 9. Glissement d'Ombre (Shadow Dash)
- **Type** : `Traversée (Mobilité)`
- **Statistiques** : `Dégâts: 1 | Cooldown: 5 (Tuile de base)`
- **Lore** : *Le Pèlerin se dissout temporairement en encre liquide pour traverser les silhouettes adverses.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_09_glissement_d_ombre.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_09_glissement_d_ombre.png)
- **Prompt SD** : `Game action card tile icon, shadow silhouette dashing forward leaving a trail of wet black ink and glowing starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon`

![Glissement d'Ombre (Shadow Dash) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_09_glissement_d_ombre.png)

#### Variante 2 (seed 100009)
- **Fichier** : [tuile_09_glissement_d_ombre_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_09_glissement_d_ombre_v2.png)
- **Prompt SD** : `Game action card tile icon, multiple ghostly afterimage silhouettes strung along a forward dash path with wet black ink and starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon`

![Glissement d'Ombre (Shadow Dash) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_09_glissement_d_ombre_v2.png)

#### Variante 3 (seed 200009)
- **Fichier** : [tuile_09_glissement_d_ombre_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_09_glissement_d_ombre_v3.png)
- **Prompt SD** : `Game action card tile icon, top-down shadow dash with an ink comet trail streaking forward, glowing particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon`

![Glissement d'Ombre (Shadow Dash) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_09_glissement_d_ombre_v3.png)

---

### 10. Buvard de Malédiction (Curse - Free-Play)
- **Type** : `Altération (Action Gratuite)`
- **Statistiques** : `Dégâts: — | Cooldown: 7 | Coût: 1 Crâne | Free-Play`
- **Lore** : *Un buvard d'encre vermillon appliqué sans délai sur la cible, doublant les dégâts du prochain coup.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_10_buvard_de_malediction.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_10_buvard_de_malediction.png)
- **Prompt SD** : `Game action card tile icon, ominous vermillion red ink seal blot (#C8382E) with glowing cursed magic runes, sumi-e ink wash style on warm paper ground (#EDE4D3), striking red accent, minimalist game UI icon`

![Buvard de Malédiction (Curse - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_10_buvard_de_malediction.png)

#### Variante 2 (seed 100010)
- **Fichier** : [tuile_10_buvard_de_malediction_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_10_buvard_de_malediction_v2.png)
- **Prompt SD** : `Game action card tile icon, ominous vermillion cursed eye symbol (#C8382E) with glowing rune lashes, sumi-e ink wash style on warm paper ground (#EDE4D3), striking red accent, minimalist game UI icon`

![Buvard de Malédiction (Curse - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_10_buvard_de_malediction_v2.png)

#### Variante 3 (seed 200010)
- **Fichier** : [tuile_10_buvard_de_malediction_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_10_buvard_de_malediction_v3.png)
- **Prompt SD** : `Game action card tile icon, darkening vermillion handprint seal (#C8382E) oozing cursed ink, sumi-e ink wash style on warm paper ground (#EDE4D3), striking red accent, minimalist game UI icon`

![Buvard de Malédiction (Curse - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_10_buvard_de_malediction_v3.png)

---

### 11. Éclat de Météore (Blazing Suisei)
- **Type** : `Explosion sur coup fatal`
- **Statistiques** : `Dégâts: 2 | Cooldown: 4 | Coût: 30 Crânes`
- **Lore** : *Une étincelle d'étoile qui explose en gerbes d'ambre sur la case et ses adjacentes en cas de coup fatal.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_11_eclat_de_meteore.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_11_eclat_de_meteore.png)
- **Prompt SD** : `Game action card tile icon, falling meteor star bursting into an explosion of fiery amber embers (#FF9A4D) and ink flecks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game UI icon design`

![Éclat de Météore (Blazing Suisei) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_11_eclat_de_meteore.png)

#### Variante 2 (seed 100011)
- **Fichier** : [tuile_11_eclat_de_meteore_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_11_eclat_de_meteore_v2.png)
- **Prompt SD** : `Game action card tile icon, meteor shower of small star fragments raining amber embers (#FF9A4D) and ink flecks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game UI icon design`

![Éclat de Météore (Blazing Suisei) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_11_eclat_de_meteore_v2.png)

#### Variante 3 (seed 200011)
- **Fichier** : [tuile_11_eclat_de_meteore_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_11_eclat_de_meteore_v3.png)
- **Prompt SD** : `Game action card tile icon, single large blazing suisei sphere ringed by an amber shockwave on impact (#FF9A4D), ink flecks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game UI icon design`

![Éclat de Météore (Blazing Suisei) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_11_eclat_de_meteore_v3.png)

---

### 12. Lame de Patience (Blade of Patience)
- **Type** : `Attaque à charge temporelle`
- **Statistiques** : `Dégâts: 0 (+1 par tour en file) | Cooldown: 6 | Coût: 10 Crânes`
- **Lore** : *Une lame d'encre dont l'intensité lumineuse s'accroît à chaque tour passé dans la strophe.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_12_lame_de_patience.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_12_lame_de_patience.png)
- **Prompt SD** : `Game action card tile icon, elegant sword blade slowly accumulating brilliant white-blue radiant energy (#EAF2F8) in an hourglass ink aura, sumi-e wash style on warm paper ground (#EDE4D3), game UI icon`

![Lame de Patience (Blade of Patience) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_12_lame_de_patience.png)

#### Variante 2 (seed 100012)
- **Fichier** : [tuile_12_lame_de_patience_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_12_lame_de_patience_v2.png)
- **Prompt SD** : `Game action card tile icon, sword blade with three stacked glowing charge glyphs intensifying in brightness (#EAF2F8) inside an hourglass ink aura, sumi-e wash style on warm paper ground (#EDE4D3), game UI icon`

![Lame de Patience (Blade of Patience) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_12_lame_de_patience_v2.png)

#### Variante 3 (seed 200012)
- **Fichier** : [tuile_12_lame_de_patience_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_12_lame_de_patience_v3.png)
- **Prompt SD** : `Game action card tile icon, scabbarded blade quietly collecting ambient starlight into its guard (#EAF2F8), serene anticipation, sumi-e wash style on warm paper ground (#EDE4D3), game UI icon`

![Lame de Patience (Blade of Patience) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_12_lame_de_patience_v3.png)

---

### 13. Barrière de Ratures (Trap / Thorns)
- **Type** : `Piège au sol / Obstacle`
- **Statistiques** : `Dégâts: 3 | Cooldown: 4 | Coût: 6 Crânes`
- **Lore** : *Un piège de ratures d'encre vives déposé sur la carte, blessant quiconque s'y aventure.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_13_barriere_de_ratures.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_13_barriere_de_ratures.png)
- **Prompt SD** : `Game action card tile icon, sharp spiky ink thorn trap drawn on paper ground with red threat markings (#C8382E), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist UI icon`

![Barrière de Ratures (Trap / Thorns) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_13_barriere_de_ratures.png)

#### Variante 2 (seed 100013)
- **Fichier** : [tuile_13_barriere_de_ratures_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_13_barriere_de_ratures_v2.png)
- **Prompt SD** : `Game action card tile icon, subtle hidden trip-wire ink trap with a faint glowing trigger mark, red threat accent (#C8382E), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist UI icon`

![Barrière de Ratures (Trap / Thorns) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_13_barriere_de_ratures_v2.png)

#### Variante 3 (seed 200013)
- **Fichier** : [tuile_13_barriere_de_ratures_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_13_barriere_de_ratures_v3.png)
- **Prompt SD** : `Game action card tile icon, blooming ink thorn bush barrier with sharp crimson-tipped barbs (#C8382E), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist UI icon`

![Barrière de Ratures (Trap / Thorns) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_13_barriere_de_ratures_v3.png)

---

### 14. Sceau du Centre (Origin of Symmetry - Free-Play)
- **Type** : `Téléportation centrale (Action Gratuite)`
- **Statistiques** : `Dégâts: — | Cooldown: 6 | Coût: 30 Crânes | Free-Play`
- **Lore** : *Une téléportation sacrée ramenant le Pèlerin au centre exact de la grille du Cosmo-Atlas.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_14_sceau_du_centre.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_14_sceau_du_centre.png)
- **Prompt SD** : `Game action card tile icon, symmetrical compass rose icon drawn in gold and dark ink with glowing center star, sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon`

![Sceau du Centre (Origin of Symmetry - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_14_sceau_du_centre.png)

#### Variante 2 (seed 100014)
- **Fichier** : [tuile_14_sceau_du_centre_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_14_sceau_du_centre_v2.png)
- **Prompt SD** : `Game action card tile icon, eightfold symmetric mandala compass rose in gold and dark ink with a glowing center star (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon`

![Sceau du Centre (Origin of Symmetry - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_14_sceau_du_centre_v2.png)

#### Variante 3 (seed 200014)
- **Fichier** : [tuile_14_sceau_du_centre_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_14_sceau_du_centre_v3.png)
- **Prompt SD** : `Game action card tile icon, mirror-perfect left-right ink reflection forming a symmetric compass seal with a radiant core, sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon`

![Sceau du Centre (Origin of Symmetry - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_14_sceau_du_centre_v3.png)

---

### 15. Miroir de Brume (Mirror - Free-Play)
- **Type** : `Contre-téléportation (Action Gratuite)`
- **Statistiques** : `Dégâts: — | Cooldown: 6 | Coût: 20 Crânes | Free-Play`
- **Lore** : *Un reflet d'encre qui permute le Pèlerin directement sur la position de son agresseur.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_15_miroir_de_brume.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_15_miroir_de_brume.png)
- **Prompt SD** : `Game action card tile icon, two mirrored ink silhouettes swapping places through a glowing silver portal frame, sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist game UI icon`

![Miroir de Brume (Mirror - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_15_miroir_de_brume.png)

#### Variante 2 (seed 100015)
- **Fichier** : [tuile_15_miroir_de_brume_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_15_miroir_de_brume_v2.png)
- **Prompt SD** : `Game action card tile icon, polished ink mirror surface with a single hand reaching through a silver portal frame (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist game UI icon`

![Miroir de Brume (Mirror - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_15_miroir_de_brume_v2.png)

#### Variante 3 (seed 200015)
- **Fichier** : [tuile_15_miroir_de_brume_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_15_miroir_de_brume_v3.png)
- **Prompt SD** : `Game action card tile icon, round bronze mirror gleaming with a starlight reflection and swap arrows (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist game UI icon`

![Miroir de Brume (Mirror - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_15_miroir_de_brume_v3.png)

---

### 16. Disque de Poussière d'Étoile (Chakram)
- **Type** : `Attaque à accumulation de victimes`
- **Statistiques** : `Dégâts: 0 (+1 par ennemi vaincu) | Cooldown: 7 | Coût: 30 Crânes`
- **Lore** : *Un cercle d'encre rotatif qui accumule la poussière dorée des étoiles à chaque ombre terrassée.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_16_disque_de_poussiere.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_16_disque_de_poussiere.png)
- **Prompt SD** : `Game action card tile icon, spinning circular chakram ring of glowing stardust motes and ink trails, sumi-e wash style on warm parchment paper (#EDE4D3), elegant UI icon`

![Disque de Poussière d'Étoile (Chakram) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_16_disque_de_poussiere.png)

#### Variante 2 (seed 100016)
- **Fichier** : [tuile_16_disque_de_poussiere_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_16_disque_de_poussiere_v2.png)
- **Prompt SD** : `Game action card tile icon, three concentric spinning chakram rings of glowing stardust motes and ink trails (#EAF2F8), sumi-e wash style on warm parchment paper (#EDE4D3), elegant UI icon`

![Disque de Poussière d'Étoile (Chakram) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_16_disque_de_poussiere_v2.png)

#### Variante 3 (seed 200016)
- **Fichier** : [tuile_16_disque_de_poussiere_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_16_disque_de_poussiere_v3.png)
- **Prompt SD** : `Game action card tile icon, single chakram ring etched with tally marks counting defeated silhouettes, glowing stardust (#EAF2F8), sumi-e wash style on warm parchment paper (#EDE4D3), elegant UI icon`

![Disque de Poussière d'Étoile (Chakram) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_16_disque_de_poussiere_v3.png)

---

### 17. Fil d'Encre (Grappling Hook)
- **Type** : `Traction d'ennemi à distance`
- **Statistiques** : `Dégâts: 1 | Cooldown: 4 | Coût: 3 Crânes`
- **Lore** : *Un fil de soie étoilée agrippant une ombre à distance pour la ramener au contact.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_17_fil_d_encre.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_17_fil_d_encre.png)
- **Prompt SD** : `Game action card tile icon, fine glowing thread of starlight silk pulling a dark target icon closer, sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist clean UI icon`

![Fil d'Encre (Grappling Hook) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_17_fil_d_encre.png)

#### Variante 2 (seed 100017)
- **Fichier** : [tuile_17_fil_d_encre_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_17_fil_d_encre_v2.png)
- **Prompt SD** : `Game action card tile icon, hooked starlight thread anchored into a dark target icon with a taut pulling line (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist clean UI icon`

![Fil d'Encre (Grappling Hook) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_17_fil_d_encre_v2.png)

#### Variante 3 (seed 200017)
- **Fichier** : [tuile_17_fil_d_encre_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_17_fil_d_encre_v3.png)
- **Prompt SD** : `Game action card tile icon, web of fine starlight silk threads pulling several target icons closer at once (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist clean UI icon`

![Fil d'Encre (Grappling Hook) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_17_fil_d_encre_v3.png)

---

### 18. Canon de Starlight (Tanegashima)
- **Type** : `Attaque lourde avec recul mutuel`
- **Statistiques** : `Dégâts: 4 | Cooldown: 7 | Coût: 25 Crânes`
- **Lore** : *Un souffle de puissance céleste propulsant la cible et le Pèlerin dans des directions opposées.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_18_canon_de_starlight.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_18_canon_de_starlight.png)
- **Prompt SD** : `Game action card tile icon, heavy celestial cannon blast with recoil shockwave in black ink and bright starlight burst, sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon design`

![Canon de Starlight (Tanegashima) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_18_canon_de_starlight.png)

#### Variante 2 (seed 100018)
- **Fichier** : [tuile_18_canon_de_starlight_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_18_canon_de_starlight_v2.png)
- **Prompt SD** : `Game action card tile icon, side profile of an ink matchlock pistol firing with an ignition spark and starlight flash (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon design`

![Canon de Starlight (Tanegashima) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_18_canon_de_starlight_v2.png)

#### Variante 3 (seed 200018)
- **Fichier** : [tuile_18_canon_de_starlight_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_18_canon_de_starlight_v3.png)
- **Prompt SD** : `Game action card tile icon, cannon recoil blast pushing attacker and target silhouettes apart with opposite arrows (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon design`

![Canon de Starlight (Tanegashima) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_18_canon_de_starlight_v3.png)

---

### 19. Crocs de la Nuit (Sai)
- **Type** : `Frappe réactive (Dégâts doublés si la cible s'apprête à attaquer)`
- **Statistiques** : `Dégâts: 2 (x2 si attaque ennemie) | Cooldown: 5 | Coût: 15 Crânes`
- **Lore** : *Deux dards d'encre parallèles qui résonnent et s'embrasent face à une intention hostile.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_19_crocs_de_la_nuit.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_19_crocs_de_la_nuit.png)
- **Prompt SD** : `Game action card tile icon, twin-pronged sai dagger drawn in dark ink with glowing blue-white counter-stance energy (#EAF2F8), reactive parry burst radiating outward, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design`

![Crocs de la Nuit (Sai) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_19_crocs_de_la_nuit.png)

#### Variante 2 (seed 100019)
- **Fichier** : [tuile_19_crocs_de_la_nuit_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_19_crocs_de_la_nuit_v2.png)
- **Prompt SD** : `Game action card tile icon, two sai daggers crossed in a parry catching an incoming glowing blade, counter-burst (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design`

![Crocs de la Nuit (Sai) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_19_crocs_de_la_nuit_v2.png)

#### Variante 3 (seed 200019)
- **Fichier** : [tuile_19_crocs_de_la_nuit_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_19_crocs_de_la_nuit_v3.png)
- **Prompt SD** : `Game action card tile icon, single raised sai radiating a ready counter-stance aura of blue-white energy (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design`

![Crocs de la Nuit (Sai) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_19_crocs_de_la_nuit_v3.png)

---

### 20. Faucille Récurrente (Hookblade)
- **Type** : `Frappe en chaîne (Refrappe sur coup fatal)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 5 | Coût: 30 Crânes`
- **Lore** : *Une lame courbe d'étoile qui, après avoir fauché son ombre, se projette en avant pour une seconde entaille.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_20_faucille_recurrente.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_20_faucille_recurrente.png)
- **Prompt SD** : `Game action card tile icon, curved hook blade with glowing starlight edge (#EAF2F8) chaining into a forward follow-up slash arrow, sumi-e ink wash painting on warm paper ground (#EDE4D3), elegant game UI icon`

![Faucille Récurrente (Hookblade) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_20_faucille_recurrente.png)

#### Variante 2 (seed 100020)
- **Fichier** : [tuile_20_faucille_recurrente_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_20_faucille_recurrente_v2.png)
- **Prompt SD** : `Game action card tile icon, curved hook blade mid-refollow chain linking two targets with a forward arrow (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), elegant game UI icon`

![Faucille Récurrente (Hookblade) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_20_faucille_recurrente_v2.png)

#### Variante 3 (seed 200020)
- **Fichier** : [tuile_20_faucille_recurrente_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_20_faucille_recurrente_v3.png)
- **Prompt SD** : `Game action card tile icon, curved hook blade dripping starlight along its inner edge after a fatal pull (#EAF2F8), amber accent (#FF9A4D), sumi-e ink wash painting on warm paper ground (#EDE4D3), elegant game UI icon`

![Faucille Récurrente (Hookblade) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_20_faucille_recurrente_v3.png)

---

### 21. Bâton du Renversement (Bo)
- **Type** : `Frappe retournante (Retourne la cible)`
- **Statistiques** : `Dégâts: 1 | Cooldown: 5 | Coût: 25 Crânes`
- **Lore** : *Un long bâton d'encre qui bascule la silhouette adverse pour l'exposer au plein jour.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_21_baton_du_renversement.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_21_baton_du_renversement.png)
- **Prompt SD** : `Game action card tile icon, long wooden bo staff sweeping in ink arc with a circular flip-turn arrow icon, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Bâton du Renversement (Bo) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_21_baton_du_renversement.png)

#### Variante 2 (seed 100021)
- **Fichier** : [tuile_21_baton_du_renversement_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_21_baton_du_renversement_v2.png)
- **Prompt SD** : `Game action card tile icon, bo staff spinning vertically to flip an enemy silhouette end over end, circular flip arrow, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Bâton du Renversement (Bo) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_21_baton_du_renversement_v2.png)

#### Variante 3 (seed 200021)
- **Fichier** : [tuile_21_baton_du_renversement_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_21_baton_du_renversement_v3.png)
- **Prompt SD** : `Game action card tile icon, low sweeping bo thrust with a rotation turn arrow and dust kick, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Bâton du Renversement (Bo) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_21_baton_du_renversement_v3.png)

---

### 22. Revers Étoilé (Back Strike)
- **Type** : `Frappe vers l'arrière (Derrière soi)`
- **Statistiques** : `Dégâts: 3 | Cooldown: 3 | Coût: 15 Crânes`
- **Lore** : *Un coup porté à rebours, traçant un sillage lumineux dans l'encre derrière le Pèlerin.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_22_revers_etoile.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_22_revers_etoile.png)
- **Prompt SD** : `Game action card tile icon, glowing sword slash striking backward with reverse directional arrow in luminous blue-white ink (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist UI icon`

![Revers Étoilé (Back Strike) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_22_revers_etoile.png)

#### Variante 2 (seed 100022)
- **Fichier** : [tuile_22_revers_etoile_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_22_revers_etoile_v2.png)
- **Prompt SD** : `Game action card tile icon, over-the-shoulder backstab silhouette thrusting a glowing blade backward (#EAF2F8), reverse arrow, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist UI icon`

![Revers Étoilé (Back Strike) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_22_revers_etoile_v2.png)

#### Variante 3 (seed 200022)
- **Fichier** : [tuile_22_revers_etoile_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_22_revers_etoile_v3.png)
- **Prompt SD** : `Game action card tile icon, rear blade gleam glinting behind a cloaked figure, luminous reverse slash (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist UI icon`

![Revers Étoilé (Back Strike) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_22_revers_etoile_v3.png)

---

### 23. Éventails Jumeaux (Twin Tessen)
- **Type** : `Frappe frontale et arrière avec repoussement`
- **Statistiques** : `Dégâts: 1 | Cooldown: 6 | Coût: 20 Crânes`
- **Lore** : *Deux éventails de guerre en papier étoilé qui repoussent les ombres de part et d'autre du Pèlerin.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_23_eventails_jumeaux.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_23_eventails_jumeaux.png)
- **Prompt SD** : `Game action card tile icon, pair of folding war fans painted in ink pushing outward in both directions with amber shockwave gusts (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant UI icon`

![Éventails Jumeaux (Twin Tessen) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_23_eventails_jumeaux.png)

#### Variante 2 (seed 100023)
- **Fichier** : [tuile_23_eventails_jumeaux_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_23_eventails_jumeaux_v2.png)
- **Prompt SD** : `Game action card tile icon, pair of war fans opening wide in opposite directions with amber gust arrows (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant UI icon`

![Éventails Jumeaux (Twin Tessen) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_23_eventails_jumeaux_v2.png)

#### Variante 3 (seed 200023)
- **Fichier** : [tuile_23_eventails_jumeaux_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_23_eventails_jumeaux_v3.png)
- **Prompt SD** : `Game action card tile icon, single folding war fan emphasized with razor steel ribs and a sharp slash arc (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant UI icon`

![Éventails Jumeaux (Twin Tessen) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_23_eventails_jumeaux_v3.png)

---

### 24. Faux d'Ombre (Shadow Kama)
- **Type** : `Frappe à distance 2 (Ignore la case intermédiaire)`
- **Statistiques** : `Dégâts: 3 | Cooldown: 3 | Coût: 6 Crânes`
- **Lore** : *Une faucille d'encre fantôme qui fauche une case lointaine sans effleurer l'espace intermédiaire.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_24_faux_d_ombre.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_24_faux_d_ombre.png)
- **Prompt SD** : `Game action card tile icon, ghostly kama sickle reaching a far glowing node two tiles ahead while ignoring the middle tile, dashed phantom ink line, sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Faux d'Ombre (Shadow Kama) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_24_faux_d_ombre.png)

#### Variante 2 (seed 100024)
- **Fichier** : [tuile_24_faux_d_ombre_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_24_faux_d_ombre_v2.png)
- **Prompt SD** : `Game action card tile icon, phantom kama reaching across a wide gap along a dashed arc to a far node (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Faux d'Ombre (Shadow Kama) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_24_faux_d_ombre_v2.png)

#### Variante 3 (seed 200024)
- **Fichier** : [tuile_24_faux_d_ombre_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_24_faux_d_ombre_v3.png)
- **Prompt SD** : `Game action card tile icon, twin phantom kama crossing at distance over an ignored middle tile, dashed lines (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Faux d'Ombre (Shadow Kama) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_24_faux_d_ombre_v3.png)

---

### 25. Pilier de Brume (Nagiboku)
- **Type** : `Frappe perçante 4 cases (Laisse toujours 1 PV)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 5 | Coût: 30 Crânes`
- **Lore** : *Un long pieu d'encre qui traverse quatre cases sans jamais porter le coup de grâce, épargnant une étincelle de vie.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_25_pilier_de_brume.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_25_pilier_de_brume.png)
- **Prompt SD** : `Game action card tile icon, long ink staff piercing four nodes front and back with soft restraint halo, blue-white life spark remaining (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon`

![Pilier de Brume (Nagiboku) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_25_pilier_de_brume.png)

#### Variante 2 (seed 100025)
- **Fichier** : [tuile_25_pilier_de_brume_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_25_pilier_de_brume_v2.png)
- **Prompt SD** : `Game action card tile icon, long ink pole piercing four nodes front and back wrapped in a soft protective wisp (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon`

![Pilier de Brume (Nagiboku) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_25_pilier_de_brume_v2.png)

#### Variante 3 (seed 200025)
- **Fichier** : [tuile_25_pilier_de_brume_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_25_pilier_de_brume_v3.png)
- **Prompt SD** : `Game action card tile icon, merciful staff stopping short with a glowing spared 1-HP spark icon at the tip (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon`

![Pilier de Brume (Nagiboku) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_25_pilier_de_brume_v3.png)

---

### 26. Empalement Terrestre (Earth Impale)
- **Type** : `Frappe à distance 2 devant et derrière`
- **Statistiques** : `Dégâts: 2 | Cooldown: 4 | Coût: 20 Crânes`
- **Lore** : *Des pieux d'encre jaillissant du parchemin à distance de part et d'autre, transperçant les cases éloignées.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_26_empalement_terrestre.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_26_empalement_terrestre.png)
- **Prompt SD** : `Game action card tile icon, ink stalagmite spikes erupting from paper ground at distance two front and back, glowing blue-white tips (#EAF2F8), sumi-e ink wash painting on warm parchment (#EDE4D3), game UI icon`

![Empalement Terrestre (Earth Impale) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_26_empalement_terrestre.png)

#### Variante 2 (seed 100026)
- **Fichier** : [tuile_26_empalement_terrestre_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_26_empalement_terrestre_v2.png)
- **Prompt SD** : `Game action card tile icon, ink stalagmite spikes erupting diagonally at distance two front and back (#EAF2F8), sumi-e ink wash painting on warm parchment (#EDE4D3), game UI icon`

![Empalement Terrestre (Earth Impale) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_26_empalement_terrestre_v2.png)

#### Variante 3 (seed 200026)
- **Fichier** : [tuile_26_empalement_terrestre_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_26_empalement_terrestre_v3.png)
- **Prompt SD** : `Game action card tile icon, earthen pillar impale with billowing ink dust clouds at two distance markers (#EAF2F8), sumi-e ink wash painting on warm parchment (#EDE4D3), game UI icon`

![Empalement Terrestre (Earth Impale) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_26_empalement_terrestre_v3.png)

---

### 27. Marteau Cometique (Meteor Hammer)
- **Type** : `Frappe à portée 3 puis rebond arrière`
- **Statistiques** : `Dégâts: 2 | Cooldown: 5 | Coût: 25 Crânes`
- **Lore** : *Un orbe d'encre céleste qui frappe au loin puis rebondit pour filer dans l'ombre derrière le Pèlerin.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_27_marteau_cometique.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_27_marteau_cometique.png)
- **Prompt SD** : `Game action card tile icon, spiked sphere on ink chain striking forward then curving back behind with amber motion trail (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon`

![Marteau Cometique (Meteor Hammer) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_27_marteau_cometique.png)

#### Variante 2 (seed 100027)
- **Fichier** : [tuile_27_marteau_cometique_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_27_marteau_cometique_v2.png)
- **Prompt SD** : `Game action card tile icon, spiked sphere mid-bounce arcing behind the user on an ink chain, amber trail (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon`

![Marteau Cometique (Meteor Hammer) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_27_marteau_cometique_v2.png)

#### Variante 3 (seed 200027)
- **Fichier** : [tuile_27_marteau_cometique_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_27_marteau_cometique_v3.png)
- **Prompt SD** : `Game action card tile icon, chain pulled taut with the heavy sphere at maximum forward reach, ink tension lines, amber flash (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon`

![Marteau Cometique (Meteor Hammer) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_27_marteau_cometique_v3.png)

---

### 28. Volée de Kunai (Kunai)
- **Type** : `Frappe à projectiles multiples (1 dégât chacun)`
- **Statistiques** : `Dégâts: 2 | Cooldown: 7 | Coût: 20 Crânes`
- **Lore** : *Une volée de dards de verre étoilé lacérant la première ombre, chacun ne portant qu'une éraflure légère.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_28_volee_de_kunai.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_28_volee_de_kunai.png)
- **Prompt SD** : `Game action card tile icon, flurry of three to five small throwing knives of glowing blue-white glass (#EAF2F8) converging on a front target, sumi-e ink wash style on warm parchment paper (#EDE4D3), minimalist game UI icon`

![Volée de Kunai (Kunai) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_28_volee_de_kunai.png)

#### Variante 2 (seed 100028)
- **Fichier** : [tuile_28_volee_de_kunai_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_28_volee_de_kunai_v2.png)
- **Prompt SD** : `Game action card tile icon, fan-spread of kunai thrown in a widening arc of glowing blue-white glass (#EAF2F8), sumi-e ink wash style on warm parchment paper (#EDE4D3), minimalist game UI icon`

![Volée de Kunai (Kunai) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_28_volee_de_kunai_v2.png)

#### Variante 3 (seed 200028)
- **Fichier** : [tuile_28_volee_de_kunai_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_28_volee_de_kunai_v3.png)
- **Prompt SD** : `Game action card tile icon, several kunai embedded in a single front target with glowing hit counts, blue-white glass (#EAF2F8), sumi-e ink wash style on warm parchment paper (#EDE4D3), minimalist game UI icon`

![Volée de Kunai (Kunai) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_28_volee_de_kunai_v3.png)

---

### 29. Pièce du Serment (Mon)
- **Type** : `Frappe lourde à consommation d'or`
- **Statistiques** : `Dégâts: 5 | Cooldown: 7 | Coût: 20 Crânes (dépense 1 pièce)`
- **Lore** : *Une pièce d'or gravée d'un sceau étoilé, sacrifiée au Cosmo-Atlas pour déchaîner une frappe dévastatrice.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_29_piece_du_serment.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_29_piece_du_serment.png)
- **Prompt SD** : `Game action card tile icon, glowing golden coin seal with star sigil converting into a heavy radiant strike beam (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Pièce du Serment (Mon) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_29_piece_du_serment.png)

#### Variante 2 (seed 100029)
- **Fichier** : [tuile_29_piece_du_serment_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_29_piece_du_serment_v2.png)
- **Prompt SD** : `Game action card tile icon, golden coin spinning with a light trail before converting into a radiant strike beam (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Pièce du Serment (Mon) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_29_piece_du_serment_v2.png)

#### Variante 3 (seed 200029)
- **Fichier** : [tuile_29_piece_du_serment_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_29_piece_du_serment_v3.png)
- **Prompt SD** : `Game action card tile icon, stack of three golden coins fueling a larger amplified strike beam, star sigil (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Pièce du Serment (Mon) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_29_piece_du_serment_v3.png)

---

### 30. Arbalète d'Étoiles (Crossbow)
- **Type** : `Projectile perçant 2 cibles (Se recharge en réutilisant)`
- **Statistiques** : `Dégâts: 3 | Cooldown: 5 | Coût: 15 Crânes`
- **Lore** : *Un carreau d'encre perçant deux ombres alignées, dont la corde se retend à chaque tir répété.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_30_arbalete_d_etoiles.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_30_arbalete_d_etoiles.png)
- **Prompt SD** : `Game action card tile icon, ink crossbow firing a piercing bolt through two aligned glowing target nodes (#EAF2F8), taut bowstring of starlight, sumi-e ink wash style on warm parchment ground (#EDE4D3), game UI icon`

![Arbalète d'Étoiles (Crossbow) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_30_arbalete_d_etoiles.png)

#### Variante 2 (seed 100030)
- **Fichier** : [tuile_30_arbalete_d_etoiles_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_30_arbalete_d_etoiles_v2.png)
- **Prompt SD** : `Game action card tile icon, crossbow mid-reload with a glowing taut bowstring of starlight (#EAF2F8), ink bolts, sumi-e ink wash style on warm parchment ground (#EDE4D3), game UI icon`

![Arbalète d'Étoiles (Crossbow) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_30_arbalete_d_etoiles_v2.png)

#### Variante 3 (seed 200030)
- **Fichier** : [tuile_30_arbalete_d_etoiles_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_30_arbalete_d_etoiles_v3.png)
- **Prompt SD** : `Game action card tile icon, heavy bolt mid-flight piercing two aligned silhouettes with a starlight trail (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), game UI icon`

![Arbalète d'Étoiles (Crossbow) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_30_arbalete_d_etoiles_v3.png)

---

### 31. Charge du Pèlerin (Charge)
- **Type** : `Déplacement + Frappe avant`
- **Statistiques** : `Dégâts: 1 | Cooldown: 4 (Tuile de base)`
- **Lore** : *Une ruée d'encre emportant le Pèlerin jusqu'à la première ombre pour l'ébranler.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_31_charge_du_pelerin.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_31_charge_du_pelerin.png)
- **Prompt SD** : `Game action card tile icon, forward dashing ink silhouette with motion blur trail striking the first target with amber impact burst (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Charge du Pèlerin (Charge) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_31_charge_du_pelerin.png)

#### Variante 2 (seed 100031)
- **Fichier** : [tuile_31_charge_du_pelerin_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_31_charge_du_pelerin_v2.png)
- **Prompt SD** : `Game action card tile icon, forward charge leaving a comet of ink with a braced shoulder into the first target, amber impact (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Charge du Pèlerin (Charge) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_31_charge_du_pelerin_v2.png)

#### Variante 3 (seed 200031)
- **Fichier** : [tuile_31_charge_du_pelerin_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_31_charge_du_pelerin_v3.png)
- **Prompt SD** : `Game action card tile icon, dashing ink silhouette lowering its shoulder to ram the first target, motion blur, amber burst (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Charge du Pèlerin (Charge) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_31_charge_du_pelerin_v3.png)

---

### 32. Charge Versatile (Back Charge)
- **Type** : `Déplacement + Frappe arrière`
- **Statistiques** : `Dégâts: 1 | Cooldown: 3 (Tuile de base)`
- **Lore** : *Une ruée inversée, le Pèlerin fonçant dans son sillage d'encre pour heurter l'ombre qui le suit.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_32_charge_versatile.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_32_charge_versatile.png)
- **Prompt SD** : `Game action card tile icon, ink silhouette dashing backward with reverse motion trail striking a rear target, amber impact (#FF9A4D), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon`

![Charge Versatile (Back Charge) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_32_charge_versatile.png)

#### Variante 2 (seed 100032)
- **Fichier** : [tuile_32_charge_versatile_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_32_charge_versatile_v2.png)
- **Prompt SD** : `Game action card tile icon, ink silhouette rushing backward with a glance over the shoulder and a reverse motion trail, amber impact (#FF9A4D), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon`

![Charge Versatile (Back Charge) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_32_charge_versatile_v2.png)

#### Variante 3 (seed 200032)
- **Fichier** : [tuile_32_charge_versatile_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_32_charge_versatile_v3.png)
- **Prompt SD** : `Game action card tile icon, backward charge kicking up an ink dust plume on the way to a rear target, amber impact (#FF9A4D), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon`

![Charge Versatile (Back Charge) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_32_charge_versatile_v3.png)

---

### 33. Glissement Postérieur (Back Shadow Dash)
- **Type** : `Traversée arrière (Mobilité)`
- **Statistiques** : `Dégâts: 1 | Cooldown: 5 | Coût: 20 Crânes`
- **Lore** : *Le Pèlerin se dissout en encre pour traverser les ombres qui le talonnent par-derrière.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_33_glissement_posterieur.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_33_glissement_posterieur.png)
- **Prompt SD** : `Game action card tile icon, shadow silhouette dashing backward leaving wet black ink trail and glowing starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon`

![Glissement Postérieur (Back Shadow Dash) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_33_glissement_posterieur.png)

#### Variante 2 (seed 100033)
- **Fichier** : [tuile_33_glissement_posterieur_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_33_glissement_posterieur_v2.png)
- **Prompt SD** : `Game action card tile icon, ghostly afterimages trailing backward through rear enemies with wet ink and starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon`

![Glissement Postérieur (Back Shadow Dash) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_33_glissement_posterieur_v2.png)

#### Variante 3 (seed 200033)
- **Fichier** : [tuile_33_glissement_posterieur_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_33_glissement_posterieur_v3.png)
- **Prompt SD** : `Game action card tile icon, silhouette dissolving into an ink pool to reappear behind a rear foe, starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon`

![Glissement Postérieur (Back Shadow Dash) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_33_glissement_posterieur_v3.png)

---

### 34. Brouillard Arrière (Back Smoke Bomb)
- **Type** : `Transposition arrière`
- **Statistiques** : `Dégâts: 1 | Cooldown: 5 | Coût: 25 Crânes`
- **Lore** : *Une bouffée d'encre et de brume transposant le Pèlerin avec l'ombre qui le suit.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_34_brouillard_arriere.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_34_brouillard_arriere.png)
- **Prompt SD** : `Game action card tile icon, swirling ink smoke cloud with two intersecting swap arrows pointing backward, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon`

![Brouillard Arrière (Back Smoke Bomb) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_34_brouillard_arriere.png)

#### Variante 2 (seed 100034)
- **Fichier** : [tuile_34_brouillard_arriere_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_34_brouillard_arriere_v2.png)
- **Prompt SD** : `Game action card tile icon, ink smoke pellet rolling behind before bursting into a swap cloud, backward arrows, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon`

![Brouillard Arrière (Back Smoke Bomb) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_34_brouillard_arriere_v2.png)

#### Variante 3 (seed 200034)
- **Fichier** : [tuile_34_brouillard_arriere_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_34_brouillard_arriere_v3.png)
- **Prompt SD** : `Game action card tile icon, fog tendrils wrapping two silhouettes swapping rear positions (#EAF2F8), sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon`

![Brouillard Arrière (Back Smoke Bomb) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_34_brouillard_arriere_v3.png)

---

### 35. Pivot Tranchant (Sharp Turn - Free-Play)
- **Type** : `Rotation + Frappe autour (Action Gratuite)`
- **Statistiques** : `Dégâts: 1 | Cooldown: 7 | Coût: 3 Crânes | Free-Play`
- **Lore** : *Une pirouette de plume pivotant le Pèlerin et déployant un cercle d'encre sur les cases adjacentes.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_35_pivot_tranchant.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_35_pivot_tranchant.png)
- **Prompt SD** : `Game action card tile icon, spinning ink brush turn with radial slash marks around the center silhouette, luminous blue-white arcs (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon`

![Pivot Tranchant (Sharp Turn - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_35_pivot_tranchant.png)

#### Variante 2 (seed 100035)
- **Fichier** : [tuile_35_pivot_tranchant_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_35_pivot_tranchant_v2.png)
- **Prompt SD** : `Game action card tile icon, 180-degree pivot slash with centrifugal ink arcs around the center silhouette, luminous blue-white (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon`

![Pivot Tranchant (Sharp Turn - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_35_pivot_tranchant_v2.png)

#### Variante 3 (seed 200035)
- **Fichier** : [tuile_35_pivot_tranchant_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_35_pivot_tranchant_v3.png)
- **Prompt SD** : `Game action card tile icon, turning heel-stomp creating a radial shock of ink slashes around the pivot point, blue-white arcs (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon`

![Pivot Tranchant (Sharp Turn - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_35_pivot_tranchant_v3.png)

---

### 36. Sceau du Héros (Signature Move - Free-Play)
- **Type** : `Capacité spéciale du héros (Action Gratuite)`
- **Statistiques** : `Dégâts: — | Cooldown: 6 | Coût: 25 Crânes | Free-Play`
- **Lore** : *Le glyphe personnel du Pèlerin, déchaînant son don unique en une calligraphie éphémère.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_36_sceau_du_heros.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_36_sceau_du_heros.png)
- **Prompt SD** : `Game action card tile icon, radiant personal hero sigil of glowing starlight calligraphy (#EAF2F8) with amber spark accents (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), elegant minimalist game UI icon`

![Sceau du Héros (Signature Move - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_36_sceau_du_heros.png)

#### Variante 2 (seed 100036)
- **Fichier** : [tuile_36_sceau_du_heros_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_36_sceau_du_heros_v2.png)
- **Prompt SD** : `Game action card tile icon, hero sigil unfolding like a blooming starlight seal with amber spark accents (#EAF2F8, #FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), elegant minimalist game UI icon`

![Sceau du Héros (Signature Move - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_36_sceau_du_heros_v2.png)

#### Variante 3 (seed 200036)
- **Fichier** : [tuile_36_sceau_du_heros_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_36_sceau_du_heros_v3.png)
- **Prompt SD** : `Game action card tile icon, calligraphy brush painting the hero's special sigil in real time with glowing wet strokes (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), elegant minimalist game UI icon`

![Sceau du Héros (Signature Move - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_36_sceau_du_heros_v3.png)

---

### 37. Tiroir de Songe (Swap Toss - Free-Play)
- **Type** : `Échange des cases avant/arrière (Action Gratuite)`
- **Statistiques** : `Dégâts: — | Cooldown: 7 | Coût: 25 Crânes | Free-Play`
- **Lore** : *Un geste de plume intervertissant les ombres des cases avant et arrière, comme on retourne un sablier.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_37_tiroir_de_songe.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_37_tiroir_de_songe.png)
- **Prompt SD** : `Game action card tile icon, two glowing ink nodes front and back swapping contents through crossing arrows, hourglass motif of starlight (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Tiroir de Songe (Swap Toss - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_37_tiroir_de_songe.png)

#### Variante 2 (seed 100037)
- **Fichier** : [tuile_37_tiroir_de_songe_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_37_tiroir_de_songe_v2.png)
- **Prompt SD** : `Game action card tile icon, hourglass of starlight with two glowing icons falling through and swapping front/back (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Tiroir de Songe (Swap Toss - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_37_tiroir_de_songe_v2.png)

#### Variante 3 (seed 200037)
- **Fichier** : [tuile_37_tiroir_de_songe_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_37_tiroir_de_songe_v3.png)
- **Prompt SD** : `Game action card tile icon, two mirrored cabinets exchanging their front and back contents through crossing arrows (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon`

![Tiroir de Songe (Swap Toss - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_37_tiroir_de_songe_v3.png)

---

### 38. Flèche du Vide (Dash - Free-Play)
- **Type** : `Déplacement maximal avant (Action Gratuite)`
- **Statistiques** : `Dégâts: — | Cooldown: 6 | Coût: 10 Crânes | Free-Play`
- **Lore** : *Une traînée d'encre pure propulsant le Pèlerin jusqu'au seuil de la case la plus lointaine.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_38_fleche_du_vide.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_38_fleche_du_vide.png)
- **Prompt SD** : `Game action card tile icon, elongated ink dash trail with starlight comet streak (#EAF2F8) rushing forward across multiple tiles, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon`

![Flèche du Vide (Dash - Free-Play) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_38_fleche_du_vide.png)

#### Variante 2 (seed 100038)
- **Fichier** : [tuile_38_fleche_du_vide_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_38_fleche_du_vide_v2.png)
- **Prompt SD** : `Game action card tile icon, long horizontal ink dash streak with start and end markers and a starlight comet tail (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon`

![Flèche du Vide (Dash - Free-Play) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_38_fleche_du_vide_v2.png)

#### Variante 3 (seed 200038)
- **Fichier** : [tuile_38_fleche_du_vide_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_38_fleche_du_vide_v3.png)
- **Prompt SD** : `Game action card tile icon, forward dash trail arcing slightly upward like a skipped stone of ink, starlight comet streak (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon`

![Flèche du Vide (Dash - Free-Play) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_38_fleche_du_vide_v3.png)

---

### 39. Cicatrice Stellaire (Scar Strike)
- **Type** : `Frappe toutes les cibles blessées (PV non pleins)`
- **Statistiques** : `Dégâts: 1 | Cooldown: 5 | Coût: 10 Crânes`
- **Lore** : *Une entaille d'encre qui ne s'ouvre que sur les ombres déjà meurtries, recherchant leurs failles.*

#### Variante 1 — canonique (seed 2329875676)
- **Fichier** : [tuile_39_cicatrice_stellaire.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_39_cicatrice_stellaire.png)
- **Prompt SD** : `Game action card tile icon, jagged scar slash marks targeting wounded cracked ink silhouettes, vermillion wound accents (#C8382E), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Cicatrice Stellaire (Scar Strike) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_39_cicatrice_stellaire.png)

#### Variante 2 (seed 100039)
- **Fichier** : [tuile_39_cicatrice_stellaire_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_39_cicatrice_stellaire_v2.png)
- **Prompt SD** : `Game action card tile icon, jagged scar slashes opening only across cracked-wound silhouettes, vermillion accents (#C8382E), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Cicatrice Stellaire (Scar Strike) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_39_cicatrice_stellaire_v2.png)

#### Variante 3 (seed 200039)
- **Fichier** : [tuile_39_cicatrice_stellaire_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_39_cicatrice_stellaire_v3.png)
- **Prompt SD** : `Game action card tile icon, single seeking jagged scar homing toward a bleeding target with vermillion glow (#C8382E), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon`

![Cicatrice Stellaire (Scar Strike) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuile_39_cicatrice_stellaire_v3.png)

---

## Galerie des Tuiles Non-Obtenables (Ennemis / Boss) — 3 variantes par tuile

### N01. Volée Téléguidée (Volley)
- **Catégorie** : `Tir à case déclarée (Ennemi)`
- **Statistiques** : `Dégâts: 2 | Cible la case occupée par le héros à la déclaration`
- **Lore** : *Une volée de traits d'encre qui, une fois déclarée, s'abat infailliblement sur la case où se tenait le Pèlerin.*

#### Variante 1 — `volee_teleguidee` (seed 311001)
- **Fichier** : [tuileN_01_volee_teleguidee_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_01_volee_teleguidee_v1.png)
- **Prompt SD** : `Game action card tile icon, raining ink arrows descending onto a glowing vermillion target reticle (#C8382E) marked on a ground tile, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist enemy action icon`

![Volée Téléguidée (Volley) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_01_volee_teleguidee_v1.png)

#### Variante 2 — `volee_amber_swarm` (seed 311002)
- **Fichier** : [tuileN_01_volee_amber_swarm_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_01_volee_amber_swarm_v2.png)
- **Prompt SD** : `Game action card tile icon, swarm of amber homing projectiles (#FF9A4D) converging onto a telegraphed square with dark ink impact marks, sumi-e ink wash on warm paper ground (#EDE4D3), game UI icon`

![Volée Téléguidée (Volley) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_01_volee_amber_swarm_v2.png)

#### Variante 3 — `volee_nuage_tempete` (seed 311003)
- **Fichier** : [tuileN_01_volee_nuage_tempete_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_01_volee_nuage_tempete_v3.png)
- **Prompt SD** : `Game action card tile icon, dark ink volley falling from a small storm cloud onto a glowing red square marker, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist enemy tile icon`

![Volée Téléguidée (Volley) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_01_volee_nuage_tempete_v3.png)

---

### N02. Rempart de Papier (Barricade)
- **Catégorie** : `Obstacle défensif statique`
- **Statistiques** : `Dégâts: — | Construit une barricade devant soi`
- **Lore** : *Un mur de papier épais plié en origami défensif, barrant l'avant du parchemin.*

#### Variante 1 — `rempart_origami` (seed 312001)
- **Fichier** : [tuileN_02_rempart_origami_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_02_rempart_origami_v1.png)
- **Prompt SD** : `Game action card tile icon, folded paper origami barricade wall in aged parchment tone with ink brush creases, sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist enemy obstacle icon`

![Rempart de Papier (Barricade) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_02_rempart_origami_v1.png)

#### Variante 2 — `rempart_bois` (seed 312002)
- **Fichier** : [tuileN_02_rempart_bois_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_02_rempart_bois_v2.png)
- **Prompt SD** : `Game action card tile icon, stacked wooden ink panels forming a defensive barricade with dark brush texture, sumi-e ink wash on warm parchment (#EDE4D3), game UI icon`

![Rempart de Papier (Barricade) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_02_rempart_bois_v2.png)

#### Variante 3 — `rempart_sceau` (seed 312003)
- **Fichier** : [tuileN_02_rempart_sceau_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_02_rempart_sceau_v3.png)
- **Prompt SD** : `Game action card tile icon, stone-textured ink barrier with glowing starlight seal wards (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist icon`

![Rempart de Papier (Barricade) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_02_rempart_sceau_v3.png)

---

### N03. Sceau d'Invocation (Summon)
- **Catégorie** : `Invocation d'ennemi (Simple / Double Frappe)`
- **Statistiques** : `Dégâts: — | Invoque un ennemi sur une case aléatoire`
- **Lore** : *Un glyphe vermillon invoquant une ombre depuis les marges du Cosmo-Atlas.*

#### Variante 1 — `invocation_simple` (seed 313001)
- **Fichier** : [tuileN_03_invocation_simple_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_03_invocation_simple_v1.png)
- **Prompt SD** : `Game action card tile icon, single glowing summoning glyph in vermillion (#C8382E) with a dark silhouette rising from ink smoke, sumi-e ink wash on warm parchment (#EDE4D3), enemy action icon`

![Sceau d'Invocation (Summon) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_03_invocation_simple_v1.png)

#### Variante 2 — `invocation_double` (seed 313002)
- **Fichier** : [tuileN_03_invocation_double_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_03_invocation_double_v2.png)
- **Prompt SD** : `Game action card tile icon, twin summoning circles glowing vermillion (#C8382E) birthing two shadow silhouettes (Double Strike), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Sceau d'Invocation (Summon) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_03_invocation_double_v2.png)

#### Variante 3 — `invocation_faille` (seed 313003)
- **Fichier** : [tuileN_03_invocation_faille_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_03_invocation_faille_v3.png)
- **Prompt SD** : `Game action card tile icon, ragged ink rift with vermillion seams and a clawed silhouette emerging, sumi-e ink wash on warm parchment (#EDE4D3), minimalist enemy tile icon`

![Sceau d'Invocation (Summon) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_03_invocation_faille_v3.png)

---

### N04. Bombe à Retardement (Bomb)
- **Catégorie** : `Explosion différée (2 tours)`
- **Statistiques** : `Dégâts: 3 | Pose une bombe devant soi, explose après 2 tours`
- **Lore** : *Un orbe d'encre instable déposé sur la case avant, tic-tac de deux strophes avant l'explosion.*

#### Variante 1 — `bombe_amber` (seed 314001)
- **Fichier** : [tuileN_04_bombe_amber_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_04_bombe_amber_v1.png)
- **Prompt SD** : `Game action card tile icon, round ink bomb sphere with a glowing amber countdown glyph and lit fuse (#FF9A4D), sumi-e ink wash on warm parchment (#EDE4D3), game UI icon`

![Bombe à Retardement (Bomb) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_04_bombe_amber_v1.png)

#### Variante 2 — `bombe_vermillon` (seed 314002)
- **Fichier** : [tuileN_04_bombe_vermillon_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_04_bombe_vermillon_v2.png)
- **Prompt SD** : `Game action card tile icon, vermillion explosive orb (#C8382E) covered in cracks about to detonate with sparks, sumi-e ink wash style on warm paper ground (#EDE4D3), enemy tile icon`

![Bombe à Retardement (Bomb) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_04_bombe_vermillon_v2.png)

#### Variante 3 — `bombe_etoile` (seed 314003)
- **Fichier** : [tuileN_04_bombe_etoile_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_04_bombe_etoile_v3.png)
- **Prompt SD** : `Game action card tile icon, dark spherical bomb with a glowing blue-white starlight timer fuse (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Bombe à Retardement (Bomb) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_04_bombe_etoile_v3.png)

---

### N05. Aegis d'Encre (Shield)
- **Catégorie** : `Bouclier personnel`
- **Statistiques** : `Dégâts: — | Gagne un bouclier annulant la prochaine attaque`
- **Lore** : *Un disque d'encre protecteur se matérialisant devant le porteur, prêt à boire la prochaine frappe.*

#### Variante 1 — `aegis_disque` (seed 315001)
- **Fichier** : [tuileN_05_aegis_disque_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_05_aegis_disque_v1.png)
- **Prompt SD** : `Game action card tile icon, round cyan-blue ink shield disc with a glowing starlight rune (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), enemy buff icon`

![Aegis d'Encre (Shield) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_05_aegis_disque_v1.png)

#### Variante 2 — `aegis_hexagone` (seed 315002)
- **Fichier** : [tuileN_05_aegis_hexagone_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_05_aegis_hexagone_v2.png)
- **Prompt SD** : `Game action card tile icon, hexagonal ink shield ward with a pulsing star sigil (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Aegis d'Encre (Shield) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_05_aegis_hexagone_v2.png)

#### Variante 3 — `aegis_dome` (seed 315003)
- **Fichier** : [tuileN_05_aegis_dome_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_05_aegis_dome_v3.png)
- **Prompt SD** : `Game action card tile icon, dome of folded paper defense glowing soft blue-white (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Aegis d'Encre (Shield) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_05_aegis_dome_v3.png)

---

### N06. Aegis Partagé (Ally Shield)
- **Catégorie** : `Bouclier accordé à un allié`
- **Statistiques** : `Dégâts: — | Donne un bouclier au premier allié sans bouclier devant soi`
- **Lore** : *Un bouclier d'encre projeté vers l'allié le plus proche, tissé de la même étoile que le sien.*

#### Variante 1 — `aegis_partage_arc` (seed 316001)
- **Fichier** : [tuileN_06_aegis_partage_arc_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_06_aegis_partage_arc_v1.png)
- **Prompt SD** : `Game action card tile icon, shield disc arcing from a caster toward a front ally silhouette with a blue-white starlight trail (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), game UI icon`

![Aegis Partagé (Ally Shield) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_06_aegis_partage_arc_v1.png)

#### Variante 2 — `aegis_partage_fil` (seed 316002)
- **Fichier** : [tuileN_06_aegis_partage_fil_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_06_aegis_partage_fil_v2.png)
- **Prompt SD** : `Game action card tile icon, protective ward linking two ink figures with a glowing starlight thread (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), enemy tile icon`

![Aegis Partagé (Ally Shield) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_06_aegis_partage_fil_v2.png)

#### Variante 3 — `aegis_partage_voute` (seed 316003)
- **Fichier** : [tuileN_06_aegis_partage_voute_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_06_aegis_partage_voute_v3.png)
- **Prompt SD** : `Game action card tile icon, shared blue-white barrier arching over a small ally silhouette (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Aegis Partagé (Ally Shield) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_06_aegis_partage_voute_v3.png)

---

### N07. Miroir Mime (Copycat Mirror)
- **Catégorie** : `Copie d'une tuile du héros (Simple / Double Frappe)`
- **Statistiques** : `Dégâts: — | Se transforme en version basique d'une tuile aléatoire du héros`
- **Lore** : *Un miroir d'encre vivant qui reflète et détourne une tuile du Pèlerin pour s'en armer à son tour.*

#### Variante 1 — `miroir_reflet` (seed 317001)
- **Fichier** : [tuileN_07_miroir_reflet_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_07_miroir_reflet_v1.png)
- **Prompt SD** : `Game action card tile icon, ornate ink mirror reflecting a glowing sword glyph inside its surface (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Miroir Mime (Copycat Mirror) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_07_miroir_reflet_v1.png)

#### Variante 2 — `miroir_brise` (seed 317002)
- **Fichier** : [tuileN_07_miroir_brise_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_07_miroir_brise_v2.png)
- **Prompt SD** : `Game action card tile icon, shattered mirror with two reflected duplicated ink symbols and glowing starlight shards (#EAF2F8) (Double Strike), sumi-e ink wash on warm paper ground (#EDE4D3), game UI icon`

![Miroir Mime (Copycat Mirror) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_07_miroir_brise_v2.png)

#### Variante 3 — `miroir_liquide` (seed 317003)
- **Fichier** : [tuileN_07_miroir_liquide_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_07_miroir_liquide_v3.png)
- **Prompt SD** : `Game action card tile icon, liquid rippling ink mirror with a copied icon rising from the surface (#EAF2F8), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist icon`

![Miroir Mime (Copycat Mirror) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_07_miroir_liquide_v3.png)

---

### N08. Rideau de l'Acte (Maku)
- **Catégorie** : `Transition théâtrale (Sato)`
- **Statistiques** : `Dégâts: — | Baisse le rideau, passe à l'acte suivant`
- **Lore** : *Le lourd rideau d'encre qui tombe sur la scène, congédiant l'acte et invitant le suivant.*

#### Variante 1 — `rideau_vermillon` (seed 318001)
- **Fichier** : [tuileN_08_rideau_vermillon_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_08_rideau_vermillon_v1.png)
- **Prompt SD** : `Game action card tile icon, theatrical vermillion ink curtain dropping down within a stage frame (#C8382E), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Rideau de l'Acte (Maku) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_08_rideau_vermillon_v1.png)

#### Variante 2 — `rideau_acte` (seed 318002)
- **Fichier** : [tuileN_08_rideau_acte_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_08_rideau_acte_v2.png)
- **Prompt SD** : `Game action card tile icon, ink wash stage curtain with a glowing act numeral and amber paper lantern glow (#FF9A4D), sumi-e ink wash on warm paper ground (#EDE4D3), game UI icon`

![Rideau de l'Acte (Maku) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_08_rideau_acte_v2.png)

#### Variante 3 — `rideau_lanterne` (seed 318003)
- **Fichier** : [tuileN_08_rideau_lanterne_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_08_rideau_lanterne_v3.png)
- **Prompt SD** : `Game action card tile icon, sumi-e painted theater drape with a hanging starlight lantern and soft amber bloom (#FF9A4D) on warm parchment (#EDE4D3), minimalist icon`

![Rideau de l'Acte (Maku) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_08_rideau_lanterne_v3.png)

---

### N09. Relais du Maître (Boss Swap)
- **Catégorie** : `Transposition avec le boss`
- **Statistiques** : `Dégâts: — | Échange sa position avec le boss`
- **Lore** : *Un sceau de connivence permutant l'allié avec le maître de la scène.*

#### Variante 1 — `relais_echange` (seed 319001)
- **Fichier** : [tuileN_09_relais_echange_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_09_relais_echange_v1.png)
- **Prompt SD** : `Game action card tile icon, two swapping ink silhouettes linked by crossing arrows, one bearing a glowing crown glyph (#FF9A4D), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Relais du Maître (Boss Swap) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_09_relais_echange_v1.png)

#### Variante 2 — `relais_portail` (seed 319002)
- **Fichier** : [tuileN_09_relais_portail_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_09_relais_portail_v2.png)
- **Prompt SD** : `Game action card tile icon, transposition portal ring with a glowing crown sigil inside (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Relais du Maître (Boss Swap) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_09_relais_portail_v2.png)

#### Variante 3 — `relais_fil_etoile` (seed 319003)
- **Fichier** : [tuileN_09_relais_fil_etoile_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_09_relais_fil_etoile_v3.png)
- **Prompt SD** : `Game action card tile icon, exchange glyph between a large boss silhouette and a smaller ally linked by crossing starlight trails (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Relais du Maître (Boss Swap) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_09_relais_fil_etoile_v3.png)

---

### N10. Anneau de Souillure (Corrupted Barrage)
- **Catégorie** : `Corruption de zone extensible (Boss)`
- **Statistiques** : `Dégâts: 3 | Anneau s'étendant d'une case par tour ; blesse les unités, soigne les boss`
- **Lore** : *Un anneau de corruption rose qui s'étend d'une case à chaque strophe, rongeant les unités et abreuvant les boss.*

#### Variante 1 — `anneau_rose` (seed 320001)
- **Fichier** : [tuileN_10_anneau_rose_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_10_anneau_rose_v1.png)
- **Prompt SD** : `Game action card tile icon, expanding rose corruption ring (#D96E9A) with an outward pulse arrow on paper, sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Anneau de Souillure (Corrupted Barrage) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_10_anneau_rose_v1.png)

#### Variante 2 — `anneau_magenta` (seed 320002)
- **Fichier** : [tuileN_10_anneau_magenta_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_10_anneau_magenta_v2.png)
- **Prompt SD** : `Game action card tile icon, growing magenta ring (#B83A7A) with crackle fractures spreading outward, sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Anneau de Souillure (Corrupted Barrage) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_10_anneau_magenta_v2.png)

#### Variante 3 — `anneau_concentrique` (seed 320003)
- **Fichier** : [tuileN_10_anneau_concentrique_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_10_anneau_concentrique_v3.png)
- **Prompt SD** : `Game action card tile icon, concentric pulsing corruption circles in rose-magenta (#E07AA8) radiating outward, sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Anneau de Souillure (Corrupted Barrage) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_10_anneau_concentrique_v3.png)

---

### N11. Vague de Souillure (Corrupted Wave)
- **Catégorie** : `Corruption balayant le terrain (Boss)`
- **Statistiques** : `Dégâts: 1 | Vague traversant le terrain d'un bord à l'autre ; gauche/droite`
- **Lore** : *Une lame de fond corrompue balayant tout le parchemin d'un bord à l'autre, noyant les unités.*

#### Variante 1 — `vague_gauche` (seed 321001)
- **Fichier** : [tuileN_11_vague_gauche_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_11_vague_gauche_v1.png)
- **Prompt SD** : `Game action card tile icon, horizontal rose corruption wave sweeping leftward across the lane (#D96E9A) with an arrow direction, sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Vague de Souillure (Corrupted Wave) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_11_vague_gauche_v1.png)

#### Variante 2 — `vague_droite` (seed 321002)
- **Fichier** : [tuileN_11_vague_droite_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_11_vague_droite_v2.png)
- **Prompt SD** : `Game action card tile icon, magenta corruption tide sweeping rightward across the grid (#B83A7A) with an arrow direction, sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Vague de Souillure (Corrupted Wave) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_11_vague_droite_v2.png)

#### Variante 3 — `vague_bidir` (seed 321003)
- **Fichier** : [tuileN_11_vague_bidir_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_11_vague_bidir_v3.png)
- **Prompt SD** : `Game action card tile icon, bidirectional rose-magenta corruption waves crashing inward from both edges (#E07AA8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Vague de Souillure (Corrupted Wave) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_11_vague_bidir_v3.png)

---

### N12. Éclosion de Souillure (Corrupted Explosion)
- **Catégorie** : `Corruption totale des cases (Boss)`
- **Statistiques** : `Dégâts: 1 | Explosion corrompue touchant toutes les cases`
- **Lore** : *Une éclosion rose explosant sur l'ensemble des cases, embrasement total du songe.*

#### Variante 1 — `eclosion_radiale` (seed 322001)
- **Fichier** : [tuileN_12_eclosion_radiale_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_12_eclosion_radiale_v1.png)
- **Prompt SD** : `Game action card tile icon, radial rose corruption explosion covering all grid nodes (#D96E9A), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Éclosion de Souillure (Corrupted Explosion) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_12_eclosion_radiale_v1.png)

#### Variante 2 — `eclosion_fleur` (seed 322002)
- **Fichier** : [tuileN_12_eclosion_fleur_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_12_eclosion_fleur_v2.png)
- **Prompt SD** : `Game action card tile icon, magenta corruption bloom detonation with spreading filaments (#B83A7A), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Éclosion de Souillure (Corrupted Explosion) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_12_eclosion_fleur_v2.png)

#### Variante 3 — `eclosion_etoile` (seed 322003)
- **Fichier** : [tuileN_12_eclosion_etoile_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_12_eclosion_etoile_v3.png)
- **Prompt SD** : `Game action card tile icon, rose corruption starburst flares erupting across multiple cells (#E07AA8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Éclosion de Souillure (Corrupted Explosion) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_12_eclosion_etoile_v3.png)

---

### N13. Pouls de Souillure (Corrupted Pulse)
- **Catégorie** : `Corruption alternée par parité (Boss)`
- **Statistiques** : `Dégâts: 3 | Rayons alternant cases impaires/paires à chaque tour`
- **Lore** : *Des rayons corrompus palpitants qui alternent entre cases paires et impaires à chaque strophe.*

#### Variante 1 — `pouls_faisceaux` (seed 323001)
- **Fichier** : [tuileN_13_pouls_faisceaux_v1.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_13_pouls_faisceaux_v1.png)
- **Prompt SD** : `Game action card tile icon, vertical rose corruption beams striking alternating grid cells with odd-even markers (#D96E9A), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon`

![Pouls de Souillure (Corrupted Pulse) v1](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_13_pouls_faisceaux_v1.png)

#### Variante 2 — `pouls_damier` (seed 323002)
- **Fichier** : [tuileN_13_pouls_damier_v2.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_13_pouls_damier_v2.png)
- **Prompt SD** : `Game action card tile icon, magenta pulse rays over a checkerboard of glowing cells (#B83A7A), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon`

![Pouls de Souillure (Corrupted Pulse) v2](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_13_pouls_damier_v2.png)

#### Variante 3 — `pouls_parite` (seed 323003)
- **Fichier** : [tuileN_13_pouls_parite_v3.png](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_13_pouls_parite_v3.png)
- **Prompt SD** : `Game action card tile icon, corruption beams alternating with parity glyphs and rose flares (#E07AA8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon`

![Pouls de Souillure (Corrupted Pulse) v3](file:///C:/homeware/perso/spaces/starfall/tmp/tuiles/tuileN_13_pouls_parite_v3.png)

---

