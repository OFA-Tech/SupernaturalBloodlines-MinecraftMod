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
        cut = c.get('cut', FLUFF)
        amp = c.get('noise', 0.95)
        for x, y, p, f, lx, ly, fw, fh in self.faces():
            n = hsh(x, y // 2, self.seed) * 0.55 + hsh(x, y, self.seed + 7) * 0.45
            t = 0.5 + (n - 0.5) * amp
            t += (SHADE[f] - 1.0) * c.get('shade', 1.1)
            t += c.get('bias', {}).get(p, 0.0)
            if p.startswith(('leg', 'foot', 'arm', 'finger', 'thumb')):
                t -= 0.12 * (ly / max(1, fh))
            col = ramp(c.get('part_fur', {}).get(p, c['fur']), t)
            a = 255
            if p in cut and f not in ('up', 'down') and fh > 1:
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
    def eyes(self, core, halo, socket=None):
        o = self.face_origin('head', 'north')
        for lx in range(7):
            c = self.get(o[0] + lx, o[1] + 1)
            if c:
                self.local('head', 'north', lx, 1, tuple(int(v * self.cfg.get('brow_dark', 0.5)) for v in c[:3]))
        for lx0 in (1, 4):
            for dx in (0, 1):
                if socket:
                    self.local('head', 'north', lx0 + dx, 1, socket)
                self.local('head', 'north', lx0 + dx, 2, core)
                self.local('head', 'north', lx0 + dx, 3, halo)
            self.local('head', 'north', lx0 - 1 if lx0 == 1 else lx0 + 2, 2, halo)
        for part in ('head',):
            for face in ('east', 'west'):
                self.local(part, face, 0 if face == 'west' else 5, 2, halo)

    def teeth(self, bone, shadow):
        if self.cfg.get('fangs'):
            return self.fangs(bone, shadow)
        for part in ('snoutTeeth', 'jawTeeth'):
            for x, y, p, f, lx, ly, fw, fh in self.faces({part}):
                if f in ('up', 'down'):
                    continue
                fang = lx % 2 == 0 or f in ('east', 'west')
                col = bone if fang else mix(bone, shadow, 0.55)
                if ly == 0:
                    col = mix(col, shadow, 0.35)  # gum line
                self.put(x, y, col)

    def fangs(self, bone, gum):
        """Snout with a jagged white row of fangs over a dark mouth line."""
        dark = (34, 6, 12)
        for part in ('snoutTeeth', 'jawTeeth'):
            for x, y, p, f, lx, ly, fw, fh in self.faces({part}):
                if f in ('up', 'down'):
                    continue
                tip = lx % 2 == 0
                if part == 'snoutTeeth':
                    if ly == 0:
                        col = (52, 44, 62)
                    else:
                        col = (244, 240, 226) if tip else dark
                else:
                    col = (244, 240, 226) if tip else dark
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
        # cheek guards: the flat sideburn planes become bone plates
        for part in ('headSidburnLeft', 'headSidburnRight'):
            for x, y, pt, f, lx, ly, fw, fh in self.faces({part}):
                self.put(x, y, edge if (lx == fw - 1 or ly == fh - 1 or lx == 0) else bone)
        # bone crack lines
        rng = random.Random(self.seed + 99)
        for x, y, p, f, lx, ly, fw, fh in self.faces({'head', 'snout', 'jaw'}):
            c = self.get(x, y)
            if c and tuple(c[:3]) == tuple(bone) and rng.random() < 0.12:
                self.put(x, y, crack)

    def wrinkles(self, col, parts, step=2, p=0.8, seed=21):
        """Horizontal creases (wrinkled snout, brow)."""
        for x, y, pt, f, lx, ly, fw, fh in self.faces(parts, {'north', 'east', 'west', 'up'}):
            if ly % step == 1 and hsh(lx, ly, self.seed + seed) < p and self.get(x, y)[3] > 0:
                self.put(x, y, mix(tuple(self.get(x, y)[:3]), col, 0.7))

    def brow(self, ridge, shadow):
        o = self.face_origin('head', 'north')
        for lx in range(7):
            self.local('head', 'north', lx, 0, ridge)
            if lx not in (0, 3, 6):
                self.local('head', 'north', lx, 1, shadow)
        for face in ('east', 'west'):
            self.rect('head', face, 0, 0, 6, 1, ridge)

    def strands(self, parts, cols, density=0.5, seed=31):
        """Hair strand texture: vertical streaks in the given ramp (swept-back hair, mane)."""
        for x, y, p, f, lx, ly, fw, fh in self.faces(parts):
            c = self.get(x, y)
            if not c or c[3] == 0:
                continue
            n = hsh(x, 0, self.seed + seed) * 0.6 + hsh(x, y // 3, self.seed + seed + 1) * 0.4
            t = (n - 0.5) * density * 2 + 0.5 + (SHADE[f] - 1.0)
            self.put(x, y, ramp(cols, t), c[3])

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
    t.eyes(cfg['eye'], cfg['eye_halo'], cfg.get('socket'))
    return t.write(path)


# ---------------------------------------------------------------- variants
MANE = ['neck', 'neckFluff', 'neckFluffLeft', 'neckFluffRight', 'neckFluffBottom', 'headFluff', 'headSidburnLeft', 'headSidburnRight']
DEMON_HAIR = {'headFluff', 'neckFluff', 'neckFluffLeft', 'neckFluffRight', 'neckFluffBottom'}
HAIR_RAMP = [(7, 7, 13), (16, 16, 26), (28, 28, 42), (44, 44, 60)]


def demon_detail(t):
    # skin, not fur: smooth blue-grey with muscle definition, creases and a heavy brow ridge
    t.patches((34, 42, 58), {'body', 'hip', 'armRight', 'armLeft', 'armRight2', 'armLeft2', 'legRight', 'legLeft', 'legRight2', 'legLeft2'}, 34, 2, 1)
    t.patches((110, 124, 146), {'body', 'neck', 'armRight', 'armLeft', 'legRight', 'legLeft'}, 16, 1, 2, face={'up', 'north'})
    t.veins((40, 50, 72), {'body', 'neck', 'armRight', 'armLeft', 'armRight2', 'armLeft2', 'legRight', 'legLeft', 'head'}, 36, 8, 3)
    t.wrinkles((26, 30, 44), {'snout', 'nose', 'head', 'neck'}, step=2, p=0.85)
    t.wrinkles((30, 34, 50), {'jaw', 'armRight', 'armLeft', 'body'}, step=3, p=0.5, seed=7)
    t.strands(DEMON_HAIR, HAIR_RAMP, density=0.55)


def demon_late(t):
    t.brow((112, 126, 150), (52, 62, 84))
    t.patches((84, 40, 52), {'earLeft', 'earRight', 'earLeft2', 'earRight2'}, 4, 1, 9)


DEMON = dict(
    seed=11, noise=0.5, shade=1.5, cut=DEMON_HAIR,
    fur=[(38, 46, 62), (56, 68, 88), (76, 90, 112), (98, 112, 134), (122, 136, 156)],
    part_fur={p: HAIR_RAMP for p in DEMON_HAIR},
    bias={'bodyFluff': 0.06, 'jawFluff': 0.04, 'snout': 0.03},
    eye=(255, 40, 32), eye_halo=(176, 18, 22), socket=None, brow_dark=0.85, fangs=True, nose=(62, 56, 78), mouth=(96, 14, 22), tongue=(120, 24, 34),
    teeth=(232, 228, 212), teeth_shadow=(110, 100, 96), ear=(92, 42, 58), claw=(22, 24, 34), pad=(26, 30, 44),
    detail=demon_detail, late=demon_late)


def peter_detail(t):
    t.patches((14, 18, 26), None, 20, 3, 1)
    t.patches((86, 96, 116), {'snout', 'jaw', 'head', 'armRight2', 'armLeft2', 'legRight2', 'legLeft2'}, 8, 1, 2, face={'up', 'north'})
    t.veins((6, 8, 12), None, 24, 7, 3)
    t.wrinkles((12, 14, 22), {'snout', 'nose'}, step=2, p=0.9)  # snarl
    # shaggy black mane over the neck, shoulders and back
    t.strands(MANE, [(3, 3, 5), (9, 9, 13), (17, 17, 24), (30, 30, 42)], density=0.8)


def peter_late(t):
    mane = [(3, 3, 5), (9, 9, 13), (17, 17, 24), (30, 30, 42)]
    for x, y, p, f, lx, ly, fw, fh in t.faces({'body', 'hip', 'tail', 'tail2', 'tail3'}, {'up'}):
        c = t.get(x, y)
        if c and c[3]:
            t.put(x, y, ramp(mane, hsh(x, 0, 71) * 0.6 + hsh(x, y // 3, 72) * 0.4 + 0.1))
    for x, y, p, f, lx, ly, fw, fh in t.faces({'body'}, {'east', 'west', 'north', 'south'}):
        c = t.get(x, y)
        if c and c[3] and ly < 4:
            t.put(x, y, ramp(mane, hsh(x, 0, 73) * 0.6 + 0.1))


PETER = dict(
    seed=23, noise=1.0, shade=1.15,
    fur=[(26, 32, 46), (40, 50, 68), (58, 70, 92), (80, 94, 118), (104, 120, 146)],
    part_fur={p: [(3, 3, 5), (9, 9, 13), (17, 17, 24), (30, 30, 42)] for p in MANE},
    bias={'snout': 0.08, 'jaw': 0.06, 'bodyFluff': 0.0},
    eye=(255, 36, 30), eye_halo=(160, 14, 16), socket=(6, 6, 10), nose=(6, 6, 8), mouth=(92, 12, 18), tongue=(130, 24, 30),
    teeth=(244, 242, 234), teeth_shadow=(122, 116, 110), ear=(40, 30, 40), claw=(238, 236, 226), pad=(20, 20, 28),
    detail=peter_detail, late=peter_late)


def klaus_detail(t):
    t.patches((76, 52, 34), {'body', 'neck', 'hip', 'tail', 'tail2', 'tail3', 'head', 'armRight', 'armLeft', 'legRight', 'legLeft'}, 12, 3, 1, face={'up'})
    t.patches((214, 184, 138), {'bodyFluff', 'neckFluffBottom', 'jawFluff', 'snout', 'jaw'}, 8, 2, 2)
    # black hybrid veins radiating from the eyes, plus a few on the limbs
    t.veins((8, 6, 6), {'head', 'snout'}, 26, 7, 3)
    t.veins((8, 6, 6), {'neck', 'body', 'armRight', 'armLeft', 'armRight2', 'armLeft2', 'legRight', 'legLeft'}, 20, 8, 7)


KLAUS = dict(
    seed=37, noise=0.7,
    fur=[(92, 64, 42), (120, 86, 56), (148, 110, 72), (176, 138, 92), (204, 168, 118)],
    bias={'bodyFluff': 0.2, 'jawFluff': 0.16, 'snout': 0.08, 'jaw': 0.1, 'neckFluffBottom': 0.18},
    eye=(255, 222, 40), eye_halo=(214, 160, 20), socket=(14, 8, 6), nose=(22, 12, 10), mouth=(130, 36, 38), tongue=(170, 62, 66),
    teeth=(244, 238, 216), teeth_shadow=(130, 108, 86), ear=(150, 96, 84), claw=(54, 44, 38), pad=(52, 36, 28),
    detail=klaus_detail)


def beast_detail(t):
    t.patches((8, 6, 6), None, 22, 4, 1)
    t.patches((122, 82, 48), {'body', 'neck', 'hip', 'armRight', 'armLeft', 'legRight', 'legLeft', 'head', 'tail'}, 16, 3, 2, face={'up'})  # russet back streaks
    t.patches((186, 160, 120), {'bodyFluff', 'neckFluffBottom'}, 6, 3, 4)  # pale underbelly mark
    t.veins((150, 18, 24), {'armRight', 'armLeft', 'armRight2', 'armLeft2', 'legRight', 'legLeft', 'legRight2', 'legLeft2', 'neck', 'body', 'hip', 'head'}, 56, 10, 3, glow=(68, 10, 12))


def beast_late(t):
    t.plates((232, 224, 200), (92, 74, 56), (60, 46, 36))


BEAST = dict(
    seed=53, noise=0.9, shade=1.3,
    fur=[(10, 8, 8), (24, 17, 13), (44, 30, 21), (74, 50, 32), (112, 76, 46)],
    bias={'bodyFluff': 0.1, 'neckFluff': 0.06},
    eye=(236, 248, 255), eye_halo=(116, 178, 248), socket=(4, 4, 8), nose=(12, 8, 8), mouth=(84, 14, 16), tongue=(104, 18, 22),
    teeth=(222, 212, 186), teeth_shadow=(70, 56, 44), ear=(70, 30, 28), claw=(170, 160, 138), pad=(26, 14, 12),
    detail=beast_detail, late=beast_late)

OUT = {'deucalion_demon_wolf': DEMON, 'peter_hale_wolf': PETER, 'klaus_hybrid': KLAUS, 'beast_of_gevaudan': BEAST}

if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, cfg in OUT.items():
        make(cfg, os.path.join(out, name + '.png'))
        print('wrote', name)
