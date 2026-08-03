"""Face-plane / local-sky luminance reader for System 3b pass 2.

Independent of Facets.java and of the Java Frame reader: Rec.709 luma on 8-bit
sRGB values, mean over an inclusive rectangle, printed beside the rectangle
(STYLE.md 11.3). Usage:
    python tmp/facelum.py IMG x0 y0 x1 y1 [label]        # one box, mean L
    python tmp/facelum.py --ratio IMG fx0 fy0 fx1 fy1 sx0 sy0 sx1 sy1 [label]
"""
import sys
import numpy as np
from PIL import Image


def lum(img):
    a = np.asarray(img.convert("RGB"), dtype=np.float64)
    return 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]


def box_mean(L, x0, y0, x1, y1):
    return float(L[y0:y1 + 1, x0:x1 + 1].mean())


def main():
    args = sys.argv[1:]
    if args and args[0] == "--ratio":
        img, fx0, fy0, fx1, fy1, sx0, sy0, sx1, sy1 = args[1:10]
        label = args[10] if len(args) > 10 else ""
        L = lum(Image.open(img))
        f = box_mean(L, int(fx0), int(fy0), int(fx1), int(fy1))
        s = box_mean(L, int(sx0), int(sy0), int(sx1), int(sy1))
        print(f"{label} {img}")
        print(f"  face x{fx0}..{fx1} y{fy0}..{fy1}  mean L {f:.1f}")
        print(f"  sky  x{sx0}..{sx1} y{sy0}..{sy1}  mean L {s:.1f}")
        print(f"  face/sky {f / s:.3f}")
    else:
        img, x0, y0, x1, y1 = args[:5]
        label = args[5] if len(args) > 5 else ""
        L = lum(Image.open(img))
        m = box_mean(L, int(x0), int(y0), int(x1), int(y1))
        sub = L[int(y0):int(y1) + 1, int(x0):int(x1) + 1]
        print(f"{label} {img} x{x0}..{x1} y{y0}..{y1}  mean {m:.1f}  "
              f"min {sub.min():.1f}  max {sub.max():.1f}")


if __name__ == "__main__":
    main()
