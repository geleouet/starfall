import os

OUTPUT_DIR = os.path.abspath("tmp/maquettes")
README_PATH = os.path.join(OUTPUT_DIR, "README.md")

ALL_ITEMS = [
    {
        "num": "01",
        "title": "Maquette 01 — Écran de Combat Tactique (Vue Lane 15 cases)",
        "file": "maquette_01_combat_screen.png",
        "prompt": "2D side-view video game screenshot, tactical turn-based grid combat, two dark ink swordsman silhouettes facing each other on a linear 15-tile track, warm cream paper background (#EDE4D3), glowing luminous blue-white sword blade (#EAF2F8), floating glowing amber embers, dark ink smoke dissolve effect, clean polished video game UI, 5 glowing action queue cards at the bottom, crisp UI overlay"
    },
    {
        "num": "02",
        "title": "Maquette 02 — Focus File d'Actions (Programmation 5 Glyphes)",
        "file": "maquette_02_action_queue.png",
        "prompt": "Game UI mockup focused on action queue programming interface, bottom dashboard with 5 glowing ink action cards showing movement arrows and sword strike symbols, warm aged parchment paper texture, minimalist ink brush health bar on top left, subtle grid lines on paper ground, dusk sky background grading from deep indigo to violet to coral horizon, polished UI design"
    },
    {
        "num": "03",
        "title": "Maquette 03 — Carte du Cosmo-Atlas (Navigation & Constellations)",
        "file": "maquette_03_atlas_world_map.png",
        "prompt": "Game UI mockup, ancient celestial world map interface, Cosmo-Atlas of fading dreams, warm aged parchment paper texture with fine compass lines and meridians, glowing star nodes connected by thin ink lines, fog and mist drifting over map regions, floating jewel-toned bokeh motes, elegant fantasy UI map design"
    },
    {
        "num": "04 (Principale)",
        "title": "Maquette 04 — Zoom Exécution & Clash de Lames (Famille B Strict)",
        "file": "maquette_04_execution_parry.png",
        "prompt": "Traditional ink wash painting of two swordsmen in profile at dusk, dark indigo ink silhouettes, luminous white blade sliver, soft star bloom light at contact point, floating warm embers, wet black ink clouds dissolving off cloth extremities, sky grading from deep indigo to violet to warm coral salmon horizon, warm cream paper ground (#EDE4D3), sumi-e painterly style, minimalist game UI"
    },
    {
        "num": "04 (Variante 2)",
        "title": "Maquette 04 (Variante 2) — Clash d'Encre au Crépuscule",
        "file": "maquette_04_variant2.png",
        "prompt": "Sumi-e ink painting, dusk duel, two dark ink warrior silhouettes crossing swords, pale luminous blade (#EAF2F8), warm embers scattering (#FF9A4D), wet ink wash bleed, sky gradient indigo to coral, paper texture, minimal soft light bloom at blade clash, elegant fluid brushstrokes"
    },
    {
        "num": "04 (Variante 3)",
        "title": "Maquette 04 (Variante 3) — Ombre et Lumière au Contact",
        "file": "maquette_04_variant3.png",
        "prompt": "Minimalist ink wash artwork, two swordsmen in profile clashing blades, dark blue-black ink figures dissolving into smoke at feet, soft star light bloom at sword contact, warm orange embers floating upward, dusk sky gradient violet and salmon coral, warm paper ground, serene painterly aesthetic"
    },
    {
        "num": "05",
        "title": "Maquette 05 — Fiche du Pèlerin de la Nuit (Statut & Équipement)",
        "file": "maquette_05_character_inspection.png",
        "prompt": "Game UI mockup, character status and inventory screen, ink-painted warrior silhouette on warm paper ground with ochre rust armor stains, flowing hair trailing into ink wisps, equipment slots formatted as ink seals, minimalist typography, ethereal aesthetic, UI panel design"
    },
    {
        "num": "06",
        "title": "Maquette 06 — Écran de Victoire (Strophe Complétée)",
        "file": "maquette_06_victory_screen.png",
        "prompt": "Game UI mockup, victory end of battle screen, elegant calligraphic text 'Strophe Complétée' in wet ink on warm aged parchment background, glowing star rewards, floating amber particles, ink brush decorative borders, poetic victory interface"
    },
    {
        "num": "07",
        "title": "Maquette 07 — Sélection des Tuiles & Grimoire (Deckbuilding)",
        "file": "maquette_07_deck_grimoire.png",
        "prompt": "Game UI mockup, deck building grimoire screen, array of 8 ink-painted action cards with glowing symbols laid out on an open old manuscript book, parchment texture, warm golden highlights, clean UI layout for action queue customization"
    },
    {
        "num": "08 (Principale)",
        "title": "Maquette 08 — Duel en Brume Matinale (Famille C Strict)",
        "file": "maquette_08_misty_meadow_fight.png",
        "prompt": "Traditional sumi-e ink wash painting of a duel in a pale fog-filled meadow at dusk, soft pink and cream sky, dark ink duellist silhouettes in bruised blues and cool greys dissolving into heavy white fog at ground level, tiny glowing cyan and magenta bokeh light motes floating in air, soft painterly contemplative atmosphere, warm paper ground texture, minimal game UI overlay"
    },
    {
        "num": "08 (Variante 2)",
        "title": "Maquette 08 (Variante 2) — Brume & Joyaux Lumineux",
        "file": "maquette_08_variant2.png",
        "prompt": "Minimalist ink wash artwork, two swordsmen in profile standing in a misty meadow at twilight, pink salmon sky, figures half-dissolving into pale fog wash, floating out-of-focus cyan magenta amber jewel light motes, serene painterly aesthetic, paper texture, subtle red ink mark on ground tile"
    },
    {
        "num": "08 (Variante 3)",
        "title": "Maquette 08 (Variante 3) — Évanescence dans la Clairière",
        "file": "maquette_08_variant3.png",
        "prompt": "Sumi-e watercolor painting, atmospheric misty meadow at dusk, two dark ink warrior figures surrounded by thick horizontal drifting fog bands, soft cream and pink sky, pale glowing white sword sliver, drifting jewel bokeh lights, contemplative dreamlike mood, paper ground (#EDE4D3)"
    },
    {
        "num": "09",
        "title": "Maquette 09 — Écran Titre (STARFALL: L'Atlas des Songes Éteints)",
        "file": "maquette_09_title_screen.png",
        "prompt": "Game UI title screen mockup, 'STARFALL: L'Atlas des Songes Éteints', ink wash title logo on warm cream paper, distant falling meteors as glowing ink drops, mist drifting across bottom, start game and options in elegant serif typography, serene atmospheric UI"
    },
    {
        "num": "10",
        "title": "Maquette 10 — Écran de Défaite (Le Songe s'Efface)",
        "file": "maquette_10_defeat_screen.png",
        "prompt": "Game UI mockup, defeat screen, fading ink character dissolving into wet black wash and smoke, single glowing blade lying on warm paper ground, soft melancholic coral sky, calligraphic text 'Le Songe s'Efface', poetic game over UI"
    }
]

def update_readme():
    with open(README_PATH, "w", encoding="utf-8") as f:
        f.write("# Catalogue des Maquettes — STARFALL: L'Atlas des Songes Éteints\n\n")
        f.write("Catalogue mis à jour des maquettes générées via **Forge Neo** (`http://undefined.egaetan.me:7862/`).\n\n")
        f.write("## ⚙️ Configuration Technique :\n")
        f.write("- **Serveur** : `http://undefined.egaetan.me:7862/`\n")
        f.write("- **Modèle** : `krea2_turbo_nvfp4 [61527003b2]`\n")
        f.write("- **Modules** : `wan_2.1_vae.safetensors`, `qwen3VLInstruct4bHeretic_int8Convrot.safetensors`\n")
        f.write("- **Steps** : `8` | **Sampler** : `Euler` | **Schedule type** : `Simple` | **CFG Scale** : `1` | **Resolution** : `1920x1088` (16:9)\n")
        f.write("- **Charte Graphique** : Conforme aux familles A, B, C du document [`STYLE.md`](file:///C:/homeware/perso/spaces/starfall/STYLE.md) (Lavis d'encre sumi-e, papier chaud `#EDE4D3`, lames lumineuses `#EAF2F8`, brumes & bokehs).\n\n")
        f.write("---\n\n")
        f.write("## 🎨 Galerie des Maquettes In-Game :\n\n")

        for item in ALL_ITEMS:
            filepath = os.path.join(OUTPUT_DIR, item['file']).replace('\\', '/')
            f.write(f"### {item['num']}. {item['title']}\n")
            f.write(f"- **Fichier** : [{item['file']}](file:///{filepath})\n")
            f.write(f"- **Prompt** : `{item['prompt']}`\n\n")
            f.write(f"![{item['title']}](file:///{filepath})\n\n")
            f.write("---\n\n")

    print(f"README.md successfully updated at {README_PATH}")

if __name__ == "__main__":
    update_readme()
