"""The shared icon set: one drawing per icon on a 32 grid, drawn at 32 px (hub) and at 16 px (HUD, lists).

The bevel stays 1 texel at both sizes, so the 16 px icon is drawn, not shrunk: thin parts keep a whole texel and each
icon may branch on `I.big` where the small one needs its own shapes. Materials share the ramps of the style guide.
"""
import math
import os

from px import BLACK, GUI, PHASE2, WHITE, Cv, emit, mix, ramp, rgba, sheet, with_a

IR = dict(
    bone=ramp('#4A3A2A', '#FFFFF2', '#E9E0C8', '#B5A98A', '#FFFFFF', '#9A8E70'), blood=ramp('#3A0A10', '#FF6A60', '#C01830', '#7A0E1E', '#FF8F85', '#5E0A16'),
    gum=ramp('#2A0608', '#B02A3A', '#7A1224', '#4A0A16', '#9A2234', '#36070F'), gold=ramp('#4A3208', '#FFF0B0', '#E8B33A', '#A8761A', '#FFD86A', '#8A5E10'),
    moon=ramp('#4E3A44', '#FFFFFF', '#F2E6D0', '#C9B6A0', '#FFF8EA', '#AE9A86'), fur=ramp('#1E1A22', '#9A8E94', '#6A5A62', '#443840', '#7E7076', '#2E2630'),
    violet=ramp('#1E0F33', '#C9A6FF', '#8A5AD6', '#5A3A9A', '#A47EEA', '#3E2670'), night=ramp('#0E0820', '#3A2A5E', '#22163E', '#140C28', '#2E2250', '#0E0820'),
    teal=ramp('#0F3A44', '#D8FEFF', '#3FD0D6', '#1F8A94', '#8EEAEE', '#17707A'), paper=ramp('#5A3A1A', '#FFF6DC', '#E9DDB8', '#BBA874', '#FFFFFF', '#A08C5A'),
    steel=ramp('#2A2E36', '#E4EAF2', '#9AA4B4', '#5E6676', '#C2CAD8', '#454C5A'), paperD=ramp('#5A3A1A', '#E2D4A8', '#CDBB8A', '#9E8A58'),
    green=ramp('#12301A', '#B8F08A', '#4FAE4A', '#2C7A34', '#84D66A', '#1F5A28'), fire=ramp('#4A1A06', '#FFE9A0', '#F08A2A', '#B8501A', '#FFC25A', '#8A3410'),
    ice=ramp('#2A4A5A', '#FFFFFF', '#BFEFF4', '#7CC4D2', '#E6FBFF', '#58A0B2'), dark=ramp('#08080C', '#5A5A66', '#2A2A32', '#17171C', '#3E3E48', '#101014'),
    demon=ramp('#0C1220', '#56698F', '#2D3A52', '#1A2230', '#38476A', '#141B28'), wood=ramp('#2A1608', '#C8935A', '#8A5A30', '#5E3A1C', '#A87440', '#44280F'),
    skin=ramp('#5A3220', '#F7E0CC', '#E8B898', '#C08A6A', '#F0CCB0', '#A8704C'), redmoon=ramp('#3A0A10', '#FFD9CF', '#E8786A', '#C8423A', '#F6B1A5', '#8A2424'),
    bat=ramp('#0E0820', '#9A7AD8', '#4A3478', '#2E2050', '#6A4EA8', '#22163E'),
    tan=ramp('#26221C', '#A29682', '#766B58', '#4F483B', '#867A67', '#353027'), sky=ramp('#16305A', '#CFE6FF', '#5A9AE0', '#2F62A8', '#8EC0F4', '#234A84'))
INK = rgba('#17121A')


class Ic:
    def __init__(self, n):
        self.n, self.k, self.big, self.cv = n, n / 32, n >= 32, Cv(n, n)

    # ---- masks from a list of ops in 32-grid units; an op whose name starts with '-' cuts ----
    def mask(self, ops):
        m, k = Cv(self.n, self.n), self.k
        for op in ops:
            kind, cut = op[0].lstrip('-'), op[0].startswith('-')
            t = Cv(self.n, self.n)
            if kind == 'd':
                t.disc(op[1] * k, op[2] * k, op[3] * k, (op[4] if len(op) > 4 else op[3]) * k, 1)
            elif kind == 'p':
                t.poly([(x * k, y * k) for x, y in op[1]], 1)
            elif kind == 'r':
                x0, y0 = round(op[1] * k), round(op[2] * k)
                t.rect(x0, y0, max(1, round((op[1] + op[3]) * k) - x0), max(1, round((op[2] + op[4]) * k) - y0), 1)
            elif kind == 'a':
                t.arc(op[1] * k, op[2] * k, op[3] * k, max(1.0, op[4] * k), op[5], op[6], 1)
            elif kind == 'l':
                x0, y0, x1, y1 = [v * k for v in op[1:5]]
                h = max(1.0, op[5] * k) / 2
                d = math.hypot(x1 - x0, y1 - y0) or 1
                nx, ny = -(y1 - y0) / d * h, (x1 - x0) / d * h
                t.poly([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)], 1)
            if cut:
                m.cut(t)
            else:
                m.stamp(0, 0, t, 1)
        return m

    def shape(self, r, ops, band=None):
        """Bevel a mask with a ramp: outline, highlight, base, shade."""
        self.cv.bevel(self.mask(ops), IR[r] if isinstance(r, str) else r, 0, 0, self.big if band is None else band)
        return self

    def paint(self, ops, c, within=None):
        """Flat colour, no outline; `within` keeps it inside another mask."""
        m = self.mask(ops)
        if within is not None:
            m.keep(self.mask(within))
        self.cv.stamp(0, 0, m, rgba(c))
        return self

    def bits(self, r, rows, x=0, y=0):
        """Bevel a mask typed row by row, in texels of this size."""
        m = Cv(self.n, self.n)
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == '#':
                    m.px(x + i, y + j, 1)
        self.cv.bevel(m, IR[r] if isinstance(r, str) else r, 0, 0, False)
        return self

    def dot(self, x, y, c, w=1, h=1):
        k = self.k
        self.cv.rect(round(x * k), round(y * k), w, h, rgba(c))
        return self

    def tx(self, x, y, c, w=1, h=1):
        """A detail placed in texels of this size, not in grid units."""
        self.cv.rect(x, y, w, h, rgba(c))
        return self


# ---------- motifs: lists of ops round a centre, at a scale ----------
def drop(cx=16, cy=16, s=1.0):
    return [('d', cx, cy + 4 * s, 9.5 * s), ('p', [(cx, cy - 13.5 * s), (cx + 8.6 * s, cy + 1 * s), (cx - 8.6 * s, cy + 1 * s)]),
            ('p', [(cx, cy - 13.5 * s), (cx + 6.5 * s, cy - 2 * s), (cx - 6.5 * s, cy - 2 * s)])]


def heart(cx=16, cy=16, s=1.0):
    return [('d', cx - 4.9 * s, cy - 3.6 * s, 5.9 * s), ('d', cx + 4.9 * s, cy - 3.6 * s, 5.9 * s),
            ('p', [(cx - 10.2 * s, cy - 1.2 * s), (cx + 10.2 * s, cy - 1.2 * s), (cx, cy + 10.5 * s)])]


def shield(cx=16, cy=16, s=1.0):
    return [('p', [(cx - 10 * s, cy - 11 * s), (cx + 10 * s, cy - 11 * s), (cx + 10 * s, cy), (cx + 5 * s, cy + 8 * s), (cx, cy + 12 * s), (cx - 5 * s, cy + 8 * s), (cx - 10 * s, cy)])]


def crescent(cx=16, cy=16, s=1.0):
    return [('d', cx - 1.5 * s, cy + 0.5 * s, 11.5 * s), ('-d', cx + 4.5 * s, cy - 2.5 * s, 9.8 * s)]


def star4(cx, cy, a, b):
    return [('p', [(cx, cy - a), (cx + b, cy - b), (cx + a, cy), (cx + b, cy + b), (cx, cy + a), (cx - b, cy + b), (cx - a, cy), (cx - b, cy - b)])]


def star5(cx, cy, R_, r_):
    pts = []
    for i in range(10):
        a, rad = -math.pi / 2 + i * math.pi / 5, (R_ if i % 2 == 0 else r_)
        pts.append((cx + math.cos(a) * rad, cy + math.sin(a) * rad))
    return [('p', pts)]


def paw_pad(cx=16, cy=16, s=1.0):
    return [('d', cx, cy + 7.2 * s, 7.8 * s, 5.6 * s), ('d', cx - 5 * s, cy + 3.6 * s, 3.8 * s, 3.4 * s), ('d', cx + 5 * s, cy + 3.6 * s, 3.8 * s, 3.4 * s), ('d', cx, cy + 2.6 * s, 4.6 * s, 3.2 * s)]


def paw_toes(cx=16, cy=16, s=1.0):
    return [[('d', cx + x * s, cy + y * s, rx * s, ry * s)] for x, y, rx, ry in ((-9, -2.4, 2.7, 3.5), (-3.6, -7.4, 2.9, 3.7), (3.6, -7.4, 2.9, 3.7), (9, -2.4, 2.7, 3.5))]


PAW16 = ['.....##..##.....', '.....##..##.....', '................', '..##........##..', '..##..####..##..', '.....######.....', '....########....', '....########....',
         '....########....', '.....##..##.....']


def paw(I, cx=16, cy=16, s=1.0, r='fur'):
    if not I.big and s == 1.0:
        return I.bits(r, PAW16, 0, 2 + round((cy - 16) / 2))
    for t in paw_toes(cx, cy, s):
        I.shape(r, t)
    return I.shape(r, paw_pad(cx, cy, s))


def boot(cx=16, cy=16, s=1.0):
    P = [(-6, -11), (2, -11), (2, 1), (8, 3), (11, 6), (11, 11), (-7, 11), (-7, 6), (-6, 4)]
    return [('p', [(cx + x * s, cy + y * s) for x, y in P])]


def bolt(cx=16, cy=16, s=1.0):
    P = [(3, -13), (-8, 2), (-1, 2), (-4, 13), (8, -3), (1, -3)]
    return [('p', [(cx + x * s, cy + y * s) for x, y in P])]


def waves(cx, cy, radii, t=2.4, a0=48, a1=132):
    return [('a', cx, cy, r, t, a0, a1) for r in radii]


def flame(cx=16, cy=16, s=1.0):
    P = [(0, -12), (5, -4), (7, 3), (5, 9), (0, 12), (-5, 9), (-7, 3), (-4, -2), (-2, 1), (-3, -6)]
    return [('p', [(cx + x * s, cy + y * s) for x, y in P])]


def leaf(cx, cy, s=1.0):
    P = [(0, -9), (5, -3), (5, 3), (0, 9), (-5, 3), (-5, -3)]
    return [('p', [(cx + x * s, cy + y * s) for x, y in P])]


def fangs(I, top=9.0, gum=True, blood=True):
    """The bite: a gum with two fangs, the left one longer (phase 1's first icon)."""
    lo = not I.big
    t = 4.5 if lo else top
    I.shape('bone', [('p', [(6.5, t), (16, t), (15.4, 17), (11.6, 26.5), (8, 17.5)])])
    I.shape('bone', [('p', [(17.5, t), (26, t), (24.8, 16), (21.8, 25), (19, 17)])])
    if gum:
        if lo:
            I.cv.bevel(Cv(16, 16).rect(2, 2, 11, 2, 1), IR['gum'], 0, 0, False)
        else:
            I.shape('gum', [('r', 5, 4, 22, 6), ('d', 5, 7, 3), ('d', 27, 7, 3)])
    if blood and I.big:
        I.shape('blood', [('d', 11.8, 28.9, 1.4), ('p', [(11.8, 27.3), (13, 28.7), (10.6, 28.7)])], False)
    return I


def wolf(I, cx=16, cy=16, f=1.15, r='fur', eye='#FFD36B', mouth=False, muzzle=None, line=True, nose=INK):
    """A wolf head from the front, on its own 24 grid mapped round (cx, cy)."""
    R_ = IR[r] if isinstance(r, str) else r

    def T(pts):
        return [(cx + (x - 12) * f, cy + (y - 12) * f) for x, y in pts]

    def X(x): return cx + (x - 12) * f
    def Y(y): return cy + (y - 12) * f
    head = [('p', T([(2, 1), (10, 8), (4, 12)])), ('p', T([(22, 1), (14, 8), (20, 12)])), ('d', X(12), Y(13), 9.4 * f, 8 * f), ('p', T([(8, 17), (16, 17), (15, 23), (9, 23)]))]
    I.shape(R_, head)
    I.paint([('p', T([(8.5, 15), (15.5, 15), (14.5, 22.5), (9.5, 22.5)]))], muzzle or mix(R_.b, WHITE, 0.28), head)
    I.paint([('p', T([(3.4, 3.4), (8, 7.6), (4.8, 10)])), ('p', T([(20.6, 3.4), (16, 7.6), (19.2, 10)]))], R_.o, head)
    k = I.k
    ex0, ex1, ey = round(X(6.3) * k), round(X(14.3) * k), round(Y(10.8) * k)
    ew, eh = max(2, round(3.6 * f * k)), max(1, round(2 * f * k))
    for ex in (ex0, ex1):
        I.tx(ex, ey, INK, ew, eh).tx(ex + (1 if ew > 2 else 0), ey, eye, max(1, ew - 2), eh)
        if I.big:
            I.tx(ex + 1, ey, mix(rgba(eye), WHITE, 0.6))
    nx, ny, nw = round(X(10.4) * k), round(Y(15.8) * k), max(2, round(3.4 * f * k))
    I.tx(nx, ny, nose, nw, max(1, round(1.8 * f * k)))
    if mouth:
        I.paint([('p', T([(9.6, 18.6), (14.4, 18.6), (13.8, 22.2), (10.2, 22.2)]))], '#7A1224', head)
        mx0, mx1, my = round(X(9.9) * k), round(X(13.4) * k), round(Y(18.7) * k)
        th = max(1, round(1.8 * f * k))
        I.tx(mx0, my, '#FFFFF2', 1, th).tx(mx1, my, '#FFFFF2', 1, th)
    elif I.big and line:
        I.tx(round(X(11.3) * k), round(Y(18.6) * k), INK, max(2, round(1.6 * f * k)), 1)
    return I


def eye(I, cx=16, cy=16, s=1.0, iris='teal', slit=False, lid='bone'):
    lens = [('p', [(cx + x * s, cy + y * s) for x, y in ((-12.5, 0), (-7, -5.6), (0, -7.6), (7, -5.6), (12.5, 0), (7, 5.6), (0, 7.6), (-7, 5.6))])]
    I.shape(lid, lens)
    I.shape(iris, [('d', cx, cy, 5.4 * s)], False)
    if slit:
        I.paint([('p', [(cx, cy - 5 * s), (cx + 1.6 * s, cy), (cx, cy + 5 * s), (cx - 1.6 * s, cy)])], INK)
    else:
        I.paint([('d', cx, cy, 2.4 * s)], INK)
    k = I.k
    I.tx(round((cx - 2.4 * s) * k), round((cy - 2.8 * s) * k), WHITE)
    return I


def moon_phase(I, lit, waxing):
    """lit 0 (new) to 1 (full). The lit side is on the right while waxing."""
    disc = [('d', 16, 16, 12)]
    I.shape('moon', disc)
    k, n = I.k, I.n
    dark, dark2 = IR['night'].b, IR['night'].hi
    r = 12 * k
    for y in range(n):
        dy = y + 0.5 - 16 * k
        if abs(dy) > r:
            continue
        half = math.sqrt(r * r - dy * dy)
        term = half * (1 - 2 * lit)  # the terminator: +half at new, 0 at the quarter, -half at full
        for x in range(n):
            dx = x + 0.5 - 16 * k
            if abs(dx) > half:
                continue
            shadow = (dx < term) if waxing else (-dx < term)
            if shadow:
                edge = abs(dx) > half - 1 or abs(dy) > r - 1
                I.cv.d[y * n + x] = dark2 if (edge and (dx < 0 or dy < 0)) else dark
    if lit == 0:
        I.paint([('a', 16, 16, 12, 1.2, 40, 150)], IR['violet'].sh, disc)
    if I.big and lit >= 0.5:
        for cx_, cy_, rr_ in ((19.5, 12, 2.2), (15, 20, 1.6), (21, 20.5, 1.2)) if waxing or lit == 1 else ((12.5, 12, 2.2), (17, 20, 1.6), (11, 20.5, 1.2)):
            I.paint([('d', cx_, cy_, rr_)], IR['moon'].sh, disc)
    return I


def medal(I, r, rows32, rows16, ink='#FFFFF2'):
    """A rank medallion: a disc of the rank's colour with its Greek letter."""
    I.shape(r, [('d', 16, 16, 12.5)])
    if I.big:
        I.paint([('a', 16, 16, 10.2, 1, 0, 360)], IR[r].sh2 or IR[r].sh)
    rows = rows32 if I.big else rows16
    w = max(len(x) for x in rows)
    I.cv.bmp(rows, {'#': ink, 'o': IR[r].o}, (I.n - w) // 2, (I.n - len(rows)) // 2 + (0 if I.big else 0))
    return I


ICONS = {}


def icon(name):
    def reg(fn):
        ICONS[name] = fn
        return fn
    return reg


# ================================ pages ================================
@icon('home')
def _(I):
    I.shape('paper', [('r', 8, 14, 16, 13)])
    I.shape('blood', [('p', [(3.5, 15), (16, 4.5), (28.5, 15), (26, 17), (16, 8.6), (6, 17)])])
    I.shape('gum', [('r', 13.5, 19, 5, 8)], False)
    if I.big:
        I.shape('sky', [('r', 19.5, 17, 3, 3)], False)


@icon('path')
def _(I):
    I.shape('moon', crescent(14.5 + 1.5, 16))
    if I.big:
        I.shape('gold', star4(24, 9, 3.4, 0.9), False)
    else:
        I.bits('gold', ['.#.', '###', '.#.'], 11, 2)


@icon('skills')
def _(I):
    I.shape('steel', [('l', 16, 8, 8, 23, 2.2), ('l', 16, 8, 24, 23, 2.2), ('l', 8, 23, 24, 23, 2.2)], False)
    I.shape('gold', [('d', 16, 8, 4.6)])
    I.shape('blood', [('d', 8, 23, 4.2)])
    I.shape('violet', [('d', 24, 23, 4.2)])


@icon('quests')
def _(I):
    I.shape('paper', [('r', 8, 6, 16, 20)])
    I.shape('paperD', [('d', 16, 6.4, 10.5, 3.2)])
    I.shape('paperD', [('d', 16, 25.6, 10.5, 3.2)])
    if I.big:
        for y, w in ((11, 11), (14.5, 11), (18, 7)):
            I.cv.hl(11, round(y), w, rgba('#6A4A28'))
        I.shape('blood', [('d', 21.4, 21.4, 3.4)], False)
    else:
        I.tx(5, 7, '#6A4A28', 6).tx(5, 9, '#6A4A28', 4).tx(10, 10, '#C01830', 2, 2)


STAR16 = ['....##....', '....##....', '...####...', '##########', '.########.', '..######..', '..######..', '.###..###.', '.#......#.']


@icon('rites')
def _(I):
    I.shape('night', [('d', 16, 16, 13.5)])
    k = I.k
    if not I.big:  # a line pentagram turns to mud at 16: a solid star on the dark seal
        I.cv.bmp(STAR16, {'#': '#C9A6FF'}, 3, 3)
        I.tx(7, 3, WHITE, 2, 1).tx(7, 7, '#FFD36B', 2, 2)
        return
    I.cv.ring(16 * k, 16 * k, 11 * k, 11 * k, max(1, round(1.2 * k)), rgba('#8A5AD6'))
    pts = [(round(16 * k + math.cos(-math.pi / 2 + i * 2 * math.pi / 5) * 9.6 * k - 0.5), round(16 * k + math.sin(-math.pi / 2 + i * 2 * math.pi / 5) * 9.6 * k - 0.5)) for i in range(5)]
    for i in range(5):
        I.cv.line(*pts[i], *pts[(i + 2) % 5], rgba('#C9A6FF'))
    for x, y in pts:
        I.cv.px(x, y, WHITE)
    I.tx(15, 15, '#FFD36B', 2, 2)


@icon('bloodline')
def _(I):
    I.shape('gold', [('r', 15, 12, 2, 5), ('r', 7.5, 16, 17, 2), ('r', 7.5, 16, 2, 4), ('r', 22.5, 16, 2, 4)], False)
    I.shape('blood', drop(16, 8.5, 0.44))
    I.shape('blood', drop(8.5, 23.5, 0.44))
    I.shape('blood', drop(23.5, 23.5, 0.44))


@icon('pack')
def _(I):
    paw(I)
    if I.big:
        for x, y in ((12.4, 8), (19.6, 8), (7, 13), (25, 13)):
            I.dot(x - 1, y - 1, mix(IR['fur'].hi, WHITE, 0.3))


@icon('guide')
def _(I):
    I.shape('gum', [('r', 6, 5, 20, 21)])
    I.shape('paper', [('r', 8, 24, 18, 3)], False)
    I.shape('gold', [('p', [(16, 9), (20, 13), (16, 17), (12, 13)])], False)
    if I.big:
        I.cv.vl(9, 6, 18, rgba('#B02A3A'))


@icon('loadout')
def _(I):
    for j, y in enumerate((6, 17)):
        for i, x in enumerate((4, 12.5, 21)):
            I.shape('gold' if (i, j) == (1, 0) else 'steel', [('r', x, y, 7, 9)])
    if I.big:
        for y in (6, 17):
            for x in (4, 12.5, 21):
                I.paint([('r', x + 2, y + 2.5, 3, 4)], INK)


@icon('settings')
def _(I):
    ops = [('d', 16, 16, 9.2)] + [('d', 16 + math.cos(i * math.pi / 4) * 11.2, 16 + math.sin(i * math.pi / 4) * 11.2, 2.5) for i in range(8)] + [('-d', 16, 16, 4)]
    I.shape('steel', ops)


@icon('admin')
def _(I):
    I.shape('gold', [('p', [(6, 5), (26, 5), (26, 16), (21, 24), (16, 28), (11, 24), (6, 16)])])
    I.shape('blood', [('p', [(10, 9), (22, 9), (22, 15.5), (18.5, 21), (16, 23), (13.5, 21), (10, 15.5)])], False)


# ================================ controls and status ================================
@icon('check')
def _(I):
    I.shape('green', [('l', 6, 16, 13.5, 24, 5), ('l', 12, 24.5, 26, 8, 5)])


@icon('lock')
def _(I):
    I.shape('steel', [('a', 16, 13.5, 7.5, 3.4, 270, 90), ('r', 8.5, 12.5, 3.4, 4), ('r', 20.1, 12.5, 3.4, 4)])
    I.shape('gold', [('r', 6.5, 15, 19, 13)])
    I.paint([('d', 16, 20, 2.2), ('r', 15, 20.5, 2, 4.5)], INK)


@icon('crown')
def _(I):
    I.shape('gold', [('p', [(6, 21), (5, 9), (11.5, 15), (16, 6), (20.5, 15), (27, 9), (26, 21)])])
    I.shape('gold', [('r', 6, 21, 20, 5)])
    for x, y in ((5, 8.4), (16, 5.2), (27, 8.4)):
        I.shape('gold', [('d', x, y, 1.9)], False)
    I.shape('blood', [('d', 16, 23.4, 1.9)], False)


@icon('dot')
def _(I):
    I.shape('bone', [('d', 16, 16, 5.5)])


@icon('close')
def _(I):
    I.shape('blood', [('l', 8, 8, 24, 24, 5), ('l', 24, 8, 8, 24, 5)])


@icon('theme')
def _(I):
    disc = [('d', 16, 16, 12)]
    I.shape('paper', disc)
    I.paint([('p', [(16, 16), (16, 2), (30, 2), (30, 24)])], '#C01830', disc)
    I.paint([('p', [(16, 16), (30, 24), (30, 30), (2, 30), (2, 24)])], '#3FD0D6', disc)
    if I.big:
        I.paint([('a', 16, 16, 11.4, 1, 300, 30)], '#FFF6DC', disc)
        I.paint([('d', 16, 16, 3.2)], IR['paper'].o)
        I.paint([('d', 16, 16, 2.2)], '#FFF6DC')
    else:
        I.tx(7, 7, IR['paper'].o, 2, 2)


@icon('launch')
def _(I):
    I.shape('steel', [('r', 5, 10, 17, 17), ('-r', 8.4, 13.4, 10.2, 10.2), ('-r', 13, 7, 12, 11)])
    I.shape('teal', [('l', 14, 18, 24, 8, 3.6), ('p', [(17, 5), (27, 5), (27, 15)])])


@icon('question')
def _(I):
    I.shape('bone', [('a', 16, 11.5, 7.5, 4.2, 270, 140), ('l', 19.2, 15.4, 16, 20, 4.2), ('d', 16, 25.5, 2.6)])


@icon('info')
def _(I):
    I.shape('sky', [('d', 16, 7.2, 2.8), ('r', 13.5, 12.5, 5, 14), ('r', 10.5, 12.5, 4, 3), ('r', 10.5, 24, 11, 3)])


@icon('plus')
def _(I):
    I.shape('bone', [('r', 13.5, 6, 5, 20), ('r', 6, 13.5, 20, 5)])


@icon('arrow_left')
def _(I):
    I.shape('bone', [('p', [(5, 16), (15, 6), (15, 12.5), (27, 12.5), (27, 19.5), (15, 19.5), (15, 26)])])


@icon('arrow_right')
def _(I):
    I.shape('bone', [('p', [(27, 16), (17, 6), (17, 12.5), (5, 12.5), (5, 19.5), (17, 19.5), (17, 26)])])


@icon('bell')
def _(I):
    I.shape('gold', [('d', 16, 26.5, 2.4)], False)
    I.shape('gold', [('d', 16, 13, 7.6), ('p', [(8.4, 13), (23.6, 13), (26, 22), (6, 22)]), ('r', 5, 21.5, 22, 3.4), ('d', 16, 5, 1.9)])


@icon('key')
def _(I):
    I.shape('gold', [('d', 10, 16, 6.4), ('-d', 10, 16, 2.6), ('r', 15, 14.4, 13.5, 3.2), ('r', 21.5, 17, 2.6, 4.5), ('r', 25.9, 17, 2.6, 3.4)])


@icon('unknown')
def _(I):
    I.shape('violet', [('l', 11.5, 5, 11.5, 27, 3.6), ('l', 11.5, 6.5, 21, 11.5, 3.2), ('l', 21, 11.5, 12, 16.5, 3.2), ('l', 13, 16.5, 22, 27, 3.4)])


# ================================ sky ================================
for _name, _lit, _wax in (('moon_new', 0.0, True), ('moon_waxing_crescent', 0.25, True), ('moon_first_quarter', 0.5, True), ('moon_waxing_gibbous', 0.75, True),
                          ('moon_full', 1.0, True), ('moon_waning_gibbous', 0.75, False), ('moon_last_quarter', 0.5, False), ('moon_waning_crescent', 0.25, False)):
    ICONS[_name] = (lambda lit, wax: lambda I: moon_phase(I, lit, wax))(_lit, _wax)


@icon('sun')
def _(I):
    for i in range(8):
        a = i * math.pi / 4
        c, s = math.cos(a), math.sin(a)
        I.shape('gold', [('l', 16 + c * 9.5, 16 + s * 9.5, 16 + c * 13.5, 16 + s * 13.5, 2.6)], False)
    I.shape('gold', [('d', 16, 16, 7.6)])
    if I.big:
        I.paint([('d', 14, 14, 2.4)], IR['gold'].hi, [('d', 16, 16, 7.6)])


# ================================ motifs ================================
@icon('drop')
def _(I):
    I.shape('blood', drop())
    if I.big:
        I.paint([('d', 12.2, 21, 1.5, 2.8)], '#FFC8C2').paint([('d', 13, 15.5, 0.8, 1.2)], '#FF9E96')
    else:
        I.tx(6, 10, '#FFC8C2')


@icon('shield')
def _(I):
    I.shape('steel', shield())
    I.shape('sky', shield(16, 15.5, 0.56), False)


@icon('fang')
def _(I):
    I.shape('bone', [('p', [(9, 4), (23, 4), (21.5, 14), (16.5, 28.5), (10.5, 15)])])
    if I.big:
        I.paint([('l', 13.5, 7, 14.5, 17, 1.4)], IR['bone'].sh)
    I.shape('blood', [('d', 21.5, 25.5, 2.4), ('p', [(21.5, 20.5), (23.7, 25), (19.3, 25)])], False)


@icon('vial')
def _(I):
    body = [('d', 16, 19.5, 8.6), ('r', 13, 6, 6, 8)]
    I.shape('ice', body)
    I.paint([('r', 5, 18, 22, 12)], '#C01830', [('d', 16, 19.5, 7.4)])
    I.paint([('r', 5, 18, 22, 1.4)], '#FF6A60', [('d', 16, 19.5, 7.4)])
    I.shape('wood', [('r', 12, 3.5, 8, 3.4)], False)
    if I.big:
        I.paint([('d', 12.4, 22.5, 1.2, 2)], '#FF8F85')


@icon('claw')
def _(I):
    for x in (11, 18, 25):
        I.shape('blood', [('p', [(x - 1, 4.5), (x + 3.2, 4.5), (x - 4.6, 27.5), (x - 6.4, 27.5)])])


@icon('heart')
def _(I):
    I.shape('blood', heart(16, 15.5, 1.12))
    if I.big:
        I.paint([('d', 10.6, 10.8, 1.8, 1.4)], '#FFC8C2')
    else:
        I.tx(4, 5, '#FFC8C2')


@icon('boot')
def _(I):
    I.shape('wood', boot(15, 16, 1.12))
    I.paint([('r', 6, 25.4, 22, 3)], IR['wood'].o, boot(15, 16, 1.12))
    I.shape('bone', [('r', 7.4, 3.4, 10.6, 4.6)], False)
    if I.big:
        I.paint([('l', 17.5, 16.5, 22, 18.5, 1.2), ('l', 17.5, 13.5, 20.5, 15, 1.2)], IR['wood'].o, boot(15, 16, 1.12))


@icon('eye')
def _(I):
    eye(I)


@icon('voice')
def _(I):
    I.shape('bone', [('p', [(4, 13), (9, 13), (14, 8), (14, 24), (9, 19), (4, 19)])])
    I.shape('gold', waves(10, 16, (10.5, 16.5)))


@icon('spark')
def _(I):
    I.shape('gold', star4(16, 16, 13, 2.8))
    if I.big:
        I.shape('gold', star4(24.5, 7.5, 3.6, 1), False)
        I.paint([('d', 16, 16, 2)], WHITE)
    else:
        I.tx(7, 7, WHITE, 2, 2)


@icon('bolt')
def _(I):
    I.shape('gold', bolt())


# ================================ HUD actions ================================
@icon('bite')
def _(I):
    fangs(I)


@icon('alpha_roar')
def _(I):
    wolf(I, 12.5, 17, 0.92, 'fur', '#FF4A3A', mouth=True)
    I.shape('gold', waves(13, 16, (13, 17.5), 2.2, 52, 128))


@icon('thirst')
def _(I):
    cup = [('p', [(7, 6), (25, 6), (22.5, 15.5), (18, 18.5), (18, 23), (23, 25.6), (23, 28), (9, 28), (9, 25.6), (14, 23), (14, 18.5), (9.5, 15.5)])]
    I.shape('gold', cup)
    I.paint([('r', 6, 6, 20, 3.4)], '#C01830', cup)
    I.paint([('r', 6, 6, 20, 1.2)], '#FF6A60', cup)
    I.shape('blood', [('d', 24.5, 12.6, 1.7), ('p', [(24.5, 9), (26, 12.4), (23, 12.4)])], False)


@icon('moonblood_surge')
def _(I):
    I.shape('redmoon', [('d', 13.5, 18, 10.5)])
    if I.big:
        for x, y, r_ in ((10, 15, 2), (15.5, 22, 1.6), (9, 22, 1.1)):
            I.paint([('d', x, y, r_)], IR['redmoon'].sh, [('d', 13.5, 18, 9.4)])
    I.shape('gold', bolt(22, 12, 0.72))


@icon('wolf_spirits')
def _(I):
    wolf(I, 16, 14, 1.0, 'ice', '#17707A', muzzle='#FFFFFF', line=False, nose='#58A0B2')
    I.shape('ice', star4(5.5, 24.5, 3.6, 0.9), False)
    I.shape('ice', star4(26.5, 24.5, 3.6, 0.9), False)
    I.shape('ice', star4(16, 28, 2.6, 0.7), False)


@icon('leap')
def _(I):  # a paw print leaving the ground: the arrow head above it
    if I.big:
        I.shape('gold', [('l', 8.5, 10.5, 16, 3.5, 3.6), ('l', 16, 3.5, 23.5, 10.5, 3.6)])
        paw(I, 16, 19.5, 0.8)
    else:
        I.bits('gold', ['...##...', '..####..', '.##..##.'], 4, 1)
        I.bits('fur', ['...##..##...', '...##..##...', '##........##', '##..####..##', '...######...', '..########..', '..########..', '...##..##...'], 2, 6)


# ================================ skills ================================
@icon('s-hum-1')
def _(I):  # Survivor
    I.shape('blood', heart(16, 15.5, 1.12))
    I.shape('paper', [('l', 8, 21, 24, 9, 5)], False)
    if I.big:
        I.paint([('d', 16, 15, 1), ('d', 13, 17.2, 0.9), ('d', 19, 12.8, 0.9)], IR['paper'].sh)


@icon('s-hum-2')
def _(I):  # Vervain Blood
    I.shape('blood', drop(13.5, 16.5, 0.9))
    I.shape('green', [('l', 23, 27, 23, 9, 1.8)], False)
    for y, sx in ((20, -1), (15, 1), (10.5, -1)):
        I.shape('violet', [('d', 23 + sx * 2.6, y, 2.4)], False)


@icon('s-hum-3')
def _(I):  # Swift Feet
    I.shape('wood', boot(18, 16, 1.0))
    I.paint([('r', 9, 24.4, 22, 3)], IR['wood'].o, boot(18, 16, 1.0))
    for y, w in ((10, 6), (15.5, 8), (21, 6)):
        I.shape('gold', [('r', 3, y, w, 1.8)], False)


@icon('s-hun-1')
def _(I):  # Steady Aim
    I.shape('steel', [('a', 16, 16, 11, 2.6, 0, 360), ('r', 15, 3, 2, 8), ('r', 15, 21, 2, 8), ('r', 3, 15, 8, 2), ('r', 21, 15, 8, 2)])
    I.shape('blood', [('d', 16, 16, 2.6)], False)


@icon('s-hun-2')
def _(I):  # Hunter's Eye
    eye(I, iris='wood')
    I.paint([('r', 15.3, 9, 1.4, 14), ('r', 9, 15.3, 14, 1.4)], '#FFD36B', [('d', 16, 16, 5.4)])


@icon('s-hun-3')
def _(I):  # Patient Hunter
    glass = [('p', [(8, 6), (24, 6), (17.5, 16), (24, 26), (8, 26), (14.5, 16)])]
    I.shape('ice', glass)
    I.paint([('p', [(11, 9.5), (21, 9.5), (16, 15.5)])], '#E8B33A', glass)
    I.paint([('p', [(10, 25.5), (22, 25.5), (16, 20.5)])], '#E8B33A', glass)
    I.shape('wood', [('r', 6, 3.5, 20, 3.2)])
    I.shape('wood', [('r', 6, 25.4, 20, 3.2)])


@icon('s-hun-4')
def _(I):  # Danger Sense
    I.shape('gold', [('p', [(16, 3.5), (29, 27), (3, 27)])])
    I.paint([('r', 14.6, 11, 2.8, 9), ('d', 16, 23.4, 1.7)], INK)


@icon('s-hun-5')
def _(I):  # Mark Mastery
    I.shape('paper', [('d', 14, 18, 11)])
    I.paint([('a', 14, 18, 8.4, 2.6, 0, 360)], '#C01830')
    I.paint([('d', 14, 18, 2.6)], '#C01830')
    I.shape('wood', [('l', 14.5, 17.5, 26, 6, 2.2)], False)
    I.shape('steel', [('p', [(24, 3.5), (29, 3), (28.5, 8), (26.5, 6.5)])], False)


@icon('s-hyb-1')
def _(I):  # Steady Thirst
    d = drop()
    I.shape('bone', d)
    I.paint([('r', 2, 17, 28, 16)], '#C01830', d)
    I.paint([('r', 2, 17, 28, 1.4)], '#FF6A60', d)
    I.paint([('r', 2, 26, 28, 6)], '#7A0E1E', d)


@icon('s-hyb-2')
def _(I):  # Venom Fang
    I.shape('bone', [('p', [(7, 4), (21, 4), (19.5, 14), (14.5, 28.5), (8.5, 15)])])
    I.shape('green', drop(23, 22.5, 0.42), False)
    if I.big:
        I.paint([('l', 11.5, 7, 12.5, 17, 1.4)], IR['bone'].sh)


@icon('s-ori-1')
def _(I):  # Old Blood
    I.shape('blood', drop(16, 19, 0.78))
    I.shape('gold', [('p', [(9, 11), (8, 3.5), (12.5, 7.5), (16, 2.8), (19.5, 7.5), (24, 3.5), (23, 11)])])


@icon('s-ori-2')
def _(I):  # Bloodmaker's Yield
    body = [('d', 14, 21, 7.6), ('r', 11.2, 9, 5.6, 7)]
    I.shape('ice', body)
    I.paint([('r', 4, 17.5, 20, 13)], '#C01830', [('d', 14, 21, 6.4)])
    I.paint([('r', 4, 17.5, 20, 1.3)], '#FF6A60', [('d', 14, 21, 6.4)])
    I.shape('wood', [('r', 10.4, 6.6, 7.2, 3)], False)
    I.shape('gold', [('r', 23.4, 5, 2.4, 9), ('r', 20.1, 8.3, 9, 2.4)], False)


@icon('s-ori-3')
def _(I):  # Sire's Call
    I.shape('bone', [('p', [(3.5, 14), (8, 14), (15, 7.5), (15, 24.5), (8, 18), (3.5, 18)])])
    I.shape('gold', [('r', 13.6, 6.5, 2.4, 19)], False)
    I.shape('blood', waves(11, 16, (11, 17)))


@icon('s-ori-4')
def _(I):  # Primordial Mind
    I.shape('violet', [('a', 16, 16, 13, 2.4, 0, 360)])
    eye(I, 16, 16, 0.8, 'gold', slit=True)


def spiral(cx, cy, s=1.0, t=2.4):
    return [('a', cx, cy, 11 * s, t * s, 300, 200), ('a', cx - 2.2 * s, cy, 6.6 * s, t * s, 90, 320), ('a', cx, cy + 0.6 * s, 3.4 * s, t * s, 250, 110)]


@icon('s-sip-1')
def _(I):  # Deep Draw
    I.shape('violet', spiral(16, 16, 1.12))


@icon('s-sip-2')
def _(I):  # Quick Draw
    I.shape('violet', spiral(13, 17, 0.92))
    I.shape('gold', bolt(23.5, 11.5, 0.66))


@icon('s-sip-3')
def _(I):  # Life Siphon
    if I.big:
        I.shape('blood', heart(13.5, 14, 0.94))
        I.shape('violet', spiral(22.5, 22, 0.56, 3.2), False)
    else:  # a whole heart, and one violet coil drawing from it
        I.bits('blood', ['.###.###.', '#########', '#########', '#########', '.#######.', '..#####..', '...###...', '....#....'], 1, 2)
        I.tx(2, 3, '#FFC8C2')
        I.bits('violet', ['.###.', '#...#', '#.#.#', '#.###', '.#...'], 9, 9)


@icon('s-sip-4')
def _(I):  # Spent Wisely
    for cx, cy in ((11, 23), (21, 23), (16, 15.5)):
        I.shape('gold', [('d', cx, cy + 2, 6.6, 3.6)])
        I.shape('gold', [('d', cx, cy, 6.6, 3.6)])
    I.shape('gold', [('d', 16, 9.5, 6.6, 3.6)])
    if I.big:
        I.paint([('d', 16, 9.5, 3.4, 1.4)], IR['gold'].sh)


@icon('s-sip-5')
def _(I):  # Void Hunger
    I.shape('violet', [('d', 16, 16, 13)])
    I.shape('night', [('d', 16, 16, 9.4)], False)
    I.paint([('d', 16, 16, 5.2)], '#08060F')
    for i in range(6):
        a = i * math.pi / 3 + 0.3
        I.paint(star4(16 + math.cos(a) * 7.6, 16 + math.sin(a) * 7.6, 2.2, 0.6), '#C9A6FF')


@icon('s-vam-1')
def _(I):  # Keen Fangs
    I.shape('bone', [('p', [(5, 5), (14.5, 5), (13.5, 15), (10, 27.5), (6.2, 15.5)])])
    I.shape('bone', [('p', [(17.5, 5), (27, 5), (25.8, 15.5), (22, 27.5), (18.5, 15)])])
    I.shape('gold', star4(16, 22.5, 4.4, 1.1), False)


@icon('s-vam-2')
def _(I):  # Night Stride
    I.shape('bat', boot(13.5, 17.5, 0.98))
    I.paint([('r', 5, 25.6, 22, 3)], IR['bat'].o, boot(13.5, 17.5, 0.98))
    I.shape('moon', crescent(23.5, 8.5, 0.44))


@icon('s-vam-3')
def _(I):  # Silver Tongue
    I.shape('steel', [('p', [(5, 6), (27, 6), (27, 20), (16, 20), (10, 27), (10.5, 20), (5, 20)])])
    if I.big:
        I.paint([('r', 9, 10, 14, 1.6), ('r', 9, 14, 10, 1.6)], IR['steel'].o)
    else:
        I.tx(4, 5, IR['steel'].o, 7).tx(4, 7, IR['steel'].o, 5)


@icon('s-vam-4')
def _(I):  # Hardened
    I.shape('steel', shield())
    I.paint([('r', 15, 5.5, 2, 22), ('r', 6.5, 12, 19, 2)], IR['steel'].sh2, shield(16, 16, 0.86))
    for x, y in ((10, 8.5), (22, 8.5)):
        I.dot(x - 0.5, y - 0.5, IR['steel'].hi)


@icon('s-vam-5')
def _(I):  # Blood Mist
    cloud = [('d', 9.5, 15.5, 5.6), ('d', 16.5, 11, 7.2), ('d', 23.5, 15.5, 5.4), ('r', 9, 15, 15, 6)]
    I.shape('redmoon', cloud)
    if I.big:
        I.paint([('a', 16.5, 12, 4.4, 1.2, 200, 330), ('a', 10, 16.5, 3, 1.2, 200, 320), ('a', 23, 16.5, 3, 1.2, 40, 150)], IR['redmoon'].sh, cloud)
    for x, y, sc in ((9, 23.8, 0.26), (16, 25.4, 0.3), (23, 23.8, 0.26)):
        I.shape('blood', drop(x, y, sc), False)


@icon('s-vam-6')
def _(I):  # Predator's Thirst
    for x in (9, 15.5, 22):
        I.shape('bone', [('p', [(x - 1, 4), (x + 3, 4), (x - 3.4, 22), (x - 5.2, 22)])])
    I.shape('blood', drop(22.5, 22.5, 0.44), False)


@icon('s-vam-7')
def _(I):  # Elder's Skin
    for i in range(5):
        a = math.pi + i * math.pi / 4
        I.shape('gold', [('l', 16 + math.cos(a) * 8.2, 14 + math.sin(a) * 8.2, 16 + math.cos(a) * 11.4, 14 + math.sin(a) * 11.4, 2.6)], False)
    I.shape('gold', [('d', 16, 14, 7.4)])
    I.shape('steel', shield(16, 20.5, 0.7))
    I.shape('sky', shield(16, 20.1, 0.38), False)


@icon('s-wit-1')
def _(I):  # Steady Circle
    I.shape('paper', [('a', 16, 16, 12.5, 2.6, 0, 360)])
    I.shape('violet', [('d', 16, 16, 3.4)], False)
    for a in (0, 90, 180, 270):
        x, y = 16 + math.sin(math.radians(a)) * 7.4, 16 - math.cos(math.radians(a)) * 7.4
        I.paint([('d', x, y, 1.1)], IR['paper'].hi)


@icon('s-wit-2')
def _(I):  # Wide Circle
    I.shape('paper', [('a', 16, 16, 13.2, 2.2, 0, 360)])
    I.shape('violet', [('a', 16, 16, 8, 2.2, 0, 360)], False)
    I.paint([('d', 16, 16, 1.6)], IR['paper'].hi)


@icon('s-wit-3')
def _(I):  # Ashwarden
    I.shape('fur', shield())
    I.shape('green', leaf(16, 15.5, 0.86), False)
    I.paint([('r', 15.5, 8, 1.2, 15)], IR['green'].o, leaf(16, 15.5, 0.86))


@icon('s-wit-4')
def _(I):  # Emissary's Bond
    I.shape('gold', [('a', 11.5, 16, 8, 2.8, 0, 360)])
    I.shape('violet', [('a', 20.5, 16, 8, 2.8, 0, 360)])
    I.shape('gold', [('a', 11.5, 16, 8, 2.8, 20, 110)], False)


@icon('s-wit-5')
def _(I):  # Magic Sense
    eye(I, 16, 18, 0.95, 'violet')
    I.shape('gold', star4(16, 6, 4.2, 1), False)


@icon('s-wit-6')
def _(I):  # Ancestral Voice
    I.shape('paper', [('r', 8.5, 17, 7, 11)])
    I.shape('fire', flame(12, 9.5, 0.62), False)
    I.shape('violet', waves(13, 14, (9.5, 15.5), 2.4, 40, 120))


@icon('s-wit-7')
def _(I):  # Coven's Ward
    I.shape('violet', shield())
    I.shape('gold', star5(16, 14.8, 6.8, 2.8), False)


@icon('s-wol-1')
def _(I):  # Thick Hide
    I.shape('wood', shield())
    for x in (11.5, 16, 20.5):
        I.paint([('p', [(x, 8), (x + 2, 8), (x - 1.6, 21), (x - 2.6, 21)])], IR['wood'].o, shield(16, 16, 0.86))


@icon('s-wol-2')
def _(I):  # Moon Calm
    I.shape('moon', [('d', 16, 12.5, 9)])
    if I.big:
        I.paint([('d', 13.5, 10.5, 1.6), ('d', 18.5, 14.5, 1.2)], IR['moon'].sh)
    for y, x, w in ((23.2, 4, 24), (26.6, 8, 16)):
        I.shape('sky', [('r', x, y, w, 1.8)], False)


@icon('s-wol-3')
def _(I):  # Godwolf Sense
    eye(I, iris='gold', slit=True, lid='fur')


@icon('s-wol-4')
def _(I):  # Wolfsbane Tolerance
    I.shape('green', [('l', 16, 28, 16, 14, 2.2), ('p', [(16, 23), (22.5, 19), (21, 24.5)]), ('p', [(16, 25), (9.5, 21), (11, 26.5)])], False)
    for a in range(5):
        an = math.radians(a * 72)
        I.shape('violet', [('d', 16 + math.sin(an) * 5.2, 10.5 - math.cos(an) * 5.2, 3.5)], False)
    I.shape('gold', [('d', 16, 10.5, 2.3)], False)


@icon('s-wol-5')
def _(I):  # Pack Heart
    I.shape('blood', heart(16, 15.5, 1.12))
    k = 0.48
    for t in paw_toes(16, 15, k):
        I.paint(t, '#FFF6DC')
    I.paint(paw_pad(16, 15, k), '#FFF6DC')


@icon('s-wol-6')
def _(I):  # Roar's Echo
    I.shape('gold', waves(6, 16, (6.5, 12), 2.6))
    I.shape('bone', waves(6, 16, (17.5, 22.5), 2.4, 54, 126))


@icon('s-wol-7')
def _(I):  # Moon Heart
    I.shape('moon', [('d', 16, 16, 12.5)])
    if I.big:
        I.paint([('d', 9.5, 10, 1.6), ('d', 23.5, 21, 1.3)], IR['moon'].sh)
    I.shape('blood', heart(16, 16.5, 0.66), False)


@icon('s-wol-8')
def _(I):  # Primeval Roar
    wolf(I, 16, 16, 1.15, 'tan', '#D6A014', mouth=True)


@icon('s-wol-11')
def _(I):  # Demon Wolf Roar
    I.shape('blood', [('a', 16, 17, 14.5, 2.2, 215, 262), ('a', 16, 17, 14.5, 2.2, 98, 145)], False)
    wolf(I, 16, 15.5, 1.0, 'demon', '#FF3A2A', mouth=True)


# ================================ plan 18: the Augustine skills and the skills that are HUD actions ================================
@icon('s-aug-1')
def _(I):  # Garlic Ward: the bulb, and the shield that keeps it off
    cx = 13 if I.big else 12
    bulb = [('d', cx, 17.5, 9, 8), ('p', [(cx, 3.5), (cx + 5.5, 12.5), (cx - 5.5, 12.5)])]
    I.shape('bone', bulb)
    if I.big:
        I.paint([('l', 11, 9, 9, 25, 1.3), ('l', 15, 9, 17, 25, 1.3)], IR['bone'].sh, bulb)
        I.paint([('r', 11, 25.6, 4, 1.4)], IR['wood'].b)
        I.shape('steel', shield(23, 21.5, 0.56))
        I.shape('sky', shield(23, 21.2, 0.3), False)
    else:
        I.tx(4, 6, IR['bone'].sh, 1, 6).tx(7, 6, IR['bone'].sh, 1, 6)
        I.bits('steel', ['######', '######', '######', '######', '.####.', '..##..'], 9, 8)
        I.tx(11, 10, IR['sky'].b, 2, 2).tx(11, 10, IR['sky'].hi)


@icon('s-aug-2')
def _(I):  # Blessed Skin: holy water that no longer burns, under a halo
    if I.big:
        I.shape('gold', [('d', 16, 6.5, 8, 3.2), ('-d', 16, 6.5, 5.2, 1.4)], False)
        I.shape('sky', drop(16, 18.5, 0.78))
        I.paint([('r', 15, 15, 2, 10), ('r', 12, 18, 8, 2)], '#FFF6DC')
    else:
        I.bits('gold', ['.####.', '#....#'], 5, 1)
        I.shape('sky', drop(16, 19.5, 0.72))
        I.tx(7, 8, '#FFF6DC', 2, 5).tx(6, 9, '#FFF6DC', 4, 1)


RIP16 = ['....ooo..ooo....', '...oHHHooHHHo...', '..oHpHrrHrrrdo..', '..oHrrrrrrrrdo..', '..oHrrrrrrrrdo..', '..koHrrrrrrdok..', '.kWWorWrrWroWWk.', '.kWWkWWrrWWkWWk.',
         '.kStkWWrrWWkStk.', '.kStkStrrStkStk.', '.kStkStddStkStk.', '.kStkStkkStkStk.', '.kSskSskkSskstk.', '..kssssssssstk..', '...kttttttttk...', '....kkkkkkkk....']


def rip_heart(I):  # Rip Heart: the heart, in the clawed hand that took it
    if I.big:
        I.shape('blood', heart(16, 12, 0.96))
        I.paint([('d', 10.8, 7.4, 1.8, 1.4)], '#FFC8C2')
        I.shape('skin', [('p', [(5, 24), (3.5, 19), (4.5, 15.5), (7.5, 15.5), (8.5, 24)]), ('r', 10.5, 17.5, 3.5, 7.5), ('r', 18, 17.5, 3.5, 7.5),
                         ('p', [(23.5, 24), (24.5, 15.5), (27.5, 15.5), (28.5, 19), (27, 24)]),
                         ('p', [(5, 23), (27, 23), (25.5, 27), (21.5, 28.5), (21.5, 30.5), (10.5, 30.5), (10.5, 28.5), (6.5, 27)])])
        for tip in ([(4.3, 16.5), (7.7, 16.5), (7.4, 11)], [(10.5, 18.5), (14, 18.5), (12.8, 13)], [(18, 18.5), (21.5, 18.5), (19.2, 13)], [(24.3, 16.5), (27.7, 16.5), (24.6, 11)]):
            I.shape('bone', [('p', tip)], False)
        I.paint([('r', 15.2, 22, 1.6, 4.5), ('d', 16, 27, 1.5)], '#C01830')
    else:  # typed texel by texel: at 16 an outline round each finger would leave no heart
        B, S = IR['blood'], IR['skin']
        I.cv.bmp(RIP16, {'o': B.o, 'H': B.hi, 'r': B.b, 'd': B.sh, 'p': '#FFC8C2', 'k': S.o, 'S': S.hi, 's': S.b, 't': S.sh, 'W': '#FFFFF2'})


def neck_snap(I):  # Neck Snap: a head knocked off the line of its neck, and the crack of it
    th, bx, by = math.radians(45), 14, 20  # the angle of the head, and where the neck breaks
    ux, uy, vx, vy = math.sin(th), -math.cos(th), math.cos(th), math.sin(th)  # u: to the crown of the head, v: across its face
    cx, cy = bx + ux * 7.5, by + uy * 7.5
    I.shape('skin', [('r', 11.5, 17, 5, 6.5)])
    I.shape('steel', [('d', 14, 30.5, 10.5, 8.5), ('-r', 0, 29.5, 32, 4)])
    I.shape('skin', [('d', cx, cy, 6.5)])
    a, b = (cx + ux * 1.4 - vx * 12, cy + uy * 1.4 - vy * 12), (cx + ux * 1.4 + vx * 12, cy + uy * 1.4 + vy * 12)
    I.paint([('p', [a, b, (b[0] + ux * 12, b[1] + uy * 12), (a[0] + ux * 12, a[1] + uy * 12)])], '#6A4426', [('d', cx, cy, 5.5)])
    for s in (-2.4, 2.4):  # the eyes: shut to crosses, a dot each at 16
        ex, ey = int((cx + vx * s - ux * 1.7) * I.k), int((cy + vy * s - uy * 1.7) * I.k)
        for dx, dy in ((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)) if I.big else ((0, 0),):
            I.tx(ex + dx, ey + dy, INK)
    if I.big:
        I.paint([('p', [(11.5, 23), (16.5, 23), (14, 26.5)])], IR['skin'].sh)
        for x0, y0, x1, y1 in ((8.5, 5, 10.5, 11), (4.5, 9, 8.5, 13.5), (2.5, 16.5, 8, 17.5)):
            I.shape('gold', [('l', x0, y0, x1, y1, 1.8)], False)
    else:
        I.bits('gold', ['.#.', '###', '.#.'], 2, 4)


@icon('bottle_blood')
def _(I):  # Bottle my blood: an open bottle, half full, and the drop that fills it
    if I.big:
        body = [('r', 11.5, 13, 9, 2.5), ('r', 13, 15, 6, 3.5), ('p', [(12.5, 18), (19.5, 18), (24, 21), (24, 28), (22, 30), (10, 30), (8, 28), (8, 21)])]
        inner = [('p', [(13, 19.2), (19, 19.2), (22.7, 21.6), (22.7, 27.6), (21.5, 28.8), (10.5, 28.8), (9.3, 27.6), (9.3, 21.6)])]
        I.shape('ice', body)
        I.paint([('r', 5, 23.5, 22, 10)], '#C01830', inner)
        I.paint([('r', 5, 23.5, 22, 1.4)], '#FF6A60', inner)
        I.paint([('r', 5, 27, 22, 3)], '#7A0E1E', inner)
        I.shape('blood', drop(16, 5.6, 0.25), False)
    else:
        I.bits('ice', ['..######..', '...####...', '..######..', '.########.', '##########', '##########', '##########', '.########.'], 3, 7)
        I.tx(4, 12, '#FF6A60', 8, 1).tx(4, 13, '#C01830', 8, 1).tx(5, 14, '#7A0E1E', 6, 1)
        I.bits('blood', ['#', '#'], 8, 2)
        I.tx(8, 2, '#FF6A60').tx(8, 3, '#C01830')


def hybrid_leap(I):  # Hybrid Leap: the wolf-man thrown at its prey
    if I.big:
        I.shape('fur', [('d', 21.5, 9, 4.3), ('p', [(23, 6), (30, 9.2), (30, 11.2), (25, 12.2)]), ('p', [(24, 11.5), (28.5, 13.6), (27.5, 15), (22.5, 13.5)]),
                        ('p', [(18.2, 7.5), (17.4, 2.2), (22, 5.2)]), ('l', 19, 14, 10.5, 21, 7.5), ('d', 18.5, 14.5, 4.6),
                        ('l', 19.5, 16, 27.5, 19.5, 3), ('l', 16.5, 18, 23, 24.5, 3),
                        ('l', 10.5, 21, 4.5, 25, 4), ('l', 4.5, 25, 3.5, 28, 2.8), ('l', 11.5, 22, 12, 27.5, 3.8), ('l', 12, 27.5, 8.5, 29.3, 2.8)])
        I.shape('bone', [('p', [(27, 18), (30.4, 19.6), (27.6, 21.2)])], False)
        I.shape('bone', [('p', [(22.3, 23.2), (25.6, 26.2), (22.6, 26.4)])], False)
        I.tx(23, 8, '#FF4A3A', 2, 1)
        for y, x, w in ((3.5, 4, 9), (7.5, 2, 10), (11.5, 5, 6)):
            I.shape('gold', [('r', x, y, w, 1.8)], False)
    else:
        I.bits('fur', ['..........#.....', '.........###....', '........######..', '........#######.', '.......#####....', '.....#######....', '...##########...',
                       '..#######.####..', '.#######..##....', '.#####....###...', '.###.##.........', '.##...##........', '......#.........'], 0, 1)
        I.tx(12, 3, '#FF4A3A').tx(13, 8, '#FFFFF2').tx(12, 10, '#FFFFF2')
        I.bits('gold', ['####'], 2, 2)
        I.bits('gold', ['####'], 1, 4)


ICONS['s-vam-8'] = ICONS['rip_heart'] = rip_heart  # one picture for the skill and for its HUD action
ICONS['s-vam-9'] = ICONS['neck_snap'] = neck_snap
ICONS['s-hyb-3'] = ICONS['hybrid_leap'] = hybrid_leap


# ================================ bloodlines ================================
@icon('species_human')
def _(I):
    I.shape('sky', [('d', 16, 27, 10.5, 8), ('-r', 0, 28, 32, 6)])
    I.shape('skin', [('d', 16, 11.5, 6.4)])
    I.paint([('d', 16, 9.2, 6.6, 4.4), ('-r', 0, 10.5, 32, 10)], '#6A4426', [('d', 16, 11.5, 6.4)])


@icon('species_hunter')
def _(I):
    I.shape('wood', [('a', 11, 16, 13, 2.8, 20, 160)])
    I.shape('bone', [('l', 12, 5.4, 12, 26.6, 1.2)], False)
    I.shape('steel', [('l', 6, 16, 26, 16, 2), ('p', [(24.5, 11.5), (29.5, 16), (24.5, 20.5)])], False)


@icon('species_vampire')
def _(I):
    wing = [(16, 13), (19, 10.5), (24, 7.5), (29.5, 5.5), (28, 11), (29, 17.5), (25, 15.5), (23, 20.5), (19.5, 17.5), (16, 24)]
    I.shape('bat', [('p', wing), ('p', [(32 - x, y) for x, y in wing]), ('p', [(12.6, 5.5), (14.6, 10), (17.4, 10), (19.4, 5.5), (19.4, 15), (12.6, 15)])])
    k = I.k
    I.tx(round(14 * k), round(11.6 * k), '#FF3A2A').tx(round(17.6 * k), round(11.6 * k), '#FF3A2A')
    if I.big:
        I.tx(15, 15, '#FFFFF2', 1, 2).tx(17, 15, '#FFFFF2', 1, 2)


@icon('species_werewolf')
def _(I):
    wolf(I)


@icon('species_witch')
def _(I):
    I.shape('violet', [('p', [(17.5, 3), (22.5, 19), (9.5, 19), (13, 9)])])
    I.shape('violet', [('d', 16, 22, 13, 4.4)])
    I.shape('gold', [('r', 10.6, 16.2, 11, 2.6)], False)


@icon('species_hybrid')
def _(I):
    paw(I)
    if I.big:
        I.shape('blood', drop(16, 22.2, 0.36), False)
    else:
        I.tx(7, 8, '#C01830', 2, 3).tx(6, 9, '#C01830', 4, 2).tx(7, 9, '#FF6A60')


@icon('species_original')
def _(I):
    I.shape('bone', [('p', [(7.5, 15), (15, 15), (14.4, 21), (11.6, 29), (8.6, 21.5)])])
    I.shape('bone', [('p', [(17, 15), (24.5, 15), (23.4, 21.5), (20.4, 29), (17.6, 21)])])
    I.shape('gold', [('p', [(7, 12.5), (5.5, 3.5), (11.5, 8), (16, 3), (20.5, 8), (26.5, 3.5), (25, 12.5)]), ('r', 7, 12, 18, 3)])


@icon('species_original_hybrid')
def _(I):
    wolf(I, 16, 19, 0.92, 'tan', '#D6A014')
    I.shape('gold', [('p', [(9, 8), (8, 2.6), (12.4, 5.4), (16, 2.2), (19.6, 5.4), (24, 2.6), (23, 8)])], False)


@icon('species_siphoner')
def _(I):
    I.shape('skin', [('r', 9, 13, 14, 15), ('r', 9, 6.5, 3, 8), ('r', 12.7, 4, 3, 10), ('r', 16.4, 4.6, 3, 10), ('r', 20, 7, 3, 8), ('p', [(9, 17), (4.5, 13), (6.5, 11), (10, 14)])])
    I.shape('violet', spiral(16, 20.5, 0.44, 3.4), False)


@icon('species_heretic')
def _(I):
    I.shape('violet', [('a', 16, 16, 13.5, 2.4, 0, 360)])
    I.shape('bone', [('p', [(9.5, 9.5), (15.2, 9.5), (14.6, 15), (12.4, 23.5), (10.2, 15.5)])], False)
    I.shape('bone', [('p', [(16.8, 9.5), (22.5, 9.5), (21.8, 15.5), (19.6, 23.5), (17.4, 15)])], False)


@icon('species_tribrid')
def _(I):
    I.shape('blood', [('a', 16, 10.5, 7.5, 2.8, 0, 360)])
    I.shape('fur', [('a', 10, 20.5, 7.5, 2.8, 0, 360)])
    I.shape('violet', [('a', 22, 20.5, 7.5, 2.8, 0, 360)])
    I.shape('blood', [('a', 16, 10.5, 7.5, 2.8, 200, 250)], False)


@icon('species_ghoul')
def _(I):
    skull = [('d', 16, 13.5, 10.5, 9.5), ('r', 10.5, 19, 11, 8.5)]
    I.shape('bone', skull)
    I.paint([('d', 11.8, 14.5, 2.8, 3.2), ('d', 20.2, 14.5, 2.8, 3.2), ('p', [(16, 18), (17.6, 21.4), (14.4, 21.4)])], INK)
    I.paint([('r', 13.2, 23.6, 1.2, 4), ('r', 15.6, 23.6, 1.2, 4), ('r', 18, 23.6, 1.2, 4)], IR['bone'].o)
    I.tx(round(11.8 * I.k), round(14.6 * I.k), '#4FAE4A').tx(round(20.2 * I.k), round(14.6 * I.k), '#4FAE4A')


# ================================ pack ranks ================================
OMEGA32 = ['...######...', '..##....##..', '.##......##.', '.##......##.', '.##......##.', '.##......##.', '..##....##..', '...##..##...', '...##..##...', '.####..####.', '.####..####.']
OMEGA16 = ['.###.', '#...#', '#...#', '#...#', '.#.#.', '##.##']
BETA32 = ['.#####...', '.##..##..', '.##...##.', '.##..##..', '.#####...', '.##..###.', '.##...##.', '.##...##.', '.##..###.', '.######..', '.##......', '.##......']
BETA16 = ['###.', '#..#', '###.', '#..#', '#..#', '###.', '#...']
ALPHA32 = ['...........', '..####..##.', '.##..##.##.', '##....###..', '##....###..', '##.....##..', '##....###..', '##....###..', '.##..##.##.', '..####..##.', '...........']
ALPHA16 = ['.##.#', '#..#.', '#..#.', '#..#.', '.##.#']


@icon('rank_omega')
def _(I):
    medal(I, 'steel', OMEGA32, OMEGA16, ink='#17121A')


@icon('rank_beta')
def _(I):
    medal(I, 'gold', BETA32, BETA16, ink='#3A2206')


@icon('rank_alpha')
def _(I):
    medal(I, 'blood', ALPHA32, ALPHA16)


@icon('rank_true_alpha')
def _(I):
    I.shape('blood', [('d', 16, 18.5, 10.5)])
    rows = ALPHA32[1:-1] if I.big else ALPHA16
    I.cv.bmp(rows, {'#': '#FFFFF2'}, (I.n - len(rows[0])) // 2, round(14 * I.k) if I.big else 7)
    I.shape('gold', [('p', [(8, 9.5), (6.5, 2.8), (12, 6), (16, 2.2), (20, 6), (25.5, 2.8), (24, 9.5)]), ('r', 8, 9, 16, 2.6)])


# ================================ source tags ================================
@icon('tag_sb')
def _(I):
    I.shape('blood', drop())
    I.shape('moon', crescent(16, 20, 0.5), False)


@icon('tag_vmp')
def _(I):
    I.shape('gum', [('d', 16, 16, 12.5)])
    I.shape('bone', [('p', [(9.5, 10), (15.2, 10), (14.6, 15), (12.4, 23), (10.2, 15.5)])], False)
    I.shape('bone', [('p', [(16.8, 10), (22.5, 10), (21.8, 15.5), (19.6, 23), (17.4, 15)])], False)


@icon('tag_ww')
def _(I):
    I.shape('tan', [('d', 16, 16, 12.5)])
    for x in (12, 17, 22):
        I.paint([('p', [(x - 0.4, 7.5), (x + 2.6, 7.5), (x - 3.4, 24.5), (x - 5, 24.5)])], '#FFFFF2', [('d', 16, 16, 11)])


@icon('tag_enc')
def _(I):
    pot = [('d', 16, 19, 10.5, 8.5), ('-r', 0, 0, 32, 13)]
    I.shape('dark', pot)
    I.shape('dark', [('r', 4.5, 11.4, 23, 3.2)])
    I.shape('green', [('d', 12, 8, 2.2)], False)
    I.shape('green', [('d', 18.5, 5.5, 1.7)], False)
    I.shape('green', [('d', 21.5, 9.5, 1.3)], False)
    I.paint([('r', 6.5, 12.2, 19, 1.4)], '#4FAE4A')
    I.shape('dark', [('r', 8, 26.5, 3, 2.5)], False)
    I.shape('dark', [('r', 21, 26.5, 3, 2.5)], False)


@icon('tag_ten')
def _(I):
    slime = [('d', 16, 18, 12, 9.5), ('d', 16, 14, 8.5, 8), ('-r', 0, 26, 32, 6)]
    I.shape('sky', slime)
    k = I.k
    for x in (11.4, 19):
        I.tx(round(x * k), round(16 * k), '#16305A', max(1, round(1.6 * k)), max(2, round(4 * k)))
    if I.big:
        I.paint([('d', 10.5, 10.5, 2.2, 1.4)], '#FFFFFF')
    else:
        I.tx(4, 5, '#FFFFFF', 2, 1)


GROUPS = [('pages', ['home', 'path', 'skills', 'quests', 'rites', 'bloodline', 'pack', 'guide', 'loadout', 'settings', 'admin']),
          ('controls', ['check', 'lock', 'crown', 'dot', 'close', 'theme', 'launch', 'question', 'info', 'plus', 'arrow_left', 'arrow_right', 'bell', 'key', 'unknown']),
          ('sky', ['moon_new', 'moon_waxing_crescent', 'moon_first_quarter', 'moon_waxing_gibbous', 'moon_full', 'moon_waning_gibbous', 'moon_last_quarter', 'moon_waning_crescent', 'sun']),
          ('motifs', ['drop', 'shield', 'fang', 'vial', 'claw', 'heart', 'boot', 'eye', 'voice', 'spark', 'bolt']),
          ('actions', ['bite', 'alpha_roar', 'thirst', 'moonblood_surge', 'wolf_spirits', 'leap', 'rip_heart', 'neck_snap', 'bottle_blood', 'hybrid_leap']),
          ('skills', [n for n in ICONS if n.startswith('s-')]),
          ('bloodlines', [n for n in ICONS if n.startswith('species_')]),
          ('ranks', [n for n in ICONS if n.startswith('rank_')]),
          ('tags', [n for n in ICONS if n.startswith('tag_')])]


def draw(name, n):
    I = Ic(n)
    ICONS[name](I)
    return I.cv


def silhouette(cv, c):
    o = Cv(cv.w, cv.h)
    o.d = [c if v else 0 for v in cv.d]
    return o


HUD = ['bite', 'alpha_roar', 'unknown', 'rip_heart', 'neck_snap', 'bottle_blood', 'hybrid_leap']  # the slot icons: the same names as HudIcons.OURS
HUD_RAW = HUD[3:]  # textures/gui/hud/<name>.png of these is the 16 px icon itself (the Loadout tab draws it); the first three keep the pictures they had before the kit
PALE = rgba('#E6DCC8')


def hud_twin(cv):
    """The 16 px icon as a HUD slot draws it: its outer line pale, so an icon with a dark outline is not lost on the dark face of a slot."""
    o = cv.copy()
    for y in range(cv.h):
        for x in range(cv.w):
            if cv.get(x, y) and not (cv.get(x - 1, y) and cv.get(x + 1, y) and cv.get(x, y - 1) and cv.get(x, y + 1)):
                o.d[y * cv.w + x] = PALE
    return o


def build():
    names = [n for _, g in GROUPS for n in g]
    assert sorted(names) == sorted(ICONS), set(names) ^ set(ICONS)
    for n in (32, 16):
        cells = []
        for g, members in GROUPS:
            for name in members:
                cv = emit(draw(name, n), 'sprites/icons/%d/%s.png' % (n, name), '%s icon, %d px' % (g, n))
                cells.append((name, cv))
                if n == 16 and name in HUD:
                    emit(hud_twin(cv), 'sprites/icons/hud/%s.png' % name, 'HUD slot icon (pale outline), 16 px')
                    if name in HUD_RAW:
                        emit(cv, 'hud/%s.png' % name, 'HUD action texture (the Loadout tab), 16 px')
        sheet(os.path.join(PHASE2, 'icons-%d.png' % n), cells, rgba('#17171C'), zoom=3 if n == 32 else 4, bg2=rgba('#E9DDB8'), max_w=1500)
        sil = [(name, silhouette(cv, rgba('#101014'))) for name, cv in cells]
        sheet(os.path.join(PHASE2, 'icons-%d-silhouettes.png' % n), sil, rgba('#E8E8EC'), zoom=1, label=False, max_w=900 if n == 32 else 600, pad=4)
    # the mob effect of a snapped neck: the Neck Snap picture on the 18 x 18 of an effect icon
    emit(Cv(18, 18).blit(draw('neck_snap', 16), 1, 1), 'mob_effect/snapped.png', 'mob effect icon, 18 px', root=os.path.dirname(GUI))
    print('icons', len(names), 'x 2 sizes')
