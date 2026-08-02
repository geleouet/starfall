import os
import json
import base64
import urllib.request

from generate_tuiles import SERVER_URL, OUTPUT_DIR, COMMON_PARAMS, TUILES

os.makedirs(OUTPUT_DIR, exist_ok=True)

NEW_TUILES = [
    {
        "filename": "tuile_19_crocs_de_la_nuit.png",
        "name": "Crocs de la Nuit (Sai)",
        "type": "Frappe réactive (Dégâts doublés si la cible s'apprête à attaquer)",
        "stats": "Dégâts: 2 (x2 si attaque ennemie) | Cooldown: 5 | Coût: 15 Crânes",
        "lore": "Deux dards d'encre parallèles qui résonnent et s'embrasent face à une intention hostile.",
        "prompt": "Game action card tile icon, twin-pronged sai dagger drawn in dark ink with glowing blue-white counter-stance energy (#EAF2F8), reactive parry burst radiating outward, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon design"
    },
    {
        "filename": "tuile_20_faucille_recurrente.png",
        "name": "Faucille Récurrente (Hookblade)",
        "type": "Frappe en chaîne (Refrappe sur coup fatal)",
        "stats": "Dégâts: 2 | Cooldown: 5 | Coût: 30 Crânes",
        "lore": "Une lame courbe d'étoile qui, après avoir fauché son ombre, se projette en avant pour une seconde entaille.",
        "prompt": "Game action card tile icon, curved hook blade with glowing starlight edge (#EAF2F8) chaining into a forward follow-up slash arrow, sumi-e ink wash painting on warm paper ground (#EDE4D3), elegant game UI icon"
    },
    {
        "filename": "tuile_21_baton_du_renversement.png",
        "name": "Bâton du Renversement (Bo)",
        "type": "Frappe retournante (Retourne la cible)",
        "stats": "Dégâts: 1 | Cooldown: 5 | Coût: 25 Crânes",
        "lore": "Un long bâton d'encre qui bascule la silhouette adverse pour l'exposer au plein jour.",
        "prompt": "Game action card tile icon, long wooden bo staff sweeping in ink arc with a circular flip-turn arrow icon, sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_22_revers_etoile.png",
        "name": "Revers Étoilé (Back Strike)",
        "type": "Frappe vers l'arrière (Derrière soi)",
        "stats": "Dégâts: 3 | Cooldown: 3 | Coût: 15 Crânes",
        "lore": "Un coup porté à rebours, traçant un sillage lumineux dans l'encre derrière le Pèlerin.",
        "prompt": "Game action card tile icon, glowing sword slash striking backward with reverse directional arrow in luminous blue-white ink (#EAF2F8), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist UI icon"
    },
    {
        "filename": "tuile_23_eventails_jumeaux.png",
        "name": "Éventails Jumeaux (Twin Tessen)",
        "type": "Frappe frontale et arrière avec repoussement",
        "stats": "Dégâts: 1 | Cooldown: 6 | Coût: 20 Crânes",
        "lore": "Deux éventails de guerre en papier étoilé qui repoussent les ombres de part et d'autre du Pèlerin.",
        "prompt": "Game action card tile icon, pair of folding war fans painted in ink pushing outward in both directions with amber shockwave gusts (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant UI icon"
    },
    {
        "filename": "tuile_24_faux_d_ombre.png",
        "name": "Faux d'Ombre (Shadow Kama)",
        "type": "Frappe à distance 2 (Ignore la case intermédiaire)",
        "stats": "Dégâts: 3 | Cooldown: 3 | Coût: 6 Crânes",
        "lore": "Une faucille d'encre fantôme qui fauche une case lointaine sans effleurer l'espace intermédiaire.",
        "prompt": "Game action card tile icon, ghostly kama sickle reaching a far glowing node two tiles ahead while ignoring the middle tile, dashed phantom ink line, sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_25_pilier_de_brume.png",
        "name": "Pilier de Brume (Nagiboku)",
        "type": "Frappe perçante 4 cases (Laisse toujours 1 PV)",
        "stats": "Dégâts: 2 | Cooldown: 5 | Coût: 30 Crânes",
        "lore": "Un long pieu d'encre qui traverse quatre cases sans jamais porter le coup de grâce, épargnant une étincelle de vie.",
        "prompt": "Game action card tile icon, long ink staff piercing four nodes front and back with soft restraint halo, blue-white life spark remaining (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon"
    },
    {
        "filename": "tuile_26_empalement_terrestre.png",
        "name": "Empalement Terrestre (Earth Impale)",
        "type": "Frappe à distance 2 devant et derrière",
        "stats": "Dégâts: 2 | Cooldown: 4 | Coût: 20 Crânes",
        "lore": "Des pieux d'encre jaillissant du parchemin à distance de part et d'autre, transperçant les cases éloignées.",
        "prompt": "Game action card tile icon, ink stalagmite spikes erupting from paper ground at distance two front and back, glowing blue-white tips (#EAF2F8), sumi-e ink wash painting on warm parchment (#EDE4D3), game UI icon"
    },
    {
        "filename": "tuile_27_marteau_cometique.png",
        "name": "Marteau Cometique (Meteor Hammer)",
        "type": "Frappe à portée 3 puis rebond arrière",
        "stats": "Dégâts: 2 | Cooldown: 5 | Coût: 25 Crânes",
        "lore": "Un orbe d'encre céleste qui frappe au loin puis rebondit pour filer dans l'ombre derrière le Pèlerin.",
        "prompt": "Game action card tile icon, spiked sphere on ink chain striking forward then curving back behind with amber motion trail (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), elegant game UI icon"
    },
    {
        "filename": "tuile_28_volee_de_kunai.png",
        "name": "Volée de Kunai (Kunai)",
        "type": "Frappe à projectiles multiples (1 dégât chacun)",
        "stats": "Dégâts: 2 | Cooldown: 7 | Coût: 20 Crânes",
        "lore": "Une volée de dards de verre étoilé lacérant la première ombre, chacun ne portant qu'une éraflure légère.",
        "prompt": "Game action card tile icon, flurry of three to five small throwing knives of glowing blue-white glass (#EAF2F8) converging on a front target, sumi-e ink wash style on warm parchment paper (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_29_piece_du_serment.png",
        "name": "Pièce du Serment (Mon)",
        "type": "Frappe lourde à consommation d'or",
        "stats": "Dégâts: 5 | Cooldown: 7 | Coût: 20 Crânes (dépense 1 pièce)",
        "lore": "Une pièce d'or gravée d'un sceau étoilé, sacrifiée au Cosmo-Atlas pour déchaîner une frappe dévastatrice.",
        "prompt": "Game action card tile icon, glowing golden coin seal with star sigil converting into a heavy radiant strike beam (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_30_arbalete_d_etoiles.png",
        "name": "Arbalète d'Étoiles (Crossbow)",
        "type": "Projectile perçant 2 cibles (Se recharge en réutilisant)",
        "stats": "Dégâts: 3 | Cooldown: 5 | Coût: 15 Crânes",
        "lore": "Un carreau d'encre perçant deux ombres alignées, dont la corde se retend à chaque tir répété.",
        "prompt": "Game action card tile icon, ink crossbow firing a piercing bolt through two aligned glowing target nodes (#EAF2F8), taut bowstring of starlight, sumi-e ink wash style on warm parchment ground (#EDE4D3), game UI icon"
    },
    {
        "filename": "tuile_31_charge_du_pelerin.png",
        "name": "Charge du Pèlerin (Charge)",
        "type": "Déplacement + Frappe avant",
        "stats": "Dégâts: 1 | Cooldown: 4 (Tuile de base)",
        "lore": "Une ruée d'encre emportant le Pèlerin jusqu'à la première ombre pour l'ébranler.",
        "prompt": "Game action card tile icon, forward dashing ink silhouette with motion blur trail striking the first target with amber impact burst (#FF9A4D), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_32_charge_versatile.png",
        "name": "Charge Versatile (Back Charge)",
        "type": "Déplacement + Frappe arrière",
        "stats": "Dégâts: 1 | Cooldown: 3 (Tuile de base)",
        "lore": "Une ruée inversée, le Pèlerin fonçant dans son sillage d'encre pour heurter l'ombre qui le suit.",
        "prompt": "Game action card tile icon, ink silhouette dashing backward with reverse motion trail striking a rear target, amber impact (#FF9A4D), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist UI icon"
    },
    {
        "filename": "tuile_33_glissement_posterieur.png",
        "name": "Glissement Postérieur (Back Shadow Dash)",
        "type": "Traversée arrière (Mobilité)",
        "stats": "Dégâts: 1 | Cooldown: 5 | Coût: 20 Crânes",
        "lore": "Le Pèlerin se dissout en encre pour traverser les ombres qui le talonnent par-derrière.",
        "prompt": "Game action card tile icon, shadow silhouette dashing backward leaving wet black ink trail and glowing starlight particles (#EAF2F8), sumi-e ink wash painting on warm parchment paper, minimalist game UI icon"
    },
    {
        "filename": "tuile_34_brouillard_arriere.png",
        "name": "Brouillard Arrière (Back Smoke Bomb)",
        "type": "Transposition arrière",
        "stats": "Dégâts: 1 | Cooldown: 5 | Coût: 25 Crânes",
        "lore": "Une bouffée d'encre et de brume transposant le Pèlerin avec l'ombre qui le suit.",
        "prompt": "Game action card tile icon, swirling ink smoke cloud with two intersecting swap arrows pointing backward, sumi-e ink wash style on warm cream paper ground (#EDE4D3), elegant game UI icon"
    },
    {
        "filename": "tuile_35_pivot_tranchant.png",
        "name": "Pivot Tranchant (Sharp Turn - Free-Play)",
        "type": "Rotation + Frappe autour (Action Gratuite)",
        "stats": "Dégâts: 1 | Cooldown: 7 | Coût: 3 Crânes | Free-Play",
        "lore": "Une pirouette de plume pivotant le Pèlerin et déployant un cercle d'encre sur les cases adjacentes.",
        "prompt": "Game action card tile icon, spinning ink brush turn with radial slash marks around the center silhouette, luminous blue-white arcs (#EAF2F8), sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_36_sceau_du_heros.png",
        "name": "Sceau du Héros (Signature Move - Free-Play)",
        "type": "Capacité spéciale du héros (Action Gratuite)",
        "stats": "Dégâts: — | Cooldown: 6 | Coût: 25 Crânes | Free-Play",
        "lore": "Le glyphe personnel du Pèlerin, déchaînant son don unique en une calligraphie éphémère.",
        "prompt": "Game action card tile icon, radiant personal hero sigil of glowing starlight calligraphy (#EAF2F8) with amber spark accents (#FF9A4D), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), elegant minimalist game UI icon"
    },
    {
        "filename": "tuile_37_tiroir_de_songe.png",
        "name": "Tiroir de Songe (Swap Toss - Free-Play)",
        "type": "Échange des cases avant/arrière (Action Gratuite)",
        "stats": "Dégâts: — | Cooldown: 7 | Coût: 25 Crânes | Free-Play",
        "lore": "Un geste de plume intervertissant les ombres des cases avant et arrière, comme on retourne un sablier.",
        "prompt": "Game action card tile icon, two glowing ink nodes front and back swapping contents through crossing arrows, hourglass motif of starlight (#EAF2F8), sumi-e ink wash style on warm paper ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_38_fleche_du_vide.png",
        "name": "Flèche du Vide (Dash - Free-Play)",
        "type": "Déplacement maximal avant (Action Gratuite)",
        "stats": "Dégâts: — | Cooldown: 6 | Coût: 10 Crânes | Free-Play",
        "lore": "Une traînée d'encre pure propulsant le Pèlerin jusqu'au seuil de la case la plus lointaine.",
        "prompt": "Game action card tile icon, elongated ink dash trail with starlight comet streak (#EAF2F8) rushing forward across multiple tiles, sumi-e ink wash style on warm parchment ground (#EDE4D3), minimalist game UI icon"
    },
    {
        "filename": "tuile_39_cicatrice_stellaire.png",
        "name": "Cicatrice Stellaire (Scar Strike)",
        "type": "Frappe toutes les cibles blessées (PV non pleins)",
        "stats": "Dégâts: 1 | Cooldown: 5 | Coût: 10 Crânes",
        "lore": "Une entaille d'encre qui ne s'ouvre que sur les ombres déjà meurtries, recherchant leurs failles.",
        "prompt": "Game action card tile icon, jagged scar slash marks targeting wounded cracked ink silhouettes, vermillion wound accents (#C8382E), sumi-e ink wash style on warm parchment paper ground (#EDE4D3), minimalist game UI icon"
    },
]

ALL_TUILES = TUILES + NEW_TUILES


def generate_new():
    summary_data = []
    print(f"Starting generation of {len(NEW_TUILES)} new tile illustrations to {OUTPUT_DIR}...")
    for idx, item in enumerate(NEW_TUILES, start=len(TUILES) + 1):
        filename = item["filename"]
        name = item["name"]
        prompt = item["prompt"]
        out_path = os.path.join(OUTPUT_DIR, filename)

        if os.path.exists(out_path):
            print(f"[{idx}/{len(ALL_TUILES)}] SKIP (exists) tile '{name}' -> {filename}")
        else:
            payload = dict(COMMON_PARAMS)
            payload["prompt"] = prompt

            print(f"[{idx}/{len(ALL_TUILES)}] Generating tile '{name}' -> {filename}...")
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

    return summary_data


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
        f.write("- **Style** : Lavis d'encre sumi-e sur papier chaud (`#EDE4D3`), touches d'acier étoilé (`#EAF2F8`), ambre (`#FF9A4D`) et vermillon (`#C8382E`).\n")
        f.write("- **Scripts** : `tools/generate_tuiles.py` (tuiles 1-18), `tools/generate_tuiles_2.py` (tuiles 19-39)\n\n")
        f.write("---\n\n")
        f.write("## Galerie des Tuiles :\n\n")
        for idx, item in enumerate(ALL_TUILES, 1):
            filepath = os.path.join(OUTPUT_DIR, item["filename"]).replace('\\', '/')
            f.write(f"### {idx}. {item['name']}\n")
            f.write(f"- **Type** : `{item['type']}`\n")
            f.write(f"- **Statistiques** : `{item['stats']}`\n")
            f.write(f"- **Lore** : *{item['lore']}*\n")
            f.write(f"- **Fichier** : [{item['filename']}](file:///{filepath})\n")
            f.write(f"- **Prompt SD** : `{item['prompt']}`\n\n")
            f.write(f"![{item['name']}](file:///{filepath})\n\n")
            f.write("---\n\n")

    print(f"Catalogue rebuilt with {len(ALL_TUILES)} tiles -> {readme_path}")


if __name__ == "__main__":
    generate_new()
    rebuild_catalogue()
