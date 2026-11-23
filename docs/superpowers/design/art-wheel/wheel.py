"""The sprites of the skill wheel, after the approved W3 mockups: drawn here texel by texel with the phase 2 canvas and palettes.

Run: py docs/superpowers/design/art-wheel/wheel.py
Writes the PNGs (and the .mcmeta of the ones that scale) under textures/gui/sprites, adds them to phase2/manifest.json, checks them
and draws contact-sheet.png next to this file. One texel is one GUI pixel.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'ui-overhaul', 'phase2', 'src'))
import build  # noqa: E402
import kit as K  # noqa: E402
from icons import INK  # noqa: E402
from px import BLACK, GUI, WHITE, Cv, Pal, contrast, emit, nine, pie, ramp, rgba, rr, with_a  # noqa: E402

KITS = (K.Grimoire(), K.Moon(), K.Tensura())
PG, PM, PT = (k.P for k in KITS)
LIGHT, DARK = rgba('#E4EAF2'), rgba('#2A2E36')  # the steel of the lock icon: the two tones of the locked outline
HALO = (153, 82, 41, 20)  # the kit's stepped glow, two texels a step


# ---------- shapes ----------
def rnd(n, r):
    """A square with round corners."""
    m = Cv(n, n)
    for y in range(n):
        for x in range(n):
            cx, cy = min(max(x + 0.5, r), n - r), min(max(y + 0.5, r), n - r)
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                m.d[y * n + x] = 1
    return m


def hexcut(n, ins):
    """Tensura's hexagon with its side points cut flat: `ins` texels come off each end of the first and last rows."""
    m = Cv(n, n)
    for y in range(n):
        e = min(y, n - 1 - y)
        i = ins[e] if e < len(ins) else 0
        for x in range(i, n - i):
            m.d[y * n + x] = 1
    return m


# the frame (34, drawn one texel in so its outline fills the 36) and the well (28, the live area of a 32 px icon)
SHAPES = {'grimoire': (rr(34, 34, 2), rr(28, 28, 1)), 'bloodmoon': (rnd(34, 9), rnd(28, 6)),
          'tensura': (hexcut(34, (7, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1)), hexcut(28, (5, 4, 4, 3, 3, 2, 2, 1, 1)))}


def cell(k, active):
    """The 36 x 36 frame of an outer ring power: its 32 px icon goes at 2, 2."""
    P, (frame, well) = k.P, SHAPES[k.id]
    if k.id == 'grimoire':  # a page tile, gilded when the power is on
        f, w = (ramp(P.brD, P.brHH, P.brH, P.br) if active else k.R.page), k.R.well
    elif k.id == 'bloodmoon':
        f, w = ramp(P.hot if active else P.moon5, P.red if active else P.edgeH, P.cardH if active else P.card, P.cardD), ramp(0, P.void, P.cardD, P.cardD)
    else:
        f, w = ramp(P.tealH if active else P.teal, P.teal if active else P.edgeH, P.lit if active else P.panel, P.panelD), ramp(0, P.panelD, '#0A1827', P.panelD)
    return Cv(36, 36).bevel(frame, f, 1, 1).bevel(well, w, 4, 4)


def halo(k):
    """The glow behind a power that is on: 52 x 52, the cell at 8, 8. Four flat steps, no blur."""
    body = Cv(52, 52).bevel(SHAPES[k.id][0], ramp(1, 1, 1, 1), 9, 9)
    rim = [(x, y) for y in range(52) for x in range(52) if body.get(x, y) and not (body.get(x - 1, y) and body.get(x + 1, y) and body.get(x, y - 1) and body.get(x, y + 1))]
    cv, c = Cv(52, 52), PG.ready if k.id == 'grimoire' else k.P.glow  # on parchment the pale glow is lost: the gold of the kit's active slot
    for y in range(52):
        for x in range(52):
            d = 0 if body.get(x, y) else min(math.hypot(x - a, y - b) for a, b in rim)
            if d <= 8:
                cv.d[y * 52 + x] = with_a(c, HALO[max(0, math.ceil(d / 2) - 1)])
    return cv


def edge(k):
    """The piece on the edge of a ring: Grimoire's rivet at a spoke end, Blood Moon's hairline round the rim (a tile), Tensura's tick at a vertex."""
    P = k.P
    if k.id == 'grimoire':
        return Cv(3, 3).bmp(['.t.', 'tHd', '.d.'], {'t': P.bord, 'H': P.brHH, 'd': P.brD}), None
    if k.id == 'bloodmoon':
        return Cv(8, 1).rect(0, 0, 8, 1, P.red), 'tile'
    return Cv(2, 2).bmp(['Ht', 'ts'], {'H': P.tealH, 't': P.teal, 's': '#2AA0A8'}), None


def caret(k):
    """The mark of the open path, pointing up: 7 x 5."""
    pal = {'grimoire': {'O': PG.brD, 'H': PG.brHH, 'F': PG.brH, 'S': PG.br}, 'bloodmoon': {'O': PM.void, 'H': PM.moon, 'F': PM.gold, 'S': PM.goldD},
           'tensura': {'O': PT.void, 'H': WHITE, 'F': PT.tealH, 'S': PT.teal}}[k.id]
    return Cv(7, 5).bmp(['...O...', '..OHO..', '.OHFSO.', 'OHFFFSO', 'OOOOOOO'], pal)


def lock_outline():
    """The dashed line round a locked cell, 42 x 42: light and dark dashes of four, so one of them reads on any ground. The middle 32 tile."""
    cv = Cv(42, 42)
    for i in range(4, 38):
        c = LIGHT if ((i - 7) // 4) % 2 else DARK
        cv.px(i, 0, c).px(i, 41, c).px(0, i, c).px(41, i, c)
    for a, b in ((3, 1), (2, 2), (1, 3)):  # the round corners
        cv.px(a, b, LIGHT).px(41 - a, b, LIGHT).px(a, 41 - b, LIGHT).px(41 - a, 41 - b, LIGHT)
    return cv, (5, 5, 5, 5)


def sweep():
    """The cooldown pie of a 32 px icon: 33 frames of 32 x 32, frame 0 empty, like the 16 px strip. A 28 px disc, the well of every theme."""
    cv = Cv(32, 32 * 33)
    for step in range(1, 33):
        f = Cv(32, 32)
        pie(f, 16, 16, 14, step / 32, with_a(BLACK, 175))
        cv.blit(f, 0, step * 32)
    return cv


# ---------- the two mod icons of the inner ring, 16 px ----------
# offset from the middle, then light, body, shade. Clockwise from the top: power, space, reality, soul, time, mind
STONES = (((0, -4), '#C9A6FF', '#A47EEA', '#8A5AD6'), ((4, -2), '#CFE6FF', '#8EC0F4', '#5A9AE0'), ((4, 2), '#FF8F85', '#D8182E', '#7A0E1E'),
          ((0, 4), '#FFC25A', '#F08A2A', '#B8501A'), ((-4, 2), '#B8F08A', '#84D66A', '#4FAE4A'), ((-4, -2), '#FFF0B0', '#FFD86A', '#E8B33A'))


def infinity_stones():
    """Six cut stones set in a ring of dark metal."""
    cv = Cv(16, 16)
    for (dx, dy), hi, b, sh in STONES:
        cv.bmp(['HBB', 'BBS', 'BSS'], {'H': hi, 'B': b, 'S': sh}, 7 + dx, 7 + dy)
    return cv.outline(INK).rect(7, 7, 3, 3, INK).rect(6, 8, 5, 1, INK)


SAGE = ['........K.......',
        '.......KTK......',
        '......KKHKK.....',
        '.....KTHWHTK....',
        '......KKHKK.....',
        '.......KTK......',
        '........K.......',
        '..OOOOO...OOOOO.',
        '.OPPPPPO.OpppppO',
        '.OPlllPPOpplllpO',
        '.OPPPPPPOppppppO',
        '.OPllllPOpllllpO',
        '.OppppppOssssssO',
        '.kCCCCCCkCCCCCCk',
        '..kkkkkkkkkkkkk.',
        '................']
SAGE_PAL = {'K': '#0F3A44', 'T': '#3FD0D6', 'H': '#D8FEFF', 'W': '#FFFFFF', 'O': '#5A3A1A', 'P': '#FFF6DC', 'p': '#E9DDB8', 's': '#BBA874', 'l': '#A08C5A', 'C': '#1F8A94',
            'k': '#0F3A44'}


def great_sage():
    """An open book under a star of insight."""
    return Cv(16, 16).bmp(SAGE, SAGE_PAL)


# ---------- build ----------
def build_all():
    out = {}
    lock, sl = lock_outline()
    for k in KITS:
        e, tile = edge(k)
        out[k.id] = Pal(
            cell=emit(cell(k, False), 'sprites/kit/%s/wheel_cell_36.png' % k.id, 'skill wheel: the frame of a 32 px power on the outer ring'),
            active=emit(cell(k, True), 'sprites/kit/%s/wheel_cell_36_active.png' % k.id, 'skill wheel: the frame of a power that is on'),
            glow=emit(halo(k), 'sprites/kit/%s/cell_glow_52.png' % k.id, 'skill wheel: the halo behind a power that is on (the cell at 8, 8)', None, True),
            edge=emit(e, 'sprites/kit/%s/wheel_edge.png' % k.id, 'skill wheel: ring edge piece (rivet, rim hairline, vertex tick)', tile),
            caret=emit(caret(k), 'sprites/kit/%s/wheel_caret.png' % k.id, 'skill wheel: the caret of the open path, pointing up'))
    shared = Pal(
        sweep=emit(sweep(), 'sprites/kit/shared/cooldown_sweep_32.png', 'cooldown pie of a 32 px icon, 33 frames of 32 x 32 (frame 0 empty)', None, True),
        lock=emit(lock, 'sprites/kit/shared/lock_outline_42.png', 'skill wheel: the dashed outline of a locked cell', sl),
        stones=emit(infinity_stones(), 'sprites/icons/16/infinity_stones.png', 'mod icon of the inner ring, 16 px'),
        sage=emit(great_sage(), 'sprites/icons/16/great_sage.png', 'mod icon of the inner ring, 16 px'))
    return out, shared


def grounds(k):
    """Where the wheel puts a sprite: the outer band, the band lit (hovered, the open fan), the middle track, the inner ring, the well of a slot."""
    return {'grimoire': dict(band=PG.paS, lit=PG.paH, track=PG.paD, inner=PG.le, well=rgba('#2A1A18')),
            'bloodmoon': dict(band=PM.cardD, lit=PM.cardH, track=PM.card, inner=PM.card, well=PM.cardD),
            'tensura': dict(band=PT.panel, lit=PT.panelH, track=PT.panelD, inner=PT.panel, well=rgba('#0A1827'))}[k.id]


def check(out, shared):
    """The 3:1 non-text rule: the tone that carries each sprite against every ground it is drawn on. Returns the table of the README."""
    rows, bad = [], []

    def need(theme, sprite, tones, where):
        for name, g in where.items():
            best = max(contrast(t, g) for t in tones)
            rows.append((theme, sprite, name, best))
            if best < 3:
                bad.append('%s %s on %s: %.2f' % (theme, sprite, name, best))

    for k in KITS:
        o, g = out[k.id], grounds(k)
        outer = {n: g[n] for n in ('band', 'lit')}
        rings = {n: g[n] for n in ('band', 'lit', 'track', 'inner')}
        need(k.id, 'wheel_cell_36', [o.cell.get(18, 0)], outer)
        need(k.id, 'wheel_cell_36_active', [o.active.get(18, 0)], outer)
        need(k.id, 'wheel_caret', [o.caret.get(3, 0), o.caret.get(3, 3)], rings)
        need(k.id, 'wheel_caret fill on its outline', [o.caret.get(3, 3)], {'outline': o.caret.get(3, 0)})
        need(k.id, 'wheel_edge', [c for c in o.edge.d if c], {'band': g['band'], 'inner': g['inner']} if k.id == 'grimoire' else {'band': g['band']})
        need(k.id, 'lock_outline_42', [LIGHT, DARK], outer)
        for name, (_, _, body, _) in zip(('power', 'space', 'reality', 'soul', 'time', 'mind'), STONES):
            need(k.id, 'infinity_stones ' + name, [rgba(body)], {'well': g['well']})
        need(k.id, 'great_sage', [rgba(SAGE_PAL['P'])], {'well': g['well']})
    assert not bad, '\n'.join(bad)
    lock, wide = shared.lock, nine(shared.lock, 58, 58, (5, 5, 5, 5))
    assert lock.flipped(True).d == lock.d and lock.flipped(False, True).d == lock.d, 'the dashes mirror'
    assert all(wide.get(x, 0) == wide.get(x + 8, 0) for x in range(5, 45)), 'the dashes tile without a seam'
    assert not any(shared.sweep.d[:32 * 32]) and sum(1 for c in shared.sweep.d[32 * 32 * 32:] if c) > 600, 'frame 0 is empty, frame 32 the whole disc'
    return rows


# ---------- contact sheet ----------
W, H, C = 450, 292, (146, 146)
SKY, GRASS = rgba('#2E3C57'), rgba('#2E3F1B')
POWERS = ('fang', 'boot', 'heart', 'spark', 'bolt', 'shield', 'eye', 'leap', 'vial', 'claw')
MODS = ('tag_sb', 'tag_vmp', 'tag_ww', 'tag_ten', 'infinity_stones', 'great_sage')  # clockwise from the top left, the owner's order


def load(rel):
    im = Image.open(os.path.join(GUI, 'sprites', *rel.split('/')) + '.png').convert('RGBA')
    cv = Cv(*im.size)
    b = im.tobytes()
    cv.d = [(b[i + 3] << 24) | (b[i] << 16) | (b[i + 1] << 8) | b[i + 2] if b[i + 3] else 0 for i in range(0, len(b), 4)]
    return cv


def grey(cv):
    """A locked power: its picture without colour, dimmed (the code does this, not a sprite)."""
    o = Cv(cv.w, cv.h)
    o.d = [0 if not c else (c & 0xFF000000) | (int(((c >> 16 & 255) * 3 + (c >> 8 & 255) * 6 + (c & 255)) / 10 * 0.6) * 0x010101) for c in cv.d]
    return o


def turned(cv):
    """A quarter turn clockwise."""
    o = Cv(cv.h, cv.w)
    for y in range(cv.h):
        for x in range(cv.w):
            o.d[x * o.w + cv.h - 1 - y] = cv.d[y * cv.w + x]
    return o


def at(a, r):
    """The point at angle a (degrees clockwise from twelve o'clock) and radius r from the middle of the wheel."""
    return C[0] + r * math.sin(math.radians(a)), C[1] - r * math.cos(math.radians(a))


def scene(k, o, shared):
    """The wheel of the mockup's first frame at 1 GUI px a texel, then every new sprite alone. Bands and strokes are flat fills, as the code will draw them."""
    g = grounds(k)
    look = {'grimoire': dict(open=PG.paS, hub=PG.leD, rim=PG.br, spoke=PG.brS), 'bloodmoon': dict(open=PM.redD, hub=PM.void, rim=PM.redD, spoke=PM.edge),
            'tensura': dict(open=PT.edgeH, hub=PT.void, rim=PT.edgeH, spoke=PT.edge)}[k.id]
    cx, cy = C
    cv = Cv(W, H).rect(0, 0, W, 180, SKY).rect(0, 180, W, H - 180, GRASS)
    cv.arc(cx, cy, 126, 38, 204, 156, g['band']).arc(cx, cy, 86, 34, 0, 360, g['track']).arc(cx, cy, 86, 34, 249, 291, g['lit'])
    cv.arc(cx, cy, 49, 30, 0, 360, g['inner']).arc(cx, cy, 49, 30, 240, 300, look['open']).disc(cx, cy, 17, 17, look['hub'])
    for r, a0, a1 in ((126, 204, 156), (89, 204, 156), (86, 249, 291), (49, 0, 360), (20, 0, 360)):
        cv.arc(cx, cy, r, 1, a0, a1, look['rim'])
    cuts = [204 + 31.2 * i for i in range(11)]
    for a in cuts:
        cv.line(*at(a, 89), *at(a, 125), look['spoke'])
    for a in range(0, 360, 60):
        cv.line(*at(a, 20), *at(a, 48), look['spoke'])
    if k.id == 'bloodmoon':
        cv.arc(cx, cy, 129, 1, 204, 156, o.edge.get(0, 0))
    else:
        for a, r in [(a, r) for a in cuts for r in ((125.5, 88.5) if k.id == 'grimoire' else (125.5,))] + ([(a, 48.5) for a in range(0, 360, 60)] if k.id == 'grimoire' else []):
            x, y = at(a, r)
            cv.blit(o.edge, round(x - o.edge.w / 2), round(y - o.edge.h / 2))
    for i, name in enumerate(POWERS):
        x, y = at(204 + 31.2 * (i + 0.5), 107)
        power(cv, o, shared, round(x) - 18, round(y) - 18, load('icons/32/' + name), {1: 'on', 2: 'cooldown', 5: 'locked'}.get(i))
    for i, name in enumerate(MODS):
        x, y = at(330 + 60 * i, 34)
        x, y = round(x) - 14, round(y) - 14
        cv.blit(load('kit/%s/slot%s' % (k.id, '_active' if i == 5 else '')), x, y).blit(load('icons/16/' + name), x + 6, y + 6)
    side = turned(o.caret)
    cv.blit(side, cx - 56, cy - 4).blit(side.flipped(True), cx - 95, cy - 4)

    # every sprite alone: on the band, on the band lit, on the inner ring
    cv.rect(296, 8, 150, 232, g['band']).rect(296, 240, 150, 22, g['lit']).rect(296, 262, 150, 22, g['inner'])
    cv.blit(o.cell, 304, 14).blit(o.active, 346, 14).blit(o.glow, 390, 8)
    cv.blit(shared.lock, 304, 74)
    power(cv, o, shared, 357, 77, load('icons/32/shield'), 'locked')
    for i, step in enumerate((8, 16, 24)):
        power(cv, o, shared, 304 + 42 * i, 130, load('icons/32/heart'), step)
    for i, icon in enumerate((shared.stones, shared.sage)):
        cv.blit(load('kit/%s/slot' % k.id), 304 + 32 * i, 180).blit(icon, 310 + 32 * i, 186).blit(icon, 376 + 22 * i, 186)
    for y in (224, 246, 268):
        cv.blit(o.edge, 306, y + 2).blit(o.caret, 322, y).blit(side, 336, y - 1)
    for i, icon in enumerate((shared.stones, shared.sage)):
        cv.blit(icon, 376 + 22 * i, 243).blit(icon, 376 + 22 * i, 265)
    labels = [(304, 52, 'wheel_cell_36'), (346, 61, '_active'), (392, 61, 'cell_glow_52'), (304, 118, 'lock_outline_42, alone and on a locked cell'),
              (304, 168, 'cooldown_sweep_32: frames 8, 16, 24'), (304, 210, 'infinity_stones, great_sage (16 px)'), (350, 228, 'wheel_edge, wheel_caret'),
              (422, 244, 'lit'), (418, 266, 'inner')]
    return cv, labels


def power(cv, o, shared, x, y, icon, state):
    """One outer cell as the wheel stacks it. state: None, 'on', 'locked', 'cooldown' or a sweep frame."""
    if state == 'on':
        cv.blit(o.glow, x - 8, y - 8)
    cv.blit(o.active if state == 'on' else o.cell, x, y).blit(grey(icon) if state == 'locked' else icon, x + 2, y + 2)
    if state == 'locked':
        cv.blit(load('icons/16/lock'), x + 20, y + 20).blit(shared.lock, x - 3, y - 3)
    elif state not in (None, 'on'):
        step = 20 if state == 'cooldown' else state
        for j in range(32):
            for i in range(32):
                cv.px(x + 2 + i, y + 2 + j, shared.sweep.d[(step * 32 + j) * 32 + i])


def contact_sheet(out, shared):
    pad, top = 12, 22
    sheet = Image.new('RGBA', (W * 6 + pad * 3, (H * 4 + top + pad) * len(KITS) + pad), (24, 24, 30, 255))
    d = ImageDraw.Draw(sheet)
    for r, k in enumerate(KITS):
        cv, labels = scene(k, out[k.id], shared)
        y = pad + r * (H * 4 + top + pad)
        d.text((pad, y), '%s    4x, 2x, 1x    the wheel of the mockup, then each new sprite on the band, the lit band and the inner ring' % k.name, fill=(240, 240, 245, 255))
        one = cv.image()
        x = pad
        for z, yy in ((4, y + top), (2, y + top), (1, y + top + H * 2 + pad)):
            if z == 2:
                x = pad * 2 + W * 4
            sheet.alpha_composite(one.resize((W * z, H * z), Image.NEAREST), (x, yy))
        ink = (40, 30, 20, 255) if k.id == 'grimoire' else (200, 210, 220, 255)
        for lx, ly, text in labels:
            d.text((pad + lx * 4, y + top + ly * 4), text, fill=(240, 240, 245, 255) if ly > 261 else ink)
    path = os.path.join(HERE, 'contact-sheet.png')
    sheet.convert('RGB').save(path, optimize=True)
    return path


if __name__ == '__main__':
    out, shared = build_all()
    build.check()
    rows = check(out, shared)
    build.write_manifest([])
    for theme, sprite, name, ratio in rows:
        print('| %s | %s | %s | %.1f |' % (theme, sprite, name, ratio))
    print(contact_sheet(out, shared))
