"""
Generates the guard_banner.png texture (16x16) for the Village Guard mod.
Run from any directory:
    python3 gen_guard_banner_texture.py

Output: The-Guard/src/main/resources/assets/village-guard/textures/item/guard_banner.png

Design:
  - Base: deep red (#8B0000) — standard banner red
  - Diamond centre: emerald green (#00A550)
  - Outline: dark gold (#7A5C00) border + top point
  - Guard emblem: small white 'G' cross mark in the diamond
"""

from pathlib import Path
from PIL import Image

# --- Pixel grid (16x16) ---
# Colour palette
R = (139, 0, 0, 255)      # deep red
G = (0, 165, 80, 255)     # emerald green
K = (122, 92, 0, 255)     # dark gold (border)
W = (255, 255, 255, 255)  # white (cross mark)
_ = R                      # alias for red background

grid = [
    [K, K, K, K, K, K, K, K, K, K, K, K, K, K, K, K],  # row 0 — top border
    [K, _, _, _, _, _, _, _, _, _, _, _, _, _, _, K],
    [K, _, _, _, _, _, G, G, G, _, _, _, _, _, _, K],
    [K, _, _, _, _, G, G, G, G, G, _, _, _, _, _, K],
    [K, _, _, _, G, G, G, W, G, G, G, _, _, _, _, K],  # row 4 — centre row
    [K, _, _, G, G, G, W, W, W, G, G, G, _, _, _, K],
    [K, _, G, G, G, G, W, W, W, G, G, G, G, _, _, K],
    [K, _, _, G, G, G, W, W, W, G, G, G, _, _, _, K],
    [K, _, _, _, G, G, G, W, G, G, G, _, _, _, _, K],
    [K, _, _, _, _, G, G, G, G, G, _, _, _, _, _, K],
    [K, _, _, _, _, _, G, G, G, _, _, _, _, _, _, K],
    [K, _, _, _, _, _, _, _, _, _, _, _, _, _, _, K],
    [K, _, _, _, _, _, _, _, _, _, _, _, _, _, _, K],
    [K, _, _, _, _, _, _, _, _, _, _, _, _, _, _, K],
    [K, _, _, _, _, _, _, _, _, _, _, _, _, _, _, K],
    [K, K, K, K, K, K, K, K, K, K, K, K, K, K, K, K],  # row 15 — bottom border
]

out_path = Path(__file__).parent / \
    "The-Guard/src/main/resources/assets/village-guard/textures/item/guard_banner.png"
out_path.parent.mkdir(parents=True, exist_ok=True)

img = Image.new("RGBA", (16, 16))
for y, row in enumerate(grid):
    for x, pixel in enumerate(row):
        img.putpixel((x, y), pixel)

img.save(out_path)
print(f"Saved {out_path}")
