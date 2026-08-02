import os
import json
import base64
import urllib.request

SERVER_URL = "http://undefined.egaetan.me:7862/sdapi/v1/txt2img"
OUTPUT_DIR = os.path.abspath("tmp/tuiles")
os.makedirs(OUTPUT_DIR, exist_ok=True)

COMMON_PARAMS = {
    "steps": 8,
    "sampler_name": "Euler",
    "scheduler": "Simple",
    "cfg_scale": 1.0,
    "seed": 2329875676,
    "width": 1024,
    "height": 1024,
    "negative_prompt": "3d render, CGI, photograph, photorealistic, ugly, distorted, noisy, watermark, frame, text overlay, bad anatomy",
    "override_settings": {
        "sd_model_checkpoint": "krea2_turbo_nvfp4.safetensors",
    }
}

TUILES = [
    {
        "filename": "tuile_01_trace_de_lame.png",
        "name": "Tracé de la Lame (Katana)",
        "type": "Frappe simple (Attaque)",
        "stats": "Dégâts: 2 | Cooldown: 0 (Tuile de base)",
        "lore": "Un trait d'encre fluide et direct gravé à la plume sur la case adjacente.",
        "prompt": "Game action card tile icon, single glowing sword slash stroke in luminous blue-white ink (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), dark ink brush border, minimalist game UI icon design, high quality"
    },
    {
        "filename": "tuile_02_marteau_de_meteore.png",
        "name": "Marteau de Météore (Tetsubo)",
        "type": "Frappe lourde (Attaque)",
        "stats": "Dégâts: 4 | Cooldown: 7 | Coût: 3 Crânes",
        "lore": "Un lourd stamp d'encre sombre écrasant la case sous la pression d'une masse céleste.",
        "prompt": "Game action card tile icon, heavy warhammer head crashing down with floating amber embers (#FF9A4D), sumi-e ink wash painting on warm parchment paper ground (#EDE4D3), dark wet ink splatter, minimalist game UI icon design"
    },
    {
        "filename": "tuile_03_pointe_du_meridien.png",
        "name": "Pointe du Méridien (Spear)",
        "type": "Frappe perçante (Attaque à zone)",
        "stats": "Dégâts: 2 | Cooldown: 5 (Tuile de base)",
        "lore": "Une aiguille d'encre effilée transperçant deux cases consécutives sur la ligne de boussole.",
        "prompt": "Game action card tile icon, long sharp spear thrust piercing forward through two glowing ink nodes, sumi-e ink wash style on warm paper ground (#EDE4D3), luminous blade tip (#EAF2F8), dark brush strokes, game icon UI"
    },
    {
        "filename": "tuile_04_vortex_de_pinceau.png",
        "name": "Vortex de Pinceau (Swirl)",
        "type": "Frappe circulaire (Devant & Derrière)",
        "stats": "Dégâts: 2 | Cooldown: 3 (Tuile de base)",
        "lore": "Une rotation de la plume libérant un halo d'encre devant et derrière soi simultanément.",
        "prompt": "Game action card tile icon, circular 360-degree brush swirl of black ink and glowing blue starlight (#EAF2F8), sumi-e wash style on warm cream paper ground (#EDE4D3), elegant minimalist UI icon"
    },
    {
        "filename": "tuile_05_fleche_d_etoile.png",
        "name": "Flèche d'Étoile (Shuriken / Arrow)",
        "type": "Frappe à distance (Projectile)",
        "stats": "Dégâts: 2 | Cooldown: 5 (Tuile de base)",
        "lore": "Un projectile de lumière stellaire taillé dans le verre de météore, frappant à distance.",
        "prompt": "Game action card tile icon, glowing starlight arrow projectile flying through dark ink mist, sumi-e ink wash painting on warm paper ground (#EDE4D3), radiant blue-white core (#EAF2F8), floating amber embers, game icon"
    },
    {
        "filename": "tuile_06_eclair_du_firmament.png",
        "name": "Éclair du Firmament (Lightning)",
        "type": "Frappe à distance éloignée",
        "stats": "Dégâts: 2 | Cooldown: 5 | Coût: 3 Crânes",
        "lore": "Un trait de foudre céleste frappant directement la cible la plus distante de la carte.",
        "prompt": "Game action card tile icon, zig-zag lightning bolt of pure cyan starlight (#5FD8E8) striking down from an ink storm cloud, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game icon design"
    },
    {
        "filename": "tuile_07_onde_de_compas.png",
        "name": "Onde de Compas (Dragon Punch / Ki Push)",
        "type": "Frappe à repoussement (Mobilité)",
        "stats": "Dégâts: 1 | Cooldown: 4 | Coût: 1 Crâne",
        "lore": "Une impulsion de force cartographique repoussant l'ennemi jusqu'au bord du parchemin.",
        "prompt": "Game action card tile icon, glowing shockwave burst expanding outward with directional push arrow in ink, sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist clean UI icon design"
    },
    {
        "filename": "tuile_08_brouillard_de_songe.png",
        "name": "Brouillard de Songe (Smoke Bomb)",
        "type": "Téléportation / Transposition",
        "stats": "Dégâts: 1 | Cooldown: 5 | Coût: 1 Crâne",
        "lore": "Une bouffée d'encre et de brume transposant la position du Pèlerin avec celle de son adversaire.",
        "prompt": "Game action card tile icon, swirling cloud of dense black ink smoke and white fog with two intersecting swap arrows, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon"
    },
    {
        "filename": "tuile_09_glissement_d_ombre.png",
        "name": "Glissement d'Ombre (Shadow Dash)",
        "type": "Traversée (Mobilité)",
        "stats": "Dégâts: 1 | Cooldown: 5 (Tuile de base)",
        "lore": "Le Pèlerin se dissout temporairement en encre liquide pour traverser les silhouettes adverses.",
        "prompt": "Game action card tile icon, shadow silhouette dashing forward leaving a trail of wet black ink and glowing starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon"
    },
    {
        "filename": "tuile_10_buvard_de_malediction.png",
        "name": "Buvard de Malédiction (Curse - Free-Play)",
        "type": "Altération (Action Gratuite)",
        "stats": "Dégâts: — | Cooldown: 7 | Coût: 1 Crâne | Free-Play",
        "lore": "Un buvard d'encre vermillon appliqué sans délai sur la cible, doublant les dégâts du prochain coup.",
        "prompt": "Game action card tile icon, ominous vermillion red ink seal blot (#C8382E) with glowing cursed magic runes, sumi-e ink wash style on warm paper ground (#EDE4D3), striking red accent, minimalist game UI icon"
    },
    {
        "filename": "tuile_11_eclat_de_meteore.png",
        "name": "Éclat de Météore (Blazing Suisei)",
        "type": "Explosion sur coup fatal",
        "stats": "Dégâts: 2 | Cooldown: 4 | Coût: 30 Crânes",
        "lore": "Une étincelle d'étoile qui explose en gerbes d'ambre sur la case et ses adjacentes en cas de coup fatal.",
        "prompt": "Game action card tile icon, falling meteor star bursting into an explosion of fiery amber embers (#FF9A4D) and ink flecks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game UI icon design"
    },
    {
        "filename": "tuile_12_lame_de_patience.png",
        "name": "Lame de Patience (Blade of Patience)",
        "type": "Attaque à charge temporelle",
        "stats": "Dégâts: 0 (+1 par tour en file) | Cooldown: 6 | Coût: 10 Crânes",
        "lore": "Une lame d'encre dont l'intensité lumineuse s'accroît à chaque tour passé dans la strophe.",
        "prompt": "Game action card tile icon, elegant sword blade slowly accumulating brilliant white-blue radiant energy (#EAF2F8) in an hourglass ink aura, sumi-e wash style on warm paper ground (#EDE4D3), game UI icon"
    },
    {
        "filename": "tuile_13_barriere_de_ratures.png",
        "name": "Barrière de Ratures (Trap / Thorns)",
        "type": "Piège au sol / Obstacle",
        "stats": "Dégâts: 3 | Cooldown: 4 | Coût: 6 Crânes",
        "lore": "Un piège de ratures d'encre vives déposé sur la carte, blessant quiconque s'y aventure.",
        "prompt": "Game action card tile icon, sharp spiky ink thorn trap drawn on paper ground with red threat markings (#C8382E), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist UI icon"
    },
    {
        "filename": "tuile_14_sceau_du_centre.png",
        "name": "Sceau du Centre (Origin of Symmetry - Free-Play)",
        "type": "Téléportation centrale (Action Gratuite)",
        "stats": "Dégâts: — | Cooldown: 6 | Coût: 30 Crânes | Free-Play",
        "lore": "Une téléportation sacrée ramenant le Pèlerin au centre exact de la grille du Cosmo-Atlas.",
        "prompt": "Game action card tile icon, symmetrical compass rose icon drawn in gold and dark ink with glowing center star, sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon"
    },
    {
        "filename": "tuile_15_miroir_de_brume.png",
        "name": "Miroir de Brume (Mirror - Free-Play)",
        "type": "Contre-téléportation (Action Gratuite)",
        "stats": "Dégâts: — | Cooldown: 6 | Coût: 20 Crânes | Free-Play",
        "lore": "Un reflet d'encre qui permute le Pèlerin directement sur la position de son agresseur.",
        "prompt": "Game action card tile icon, two mirrored ink silhouettes swapping places through a glowing silver portal frame, sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_16_disque_de_poussiere.png",
        "name": "Disque de Poussière d'Étoile (Chakram)",
        "type": "Attaque à accumulation de victimes",
        "stats": "Dégâts: 0 (+1 par ennemi vaincu) | Cooldown: 7 | Coût: 30 Crânes",
        "lore": "Un cercle d'encre rotatif qui accumule la poussière dorée des étoiles à chaque ombre terrassée.",
        "prompt": "Game action card tile icon, spinning circular chakram ring of glowing stardust motes and ink trails, sumi-e wash style on warm parchment paper (#EDE4D3), elegant UI icon"
    },
    {
        "filename": "tuile_17_fil_d_encre.png",
        "name": "Fil d'Encre (Grappling Hook)",
        "type": "Traction d'ennemi à distance",
        "stats": "Dégâts: 1 | Cooldown: 4 | Coût: 3 Crânes",
        "lore": "Un fil de soie étoilée agrippant une ombre à distance pour la ramener au contact.",
        "prompt": "Game action card tile icon, fine glowing thread of starlight silk pulling a dark target icon closer, sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist clean UI icon"
    },
    {
        "filename": "tuile_18_canon_de_starlight.png",
        "name": "Canon de Starlight (Tanegashima)",
        "type": "Attaque lourde avec recul mutuel",
        "stats": "Dégâts: 4 | Cooldown: 7 | Coût: 25 Crânes",
        "lore": "Un souffle de puissance céleste propulsant la cible et le Pèlerin dans des directions opposées.",
        "prompt": "Game action card tile icon, heavy celestial cannon blast with recoil shockwave in black ink and bright starlight burst, sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon design"
    }
]

def generate_tuiles():
    summary_data = []
    print(f"Starting generation of {len(TUILES)} tile illustrations to {OUTPUT_DIR}...")
    for idx, item in enumerate(TUILES, 1):
        filename = item["filename"]
        name = item["name"]
        prompt = item["prompt"]
        out_path = os.path.join(OUTPUT_DIR, filename)

        payload = dict(COMMON_PARAMS)
        payload["prompt"] = prompt

        print(f"[{idx}/{len(TUILES)}] Generating tile '{name}' -> {filename}...")
        try:
            req = urllib.request.Request(
                SERVER_URL,
                data=json.dumps(payload).encode('utf-8'),
                headers={'Content-Type': 'application/json'}
            )
            res = json.loads(urllib.request.urlopen(req, timeout=300).read().decode('utf-8'))
            img_b64 = res['images'][0]
            img_data = base64.b64decode(img_b64)
            with open(out_path, 'wb') as f:
                f.write(img_data)
            print(f"    Saved {out_path} ({len(img_data)} bytes)")
            summary_data.append({
                "index": idx,
                "name": name,
                "type": item["type"],
                "stats": item["stats"],
                "lore": item["lore"],
                "filename": filename,
                "filepath": out_path,
                "prompt": prompt,
            })
        except Exception as e:
            print(f"    ERROR generating {filename}: {e}")

    # Generate README.md catalogue in tmp/tuiles
    readme_path = os.path.join(OUTPUT_DIR, "README.md")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write("# Catalogue des Tuiles d'Action — STARFALL: L'Atlas des Songes Éteints\n\n")
        f.write("Catalogue officiel des tuiles d'attaque, de déplacement et d'action du jeu, adaptées des mécaniques de *Shogun Showdown* vers l'univers **L'Atlas des Songes Éteints**.\n\n")
        f.write("## ⚙️ Configuration de Génération (Forge Neo) :\n")
        f.write("- **Serveur** : `http://undefined.egaetan.me:7862/`\n")
        f.write("- **Modèle** : `krea2_turbo_nvfp4 [61527003b2]`\n")
        f.write("- **Modules** : `wan_2.1_vae.safetensors`, `qwen3VLInstruct4bHeretic_int8Convrot.safetensors`\n")
        f.write("- **Format** : `1024x1024` (Icônes carrées 1:1 pour cartes d'action)\n")
        f.write("- **Style** : Lavis d'encre sumi-e sur papier chaud (`#EDE4D3`), touches d'acier étoilé (`#EAF2F8`), ambre (`#FF9A4D`) et vermillon (`#C8382E`).\n\n")
        f.write("---\n\n")
        f.write("## 🎴 Galerie des Tuiles :\n\n")
        for item in summary_data:
            filepath = item['filepath'].replace('\\', '/')
            f.write(f"### {item['index']}. {item['name']}\n")
            f.write(f"- **Type** : `{item['type']}`\n")
            f.write(f"- **Statistiques** : `{item['stats']}`\n")
            f.write(f"- **Lore** : *{item['lore']}*\n")
            f.write(f"- **Fichier** : [{item['filename']}](file:///{filepath})\n")
            f.write(f"- **Prompt Prompt SD** : `{item['prompt']}`\n\n")
            f.write(f"![{item['name']}](file:///{filepath})\n\n")
            f.write("---\n\n")

    print(f"Catalogue complete saved to {readme_path}")

if __name__ == "__main__":
    generate_tuiles()
