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
        "filename": "maquette_08_misty_meadow_fight.png",
        "title": "Maquette 08 — Duel en Brume Matinale (Family C Strict)",
        "seed": 555111,
        "prompt": "Traditional sumi-e ink wash painting of a duel in a pale fog-filled meadow at dusk, soft pink and cream sky, dark ink duellist silhouettes in bruised blues and cool greys dissolving into heavy white fog at ground level, tiny glowing cyan and magenta bokeh light motes floating in air, soft painterly contemplative atmosphere, warm paper ground texture, minimal game UI overlay"
    },
    {
        "filename": "maquette_08_variant2.png",
        "title": "Maquette 08 (Variante 2) — Brume & Joyaux Lumineux",
        "seed": 666222,
        "prompt": "Minimalist ink wash artwork, two swordsmen in profile standing in a misty meadow at twilight, pink salmon sky, figures half-dissolving into pale fog wash, floating out-of-focus cyan magenta amber jewel light motes, serene painterly aesthetic, paper texture, subtle red ink mark on ground tile"
    },
    {
        "filename": "maquette_08_variant3.png",
        "title": "Maquette 08 (Variante 3) — Évanescence dans la Clairière",
        "seed": 777333,
        "prompt": "Sumi-e watercolor painting, atmospheric misty meadow at dusk, two dark ink warrior figures surrounded by thick horizontal drifting fog bands, soft cream and pink sky, pale glowing white sword sliver, drifting jewel bokeh lights, contemplative dreamlike mood, paper ground (#EDE4D3)"
    }
]

def main():
    print("Generating 3 candidates for Maquette 08 (Strict STYLE.md Family C)...")
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
