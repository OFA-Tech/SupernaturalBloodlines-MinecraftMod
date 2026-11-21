"""Pixel canvas for the phase 2 art kit: a Python port of phase1/src/lib-core.js and kit-common.js.

One texel is one GUI pixel. Colours are 0xAARRGGBB ints, 0 is empty. A mask is a Cv whose filled cells are 1.
Light comes from the top-left; bevel() gives a 1 texel outline in a darker shade of the shape's own colour.
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PHASE2 = os.path.dirname(HERE)
ROOT = os.path.abspath(os.path.join(PHASE2, '..', '..', '..', '..', '..'))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
GUI = os.path.join(RES, 'assets', 'supernaturalbloodlines', 'textures', 'gui')
WHITE, BLACK = 0xFFFFFFFF, 0xFF000000


def rgba(h):
    if isinstance(h, int):
        return h
    h = h.lstrip('#')
    a = int(h[6:8], 16) if len(h) == 8 else 255
    return (a << 24) | int(h[:6], 16)


def A(c): return c >> 24
def R(c): return (c >> 16) & 255
def G(c): return (c >> 8) & 255
def B(c): return c & 255
def pack(r, g, b, a=255): return (a << 24) | (r << 16) | (g << 8) | b
def with_a(c, a): return (a << 24) | (c & 0xFFFFFF)


def mix(c1, c2, t):
    return pack(round(R(c1) + (R(c2) - R(c1)) * t), round(G(c1) + (G(c2) - G(c1)) * t), round(B(c1) + (B(c2) - B(c1)) * t))


def lum(c):
    def lin(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    return 0.2126 * lin(R(c)) + 0.7152 * lin(G(c)) + 0.0722 * lin(B(c))


def contrast(a, b):
    x, y = lum(a), lum(b)
    return (max(x, y) + 0.05) / (min(x, y) + 0.05)


def rng(seed):
    """A seeded stream of numbers in [0, 1): the same art on every run."""
    import random
    return random.Random(seed).random


class Ramp:
    """o outline, hi highlight, b base, sh shade, hi2 / sh2 the second bands (bigger shapes)."""

    def __init__(self, o, hi, b, sh, hi2=None, sh2=None):
        self.o, self.hi, self.b, self.sh, self.hi2, self.sh2 = [rgba(v) if v is not None else None for v in (o, hi, b, sh, hi2, sh2)]


def ramp(o, hi, b, sh, hi2=None, sh2=None):
    return Ramp(o, hi, b, sh, hi2, sh2)


def flat(c):
    return Ramp(0, c, c, c)


class Cv:
    def __init__(self, w, h):
        self.w, self.h = int(w), int(h)
        self.d = [0] * (self.w * self.h)

    def get(self, x, y):
        return 0 if x < 0 or y < 0 or x >= self.w or y >= self.h else self.d[y * self.w + x]

    def px(self, x, y, c):
        x, y = int(x), int(y)
        if x < 0 or y < 0 or x >= self.w or y >= self.h or c == 0:
            return self
        i, sa = y * self.w + x, A(c)
        dst = self.d[i]
        if sa == 255 or sa == 0 or dst == 0 or A(dst) == 0:
            self.d[i] = c
            return self
        da = A(dst)
        oa = sa + da * (255 - sa) / 255
        f, g = sa / oa, (da * (255 - sa) / 255) / oa
        self.d[i] = pack(round(R(c) * f + R(dst) * g), round(G(c) * f + G(dst) * g), round(B(c) * f + B(dst) * g), round(oa))
        return self

    def clr(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.d[y * self.w + x] = 0
        return self

    def rect(self, x, y, w, h, c):
        for j in range(int(h)):
            for i in range(int(w)):
                self.px(x + i, y + j, c)
        return self

    def box(self, x, y, w, h, c):
        self.rect(x, y, w, 1, c).rect(x, y + h - 1, w, 1, c).rect(x, y + 1, 1, h - 2, c).rect(x + w - 1, y + 1, 1, h - 2, c)
        return self

    def hl(self, x, y, w, c): return self.rect(x, y, w, 1, c)
    def vl(self, x, y, h, c): return self.rect(x, y, 1, h, c)

    def line(self, x0, y0, x1, y1, c):
        x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        e = dx + dy
        while True:
            self.px(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * e
            if e2 >= dy:
                e += dy
                x0 += sx
            if e2 <= dx:
                e += dx
                y0 += sy
        return self

    def disc(self, cx, cy, rx, ry=None, c=1):
        ry = rx if ry is None else ry
        if rx <= 0 or ry <= 0:
            return self
        for j in range(math.floor(cy - ry - 1), math.ceil(cy + ry + 1) + 1):
            for i in range(math.floor(cx - rx - 1), math.ceil(cx + rx + 1) + 1):
                dx, dy = (i + 0.5 - cx) / rx, (j + 0.5 - cy) / ry
                if dx * dx + dy * dy <= 1.0:
                    self.px(i, j, c)
        return self

    def ring(self, cx, cy, rx, ry, t, c):
        ix, iy = (rx - t) / rx, (ry - t) / ry
        for j in range(math.floor(cy - ry - 1), math.ceil(cy + ry + 1) + 1):
            for i in range(math.floor(cx - rx - 1), math.ceil(cx + rx + 1) + 1):
                dx, dy = (i + 0.5 - cx) / rx, (j + 0.5 - cy) / ry
                if dx * dx + dy * dy <= 1.0 and (dx * dx) / (ix * ix) + (dy * dy) / (iy * iy) >= 1.0:
                    self.px(i, j, c)
        return self

    def arc(self, cx, cy, r, t, a0, a1, c):
        """Ring segment between angles a0 and a1 in degrees, clockwise from twelve o'clock."""
        for j in range(math.floor(cy - r - 1), math.ceil(cy + r + 1) + 1):
            for i in range(math.floor(cx - r - 1), math.ceil(cx + r + 1) + 1):
                dx, dy = i + 0.5 - cx, j + 0.5 - cy
                d = math.hypot(dx, dy)
                if d > r or d < r - t:
                    continue
                a = math.degrees(math.atan2(dx, -dy)) % 360
                if (a0 <= a <= a1) if a0 <= a1 else (a >= a0 or a <= a1):
                    self.px(i, j, c)
        return self

    def poly(self, pts, c=1):
        ys, xs = [p[1] for p in pts], [p[0] for p in pts]
        n = len(pts)
        for j in range(math.floor(min(ys)), math.ceil(max(ys)) + 1):
            for i in range(math.floor(min(xs)), math.ceil(max(xs)) + 1):
                X, Y, inside = i + 0.5, j + 0.5, False
                b = n - 1
                for a in range(n):
                    xa, ya = pts[a]
                    xb, yb = pts[b]
                    if ((ya > Y) != (yb > Y)) and (X < (xb - xa) * (Y - ya) / (yb - ya) + xa):
                        inside = not inside
                    b = a
                if inside:
                    self.px(i, j, c)
        return self

    def blit(self, o, x, y, flip_x=False, flip_y=False):
        for j in range(o.h):
            for i in range(o.w):
                c = o.d[j * o.w + i]
                if c:
                    self.px(x + (o.w - 1 - i if flip_x else i), y + (o.h - 1 - j if flip_y else j), c)
        return self

    def bmp(self, rows, pal, x=0, y=0, flip_x=False):
        for j, r in enumerate(rows):
            for i, ch in enumerate(r):
                if ch not in '. ' and ch in pal:
                    self.px(x + (len(r) - 1 - i if flip_x else i), y + j, rgba(pal[ch]))
        return self

    def grain(self, x, y, w, h, c, density, seed, length=2):
        r = rng(seed)
        for j in range(y, y + h):
            for i in range(x, x + w):
                if r() < density:
                    for k in range(length):
                        self.px(i + k, j, c)
        return self

    def dash(self, x, y, w, c, on=3, off=3):
        for i in range(0, w, on + off):
            self.rect(x + i, y, min(on, w - i), 1, c)
        return self

    def copy(self):
        o = Cv(self.w, self.h)
        o.d = list(self.d)
        return o

    def mask(self):
        m = Cv(self.w, self.h)
        m.d = [1 if v else 0 for v in self.d]
        return m

    def cut(self, other, ox=0, oy=0):
        """Clear every cell that `other` fills."""
        for j in range(other.h):
            for i in range(other.w):
                if other.d[j * other.w + i]:
                    self.clr(ox + i, oy + j)
        return self

    def keep(self, other):
        """Clear every cell that `other` does not fill (same size)."""
        for i in range(len(self.d)):
            if not other.d[i]:
                self.d[i] = 0
        return self

    def outline(self, c):
        o = list(self.d)
        for j in range(self.h):
            for i in range(self.w):
                if not self.get(i, j) and (self.get(i - 1, j) or self.get(i + 1, j) or self.get(i, j - 1) or self.get(i, j + 1)):
                    o[j * self.w + i] = c
        self.d = o
        return self

    def bevel(self, mask, r, ox=0, oy=0, band2=False):
        def m(x, y):
            return 0 if x < 0 or y < 0 or x >= mask.w or y >= mask.h else (1 if mask.d[y * mask.w + x] else 0)
        for y in range(-1, mask.h + 1):
            for x in range(-1, mask.w + 1):
                if m(x, y):
                    up, dn, lf, rt = m(x, y - 1), m(x, y + 1), m(x - 1, y), m(x + 1, y)
                    c = r.b
                    if not up:
                        c = r.hi
                    elif not dn:
                        c = r.sh
                    elif not lf:
                        c = r.hi
                    elif not rt:
                        c = r.sh
                    elif band2:
                        if not m(x, y - 2) or not m(x - 2, y):
                            c = r.hi2 or r.hi
                        elif not m(x, y + 2) or not m(x + 2, y):
                            c = r.sh2 or r.sh
                    self.px(ox + x, oy + y, c)
                elif r.o and (m(x - 1, y) or m(x + 1, y) or m(x, y - 1) or m(x, y + 1)):
                    self.px(ox + x, oy + y, r.o)
        return self

    def stamp(self, x, y, mask, c):
        for j in range(mask.h):
            for i in range(mask.w):
                if mask.d[j * mask.w + i]:
                    self.px(x + i, y + j, c)
        return self

    def paint_in(self, mask, shape, c):
        """Paint `shape` (a mask of the same size) where `mask` is filled, replacing what is there."""
        for i in range(len(self.d)):
            if shape.d[i] and mask.d[i]:
                self.d[i] = c
        return self

    def flipped(self, fx=False, fy=False):
        return Cv(self.w, self.h).blit(self, 0, 0, fx, fy)

    def scaled(self, k):
        o = Cv(self.w * k, self.h * k)
        for j in range(self.h):
            for i in range(self.w):
                c = self.d[j * self.w + i]
                if c:
                    for b in range(k):
                        for a in range(k):
                            o.d[(j * k + b) * o.w + i * k + a] = c
        return o

    def bounds(self):
        xs = [i % self.w for i, v in enumerate(self.d) if v]
        ys = [i // self.w for i, v in enumerate(self.d) if v]
        return (min(xs), min(ys), max(xs), max(ys)) if xs else None

    def image(self):
        im = Image.new('RGBA', (self.w, self.h))
        im.putdata([(R(c), G(c), B(c), A(c)) if c and A(c) else (0, 0, 0, 0) for c in self.d])
        return im

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.image().save(path, optimize=True)
        return self


# ---------- masks ----------
def solid(w, h):
    m = Cv(w, h)
    m.d = [1] * (m.w * m.h)
    return m


def rr(w, h, r):
    """Chamfered rectangle: corner cells with dx + dy < r are removed."""
    return notch(w, h, r, True, True, True, True)


def notch(w, h, r, tl, tr, bl, br):
    m = solid(w, h)
    for y in range(m.h):
        for x in range(m.w):
            if (tl and x + y < r) or (tr and (m.w - 1 - x) + y < r) or (bl and x + (m.h - 1 - y) < r) or (br and (m.w - 1 - x) + (m.h - 1 - y) < r):
                m.d[y * m.w + x] = 0
    return m


def pill(w, h):
    m, r = Cv(w, h), h / 2
    for y in range(m.h):
        for x in range(m.w):
            px_, py_ = x + 0.5, y + 0.5
            cx = min(max(px_, r), w - r)
            if (px_ - cx) ** 2 + (py_ - r) ** 2 <= r * r:
                m.d[y * m.w + x] = 1
    return m


def disc_mask(d):
    return Cv(d, d).disc(d / 2, d / 2, d / 2, d / 2, 1)


def hex_mask(w, h, tip):
    """Flat-top hexagon with points left and right; `tip` columns are inset at the top row."""
    m = Cv(w, h)
    for y in range(m.h):
        ins = round(tip * abs(y + 0.5 - h / 2) / (h / 2))
        for x in range(ins, m.w - ins):
            m.d[y * m.w + x] = 1
    return m


def diamond(n):
    h = (n - 1) / 2
    return Cv(n, n).poly([(h + 0.5, 0), (n, h + 0.5), (h + 0.5, n), (0, h + 0.5)], 1)


def pie(cv, cx, cy, r, left, c):
    """Cooldown sweep: covers `left` of the disc, clockwise from twelve o'clock."""
    for j in range(math.floor(cy - r), math.ceil(cy + r) + 1):
        for i in range(math.floor(cx - r), math.ceil(cx + r) + 1):
            dx, dy = i + 0.5 - cx, j + 0.5 - cy
            if dx * dx + dy * dy > r * r:
                continue
            a = math.atan2(dx, -dy)
            if a < 0:
                a += 2 * math.pi
            if a / (2 * math.pi) < left:
                cv.px(i, j, c)


def load_palettes():
    with open(os.path.join(PHASE2, '..', 'phase1', 'palettes.json'), encoding='utf8') as f:
        raw = json.load(f)
    return {t: {k: rgba(v) for k, v in raw[t].items()} for t in ('grimoire', 'bloodmoon', 'tensura')}


class Pal(dict):
    __getattr__ = dict.__getitem__


# ---------- output ----------
MANIFEST = []


def emit(cv, rel, use, slice_=None, glow=False, root=GUI):
    """Write one sprite and record it. `slice_` is (left, top, right, bottom), 'tile' or None (stretch).

    A sprite under gui/sprites that scales gets the .mcmeta Minecraft reads for blitSprite (nine_slice or tile).
    """
    path = os.path.join(root, *rel.split('/'))
    cv.save(path)
    scaling = 'tile' if slice_ == 'tile' else 'nine_slice' if slice_ else 'stretch'
    if rel.startswith('sprites/'):
        meta = path + '.mcmeta'
        if scaling == 'stretch':
            if os.path.exists(meta):
                os.remove(meta)
        else:
            body = {'type': scaling, 'width': cv.w, 'height': cv.h}
            if scaling == 'nine_slice':
                body['border'] = dict(zip(('left', 'top', 'right', 'bottom'), slice_))
            with open(meta, 'w', encoding='utf8', newline='\n') as f:
                json.dump({'gui': {'scaling': body}}, f, indent=2)
                f.write('\n')
    MANIFEST.append({'path': rel if root == GUI else os.path.relpath(path, RES).replace('\\', '/'), 'sprite': rel[len('sprites/'):-4] if rel.startswith('sprites/') else None,
                     'w': cv.w, 'h': cv.h, 'scaling': scaling, 'slice': list(slice_) if scaling == 'nine_slice' else None, 'glow': glow, 'use': use, '_cv': cv})
    return cv


def nine(cv, w, h, sl):
    """Stretch a nine-slice sprite to w x h by tiling its edges and centre: a preview of what the game will draw."""
    l, t, r, b = sl
    o = Cv(w, h)
    cw, ch = max(1, cv.w - l - r), max(1, cv.h - t - b)
    for y in range(h):
        sy = y if y < t else (cv.h - (h - y) if y >= h - b else t + (y - t) % ch)
        for x in range(w):
            sx = x if x < l else (cv.w - (w - x) if x >= w - r else l + (x - l) % cw)
            o.d[y * w + x] = cv.d[sy * cv.w + sx]
    return o


def sheet(path, cells, bg, zoom=4, pad=8, max_w=1560, bg2=None, label=True):
    """Contact sheet, flowed in rows: every sprite at real size and, right of it, at `zoom`. cells = [(name, Cv)].

    With bg2 the zoomed copy is shown twice, on bg and on bg2 (icons must read on a light and on a dark ground).
    """
    from PIL import ImageDraw
    rgb = lambda c: (R(c), G(c), B(c), 255)
    placed, x, y, row_h = [], pad, pad, 0
    for name, c in cells:
        copies = 2 if bg2 is not None else 1
        w = max(c.w + pad + (c.w * zoom + pad) * copies, len(name) * 6 + pad if label else 0)
        h = c.h * zoom + (14 if label else 2)
        if x + w > max_w and x > pad:
            x, y, row_h = pad, y + row_h + pad, 0
        placed.append((name, c, x, y))
        x, row_h = x + w + pad, max(row_h, h)
    W, H = max(px_ + max(c.w + pad + (c.w * zoom + pad) * (2 if bg2 is not None else 1), len(n) * 6) for n, c, px_, _ in placed) + pad, y + row_h + pad
    im = Image.new('RGBA', (W, H), rgb(bg))
    dr = ImageDraw.Draw(im)
    for name, c, x, y in placed:
        one, big = c.image(), c.image().resize((c.w * zoom, c.h * zoom), Image.NEAREST)
        im.alpha_composite(one, (x, y))
        im.alpha_composite(big, (x + c.w + pad, y))
        if bg2 is not None:
            bx = x + c.w + pad + c.w * zoom + pad
            dr.rectangle([bx - 2, y - 2, bx + c.w * zoom + 1, y + c.h * zoom + 1], fill=rgb(bg2))
            im.alpha_composite(big, (bx, y))
        if label:
            dr.text((x, y + c.h * zoom + 2), name, fill=(150, 150, 160, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    return path
