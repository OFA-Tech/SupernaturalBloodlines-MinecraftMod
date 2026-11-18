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
    c = shade(skin, 0.84)
    for ly in range(*rows):
        for lx in range(8):
            if (ly == 5 and lx in (3, 4)) or (ly == 6 and lx in (3, 4)):
                continue
            if hsh(lx, ly, s.seed + 55) < strength:
                s.set('head', 'front', lx, ly, mixc(skin, c, 0.55), skin=True)
    for ly in range(5, 8):
        for lx in (5, 6, 7):
            if hsh(lx, ly, s.seed + 56) < strength:
                s.set('head', 'right', lx, ly, mixc(skin, c, 0.55), skin=True)
        for lx in (0, 1, 2):
            if hsh(lx, ly, s.seed + 57) < strength:
                s.set('head', 'left', lx, ly, mixc(skin, c, 0.55), skin=True)


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


def ribbed(s, part, base, rows=(0, 12), sides=SIDES, seed=0, hi=1.12, lo=0.88):
    """Knit sweater look: vertical ribs."""
    for side in sides:
        fx, fy, fw, fh = s.face(part, side)
        for ly in range(rows[0], rows[1]):
            for lx in range(fw):
                f = hi if lx % 2 == 0 else lo
                n = (hsh(fx + lx, fy + ly, s.seed + seed) - 0.5) * 0.08
                s.set(part, side, lx, ly, shade(base, f + n))


def herringbone(s, part, side, x0, y0, w, h, a, b):
    for j in range(h):
        for i in range(w):
            s.set(part, side, x0 + i, y0 + j, a if ((i + j) // 1 + (j // 2)) % 2 == 0 else b)


def freckles(s, skin, pts):
    for x, y in pts:
        s.set('head', 'front', x, y, mixc(skin, (190, 140, 110), 0.3), skin=True)


def mud(s, p_cloth=0.7, p_skin=0.5, seed=200):
    """Risen look: grave soil over everything, pale skin showing through."""
    MUD = [(40, 30, 24), (54, 40, 30), (70, 52, 38), (88, 66, 48)]
    PALE = (210, 200, 198)
    for part in s.P:
        for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
            fx, fy, fw, fh = s.face(part, side)
            for ly in range(fh):
                for lx in range(fw):
                    pos = (fx + lx, fy + ly)
                    if s.px[pos][3] == 0:
                        continue
                    skin = pos in s.skinmask
                    if part == 'head' and side == 'front' and ly in (3, 4, 6) and lx in (1, 2, 3, 4, 5, 6):
                        continue  # keep eyes, brows and mouth readable
                    if skin:
                        s.set(part, side, lx, ly, mixc(PALE, s.px[pos][:3], 0.25 + 0.2 * hsh(lx, ly, seed)), 255, True)
                    # low-frequency clumps so the mud reads as patches, not noise
                    clump = hsh((fx + lx) // 2, (fy + ly) // 2, seed + 1) * 0.65 + hsh(fx + lx, fy + ly, seed + 2) * 0.35
                    low = ly / max(1, fh - 1)
                    p = (p_skin if skin else p_cloth) + 0.25 * low * (1 if part in ('rleg', 'lleg', 'rpants', 'lpants') else 0.4)
                    if clump < p:
                        c = MUD[int(hsh(fx + lx, fy + ly, seed + 3) * 4) % 4]
                        s.set(part, side, lx, ly, c, 255, False)
    for part in ('jacket', 'rsleeve', 'lsleeve', 'rpants', 'lpants', 'hat'):  # torn, rotted outer layer
        for side in ('top', 'right', 'front', 'left', 'back'):
            fx, fy, fw, fh = s.face(part, side)
            for ly in range(fh):
                for lx in range(fw):
                    if hsh(fx + lx, fy + ly, seed + 9) < (0.16 + (0.2 if ly >= fh - 2 else 0)):
                        s.clear(part, side, lx, ly)


# ------------------------------------------------------------------ characters
def mikael():
    s = Skin(False, 101)
    SK, HAIR = (232, 212, 196), (176, 160, 126)
    head(s, SK, HAIR, (150, 186, 212), brow=(132, 116, 88), hair_rows=1, fringe=0.15, back_rows=4, side_rows=3, hair_amp=0.1)
    for lx in range(8):  # grey through the blond, slicked back
        if hsh(lx, 3, 5) < 0.4:
            s.set('head', 'top', lx, 2 + lx % 3, (158, 154, 146))
    stubble(s, SK, 0.4)
    hat_hair(s, HAIR, 0.5, back_rows=3, side_rows=2, front_rows=1, seed=3)
    BLK, SHIRT = (24, 24, 28), (32, 32, 38)
    body_cloth(s, SHIRT, amp=0.05, seed=7, weave=1)
    s.set('body', 'front', 3, 0, SK, skin=True); s.set('body', 'front', 4, 0, SK, skin=True)
    arms_skin(s, SK); sleeves_base(s, BLK, rows=(0, 10), seed=8); hand_rows(s, SK, (10, 12))
    legs_cloth(s, (26, 26, 30), seed=9); boots(s, (14, 14, 16), rows=(9, 12), sole=(8, 8, 10))
    # black belt with a silver buckle
    for side in SIDES:
        s.rect('body', side, 0, 9, s.face('body', side)[2], 2, (12, 12, 14))
    s.rect('body', 'front', 3, 9, 2, 2, (176, 178, 188)); s.rect('body', 'front', 3, 10, 1, 1, (92, 94, 104))
    # black jacket, open, with lapels
    overlay(s, 'jacket', BLK, rows=(0, 10), amp=0.05, seed=11)
    for ly in range(0, 10):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
    for i in range(6):
        s.set('jacket', 'front', 2, i, (40, 40, 48)); s.set('jacket', 'front', 5, i, (40, 40, 48))
    s.hband('jacket', 9, (14, 14, 16), x0=0, x1=8, sides=('right', 'left', 'back'))
    s.rect('jacket', 'back', 3, 6, 2, 4, shade(BLK, 0.7))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, BLK, rows=(0, 10), amp=0.05, seed=12)
        s.hband(part, 9, (14, 14, 16))
    # white oak stake: silvery-white, carved, tucked at the left hip
    OAK, GRV = (222, 226, 232), (146, 152, 166)
    s.rect('jacket', 'front', 6, 5, 1, 6, OAK); s.rect('jacket', 'left', 0, 7, 2, 3, OAK)
    for ly in range(5, 11, 2):
        s.set('jacket', 'front', 6, ly, GRV)
    s.set('jacket', 'left', 1, 8, GRV); s.set('jacket', 'left', 0, 7, GRV)
    s.rect('lpants', 'front', 3, 0, 1, 4, OAK); s.set('lpants', 'front', 3, 1, GRV); s.set('lpants', 'front', 3, 3, GRV); s.set('lpants', 'front', 3, 4, (170, 176, 188))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (26, 26, 30), rows=(0, 3), amp=0.05, seed=13, p=0.6)
        s.hband(part, 9, (10, 10, 12))
    # carved stake prop nets (free skin space): shaft net at (56,16) 8x12, tip net at (56,30) 4x4
    for j in range(12):
        for i in range(8):
            s.px[56 + i, 16 + j] = OAK if (i + j // 2) % 4 else GRV
    for j in range(4):
        for i in range(4):
            s.px[56 + i, 30 + j] = (200, 204, 212)
    return s


def deucalion_human():
    s = Skin(False, 707)
    SK, HAIR = (212, 178, 150), (170, 144, 98)
    head(s, SK, HAIR, (88, 132, 178), brow=(120, 98, 64), hair_rows=1, fringe=0.65, back_rows=5, side_rows=3, hair_amp=0.14)
    stubble(s, SK, 0.35)
    s.set('head', 'front', 3, 6, (160, 96, 88)); s.set('head', 'front', 4, 6, (160, 96, 88))
    hat_hair(s, HAIR, 0.75, back_rows=3, side_rows=2, front_rows=2, seed=7)  # swept, voluminous top
    SW, SWL = (38, 40, 46), (122, 124, 130)
    # dark knit sweater (base), lighter textured grey shawl collar and shoulder panels (second layer)
    for side in SIDES:
        ribbed(s, 'body', SW, sides=(side,), seed=21)
    s.noisy('body', 'top', 0, 0, 8, 4, SW, 0.05, 22)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        ribbed(s, part, SW, rows=(0, 10), seed=23)
        s.noisy(part, 'top', 0, 0, 4, 4, SWL, 0.06, 24)
        s.hband(part, 9, shade(SW, 0.75))
    legs_cloth(s, (30, 32, 38), seed=25); boots(s, (24, 22, 22), rows=(9, 12), sole=(10, 10, 12))
    overlay(s, 'jacket', SWL, rows=(0, 2), amp=0.1, seed=26)
    ribbed(s, 'jacket', SWL, rows=(0, 2), seed=27, hi=1.08, lo=0.9)
    lobes = {0: (1, 2, 5, 6), 1: (1, 2, 5, 6), 2: (1, 2, 5, 6), 3: (2, 5), 4: (2, 5), 5: (3, 4)}
    for ly, xs in lobes.items():
        for lx in xs:
            s.set('jacket', 'front', lx, ly, shade(SWL, 1.1 if (lx + ly) % 2 else 0.9))
    for lx in (3, 4):
        for ly in range(0, 3):
            s.clear('jacket', 'front', lx, ly)
    s.hband('jacket', 0, shade(SWL, 1.1), sides=('back',)); s.hband('jacket', 1, shade(SWL, 0.9), sides=('back',))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, SWL, rows=(0, 4), amp=0.1, seed=28)
        ribbed(s, part, SWL, rows=(0, 4), seed=29, hi=1.08, lo=0.9)
        s.hband(part, 3, shade(SWL, 0.78))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (30, 32, 38), rows=(0, 2), amp=0.06, seed=30, p=0.5)
    return s


def peter_hale(burned=False):
    s = Skin(False, 808)
    SK, HAIR = (226, 196, 172), (36, 28, 24)
    head(s, SK, HAIR, (96, 140, 152), brow=(30, 22, 18), hair_rows=1, fringe=0.1, back_rows=5, side_rows=3, hair_amp=0.1)
    for lx in range(8):  # slicked-back sheen
        s.set('head', 'top', lx, 1 + lx % 2, (60, 48, 40))
    beard(s, (46, 34, 28), rows=(5, 8), hat=True)
    hat_hair(s, HAIR, 0.45, back_rows=3, side_rows=2, front_rows=1, seed=8)
    CAM, CAML = (188, 146, 98), (206, 166, 116)
    WHITE, WHD = (234, 232, 228), (200, 198, 196)
    H1, H2 = (70, 70, 78), (50, 50, 58)
    body_cloth(s, WHITE, amp=0.03, seed=81)
    for lx in (3, 4):
        s.set('body', 'front', lx, 0, SK, skin=True); s.set('body', 'front', lx, 1, shade(SK, 0.94), skin=True)
    s.rect('body', 'front', 1, 0, 2, 2, WHITE); s.rect('body', 'front', 5, 0, 2, 2, WHITE)
    for lx in (2, 5):
        s.set('body', 'front', lx, 0, WHD); s.set('body', 'front', lx, 1, WHD)
    # dark grey herringbone waistcoat
    herringbone(s, 'body', 'front', 1, 2, 6, 10, H1, H2)
    for ly in (4, 6, 8):
        s.set('body', 'front', 4, ly, (150, 150, 158))
    s.rect('body', 'front', 3, 2, 2, 1, WHITE)
    for side in ('right', 'left', 'back'):
        herringbone(s, 'body', side, 0, 2, s.face('body', side)[2], 10, H1, H2)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 10, CAM, 0.06, 82)
        s.noisy(part, 'top', 0, 0, 4, 4, CAM, 0.06, 82)
    legs_cloth(s, (36, 38, 44), seed=83); boots(s, (22, 20, 20), rows=(9, 12), sole=(10, 10, 10))
    # camel wool overcoat, open at the front, wide lapels
    overlay(s, 'jacket', CAM, rows=(0, 12), amp=0.07, seed=84, weave=1)
    for ly in range(0, 12):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
        if ly >= 3:
            s.clear('jacket', 'front', 2, ly); s.clear('jacket', 'front', 5, ly)
    for i in range(4):
        s.set('jacket', 'front', 2, i, CAML); s.set('jacket', 'front', 5, i, CAML)
    s.rect('jacket', 'front', 1, 0, 1, 3, CAML); s.rect('jacket', 'front', 6, 0, 1, 3, CAML)
    s.hband('jacket', 11, shade(CAM, 0.85))
    s.rect('jacket', 'front', 0, 7, 2, 1, shade(CAM, 0.75)); s.rect('jacket', 'front', 6, 7, 2, 1, shade(CAM, 0.75))
    s.rect('jacket', 'back', 3, 7, 2, 5, shade(CAM, 0.8))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, CAM, rows=(0, 10), amp=0.07, seed=85, weave=1)
        s.hband(part, 9, shade(CAM, 0.82))
    for part in ('rpants', 'lpants'):
        overlay(s, part, CAM, rows=(0, 8), amp=0.07, seed=86, weave=1, sides=('right', 'left', 'back', 'front'))
        s.hband(part, 7, shade(CAM, 0.85))
    if burned:
        risen(s)
    return s


def risen(s):
    """Peter's resurrection: stripped to rotted trousers, pale bare skin, grave soil on top."""
    PALE = (214, 204, 202)
    for part in ('body', 'rarm', 'larm'):
        s.fill_part(part, PALE, 0.05, 5, skin=True)
    for part in ('jacket', 'rsleeve', 'lsleeve', 'rpants', 'lpants', 'hat'):
        s.clear_part(part)
    for part in ('rleg', 'lleg'):
        s.fill_part(part, PALE, 0.05, 6, skin=True, rows=(8, 12), sides=('right', 'front', 'left', 'back'))
        legs_rot = (38, 38, 44)
        for side in SIDES:
            fw = s.face(part, side)[2]
            for ly in range(0, 8):
                for lx in range(fw):
                    if hsh(lx, ly, 17) > ly * 0.04:
                        s.set(part, side, lx, ly, shade(legs_rot, 0.9 + 0.2 * hsh(lx, ly, 18)))
    hat_hair(s, (30, 24, 20), 0.55, back_rows=3, side_rows=2, front_rows=1, seed=19)
    mud(s, p_cloth=0.6, p_skin=0.45)
    LUMP = [(34, 26, 20), (50, 38, 28), (66, 50, 36)]  # caked soil on the second layer
    for part in ('jacket', 'rsleeve', 'lsleeve', 'rpants', 'lpants'):
        for side in SIDES:
            fx, fy, fw, fh = s.face(part, side)
            for ly in range(fh):
                for lx in range(fw):
                    clump = hsh((fx + lx) // 2, (fy + ly) // 2, 301) * 0.6 + hsh(fx + lx, fy + ly, 302) * 0.4
                    if clump < 0.3 + 0.2 * ly / max(1, fh - 1):
                        s.set(part, side, lx, ly, LUMP[int(hsh(fx + lx, fy + ly, 303) * 3) % 3])


def kol():
    s = Skin(False, 404)
    SK, HAIR = (230, 198, 172), (82, 58, 42)
    head(s, SK, HAIR, (62, 42, 30), brow=(56, 40, 30), hair_rows=2, fringe=0.85, back_rows=4, side_rows=2, hair_amp=0.16)
    stubble(s, SK, 0.4)
    s.set('head', 'front', 4, 6, (190, 100, 96)); s.set('head', 'front', 5, 6, (190, 100, 96)); s.set('head', 'front', 5, 5, shade(SK, 0.88), skin=True)
    hat_hair(s, HAIR, 0.8, back_rows=3, side_rows=2, front_rows=2, seed=4)  # messy, falling over the brow
    HEN, NAVY = (98, 100, 106), (34, 42, 62)
    body_cloth(s, HEN, amp=0.07, seed=41, weave=1)
    s.set('body', 'front', 3, 0, SK, skin=True); s.set('body', 'front', 4, 0, SK, skin=True); s.set('body', 'front', 3, 1, shade(SK, 0.9), skin=True)
    for ly in (1, 3, 5):
        s.set('body', 'front', 4, ly, (200, 200, 206))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 8, NAVY, 0.06, 42)
        s.noisy(part, 'top', 0, 0, 4, 4, NAVY, 0.06, 42)
    legs_cloth(s, (46, 48, 54), seed=43); boots(s, (72, 52, 38), rows=(9, 12), sole=(30, 22, 18))
    overlay(s, 'jacket', NAVY, rows=(0, 12), amp=0.06, seed=44)
    for ly in range(0, 12):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly) if ly < 10 else None
    for i in range(6):
        s.set('jacket', 'front', 2, i, shade(NAVY, 1.35)); s.set('jacket', 'front', 5, i, shade(NAVY, 1.35))
    s.rect('jacket', 'front', 3, 10, 2, 1, (180, 180, 190))
    s.hband('jacket', 11, shade(NAVY, 0.8))
    s.rect('jacket', 'back', 3, 6, 2, 6, shade(NAVY, 0.75))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, NAVY, rows=(0, 9), amp=0.06, seed=45)
        s.hband(part, 8, shade(NAVY, 0.78))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (46, 48, 54), rows=(0, 2), amp=0.06, seed=46, p=0.7)
        s.hband(part, 9, (40, 30, 24))
    return s


def klaus():
    s = Skin(False, 202)
    SK, HAIR = (242, 228, 214), (164, 136, 92)
    head(s, SK, HAIR, (56, 104, 112), brow=(108, 84, 56), hair_rows=2, fringe=0.65, back_rows=4, side_rows=3, hair_amp=0.16)
    stubble(s, SK, 0.4)
    s.set('head', 'front', 4, 6, (190, 100, 96)); s.set('head', 'front', 5, 5, shade(SK, 0.88), skin=True)
    hat_hair(s, HAIR, 0.75, back_rows=3, side_rows=2, front_rows=2, seed=4)  # curled, dirty blond
    H = (56, 58, 64)  # dark henley
    body_cloth(s, H, amp=0.07, seed=21)
    for ly in range(0, 4):
        s.set('body', 'front', 3, ly, shade(H, 0.7)); s.set('body', 'front', 4, ly, shade(H, 0.7))
        s.set('body', 'front', 3 if ly % 2 == 0 else 4, ly, (170, 170, 176))
    s.hband('body', 0, shade(H, 1.3), sides=('front',), x0=2, x1=6)
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 6, H, 0.07, 22)
        s.noisy(part, 'top', 0, 0, 4, 4, H, 0.07, 22)
        s.hband(part, 5, shade(H, 0.75))
    legs_cloth(s, (40, 46, 64), seed=23); boots(s, (62, 44, 34), rows=(9, 12))
    SIL, GOLD, CORD = (190, 196, 206), (206, 168, 72), (90, 62, 44)
    s.line('jacket', 'front', [(2, 0), (5, 0), (2, 1), (5, 1), (3, 2), (4, 2)], SIL)
    s.line('jacket', 'front', [(1, 0), (6, 0), (1, 1), (6, 1), (1, 2), (6, 2), (2, 3), (5, 3), (3, 4), (4, 4)], CORD)
    s.line('jacket', 'front', [(2, 2), (5, 2), (2, 4), (5, 4), (3, 5), (4, 5)], GOLD)
    s.rect('jacket', 'front', 3, 3, 2, 1, GOLD); s.set('jacket', 'front', 3, 6, (150, 40, 44)); s.set('jacket', 'front', 4, 6, (150, 40, 44))
    s.set('jacket', 'back', 0, 0, SIL)
    for side in ('right', 'left'):
        s.hband('jacket', 0, SIL, sides=(side,), x0=1, x1=3)
        s.hband('jacket', 1, CORD, sides=(side,), x0=1, x1=3)
    s.hband('jacket', 0, CORD, sides=('back',), x0=1, x1=7)
    for side in SIDES:
        s.rect('jacket', side, 0, 10, s.face('jacket', side)[2], 1, (60, 40, 28))
    s.set('jacket', 'front', 3, 10, (180, 150, 80)); s.set('jacket', 'front', 4, 10, (180, 150, 80))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, H, rows=(5, 7), amp=0.08, seed=24)
        s.hband(part, 6, shade(H, 0.65))
    for (lx, ly, c) in ((1, 8, (160, 34, 40)), (2, 9, (160, 34, 40)), (0, 10, (196, 146, 52)), (2, 11, (196, 146, 52)), (3, 7, (40, 90, 140))):
        s.set('rarm', 'front', lx, ly, c); s.set('rarm', 'right', 1 + lx % 2, ly, c)
    for (lx, ly, c) in ((1, 10, (196, 146, 52)), (0, 9, (160, 34, 40))):
        s.set('larm', 'front', lx, ly, c)
    for (lx, ly, c) in ((2, 3, (160, 34, 40)), (3, 4, (160, 34, 40)), (1, 5, (196, 146, 52))):
        s.set('lpants', 'front', lx, ly, c, 220)
    for part in ('rpants', 'lpants'):
        overlay(s, part, (40, 46, 64), rows=(0, 2), amp=0.1, seed=25, p=0.7)
    return s


def elijah():
    s = Skin(False, 303)
    SK, HAIR = (222, 190, 164), (92, 66, 46)
    head(s, SK, HAIR, (122, 94, 52), brow=(70, 50, 36), hair_rows=1, fringe=0.3, back_rows=5, side_rows=3, hair_amp=0.08)
    s.set('head', 'front', 5, 1, shade(HAIR, 1.3))
    hat_hair(s, HAIR, 0.5, back_rows=3, side_rows=2, front_rows=1, seed=2)
    SUIT, SUITL = (32, 34, 40), (46, 48, 56)
    SHIRT = (234, 234, 238)
    body_cloth(s, SHIRT, amp=0.03, seed=31)
    TIE = (50, 58, 78)
    s.rect('body', 'front', 3, 0, 2, 12, TIE)
    s.rect('body', 'front', 3, 0, 2, 1, shade(TIE, 1.25)); s.rect('body', 'front', 4, 5, 1, 7, shade(TIE, 0.8))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 11, SUIT, 0.05, 32)
            s.rect(part, side, 0, 10, s.face(part, side)[2], 1, SHIRT)
        s.noisy(part, 'top', 0, 0, 4, 4, SUIT, 0.05, 32)
    legs_cloth(s, (30, 32, 38), seed=33, weave=1); boots(s, (18, 18, 22), rows=(10, 12), sole=(10, 10, 12))
    overlay(s, 'jacket', SUIT, rows=(0, 12), amp=0.04, seed=34)
    for side in ('front', 'back', 'right', 'left'):
        w = s.face('jacket', side)[2]
        for lx in range(0, w, 2):
            for ly in range(0, 12):
                if s.px[s.face('jacket', side)[0] + lx, s.face('jacket', side)[1] + ly][3] > 0:
                    s.set('jacket', side, lx, ly, SUITL)
    for ly in range(0, 9):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
    for i in range(7):
        s.set('jacket', 'front', 2 - (i // 4), i, (58, 60, 70)); s.set('jacket', 'front', 5 + (i // 4), i, (58, 60, 70))
    s.rect('jacket', 'front', 1, 0, 1, 7, (26, 28, 34)); s.rect('jacket', 'front', 6, 0, 1, 7, (26, 28, 34))
    s.set('jacket', 'front', 6, 3, (240, 240, 244)); s.set('jacket', 'front', 6, 2, (220, 222, 230))
    s.set('jacket', 'front', 3, 9, (160, 160, 170))
    s.rect('jacket', 'back', 3, 8, 2, 4, (26, 28, 34))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, SUIT, rows=(0, 10), amp=0.04, seed=35)
        s.set(part, 'front', 1, 9, (160, 160, 170)); s.set(part, 'left', 1, 9, (160, 160, 170))
    for part in ('rpants', 'lpants'):
        overlay(s, part, (30, 32, 38), rows=(0, 3), amp=0.05, seed=36)
        s.hband(part, 11, (12, 12, 14))
    return s


def rebekah():
    s = Skin(True, 505)
    SK, HAIR = (244, 226, 212), (230, 214, 168)
    head(s, SK, HAIR, (140, 184, 222), brow=(190, 168, 120), lips=(198, 122, 122), hair_rows=2, fringe=0.55, back_rows=8, side_rows=6, sideburn=False, hair_amp=0.12)
    freckles(s, SK, [(1, 5), (5, 5), (6, 5), (3, 4)])
    for side in ('right', 'left'):
        for ly in range(3, 8):
            for lx in (range(0, 4) if side == 'right' else range(4, 8)):
                s.set('head', side, lx, ly, shade(HAIR, 0.88 + 0.22 * hsh(lx, ly, 8)))
    hat_hair(s, HAIR, 0.9, back_rows=8, side_rows=6, front_rows=2, seed=5)
    for ly in range(2, 5):
        s.set('hat', 'front', 7, ly, shade(HAIR, 1.0)); s.set('hat', 'front', 0, ly, shade(HAIR, 1.0))
    COAT, COATD, TOP, JEAN = (112, 114, 120), (84, 86, 92), (238, 234, 226), (38, 42, 58)
    body_cloth(s, TOP, amp=0.04, seed=51)
    s.rect('body', 'front', 3, 0, 2, 1, SK, skin=True)
    # long pendant necklace on the blouse
    s.line('body', 'front', [(2, 1), (5, 1), (2, 2), (5, 2), (3, 3), (4, 3), (3, 4), (4, 4)], (200, 168, 82))
    s.rect('body', 'front', 3, 5, 2, 2, (204, 172, 84)); s.set('body', 'front', 3, 5, (136, 96, 40))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 9, COAT, 0.05, 52)
        s.noisy(part, 'top', 0, 0, 3, 4, COAT, 0.05, 52)
        s.set(part, 'front', 1, 10, (200, 168, 82))
    legs_cloth(s, JEAN, seed=53); boots(s, (86, 60, 42), rows=(9, 12), sole=(40, 28, 22))
    # grey wool coat, open, with a wide collar; hair spilling over it
    overlay(s, 'jacket', COAT, rows=(0, 11), amp=0.05, seed=55, weave=1)
    for ly in range(0, 11):
        for lx in (2, 3, 4, 5):
            s.clear('jacket', 'front', lx, ly)
    for i in range(5):
        s.set('jacket', 'front', 1, i, shade(COAT, 1.3)); s.set('jacket', 'front', 6, i, shade(COAT, 1.3))
    s.hband('jacket', 10, COATD)
    s.rect('jacket', 'front', 0, 6, 2, 1, COATD); s.rect('jacket', 'front', 6, 6, 2, 1, COATD)
    for ly in range(0, 9):
        for lx in range(1, 7):
            if hsh(lx, ly, 5) < 0.95 - ly * 0.04:
                s.set('jacket', 'back', lx, ly, shade(HAIR, 0.82 + 0.3 * hsh(lx, ly, 6)))
    for side in ('right', 'left'):
        for ly in range(0, 6):
            for lx in ((0, 1) if side == 'right' else (2, 3)):
                s.set('jacket', side, lx, ly, shade(HAIR, 0.9 + 0.25 * hsh(lx, ly, 7)))
    for lx in (0, 1, 6, 7):
        for ly in range(0, 3):
            s.set('jacket', 'front', lx, ly, shade(HAIR, 0.95 + 0.2 * hsh(lx, ly, 8)))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, COAT, rows=(0, 9), amp=0.05, seed=56, weave=1)
        s.hband(part, 8, COATD)
    for part in ('rpants', 'lpants'):
        overlay(s, part, JEAN, rows=(0, 2), amp=0.08, seed=57, p=0.6)
        s.hband(part, 8, shade((86, 60, 42), 1.15), sides=('front', 'back', 'right', 'left'))
    return s


def finn():
    s = Skin(False, 606)
    SK, HAIR = (226, 200, 178), (84, 62, 44)
    head(s, SK, HAIR, (110, 128, 84), brow=(66, 48, 34), hair_rows=1, fringe=0.2, back_rows=5, side_rows=3, hair_amp=0.1)
    stubble(s, SK, 0.2)
    for lx in range(8):
        s.set('head', 'top', lx, 1 + lx % 2, shade(HAIR, 1.3))
    hat_hair(s, HAIR, 0.5, back_rows=3, side_rows=2, front_rows=1, seed=6)
    COAT, SHIRT, TIE = (94, 96, 102), (228, 228, 230), (54, 54, 62)
    body_cloth(s, SHIRT, amp=0.03, seed=61)
    s.rect('body', 'front', 3, 0, 2, 1, SK, skin=True)
    s.rect('body', 'front', 3, 1, 2, 9, TIE); s.rect('body', 'front', 3, 1, 2, 1, shade(TIE, 1.3))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 10, COAT, 0.05, 62)
            s.rect(part, side, 0, 10, s.face(part, side)[2], 1, SHIRT)
        s.noisy(part, 'top', 0, 0, 4, 4, COAT, 0.05, 62)
    legs_cloth(s, (44, 44, 52), seed=63); boots(s, (16, 16, 18), rows=(10, 12), sole=(8, 8, 10))
    overlay(s, 'jacket', COAT, rows=(0, 12), amp=0.05, seed=64, weave=1)
    for ly in range(0, 12):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
    for i in range(5):
        s.set('jacket', 'front', 2, i, shade(COAT, 1.25)); s.set('jacket', 'front', 5, i, shade(COAT, 1.25))
    for ly in (7, 9):
        s.set('jacket', 'front', 2, ly, (30, 30, 34))
    s.hband('jacket', 11, shade(COAT, 0.8))
    s.rect('jacket', 'back', 3, 7, 2, 5, shade(COAT, 0.78))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, COAT, rows=(0, 10), amp=0.05, seed=65, weave=1)
        s.hband(part, 9, shade(COAT, 0.8))
    for part in ('rpants', 'lpants'):
        overlay(s, part, COAT, rows=(0, 5), amp=0.05, seed=66, weave=1)  # coat tails
        s.hband(part, 4, shade(COAT, 0.8))
    return s


def freya():
    s = Skin(True, 1111)
    SK, HAIR = (238, 218, 200), (170, 144, 100)
    head(s, SK, HAIR, (92, 140, 104), brow=(112, 88, 56), lips=(178, 88, 98), hair_rows=2, fringe=0.6, back_rows=8, side_rows=6, sideburn=False, hair_amp=0.14)
    for lx in (1, 6):  # smoky liner
        s.set('head', 'front', lx, 3, (86, 64, 56))
    for side in ('right', 'left'):
        for ly in range(3, 8):
            for lx in (range(0, 4) if side == 'right' else range(4, 8)):
                s.set('head', side, lx, ly, shade(HAIR, 0.88 + 0.22 * hsh(lx, ly, 8)))
    hat_hair(s, HAIR, 0.9, back_rows=8, side_rows=6, front_rows=2, seed=9)
    LEATH, TOP, JEAN = (26, 26, 30), (54, 54, 62), (32, 36, 50)
    body_cloth(s, TOP, amp=0.05, seed=111)
    s.rect('body', 'front', 3, 0, 2, 1, SK, skin=True)
    # pendant on a cord
    s.line('body', 'front', [(2, 1), (5, 1), (2, 2), (5, 2), (3, 3), (4, 3)], (82, 62, 44))
    s.rect('body', 'front', 3, 4, 2, 2, (200, 204, 212)); s.set('body', 'front', 3, 4, (214, 142, 44)); s.set('body', 'front', 4, 5, (214, 142, 44))
    arms_skin(s, SK)
    for part in ('rarm', 'larm'):
        for side in SIDES:
            s.noisy(part, side, 0, 0, s.face(part, side)[2], 10, TOP, 0.05, 112)
        s.noisy(part, 'top', 0, 0, 3, 4, TOP, 0.05, 112)
    legs_cloth(s, JEAN, seed=113); boots(s, (20, 20, 24), rows=(9, 12), sole=(8, 8, 10))
    overlay(s, 'jacket', LEATH, rows=(0, 10), amp=0.08, seed=114)
    for ly in range(0, 10):
        for lx in (3, 4):
            s.clear('jacket', 'front', lx, ly)
    for i in range(5):
        s.set('jacket', 'front', 2, i, (44, 44, 52)); s.set('jacket', 'front', 5, i, (44, 44, 52))
    s.set('jacket', 'front', 2, 9, (170, 172, 180)); s.set('jacket', 'front', 5, 9, (170, 172, 180))
    s.hband('jacket', 9, (14, 14, 16))
    for ly in range(0, 9):
        for lx in range(1, 7):
            if hsh(lx, ly, 5) < 0.95 - ly * 0.04:
                s.set('jacket', 'back', lx, ly, shade(HAIR, 0.82 + 0.3 * hsh(lx, ly, 6)))
    for side in ('right', 'left'):
        for ly in range(0, 6):
            for lx in ((0, 1) if side == 'right' else (2, 3)):
                s.set('jacket', side, lx, ly, shade(HAIR, 0.9 + 0.25 * hsh(lx, ly, 7)))
    for lx in (0, 1, 6, 7):
        for ly in range(0, 3):
            s.set('jacket', 'front', lx, ly, shade(HAIR, 0.95 + 0.2 * hsh(lx, ly, 8)))
    for part in ('rsleeve', 'lsleeve'):
        overlay(s, part, LEATH, rows=(0, 10), amp=0.08, seed=115)
        s.hband(part, 9, (14, 14, 16)); s.set(part, 'front', 1, 4, (60, 60, 68))
    for part in ('rpants', 'lpants'):
        overlay(s, part, JEAN, rows=(0, 2), amp=0.06, seed=116, p=0.6)
        s.hband(part, 8, (14, 14, 18), sides=('front', 'back', 'right', 'left'))
    return s


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
    'mikael': mikael, 'klaus': klaus, 'elijah': elijah, 'kol': kol, 'rebekah': rebekah, 'finn': finn, 'freya': freya,
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
