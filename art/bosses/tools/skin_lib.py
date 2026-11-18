"""Tiny pixel-art toolkit for 64x64 Minecraft player skins (1.8+ layout, separate left limbs)."""
from PIL import Image

SIDES = ('right', 'front', 'left', 'back')
BASE_OF = {'hat': 'head', 'jacket': 'body', 'rsleeve': 'rarm', 'lsleeve': 'larm', 'rpants': 'rleg', 'lpants': 'lleg'}


def hsh(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def mixc(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Skin:
    def __init__(self, slim=False, seed=1):
        self.im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.px = self.im.load()
        self.slim, self.seed = slim, seed
        aw = 3 if slim else 4
        self.P = {
            'head': (0, 0, 8, 8, 8), 'hat': (32, 0, 8, 8, 8),
            'body': (16, 16, 8, 12, 4), 'jacket': (16, 32, 8, 12, 4),
            'rarm': (40, 16, aw, 12, 4), 'rsleeve': (40, 32, aw, 12, 4),
            'larm': (32, 48, aw, 12, 4), 'lsleeve': (48, 48, aw, 12, 4),
            'rleg': (0, 16, 4, 12, 4), 'rpants': (0, 32, 4, 12, 4),
            'lleg': (16, 48, 4, 12, 4), 'lpants': (0, 48, 4, 12, 4),
        }
        self.skinmask = set()

    # -- geometry ---------------------------------------------------------
    def face(self, part, side):
        x, y, w, h, d = self.P[part]
        return {'top': (x + d, y, w, d), 'bottom': (x + d + w, y, w, d), 'right': (x, y + d, d, h),
                'front': (x + d, y + d, w, h), 'left': (x + d + w, y + d, d, h),
                'back': (x + 2 * d + w, y + d, w, h)}[side]

    # -- pixel ops ---------------------------------------------------------
    def set(self, part, side, lx, ly, c, a=255, skin=False):
        x, y, w, h = self.face(part, side)
        if 0 <= lx < w and 0 <= ly < h:
            self.px[x + lx, y + ly] = (*c[:3], a)
            if skin:
                self.skinmask.add((x + lx, y + ly))
            else:
                self.skinmask.discard((x + lx, y + ly))

    def get(self, part, side, lx, ly):
        x, y, w, h = self.face(part, side)
        return self.px[x + lx, y + ly]

    def clear(self, part, side, lx, ly):
        x, y, w, h = self.face(part, side)
        if 0 <= lx < w and 0 <= ly < h:
            self.px[x + lx, y + ly] = (0, 0, 0, 0)
            self.skinmask.discard((x + lx, y + ly))

    def rect(self, part, side, lx, ly, w, h, c, a=255, skin=False):
        for j in range(h):
            for i in range(w):
                self.set(part, side, lx + i, ly + j, c, a, skin)

    def noisy(self, part, side, lx, ly, w, h, c, amp=0.07, seed=0, weave=0, skin=False):
        """Fill with fabric-like noise; weave>0 adds a checker/pinstripe modulation."""
        fx, fy, fw, fh = self.face(part, side)
        for j in range(h):
            for i in range(w):
                n = (hsh(fx + lx + i, fy + ly + j, self.seed + seed) - 0.5) * 2 * amp
                if weave:
                    n += weave * (0.03 if (lx + i + ly + j) % 2 else -0.03)
                self.set(part, side, lx + i, ly + j, shade(c, 1 + n), 255, skin)

    def fill_part(self, part, c, amp=0.06, seed=0, skin=False, sides=None, rows=None):
        for side in (sides or ('top', 'bottom', 'right', 'front', 'left', 'back')):
            fx, fy, fw, fh = self.face(part, side)
            r0, r1 = (0, fh) if (rows is None or side in ('top', 'bottom')) else rows
            self.noisy(part, side, 0, r0, fw, r1 - r0, c, amp, seed, skin=skin)

    def band(self, part, y0, y1, c, amp=0.05, seed=0, sides=SIDES, skin=False):
        for side in sides:
            fx, fy, fw, fh = self.face(part, side)
            self.noisy(part, side, 0, y0, fw, y1 - y0, c, amp, seed, skin=skin)

    def hband(self, part, ly, c, sides=SIDES, x0=0, x1=None):
        for side in sides:
            fx, fy, fw, fh = self.face(part, side)
            self.rect(part, side, x0, ly, (x1 or fw) - x0, 1, c)

    def line(self, part, side, pts, c, a=255):
        for lx, ly in pts:
            self.set(part, side, lx, ly, c, a)

    def clear_part(self, part):
        for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
            fx, fy, fw, fh = self.face(part, side)
            for j in range(fh):
                for i in range(fw):
                    self.clear(part, side, i, j)

    # -- limbs ------------------------------------------------------------
    def mirror(self, src, dst):
        """Copy part src onto dst mirrored (right limb -> left limb)."""
        pairs = {'top': 'top', 'bottom': 'bottom', 'front': 'front', 'back': 'back', 'right': 'left', 'left': 'right'}
        for s_side, d_side in pairs.items():
            sx, sy, sw, sh = self.face(src, s_side)
            dx, dy, dw, dh = self.face(dst, d_side)
            for j in range(sh):
                for i in range(sw):
                    self.px[dx + (dw - 1 - i), dy + j] = self.px[sx + i, sy + j]
                    if (sx + i, sy + j) in self.skinmask:
                        self.skinmask.add((dx + (dw - 1 - i), dy + j))

    def mirror_all(self):
        for a, b in (('rarm', 'larm'), ('rsleeve', 'lsleeve'), ('rleg', 'lleg'), ('rpants', 'lpants')):
            self.mirror(a, b)

    # -- checks -----------------------------------------------------------
    def coverage(self, part):
        n = 0
        for side in ('top', 'bottom', 'right', 'front', 'left', 'back'):
            fx, fy, fw, fh = self.face(part, side)
            n += sum(1 for j in range(fh) for i in range(fw) if self.px[fx + i, fy + j][3] > 0)
        return n

    def save(self, path):
        self.im.save(path)
