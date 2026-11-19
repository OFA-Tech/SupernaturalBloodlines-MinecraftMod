"""Worn-ring models for a Curios ring renderer (assets only, see RING_RENDER.md).

A ring reads as small jewellery at the hand: a 1 px tall silver band at the very bottom of the arm that wraps the outer
face and half of the front and back faces (open on the inside of the arm, against the body), plus a stone of at most
2x2 px on the outer face (the back of the hand), raised 0.5 px, with one highlight pixel.

Models are authored y-up in 0..16 units, 1 unit = 1 player pixel, ring centre at (8, 8, 8), for the RIGHT arm.
Arm centre is (8, ., 8): outer face x = 8 - hx. Variants: `<ring>.json` (classic right), `_left`, `_slim`, `_slim_left`
in models/curio/. Palette textures: textures/item/3d/curio_<ring>.png; Blockbench spec (classic right): spec3d/curio_<ring>.json.
"""
import json
from pathlib import Path

from PIL import Image

import models3d as m3
from draw import hexc

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/supernaturalbloodlines"
CURIO = ASSETS / "models/curio"
NS = "supernaturalbloodlines:item/3d/"

SILVER = {"side": "#aab3c4", "u": "#d4dae4", "d": "#6a7388"}
HZ = 2.0                 # arm half depth (the 0.25 sleeve layer sits inside the band)
T0, T1 = 0.3, 0.7        # band inner/outer offset from the arm skin
Y0, Y1 = 7.5, 8.5        # 1 px tall; local y 9..10 = the lowest pixel of the arm
RAISE = 0.5              # stone height over the band


def stone_box(m, ox, base, top, bottom):
    """2 (z) x 1.2 (y) stone on the outer face; returns its outer plane x."""
    m.box(ox - RAISE, 7.5, 7.0, ox, 8.7, 9.0, {"side": base, "u": top, "d": bottom, "w": base})
    return ox - RAISE


def stone_sun(m, ox):
    x = stone_box(m, ox, "#1f3a9a", "#3d5fc0", "#152a78")
    m.box(x - 0.03, 7.9, 7.1, x, 8.4, 7.6, "#c6d6f6")      # highlight
    m.box(x - 0.03, 7.6, 8.3, x, 8.0, 8.8, "#e8c850")      # gold pyrite fleck


def stone_moon(m, ox):
    x = stone_box(m, ox, "#16264a", "#2a4580", "#0c1530")
    m.box(x - 0.03, 8.0, 7.9, x, 8.3, 8.9, "#6fa6dc")      # kyanite streak
    m.box(x - 0.03, 7.7, 7.3, x, 8.0, 8.1, "#3f6fb0")
    m.box(x - 0.03, 8.1, 7.1, x, 8.4, 7.6, "#e8f4ff")      # highlight


def stone_onyx(m, ox):
    x = stone_box(m, ox, "#0e0e14", "#2a2a38", "#0e0e14")
    m.box(x - 0.03, 7.9, 7.1, x, 8.4, 7.6, "#8a8aa0")      # highlight


RINGS = {"sun_ring": stone_sun, "moon_ring": stone_moon, "resurrection_ring": stone_onyx}


def build(ring, slim=False):
    m = m3.M("curio_" + ring, preset={})
    hx = 1.5 if slim else 2.0
    ox0, ox1 = 8 - hx - T1, 8 - hx - T0
    m.box(ox0, Y0, 8 - HZ - T1, ox1, Y1, 8 + HZ + T1, SILVER)                      # outer face + corners
    m.box(ox1, Y0, 8 - HZ - T1, 8.0, Y1, 8 - HZ - T0, SILVER)                      # front, to the arm centre
    m.box(ox1, Y0, 8 + HZ + T0, 8.0, Y1, 8 + HZ + T1, SILVER)                      # back, to the arm centre
    RINGS[ring](m, ox0)
    return m


def to_model(m, left=False):
    def face(idx):
        px, py = idx % 16, idx // 16
        return {"uv": [px + 0.2, py + 0.2, px + 0.8, py + 0.8], "texture": "#0"}
    els = []
    for c in m.cubes:
        f, t = [round(v, 4) for v in c["f"]], [round(v, 4) for v in c["t"]]
        faces = dict(c["c"])
        if left:
            f[0], t[0] = round(16 - c["t"][0], 4), round(16 - c["f"][0], 4)
            faces["east"], faces["west"] = faces["west"], faces["east"]
        els.append({"name": c["n"], "from": f, "to": t,
                    "faces": {k: face(v) for k, v in faces.items() if v is not None}})
    tex = NS + m.name
    return {"textures": {"0": tex, "particle": tex}, "elements": els}


def write_all():
    CURIO.mkdir(parents=True, exist_ok=True)
    for ring in RINGS:
        for slim in (False, True):
            m = build(ring, slim)
            for left in (False, True):
                name = ring + ("_slim" if slim else "") + ("_left" if left else "")
                (CURIO / f"{name}.json").write_text(json.dumps(to_model(m, left), indent=2) + "\n")
            if not slim:
                im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
                for h, i in m.pal.items():
                    im.putpixel((i % 16, i // 16), hexc(h))
                im.save(m3.TEX3D / f"{m.name}.png")
                (m3.SPEC / f"{m.name}.json").write_text(json.dumps(m.spec(), indent=1))
    print(len(RINGS) * 4, "curio models")


if __name__ == "__main__":
    write_all()
