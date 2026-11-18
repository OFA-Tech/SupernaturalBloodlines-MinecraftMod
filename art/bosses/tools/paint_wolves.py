"""Paints the four werewolf-model boss textures (128x64) on the exact UV layout of
the Werewolves mod beast model. Run: python paint_wolves.py <out_dir>"""
import sys, os, random, math
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wolf_uv import build_info, W, H, MODEL

INFO = build_info()
FLUFF = {'neckFluff', 'neckFluffLeft', 'neckFluffRight', 'neckFluffBottom', 'bodyFluff',
         'headFluff', 'jawFluff', 'headSidburnLeft', 'headSidburnRight'}
SHADE = {'up': 1.12, 'north': 1.0, 'south': 0.94, 'east': 0.9, 'west': 0.9, 'down': 0.7}


def hsh(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    return cols[i]  # hard steps keep it pixel-art


class Tex:
    def __init__(self, cfg):
        self.cfg = cfg
        self.px = {}      # (x,y) -> [r,g,b,a]
        self.seed = cfg['seed']

    def faces(self, parts=None, face=None):
        for (x, y), (p, f, lx, ly, fw, fh) in INFO.items():
            if (parts is None or p in parts) and (face is None or f in face):
                yield x, y, p, f, lx, ly, fw, fh

    def base(self):
        c = self.cfg
        for x, y, p, f, lx, ly, fw, fh in self.faces():
            n = hsh(x, y // 2, self.seed) * 0.55 + hsh(x, y, self.seed + 7) * 0.45
            t = 0.5 + (n - 0.5) * 0.95
            t += (SHADE[f] - 1.0) * 1.1
            t += c.get('bias', {}).get(p, 0.0)
            if p.startswith(('leg', 'foot', 'arm', 'finger', 'thumb')):
                t -= 0.12 * (ly / max(1, fh))
            col = ramp(c['fur'], t)
            a = 255
            if p in FLUFF and f not in ('up', 'down') and fh > 1:
                tt = ly / (fh - 1)
                if tt > 0.45 and hsh(x, y, self.seed + 3) < (tt - 0.35) * 1.3:
                    a = 0
            self.px[(x, y)] = [*col, a]

    def put(self, x, y, col, a=255):
        if (x, y) in self.px:
            self.px[(x, y)] = [*col, a]

    def get(self, x, y):
        return self.px.get((x, y))

    def face_origin(self, part, face):
        for (x, y), (p, f, lx, ly, fw, fh) in INFO.items():
            if p == part and f == face and lx == 0 and ly == 0:
                return x, y, fw, fh
        return None

    def local(self, part, face, lx, ly, col, a=255):
        o = self.face_origin(part, face)
        if o and 0 <= lx < o[2] and 0 <= ly < o[3]:
            self.put(o[0] + lx, o[1] + ly, col, a)

    def rect(self, part, face, lx, ly, w, h, col, a=255):
        for j in range(h):
            for i in range(w):
                self.local(part, face, lx + i, ly + j, col, a)

    # ---- details ----
    def eyes(self, core, halo):
        for lx in (1, 5):
            self.local('head', 'north', lx, 2, core)
            self.local('head', 'north', lx, 3, halo)
            self.local('head', 'north', lx - 1 if lx == 5 else lx + 1, 2, halo)
        for lx in (0, 2, 4, 6):  # brow shadow
            pass
        for lx in range(7):
            c = self.get(*(lambda o: (o[0] + lx, o[1] + 1))(self.face_origin('head', 'north')))
            if c and lx not in (1, 5):
                self.local('head', 'north', lx, 1, tuple(int(v * 0.55) for v in c[:3]))

    def teeth(self, bone, shadow):
        for part in ('snoutTeeth', 'jawTeeth'):
            for x, y, p, f, lx, ly, fw, fh in self.faces({part}):
                if f in ('up', 'down'):
                    continue
                fang = lx % 2 == 0 or f in ('east', 'west')
                col = bone if fang else mix(bone, shadow, 0.55)
                if ly == 0:
                    col = mix(col, shadow, 0.35)  # gum line
                self.put(x, y, col)

    def mouth(self, inner, tongue):
        for x, y, p, f, lx, ly, fw, fh in self.faces({'snout', 'jaw', 'jawFluff'}, {'down', 'up'}):
            if p == 'snout' and f == 'down':
                self.put(x, y, mix(inner, (20, 4, 6), hsh(x, y, 5) * 0.5))
            if p == 'jaw' and f == 'up':
                self.put(x, y, mix(tongue, (30, 5, 8), hsh(x, y, 6) * 0.5))

    def nose(self, col):
        for x, y, p, f, lx, ly, fw, fh in self.faces({'nose'}):
            self.put(x, y, mix(col, (0, 0, 0), hsh(x, y, 9) * 0.3))
        self.rect('snout', 'north', 1, 0, 2, 1, col)

    def ears(self, inner):
        for x, y, p, f, lx, ly, fw, fh in self.faces({'earLeft', 'earRight', 'earLeft2', 'earRight2'}, {'north', 'south'}):
            if 0 < lx < fw - 1 and ly > 0:
                self.put(x, y, mix(inner, (0, 0, 0), hsh(x, y, 11) * 0.35))

    def claws(self, claw, pad):
        for x, y, p, f, lx, ly, fw, fh in self.faces():
            if p.startswith('finger') or p.startswith('thumb'):
                if ly >= fh - 1 and f not in ('up', 'down'):
                    self.put(x, y, mix(claw, (0, 0, 0), hsh(x, y, 13) * 0.35))
                if f == 'down':
                    self.put(x, y, claw)
            if p.startswith('foot'):
                if f == 'down':
                    self.put(x, y, mix(pad, (0, 0, 0), hsh(x, y, 14) * 0.4))
                if f in ('north', 'east', 'west') and ly == fh - 1 and lx % 2 == 0:
                    self.put(x, y, claw)

    def veins(self, col, parts, count, length, seed, a=255, glow=None):
        rng = random.Random(self.seed * 31 + seed)
        faces = [k for k in self.faces(parts) if k[2:5] == k[2:5]]
        origins = {}
        for x, y, p, f, lx, ly, fw, fh in faces:
            if lx == 0 and ly == 0 and fw > 2 and fh > 2:
                origins[(p, f)] = (x, y, fw, fh)
        keys = list(origins)
        for _ in range(count):
            p, f = rng.choice(keys)
            x0, y0, fw, fh = origins[(p, f)]
            x, y = rng.randrange(fw), rng.randrange(fh)
            dx, dy = rng.choice([(1, 0), (0, 1), (1, 1), (-1, 1), (0, 1)])
            for _s in range(length):
                if not (0 <= x < fw and 0 <= y < fh):
                    break
                cur = self.get(x0 + x, y0 + y)
                if cur and cur[3] > 0:
                    self.put(x0 + x, y0 + y, col, a)
                    if glow:
                        for gx, gy in ((x + 1, y), (x - 1, y)):
                            if 0 <= gx < fw and 0 <= gy < fh and rng.random() < 0.5:
                                cc = self.get(x0 + gx, y0 + gy)
                                if cc and cc[3] > 0:
                                    self.put(x0 + gx, y0 + gy, mix(tuple(cc[:3]), glow, 0.55))
                r = rng.random()
                if r < 0.25:
                    dx, dy = rng.choice([(1, 0), (0, 1), (-1, 1), (1, 1), (-1, 0)])
                elif r < 0.33:  # branch
                    bx, by = x + rng.choice((-1, 1)), y + 1
                    if 0 <= bx < fw and 0 <= by < fh:
                        self.put(x0 + bx, y0 + by, col, a)
                x += dx; y += dy

    def patches(self, col, parts, count, size, seed, face=None):
        rng = random.Random(self.seed * 17 + seed)
        origins = {}
        for x, y, p, f, lx, ly, fw, fh in self.faces(parts, face):
            if lx == 0 and ly == 0 and fw > 2 and fh > 2:
                origins[(p, f)] = (x, y, fw, fh)
        keys = list(origins)
        for _ in range(count):
            p, f = rng.choice(keys)
            x0, y0, fw, fh = origins[(p, f)]
            cx, cy = rng.randrange(fw), rng.randrange(fh)
            r = rng.randint(1, size)
            for j in range(-r, r + 1):
                for i in range(-r, r + 1):
                    if i * i + j * j <= r * r + rng.random() * 2 and 0 <= cx + i < fw and 0 <= cy + j < fh:
                        cur = self.get(x0 + cx + i, y0 + cy + j)
                        if cur and cur[3] > 0:
                            self.put(x0 + cx + i, y0 + cy + j, mix(col, tuple(cur[:3]), rng.random() * 0.35), cur[3])

    def plates(self, bone, edge, crack):
        """Bony face plates (Beast of Gevaudan)."""
        H_ = 'head'
        # forehead / brow band (leaves the eye row open)
        self.rect(H_, 'north', 0, 0, 7, 2, bone)
        self.rect(H_, 'north', 2, 2, 3, 3, bone)
        self.rect(H_, 'north', 0, 0, 7, 1, edge)
        self.rect(H_, 'north', 3, 1, 1, 4, edge)
        # skull cap segments
        self.rect(H_, 'up', 1, 0, 5, 6, bone)
        for ly in (1, 3, 5):
            self.rect(H_, 'up', 1, ly, 5, 1, edge)
        self.rect(H_, 'up', 3, 0, 1, 6, edge)
        # cheek plates
        for face in ('east', 'west'):
            self.rect(H_, face, 0, 0, 5, 5, bone)
            self.rect(H_, face, 0, 0, 5, 1, edge)
            self.rect(H_, face, 4, 0, 1, 5, edge)
            self.rect(H_, face, 0, 2, 3, 1, edge)
            self.rect('jaw', face, 0, 0, 4, 2, bone)
        for face in ('east', 'west'):
            self.rect('body', face, 0, 0, 4, 3, bone); self.rect('body', face, 0, 2, 4, 1, edge)
            for part in ('armRight', 'armLeft'):
                self.rect(part, face, 0, 0, 4, 3, bone); self.rect(part, face, 0, 2, 4, 1, edge)
        # muzzle ridge
        self.rect('snout', 'up', 1, 0, 2, 4, bone)
        self.rect('snout', 'up', 1, 2, 2, 1, edge)
        self.rect('snout', 'east', 0, 0, 4, 1, bone)
        self.rect('snout', 'west', 0, 0, 4, 1, bone)
        self.rect('jaw', 'east', 0, 0, 4, 1, bone)
        self.rect('jaw', 'west', 0, 0, 4, 1, bone)
        # vertebra ridge down neck, body and tail
        for part, step in (('neck', 2), ('body', 2), ('hip', 2), ('tail', 2), ('tail2', 2)):
            o = self.face_origin(part, 'up')
            if o:
                for ly in range(0, o[3], step):
                    self.rect(part, 'up', max(0, o[2] // 2 - 1), ly, 2 if o[2] > 3 else 1, 1, bone)
        for part in ('neck', 'body', 'hip'):
            o = self.face_origin(part, 'north') or self.face_origin(part, 'south')
        # bone crack lines
        rng = random.Random(self.seed + 99)
        for x, y, p, f, lx, ly, fw, fh in self.faces({'head', 'snout', 'jaw'}):
            c = self.get(x, y)
            if c and tuple(c[:3]) == tuple(bone) and rng.random() < 0.12:
                self.put(x, y, crack)

    def write(self, path):
        im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), tuple(c))
        im.save(path)
        return im


def make(cfg, path):
    t = Tex(cfg)
    t.base()
    cfg['detail'](t)
    t.nose(cfg['nose'])
    t.mouth(cfg['mouth'], cfg['tongue'])
    t.teeth(cfg['teeth'], cfg['teeth_shadow'])
    t.ears(cfg['ear'])
    t.claws(cfg['claw'], cfg['pad'])
    cfg.get('late', lambda t: None)(t)
    t.eyes(cfg['eye'], cfg['eye_halo'])
    return t.write(path)


# ---------------------------------------------------------------- variants
def demon_detail(t):
    # charred, split skin: bare scorched patches and dull ember cracks
    t.patches((52, 40, 38), None, 26, 3, 1)
    t.patches((86, 38, 30), {'body', 'neck', 'armRight', 'armLeft', 'legRight', 'legLeft', 'head'}, 14, 2, 2)
    t.veins((170, 52, 22), None, 70, 9, 3, glow=(95, 24, 14))
    t.veins((236, 96, 30), {'body', 'armRight', 'armLeft', 'head', 'legRight', 'legLeft'}, 22, 5, 4)
    t.patches((140, 135, 128), {'head', 'neck', 'body'}, 8, 1, 5)  # ash dust


DEMON = dict(
    seed=11,
    fur=[(10, 8, 8), (20, 15, 14), (33, 25, 23), (49, 38, 34), (70, 55, 48)],
    bias={'bodyFluff': 0.05, 'jawFluff': 0.0},
    eye=(255, 40, 28), eye_halo=(150, 14, 12), nose=(30, 14, 14), mouth=(95, 16, 16), tongue=(120, 24, 24),
    teeth=(196, 186, 160), teeth_shadow=(86, 70, 58), ear=(80, 36, 32), claw=(64, 58, 54), pad=(34, 16, 14),
    detail=demon_detail)


def peter_detail(t):
    t.patches((150, 88, 80), {'body', 'armLeft', 'armRight', 'neck', 'head', 'hip', 'legLeft'}, 18, 3, 1)   # burn scarring
    t.patches((196, 128, 116), {'head', 'armLeft', 'neck', 'body'}, 10, 2, 2)
    t.veins((24, 14, 14), None, 40, 7, 3)
    # matted pale muzzle and chest
    t.patches((120, 112, 100), {'snout', 'jaw', 'bodyFluff', 'neckFluffBottom', 'jawFluff'}, 12, 2, 4)
    # claw-mark slashes
    t.veins((166, 70, 60), {'head', 'body', 'neck'}, 10, 6, 5)


PETER = dict(
    seed=23,
    fur=[(44, 40, 38), (70, 63, 58), (98, 90, 82), (128, 118, 106), (160, 148, 134)],
    bias={'bodyFluff': 0.14, 'jawFluff': 0.1, 'snout': 0.06, 'jaw': 0.06},
    eye=(235, 28, 24), eye_halo=(130, 16, 14), nose=(18, 12, 12), mouth=(110, 22, 22), tongue=(150, 40, 44),
    teeth=(222, 214, 190), teeth_shadow=(104, 88, 72), ear=(120, 70, 64), claw=(92, 84, 76), pad=(46, 28, 26),
    detail=peter_detail)


def klaus_detail(t):
    # darker saddle along the back and legs: lighter belly already via bias
    t.patches((78, 54, 36), {'body', 'neck', 'hip', 'tail', 'tail2', 'tail3', 'head'}, 14, 3, 1, face={'up', 'east', 'west'})
    t.patches((206, 176, 130), {'bodyFluff', 'neckFluffBottom', 'jawFluff', 'snout', 'jaw'}, 10, 2, 2)
    # black hybrid veins, longest from the eyes and across the chest and limbs
    t.veins((10, 8, 8), None, 90, 11, 3)
    t.veins((10, 8, 8), {'head'}, 22, 6, 6)
    t.veins((10, 8, 8), {'armRight', 'armLeft', 'armRight2', 'armLeft2', 'legRight', 'legLeft'}, 24, 8, 7)


KLAUS = dict(
    seed=37,
    fur=[(76, 52, 34), (104, 74, 48), (132, 98, 64), (160, 124, 82), (188, 152, 108)],
    bias={'bodyFluff': 0.2, 'jawFluff': 0.16, 'snout': 0.08, 'jaw': 0.1, 'neckFluffBottom': 0.18},
    eye=(255, 224, 40), eye_halo=(190, 150, 20), nose=(24, 14, 12), mouth=(130, 36, 38), tongue=(170, 62, 66),
    teeth=(236, 228, 204), teeth_shadow=(120, 100, 80), ear=(150, 96, 84), claw=(60, 50, 44), pad=(52, 36, 28),
    detail=klaus_detail)


def beast_detail(t):
    t.patches((10, 7, 6), None, 26, 4, 1)
    t.patches((70, 48, 34), {'body', 'neck', 'hip', 'armRight', 'armLeft', 'legRight', 'legLeft'}, 22, 3, 2)
    # dull red veins, thick, on limbs, neck and chest
    t.veins((126, 14, 18), {'armRight', 'armLeft', 'armRight2', 'armLeft2', 'legRight', 'legLeft', 'legRight2', 'legLeft2', 'neck', 'body', 'hip'}, 46, 9, 3, glow=(52, 8, 10))


def beast_late(t):
    t.plates((228, 218, 192), (112, 94, 72), (74, 60, 48))


BEAST = dict(
    seed=53,
    fur=[(14, 10, 8), (34, 23, 16), (56, 39, 27), (82, 58, 40), (112, 82, 57)],
    bias={'bodyFluff': 0.08, 'neckFluff': 0.06},
    eye=(226, 242, 255), eye_halo=(112, 170, 235), nose=(14, 9, 9), mouth=(84, 14, 16), tongue=(104, 18, 22),
    teeth=(206, 196, 172), teeth_shadow=(72, 58, 46), ear=(66, 30, 28), claw=(150, 140, 120), pad=(26, 14, 12),
    detail=beast_detail, late=beast_late)

OUT = {'deucalion_demon_wolf': DEMON, 'peter_hale_wolf': PETER, 'klaus_hybrid': KLAUS, 'beast_of_gevaudan': BEAST}

if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, cfg in OUT.items():
        make(cfg, os.path.join(out, name + '.png'))
        print('wrote', name)
