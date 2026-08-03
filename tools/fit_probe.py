"""Print the DuelScene probe positions at a given sample index of a timing series.

Usage: python tools/fit_probe.py <timing.json> <sampleIndex>
"""
import json
import sys

d = json.load(open(sys.argv[1]))
idx = int(sys.argv[2])
print("keys:", [k for k in d.keys()])
for p in d.get("probes", []):
    x = p["x"][idx]
    y = p["y"][idx]
    dk = p.get("darkest", [None] * (idx + 1))[idx]
    print(f'{p["name"]:14s} x={x:7.1f} y={y:7.1f} darkest={dk}')
