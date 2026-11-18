"""Writes the per-item model JSONs: neoforge:separate_transforms with the 3D model as base and the 2D sprite for the GUI.

The `<id>_3d.json` files come from Blockbench (bb_helpers.js). The vampire compass keeps its `angle` overrides; each
frame model swaps the dial texture on the 3D model and the sprite. Run from the repo root: py art/items/gen_models.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
M = ROOT / "src/main/resources/assets/supernaturalbloodlines/models/item"
NS = "supernaturalbloodlines:item/"
SPRITE = ("minecraft:item/generated",)


def sprite(tex):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": NS + tex}}


def loader(base, gui, extra=None):
    d = {"loader": "neoforge:separate_transforms", "gui_light": "front", "base": base, "perspectives": {"gui": gui}}
    if extra:
        d.update(extra)
    return d


def write(name, d):
    (M / f"{name}.json").write_text(json.dumps(d, indent=2) + "\n")


def main():
    ids = sorted(p.name[:-8] for p in M.glob("*_3d.json"))
    for n in ids:
        if n == "vampire_compass":
            continue
        write(n, loader({"parent": NS + f"{n}_3d"}, sprite(n)))
    old = json.loads((M / "vampire_compass.json").read_text())
    overrides = old["overrides"]
    for i in range(32):
        f = f"vampire_compass_{i:02d}"
        write(f, loader({"parent": NS + "vampire_compass_3d", "textures": {"face": NS + f}}, sprite(f)))
    write("vampire_compass", loader({"parent": NS + "vampire_compass_3d"}, sprite("vampire_compass_00"), {"overrides": overrides}))
    print(len(ids), "items,", 32, "compass frames")


main()
