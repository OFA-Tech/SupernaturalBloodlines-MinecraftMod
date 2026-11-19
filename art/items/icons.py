"""2D inventory icons (16x16) drawn with draw.Canvas. ICONS maps item id -> Canvas.

Colours follow the show/real-plant research in REFERENCES.md.
"""
from draw import Canvas

ICONS = {}


def icon(fn):
    ICONS[fn.__name__] = fn()
    return fn


def grid(pal, rows):
    c = Canvas()
    assert len(rows) == 16, len(rows)
    for y, r in enumerate(rows):
        assert len(r) == 16, (y, r)
        for x, ch in enumerate(r):
            if ch != ".":
                c.d(x, y, pal[ch])
    return c


# ---------------------------------------------------------------- blood vials
def vial(liq, hi, lo, cork, cork_hi, band=None, flecks=(), fleck="#e8b830", glass="#cfdde4", out="#2c1a1e"):
    c = Canvas()
    c.r(6, 1, 9, 2, cork)
    c.pts(cork_hi, (6, 1), (7, 1))
    c.r(7, 3, 8, 4, glass)
    if band:
        c.r(7, 4, 8, 4, band)
    c.r(5, 5, 10, 5, glass)
    c.r(3, 6, 12, 12, glass)
    c.r(4, 6, 11, 11, liq)
    c.r(4, 6, 4, 9, hi)
    c.r(8, 10, 11, 11, lo)
    c.r(4, 11, 7, 11, lo)
    c.pts("#ffffff", (3, 7), (3, 8))
    for p in flecks:
        c.d(p[0], p[1], fleck)
    c.outline(out)
    return c


@icon
def original_blood():
    return vial("#8c010d", "#b02030", "#5c000a", "#d8e0e6", "#ffffff", band="#c9a24a",
                flecks=[(7, 8), (9, 7), (6, 10)], fleck="#e8c060")


@icon
def doppelganger_blood():
    return vial("#a8142c", "#d03a50", "#6e0a1c", "#6b4f2a", "#8a6a3a", band="#3aa8b8",
                flecks=[(8, 8)], fleck="#9ee8f0")


@icon
def hybrid_blood():
    return vial("#7a0a14", "#a02030", "#4c0610", "#4a3220", "#6a4a30", band="#d89a20",
                flecks=[(6, 8), (9, 9), (8, 7)], fleck="#f0b830")


@icon
def sires_blood():
    return vial("#6e0a2c", "#9a2048", "#44061c", "#201820", "#3a2c3a", band="#8a4ac0",
                flecks=[(9, 8)], fleck="#c890f0")


@icon
def the_cure():
    c = Canvas()
    c.r(6, 1, 9, 2, "#b8c0cc")
    c.pts("#e8eef4", (6, 1), (7, 1))
    c.r(6, 3, 9, 3, "#8a0c14")
    c.r(7, 4, 8, 5, "#cfdde4")
    c.r(5, 6, 10, 13, "#cfdde4")
    c.r(6, 6, 9, 12, "#a0101c")
    c.r(6, 6, 6, 9, "#d03040")
    c.r(8, 11, 9, 12, "#5c0810")
    c.pts("#ffffff", (5, 7), (5, 8))
    c.outline("#2c1a1e")
    return c


# ---------------------------------------------------------------- rings
def ring(light, mid, dark, out, stone, cx=7.5, cy=11.0, orx=5.6, ory=4.6, irx=2.6, iry=2.0):
    """Small shank (hole shrunk) with a stone that dominates the top of the icon."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            v = ((x - cx) / orx) ** 2 + ((y - cy) / ory) ** 2
            h = ((x - cx) / irx) ** 2 + ((y - cy) / iry) ** 2
            if v <= 1 and h > 1:
                s = x + y
                c.d(x, y, light if s < 16 else (mid if s < 20 else dark))
    stone(c, light, mid, dark)
    c.outline(out)
    return c


@icon
def sun_ring():
    def stone(c, l, m, d):
        c.r(4, 2, 11, 8, l)
        c.pts(l, (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1))
        c.r(4, 8, 11, 8, m)
        c.r(5, 2, 10, 7, "#1f3a9a")
        c.r(5, 2, 6, 4, "#3d5fc0")
        c.r(9, 6, 10, 7, "#152a78")
        c.pts("#a8c0f0", (5, 2), (6, 3))
        c.pts("#e8c850", (7, 3), (9, 3), (7, 4), (8, 4), (9, 4), (8, 5))
        c.pts("#e8c850", (6, 6))
    return ring("#e0e6ee", "#a2abbc", "#6a7388", "#232a3c", stone)


@icon
def moon_ring():
    def stone(c, l, m, d):
        c.ell(7.5, 4.5, 4.6, 4.6, l)
        c.pts(l, (7, 0), (8, 0))
        c.ell(7.5, 4.5, 3.4, 3.4, "#16264a")
        c.ell(7.2, 4.2, 2.4, 2.4, "#1f3566")
        c.pts("#0c1530", (10, 5), (10, 6), (9, 7), (8, 7), (7, 7), (10, 4))
        c.pts("#3f6fb0", (6, 6), (7, 5), (8, 4), (9, 3), (6, 3))
        c.pts("#6fa6dc", (5, 6), (6, 5), (7, 4), (8, 3), (9, 2))
        c.pts("#e8f4ff", (5, 4))
        c.pts(m, (3, 4), (12, 4), (7, 9), (8, 9))
    return ring("#dde4f0", "#9fa9be", "#667090", "#232a3c", stone, cx=7.5, cy=11.5, orx=5.2, ory=4.2, irx=2.4, iry=1.9)


@icon
def resurrection_ring():
    def stone(c, l, m, d):
        c.r(3, 1, 12, 8, "#b9c0cc")
        c.r(3, 8, 12, 8, "#6a7388")
        c.r(4, 2, 11, 7, "#0e0e14")
        c.r(4, 2, 7, 3, "#2a2a38")
        c.pts("#4a4a5e", (4, 2), (5, 2), (4, 3), (11, 6), (10, 7))
        c.pts("#e8ecf2", (3, 1), (12, 1), (3, 8), (12, 8))
        c.pts("#4a5164", (2, 9), (13, 9), (3, 10), (12, 10))
    return ring("#c8d0dc", "#8a93a6", "#4e566a", "#171b28", stone, cx=7.5, cy=11.5, orx=6.4, ory=4.6, irx=2.4, iry=1.7)


# ---------------------------------------------------------------- poppets
def poppet(cloth, hi, lo, out, stitch, sym):
    c = Canvas()
    c.r(5, 1, 10, 5, cloth)
    c.r(2, 6, 13, 7, cloth)
    c.r(5, 6, 10, 11, cloth)
    c.r(5, 12, 7, 14, cloth)
    c.r(8, 12, 10, 14, cloth)
    c.r(5, 1, 6, 4, hi)
    c.r(10, 2, 10, 5, lo)
    c.r(9, 6, 13, 7, lo)
    c.r(10, 8, 10, 11, lo)
    c.r(8, 12, 10, 14, lo)
    c.pts("#1e1410", (6, 3), (9, 3))
    c.pts(stitch, (7, 4), (8, 4), (3, 6), (3, 7), (12, 6), (12, 7), (5, 13), (6, 13), (9, 13), (10, 13))
    sym(c)
    c.outline(out)
    return c


@icon
def sun_poppet():
    def sym(c):
        c.r(7, 8, 8, 9, "#fff3b0")
        c.pts("#fff3b0", (6, 8), (9, 8), (6, 9), (9, 9), (7, 7), (8, 10))
    return poppet("#f0c040", "#f8dc78", "#c08a1c", "#5a3c0a", "#8a5a14", sym)


@icon
def silver_poppet():
    def sym(c):
        c.pts("#ffffff", (7, 8), (8, 8), (7, 9), (8, 9), (6, 9), (9, 8))
    return poppet("#cfd5de", "#eef1f6", "#8f98a8", "#3a4054", "#6a7488", sym)


@icon
def spirit_poppet():
    def sym(c):
        c.pts("#ffffff", (7, 7), (8, 8), (7, 9), (8, 10), (7, 10))
    return poppet("#a8d8f0", "#d0eefc", "#6aa4d0", "#24476e", "#4a82b0", sym)


# ---------------------------------------------------------------- blades
def blade_icon(hi, mid, lo, guard, grip, pommel, tipdark=None, extra=None):
    c = Canvas()
    c.d(14, 1, hi)
    for i in range(8):
        x, y = 12 - i, 2 + i
        c.d(x, y, hi)
        c.d(x + 1, y, mid)
        c.d(x + 2, y, lo)
    for p in [(3, 8), (4, 9), (5, 10), (6, 11), (7, 12)]:
        c.d(p[0], p[1], guard)
    for p in [(4, 11), (5, 11), (3, 12), (4, 12), (2, 13), (3, 13)]:
        c.d(*p, grip)
    c.pts(pommel, (1, 14), (2, 14))
    if extra:
        extra(c)
    c.outline("#20242e")
    return c


@icon
def silver_dagger():
    return blade_icon("#f4f8fc", "#c8d2de", "#8e9aab", "#6a7388", "#2a2a34", "#b8c0cc")


@icon
def white_oak_ash_dagger():
    def ash(c):
        c.pts("#d8d6cc", (6, 8), (7, 8), (7, 9), (8, 8), (6, 9))
        c.pts("#a8a69c", (5, 9), (8, 9))
    return blade_icon("#f4f8fc", "#c8d2de", "#8e9aab", "#8a93a4", "#4a3a2c", "#c8d2de", extra=ash)


def stake_icon(collar=None):
    c = Canvas()
    for i in range(10):
        x, y = 13 - i, 2 + i
        c.pts("#e2d6bc", (x, y), (x + 1, y))
        c.d(x + 2, y, "#bcae90")
    c.pts("#968a70", (14, 1), (15, 2))
    c.pts("#d4c8ae", (13, 1), (14, 2))
    c.pts("#968a70", (5, 11), (4, 12), (3, 13))
    if collar:
        for p in [(3, 9), (4, 10), (5, 11), (6, 12), (7, 13)]:
            c.d(*p, collar)
        for p in [(4, 11), (5, 12), (3, 12), (4, 13), (2, 14)]:
            c.d(*p, "#4a2e1a")
        c.pts("#a31622", (1, 15))
    c.outline("#4a3f2a")
    return c


@icon
def white_oak_stake():
    return stake_icon()


@icon
def mikaels_stake():
    return stake_icon("#e0b040")


@icon
def valet_pike():
    c = Canvas()
    for i in range(12):
        x, y = 11 - i + 1, 4 + i
        c.pts("#5a4430", (x, y))
        c.d(x + 1, y, "#3f2e20")
    for p in [(5, 10), (4, 11), (3, 12)]:
        c.d(*p, "#4a3426")
        c.d(p[0] + 1, p[1], "#2e2018")
    c.pts("#8a94a4", (1, 14), (0, 15))
    # steel fleur-de-lis head: central blade, two curled petals, collar
    c.pts("#e0e8f0", (14, 0), (14, 1), (13, 2), (12, 3), (11, 4))
    c.pts("#b8c0cc", (15, 1), (14, 2), (13, 3), (12, 4))
    c.pts("#8a7ae0", (13, 3), (12, 4))
    c.pts("#e0e8f0", (9, 4), (8, 3), (8, 2), (8, 1), (13, 8), (14, 8), (15, 7), (12, 7))
    c.pts("#b8c0cc", (10, 5), (11, 6))
    c.pts("#7a8494", (10, 4), (11, 5), (10, 6), (11, 7), (9, 6))
    c.outline("#20242e")
    return c


# ---------------------------------------------------------------- misc gear
@icon
def taser():
    c = Canvas()
    for i in range(9):
        x, y = 2 + i, 13 - i
        c.pts("#2a2a32", (x, y), (x + 1, y))
        c.d(x + 1, y + 1, "#14141a")
    for p in [(4, 11), (5, 10), (6, 9)]:
        c.d(*p, "#6a6a78")
    c.pts("#b8c0cc", (11, 4), (12, 3), (12, 4), (13, 3))
    c.pts("#8a94a4", (10, 4), (13, 2), (11, 5))
    c.pts("#6fe0ff", (13, 5), (14, 3), (12, 2), (14, 1), (15, 4))
    c.pts("#ffffff", (14, 2), (13, 4))
    c.outline("#101018")
    return c


@icon
def hearing_device():
    c = Canvas()
    c.r(7, 6, 14, 12, "#34363e")
    c.r(7, 6, 14, 7, "#4a4d58")
    c.r(7, 12, 14, 12, "#1c1d24")
    c.r(2, 4, 6, 14, "#b8c0cc")
    c.r(3, 3, 6, 3, "#b8c0cc")
    c.r(3, 15, 6, 15, "#8a94a4")
    c.r(2, 4, 3, 13, "#e0e8f0")
    c.r(4, 6, 6, 12, "#4a4d58")
    c.r(5, 8, 6, 10, "#1c1d24")
    c.r(13, 2, 13, 5, "#8a94a4")
    c.pts("#e02030", (13, 1), (12, 9))
    c.pts("#6a6e7a", (9, 9), (10, 9), (11, 9), (9, 10), (10, 10), (11, 10))
    c.outline("#101018")
    return c


@icon
def moonstone():
    c = Canvas()
    c.ell(7.5, 8, 5, 6, "#eceeea")
    c.ell(8.8, 9.4, 3.6, 4.2, "#d3d9da")
    c.ell(9.4, 10.4, 2.4, 2.8, "#b4c2cc")
    c.r(4, 6, 5, 9, "#f8faf8")
    c.pts("#ffffff", (5, 4), (6, 4), (5, 5))
    c.pts("#cfe0f2", (8, 6), (9, 6), (7, 7), (8, 7))
    c.outline("#6f7e8a")
    return c


# ---------------------------------------------------------------- plants
def stem(c, x0, y0, x1, y1, col="#3e8a34"):
    c.line(x0, y0, x1, y1, col)


@icon
def vervain():
    c = Canvas()
    g, G = "#3e8a34", "#2a6a26"
    stem(c, 7, 15, 7, 7, g)
    stem(c, 7, 11, 4, 7, g)
    stem(c, 7, 10, 11, 6, g)
    for x, y in ((7, 7), (4, 7), (11, 6)):
        for k in range(3):
            c.pts("#b48ad8", (x, y - 1 - k))
        c.pts("#d4b4f0", (x, y - 1))
        c.pts("#8a5cb8", (x, y - 3))
        c.pts("#b48ad8", (x + 1, y - 2) if x != 7 else (x - 1, y - 2))
    c.pts(G, (5, 13), (6, 12), (9, 13), (8, 12), (5, 10), (9, 9))
    c.pts(g, (4, 13), (10, 13), (4, 10), (10, 9))
    c.outline("#1e2e1e")
    return c


def hood(c, x, y, hi, mid, lo):
    c.pts(hi, (x, y), (x + 1, y))
    c.pts(mid, (x - 1, y + 1), (x, y + 1), (x + 1, y + 1), (x + 2, y + 1))
    c.pts(lo, (x, y + 2), (x + 1, y + 2))


def wolfsbane(hi, mid, lo, out):
    c = Canvas()
    g, G = "#3e8a34", "#2a6a26"
    stem(c, 8, 15, 8, 5, g)
    stem(c, 8, 9, 5, 6, g)
    stem(c, 8, 8, 11, 5, g)
    hood(c, 7, 1, hi, mid, lo)
    hood(c, 4, 4, hi, mid, lo)
    hood(c, 10, 3, hi, mid, lo)
    c.pts(G, (5, 13), (6, 12), (7, 13), (10, 13), (9, 12), (11, 12), (5, 11), (11, 10))
    c.pts(g, (4, 12), (6, 14), (10, 14), (12, 13))
    c.outline(out)
    return c


@icon
def yellow_wolfsbane():
    return wolfsbane("#f4e878", "#e8cc30", "#b89a14", "#1e2e1e")


@icon
def northern_blue_wolfsbane():
    return wolfsbane("#7a8ae8", "#3a4ac0", "#222a88", "#1e2e1e")


@icon
def wolf_lichen():
    c = Canvas()
    c.line(2, 13, 13, 5, "#6a4a30")
    c.line(2, 14, 13, 6, "#4a3220")
    c.line(7, 10, 9, 14, "#6a4a30")
    cols = ["#d8e450", "#b4c82c", "#8aa020"]
    for (x, y) in [(3, 11), (5, 10), (7, 8), (10, 6), (12, 4), (8, 11), (9, 13), (4, 12), (6, 7), (11, 8)]:
        c.pts(cols[0], (x, y), (x + 1, y))
        c.pts(cols[1], (x, y - 1), (x - 1, y + 1))
        c.pts(cols[2], (x + 1, y + 1))
    c.pts("#7a4a20", (6, 9), (11, 7), (4, 13))
    c.outline("#2e3a14")
    return c


def pile(light, mid, dark, flecks, fleck):
    c = Canvas()
    for y, (x0, x1) in zip(range(7, 14), [(7, 8), (6, 9), (5, 10), (4, 11), (3, 12), (2, 13), (1, 14)]):
        for x in range(x0, x1 + 1):
            t = (x - 7.5) * 0.4 + (y - 7) * 0.5
            c.d(x, y, light if t < 0.9 else (mid if t < 2.4 else dark))
    c.pts(light, (6, 6), (9, 5), (5, 14), (11, 14))
    for p in flecks:
        c.d(*p, fleck)
    c.outline(dark if False else "#303236")
    return c


@icon
def white_oak_ash():
    return pile("#ecebe6", "#c2c2bc", "#8e9094", [(6, 10), (9, 9)], "#d8c8a0")


@icon
def mountain_ash_dust():
    return pile("#4a4a52", "#2c2c34", "#16161c", [(6, 10), (9, 9), (8, 12)], "#6a6a74")


@icon
def yellow_wolfsbane_ash():
    return pile("#d4d2cc", "#a8a8a2", "#74767a", [(6, 10), (9, 9), (7, 12)], "#e8cc30")


@icon
def northern_blue_wolfsbane_ash():
    return pile("#d4d2cc", "#a8a8a2", "#74767a", [(6, 10), (9, 9), (7, 12)], "#4a5ae0")


@icon
def wolf_lichen_ash():
    return pile("#d4d2cc", "#a8a8a2", "#74767a", [(6, 10), (9, 9), (7, 12)], "#b4c82c")


def seeds(c1, c2, c3):
    c = Canvas()
    for (x, y) in [(4, 5), (9, 4), (6, 8), (11, 8), (3, 11), (8, 12), (12, 12)]:
        c.pts(c1, (x, y), (x + 1, y + 1))
        c.d(x + 1, y, c2)
        c.d(x, y + 1, c3)
    c.outline("#2a2218")
    return c


@icon
def vervain_seeds():
    return seeds("#6a4a2a", "#8a6a3a", "#3e2a16")


@icon
def yellow_wolfsbane_seeds():
    return seeds("#d8b830", "#f4e068", "#9a7a14")


@icon
def northern_blue_wolfsbane_seeds():
    return seeds("#3a4ac0", "#7a8ae8", "#222a88")


@icon
def vervain_tea():
    c = Canvas()
    c.r(3, 6, 11, 6, "#f4f4f0")
    c.r(3, 7, 11, 11, "#ecece6")
    c.r(4, 12, 10, 12, "#d4d4cc")
    c.r(3, 6, 11, 6, "#c9a24a")
    c.r(4, 7, 10, 7, "#9aaa4a")
    c.r(4, 8, 4, 10, "#d8d8d0")
    c.r(8, 9, 10, 11, "#d0d0c8")
    c.pts("#c9a24a", (4, 10), (5, 10), (6, 10))
    c.line(12, 8, 13, 8, "#ecece6")
    c.line(13, 8, 13, 10, "#ecece6")
    c.line(12, 11, 13, 10, "#ecece6")
    c.r(2, 13, 12, 13, "#d4d4cc")
    c.pts("#b48ad8", (13, 12), (14, 12), (14, 11), (14, 13))
    c.pts("#3e8a34", (13, 13), (12, 12))
    c.pts("#d8e8e0", (6, 3), (7, 2), (6, 1), (8, 4), (9, 3))
    c.outline("#3a3a36")
    return c
