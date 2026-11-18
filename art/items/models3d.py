"""3D held models as flat-colour box assemblies. Each item gets a 16x16 palette texture (textures/item/3d/<id>.png)
and a spec (art/items/spec3d/<id>.json) that Blockbench builds into item_<id>.bbmodel and models/item/<id>_3d.json.

Coordinates are Minecraft model units (0..16). A colour is a hex string, or a dict keyed n/s/e/w/u/d/side/all.
"""
import json
from pathlib import Path

from PIL import Image

from draw import hexc

ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "art/items/spec3d"
TEX3D = ROOT / "src/main/resources/assets/supernaturalbloodlines/textures/item/3d"

GEN = {  # item/generated transforms, for upright props
    "thirdperson_righthand": ([0, 90, 45], [0, 1, 0], 0.65),
    "thirdperson_lefthand": ([0, -90, -45], [0, 1, 0], 0.65),
    "firstperson_righthand": ([0, -90, 25], [1.13, 3.2, 1.13], 0.68),
    "firstperson_lefthand": ([0, 90, -25], [1.13, 3.2, 1.13], 0.68),
    "ground": ([0, 0, 0], [0, 2, 0], 0.5),
    "head": ([0, 180, 0], [0, 13, 7], 1.0),
    "fixed": ([0, 180, 0], [0, 0, 0], 1.0),
}
TOOL = {  # item/handheld transforms, for diagonal blades
    "thirdperson_righthand": ([0, -90, 55], [0, 4, 0.5], 0.85),
    "thirdperson_lefthand": ([0, 90, -55], [0, 4, 0.5], 0.85),
    "firstperson_righthand": ([0, -90, 25], [1.13, 3.2, 1.13], 0.68),
    "firstperson_lefthand": ([0, 90, -25], [1.13, 3.2, 1.13], 0.68),
    "ground": ([0, 0, 0], [0, 2, 0], 0.5),
    "head": ([0, 180, 0], [0, 13, 7], 1.0),
    "fixed": ([0, 180, 0], [0, 0, 0], 1.0),
}
RING = {  # rings are worn, not held: the band sits around the end of the arm/hand, stone facing outward
    "thirdperson_righthand": ([0, 0, -90], [0, 0, 0], 0.4),
    "thirdperson_lefthand": ([0, 0, 90], [0, 0, 0], 0.4),
    "firstperson_righthand": ([0, 0, 0], [1.13, 3.2, 1.13], 0.3),
    "firstperson_lefthand": ([0, 0, 0], [1.13, 3.2, 1.13], 0.3),
    "ground": ([0, 0, 0], [0, 2, 0], 0.45),
    "head": ([0, 180, 0], [0, 13, 7], 0.6),
    "fixed": ([0, 180, 0], [0, 0, 0], 0.7),
}
FACES = ("north", "east", "south", "west", "up", "down")
KEY = {"n": "north", "e": "east", "s": "south", "w": "west", "u": "up", "d": "down"}

MODELS = {}


class M:
    def __init__(self, name, preset=GEN, scale=1.0, lift=0.0, glift=0.0, fp=None):
        self.name, self.cubes, self.pal = name, [], {}
        self.preset, self.scale, self.lift, self.glift, self.fp = preset, scale, lift, glift, fp
        self.extra_tex = {}

    def col(self, h):
        h = h.lower()
        if h not in self.pal:
            self.pal[h] = len(self.pal)
        return self.pal[h]

    def faces(self, c):
        if isinstance(c, str):
            c = {"all": c}
        out = {}
        for f in FACES:
            h = c.get(f[0])
            if h is None and f in ("north", "east", "south", "west"):
                h = c.get("side")
            if h is None:
                h = c.get("all")
            out[f] = self.col(h) if h else None
        return out

    def box(self, x0, y0, z0, x1, y1, z1, c, rot=None, org=None, tex=None):
        cube = {"n": f"{self.name}_{len(self.cubes)}", "f": [x0, y0, z0], "t": [x1, y1, z1], "c": self.faces(c)}
        if rot:
            ax, ang = rot
            cube["r"] = [ax, ang]
            cube["o"] = org or [(x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2]
        if tex:
            cube["tex"] = tex
        self.cubes.append(cube)

    def diag(self, x0, y0, z0, x1, y1, z1, c):
        """A box in the vertical frame, tilted -45deg about z around (8,8,8) (blade items)."""
        self.box(x0, y0, z0, x1, y1, z1, c, rot=("z", -45), org=[8, 8, 8])

    def cyl(self, cx, cz, r, y0, y1, c, n=2, k=0.62):
        """Upright round-ish prism: two crossed boxes (a plus with notched corners); tiny radii stay one box."""
        if r <= 1.5:
            self.box(cx - r, y0, cz - r, cx + r, y1, cz + r, c)
            return
        q = r * k
        self.box(cx - r, y0, cz - q, cx + r, y1, cz + q, c)
        self.box(cx - q, y0 + 0.004, cz - r, cx + q, y1 - 0.004, cz + r, c)

    def disc(self, cx, cy, r, z0, z1, c, n=2, k=0.62):
        """Same, standing in the xy plane (axis along z)."""
        q = r * k
        self.box(cx - r, cy - q, z0, cx + r, cy + q, z1, c)
        self.box(cx - q, cy - r, z0 + 0.004, cx + q, cy + r, z1 - 0.004, c)

    def bbox(self):
        import math
        lo, hi = [1e9] * 3, [-1e9] * 3
        for c in self.cubes:
            if c.get("tex"):
                continue
            for i in range(8):
                p = [c["f"][k] if not (i >> k) & 1 else c["t"][k] for k in range(3)]
                if "r" in c:
                    ax = "xyz".index(c["r"][0])
                    a = math.radians(c["r"][1])
                    u, v = [k for k in range(3) if k != ax]
                    du, dv = p[u] - c["o"][u], p[v] - c["o"][v]
                    p[u] = c["o"][u] + du * math.cos(a) - dv * math.sin(a)
                    p[v] = c["o"][v] + du * math.sin(a) + dv * math.cos(a)
                for k in range(3):
                    lo[k], hi[k] = min(lo[k], p[k]), max(hi[k], p[k])
        return lo, hi

    def recenter(self):
        """Move a prop so its bounding box is centred on (8,8,8), like a sprite, and drop it onto the ground like one."""
        lo, hi = self.bbox()
        d = [8 - (lo[k] + hi[k]) / 2 for k in range(3)]
        for c in self.cubes:
            c["f"] = [round(c["f"][k] + d[k], 4) for k in range(3)]
            c["t"] = [round(c["t"][k] + d[k], 4) for k in range(3)]
            if "o" in c:
                c["o"] = [round(c["o"][k] + d[k], 4) for k in range(3)]
        gs = self.preset["ground"][2]
        self.glift += -(4 - (hi[1] - lo[1]) * 0.5 * gs * self.scale)

    def spec(self):
        if (self.preset is GEN or self.preset is RING) and not getattr(self, "_rc", False):
            self._rc = True
            self.recenter()
        disp = {}
        for k, (r, t, s) in self.preset.items():
            dy = self.lift + (self.glift if k == "ground" else 0.0)
            disp[k] = {"rotation": r, "translation": [t[0], t[1] + dy, t[2]], "scale": [s * self.scale] * 3}
        return {"name": self.name, "palette": list(self.pal), "cubes": self.cubes, "display": disp, "extra_tex": self.extra_tex}


def model(fn):
    MODELS[fn.__name__] = fn()
    return fn


GLASS, GLASS_D = "#cfe3ea", "#9fb8c4"


def vial(name, liq, hi, cork, band, label="#e8dcc0", half=3.0, cap=None, flecks=(), fleck="#e8c060", h=6.6, wax=None):
    m = M(name)
    x0, x1 = 8 - half - 0.2, 8 + half + 0.2
    m.cyl(8, 8, half + 0.2, 0, 0.6, GLASS_D)
    m.cyl(8, 8, half, 0.6, h, {"side": liq, "u": GLASS, "d": GLASS})
    m.box(8 - half * 0.45, 0.6, 8 - half - 0.02, 8 - half * 0.45 + 0.6, h - 0.8, 8 - half + 0.1, hi)
    m.cyl(8, 8, half * 0.8, h, h + 1.0, GLASS)
    m.cyl(8, 8, 1.2, h + 1.0, h + 4.0, GLASS)
    m.cyl(8, 8, 1.35, h + 2.4, h + 3.1, band)
    m.cyl(8, 8, 1.7, h + 4.0, h + 4.8, GLASS)
    if label:
        m.box(8 - half * 0.58, 1.6, 8 + half - 0.02, 8 + half * 0.58, h - 1.6, 8 + half + 0.15, label)
    for (fx, fy, fz) in flecks:
        m.box(fx, fy, fz, fx + 0.8, fy + 0.8, fz + 0.8, fleck)
    top = h + 4.8
    if wax:
        m.cyl(8, 8, 1.5, top, top + 2.2, {"side": wax, "u": cap or wax, "d": wax})
    else:
        m.cyl(8, 8, 1.2, top, top + 2.2, {"side": cork, "u": cap or cork, "d": cork})
    return m


@model
def original_blood():
    return vial("original_blood", "#8c010d", "#c03040", "#d8e0e6", "#c9a24a", cap="#f4f8fc",
                flecks=[(6.2, 3.4, 4.9), (9.4, 4.4, 4.9), (7.2, 2.0, 4.9)], fleck="#e8c060", label="#d8c8a0")


@model
def doppelganger_blood():
    return vial("doppelganger_blood", "#a8142c", "#d86070", "#6b4f2a", "#3aa8b8", cap="#8a6a3a", label="#e8dcc0")


@model
def hybrid_blood():
    return vial("hybrid_blood", "#7a0a14", "#b02838", "#4a3220", "#d89a20", cap="#6a4a30", label="#3a2a22",
                flecks=[(7.4, 3.2, 11.0), (8.6, 3.2, 11.0)], fleck="#f0b830")


@model
def sires_blood():
    return vial("sires_blood", "#6e0a2c", "#a02858", "#201820", "#8a4ac0", cap="#3a2c3a", label="#d8c8e0")


@model
def klaus_blood():
    return vial("klaus_blood", "#8c0a14", "#c03040", "#6b4f2a", "#e8a830", cap="#8a6a3a", label="#e8dcc0",
                flecks=[(6.2, 3.4, 4.9), (9.0, 2.6, 4.9), (7.8, 4.6, 4.9)], fleck="#f0c050")


@model
def the_cure():
    return vial("the_cure", "#a0101c", "#d03040", "#b8c0cc", "#e8eef4", cap="#e8eef4", half=2.2, label=None, h=9.0, wax="#8a0c14")


@model
def the_true_cure():
    return vial("the_true_cure", "#eaf6ff", "#ffffff", "#b8c0cc", "#f0c84a", cap="#e8eef4", half=2.2, label=None, h=9.0,
                flecks=[(5.0, 4.0, 9.0), (10.4, 7.0, 7.0)], fleck="#ffffff")


@model
def the_elixir():
    m = M("the_elixir")
    liq, hi, lo = "#f2c84b", "#fff6c0", "#b07a20"
    layers = [(0, 1, 2.0), (1, 2.2, 3.2), (2.2, 5.8, 3.9), (5.8, 7.0, 3.2), (7.0, 8.0, 2.0)]
    for i, (y0, y1, r) in enumerate(layers):
        m.cyl(8, 8, r, y0, y1, {"side": liq if 0.5 < y0 < 7 else GLASS, "u": GLASS, "d": GLASS}, n=2)
    m.box(5.0, 3.0, 4.15, 6.6, 5.6, 4.35, hi)
    m.box(9.4, 2.0, 4.15, 10.4, 4.0, 4.35, lo)
    m.cyl(8, 8, 1.2, 8.0, 11.0, GLASS)
    m.cyl(8, 8, 1.3, 9.0, 9.6, "#e0a830")
    m.cyl(8, 8, 1.7, 11.0, 12.8, {"side": "#a31622", "u": "#c8323e", "d": "#a31622"})
    return m


# ---------------------------------------------------------------- rings
def ring(name, band, band_hi, band_lo, stone_fn, thick=2.0, size=1.0):
    m = M(name, preset=RING)
    t = thick
    z0, z1 = 8 - 1.0, 8 + 1.0
    m.box(6, 3.5, z0, 10, 3.5 + t, z1, {"side": band, "u": band_hi, "d": band_lo})
    m.box(6, 10.5 - t + 1.0, z0, 10, 12.5, z1, {"side": band, "u": band_hi, "d": band_lo})
    m.box(3.5, 6, z0, 3.5 + t, 10, z1, {"side": band, "u": band_hi, "d": band_lo})
    m.box(12.5 - t, 6, z0, 12.5, 10, z1, {"side": band, "u": band_hi, "d": band_lo})
    for (cx, cy, a) in ((5.0, 5.0, -45), (11.0, 5.0, 45), (5.0, 11.0, 45), (11.0, 11.0, -45)):
        m.box(cx - 1.5, cy - t / 2, z0, cx + 1.5, cy + t / 2, z1, {"side": band, "u": band_hi, "d": band_lo}, rot=("z", a))
    stone_fn(m)
    return m


@model
def sun_ring():
    def stone(m):
        m.box(6.0, 12.4, 6.6, 10.0, 13.0, 9.4, "#d4dae4")
        m.box(6.5, 13.0, 6.9, 9.5, 14.6, 9.1, {"side": "#1f3a9a", "u": "#3d5fc0", "d": "#1f3a9a"})
        m.box(7.0, 14.6, 7.3, 9.0, 14.9, 8.7, "#3d5fc0")
        m.box(8.3, 13.6, 6.85, 8.8, 14.1, 6.95, "#e8c850")
        m.box(7.0, 13.2, 9.05, 7.5, 13.7, 9.15, "#e8c850")
    return ring("sun_ring", "#aab3c4", "#d4dae4", "#6a7388", stone)


@model
def moon_ring():
    def stone(m):
        m.box(6.0, 12.4, 6.6, 10.0, 13.0, 9.4, "#d4dae4")
        m.cyl(8, 8, 1.7, 13.0, 14.8, {"side": "#16264a", "u": "#2a4580", "d": "#0c1530"})
        m.box(7.0, 14.3, 6.6, 9.4, 14.5, 7.1, "#6fa6dc")
        m.box(6.8, 13.4, 6.2, 7.4, 14.0, 6.5, "#e8f4ff")
    r = ring("moon_ring", "#aab3c4", "#d4dae4", "#6a7388", stone)
    return r


@model
def resurrection_ring():
    def stone(m):
        m.box(5.5, 12.4, 6.4, 10.5, 13.0, 9.6, "#d4dae4")
        m.box(6.0, 13.0, 6.8, 10.0, 14.4, 9.2, {"side": "#14141c", "u": "#54546a", "d": "#14141c"})
        m.box(6.0, 13.0, 6.5, 6.5, 14.4, 6.8, "#aab3c4")
        m.box(9.5, 13.0, 6.5, 10.0, 14.4, 6.8, "#aab3c4")
        m.box(5.0, 8.0, 7.9, 5.4, 8.4, 8.1, "#4a5164")
    return ring("resurrection_ring", "#aab3c4", "#d4dae4", "#6a7388", stone, thick=2.4)


# ---------------------------------------------------------------- poppets
def poppet(name, cloth, hi, lo, stitch, sym):
    m = M(name, scale=0.95)
    m.box(5.5, 9.0, 6.5, 10.5, 14.0, 9.5, {"side": cloth, "u": hi, "d": lo})
    m.box(6.3, 11.2, 6.4, 7.0, 11.9, 6.5, "#1e1410")
    m.box(9.0, 11.2, 6.4, 9.7, 11.9, 6.5, "#1e1410")
    m.box(7.2, 9.8, 6.4, 8.8, 10.1, 6.5, stitch)
    m.box(6.0, 3.0, 6.8, 10.0, 9.0, 9.2, {"side": cloth, "u": hi, "d": lo})
    m.box(2.0, 6.0, 7.0, 6.0, 8.2, 9.0, {"side": cloth, "u": hi, "d": lo})
    m.box(10.0, 6.0, 7.0, 14.0, 8.2, 9.0, {"side": cloth, "u": hi, "d": lo})
    m.box(6.2, 0.0, 7.0, 7.9, 3.0, 9.0, {"side": lo, "u": cloth, "d": lo})
    m.box(8.1, 0.0, 7.0, 9.8, 3.0, 9.0, {"side": lo, "u": cloth, "d": lo})
    m.box(2.0, 6.9, 6.9, 2.3, 7.1, 9.1, stitch)
    m.box(13.7, 6.9, 6.9, 14.0, 7.1, 9.1, stitch)
    sym(m)
    return m


@model
def sun_poppet():
    def sym(m):
        m.box(7.2, 5.2, 6.7, 8.8, 6.8, 6.8, "#fff3b0")
        m.box(7.7, 4.7, 6.7, 8.3, 7.3, 6.8, "#fff3b0")
        m.box(6.7, 5.7, 6.7, 9.3, 6.3, 6.8, "#fff3b0")
    return poppet("sun_poppet", "#f0c040", "#f8dc78", "#c08a1c", "#8a5a14", sym)


@model
def silver_poppet():
    def sym(m):
        m.box(7.0, 5.0, 6.7, 9.0, 7.0, 6.8, "#ffffff")
        m.box(7.8, 5.0, 6.7, 9.0, 7.0, 6.8, "#cfd5de")
    return poppet("silver_poppet", "#cfd5de", "#eef1f6", "#8f98a8", "#6a7488", sym)


@model
def spirit_poppet():
    def sym(m):
        m.box(7.4, 4.4, 6.7, 8.6, 7.6, 6.8, "#ffffff")
        m.box(8.6, 5.6, 6.7, 9.2, 6.2, 6.8, "#ffffff")
    return poppet("spirit_poppet", "#a8d8f0", "#d0eefc", "#6aa4d0", "#4a82b0", sym)


# ---------------------------------------------------------------- blades (diagonal, handheld)
def dagger(name, blade, fuller, edge, guard, grip, pommel, ash=False, fancy=False):
    m = M(name, preset=TOOL)
    m.diag(7.0, -2.2, 7.2, 9.0, -0.6, 8.8, pommel)
    m.diag(7.3, -0.6, 7.3, 8.7, 4.4, 8.7, grip)
    for y in (0.6, 1.8, 3.0):
        m.diag(7.15, y, 7.15, 8.85, y + 0.5, 8.85, guard)
    m.diag(5.0, 4.4, 6.9, 11.0, 5.4, 9.1, guard)
    if fancy:
        m.diag(4.4, 5.4, 7.2, 5.6, 6.8, 8.8, guard)
        m.diag(10.4, 5.4, 7.2, 11.6, 6.8, 8.8, guard)
    m.diag(6.7, 5.4, 7.6, 9.3, 13.0, 8.4, {"side": blade, "u": edge, "d": edge})
    m.diag(7.2, 13.0, 7.65, 8.8, 15.8, 8.35, {"side": blade, "u": edge, "d": edge})
    m.diag(7.7, 15.8, 7.7, 8.3, 18.0, 8.3, {"side": edge, "u": edge, "d": edge})
    m.diag(7.75, 5.8, 7.5, 8.25, 15.0, 7.6, fuller)
    m.diag(7.75, 5.8, 8.4, 8.25, 15.0, 8.5, fuller)
    if ash:
        for (a, b, c) in ((6.9, 6.0, 7.55), (8.0, 7.4, 8.4), (7.3, 9.2, 7.55), (8.2, 10.6, 8.4), (7.0, 7.8, 8.4)):
            m.diag(a, b, c, a + 1.0, b + 0.9, c + 0.1, "#cdcbc2")
    return m


@model
def silver_dagger():
    return dagger("silver_dagger", "#d4dce6", "#aab6c4", "#f4f8fc", "#8a93a4", "#2a2a34", "#b8c0cc")


@model
def white_oak_ash_dagger():
    return dagger("white_oak_ash_dagger", "#d4dce6", "#aab6c4", "#f4f8fc", "#6a7388", "#4a3a2c", "#c8d2de", ash=True, fancy=True)


def stake(name):
    m = M(name, preset=TOOL)
    wood, wood_hi, wood_lo = "#e2d6bc", "#f0e6cc", "#b8aa8c"
    m.diag(7.0, 14.0, 7.0, 9.0, 16.0, 9.0, {"side": wood_lo, "u": wood_lo, "d": wood_lo})
    m.diag(7.5, 16.0, 7.5, 8.5, 18.0, 8.5, {"side": "#968a70", "u": "#968a70", "d": "#968a70"})
    m.diag(6.5, 4.0, 6.5, 9.5, 14.0, 9.5, {"side": wood, "u": wood_hi, "d": wood_lo})
    m.diag(7.4, 6.0, 6.4, 8.2, 12.5, 6.5, wood_lo)
    m.diag(6.2, 2.0, 6.2, 9.8, 4.0, 9.8, {"side": "#4a2e1a", "u": "#6a4528", "d": "#4a2e1a"})
    m.diag(6.5, 0.0, 6.5, 9.5, 2.0, 9.5, {"side": "#6a4528", "u": "#4a2e1a", "d": "#4a2e1a"})
    return m


@model
def white_oak_stake():
    return stake("white_oak_stake")


@model
def taser():
    m = M("taser", preset=TOOL)
    m.diag(7.2, -2.0, 7.2, 8.8, 7.0, 8.8, "#1c1c24")
    for y in (0.0, 1.8, 3.6, 5.4):
        m.diag(7.0, y, 7.0, 9.0, y + 0.8, 9.0, "#4a4a58")
    m.diag(6.6, 7.0, 6.6, 9.4, 8.4, 9.4, "#8a94a4")
    m.diag(6.8, 8.4, 6.8, 9.2, 13.0, 9.2, "#34363e")
    m.diag(6.6, 13.0, 6.8, 9.4, 14.0, 9.2, "#b8c0cc")
    m.diag(6.8, 14.0, 7.0, 7.6, 17.0, 8.0, "#b8c0cc")
    m.diag(8.4, 14.0, 8.0, 9.2, 17.0, 9.0, "#b8c0cc")
    m.diag(7.0, 17.0, 7.2, 7.4, 17.8, 7.8, "#6fe0ff")
    m.diag(8.6, 17.0, 8.2, 9.0, 17.8, 8.8, "#6fe0ff")
    m.diag(7.4, 16.4, 7.5, 8.6, 16.8, 8.5, "#ffffff")
    return m


@model
def hearing_device():
    m = M("hearing_device", scale=1.15)
    m.box(4.5, 0.0, 5.5, 11.5, 5.5, 10.5, {"side": "#34363e", "u": "#4a4d58", "d": "#1c1d24"})
    m.box(5.0, 1.0, 10.4, 11.0, 2.4, 10.6, "#6a6e7a")
    m.box(9.4, 3.2, 10.4, 10.2, 4.0, 10.7, "#e02030")
    for (y0, y1, r, c) in ((5.5, 6.5, 2.0, "#8a94a4"), (6.5, 7.5, 2.6, "#b8c0cc"), (7.5, 8.5, 3.2, "#b8c0cc"), (8.5, 9.5, 3.8, "#e0e8f0")):
        m.cyl(8, 8, r, y0, y1, c)
    m.cyl(8, 8, 3.1, 9.4, 9.55, "#1c1d24")
    m.cyl(8, 8, 1.4, 9.5, 9.7, "#4a4d58")
    m.box(11.0, 5.5, 7.6, 11.6, 12.5, 8.4, "#8a94a4")
    m.box(10.9, 12.5, 7.5, 11.7, 13.3, 8.5, "#e02030")
    return m


@model
def moonstone():
    m = M("moonstone", scale=0.95)
    c = {"side": "#eceeea", "u": "#f8faf8", "d": "#b4c2cc"}
    m.cyl(8, 8, 3.8, 2.5, 10.5, c)
    m.cyl(8, 8, 2.6, 1.2, 2.5, {"side": "#cdd3d4", "u": "#eceeea", "d": "#b4c2cc"})
    m.cyl(8, 8, 2.6, 10.5, 11.8, {"side": "#f4f6f4", "u": "#ffffff", "d": "#eceeea"})
    m.box(5.0, 5.0, 4.0, 5.9, 9.5, 4.2, "#ffffff")
    m.box(9.2, 5.0, 11.8, 10.4, 7.5, 12.0, "#cfe0f2")
    return m


# ---------------------------------------------------------------- plants and powders
def sprig(m, x, z, h, col):
    m.box(x - 0.35, 0, z - 0.35, x + 0.35, h, z + 0.35, col)


@model
def vervain():
    m = M("vervain", scale=1.1)
    g, G = "#3e8a34", "#2a6a26"
    sprig(m, 8, 8, 9, g)
    m.box(7.65, 5.0, 7.65, 8.35, 10.5, 8.35, g, rot=("z", 22.5), org=[8, 5, 8])
    m.box(7.65, 4.0, 7.65, 8.35, 9.5, 8.35, g, rot=("z", -22.5), org=[8, 4, 8])
    for (x, y, z, rz) in ((5.2, 2.5, 7.6, 22.5), (9.0, 3.5, 8.0, -22.5), (5.5, 6.0, 8.0, 22.5), (9.2, 7.0, 7.7, -22.5)):
        m.box(x, y, z, x + 2.8, y + 0.5, z + 0.8, G, rot=("z", rz))
    for (x, ytop) in ((8, 9), (4.55, 10.9), (11.2, 10.6)):
        for k in range(5):
            m.box(x - 0.45, ytop + k * 0.9, 7.6, x + 0.45, ytop + k * 0.9 + 0.8, 8.4,
                  {"side": "#b48ad8" if k % 2 == 0 else "#d4b4f0", "u": "#d4b4f0"})
    return m


def wolfsbane(name, hi, mid, lo):
    m = M(name, scale=1.1)
    g, G = "#3e8a34", "#2a6a26"
    sprig(m, 8, 8, 10, g)
    m.box(7.65, 6.0, 7.65, 8.35, 11.5, 8.35, g, rot=("z", 22.5), org=[8, 6, 8])
    m.box(7.65, 5.0, 7.65, 8.35, 10.5, 8.35, g, rot=("z", -22.5), org=[8, 5, 8])
    for (x, y, z, rz) in ((4.8, 2.0, 7.6, 22.5), (9.2, 3.0, 8.0, -22.5), (5.2, 4.8, 7.8, 22.5), (9.0, 5.6, 7.8, -22.5)):
        m.box(x, y, z, x + 3.0, y + 0.5, z + 0.8, G, rot=("z", rz))
    for (x, y) in ((8, 10.0), (5.1, 11.0), (10.9, 11.6)):
        m.box(x - 1.0, y, 7.2, x + 1.0, y + 1.6, 8.8, {"side": mid, "u": hi, "d": lo})
        m.box(x - 0.8, y + 1.6, 7.3, x + 0.8, y + 2.6, 8.7, {"side": hi, "u": hi, "d": mid})
        m.box(x - 0.5, y - 0.8, 7.5, x + 0.5, y, 8.5, lo)
    return m


@model
def yellow_wolfsbane():
    return wolfsbane("yellow_wolfsbane", "#f4e878", "#e8cc30", "#b89a14")


@model
def northern_blue_wolfsbane():
    return wolfsbane("northern_blue_wolfsbane", "#7a8ae8", "#3a4ac0", "#222a88")


@model
def wolf_lichen():
    m = M("wolf_lichen", scale=1.1)
    m.box(2.5, 2.0, 7.0, 13.5, 4.6, 9.0, "#6a4a30", rot=("z", -22.5))
    m.box(2.5, 2.0, 7.0, 13.5, 2.6, 9.0, "#4a3220", rot=("z", -22.5))
    cols = ["#d8e450", "#b4c82c", "#8aa020"]
    spots = [(3.0, 5.5, 6.6), (5.5, 4.8, 8.2), (7.5, 5.8, 6.5), (9.5, 6.8, 8.3), (11.0, 7.4, 6.7), (4.5, 3.4, 8.6), (8.0, 4.4, 8.6), (12.0, 8.6, 8.0)]
    for i, (x, y, z) in enumerate(spots):
        m.box(x, y, z, x + 1.8, y + 1.2, z + 1.6, {"side": cols[i % 3], "u": cols[0], "d": cols[2]})
    m.box(6.2, 5.0, 8.9, 6.8, 5.6, 9.4, "#7a4a20")
    return m


def pile(name, light, mid, dark, fleck, flecks=((6.0, 2.0), (9.0, 1.0), (7.5, 2.5))):
    m = M(name, scale=1.25)
    m.cyl(8, 8, 5.2, 0, 1.2, {"side": dark, "u": mid, "d": dark}, n=2)
    m.cyl(8, 8, 3.8, 1.2, 2.4, {"side": mid, "u": light, "d": dark}, n=2)
    m.cyl(8, 8, 2.2, 2.4, 3.4, {"side": light, "u": light, "d": mid}, n=2)
    for (x, z) in flecks:
        m.box(x + 1, 2.5, z + 5, x + 1.8, 3.1, z + 5.8, fleck)
    m.box(5.0, 1.2, 5.0, 5.7, 1.9, 5.7, fleck)
    return m


@model
def white_oak_ash():
    return pile("white_oak_ash", "#ecebe6", "#c2c2bc", "#8e9094", "#d8c8a0")


@model
def mountain_ash_dust():
    return pile("mountain_ash_dust", "#4a4a52", "#2c2c34", "#16161c", "#6a6a74")


@model
def yellow_wolfsbane_ash():
    return pile("yellow_wolfsbane_ash", "#d4d2cc", "#a8a8a2", "#74767a", "#e8cc30")


@model
def northern_blue_wolfsbane_ash():
    return pile("northern_blue_wolfsbane_ash", "#d4d2cc", "#a8a8a2", "#74767a", "#4a5ae0")


@model
def wolf_lichen_ash():
    return pile("wolf_lichen_ash", "#d4d2cc", "#a8a8a2", "#74767a", "#b4c82c")


def seeds(name, s1, s2, s3, label):
    m = M(name, scale=1.2)
    m.box(4.5, 0.0, 5.5, 11.5, 7.0, 10.5, {"side": "#c8a878", "u": "#d8b888", "d": "#a88858"})
    m.box(5.0, 7.0, 6.0, 11.0, 8.4, 10.0, "#b89868")
    m.box(4.3, 4.6, 5.3, 11.7, 5.1, 10.7, "#8a6a3a")
    m.box(5.6, 1.4, 10.45, 10.4, 4.2, 10.6, label)
    m.box(6.4, 2.0, 10.6, 9.6, 3.6, 10.7, s1)
    for (x, z, a) in ((3.5, 6.0, s1), (12.0, 8.0, s2), (6.0, 11.4, s3), (10.0, 4.0, s1), (2.8, 9.0, s2), (12.4, 5.2, s3)):
        m.box(x, 0.0, z, x + 1.0, 0.8, z + 0.7, a)
    return m


@model
def vervain_seeds():
    return seeds("vervain_seeds", "#6a4a2a", "#8a6a3a", "#3e2a16", "#b48ad8")


@model
def yellow_wolfsbane_seeds():
    return seeds("yellow_wolfsbane_seeds", "#d8b830", "#f4e068", "#9a7a14", "#f4e878")


@model
def northern_blue_wolfsbane_seeds():
    return seeds("northern_blue_wolfsbane_seeds", "#3a4ac0", "#7a8ae8", "#222a88", "#7a8ae8")


@model
def vervain_tea():
    m = M("vervain_tea", scale=1.15)
    m.cyl(8, 8, 6.0, 0, 0.7, {"side": "#d4d4cc", "u": "#ecece6", "d": "#b8b8b0"}, n=2)
    m.cyl(8, 8, 3.8, 0.7, 5.0, {"side": "#ecece6", "u": "#9aaa4a", "d": "#d4d4cc"}, n=2)
    m.cyl(8, 8, 3.9, 4.6, 5.0, "#c9a24a")
    m.cyl(8, 8, 3.1, 5.0, 5.1, "#9aaa4a")
    m.box(11.6, 1.6, 7.2, 13.4, 3.8, 8.8, "#ecece6")
    m.box(11.6, 1.6, 7.2, 12.2, 3.8, 8.8, "#d4d4cc")
    m.box(7.8, 5.3, 7.8, 8.2, 5.4, 8.2, "#b48ad8")
    for (x, y, z) in ((6.6, 6.5, 8.0), (8.0, 8.0, 7.6), (7.2, 9.6, 8.2)):
        m.box(x, y, z, x + 0.7, y + 0.9, z + 0.7, "#e8f0e8")
    return m


# ---------------------------------------------------------------- boss rewards
def book(name, cover, cover_hi, cover_lo, trim, deco, ribbon=None):
    m = M(name, scale=0.9)
    m.box(3.0, 0.5, 6.0, 13.0, 14.5, 6.7, {"side": cover_lo, "u": cover_lo, "d": cover_lo})
    m.box(3.5, 0.9, 6.7, 12.5, 14.1, 9.3, {"side": "#efe3c0", "u": "#c8b48a", "d": "#c8b48a", "w": cover_lo})
    m.box(3.0, 0.5, 9.3, 13.0, 14.5, 10.0, {"side": cover, "u": cover_hi, "d": cover_lo})
    m.box(2.6, 0.5, 6.0, 3.6, 14.5, 10.0, {"side": cover_lo, "u": cover, "d": cover_lo})
    for (x, y) in ((3.0, 0.5), (11.6, 0.5), (3.0, 13.1), (11.6, 13.1)):
        m.box(x, y, 9.9, x + 1.4, y + 1.4, 10.15, trim)
    deco(m)
    if ribbon:
        m.box(7.4, -2.0, 8.0, 8.6, 0.8, 8.4, ribbon)
    return m


@model
def kol_grimoire():
    def deco(m):
        m.box(5.0, 6.0, 10.0, 11.0, 10.0, 10.15, "#2c1640")
        m.box(5.6, 7.4, 10.15, 10.4, 8.6, 10.3, "#6fe0d0")
        m.box(6.6, 8.6, 10.15, 9.4, 9.2, 10.3, "#6fe0d0")
        m.box(6.6, 6.8, 10.15, 9.4, 7.4, 10.3, "#6fe0d0")
        m.box(7.5, 7.5, 10.3, 8.5, 8.5, 10.4, "#d8fff8")
    return book("kol_grimoire", "#4a2a66", "#6a3f8c", "#2c1640", "#b8c0cc", deco)


@model
def esther_grimoire():
    def deco(m):
        for ox in (0.0, 2.2):
            x = 5.2 + ox
            m.box(x, 6.0, 10.0, x + 3.4, 6.6, 10.2, "#e0b040")
            m.box(x, 9.4, 10.0, x + 3.4, 10.0, 10.2, "#e0b040")
            m.box(x, 6.0, 10.0, x + 0.6, 10.0, 10.2, "#e0b040")
            m.box(x + 2.8, 6.0, 10.0, x + 3.4, 10.0, 10.2, "#e0b040")
    return book("esther_grimoire", "#6b3a1e", "#8f5530", "#3f2010", "#e0b040", deco, ribbon="#a31622")


@model
def memory_claws():
    m = M("memory_claws", scale=1.15)
    m.cyl(8, 8, 3.4, 0, 3.0, {"side": "#5a2a30", "u": "#7a3a40", "d": "#3a1a20"})
    m.cyl(8, 8, 3.5, 2.2, 2.9, "#7a1020")
    m.cyl(8, 8, 2.6, 3.0, 6.4, {"side": "#6fe0e8", "u": "#c8fbff", "d": "#3a9aa8"}, n=2)
    m.box(6.2, 3.8, 5.5, 7.4, 5.4, 5.7, "#c8fbff")
    for (ang, x0) in ((-22.5, 5.6), (0, 7.4), (22.5, 9.2)):
        o = [8, 6, 8]
        m.box(x0 + 0.1, 6.0, 7.5, x0 + 1.5, 9.5, 8.5, {"side": "#ece4d2", "u": "#ece4d2", "d": "#b8ae96"}, rot=("z", ang), org=o)
        m.box(x0 + 0.4, 9.5, 7.65, x0 + 1.2, 12.5, 8.35, {"side": "#d8d0bc", "u": "#ece4d2", "d": "#b8ae96"}, rot=("z", ang), org=o)
        m.box(x0 + 0.7, 12.5, 7.8, x0 + 0.9, 14.5, 8.2, "#b8ae96", rot=("z", ang), org=o)
    return m


@model
def mantle_of_the_beast():
    m = M("mantle_of_the_beast", scale=0.85)
    b, l, h, d = "#2b2640", "#433c5f", "#5f5785", "#0c0912"
    m.box(5.5, 10.0, 5.5, 10.5, 15.0, 10.5, {"side": b, "u": h, "d": b})
    m.box(6.4, 10.8, 5.3, 9.6, 14.0, 5.5, d)
    m.box(6.8, 12.0, 5.2, 7.5, 12.6, 5.35, "#e02030")
    m.box(8.5, 12.0, 5.2, 9.2, 12.6, 5.35, "#e02030")
    m.box(4.0, 8.0, 5.0, 12.0, 10.0, 11.0, {"side": b, "u": l, "d": b})
    m.box(3.5, 5.0, 5.4, 12.5, 8.0, 10.6, {"side": b, "u": l, "d": b})
    m.box(3.0, 2.0, 5.4, 13.0, 5.0, 10.6, {"side": b, "u": l, "d": b})
    m.box(2.6, 0.0, 5.4, 13.4, 2.0, 10.6, {"side": l, "u": l, "d": d})
    for x in (3.2, 6.0, 9.0, 12.0):
        m.box(x, -0.8, 5.6, x + 1.0, 0.0, 10.4, d)
    m.box(7.2, 9.2, 4.9, 8.8, 10.2, 5.1, "#c8a040")
    for x in (6.2, 9.6):
        m.box(x, 2.0, 4.9, x + 0.5, 8.0, 5.3, d)
    return m


def eye(name, r, body, iris, pupil, ring_col=None, rays=None, ray2=None, scale=1.15):
    m = M(name, scale=scale)
    cy = 8.0
    rr = r
    q = rr * 0.72
    m.box(8 - rr, cy - q, 8 - q, 8 + rr, cy + q, 8 + q, body)
    m.box(8 - q, cy - rr, 8 - q + 0.004, 8 + q, cy + rr, 8 + q - 0.004, body)
    m.box(8 - q + 0.004, cy - q + 0.004, 8 - rr, 8 + q - 0.004, cy + q - 0.004, 8 + rr, body)
    for z0, z1 in ((8 - rr - 0.12, 8 - rr + 0.04), (8 + rr - 0.04, 8 + rr + 0.12)):
        m.box(8 - rr * 0.6, cy - rr * 0.6, z0, 8 + rr * 0.6, cy + rr * 0.6, z1, iris)
        m.box(8 - rr * 0.14, cy - rr * 0.5, z0 - 0.04 if z0 < 8 else z0, 8 + rr * 0.14, cy + rr * 0.5, z1 if z0 < 8 else z1 + 0.04, pupil)
        if ring_col:
            m.box(8 - rr * 0.68, cy + rr * 0.6, z0 - 0.02 if z0 < 8 else z0, 8 + rr * 0.68, cy + rr * 0.68, z1 if z0 < 8 else z1 + 0.02, ring_col)
            m.box(8 - rr * 0.68, cy - rr * 0.68, z0 - 0.02 if z0 < 8 else z0, 8 + rr * 0.68, cy - rr * 0.6, z1 if z0 < 8 else z1 + 0.02, ring_col)
    if rays:
        L = rr + 3.0
        m.box(7.5, cy + rr, 7.5, 8.5, cy + L, 8.5, rays)
        m.box(7.5, cy - L, 7.5, 8.5, cy - rr, 8.5, rays)
        m.box(8 + rr, cy - 0.5, 7.5, 8 + L, cy + 0.5, 8.5, rays)
        m.box(8 - L, cy - 0.5, 7.5, 8 - rr, cy + 0.5, 8.5, rays)
    if ray2:
        for (a, o) in ((45, [8, cy, 8]), (-45, [8, cy, 8])):
            m.box(7.6, cy + rr - 0.5, 7.6, 8.4, cy + rr + 4.0, 8.4, ray2, rot=("z", a), org=o)
            m.box(7.6, cy - rr - 4.0, 7.6, 8.4, cy - rr + 0.5, 8.4, ray2, rot=("z", a), org=o)
    return m


@model
def alpha_spark():
    return eye("alpha_spark", 3.6, "#b01828", "#ffa060", "#1a0508", rays="#d01c2c")


@model
def true_alpha_spark():
    return eye("true_alpha_spark", 3.8, "#d01818", "#ffe090", "#1a0508", ring_col="#ffd45a", rays="#ff3a30", ray2="#ffd45a")


@model
def npc_alpha_spark_0():
    return eye("npc_alpha_spark_0", 1.6, "#a07818", "#ffe070", "#3a2a08", scale=1.3)


@model
def npc_alpha_spark_1():
    return eye("npc_alpha_spark_1", 2.6, "#c06a14", "#ffd060", "#3a1a08", scale=1.2)


@model
def npc_alpha_spark_2():
    return eye("npc_alpha_spark_2", 3.4, "#c83418", "#ffa850", "#3a1008", scale=1.15)


# ---------------------------------------------------------------- compass (face texture is the animated frame)
@model
def vampire_compass():
    m = M("vampire_compass", scale=1.35)
    brass, hi, lo = "#b8903c", "#d8b460", "#7a5a20"
    m.disc(8, 7, 5.5, 6.5, 9.5, {"side": brass, "u": hi, "d": lo, "n": brass, "s": brass}, n=2)
    m.disc(8, 7, 5.9, 6.2, 6.5, {"side": lo, "u": lo, "d": lo, "n": lo, "s": lo}, n=2)
    m.disc(8, 7, 5.9, 9.5, 9.8, {"side": lo, "u": lo, "d": lo, "n": lo, "s": lo}, n=2)
    m.box(2.5, 1.5, 6.0, 13.5, 12.5, 6.15, "#ffffff", tex="face")
    m.box(2.5, 1.5, 9.85, 13.5, 12.5, 10.0, "#ffffff", tex="face")
    m.box(7.0, 12.9, 7.2, 9.0, 14.0, 8.8, hi)
    m.box(6.2, 14.0, 7.4, 9.8, 14.5, 8.6, brass)
    m.box(6.2, 15.0, 7.4, 9.8, 15.5, 8.6, brass)
    m.box(6.2, 14.0, 7.4, 6.7, 15.5, 8.6, brass)
    m.box(9.3, 14.0, 7.4, 9.8, 15.5, 8.6, brass)
    m.extra_tex = {"face": "supernaturalbloodlines:item/vampire_compass_00"}
    return m


def write_all():
    SPEC.mkdir(parents=True, exist_ok=True)
    TEX3D.mkdir(parents=True, exist_ok=True)
    for n, m in MODELS.items():
        assert len(m.pal) <= 256, n
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for h, i in m.pal.items():
            im.putpixel((i % 16, i // 16), hexc(h))
        im.save(TEX3D / f"{n}.png")
        (SPEC / f"{n}.json").write_text(json.dumps(m.spec(), indent=1))
    print(len(MODELS), "3d specs")


if __name__ == "__main__":
    write_all()


