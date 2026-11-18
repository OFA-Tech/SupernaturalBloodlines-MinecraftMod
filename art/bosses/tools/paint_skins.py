"""Paints the boss/NPC player skins (64x64). Original designs, no actor likenesses.
Run: python paint_skins.py <out_dir> [name ...]"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skin_lib import Skin, shade, mixc, hsh, SIDES

WHITE = (238, 238, 240)


# ------------------------------------------------------------------ shared pieces
def head(s, skin, hair, iris, brow=None, lips=None, hair_rows=2, fringe=0.5, side_rows=3, back_rows=6,
         sideburn=True, white=WHITE, hair_amp=0.12):
    brow = brow or shade(hair, 0.8)
    lips = lips or mixc(skin, (170, 80, 80), 0.45)
    s.fill_part('head', skin, 0.025, 1, skin=True)
    # eyes, brows, nose, mouth
    s.set('head', 'front', 1, 4, white, skin=False); s.set('head', 'front', 2, 4, iris)
    s.set('head', 'front', 5, 4, iris); s.set('head', 'front', 6, 4, white)
    for lx in (1, 2, 5, 6):
        s.set('head', 'front', lx, 3, brow)
    s.set('head', 'front', 3, 5, shade(skin, 0.9), skin=True); s.set('head', 'front', 4, 5, shade(skin, 0.94), skin=True)
    s.set('head', 'front', 3, 6, lips); s.set('head', 'front', 4, 6, lips)
    for side in ('right', 'left'):
        for lx in (3, 4):
            for ly in (4, 5):
                s.set('head', side, lx, ly, shade(skin, 0.9), skin=True)
    # hair
    if hair is None:
        return
    s.noisy('head', 'top', 0, 0, 8, 8, hair, hair_amp, 3)
    s.noisy('head', 'front', 0, 0, 8, hair_rows, hair, hair_amp, 4)
    for lx in range(8):
        if hsh(lx, 77, s.seed) < fringe:
            s.set('head', 'front', lx, hair_rows, shade(hair, 1.0 + (hsh(lx, 5, 9) - .5) * .2))
    for side in ('right', 'left'):
        s.noisy('head', side, 0, 0, 8, side_rows - 1, hair, hair_amp, 5)
        xs = range(0, 6) if side == 'right' else range(2, 8)
        for lx in xs:
            s.set('head', side, lx, side_rows - 1, shade(hair, 1 + (hsh(lx, 8, s.seed) - .5) * .2))
        if sideburn:
            for lx in ((6, 7) if side == 'right' else (0, 1)):
                for ly in (side_rows - 1, side_rows):
                    s.set('head', side, lx, ly, shade(hair, 0.95))
    s.noisy('head', 'back', 0, 0, 8, back_rows, hair, hair_amp, 6)


def hat_hair(s, hair, p=0.85, back_rows=5, side_rows=3, front_rows=1, top=True, seed=1):
    def sp(side, lx, ly, f=1.0):
        if hsh(lx + 9, ly + 3, s.seed + seed + len(side)) < p:
            s.set('hat', side, lx, ly, shade(hair, f * (0.9 + 0.25 * hsh(lx, ly, seed + 5))))
    if top:
        for lx in range(8):
            for ly in range(8):
                sp('top', lx, ly)
    for ly in range(front_rows):
        for lx in range(8):
            sp('front', lx, ly, 1.05)
    for side in ('right', 'left'):
        for ly in range(side_rows):
            for lx in range(8):
                sp(side, lx, ly)
    for ly in range(back_rows):
        for lx in range(8):
            sp('back', lx, ly)


def beard(s, c, rows=(5, 8), mouth=True, hat=True):
    r0, r1 = rows
    for ly in range(r0, r1):
        for lx in range(8):
            if ly == r0 and lx in (2, 3, 4, 5):
                continue
            s.set('head', 'front', lx, ly, shade(c, 0.92 + 0.2 * hsh(lx, ly, s.seed + 31)))
    if mouth:
        s.set('head', 'front', 3, r0 + 1, (116, 66, 62)); s.set('head', 'front', 4, r0 + 1, (116, 66, 62))
    for ly in range(r0 - 1, r1):
        for lx in (4, 5, 6, 7):
            s.set('head', 'right', lx, ly, shade(c, 0.95 + 0.15 * hsh(lx, ly, s.seed + 32)))
        for lx in (0, 1, 2, 3):
            s.set('head', 'left', lx, ly, shade(c, 0.95 + 0.15 * hsh(lx, ly, s.seed + 33)))
    s.noisy('head', 'bottom', 0, 0, 8, 8, c, 0.1, 40)
    if hat:
        for ly in range(r0 + 1, r1):
            for lx in range(1, 7):
                if hsh(lx, ly, s.seed + 34) < 0.8:
                    s.set('hat', 'front', lx, ly, shade(c, 0.85 + 0.2 * hsh(lx, ly, 7)))
        for ly in range(r0 + 1, r1):
            for lx in (5, 6, 7):
                s.set('hat', 'right', lx, ly, shade(c, 0.9))
            for lx in (0, 1, 2):
                s.set('hat', 'left', lx, ly, shade(c, 0.9))


def stubble(s, skin, strength=0.5, rows=(5, 8)):
    c = shade(skin, 0.72)
    for ly in range(*rows):
        for lx in range(8):
            if (ly == 5 and lx in (3, 4)) or (ly == 6 and lx in (3, 4)):
                continue
            if hsh(lx, ly, s.seed + 55) < strength:
                s.set('head', 'front', lx, ly, mixc(skin, c, 0.6), skin=True)
    for ly in range(5, 8):
        for lx in (5, 6, 7):
            if hsh(lx, ly, s.seed + 56) < strength:
                s.set('head', 'right', lx, ly, mixc(skin, c, 0.6), skin=True)
        for lx in (0, 1, 2):
            if hsh(lx, ly, s.seed + 57) < strength:
                s.set('head', 'left', lx, ly, mixc(skin, c, 0.6), skin=True)


def arms_skin(s, skin, from_row=11):
    for part in ('rarm', 'larm'):
        s.fill_part(part, skin, 0.025, 2, skin=True)


def legs_cloth(s, c, rows=(0, 12), amp=0.06, seed=3, weave=0):
    for part in ('rleg', 'lleg'):
        for side in ('right', 'front', 'left', 'back'):
            fx, fy, fw, fh = s.face(part, side)
            s.noisy(part, side, 0, rows[0], fw, rows[1] - rows[0], c, amp, seed, weave=weave)
        s.noisy(part, 'top', 0, 0, 4, 4, c, amp, seed)
        s.noisy(part, 'bottom', 0, 0, 4, 4, shade(c, 0.6), amp, seed)


def boots(s, c, rows=(10, 12), sole=(20, 18, 18)):
    for part in ('rleg', 'lleg'):
        for side in SIDES:
            fx, fy, fw, fh = s.face(part, side)
            s.noisy(part, side, 0, rows[0], fw, rows[1] - rows[0], c, 0.08, 9)
            s.rect(part, side, 0, 11, fw, 1, sole)
        s.noisy(part, 'bottom', 0, 0, 4, 4, sole, 0.04, 9)


def body_cloth(s, c, rows=(0, 12), amp=0.06, seed=2, weave=0):
    for side in SIDES:
        fx, fy, fw, fh = s.face('body', side)
        s.noisy('body', side, 0, rows[0], fw, rows[1] - rows[0], c, amp, seed, weave=weave)
    s.noisy('body', 'top', 0, 0, 8, 4, c, amp, seed)
    s.noisy('body', 'bottom', 0, 0, 8, 4, shade(c, 0.6), amp, seed)


def sleeves_base(s, c, rows=(0, 12), amp=0.06, seed=4, weave=0):
    for part in ('rarm', 'larm'):
        for side in SIDES:
            fx, fy, fw, fh = s.face(part, side)
            s.noisy(part, side, 0, rows[0], fw, rows[1] - rows[0], c, amp, seed, weave=weave)
        fw = s.face(part, 'top')[2]
        s.noisy(part, 'top', 0, 0, fw, 4, c, amp, seed)


def overlay(s, part, c, rows=(0, 12), amp=0.06, seed=5, sides=SIDES, weave=0, p=1.0):
    for side in sides:
        fx, fy, fw, fh = s.face(part, side)
        for ly in range(rows[0], rows[1]):
            for lx in range(fw):
                if p >= 1.0 or hsh(lx + 3, ly + 5, s.seed + seed) < p:
                    n = (hsh(fx + lx, fy + ly, s.seed + seed) - 0.5) * 2 * amp + (weave * (0.03 if (lx + ly) % 2 else -0.03))
                    s.set(part, side, lx, ly, shade(c, 1 + n))


def button_col(s, part, x, rows, c, side='front', step=2):
    for r in rows[::step]:
        s.set(part, side, x, r, c)


def hand_rows(s, skin, rows=(10, 12)):
    """Bare hands at the bottom of the arms (base layer only)."""
    for part in ('rarm', 'larm'):
        for side in SIDES:
            fx, fy, fw, fh = s.face(part, side)
            s.noisy(part, side, 0, rows[0], fw, rows[1] - rows[0], skin, 0.025, 2, skin=True)
        fw = s.face(part, 'bottom')[2]
        s.noisy(part, 'bottom', 0, 0, fw, 4, skin, 0.025, 2, skin=True)


# ------------------------------------------------------------------ characters
def mikael():
    s = Skin(False, 101)
    SK, HAIR, BRD = (224, 198, 178), (56, 40, 30), (64, 46, 34)
    head(s, SK, HAIR, (112, 150, 176), brow=(44, 32, 26), hair_rows=2, fringe=0.4, back_rows=8, side_rows=4)
    beard(s, BRD, rows=(5, 8))
    s.set('head', 'front', 5, 2, (196, 150, 140), skin=True); s.set('head', 'front', 5, 3, (190, 140, 130), skin=True)  # old scar over the brow
    hat_hair(s, HAIR, 0.75, back_rows=8, side_rows=4, front_rows=1)
    # braid tied low on the back of the head
    for ly in range(5, 8):
        s.set('hat', 'back', 3, ly, shade(HAIR, 1.2 if ly % 2 else 0.9)); s.set('hat', 'back', 4, ly, shade(HAIR, 0.9 if ly % 2 else 1.2))
    s.set('hat', 'back', 3, 4, (150, 118, 80)); s.set('hat', 'back', 4, 4, (150, 118, 80))
    body_cloth(s, (150, 140, 122), amp=0.07, seed=7)  # linen shirt
    arms_skin(s, SK); sleeves_base(s, (150, 140, 122), rows=(0, 10), seed=8); hand_rows(s, SK, (10, 12))
    legs_cloth(s, (48, 44, 40), seed=9); boots(s, (34, 26, 20), rows=(8, 12))
    # leather jerkin
    LEATHER = (58, 40, 30)
    overlay(s, 'jacket', LEATHER, rows=(1, 12), amp=0.1, seed=11)
    for lx in (3, 4):
        for ly in range(0, 3):
            s.clear('jacket', 'front', lx, ly)
    for ly in range(3, 9, 2):
        s.set('jacket', 'front', 3, ly, (156, 124, 84)); s.set('jacket', 'front', 4, ly, (156, 124, 84))
        s.set('jacket', 'front', 3, ly + 1, (110, 84, 56)); s.set('jacket', 'front', 4, ly + 1, (110, 84, 56))
    for lx in (0, 7):  # darker seam
        for ly in range(1, 9):
            s.set('jacket', 'front', lx, ly, shade(LEATHER, 0.78))
    # fur mantle
    FUR = (128, 114, 98)
    overlay(s, 'jacket', FUR, rows=(0, 2), amp=0.2, seed=12)
    overlay(s, 'rsleeve', FUR, rows=(0, 3), amp=0.2, seed=13); overlay(s, 'lsleeve', FUR, rows=(0, 3), amp=0.2, seed=14)
    for lx in range(8):
        if hsh(lx, 2, 15) < 0.5:
            s.set('jacket', 'front', lx, 2, shade(FUR, 0.85))
        if hsh(lx, 3, 16) < 0.5:
            s.set('jacket', 'back', lx, 2, shade(FUR, 0.85))
    # bracers
    overlay(s, 'rsleeve', (96, 70, 46), rows=(6, 10), amp=0.08, seed=17)
    for r in (6, 9):
        s.hband('rsleeve', r, (60, 42, 30))
    overlay(s, 'lsleeve', (96, 70, 46), rows=(6, 10), amp=0.08, seed=18)
    for r in (6, 9):
        s.hband('lsleeve', r, (60, 42, 30))
    # belt, buckle
    for side in SIDES:
        s.rect('jacket', side, 0, 9, s.face('jacket', side)[2], 2, (30, 22, 18))
    s.rect('jacket', 'front', 3, 9, 2, 2, (190, 154, 70)); s.rect('jacket', 'front', 4, 10, 1, 1, (120, 94, 40))
    # white oak stake hanging on the left hip, point down onto the thigh
    OAK, OAKD = (232, 224, 200), (176, 164, 134)
    s.rect('jacket', 'front', 6, 8, 1, 4, OAK); s.rect('jacket', 'front', 6, 8, 1, 1, (70, 50, 36))
    s.rect('jacket', 'left', 0, 8, 2, 4, OAK); s.rect('jacket', 'left', 0, 8, 2, 1, (70, 50, 36)); s.rect('jacket', 'left', 1, 9, 1, 3, OAKD)
    s.rect('lpants', 'front', 3, 0, 1, 5, OAK); s.set('lpants', 'front', 3, 5, OAKD); s.set('lpants', 'front', 3, 6, (120, 108, 86))
    s.rect('lpants', 'right', 0, 0, 1, 4, OAKD)
    # leg wraps, boot cuffs
    for part in ('rpants', 'lpants'):
        for i in range(4):
            s.set(part, 'front', i, 3 + i, (84, 62, 42)); s.set(part, 'front', 3 - i, 3 + i, (70, 52, 36))
        for side in ('right', 'left', 'back'):
            s.hband(part, 4, (84, 62, 42), sides=(side,)); s.hband(part, 7, (84, 62, 42), sides=(side,))
        overlay(s, part, (116, 104, 90), rows=(8, 9), amp=0.2, seed=19)
    return s


def klaus():
    s = Skin(False, 202)
    SK, HAIR = (232, 202, 178), (176, 140, 86)
    head(s, SK, HAIR, (104, 140, 172), brow=(140, 108, 66), hair_rows=2, fringe=0.65, back_rows=4, side_rows=3, hair_amp=0.16)
    stubble(s, SK, 0.45)
    s.set('head', 'front', 4, 6, (190, 100, 96)); s.set('head', 'front', 5, 5, shade(SK, 0.88), skin=True)  # lopsided smirk
    hat_hair(s, HAIR, 0.7, back_rows=3, side_rows=2, front_rows=2, seed=4)
    H = (98, 106, 86)  # henley, muted olive
    body_cloth(s, H, amp=0.07, seed=21)
    for ly in range(0, 4):
        s.set('body', 'front', 3, ly, shade(H, 0.7)); s.set('body', 'front', 4, ly, shade(H, 0.7))
        s.set('body', 'front', 3 if ly % 2 == 0 else 4, ly, (212, 204, 182))
    s.hband('body', 0, shade(H, 0.8), sides=('front',), x0=2, x1=6)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 6, H, 0.07, 22)
        s.noisy(part, 'top', 0, 0, 4, 4, H, 0.07, 22)
        s.hband(part, 5, shade(H, 0.75))
    legs_cloth(s, (42, 52, 78), seed=23); boots(s, (86, 60, 40), rows=(9, 12))
    # layered necklaces on the second layer
    SIL, GOLD, CORD = (190, 196, 206), (206, 168, 72), (90, 62, 44)
    s.line('jacket', 'front', [(2, 0), (5, 0), (2, 1), (5, 1), (3, 2), (4, 2)], SIL)
    s.line('jacket', 'front', [(1, 0), (6, 0), (1, 1), (6, 1), (1, 2), (6, 2), (2, 3), (5, 3), (3, 4), (4, 4)], CORD)
    s.line('jacket', 'front', [(2, 2), (5, 2), (2, 4), (5, 4), (3, 5), (4, 5)], GOLD)
    s.rect('jacket', 'front', 3, 3, 2, 1, GOLD); s.set('jacket', 'front', 3, 6, (150, 40, 44)); s.set('jacket', 'front', 4, 6, (150, 40, 44))
    for lx, c in ((0, SIL), (1, CORD), (2, GOLD), (3, SIL)):
        s.set('jacket', 'back', 0, 0, SIL)
    for side in ('right', 'left'):
        s.hband('jacket', 0, SIL, sides=(side,), x0=1, x1=3)
        s.hband('jacket', 1, CORD, sides=(side,), x0=1, x1=3)
    s.hband('jacket', 0, CORD, sides=('back',), x0=1, x1=7)
    # belt
    for side in SIDES:
        s.rect('jacket', side, 0, 10, s.face('jacket', side)[2], 1, (60, 40, 28))
    s.set('jacket', 'front', 3, 10, (180, 150, 80)); s.set('jacket', 'front', 4, 10, (180, 150, 80))
    # rolled-sleeve cuffs and paint smudges (artist)
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, H, rows=(5, 7), amp=0.08, seed=24)
        s.hband(part, 6, shade(H, 0.65))
    for (lx, ly, c) in ((1, 8, (160, 34, 40)), (2, 9, (160, 34, 40)), (0, 10, (196, 146, 52)), (2, 11, (196, 146, 52)), (3, 7, (40, 90, 140))):
        s.set('rarm', 'front', lx, ly, c); s.set('rarm', 'right', 1 + lx % 2, ly, c)
    for (lx, ly, c) in ((1, 10, (196, 146, 52)), (0, 9, (160, 34, 40))):
        s.set('larm', 'front', lx, ly, c)
    for (lx, ly, c) in ((2, 3, (160, 34, 40)), (3, 4, (160, 34, 40)), (1, 5, (196, 146, 52))):
        s.set('lpants', 'front', lx, ly, c, 220)
    s.rect('rpants', 'front', 0, 8, 4, 1, (120, 88, 60)); s.rect('lpants', 'front', 0, 8, 4, 1, (120, 88, 60))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (62, 74, 104), rows=(0, 2), amp=0.1, seed=25, p=0.7)
    return s


def elijah():
    s = Skin(False, 303)
    SK, HAIR = (222, 190, 164), (42, 30, 24)
    head(s, SK, HAIR, (92, 62, 40), brow=(34, 24, 20), hair_rows=1, fringe=0.3, back_rows=5, side_rows=3, sideburn=True, hair_amp=0.08)
    s.set('head', 'front', 5, 1, shade(HAIR, 1.3))  # side parting
    hat_hair(s, HAIR, 0.55, back_rows=3, side_rows=2, front_rows=1, seed=2)
    SUIT, SUITL = (38, 40, 48), (52, 54, 64)
    SHIRT = (234, 234, 238)
    body_cloth(s, SHIRT, amp=0.03, seed=31)
    s.rect('body', 'front', 3, 0, 2, 12, (36, 24, 32))  # tie
    s.rect('body', 'front', 3, 0, 2, 1, (60, 40, 52)); s.rect('body', 'front', 4, 5, 1, 7, (30, 20, 28))
    s.rect('body', 'front', 3, 8, 2, 2, (36, 24, 32)); s.rect('body', 'front', 3, 10, 2, 2, (36, 24, 32))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 11, SUIT, 0.05, 32)
            s.rect(part, side, 0, 10, s.face(part, side)[2], 1, SHIRT)
        s.noisy(part, 'top', 0, 0, 4, 4, SUIT, 0.05, 32)
    legs_cloth(s, (34, 36, 44), seed=33, weave=1); boots(s, (22, 22, 26), rows=(10, 12), sole=(12, 12, 14))
    # jacket with lapels, pinstripe
    overlay(s, 'jacket', SUIT, rows=(0, 12), amp=0.04, seed=34)
    for side in ('front', 'back', 'right', 'left'):
        w = s.face('jacket', side)[2]
        for lx in range(0, w, 2):
            for ly in range(0, 12):
                if s.px[s.face('jacket', side)[0] + lx, s.face('jacket', side)[1] + ly][3] > 0:
                    s.set('jacket', side, lx, ly, shade(SUITL, 1.0))
    for ly in range(0, 12):  # open front, shirt shows through
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly) if ly < 9 else None
    for i in range(7):  # lapels
        s.set('jacket', 'front', 2 - (i // 4), i, (62, 64, 76)); s.set('jacket', 'front', 5 + (i // 4), i, (62, 64, 76))
        s.set('jacket', 'front', 2, i, (28, 30, 36)) if i > 3 else None
    s.rect('jacket', 'front', 1, 0, 1, 7, (30, 32, 38)); s.rect('jacket', 'front', 6, 0, 1, 7, (30, 32, 38))
    s.set('jacket', 'front', 6, 3, (240, 240, 244)); s.set('jacket', 'front', 6, 2, (220, 222, 230))  # pocket square
    s.rect('jacket', 'front', 3, 9, 2, 1, (24, 24, 30)); s.set('jacket', 'front', 3, 9, (160, 160, 170))  # button
    s.rect('jacket', 'back', 3, 8, 2, 4, (30, 32, 38))  # vent
    for part, base in (('rsleeve', SUIT), ('lsleeve', SUIT)):
        overlay(s, part, base, rows=(0, 10), amp=0.04, seed=35)
        s.set(part, 'front', 1, 9, (160, 160, 170)); s.set(part, 'left', 1, 9, (160, 160, 170))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (34, 36, 44), rows=(0, 3), amp=0.05, seed=36)
        s.hband(part, 11, (12, 12, 14))
    return s


def kol():
    s = Skin(False, 404)
    SK, HAIR = (226, 192, 166), (38, 26, 22)
    head(s, SK, HAIR, (86, 60, 40), brow=(30, 22, 18), hair_rows=1, fringe=0.9, back_rows=4, side_rows=2, hair_amp=0.16)
    stubble(s, SK, 0.3)
    s.set('head', 'front', 4, 6, (190, 100, 96)); s.set('head', 'front', 5, 6, (190, 100, 96)); s.set('head', 'front', 5, 5, shade(SK, 0.88), skin=True)
    # newsboy cap on the hat layer
    CAP, CAPD = (96, 88, 74), (70, 64, 54)
    for side in ('top', 'back', 'right', 'left'):
        rows = range(0, 8) if side == 'top' else range(0, 3)
        for ly in rows:
            for lx in range(8):
                s.set('hat', side, lx, ly, shade(CAP if (lx + ly) % 4 else CAPD, 1 + (hsh(lx, ly, 9) - .5) * .14))
    for lx in range(8):
        s.set('hat', 'front', lx, 0, shade(CAP, 1.05)); s.set('hat', 'front', lx, 1, CAPD)
    for lx in range(1, 7):
        s.set('hat', 'front', lx, 2, (50, 44, 36))  # brim
    s.set('hat', 'top', 3, 3, (130, 120, 100)); s.set('hat', 'top', 4, 3, (130, 120, 100))  # cap button
    for lx in (0, 7):
        s.set('hat', 'back', lx, 3, shade(HAIR, 1.1))
    # shirt, waistcoat
    SHIRT, VEST, TIE = (214, 204, 178), (66, 30, 36), (138, 30, 36)
    body_cloth(s, SHIRT, amp=0.05, seed=41)
    s.set('body', 'front', 3, 0, SK, skin=True); s.set('body', 'front', 4, 0, SK, skin=True); s.set('body', 'front', 3, 1, shade(SK, 0.9), skin=True)
    s.line('body', 'front', [(3, 1), (4, 1), (4, 2), (4, 3), (3, 4)], TIE)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 6, SHIRT, 0.05, 42)
            s.rect(part, side, 0, 5, s.face(part, side)[2], 1, shade(SHIRT, 0.82))
        s.noisy(part, 'top', 0, 0, 4, 4, SHIRT, 0.05, 42)
    legs_cloth(s, (92, 76, 62), seed=43, weave=1); boots(s, (44, 30, 22), rows=(9, 12))
    overlay(s, 'jacket', VEST, rows=(2, 11), amp=0.06, seed=44)
    for lx in (3, 4):
        for ly in range(2, 5):
            pass
    s.clear('jacket', 'front', 3, 2); s.clear('jacket', 'front', 4, 2); s.clear('jacket', 'front', 3, 3); s.clear('jacket', 'front', 4, 3)
    s.rect('jacket', 'front', 3, 4, 2, 7, shade(VEST, 1.2))
    for ly in (5, 7, 9):
        s.set('jacket', 'front', 3, ly, (200, 170, 90))
    s.rect('jacket', 'front', 0, 2, 1, 3, (30, 22, 18)); s.rect('jacket', 'front', 7, 2, 1, 3, (30, 22, 18))  # braces/suspenders
    for lx in (1, 6):
        for ly in range(0, 3):
            s.set('jacket', 'front', lx, ly, (30, 22, 18))
    for ly in range(0, 11):
        s.set('jacket', 'back', 1, ly, (30, 22, 18)); s.set('jacket', 'back', 6, ly, (30, 22, 18))
    s.rect('jacket', 'front', 5, 8, 2, 2, (30, 22, 18))  # watch pocket / knife sheath
    s.rect('jacket', 'front', 5, 10, 1, 2, (110, 100, 90)); s.set('jacket', 'front', 5, 10, (70, 50, 36))
    s.rect('jacket', 'right', 2, 9, 1, 3, (90, 66, 40))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, SHIRT, rows=(0, 6), amp=0.06, seed=45, p=0.0)
        s.rect(part, 'front', 0, 5, 4, 1, shade(SHIRT, 0.95)); s.rect(part, 'back', 0, 5, 4, 1, shade(SHIRT, 0.95))
        s.rect(part, 'right', 0, 5, 4, 1, shade(SHIRT, 0.95)); s.rect(part, 'left', 0, 5, 4, 1, shade(SHIRT, 0.95))
    s.set('rsleeve', 'front', 1, 3, (0, 0, 0), 0)
    for part in ('rpants', 'lpants'):
        overlay(s, part, (92, 76, 62), rows=(0, 3), amp=0.08, seed=46, p=0.8)
        s.hband(part, 9, (30, 22, 18), x0=0)
    s.set('lpants', 'front', 3, 4, (60, 40, 30)); s.set('lpants', 'front', 3, 5, (60, 40, 30))
    return s


def rebekah():
    s = Skin(True, 505)
    SK, HAIR = (240, 208, 186), (206, 168, 92)
    head(s, SK, HAIR, (112, 156, 200), brow=(170, 134, 76), lips=(196, 98, 108), hair_rows=2, fringe=0.55, back_rows=8, side_rows=6, sideburn=False, hair_amp=0.14)
    for side in ('right', 'left'):
        for ly in range(3, 8):
            for lx in (range(0, 4) if side == 'right' else range(4, 8)):
                s.set('head', side, lx, ly, shade(HAIR, 0.85 + 0.25 * hsh(lx, ly, 8)))
    # ear + long, wavy hair on the hat layer, spilling over shoulders on the jacket layer
    hat_hair(s, HAIR, 0.9, back_rows=8, side_rows=6, front_rows=2, seed=5)
    for ly in range(2, 8):
        for lx in (0, 1) if True else ():
            s.set('hat', 'front', lx, ly, shade(HAIR, 0.9 + 0.2 * hsh(lx, ly, 3))) if lx == 0 or ly > 3 else None
    for ly in range(2, 5):
        s.set('hat', 'front', 7, ly, shade(HAIR, 1.0)); s.set('hat', 'front', 0, ly, shade(HAIR, 1.0))
    DRESS, DRESSD, LACE = (70, 94, 140), (48, 66, 108), (240, 236, 226)
    body_cloth(s, DRESS, amp=0.06, seed=51)
    s.rect('body', 'front', 2, 0, 4, 2, SK, skin=True); s.rect('body', 'front', 3, 2, 2, 1, SK, skin=True)  # sweetheart neckline
    for lx in (1, 6):
        s.set('body', 'front', lx, 1, LACE)
    s.line('body', 'front', [(2, 2), (3, 3), (4, 3), (5, 2)], LACE)
    s.rect('body', 'front', 0, 7, 8, 2, DRESSD)  # sash
    s.set('body', 'front', 3, 7, (220, 190, 110)); s.set('body', 'front', 4, 7, (220, 190, 110)); s.set('body', 'front', 3, 8, (220, 190, 110)); s.set('body', 'front', 4, 8, (150, 40, 60))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            fw = s.face(part, side)[2]
            s.noisy(part, side, 0, 0, fw, 7, LACE, 0.04, 52, weave=1)
            s.noisy(part, side, 0, 7, fw, 1, shade(DRESS, 1.1), 0.04, 52)
        s.noisy(part, 'top', 0, 0, 3, 4, DRESS, 0.05, 52)
        s.set(part, 'front', 1, 9, (220, 190, 110)); s.set(part, 'right', 1, 9, (220, 190, 110))  # bracelet
    legs_cloth(s, SK, seed=53); 
    for part in ('rleg', 'lleg'):
        s.fill_part(part, SK, 0.02, 2, skin=True)
        for side in SIDES:
            s.noisy(part, side, 0, 10, 4, 2, (214, 200, 180), 0.04, 54)
    overlay(s, 'jacket', DRESS, rows=(0, 12), amp=0.05, seed=55)
    for lx in range(1, 7):
        s.clear('jacket', 'front', lx, 0); 
    s.rect('jacket', 'front', 2, 0, 4, 2, (0, 0, 0), 0); s.clear('jacket', 'front', 3, 2); s.clear('jacket', 'front', 4, 2)
    s.rect('jacket', 'front', 0, 7, 8, 2, DRESSD)
    s.set('jacket', 'front', 3, 7, (220, 190, 110)); s.set('jacket', 'front', 4, 7, (220, 190, 110))
    for ly in range(0, 9):
        s.set('jacket', 'back', 3 if ly % 2 else 4, ly, (60, 80, 124))
    # hair cascading down the back over the dress
    for ly in range(0, 9):
        for lx in range(1, 7):
            if hsh(lx, ly, 5) < 0.95 - ly * 0.03:
                s.set('jacket', 'back', lx, ly, shade(HAIR, 0.85 + 0.3 * hsh(lx, ly, 6)))
    for side in ('right', 'left'):
        for ly in range(0, 6):
            for lx in ((0, 1) if side == 'right' else (2, 3)):
                s.set('jacket', side, lx, ly, shade(HAIR, 0.9 + 0.25 * hsh(lx, ly, 7)))
    for lx in (0, 1, 6, 7):
        for ly in range(0, 3):
            s.set('jacket', 'front', lx, ly, shade(HAIR, 0.95 + 0.2 * hsh(lx, ly, 8)))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, LACE, rows=(0, 7), amp=0.03, seed=56, weave=1)
        for lx in range(3):
            s.set(part, 'front', lx, 6, shade(LACE, 0.8) if lx % 2 else (255, 255, 255))
    # full-length skirt on the leg layer
    for part in ('rpants', 'lpants'):
        overlay(s, part, DRESS, rows=(0, 12), amp=0.05, seed=57, sides=SIDES)
        for side in SIDES:
            fw = s.face(part, side)[2]
            for lx in range(fw):
                if lx % 2 == 0:
                    for ly in range(1, 12):
                        s.set(part, side, lx, ly, shade(DRESS, 0.9 if ly % 3 else 0.85))
            s.rect(part, side, 0, 11, fw, 1, LACE)
            s.rect(part, side, 0, 10, fw, 1, shade(DRESSD, 1.1))
        s.noisy(part, 'bottom', 0, 0, 4, 4, DRESSD, 0.05, 58)
    return s


def finn():
    s = Skin(False, 606)
    SK, HAIR, BRD = (218, 190, 166), (92, 68, 46), (88, 64, 44)
    head(s, SK, HAIR, (110, 132, 112), brow=(70, 52, 36), hair_rows=2, fringe=0.55, back_rows=7, side_rows=5, hair_amp=0.14)
    beard(s, BRD, rows=(6, 8), mouth=False, hat=False)
    for lx in (3, 4):
        s.set('head', 'front', lx, 6, (150, 98, 88))
    for (x, y) in ((3, 1), (4, 1)):  # faint woad rune on the forehead
        s.set('head', 'front', x, y, (96, 128, 150))
    s.set('head', 'front', 3, 2, (96, 128, 150))
    hat_hair(s, HAIR, 0.8, back_rows=6, side_rows=4, front_rows=1, seed=6)
    for ly in range(3, 5):
        s.set('hat', 'back', 3, ly, (120, 104, 82)); s.set('hat', 'back', 4, ly, (120, 104, 82))  # leather tie
    WOOL, WOOLD = (140, 128, 106), (116, 104, 84)
    body_cloth(s, WOOL, amp=0.1, seed=61, weave=1)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 10, WOOL, 0.1, 62, weave=1)
            s.rect(part, side, 0, 9, s.face(part, side)[2], 1, WOOLD)
        s.noisy(part, 'top', 0, 0, 4, 4, WOOL, 0.1, 62)
    legs_cloth(s, (96, 84, 66), seed=63, weave=1); boots(s, (72, 52, 36), rows=(9, 12), sole=(36, 28, 22))
    overlay(s, 'jacket', WOOL, rows=(0, 12), amp=0.09, seed=64, weave=1)
    for lx in (3, 4):
        for ly in range(0, 2):
            s.clear('jacket', 'front', lx, ly)
    s.hband('jacket', 11, WOOLD)  # hem
    for side in SIDES:
        s.rect('jacket', side, 0, 8, s.face('jacket', side)[2], 1, (164, 140, 96)); s.rect('jacket', side, 0, 9, s.face('jacket', side)[2], 1, (132, 110, 76))
    s.line('jacket', 'front', [(3, 10), (4, 11), (3, 12 - 1)], (164, 140, 96)); s.line('jacket', 'front', [(4, 9), (4, 10), (5, 11)], (132, 110, 76))
    # bone amulet on a cord
    s.line('jacket', 'front', [(2, 0), (5, 0), (2, 1), (5, 1), (3, 2), (4, 2)], (70, 54, 40))
    s.rect('jacket', 'front', 3, 3, 2, 2, (226, 218, 196)); s.set('jacket', 'front', 4, 4, (96, 150, 130)); s.set('jacket', 'front', 3, 3, (60, 90, 100))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, WOOL, rows=(0, 4), amp=0.1, seed=65, weave=1, p=0.9)
        s.hband(part, 9, WOOLD)
    # woad rune marks on the forearm (second layer, partly transparent)
    for (x, y) in ((1, 7), (2, 7), (1, 8), (1, 9), (2, 10)):
        s.set('rarm', 'front', x, y, (88, 120, 146))
    for part in ('rpants', 'lpants'):
        for i in range(4):
            s.set(part, 'front', 0, 5 + i, (150, 124, 84)); s.set(part, 'front', 3, 5 + i, (150, 124, 84)); s.set(part, 'front', 1 + (i % 2), 5 + i, (126, 104, 70))
        for side in ('right', 'left', 'back'):
            s.hband(part, 5, (130, 108, 72), sides=(side,)); s.hband(part, 8, (130, 108, 72), sides=(side,))
        overlay(s, part, (96, 84, 66), rows=(0, 4), amp=0.1, seed=66, p=0.6)
    return s


def deucalion_human():
    s = Skin(False, 707)
    SK, HAIR = (196, 158, 128), (172, 172, 178)
    head(s, SK, HAIR, (60, 60, 66), brow=(120, 120, 126), hair_rows=1, fringe=0.4, back_rows=5, side_rows=3, hair_amp=0.1)
    # dark round glasses, wrapping onto the temples
    LENS, FRAME = (10, 10, 14), (40, 40, 48)
    for lx in (1, 2, 5, 6):
        for ly in (3, 4, 5):
            s.set('head', 'front', lx, ly, LENS)
    for lx in (1, 6):
        s.set('head', 'front', lx, 3, FRAME); s.set('head', 'front', lx, 5, FRAME)
    s.set('head', 'front', 3, 3, FRAME); s.set('head', 'front', 4, 3, FRAME)
    s.set('head', 'front', 2, 3, LENS); s.set('head', 'front', 5, 3, LENS)
    s.set('head', 'front', 0, 3, FRAME); s.set('head', 'front', 7, 3, FRAME)
    for side in ('right', 'left'):
        for lx in range(0, 8):
            s.set('head', side, lx, 3, FRAME) if (lx >= 4 if side == 'right' else lx <= 3) else None
    # trimmed goatee and short grey beard
    for lx in (2, 3, 4, 5):
        s.set('head', 'front', lx, 7, shade(HAIR, 0.95)); 
    for lx in (3, 4):
        s.set('head', 'front', lx, 6, shade(HAIR, 0.85)); 
    for lx in (1, 2, 5, 6):
        s.set('head', 'front', lx, 6, shade(HAIR, 0.8)) if hsh(lx, 6, 3) < 0.5 else None
    s.set('hat', 'front', 3, 7, shade(HAIR, 0.8)); s.set('hat', 'front', 4, 7, shade(HAIR, 0.8))
    s.noisy('head', 'bottom', 2, 3, 4, 3, HAIR, 0.1, 71)
    # cane prop texture, free skin area (not used by the game model): shaft net at (56,16) 4x14, knob net at (56,32) 8x4
    for j in range(14):
        for i in range(4):
            s.px[56 + i, 16 + j] = (26 + 8 * (i % 2), 22, 24 + 8 * (i % 2), 255)
    for j in range(4):
        for i in range(8):
            s.px[56 + i, 32 + j] = (176 + 14 * ((i + j) % 2), 180 + 14 * ((i + j) % 2), 192 + 12 * ((i + j) % 2), 255)
    hat_hair(s, HAIR, 0.6, back_rows=2, side_rows=2, front_rows=1, seed=7)
    COAT, COATL = (30, 28, 34), (44, 42, 50)
    body_cloth(s, (58, 58, 66), amp=0.05, seed=72)
    s.rect('body', 'front', 2, 0, 4, 2, (232, 232, 236)); s.rect('body', 'front', 3, 1, 2, 4, (28, 28, 32))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 11, COAT, 0.05, 73)
        s.noisy(part, 'top', 0, 0, 4, 4, COAT, 0.05, 73)
        s.hband(part, 10, (232, 232, 236))
    legs_cloth(s, COAT, seed=74); boots(s, (18, 16, 18), rows=(9, 12), sole=(8, 8, 10))
    # long coat, double breasted, high collar
    overlay(s, 'jacket', COAT, rows=(0, 12), amp=0.05, seed=75)
    s.rect('jacket', 'front', 2, 3, 4, 9, COATL)
    s.rect('jacket', 'front', 3, 0, 2, 3, (0, 0, 0), 0)
    for ly in (3, 5, 7, 9):
        s.set('jacket', 'front', 2, ly, (170, 172, 182)); s.set('jacket', 'front', 5, ly, (170, 172, 182))
    s.set('jacket', 'front', 3, 3, shade(COAT, 0.6)); 
    for ly in range(0, 3):
        s.set('jacket', 'front', 2, ly, COATL); s.set('jacket', 'front', 5, ly, COATL)
    for side in SIDES:
        s.hband('jacket', 0, shade(COAT, 1.45), sides=(side,))
        s.hband('jacket', 1, shade(COAT, 1.2), sides=(side,))
    s.rect('jacket', 'back', 3, 6, 2, 6, shade(COAT, 0.7))  # back vent
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, COAT, rows=(0, 10), amp=0.05, seed=76)
        s.rect(part, 'front', 0, 9, 4, 1, COATL); s.set(part, 'front', 1, 9, (170, 172, 182))
    for part in ('rpants', 'lpants'):
        overlay(s, part, COAT, rows=(0, 10), amp=0.05, seed=77)
        for side in SIDES:
            s.rect(part, side, 0, 9, s.face(part, side)[2], 1, COATL)
    return s


def peter_hale(burned=False):
    s = Skin(False, 808 if not burned else 809)
    SK, HAIR = (226, 192, 168), (40, 30, 26)
    head(s, SK, HAIR, (92, 140, 148), brow=(32, 24, 20), hair_rows=1, fringe=0.6, back_rows=4, side_rows=3, hair_amp=0.12)
    stubble(s, SK, 0.55)
    s.set('head', 'front', 4, 6, (186, 98, 94)); s.set('head', 'front', 5, 6, (186, 98, 94)); s.set('head', 'front', 5, 5, shade(SK, 0.88), skin=True)
    hat_hair(s, HAIR, 0.65, back_rows=3, side_rows=2, front_rows=2, seed=8)
    HEN, JK = (66, 66, 74), (78, 50, 36)
    body_cloth(s, HEN, amp=0.07, seed=81)
    s.set('body', 'front', 3, 0, SK, skin=True); s.set('body', 'front', 4, 0, SK, skin=True); s.set('body', 'front', 3, 1, shade(SK, .92), skin=True); s.set('body', 'front', 4, 1, shade(SK, .92), skin=True)
    for ly in range(1, 5):
        s.set('body', 'front', 3 if ly == 1 else 4, ly, shade(HEN, 0.7))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 8, HEN, 0.07, 82)
        s.noisy(part, 'top', 0, 0, 4, 4, HEN, 0.07, 82)
        s.hband(part, 8, shade(HEN, 0.8))
    legs_cloth(s, (46, 54, 70), seed=83); boots(s, (30, 26, 24), rows=(9, 12))
    overlay(s, 'jacket', JK, rows=(0, 12), amp=0.09, seed=84)
    for ly in range(1, 12):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
    s.rect('jacket', 'front', 2, 0, 1, 3, shade(JK, 1.25)); s.rect('jacket', 'front', 5, 0, 1, 3, shade(JK, 1.25))  # collar
    s.rect('jacket', 'front', 0, 1, 1, 10, shade(JK, 0.78)); s.rect('jacket', 'front', 7, 1, 1, 10, shade(JK, 0.78))
    s.rect('jacket', 'front', 5, 6, 2, 1, shade(JK, 0.65)); s.rect('jacket', 'front', 1, 6, 2, 1, shade(JK, 0.65))  # pockets
    s.set('jacket', 'front', 6, 6, (170, 170, 176)); s.set('jacket', 'front', 1, 6, (170, 170, 176))
    s.hband('jacket', 11, shade(JK, 0.8))
    s.rect('jacket', 'back', 0, 3, 8, 1, shade(JK, 0.8))  # yoke seam
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, JK, rows=(0, 8), amp=0.09, seed=85)
        s.hband(part, 7, shade(JK, 0.78)); s.hband(part, 4, shade(JK, 0.88))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (46, 54, 70), rows=(0, 2), amp=0.06, seed=86)
        for side in SIDES:
            s.hband(part, 8, (56, 42, 34), sides=(side,))
        s.hband(part, 9, (30, 26, 24), sides=('front', 'back', 'right', 'left'))
    if burned:
        burn(s)
    return s


def burn(s):
    """Burn damage on top of a finished skin: scars on bare skin, charred and torn cloth, patchy hair."""
    SCAR, SCAR2, CHAR, ASH = (190, 104, 96), (156, 82, 78), (88, 54, 48), (26, 22, 22)
    def blob(part, side, cx, cy, r, col, only_skin=True, p=1.0):
        fx, fy, fw, fh = s.face(part, side)
        for j in range(-r, r + 1):
            for i in range(-r, r + 1):
                if i * i + j * j <= r * r + 1 and 0 <= cx + i < fw and 0 <= cy + j < fh:
                    pos = (fx + cx + i, fy + cy + j)
                    if (not only_skin or pos in s.skinmask) and s.px[pos][3] > 0 and hsh(pos[0], pos[1], 91) < p:
                        s.set(part, side, cx + i, cy + j, mixc(col, s.px[pos][:3], 0.2 * hsh(pos[0], pos[1], 92)), 255, True)
    # face: left half of the face (viewer's right) is ruined
    blob('head', 'front', 5, 3, 2, SCAR); blob('head', 'front', 6, 5, 2, SCAR2); blob('head', 'front', 5, 6, 1, CHAR)
    blob('head', 'front', 1, 6, 1, SCAR, p=0.8); blob('head', 'front', 2, 2, 1, SCAR2, p=0.9)
    blob('head', 'left', 3, 4, 2, SCAR); blob('head', 'left', 5, 6, 1, SCAR2); blob('head', 'bottom', 5, 4, 2, SCAR)
    # clouded eye on the scarred side
    s.set('head', 'front', 5, 4, (172, 180, 186)); s.set('head', 'front', 6, 4, (196, 200, 204))
    # hair burnt away in patches
    for side, xr in (('top', range(8)), ('front', range(8)), ('right', range(8)), ('left', range(8)), ('back', range(8))):
        for ly in range(0, 8):
            for lx in xr:
                if hsh(lx, ly, 93 + len(side)) < 0.42 and s.px[(s.face('head', side)[0] + lx, s.face('head', side)[1] + ly)][3] > 0 and (s.face('head', side)[0] + lx, s.face('head', side)[1] + ly) not in s.skinmask:
                    px = (s.face('head', side)[0] + lx, s.face('head', side)[1] + ly)
                    if s.px[px][:3][0] < 90:
                        s.set('head', side, lx, ly, mixc(SCAR2, CHAR, hsh(lx, ly, 94)), 255, True)
    for part, side in (('hat', 'top'), ('hat', 'back'), ('hat', 'left'), ('hat', 'front'), ('hat', 'right')):
        for ly in range(8):
            for lx in range(8):
                if hsh(lx, ly, 95 + len(side)) < 0.35:
                    s.clear(part, side, lx, ly)
    # arms: scarring from the hands up
    for part in ('rarm', 'larm'):
        for side in ('front', 'back', 'left', 'right'):
            blob(part, side, 1, 11, 2, SCAR); blob(part, side, 2, 9, 1, SCAR2, p=0.8)
        blob(part, 'front', 0, 10, 1, CHAR)
    # cloth: soot, scorched edges, holes (second layer loses pixels = torn leather)
    for part in ('jacket', 'rsleeve', 'lsleeve', 'rpants', 'lpants'):
        for side in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = s.face(part, side)
            for ly in range(fh):
                for lx in range(fw):
                    pos = (fx + lx, fy + ly)
                    if s.px[pos][3] == 0:
                        continue
                    n = hsh(pos[0], pos[1], 96)
                    edge = ly >= fh - 2 or lx in (0, fw - 1)
                    if n < 0.14 + (0.2 if edge else 0):
                        s.set(part, side, lx, ly, shade(ASH, 1 + hsh(lx, ly, 97) * 0.8))
                    elif n < 0.20 + (0.18 if edge else 0):
                        s.clear(part, side, lx, ly)
                    elif n < 0.28:
                        s.set(part, side, lx, ly, shade(s.px[pos][:3], 0.55))
    for part in ('body', 'rleg', 'lleg'):
        for side in ('front', 'back', 'right', 'left'):
            fx, fy, fw, fh = s.face(part, side)
            for ly in range(fh):
                for lx in range(fw):
                    pos = (fx + lx, fy + ly)
                    if s.px[pos][3] and pos not in s.skinmask and hsh(pos[0], pos[1], 98) < 0.18:
                        s.set(part, side, lx, ly, shade(s.px[pos][:3], 0.5))
    blob('body', 'front', 3, 1, 2, SCAR2)


def sebastien_valet():
    s = Skin(False, 909)
    SK = (238, 218, 204)
    WIG, WIGD = (238, 236, 232), (200, 198, 204)
    head(s, SK, WIG, (74, 52, 40), brow=(120, 112, 112), lips=(176, 84, 92), hair_rows=2, fringe=0.2, back_rows=5, side_rows=3, hair_amp=0.05, sideburn=False)
    s.set('head', 'front', 2, 2, (50, 36, 34)); s.set('head', 'front', 6, 5, (50, 36, 34))  # patch/beauty mark
    s.set('head', 'front', 1, 5, (232, 160, 160)); s.set('head', 'front', 6, 5, (232, 164, 160))  # rouge
    s.set('head', 'front', 6, 5, (50, 36, 34))
    for lx in range(8):
        s.set('head', 'front', lx, 0, WIG)
    # powdered wig: curled side rolls, queue tied with a black ribbon
    WIGD = (214, 212, 220)
    curl = lambda lx, ly: WIG if ((lx // 2 + ly // 2) % 2 == 0) else mixc(WIG, WIGD, 0.55 + 0.3 * hsh(lx, ly, 3))
    for side, xs in (('right', range(2, 8)), ('left', range(0, 6))):
        for ly in range(0, 6):
            for lx in xs:
                s.set('hat', side, lx, ly, curl(lx, ly))
        for lx in xs:  # rolled curls at the bottom edge
            s.set('hat', side, lx, 5, WIGD if lx % 2 else WIG)
    for lx in range(8):
        for ly in range(0, 2):
            s.set('hat', 'front', lx, ly, curl(lx, ly))
    for lx in range(8):
        for ly in range(0, 8):
            s.set('hat', 'top', lx, ly, curl(lx, ly))
    for ly in range(0, 6):
        for lx in range(8):
            s.set('hat', 'back', lx, ly, curl(lx, ly))
    for lx in (3, 4):
        for ly in range(6, 8):
            s.set('hat', 'back', lx, ly, WIG if ly == 6 else WIGD)
    RIB = (30, 26, 34)
    s.rect('hat', 'back', 3, 5, 2, 1, RIB); s.set('hat', 'back', 2, 5, shade(RIB, 1.6)); s.set('hat', 'back', 5, 5, shade(RIB, 1.6))
    s.set('hat', 'back', 3, 6, RIB); s.set('hat', 'back', 4, 6, RIB)
    BURG, GOLD, CREAM, LACE = (112, 28, 46), (208, 168, 66), (236, 222, 184), (244, 242, 238)
    body_cloth(s, CREAM, amp=0.04, seed=91)
    for ly in range(1, 11):  # embroidered waistcoat
        if ly % 2 == 0:
            s.set('body', 'front', 2, ly, GOLD); s.set('body', 'front', 5, ly, GOLD)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 8, BURG, 0.05, 92)
            s.rect(part, side, 0, 8, s.face(part, side)[2], 3, LACE)
        s.noisy(part, 'top', 0, 0, 4, 4, BURG, 0.05, 92)
    legs_cloth(s, (230, 218, 190), rows=(0, 6), seed=93, amp=0.03)
    for part in ('rleg', 'lleg'):
        for side in SIDES:
            fw = s.face(part, side)[2]
            s.noisy(part, side, 0, 6, fw, 4, (240, 238, 232), 0.03, 94)
            s.noisy(part, side, 0, 10, fw, 2, (22, 20, 24), 0.05, 95)
            s.rect(part, side, 0, 5, fw, 1, GOLD)
        s.set(part, 'front', 1, 10, GOLD); s.set(part, 'front', 2, 10, GOLD)
    overlay(s, 'jacket', BURG, rows=(0, 12), amp=0.05, seed=96)
    for ly in range(0, 12):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
    for ly in range(0, 12):
        s.set('jacket', 'front', 2, ly, GOLD); s.set('jacket', 'front', 5, ly, GOLD)
    for ly in range(1, 11, 2):
        s.set('jacket', 'front', 1, ly, GOLD); s.set('jacket', 'front', 6, ly, GOLD)
    s.rect('jacket', 'front', 3, 0, 2, 4, LACE)  # jabot
    for ly in range(0, 4):
        s.set('jacket', 'front', 3 if ly % 2 else 4, ly, (206, 204, 210))
    s.rect('jacket', 'front', 2, 0, 4, 1, LACE)
    s.rect('jacket', 'back', 3, 3, 2, 9, shade(BURG, 0.7)); s.hband('jacket', 11, GOLD, sides=SIDES)
    s.hband('jacket', 9, shade(BURG, 0.7), sides=SIDES)
    s.rect('jacket', 'left', 0, 8, 4, 2, (24, 20, 24)); s.rect('jacket', 'left', 0, 8, 1, 2, GOLD)  # sword hanger
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, BURG, rows=(0, 7), amp=0.05, seed=97)
        for side in SIDES:
            fw = s.face(part, side)[2]
            s.rect(part, side, 0, 7, fw, 1, GOLD)
            for lx in range(fw):
                s.set(part, side, lx, 8, LACE); s.set(part, side, lx, 9, shade(LACE, 0.88) if lx % 2 else LACE)
                if lx % 2 == 0:
                    s.set(part, side, lx, 10, shade(LACE, 0.94))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (230, 218, 190), rows=(0, 5), amp=0.03, seed=98)
        for side in SIDES:
            s.hband(part, 5, GOLD, sides=(side,))
        s.set(part, 'front', 1, 10, GOLD); s.set(part, 'front', 2, 10, GOLD)
    return s


def beast_host():
    s = Skin(False, 1010)
    SK, HAIR = (222, 184, 154), (92, 66, 46)
    head(s, SK, HAIR, (122, 94, 60), brow=(70, 50, 36), hair_rows=2, fringe=0.7, back_rows=5, side_rows=3, hair_amp=0.16)
    s.set('head', 'front', 3, 6, (190, 108, 102)); s.set('head', 'front', 4, 6, (190, 108, 102))
    hat_hair(s, HAIR, 0.6, back_rows=4, side_rows=2, front_rows=2, seed=9)
    LIN, VEST, PANT, SCARF = (196, 184, 150), (96, 72, 50), (88, 84, 76), (150, 66, 58)
    body_cloth(s, LIN, amp=0.08, seed=101, weave=1)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 7, LIN, 0.08, 102, weave=1)
            s.rect(part, side, 0, 6, s.face(part, side)[2], 1, shade(LIN, 0.8))
        s.noisy(part, 'top', 0, 0, 4, 4, LIN, 0.08, 102)
    legs_cloth(s, PANT, seed=103, weave=1); boots(s, (74, 54, 38), rows=(9, 12), sole=(36, 28, 22))
    overlay(s, 'jacket', VEST, rows=(2, 11), amp=0.09, seed=104)
    for lx in (3, 4):
        for ly in range(2, 11):
            s.clear('jacket', 'front', lx, ly)
    for ly in (3, 5, 7, 9):
        s.set('jacket', 'front', 2, ly, (180, 150, 90))
    s.rect('jacket', 'front', 5, 7, 2, 2, shade(VEST, 0.8)); s.hband('jacket', 10, shade(VEST, 0.8))
    s.rect('jacket', 'back', 3, 2, 2, 9, shade(VEST, 0.85))
    # worn red scarf
    overlay(s, 'jacket', SCARF, rows=(0, 2), amp=0.1, seed=105)
    s.rect('jacket', 'front', 2, 2, 2, 1, SCARF); s.rect('jacket', 'front', 2, 3, 1, 3, SCARF); s.rect('jacket', 'front', 2, 5, 1, 1, shade(SCARF, 0.7))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, LIN, rows=(0, 6), amp=0.08, seed=106, p=0.0)
        for side in SIDES:
            s.rect(part, side, 0, 6, s.face(part, side)[2], 1, shade(LIN, 0.9))
        s.hband(part, 0, shade(VEST, 1.1), sides=SIDES)
    for part in ('rpants', 'lpants'):
        overlay(s, part, PANT, rows=(0, 3), amp=0.08, seed=107, p=0.7)
        s.hband(part, 8, (110, 90, 66)); s.hband(part, 9, (110, 90, 66))
    s.rect('lpants', 'front', 1, 4, 2, 2, shade(PANT, 0.75)); s.set('lpants', 'front', 1, 4, (130, 124, 110))  # patch
    return s


SKINS = {
    'mikael': mikael, 'klaus': klaus, 'elijah': elijah, 'kol': kol, 'rebekah': rebekah, 'finn': finn,
    'deucalion_human': deucalion_human, 'peter_hale': lambda: peter_hale(False), 'peter_hale_burned': lambda: peter_hale(True),
    'sebastien_valet': sebastien_valet, 'beast_host': beast_host,
}

if __name__ == '__main__':
    out = sys.argv[1]
    only = sys.argv[2:] or list(SKINS)
    os.makedirs(out, exist_ok=True)
    for n in only:
        s = SKINS[n]()
        s.save(os.path.join(out, n + '.png'))
        print(n, {p: s.coverage(p) for p in ('hat', 'jacket', 'rsleeve', 'lsleeve', 'rpants', 'lpants')})
