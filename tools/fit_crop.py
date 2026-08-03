"""Crop a box from an image, optionally upscale, save. Prints luminance stats.

Usage: python tools/fit_crop.py <img> <x0> <y0> <x1> <y1> <out> [scale]
"""
import sys

import numpy as np
from PIL import Image

img = Image.open(sys.argv[1]).convert("RGB")
x0, y0, x1, y1 = (int(v) for v in sys.argv[2:6])
out = sys.argv[6]
scale = int(sys.argv[7]) if len(sys.argv) > 7 else 1
crop = img.crop((x0, y0, x1, y1))
a = np.asarray(crop, dtype=np.float64)
lum = 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]
print(f"region x{x0}..{x1} y{y0}..{y1}  lum min {lum.min():.1f}  p2 {np.percentile(lum,2):.1f}  "
      f"median {np.median(lum):.1f}  p98 {np.percentile(lum,98):.1f}  max {lum.max():.1f}")
if scale > 1:
    crop = crop.resize((crop.width * scale, crop.height * scale), Image.NEAREST)
crop.save(out)
