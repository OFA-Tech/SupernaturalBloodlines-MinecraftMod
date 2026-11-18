"""Tiny GeckoLib asset generator: bone/cube model -> geo.json, procedural hand-painted
texture (painted per texel from the 3D position on the cube surface) and animation.json.
Python 3 + Pillow only. Coordinates are Bedrock/GeckoLib model space: pixels, +Y up,
creature faces -Z, 'left' is +X."""
import json, math, copy
from PIL import Image


# ---------------------------------------------------------------- noise
def _h(ix, iy, iz, seed):
    n = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, z, seed=0):
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    def L(a, b, t): return a + (b - a) * t
    c = [[[_h(ix + i, iy + j, iz + k, seed) for k in (0, 1)] for j in (0, 1)] for i in (0, 1)]
    return L(L(L(c[0][0][0], c[0][0][1], fz), L(c[0][1][0], c[0][1][1], fz), fy),
             L(L(c[1][0][0], c[1][0][1], fz), L(c[1][1][0], c[1][1][1], fz), fy), fx)


def fbm(x, y, z, seed=0, oct=3):
    a, s, tot, amp = 0.0, 1.0, 0.0, 1.0
    for o in range(oct):
        a += vnoise(x * s, y * s, z * s, seed + o * 17) * amp
        tot += amp
        amp *= 0.5
        s *= 2.0
    return a / tot


def hrand(*v):
    n = 7
    for i, a in enumerate(v):
        if isinstance(a, str):
            a = sum(ord(ch) * (k + 1) for k, ch in enumerate(a)) % 9973
        n = (n * 31 + int(a * 1000) + i * 977) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def ramp(cols, t):
    t = max(0.0, min(0.9999, t))
    return cols[int(t * len(cols))]


# ---------------------------------------------------------------- model
FACES = ('north', 'east', 'south', 'west', 'up', 'down')


class Model:
    def __init__(self, ident, scale=1.0):
        self.ident, self.S = ident, scale
        self.bones, self.order, self.cubes = {}, [], []
        self.tw = self.th = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        S = self.S
        self.bones[name] = dict(name=name, parent=parent, pivot=[v * S for v in pivot], rot=list(rot))
        self.order.append(name)

    def cube(self, bone, name, o, s, mat='skin', den=1, infl=0.0, rot=None, pivot=None, tag=None):
        S = self.S
        self.cubes.append(dict(bone=bone, name=name, o=[v * S for v in o], s=[v * S for v in s], mat=mat,
                               den=den, infl=infl * S, rot=list(rot) if rot else None,
                               pivot=[v * S for v in pivot] if pivot else None, tag=tag))

    def mirror(self, suffix_from='_l', suffix_to='_r'):
        """Copy every bone/cube whose name ends in suffix_from to the other side."""
        for n in list(self.order):
            b = self.bones[n]
            if not n.endswith(suffix_from):
                continue
            nb = copy.deepcopy(b)
            nb['name'] = n[:-len(suffix_from)] + suffix_to
            if b['parent'] and b['parent'].endswith(suffix_from):
                nb['parent'] = b['parent'][:-len(suffix_from)] + suffix_to
            nb['pivot'][0] = -nb['pivot'][0]
            nb['rot'] = [nb['rot'][0], -nb['rot'][1], -nb['rot'][2]]
            self.bones[nb['name']] = nb
            self.order.append(nb['name'])
        for c in list(self.cubes):
            in_side_bone = c['bone'].endswith(suffix_from)
            if not (in_side_bone or c['name'].endswith(suffix_from)):   # centre-bone cubes named *_l are mirrored too
                continue
            nc = copy.deepcopy(c)
            nc['bone'] = c['bone'][:-len(suffix_from)] + suffix_to if in_side_bone else c['bone']
            nc['name'] = c['name'][:-len(suffix_from)] + suffix_to if c['name'].endswith(suffix_from) else c['name'] + suffix_to
            nc['o'][0] = -(c['o'][0] + c['s'][0])
            if c['pivot']:
                nc['pivot'][0] = -c['pivot'][0]
            if c['rot']:
                nc['rot'] = [c['rot'][0], -c['rot'][1], -c['rot'][2]]
            self.cubes.append(nc)

    def shift_y(self, dy, skip=('root',)):
        """Lift the whole model (e.g. to put the paws on y=0); bones in `skip` keep their pivot height."""
        dy *= self.S
        for n, b in self.bones.items():
            if n not in skip:
                b['pivot'][1] += dy
        for c in self.cubes:
            c['o'][1] += dy
            if c['pivot']:
                c['pivot'][1] += dy

    # ---- UV packing (cross layout per cube, shelf packed)
    def pack(self, W=256):
        items = []
        for c in self.cubes:
            d = c['den']
            w, h, dd = (max(1, math.ceil(c['s'][i] * d - 1e-6)) for i in (0, 1, 2))
            c['dim'] = (w, h, dd)
            items.append((c, 2 * (dd + w), dd + h))
        items.sort(key=lambda t: (-t[2], -t[1]))
        x = y = rowh = 0
        for c, fw, fh in items:
            if x + fw > W:
                x, y, rowh = 0, y + rowh, 0
            c['uv0'] = (x, y)
            x += fw
            rowh = max(rowh, fh)
        H = y + rowh
        p = 16
        while p < H:
            p *= 2
        self.tw, self.th = W, p
        for c in self.cubes:
            u, v = c['uv0']
            w, h, d = c['dim']
            c['rects'] = {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
                          'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
                          'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}

    # ---- geo.json
    def geo(self):
        bones = []
        for n in self.order:
            b = self.bones[n]
            e = dict(name=n)
            if b['parent']:
                e['parent'] = b['parent']
            e['pivot'] = [round(v, 3) for v in b['pivot']]
            if any(b['rot']):
                e['rotation'] = [round(v, 3) for v in b['rot']]
            cubes = []
            for c in self.cubes:
                if c['bone'] != n:
                    continue
                uv = {}
                for f in FACES:
                    u, v, w, h = c['rects'][f]
                    if f == 'down':
                        uv[f] = {'uv': [u, v + h], 'uv_size': [w, -h]}
                    else:
                        uv[f] = {'uv': [u, v], 'uv_size': [w, h]}
                ce = dict(origin=[round(v, 3) for v in c['o']], size=[round(v, 3) for v in c['s']], uv=uv)
                if c['infl']:
                    ce['inflate'] = round(c['infl'], 3)
                if c['rot']:
                    ce['pivot'] = [round(v, 3) for v in (c['pivot'] or b['pivot'])]
                    ce['rotation'] = [round(v, 3) for v in c['rot']]
                cubes.append(ce)
            if cubes:
                e['cubes'] = cubes
            bones.append(e)
        allx = [abs(c['o'][0]) for c in self.cubes] + [abs(c['o'][0] + c['s'][0]) for c in self.cubes]
        ymax = max(c['o'][1] + c['s'][1] for c in self.cubes)
        zs = [c['o'][2] for c in self.cubes] + [c['o'][2] + c['s'][2] for c in self.cubes]
        ext = max(max(allx) * 2, max(map(abs, zs)) * 2, ymax)
        return {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': 'geometry.' + self.ident, 'texture_width': self.tw,
                            'texture_height': self.th, 'visible_bounds_width': math.ceil(ext / 16) + 2,
                            'visible_bounds_height': math.ceil(ymax / 16) + 2,
                            'visible_bounds_offset': [0, round(ymax / 32, 2), 0]},
            'bones': bones}]}

    # ---- texture painting
    def face_point(self, c, f, fx, fy):
        # Blockbench/GeckoLib render with x mirrored: 'east' is the cube side at the file's -X.
        # Work in that mirrored space (xi = -x), then flip back to file x.
        sx, sy, sz = c['s']
        x0i, y0, z0 = -(c['o'][0] + sx), c['o'][1], c['o'][2]
        x1i, y1, z1 = x0i + sx, y0 + sy, z0 + sz
        if f == 'north': q = (x1i - fx * sx, y1 - fy * sy, z0)
        elif f == 'south': q = (x0i + fx * sx, y1 - fy * sy, z1)
        elif f == 'east': q = (x1i, y1 - fy * sy, z1 - fx * sz)
        elif f == 'west': q = (x0i, y1 - fy * sy, z0 + fx * sz)
        elif f == 'up': q = (x1i - fx * sx, y1, z1 - fy * sz)
        else: q = (x1i - fx * sx, y0, z0 + fy * sz)
        return (-q[0], q[1], q[2])

    def paint(self, shaders, glow=None):
        img = Image.new('RGBA', (self.tw, self.th), (0, 0, 0, 0))
        px = img.load()
        gimg = Image.new('RGBA', (self.tw, self.th), (0, 0, 0, 0))
        gpx = gimg.load()
        NORM = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0),
                'up': (0, 1, 0), 'down': (0, -1, 0)}
        for c in self.cubes:
            sh = shaders[c['mat']]
            for f in FACES:
                u, v, w, h = c['rects'][f]
                for j in range(h):
                    for i in range(w):
                        fx, fy = (i + 0.5) / w, (j + 0.5) / h
                        p = self.face_point(c, f, fx, fy)
                        lp = tuple((p[k] - c['o'][k]) / max(c['s'][k], 1e-6) for k in range(3))
                        ctx = dict(p=p, lp=lp, face=f, n=NORM[f], cube=c, fx=fx, fy=fy, i=i, j=j, w=w, h=h)
                        r = sh(ctx)
                        if len(r) == 4 and r[3] == 'glow':
                            gpx[u + i, v + j] = (*r[:3], 255)
                            r = r[:3]
                        a = 255
                        if len(r) == 4:
                            r, a = r[:3], r[3]
                        px[u + i, v + j] = (*[max(0, min(255, int(k))) for k in r], a)
        return img, gimg


# ---------------------------------------------------------------- animation
class Anim:
    def __init__(self, name, length, loop=True):
        self.name, self.length, self.loop = name, length, loop
        self.ch = {}

    def k(self, bone, chan, t, v, ease=None):
        d = self.ch.setdefault(bone, {}).setdefault(chan, [])
        d.append((round(t, 4), list(v), ease))
        return self

    def sine(self, bone, chan, base, amp, phase=0.0, n=12, cycles=1, ease='linear'):
        """Sampled sine over the loop; base/amp are 3-vectors. phase in cycles (0..1)."""
        for s in range(n * cycles + 1):
            t = self.length * s / (n * cycles)
            a = math.sin(2 * math.pi * (s / n + phase))
            self.k(bone, chan, t, [base[i] + amp[i] * a for i in range(3)], ease)
        return self

    def to_json(self):
        bones = {}
        for b, chans in self.ch.items():
            out = {}
            for ch, ks in chans.items():
                ks = sorted(ks, key=lambda x: x[0])
                m = {}
                for t, v, e in ks:
                    v = [round(x, 3) for x in v]
                    key = '%.4f' % t
                    key = key.rstrip('0').rstrip('.') if '.' in key else key
                    if '.' not in key:
                        key += '.0'
                    m[key] = {'vector': v, 'easing': e} if e and e != 'linear' else v
                out[ch] = m
            bones[b] = out
        loop = self.loop
        return {'loop': loop, 'animation_length': self.length, 'bones': bones}


def anim_file(anims):
    return {'format_version': '1.8.0', 'animations': {a.name: a.to_json() for a in anims}}


def save_json(path, obj):
    import os
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf8') as f:
        json.dump(obj, f, indent=1)
