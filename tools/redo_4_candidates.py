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
    "width": 1920,
    "height": 1088,
    "negative_prompt": "3d render, CGI, digital 3d, photorealistic, photograph, anime, manga, comic, sharp edges, vibrant saturation, neon glow, crowded, noisy, watermark, ugly, detailed face",
    "override_settings": {
        "sd_model_checkpoint": "krea2_turbo_nvfp4.safetensors",
    }
}

CANDIDATES = [
    {
        "filename": "maquette_04_execution_parry.png",
        "title": "Maquette 04 — Zoom Exécution & Clash de Lames (Family B Strict)",
        "seed": 777123,
        "prompt": "Traditional ink wash painting of two swordsmen in profile at dusk, dark indigo ink silhouettes, luminous white blade sliver, soft star bloom light at contact point, floating warm embers, wet black ink clouds dissolving off cloth extremities, sky grading from deep indigo to violet to warm coral salmon horizon, warm cream paper ground (#EDE4D3), sumi-e painterly style, minimalist game UI"
    },
    {
        "filename": "maquette_04_variant2.png",
        "title": "Maquette 04 (Variante 2) — Clash d'Encre au Crépuscule",
        "seed": 888456,
        "prompt": "Sumi-e ink painting, dusk duel, two dark ink warrior silhouettes crossing swords, pale luminous blade (#EAF2F8), warm embers scattering (#FF9A4D), wet ink wash bleed, sky gradient indigo to coral, paper texture, minimal soft light bloom at blade clash, elegant fluid brushstrokes"
    },
    {
        "filename": "maquette_04_variant3.png",
        "title": "Maquette 04 (Variante 3) — Ombre et Lumière au Contact",
        "seed": 999789,
        "prompt": "Minimalist ink wash artwork, two swordsmen in profile clashing blades, dark blue-black ink figures dissolving into smoke at feet, soft star light bloom at sword contact, warm orange embers floating upward, dusk sky gradient violet and salmon coral, warm paper ground, serene painterly aesthetic"
    }
]

def main():
    print("Generating 3 candidates for Maquette 04 (Strict STYLE.md Family B)...")
    for item in CANDIDATES:
        filename = item["filename"]
        title = item["title"]
        prompt = item["prompt"]
        seed = item["seed"]
        out_path = os.path.join(OUTPUT_DIR, filename)

        payload = dict(COMMON_PARAMS)
        payload["prompt"] = prompt
        payload["seed"] = seed

        print(f"Generating '{title}' (seed={seed}) -> {filename}...")
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

if __name__ == "__main__":
    main()
