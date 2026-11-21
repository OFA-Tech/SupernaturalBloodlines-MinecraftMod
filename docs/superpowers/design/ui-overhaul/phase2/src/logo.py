"""The logo, direction B "Drop": a blood drop with the moon cut in, in a red corona. Ported from phase1/src/scene-logo.js.

The mod icon is a 64 x 64 tile; the 128 x 128 file is that tile at an exact 2x. Nothing is scaled by a fraction.
"""
import os

from icons import IR, Ic, crescent, drop, star4
from px import PHASE2, RES, WHITE, Cv, Pal, disc_mask, emit, load_palettes, lum, pack, ramp, rgba, rr, sheet, with_a

MP = Pal(load_palettes()['bloodmoon'])
TILE = rgba('#12060A')
BANDS = [rgba(c) for c in ('#FFF1EA', '#FFD9CF', '#F6B1A5', '#E8786A')]
OUTLINE, SHADOW = rgba('#2A0B14'), rgba('#8A211D')
G57 = {
    'A': ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'], 'B': ['####.', '#...#', '#...#', '####.', '#...#', '#...#', '####.'],
    'D': ['###..', '#..#.', '#...#', '#...#', '#...#', '#..#.', '###..'], 'E': ['#####', '#....', '#....', '####.', '#....', '#....', '#####'],
    'I': ['.###.', '..#..', '..#..', '..#..', '..#..', '..#..', '.###.'], 'L': ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
    'N': ['#...#', '##..#', '##..#', '#.#.#', '#..##', '#..##', '#...#'], 'O': ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
    'P': ['####.', '#...#', '#...#', '####.', '#....', '#....', '#....'], 'R': ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'],
    'S': ['.####', '#....', '#....', '.###.', '....#', '....#', '####.'], 'T': ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
    'U': ['#...#', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
}


def bits(ch):
    return [[1 if c == '#' else 0 for c in r] for r in G57[ch]]


def scale2x(b):
    """Scale2x (EPX): doubles a bitmap and turns stair corners into diagonals. Whole texels in, whole texels out."""
    h, w = len(b), len(b[0])
    o = [[0] * (w * 2) for _ in range(h * 2)]

    def g(x, y):
        return 0 if x < 0 or y < 0 or x >= w or y >= h else b[y][x]
    for y in range(h):
        for x in range(w):
            P, A_, B_, C_, D_ = g(x, y), g(x, y - 1), g(x + 1, y), g(x - 1, y), g(x, y + 1)
            p1 = A_ if (C_ == A_ and C_ != D_ and A_ != B_) else P
            p2 = B_ if (A_ == B_ and A_ != C_ and B_ != D_) else P
            p3 = C_ if (D_ == C_ and D_ != B_ and C_ != A_) else P
            p4 = D_ if (B_ == D_ and B_ != A_ and D_ != C_) else P
            o[2 * y][2 * x], o[2 * y][2 * x + 1], o[2 * y + 1][2 * x], o[2 * y + 1][2 * x + 1] = p1, p2, p3, p4
    return o


def word_mask(text, big, gap):
    """Big letters are Scale2x of the 5 x 7 ones, thickened by one texel sideways: 11 x 14."""
    letters = []
    for ch in text:
        b = bits(ch)
        if big:
            b = [[1 if (v or (i and r[i - 1])) else 0 for i, v in enumerate(r + [0])] for r in scale2x(b)]
        letters.append(b)
    lw, lh = (11 if big else 5), (14 if big else 7)
    xs, cx = [], 0
    for i, ch in enumerate(text):
        if i and text[i - 1] == 'L' and ch == 'I':
            cx -= 3 if big else 1
        xs.append(cx)
        cx += lw + gap
    m = Cv(cx - gap, lh)
    for b, x0 in zip(letters, xs):
        for y, r in enumerate(b):
            for x, v in enumerate(r):
                if v:
                    m.px(x0 + x, y, 1)
    return m


def style_word(m, bands):
    body = Cv(m.w + 2, m.h + 2)
    for y in range(m.h):
        for x in range(m.w):
            if m.d[y * m.w + x]:
                body.px(x + 1, y + 1, bands[min(len(bands) - 1, y * len(bands) // m.h)])
    body.outline(OUTLINE)
    sh = Cv(body.w, body.h)
    sh.d = [SHADOW if v else 0 for v in body.d]
    return Cv(m.w + 6, m.h + 6).blit(sh, 1, 3).blit(body, 0, 0)


def wordmark():
    top = style_word(word_mask('SUPERNATURAL', False, 3), [BANDS[1], BANDS[2]])
    big = style_word(word_mask('BLOODLINES', True, 2), BANDS)
    w = max(top.w, big.w)
    return Cv(w, top.h + big.h + 1).blit(top, round((w - top.w) / 2), 0).blit(big, round((w - big.w) / 2), top.h + 1)


def icon_at(n, fn):
    I = Ic(n)
    fn(I)
    return I.cv


def emblem():
    c = Cv(56, 56)
    for i in (3, 2, 1):  # the corona: three flat steps
        c.disc(28, 28, 22 + i * 3, 22 + i * 3, with_a(MP.glow, round(46 / i)))
    c.bevel(disc_mask(50), ramp(MP.red, MP.edgeH, TILE, MP.void), 3, 3)
    c.ring(28, 28, 24, 24, 1, MP.red).ring(28, 28, 21.5, 21.5, 1, MP.redDD)

    def the_drop(I):
        I.shape('blood', drop())
        I.paint([('d', 11.4, 22, 1.3, 2.6)], '#FFC8C2').paint([('d', 12.4, 15.5, 0.8, 1.2)], '#FF9E96')

    def the_moon(I):
        I.shape(ramp('#5E0A16', '#FFFFFF', '#F2E6D0', '#C9B6A0'), crescent(16, 16), False)
        I.shape('gold', star4(22.5, 10, 3.6, 1), False)
    c.blit(icon_at(46, the_drop), 5, 6).blit(icon_at(22, the_moon), 17, 21)
    return c


def tile():
    t = Cv(64, 64).bevel(rr(62, 62, 4), ramp(MP.void, MP.edge, TILE, TILE), 1, 1)
    return t.blit(emblem(), 4, 4)


def grey(cv):
    o = Cv(cv.w, cv.h)
    for i, c in enumerate(cv.d):
        if c:
            g = round(lum(c) ** 0.45 * 255)
            o.d[i] = pack(g, g, g)
    return o


def build():
    emb, til, wm = emblem(), tile(), wordmark()
    lock = Cv(56 + 6 + wm.w, 56).blit(emb, 0, 0).blit(wm, 62, round((56 - wm.h) / 2))
    emit(til, 'logo.png', 'the mod icon, 64 x 64 (logoFile)', root=RES)
    emit(til.scaled(2), 'sprites/kit/shared/logo_128.png', 'the mod icon at an exact 2x, for stores')
    emit(emb, 'sprites/kit/shared/logo_emblem.png', 'the emblem alone', glow=True)
    emit(wm, 'sprites/kit/shared/logo_wordmark.png', 'the wordmark')
    emit(lock, 'sprites/kit/shared/logo_lockup.png', 'emblem and wordmark', glow=True)
    half = Cv(32, 32)
    for y in range(32):
        for x in range(32):
            half.d[y * 32 + x] = til.d[(2 * y) * 64 + 2 * x]
    row = Cv(300, 72).rect(0, 0, 300, 72, rgba('#26262E')).blit(til, 4, 4)
    banner = Cv(lock.w + 24, 72).rect(0, 0, lock.w + 24, 72, TILE).blit(lock, 12, 8)
    cells = [('logo.png 64 x 64', til), ('greyscale value check', grey(til)), ('every second texel (how it reads at 32)', half), ('emblem', emb), ('wordmark', wm),
             ('lockup on the tile colour', banner), ('mod list row (the game draws the text)', row)]
    sheet(os.path.join(PHASE2, 'logo.png'), cells, rgba('#101014'), zoom=3, max_w=1500)
    sheet(os.path.join(PHASE2, 'logo-128.png'), [('logo_128.png, real size', til.scaled(2))], rgba('#101014'), zoom=1, max_w=600)
    print('logo', 5)
