import os
import json
import base64
import urllib.request

from generate_tuiles import TUILES, COMMON_PARAMS, SERVER_URL, OUTPUT_DIR
from generate_tuiles_2 import NEW_TUILES
from generate_tuiles_3 import NON_OBTENABLES

os.makedirs(OUTPUT_DIR, exist_ok=True)

OBTENABLES = TUILES + NEW_TUILES
BASE_SEED = COMMON_PARAMS["seed"]

OBTENABLE_VARIANTS = {
    1: (
        "Game action card tile icon, diagonal falling katana slash with luminous blue-white ink spray (#EAF2F8), macro focus on the glowing blade edge, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), dark ink brush border, minimalist game UI icon",
        "Game action card tile icon, twin parallel calligraphic sword strokes crossing in an X of luminous ink (#EAF2F8), expressive wet brush, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design"
    ),
    2: (
        "Game action card tile icon, side-swing heavy warhammer sweeping horizontally with an amber shockwave ring (#FF9A4D), motion arc, sumi-e ink wash painting on warm parchment paper ground (#EDE4D3), dark ink splatter, minimalist game UI icon",
        "Game action card tile icon, overhead warhammer impact crater with radiating amber cracks (#FF9A4D) and ink debris, sumi-e ink wash painting on warm parchment paper ground (#EDE4D3), minimalist game UI icon design"
    ),
    3: (
        "Game action card tile icon, spear thrust drawn in profile with motion streaks impaling two glowing ink nodes (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), dark brush strokes, game icon UI",
        "Game action card tile icon, close-up of a glowing spear tip with a trailing ink ribbon and two pierced node markers, sumi-e ink wash style on warm paper ground (#EDE4D3), luminous blade tip (#EAF2F8), game icon UI"
    ),
    4: (
        "Game action card tile icon, asymmetric double-arc brush swirl, larger front arc and smaller rear arc of black ink and glowing starlight (#EAF2F8), sumi-e wash style on warm cream paper ground (#EDE4D3), elegant minimalist UI icon",
        "Game action card tile icon, spiral galaxy-like ink swirl with orbiting stardust flecks (#EAF2F8), sumi-e wash style on warm cream paper ground (#EDE4D3), elegant minimalist UI icon"
    ),
    5: (
        "Game action card tile icon, three glowing shuriken stars flying in sequence through dark ink mist (#EAF2F8), amber sparks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game icon",
        "Game action card tile icon, single starlight arrow with feathered ink fletching and a long glowing trail (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), floating amber embers, game icon"
    ),
    6: (
        "Game action card tile icon, forked twin cyan lightning bolts (#5FD8E8) striking down simultaneously from an ink storm cloud, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game icon design",
        "Game action card tile icon, crackling ball lightning sphere of cyan starlight (#5FD8E8) with radiating sparks, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game icon design"
    ),
    7: (
        "Game action card tile icon, forward glowing fist impact with concentric push rings and a directional knockback arrow (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist clean UI icon design",
        "Game action card tile icon, open telekinetic palm emitting a directional gust of ink wind with push arrows, amber dust (#FF9A4D), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist clean UI icon design"
    ),
    8: (
        "Game action card tile icon, ink smoke bomb pellet bursting mid-air into swirling black and white fog with swap arrows, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon",
        "Game action card tile icon, two half-dissolved ink silhouettes swapping positions through dense fog, intersecting arrows (#EAF2F8), sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon"
    ),
    9: (
        "Game action card tile icon, multiple ghostly afterimage silhouettes strung along a forward dash path with wet black ink and starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon",
        "Game action card tile icon, top-down shadow dash with an ink comet trail streaking forward, glowing particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon"
    ),
    10: (
        "Game action card tile icon, ominous vermillion cursed eye symbol (#C8382E) with glowing rune lashes, sumi-e ink wash style on warm paper ground (#EDE4D3), striking red accent, minimalist game UI icon",
        "Game action card tile icon, darkening vermillion handprint seal (#C8382E) oozing cursed ink, sumi-e ink wash style on warm paper ground (#EDE4D3), striking red accent, minimalist game UI icon"
    ),
    11: (
        "Game action card tile icon, meteor shower of small star fragments raining amber embers (#FF9A4D) and ink flecks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game UI icon design",
        "Game action card tile icon, single large blazing suisei sphere ringed by an amber shockwave on impact (#FF9A4D), ink flecks, sumi-e ink wash painting on warm paper ground (#EDE4D3), game UI icon design"
    ),
    12: (
        "Game action card tile icon, sword blade with three stacked glowing charge glyphs intensifying in brightness (#EAF2F8) inside an hourglass ink aura, sumi-e wash style on warm paper ground (#EDE4D3), game UI icon",
        "Game action card tile icon, scabbarded blade quietly collecting ambient starlight into its guard (#EAF2F8), serene anticipation, sumi-e wash style on warm paper ground (#EDE4D3), game UI icon"
    ),
    13: (
        "Game action card tile icon, subtle hidden trip-wire ink trap with a faint glowing trigger mark, red threat accent (#C8382E), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist UI icon",
        "Game action card tile icon, blooming ink thorn bush barrier with sharp crimson-tipped barbs (#C8382E), sumi-e ink wash style on warm parchment (#EDE4D3), minimalist UI icon"
    ),
    14: (
        "Game action card tile icon, eightfold symmetric mandala compass rose in gold and dark ink with a glowing center star (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon",
        "Game action card tile icon, mirror-perfect left-right ink reflection forming a symmetric compass seal with a radiant core, sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon"
    ),
    15: (
        "Game action card tile icon, polished ink mirror surface with a single hand reaching through a silver portal frame (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, round bronze mirror gleaming with a starlight reflection and swap arrows (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), minimalist game UI icon"
    ),
    16: (
        "Game action card tile icon, three concentric spinning chakram rings of glowing stardust motes and ink trails (#EAF2F8), sumi-e wash style on warm parchment paper (#EDE4D3), elegant UI icon",
        "Game action card tile icon, single chakram ring etched with tally marks counting defeated silhouettes, glowing stardust (#EAF2F8), sumi-e wash style on warm parchment paper (#EDE4D3), elegant UI icon"
    ),
    17: (
        "Game action card tile icon, hooked starlight thread anchored into a dark target icon with a taut pulling line (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist clean UI icon",
        "Game action card tile icon, web of fine starlight silk threads pulling several target icons closer at once (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist clean UI icon"
    ),
    18: (
        "Game action card tile icon, side profile of an ink matchlock pistol firing with an ignition spark and starlight flash (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon design",
        "Game action card tile icon, cannon recoil blast pushing attacker and target silhouettes apart with opposite arrows (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), game UI icon design"
    ),
    19: (
        "Game action card tile icon, two sai daggers crossed in a parry catching an incoming glowing blade, counter-burst (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design",
        "Game action card tile icon, single raised sai radiating a ready counter-stance aura of blue-white energy (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design"
    ),
    20: (
        "Game action card tile icon, curved hook blade mid-refollow chain linking two targets with a forward arrow (#EAF2F8), sumi-e ink wash painting on warm paper ground (#EDE4D3), elegant game UI icon",
        "Game action card tile icon, curved hook blade dripping starlight along its inner edge after a fatal pull (#EAF2F8), amber accent (#FF9A4D), sumi-e ink wash painting on warm paper ground (#EDE4D3), elegant game UI icon"
    ),
    21: (
        "Game action card tile icon, bo staff spinning vertically to flip an enemy silhouette end over end, circular flip arrow, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, low sweeping bo thrust with a rotation turn arrow and dust kick, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon"
    ),
    22: (
        "Game action card tile icon, over-the-shoulder backstab silhouette thrusting a glowing blade backward (#EAF2F8), reverse arrow, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist UI icon",
        "Game action card tile icon, rear blade gleam glinting behind a cloaked figure, luminous reverse slash (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist UI icon"
    ),
    23: (
        "Game action card tile icon, pair of war fans opening wide in opposite directions with amber gust arrows (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant UI icon",
        "Game action card tile icon, single folding war fan emphasized with razor steel ribs and a sharp slash arc (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant UI icon"
    ),
    24: (
        "Game action card tile icon, phantom kama reaching across a wide gap along a dashed arc to a far node (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, twin phantom kama crossing at distance over an ignored middle tile, dashed lines (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon"
    ),
    25: (
        "Game action card tile icon, long ink pole piercing four nodes front and back wrapped in a soft protective wisp (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon",
        "Game action card tile icon, merciful staff stopping short with a glowing spared 1-HP spark icon at the tip (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon"
    ),
    26: (
        "Game action card tile icon, ink stalagmite spikes erupting diagonally at distance two front and back (#EAF2F8), sumi-e ink wash painting on warm parchment (#EDE4D3), game UI icon",
        "Game action card tile icon, earthen pillar impale with billowing ink dust clouds at two distance markers (#EAF2F8), sumi-e ink wash painting on warm parchment (#EDE4D3), game UI icon"
    ),
    27: (
        "Game action card tile icon, spiked sphere mid-bounce arcing behind the user on an ink chain, amber trail (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon",
        "Game action card tile icon, chain pulled taut with the heavy sphere at maximum forward reach, ink tension lines, amber flash (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon"
    ),
    28: (
        "Game action card tile icon, fan-spread of kunai thrown in a widening arc of glowing blue-white glass (#EAF2F8), sumi-e ink wash style on warm parchment paper (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, several kunai embedded in a single front target with glowing hit counts, blue-white glass (#EAF2F8), sumi-e ink wash style on warm parchment paper (#EDE4D3), minimalist game UI icon"
    ),
    29: (
        "Game action card tile icon, golden coin spinning with a light trail before converting into a radiant strike beam (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, stack of three golden coins fueling a larger amplified strike beam, star sigil (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon"
    ),
    30: (
        "Game action card tile icon, crossbow mid-reload with a glowing taut bowstring of starlight (#EAF2F8), ink bolts, sumi-e ink wash style on warm parchment ground (#EDE4D3), game UI icon",
        "Game action card tile icon, heavy bolt mid-flight piercing two aligned silhouettes with a starlight trail (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), game UI icon"
    ),
    31: (
        "Game action card tile icon, forward charge leaving a comet of ink with a braced shoulder into the first target, amber impact (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, dashing ink silhouette lowering its shoulder to ram the first target, motion blur, amber burst (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon"
    ),
    32: (
        "Game action card tile icon, ink silhouette rushing backward with a glance over the shoulder and a reverse motion trail, amber impact (#FF9A4D), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon",
        "Game action card tile icon, backward charge kicking up an ink dust plume on the way to a rear target, amber impact (#FF9A4D), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon"
    ),
    33: (
        "Game action card tile icon, ghostly afterimages trailing backward through rear enemies with wet ink and starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon",
        "Game action card tile icon, silhouette dissolving into an ink pool to reappear behind a rear foe, starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon"
    ),
    34: (
        "Game action card tile icon, ink smoke pellet rolling behind before bursting into a swap cloud, backward arrows, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon",
        "Game action card tile icon, fog tendrils wrapping two silhouettes swapping rear positions (#EAF2F8), sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon"
    ),
    35: (
        "Game action card tile icon, 180-degree pivot slash with centrifugal ink arcs around the center silhouette, luminous blue-white (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, turning heel-stomp creating a radial shock of ink slashes around the pivot point, blue-white arcs (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon"
    ),
    36: (
        "Game action card tile icon, hero sigil unfolding like a blooming starlight seal with amber spark accents (#EAF2F8, #FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), elegant minimalist game UI icon",
        "Game action card tile icon, calligraphy brush painting the hero's special sigil in real time with glowing wet strokes (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), elegant minimalist game UI icon"
    ),
    37: (
        "Game action card tile icon, hourglass of starlight with two glowing icons falling through and swapping front/back (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, two mirrored cabinets exchanging their front and back contents through crossing arrows (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon"
    ),
    38: (
        "Game action card tile icon, long horizontal ink dash streak with start and end markers and a starlight comet tail (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, forward dash trail arcing slightly upward like a skipped stone of ink, starlight comet streak (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon"
    ),
    39: (
        "Game action card tile icon, jagged scar slashes opening only across cracked-wound silhouettes, vermillion accents (#C8382E), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon",
        "Game action card tile icon, single seeking jagged scar homing toward a bleeding target with vermillion glow (#C8382E), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon"
    ),
}


def base_slug(filename):
    return filename[:-4] if filename.endswith(".png") else filename


def generate_obtenable_variants():
    total = len(OBTENABLES) * 2
    print(f"Starting generation of obtainable variants ({len(OBTENABLES)} tiles x 2 = {total} images) to {OUTPUT_DIR}...")
    done = 0
    for idx, item in enumerate(OBTENABLES, 1):
        slug = base_slug(item["filename"])
        v2_prompt, v3_prompt = OBTENABLE_VARIANTS[idx]
        variants = [
            (f"{slug}_v2.png", 100000 + idx, v2_prompt),
            (f"{slug}_v3.png", 200000 + idx, v3_prompt),
        ]
        for filename, seed, prompt in variants:
            done += 1
            out_path = os.path.join(OUTPUT_DIR, filename)
            if os.path.exists(out_path):
                print(f"[{done}/{total}] SKIP (exists) '{item['name']}' -> {filename}")
                continue

            payload = dict(COMMON_PARAMS)
            payload["prompt"] = prompt
            payload["seed"] = seed

            print(f"[{done}/{total}] Generating '{item['name']}' variant (seed={seed}) -> {filename}...")
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
        f.write("- **Variante** = 3 illustrations candidate par tuile (seed + prompt distincts).\n")
        f.write("- **Scripts** : `generate_tuiles.py` (1-18 v1), `generate_tuiles_2.py` (19-39 v1), `generate_tuiles_3.py` (non-obtenables), `generate_tuiles_4.py` (obtenables v2/v3 + README canonique).\n\n")
        f.write("---\n\n")

        f.write("## Galerie des Tuiles Obtenables (1-39) — 3 variantes par tuile\n\n")
        for idx, item in enumerate(OBTENABLES, 1):
            slug = base_slug(item["filename"])
            v1_path = os.path.join(OUTPUT_DIR, item["filename"]).replace('\\', '/')
            v2_file = f"{slug}_v2.png"
            v3_file = f"{slug}_v3.png"
            v2_prompt, v3_prompt = OBTENABLE_VARIANTS[idx]
            v2_path = os.path.join(OUTPUT_DIR, v2_file).replace('\\', '/')
            v3_path = os.path.join(OUTPUT_DIR, v3_file).replace('\\', '/')

            f.write(f"### {idx}. {item['name']}\n")
            f.write(f"- **Type** : `{item['type']}`\n")
            f.write(f"- **Statistiques** : `{item['stats']}`\n")
            f.write(f"- **Lore** : *{item['lore']}*\n\n")

            f.write(f"#### Variante 1 — canonique (seed {BASE_SEED})\n")
            f.write(f"- **Fichier** : [{item['filename']}](file:///{v1_path})\n")
            f.write(f"- **Prompt SD** : `{item['prompt']}`\n\n")
            f.write(f"![{item['name']} v1](file:///{v1_path})\n\n")

            f.write(f"#### Variante 2 (seed {100000 + idx})\n")
            f.write(f"- **Fichier** : [{v2_file}](file:///{v2_path})\n")
            f.write(f"- **Prompt SD** : `{v2_prompt}`\n\n")
            f.write(f"![{item['name']} v2](file:///{v2_path})\n\n")

            f.write(f"#### Variante 3 (seed {200000 + idx})\n")
            f.write(f"- **Fichier** : [{v3_file}](file:///{v3_path})\n")
            f.write(f"- **Prompt SD** : `{v3_prompt}`\n\n")
            f.write(f"![{item['name']} v3](file:///{v3_path})\n\n")
            f.write("---\n\n")

        f.write("## Galerie des Tuiles Non-Obtenables (Ennemis / Boss) — 3 variantes par tuile\n\n")
        for n_idx, tile in enumerate(NON_OBTENABLES, 1):
            f.write(f"### N{n_idx:02d}. {tile['name']}\n")
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

    total_img = len(OBTENABLES) * 3 + sum(len(t["variants"]) for t in NON_OBTENABLES)
    print(f"Catalogue rebuilt: {len(OBTENABLES)} obtenables x3 + {sum(len(t['variants']) for t in NON_OBTENABLES)} non-obtenables ({total_img} images) -> {readme_path}")


if __name__ == "__main__":
    generate_obtenable_variants()
    rebuild_catalogue()
