"""Klaus skin fix, applied by paint_skins.py after it paints klaus.png: shaded hands, a rolled henley cuff, the paint squares off the trousers.
Run on its own: klaus_skin_fix.py <in.png> <out.png>"""
import sys, hashlib
from PIL import Image

STRAY = {(160, 34, 40), (196, 146, 52), (40, 90, 140)}
BLOOD = (110, 24, 28)
H = (56, 58, 64)              # henley, from paint_skins.klaus()


def n(x, y, amp):
    """Deterministic per-pixel brightness factor in 1 +- amp."""
    return 1 + (hashlib.md5(b'%d,%d' % (x, y)).digest()[0] / 255 - 0.5) * 2 * amp


def put(px, x, y, c, amp=0.015):
    f = n(x, y, amp)
    px[x, y] = tuple(min(255, round(v * f)) for v in c) + (255,)


# the face is (242,228,214); the hands are a touch warmer and a little darker
WRIST_HID, WRIST, PALE, BACK, KNUCK, FINGER, GAP, UNDER = (
    (196, 166, 144), (212, 184, 162), (240, 218, 198), (236, 212, 190), (224, 190, 172), (230, 204, 182), (206, 176, 154), (212, 184, 162))


def hand(px, x0, top, outer, front, inner, back, bottom):
    """x0: left edge of the arm's four side faces; top: first skin row (hidden under the cuff); faces are x offsets."""
    for side, off in (('outer', outer), ('front', front), ('inner', inner), ('back', back)):
        dim = 0.93 if side == 'inner' else 0.97 if side == 'back' else 1.0     # the side against the body is in shade
        for lx in range(4):
            x = x0 + off + lx
            knuckles = side in ('front', 'outer')
            rows = (WRIST_HID, WRIST, PALE, BACK,
                    KNUCK if knuckles and lx % 2 == 0 else BACK,
                    GAP if lx % 2 == 1 else FINGER)
            for r, c in enumerate(rows):
                put(px, x, top + r, tuple(v * dim for v in c))
    bx, by = bottom
    for y in range(by, by + 4):
        for x in range(bx, bx + 4):
            put(px, x, y, UNDER, 0.02)


def cuff(px, x0, y):
    """Henley cuff on the sleeve layer: a lit roll over its own shadow, in place of two flat bands."""
    for x in range(x0, x0 + 16):
        put(px, x, y, tuple(v * (1.0 if x % 4 == 3 else 1.27) for v in H), 0.05)     # a crease at each corner of the arm
        put(px, x, y + 1, tuple(v * 0.72 for v in H), 0.05)



def apply(im):
    px = im.load()
    hand(px, 40, 26, 0, 4, 8, 12, (48, 16))       # right arm: outer x40, front x44, inner x48, back x52
    hand(px, 32, 58, 8, 4, 0, 12, (40, 48))       # left arm: inner x32, front x36, outer x40, back x44
    cuff(px, 40, 41)
    cuff(px, 48, 57)

    # the paint squares floating on the left trouser leg (overlay layer)
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y][:3] in STRAY:
                px[x, y] = (0, 0, 0, 0)

    # dried blood, one colour, on the knuckles only
    for x, y in ((45, 30), (46, 31), (38, 62)):
        px[x, y] = BLOOD + (255,)

    assert not any(px[x, y][:3] in STRAY for x in range(64) for y in range(64))
    return im



if __name__ == '__main__':
    apply(Image.open(sys.argv[1]).convert('RGBA')).save(sys.argv[2])
