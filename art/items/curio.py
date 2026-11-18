"""Worn-ring models for a Curios ring renderer (assets only, see RING_RENDER.md).

Each ring is a band around the lower arm with the stone on the back of the hand. The model is authored upright
(y up, ring axis = arm axis, ring centre at (8,8,8), 1 unit = 1 entity-model pixel), for the RIGHT arm, classic width.
Variants: `<ring>.json` (classic right), `_left`, `_slim`, `_slim_left` in models/curio/. Palette textures are
textures/item/3d/curio_<ring>.png; the Blockbench spec (classic right) is spec3d/curio_<ring>.json.
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
HZ = 2.35          # half arm depth incl. the 0.25 sleeve layer
T, H = 0.55, 1.2   # band thickness and height


def band(m, hx):
    y0, y1 = 8 - H / 2, 8 + H / 2
    m.box(8 - hx - T, y0, 8 - HZ - T, 8 + hx + T, y1, 8 - HZ, SILVER)
    m.box(8 - hx - T, y0, 8 + HZ, 8 + hx + T, y1, 8 + HZ + T, SILVER)
    m.box(8 - hx - T, y0, 8 - HZ, 8 - hx, y1, 8 + HZ, SILVER)
    m.box(8 + hx, y0, 8 - HZ, 8 + hx + T, y1, 8 + HZ, SILVER)


def stone_sun(m, x):
    m.box(x - 0.5, 7.1, 6.55, x, 8.9, 9.45, SILVER)
    m.box(x - 1.15, 7.3, 6.8, x - 0.5, 8.7, 9.2, {"side": "#1f3a9a", "u": "#3d5fc0", "d": "#152a78", "w": "#3d5fc0"})
    m.box(x - 1.2, 7.8, 7.7, x - 1.15, 8.3, 8.3, "#dfe4ee")
    m.box(x - 1.2, 8.3, 8.4, x - 1.15, 8.6, 8.7, "#e8c850")


def stone_moon(m, x):
    m.box(x - 0.5, 7.1, 6.6, x, 8.9, 9.4, SILVER)
    m.box(x - 1.1, 7.35, 7.0, x - 0.5, 8.65, 9.0, {"side": "#eef2f6", "u": "#ffffff", "d": "#c4d2e6", "w": "#ffffff"})
    m.box(x - 0.9, 7.25, 7.2, x - 0.5, 8.75, 8.8, "#d0dcf2")
    m.box(x - 1.15, 8.0, 7.3, x - 1.1, 8.5, 7.8, "#ffffff")


def stone_onyx(m, x):
    m.box(x - 0.5, 7.0, 6.4, x, 9.0, 9.6, SILVER)
    m.box(x - 1.1, 7.3, 6.7, x - 0.5, 8.7, 9.3, {"side": "#0e0e14", "u": "#2a2a38", "d": "#0e0e14", "w": "#2a2a38"})
    m.box(x - 1.15, 8.3, 6.8, x - 1.1, 8.65, 7.6, "#4a4a5e")


RINGS = {"sun_ring": stone_sun, "moon_ring": stone_moon, "resurrection_ring": stone_onyx}


def build(ring, slim=False):
    m = m3.M("curio_" + ring, preset={})
    hx = 1.85 if slim else 2.35
    band(m, hx)
    RINGS[ring](m, 8 - hx - T)
    return m


def to_model(m, left=False):
    def face(idx, uv=None):
        px, py = idx % 16, idx // 16
        return {"uv": [px + 0.2, py + 0.2, px + 0.8, py + 0.8], "texture": "#0"}
    els = []
    for c in m.cubes:
        f, t = [round(v, 4) for v in c["f"]], [round(v, 4) for v in c["t"]]
        faces = {k: v for k, v in c["c"].items()}
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

