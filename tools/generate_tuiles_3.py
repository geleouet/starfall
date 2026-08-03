import os
import json
import base64
import urllib.request

from generate_tuiles import TUILES, COMMON_PARAMS, SERVER_URL, OUTPUT_DIR
from generate_tuiles_2 import NEW_TUILES

os.makedirs(OUTPUT_DIR, exist_ok=True)

OBTENABLES = TUILES + NEW_TUILES

NON_OBTENABLES = [
    {
        "name": "Volée Téléguidée (Volley)",
        "category": "Tir à case déclarée (Ennemi)",
        "stats": "Dégâts: 2 | Cible la case occupée par le héros à la déclaration",
        "lore": "Une volée de traits d'encre qui, une fois déclarée, s'abat infailliblement sur la case où se tenait le Pèlerin.",
        "variants": [
            {"suffix": "volee_teleguidee", "seed": 311001, "prompt": "Game action card tile icon, raining ink arrows descending onto a glowing vermillion target reticle (#C8382E) marked on a ground tile, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist enemy action icon"},
            {"suffix": "volee_amber_swarm", "seed": 311002, "prompt": "Game action card tile icon, swarm of amber homing projectiles (#FF9A4D) converging onto a telegraphed square with dark ink impact marks, sumi-e ink wash on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "volee_nuage_tempete", "seed": 311003, "prompt": "Game action card tile icon, dark ink volley falling from a small storm cloud onto a glowing red square marker, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist enemy tile icon"},
        ],
    },
    {
        "name": "Rempart de Papier (Barricade)",
        "category": "Obstacle défensif statique",
        "stats": "Dégâts: — | Construit une barricade devant soi",
        "lore": "Un mur de papier épais plié en origami défensif, barrant l'avant du parchemin.",
        "variants": [
            {"suffix": "rempart_origami", "seed": 312001, "prompt": "Game action card tile icon, folded paper origami barricade wall in aged parchment tone with ink brush creases, sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist enemy obstacle icon"},
            {"suffix": "rempart_bois", "seed": 312002, "prompt": "Game action card tile icon, stacked wooden ink panels forming a defensive barricade with dark brush texture, sumi-e ink wash on warm parchment (#EDE4D3), game UI icon"},
            {"suffix": "rempart_sceau", "seed": 312003, "prompt": "Game action card tile icon, stone-textured ink barrier with glowing starlight seal wards (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Sceau d'Invocation (Summon)",
        "category": "Invocation d'ennemi (Simple / Double Frappe)",
        "stats": "Dégâts: — | Invoque un ennemi sur une case aléatoire",
        "lore": "Un glyphe vermillon invoquant une ombre depuis les marges du Cosmo-Atlas.",
        "variants": [
            {"suffix": "invocation_simple", "seed": 313001, "prompt": "Game action card tile icon, single glowing summoning glyph in vermillion (#C8382E) with a dark silhouette rising from ink smoke, sumi-e ink wash on warm parchment (#EDE4D3), enemy action icon"},
            {"suffix": "invocation_double", "seed": 313002, "prompt": "Game action card tile icon, twin summoning circles glowing vermillion (#C8382E) birthing two shadow silhouettes (Double Strike), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "invocation_faille", "seed": 313003, "prompt": "Game action card tile icon, ragged ink rift with vermillion seams and a clawed silhouette emerging, sumi-e ink wash on warm parchment (#EDE4D3), minimalist enemy tile icon"},
        ],
    },
    {
        "name": "Bombe à Retardement (Bomb)",
        "category": "Explosion différée (2 tours)",
        "stats": "Dégâts: 3 | Pose une bombe devant soi, explose après 2 tours",
        "lore": "Un orbe d'encre instable déposé sur la case avant, tic-tac de deux strophes avant l'explosion.",
        "variants": [
            {"suffix": "bombe_amber", "seed": 314001, "prompt": "Game action card tile icon, round ink bomb sphere with a glowing amber countdown glyph and lit fuse (#FF9A4D), sumi-e ink wash on warm parchment (#EDE4D3), game UI icon"},
            {"suffix": "bombe_vermillon", "seed": 314002, "prompt": "Game action card tile icon, vermillion explosive orb (#C8382E) covered in cracks about to detonate with sparks, sumi-e ink wash style on warm paper ground (#EDE4D3), enemy tile icon"},
            {"suffix": "bombe_etoile", "seed": 314003, "prompt": "Game action card tile icon, dark spherical bomb with a glowing blue-white starlight timer fuse (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Aegis d'Encre (Shield)",
        "category": "Bouclier personnel",
        "stats": "Dégâts: — | Gagne un bouclier annulant la prochaine attaque",
        "lore": "Un disque d'encre protecteur se matérialisant devant le porteur, prêt à boire la prochaine frappe.",
        "variants": [
            {"suffix": "aegis_disque", "seed": 315001, "prompt": "Game action card tile icon, round cyan-blue ink shield disc with a glowing starlight rune (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), enemy buff icon"},
            {"suffix": "aegis_hexagone", "seed": 315002, "prompt": "Game action card tile icon, hexagonal ink shield ward with a pulsing star sigil (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "aegis_dome", "seed": 315003, "prompt": "Game action card tile icon, dome of folded paper defense glowing soft blue-white (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Aegis Partagé (Ally Shield)",
        "category": "Bouclier accordé à un allié",
        "stats": "Dégâts: — | Donne un bouclier au premier allié sans bouclier devant soi",
        "lore": "Un bouclier d'encre projeté vers l'allié le plus proche, tissé de la même étoile que le sien.",
        "variants": [
            {"suffix": "aegis_partage_arc", "seed": 316001, "prompt": "Game action card tile icon, shield disc arcing from a caster toward a front ally silhouette with a blue-white starlight trail (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), game UI icon"},
            {"suffix": "aegis_partage_fil", "seed": 316002, "prompt": "Game action card tile icon, protective ward linking two ink figures with a glowing starlight thread (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), enemy tile icon"},
            {"suffix": "aegis_partage_voute", "seed": 316003, "prompt": "Game action card tile icon, shared blue-white barrier arching over a small ally silhouette (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Miroir Mime (Copycat Mirror)",
        "category": "Copie d'une tuile du héros (Simple / Double Frappe)",
        "stats": "Dégâts: — | Se transforme en version basique d'une tuile aléatoire du héros",
        "lore": "Un miroir d'encre vivant qui reflète et détourne une tuile du Pèlerin pour s'en armer à son tour.",
        "variants": [
            {"suffix": "miroir_reflet", "seed": 317001, "prompt": "Game action card tile icon, ornate ink mirror reflecting a glowing sword glyph inside its surface (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "miroir_brise", "seed": 317002, "prompt": "Game action card tile icon, shattered mirror with two reflected duplicated ink symbols and glowing starlight shards (#EAF2F8) (Double Strike), sumi-e ink wash on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "miroir_liquide", "seed": 317003, "prompt": "Game action card tile icon, liquid rippling ink mirror with a copied icon rising from the surface (#EAF2F8), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Rideau de l'Acte (Maku)",
        "category": "Transition théâtrale (Sato)",
        "stats": "Dégâts: — | Baisse le rideau, passe à l'acte suivant",
        "lore": "Le lourd rideau d'encre qui tombe sur la scène, congédiant l'acte et invitant le suivant.",
        "variants": [
            {"suffix": "rideau_vermillon", "seed": 318001, "prompt": "Game action card tile icon, theatrical vermillion ink curtain dropping down within a stage frame (#C8382E), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "rideau_acte", "seed": 318002, "prompt": "Game action card tile icon, ink wash stage curtain with a glowing act numeral and amber paper lantern glow (#FF9A4D), sumi-e ink wash on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "rideau_lanterne", "seed": 318003, "prompt": "Game action card tile icon, sumi-e painted theater drape with a hanging starlight lantern and soft amber bloom (#FF9A4D) on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Relais du Maître (Boss Swap)",
        "category": "Transposition avec le boss",
        "stats": "Dégâts: — | Échange sa position avec le boss",
        "lore": "Un sceau de connivence permutant l'allié avec le maître de la scène.",
        "variants": [
            {"suffix": "relais_echange", "seed": 319001, "prompt": "Game action card tile icon, two swapping ink silhouettes linked by crossing arrows, one bearing a glowing crown glyph (#FF9A4D), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "relais_portail", "seed": 319002, "prompt": "Game action card tile icon, transposition portal ring with a glowing crown sigil inside (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "relais_fil_etoile", "seed": 319003, "prompt": "Game action card tile icon, exchange glyph between a large boss silhouette and a smaller ally linked by crossing starlight trails (#EAF2F8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Anneau de Souillure (Corrupted Barrage)",
        "category": "Corruption de zone extensible (Boss)",
        "stats": "Dégâts: 3 | Anneau s'étendant d'une case par tour ; blesse les unités, soigne les boss",
        "lore": "Un anneau de corruption rose qui s'étend d'une case à chaque strophe, rongeant les unités et abreuvant les boss.",
        "variants": [
            {"suffix": "anneau_rose", "seed": 320001, "prompt": "Game action card tile icon, expanding rose corruption ring (#D96E9A) with an outward pulse arrow on paper, sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "anneau_magenta", "seed": 320002, "prompt": "Game action card tile icon, growing magenta ring (#B83A7A) with crackle fractures spreading outward, sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "anneau_concentrique", "seed": 320003, "prompt": "Game action card tile icon, concentric pulsing corruption circles in rose-magenta (#E07AA8) radiating outward, sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Vague de Souillure (Corrupted Wave)",
        "category": "Corruption balayant le terrain (Boss)",
        "stats": "Dégâts: 1 | Vague traversant le terrain d'un bord à l'autre ; gauche/droite",
        "lore": "Une lame de fond corrompue balayant tout le parchemin d'un bord à l'autre, noyant les unités.",
        "variants": [
            {"suffix": "vague_gauche", "seed": 321001, "prompt": "Game action card tile icon, horizontal rose corruption wave sweeping leftward across the lane (#D96E9A) with an arrow direction, sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "vague_droite", "seed": 321002, "prompt": "Game action card tile icon, magenta corruption tide sweeping rightward across the grid (#B83A7A) with an arrow direction, sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "vague_bidir", "seed": 321003, "prompt": "Game action card tile icon, bidirectional rose-magenta corruption waves crashing inward from both edges (#E07AA8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Éclosion de Souillure (Corrupted Explosion)",
        "category": "Corruption totale des cases (Boss)",
        "stats": "Dégâts: 1 | Explosion corrompue touchant toutes les cases",
        "lore": "Une éclosion rose explosant sur l'ensemble des cases, embrasement total du songe.",
        "variants": [
            {"suffix": "eclosion_radiale", "seed": 322001, "prompt": "Game action card tile icon, radial rose corruption explosion covering all grid nodes (#D96E9A), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "eclosion_fleur", "seed": 322002, "prompt": "Game action card tile icon, magenta corruption bloom detonation with spreading filaments (#B83A7A), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "eclosion_etoile", "seed": 322003, "prompt": "Game action card tile icon, rose corruption starburst flares erupting across multiple cells (#E07AA8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
    {
        "name": "Pouls de Souillure (Corrupted Pulse)",
        "category": "Corruption alternée par parité (Boss)",
        "stats": "Dégâts: 3 | Rayons alternant cases impaires/paires à chaque tour",
        "lore": "Des rayons corrompus palpitants qui alternent entre cases paires et impaires à chaque strophe.",
        "variants": [
            {"suffix": "pouls_faisceaux", "seed": 323001, "prompt": "Game action card tile icon, vertical rose corruption beams striking alternating grid cells with odd-even markers (#D96E9A), sumi-e ink wash on warm parchment (#EDE4D3), enemy tile icon"},
            {"suffix": "pouls_damier", "seed": 323002, "prompt": "Game action card tile icon, magenta pulse rays over a checkerboard of glowing cells (#B83A7A), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon"},
            {"suffix": "pouls_parite", "seed": 323003, "prompt": "Game action card tile icon, corruption beams alternating with parity glyphs and rose flares (#E07AA8), sumi-e ink wash on warm parchment (#EDE4D3), minimalist icon"},
        ],
    },
]


def generate_non_obtenables():
    total = sum(len(t["variants"]) for t in NON_OBTENABLES)
    print(f"Starting generation of {len(NON_OBTENABLES)} non-obtainable tiles x variants ({total} images) to {OUTPUT_DIR}...")
    done = 0
    for n_idx, tile in enumerate(NON_OBTENABLES, 1):
        for v_idx, variant in enumerate(tile["variants"], 1):
            done += 1
            filename = f"tuileN_{n_idx:02d}_{variant['suffix']}_v{v_idx}.png"
            out_path = os.path.join(OUTPUT_DIR, filename)

            if os.path.exists(out_path):
                print(f"[{done}/{total}] SKIP (exists) N{n_idx:02d} v{v_idx} '{tile['name']}' -> {filename}")
                continue

            payload = dict(COMMON_PARAMS)
            payload["prompt"] = variant["prompt"]
            payload["seed"] = variant["seed"]

            print(f"[{done}/{total}] Generating N{n_idx:02d} v{v_idx} '{tile['name']}' (seed={variant['seed']}) -> {filename}...")
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


def rebuild_catalogue():
    readme_path = os.path.join(OUTPUT_DIR, "README.md")
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write("# Catalogue des Tuiles d'Action — STARFALL: L'Atlas des Songes Éteints\n\n")
        f.write("Catalogue officiel des tuiles d'attaque, de déplacement et d'action du jeu, adaptées des mécaniques de *Shogun Showdown* vers l'univers **L'Atlas des Songes Éteints**.\n\n")
        f.write("## Configuration de Génération (Forge Neo) :\n")
        f.write("- **Serveur** : `http://undefined.egaetan.me:7862/`\n")
        f.write("- **Modèle** : `krea2_turbo_nvfp4 [61527003b2]`\n")
        f.write("- **Modules** : `wan_2.1_vae.safetensors`, `qwen3VLInstruct4bHeretic_int8Convrot.safetensors`\n")
        f.write("- **Format** : `1024x1024` (Icônes carrées 1:1 pour cartes d'action)\n")
        f.write("- **Palette obtenables** : papier chaud (`#EDE4D3`), acier étoilé (`#EAF2F8`), ambre (`#FF9A4D`), vermillon (`#C8382E`).\n")
        f.write("- **Palette corruption** : rose (`#D96E9A`), magenta profond (`#B83A7A`), rose clair (`#E07AA8`).\n")
        f.write("- **Scripts** : `tools/generate_tuiles.py` (1-18), `tools/generate_tuiles_2.py` (19-39), `tools/generate_tuiles_3.py` (non-obtenables N01-N13, 3 variantes chacune)\n")
        f.write("- **Note** : `tools/generate_tuiles_3.py` est le générateur canonique du README complet (obtenables + non-obtenables).\n\n")
        f.write("---\n\n")

        f.write("## Galerie des Tuiles Obtenables (1-39)\n\n")
        for idx, item in enumerate(OBTENABLES, 1):
            filepath = os.path.join(OUTPUT_DIR, item["filename"]).replace('\\', '/')
            f.write(f"### {idx}. {item['name']}\n")
            f.write(f"- **Type** : `{item['type']}`\n")
            f.write(f"- **Statistiques** : `{item['stats']}`\n")
            f.write(f"- **Lore** : *{item['lore']}*\n")
            f.write(f"- **Fichier** : [{item['filename']}](file:///{filepath})\n")
            f.write(f"- **Prompt SD** : `{item['prompt']}`\n\n")
            f.write(f"![{item['name']}](file:///{filepath})\n\n")
            f.write("---\n\n")

        f.write("## Galerie des Tuiles Non-Obtenables (Ennemis / Boss) — 3 variantes par tuile\n\n")
        for n_idx, tile in enumerate(NON_OBTENABLES, 1):
            f.write(f"### N{ n_idx:02d}. {tile['name']}\n")
            f.write(f"- **Catégorie** : `{tile['category']}`\n")
            f.write(f"- **Statistiques** : `{tile['stats']}`\n")
            f.write(f"- **Lore** : *{tile['lore']}*\n\n")
            for v_idx, variant in enumerate(tile["variants"], 1):
                filename = f"tuileN_{n_idx:02d}_{variant['suffix']}_v{v_idx}.png"
                filepath = os.path.join(OUTPUT_DIR, filename).replace('\\', '/')
                f.write(f"#### Variante {v_idx} — `{variant['suffix']}` (seed {variant['seed']})\n")
                f.write(f"- **Fichier** : [{filename}](file:///{filepath})\n")
                f.write(f"- **Prompt SD** : `{variant['prompt']}`\n\n")
                f.write(f"![{tile['name']} v{v_idx}](file:///{filepath})\n\n")
            f.write("---\n\n")

    total_img = len(OBTENABLES) + sum(len(t["variants"]) for t in NON_OBTENABLES)
    print(f"Catalogue rebuilt: {len(OBTENABLES)} obtenables + {sum(len(t['variants']) for t in NON_OBTENABLES)} non-obtenables ({total_img} images) -> {readme_path}")


if __name__ == "__main__":
    generate_non_obtenables()
    rebuild_catalogue()
