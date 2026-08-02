import os
import json
import base64
import urllib.request

SERVER_URL = "http://undefined.egaetan.me:7862/sdapi/v1/txt2img"
OUTPUT_DIR = os.path.abspath("tmp/maquettes")

COMMON_PARAMS = {
    "steps": 8,
    "sampler_name": "Euler",
    "scheduler": "Simple",
    "cfg_scale": 1.0,
    "seed": 2329875676,
    "width": 1920,
    "height": 1088,
    "negative_prompt": "blurry, low quality, 3d render, ugly, distorted, noise, watermark, oversaturated",
    "override_settings": {
        "sd_model_checkpoint": "krea2_turbo_nvfp4.safetensors",
    }
}

MISSING_MOCKUPS = [
    {
        "filename": "maquette_01_combat_screen.png",
        "title": "Maquette 01 — Écran de Combat Tactique (Vue Lane 15 cases)",
        "prompt": "Game UI mockup, 2D tactical turn-based combat lane on a linear grid of 15 tiles, two shadowy ink duellists facing each other, warm cream paper background (#EDE4D3), glowing luminous blue-white sword blade (#EAF2F8) with soft aura, warm glowing embers (#FF9A4D), ink wisps dissolving into dark clouds at extremities, floating cyan and magenta bokeh lights, clean minimalist UI layout, bottom action bar with 5 ink cartouches, 16:9 widescreen, digital illustration"
    },
    {
        "filename": "maquette_02_action_queue.png",
        "title": "Maquette 02 — Focus File d'Actions (Programmation 5 Glyphes)",
        "prompt": "Game UI mockup focused on action queue programming interface, bottom dashboard with 5 glowing ink action cards showing movement arrows and sword strike symbols, warm aged parchment paper texture, minimalist ink brush health bar on top left, subtle grid lines on paper ground, dusk sky background grading from deep indigo to violet to coral horizon, polished UI design"
    }
]

ALL_MOCKUPS = [
    ("maquette_01_combat_screen.png", "Maquette 01 — Écran de Combat Tactique (Vue Lane 15 cases)", "Game UI mockup, 2D tactical turn-based combat lane on a linear grid of 15 tiles, two shadowy ink duellists facing each other, warm cream paper background (#EDE4D3), glowing luminous blue-white sword blade (#EAF2F8) with soft aura, warm glowing embers (#FF9A4D), ink wisps dissolving into dark clouds at extremities, floating cyan and magenta bokeh lights, clean minimalist UI layout, bottom action bar with 5 ink cartouches, 16:9 widescreen, digital illustration"),
    ("maquette_02_action_queue.png", "Maquette 02 — Focus File d'Actions (Programmation 5 Glyphes)", "Game UI mockup focused on action queue programming interface, bottom dashboard with 5 glowing ink action cards showing movement arrows and sword strike symbols, warm aged parchment paper texture, minimalist ink brush health bar on top left, subtle grid lines on paper ground, dusk sky background grading from deep indigo to violet to coral horizon, polished UI design"),
    ("maquette_03_atlas_world_map.png", "Maquette 03 — Carte du Cosmo-Atlas (Navigation & Constellations)", "Game UI mockup, ancient celestial world map interface, Cosmo-Atlas of fading dreams, warm aged parchment paper texture with fine compass lines and meridians, glowing star nodes connected by thin ink lines, fog and mist drifting over map regions, floating jewel-toned bokeh motes, elegant fantasy UI map design"),
    ("maquette_04_execution_parry.png", "Maquette 04 — Zoom Exécution & Clash de Lames (Parade)", "Game UI mockup, cinematic close-up execution camera view, two duellists clashing glowing swords in profile, explosive star-shaped light bloom at contact point (#FFF6E2), scattering warm embers (#FF9A4D), ink flecks shedding from dark cloth, soft blurred ink wash background, epic dramatic duel moment, video game screenshot"),
    ("maquette_05_character_inspection.png", "Maquette 05 — Fiche du Pèlerin de la Nuit (Statut & Équipement)", "Game UI mockup, character status and inventory screen, ink-painted warrior silhouette on warm paper ground with ochre rust armor stains, flowing hair trailing into ink wisps, equipment slots formatted as ink seals, minimalist typography, ethereal aesthetic, UI panel design"),
    ("maquette_06_victory_screen.png", "Maquette 06 — Écran de Victoire (Strophe Complétée)", "Game UI mockup, victory end of battle screen, elegant calligraphic text 'Strophe Complétée' in wet ink on warm aged parchment background, glowing star rewards, floating amber particles, ink brush decorative borders, poetic victory interface"),
    ("maquette_07_deck_grimoire.png", "Maquette 07 — Sélection des Tuiles & Grimoire (Deckbuilding)", "Game UI mockup, deck building grimoire screen, array of 8 ink-painted action cards with glowing symbols laid out on an open old manuscript book, parchment texture, warm golden highlights, clean UI layout for action queue customization"),
    ("maquette_08_misty_meadow_fight.png", "Maquette 08 — Duel en Brume Matinale (Atmosphère Family C)", "Game UI mockup, tactical 2D turn-based duel in a pale fog-filled meadow at dusk, pink and salmon horizon sky, bruised blue ink garments dissolving into mist, cyan and magenta floating bokeh motes, subtle red telegraph threat zone on ground tiles, minimalist game UI"),
    ("maquette_09_title_screen.png", "Maquette 09 — Écran Titre (STARFALL: L'Atlas des Songes Éteints)", "Game UI title screen mockup, 'STARFALL: L'Atlas des Songes Éteints', ink wash title logo on warm cream paper, distant falling meteors as glowing ink drops, mist drifting across bottom, start game and options in elegant serif typography, serene atmospheric UI"),
    ("maquette_10_defeat_screen.png", "Maquette 10 — Écran de Défaite (Le Songe s'Efface)", "Game UI mockup, defeat screen, fading ink character dissolving into wet black wash and smoke, single glowing blade lying on warm paper ground, soft melancholic coral sky, calligraphic text 'Le Songe s'Efface', poetic game over UI")
]

def main():
    for item in MISSING_MOCKUPS:
        filename = item["filename"]
        title = item["title"]
        prompt = item["prompt"]
        out_path = os.path.join(OUTPUT_DIR, filename)

        payload = dict(COMMON_PARAMS)
        payload["prompt"] = prompt

        print(f"Generating missing '{title}' -> {filename}...")
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
        except Exception as e:
            print(f"    ERROR generating {filename}: {e}")

    # Re-build README.md
    readme_path = os.path.join(OUTPUT_DIR, "README.md")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write("# Catalogue des Maquettes — STARFALL: L'Atlas des Songes Éteints\n\n")
        f.write("Maquettes générées via **Forge Neo** (`http://undefined.egaetan.me:7862/`).\n\n")
        f.write("## Configuration Générale :\n")
        f.write("- **Modèle** : `krea2_turbo_nvfp4 [61527003b2]`\n")
        f.write("- **Modules** : `wan_2.1_vae.safetensors`, `qwen3VLInstruct4bHeretic_int8Convrot.safetensors`\n")
        f.write("- **Steps** : `8` | **Sampler** : `Euler` | **Schedule type** : `Simple` | **CFG Scale** : `1` | **Seed** : `2329875676`\n")
        f.write("- **Taille** : `1920x1088` (16:9 widescreen)\n\n")
        f.write("---\n\n")
        f.write("## Galerie des 10 Maquettes UI :\n\n")
        for idx, (filename, title, prompt) in enumerate(ALL_MOCKUPS, 1):
            filepath = os.path.join(OUTPUT_DIR, filename).replace('\\', '/')
            f.write(f"### {idx}. {title}\n")
            f.write(f"- **Fichier** : [{filename}](file:///{filepath})\n")
            f.write(f"- **Prompt** : `{prompt}`\n\n")
            f.write(f"![{title}](file:///{filepath})\n\n")
            f.write("---\n\n")

    print(f"Final catalog updated at {readme_path}")

if __name__ == "__main__":
    main()
