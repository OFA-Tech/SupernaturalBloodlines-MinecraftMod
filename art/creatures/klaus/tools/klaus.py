"""Klaus Mikaelson's hybrid wolf form (TVD / The Originals): a large ash-brown wolf with golden hybrid eyes and a double set of fangs.
Run: python klaus.py <repo_root>   (uses ../../wolves/tools/wolflib.py)"""
import sys, os, math
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'wolves', 'tools'))
from wolflib import *

IDENT = 'klaus_hybrid'
B = (19.0, 0.0)          # body pivot as (y, z)
FORE, HIND = [], []      # leg joints as (y, z), filled by build(): shoulder, elbow, wrist / hip, knee, hock, ankle


def V(x=0, y=0, z=0): return [x, y, z]


def R(v, a):
    """Rotate a (y, z) offset by a file-space X rotation: +a tips a nose down and swings a hanging limb back."""
    a = math.radians(a)
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c + v[1] * s, v[1] * c - v[0] * s)


def build():
    m = Model(IDENT)
    m.bone('root', None, (0, 0, 11))
    m.bone('body', 'root', (0, B[0], B[1]))
    m.cube('body', 'chest', (-5.5, 11.5, -13), (11, 12.5, 13), 'fur')
    m.cube('body', 'brisket', (-4.2, 10.2, -11.5), (8.4, 2, 9), 'belly')
    m.cube('body', 'abdomen', (-4.6, 14, -1), (9.2, 9.6, 9.5), 'fur')
    m.cube('body', 'rump', (-5, 13.2, 8), (10, 10.6, 7), 'fur')
    m.cube('body', 'withers', (-4, 23.6, -12), (8, 1.4, 9), 'fur')
    # neck is modelled level and raised 28 degrees; the head takes 34 back so the muzzle points 6 degrees down
    m.bone('neck', 'body', (0, 20, -11), V(-28, 0, 0))
    m.cube('neck', 'neck', (-3.6, 16.2, -21), (7.2, 8.4, 11), 'fur')
    m.bone('head', 'neck', (0, 21.5, -20), V(34, 0, 0))
    m.cube('head', 'skull', (-4, 18, -26.5), (8, 7.5, 8), 'fur', den=2)
    m.cube('head', 'snout', (-2.3, 18.2, -33.5), (4.6, 3.9, 7.5), 'snout', den=2)
    m.cube('head', 'bridge', (-1.7, 21.9, -33), (3.4, 1.3, 7), 'snout', den=2)
    m.cube('head', 'nose', (-1.3, 20.6, -34.3), (2.6, 1.9, 1.0), 'nose', den=2)
    m.cube('head', 'lip_up', (-2.6, 17.75, -33.2), (5.2, 0.7, 7.1), 'lip')
    m.cube('head', 'fang_up_l', (1.95, 14.9, -32.4), (0.85, 2.9, 0.9), 'tooth', den=2)      # the long vampire fang
    m.cube('head', 'fang_up2_l', (1.95, 16.0, -30.4), (0.8, 1.8, 0.9), 'tooth', den=2)      # the werewolf fang behind it
    m.cube('head', 'molar_up_l', (2.0, 16.5, -28.9), (0.7, 1.2, 2.2), 'tooth', den=2)
    m.cube('head', 'incisors_up', (-1.1, 17.0, -33.2), (2.2, 0.6, 0.6), 'tooth', den=2)
    m.cube('head', 'brow_l', (0.5, 23.3, -27.3), (3.6, 1.4, 2.4), 'fur', den=2, rot=V(0, 0, -12), pivot=(2.3, 24, -26.5))
    m.cube('head', 'eye_l', (1.3, 21.9, -26.85), (2.4, 1.4, 0.7), 'eye', den=3, rot=V(0, 0, -14), pivot=(2.5, 22.6, -26.5))
    m.cube('head', 'vein_l', (2.1, 19.6, -26.75), (1.9, 2.3, 0.4), 'vein', den=4)
    m.cube('head', 'cheek_l', (3.3, 17.6, -25.5), (1.6, 4.4, 5.5), 'fur')
    m.cube('head', 'cheek_tuft_l', (3.4, 15.4, -24.4), (2.0, 4.8, 4.6), 'ruff', rot=V(0, 0, -18), pivot=(4.3, 19.8, -22))
    m.bone('jaw', 'head', (0, 18.3, -24.5), V(6, 0, 0))
    m.cube('jaw', 'jaw', (-1.9, 16.1, -32.8), (3.8, 2.2, 9.3), 'snout', den=2)
    m.cube('jaw', 'chin', (-2.1, 15.7, -32.3), (4.2, 1.0, 8), 'snout')
    m.cube('jaw', 'fang_lo_l', (0.8, 18.3, -32.6), (0.8, 1.9, 0.8), 'tooth', den=2)
    m.cube('jaw', 'molar_lo_l', (1.0, 18.3, -29.5), (0.7, 0.9, 2.4), 'tooth', den=2)
    m.cube('jaw', 'incisors_lo', (-0.9, 18.3, -32.7), (1.8, 0.8, 0.5), 'tooth', den=2)
    m.cube('jaw', 'tongue', (-1.1, 18.0, -31.4), (2.2, 0.5, 7), 'tongue', den=2)
    m.bone('ear_l', 'head', (2.7, 25.2, -20.5), V(-8, 0, 16))
    m.cube('ear_l', 'ear_base_l', (1.2, 25.0, -21.7), (3.2, 2.4, 1.8), 'ear', den=2)
    m.cube('ear_l', 'ear_mid_l', (1.7, 27.2, -21.5), (2.2, 1.7, 1.5), 'ear', den=2)
    m.cube('ear_l', 'ear_tip_l', (2.2, 28.7, -21.3), (1.2, 1.1, 1.2), 'ear', den=2)

    def seg(bone, name, x0, w, d, joint, L, ang, mat):
        """A leg segment hanging from `joint` (y, z), swung `ang` degrees; returns the joint at its far end."""
        m.cube(bone, name, (x0, joint[0] - L - 0.4, joint[1] - d / 2), (w, L + 1.2, d), mat, rot=V(ang, 0, 0), pivot=(x0 + w / 2, joint[0], joint[1]))
        v = R((-L, 0), ang)
        return (joint[0] + v[0], joint[1] + v[1])

    def paw(bone, j, x0, w):
        z = j[1] - 3.8
        m.cube(bone, bone, (x0, 0, z), (w, 2.1, 5.4), 'fur_leg')
        for k in range(4):
            x = x0 + 0.05 + k * (w - 0.1) / 4
            m.cube(bone, 'toe%d_l' % k, (x, 0, z - 1.5), ((w - 0.1) / 4 - 0.1, 1.3, 1.7), 'fur_leg')
            m.cube(bone, 'claw%d_l' % k, (x + 0.1, 0, z - 2.7), ((w - 0.1) / 4 - 0.3, 0.8, 1.3), 'claw')

    sh = (19.5, -8.5)
    m.bone('foreleg_l', 'body', (4.8, sh[0], sh[1]))
    m.cube('foreleg_l', 'shoulder_l', (2.6, 15, -12.2), (4.2, 8.5, 7.2), 'fur')
    el = seg('foreleg_l', 'upperarm_l', 2.9, 3.8, 4.6, sh, 8.2, 14, 'fur_leg')
    m.bone('foreleg2_l', 'foreleg_l', (4.8, el[0], el[1]))
    wr = seg('foreleg2_l', 'forearm_l', 3.2, 3.2, 3.6, el, 9.6, -5, 'fur_leg')
    m.bone('paw_l', 'foreleg2_l', (4.8, wr[0], wr[1]))
    paw('paw_l', wr, 2.9, 3.8)
    hip = (20.0, 11.0)
    m.bone('hindleg_l', 'body', (4.8, hip[0], hip[1]))
    m.cube('hindleg_l', 'haunch_l', (2.4, 14.5, 7.2), (4.6, 8.5, 7.6), 'fur')
    kn = seg('hindleg_l', 'thigh_l', 2.7, 4.2, 5.6, hip, 9, -24, 'fur_leg')
    m.bone('shin_l', 'hindleg_l', (4.8, kn[0], kn[1]))
    hk = seg('shin_l', 'shin_l', 3.1, 3.4, 3.8, kn, 8.6, 38, 'fur_leg')
    m.bone('hock_l', 'shin_l', (4.8, hk[0], hk[1]))
    an = seg('hock_l', 'mt_l', 3.3, 3.0, 2.8, hk, 3.6, -10, 'fur_leg')
    m.bone('foot_l', 'hock_l', (4.8, an[0], an[1]))
    paw('foot_l', an, 3.0, 3.6)
    FORE[:], HIND[:] = [sh, el, wr], [hip, kn, hk, an]
    # tail: bushy, carried low, three bones
    m.bone('tail1', 'body', (0, 21.8, 14.5), V(-32, 0, 0))
    m.cube('tail1', 'tail1', (-1.9, 19.6, 14), (3.8, 4.2, 8), 'fur')
    m.bone('tail2', 'tail1', (0, 21.5, 21.5), V(-12, 0, 0))
    m.cube('tail2', 'tail2', (-2.7, 18.5, 21), (5.4, 5.8, 8), 'ruff')
    m.bone('tail3', 'tail2', (0, 21.2, 28.5), V(-8, 0, 0))
    m.cube('tail3', 'tail3', (-2.3, 18.9, 28.5), (4.6, 5, 5), 'ruff')
    m.cube('tail3', 'tail_tip', (-1.5, 19.5, 33), (3, 3.6, 2.5), 'fur_dark')
    m.mirror()

    def pelt(bone, tag, rows, lean):
        """Overlapping slabs of long fur lying back along the spine: a low thick ridge, not spikes. rows = (z, y, width)."""
        for ri, (z, y, w) in enumerate(rows):
            r = hrand(tag, ri)
            m.cube(bone, '%s_%d' % (tag, ri), (-w / 2, y - 0.5, z - 2.4), (w, 1.9 + r * 0.4, 4.8), 'ruff',
                   rot=V(-(lean + r * 4), 0, 0), pivot=(0, y, z))

    # thick neck ruff, chest fluff and the hackles along the spine: separate bones so they bristle on their own
    m.bone('ruff', 'neck', (0, 22, -15))
    for sx, tag in ((1, 'l'), (-1, 'r')):          # two overlapping locks a side, so the collar is not one flat plate
        for k, (x, y, z, w, h, d, flare) in enumerate(((3.0, 15.6, -20.2, 2.4, 8.4, 5.2, 6), (2.9, 14.8, -15.6, 2.2, 8.8, 5.4, 11))):
            m.cube('ruff', 'ruff_side%d_%s' % (k, tag), (x if sx > 0 else -x - w, y, z), (w, h, d), 'ruff', rot=V(0, 0, -sx * flare), pivot=(sx * 3.9, 23.5, z + d / 2))
    m.cube('ruff', 'throat', (-3.4, 12.8, -20), (6.8, 4.4, 9), 'belly')
    pelt('ruff', 'nape', [(-18.5, 24.3, 6.2), (-15.5, 24.4, 6.8), (-12.5, 24.4, 7.0)], 8)
    m.bone('chest_ruff', 'body', (0, 17, -13))
    for i in range(4):
        fx, r = i / 3 * 2 - 1, hrand('cr', i)
        m.cube('chest_ruff', 'chest_tuft_%d' % i, (fx * 3.6 - 1.5, 11 + r, -14.6 + r * 0.6), (3, 6.5 + r * 1.5, 2.2), 'belly',
               rot=V(-8 - r * 10, 0, -fx * 7), pivot=(fx * 3.6, 18, -14))
    m.bone('hackles', 'body', (0, 24, -6))
    pelt('hackles', 'hackle', [(-10.5, 24.7, 7.0), (-7.5, 24.7, 6.6), (-4.5, 24.6, 6.0), (-1.5, 23.8, 5.2), (1.5, 23.5, 4.4), (4.5, 23.4, 3.6)], 10)
    return m


# Coat (owner's still, see REFERENCES.md): ash-brown, "pretty much like his hair colour, but brownish". Each anchor is a colour the mod
# already uses for Klaus, mixed half and half with its own grey: saddle (76,52,34) -> 42362d, brow (108,84,56) -> 615547,
# hair (164,136,92) -> 978973, cream fur (214,184,138) -> c8b9a2. The other steps are in-betweens and one shadow below the saddle.
COAT = [hexc(c) for c in ('2e2620', '42362d', '524539', '615547', '7c6f5d', '978973', 'b0a18a', 'c8b9a2', 'd9cdb8')]
LIP = [hexc(c) for c in ('060505', '0c0a0a', '151011', '20171a')]
NOSE = [hexc(c) for c in ('050505', '0b0b0c', '141415', '232325')]
# the rest are the values of the old flat Klaus texture (art/bosses/tools/paint_wolves.py)
TOOTH = [hexc(c) for c in ('826c56', 'b9a98a', 'dccfb0', 'f4eed8')]
CLAW = [hexc(c) for c in ('1a1512', '2a221d', '362c26', '4a3d34')]
MOUTH = [hexc(c) for c in ('5a181b', '822426', '963135', 'aa3e42')]
EYE, EYE_HALO, EYE_HOT, VEIN, EAR_IN = (255, 222, 40), (214, 160, 20), (255, 244, 150), (8, 6, 6), (150, 96, 84)
SH = {'up': 0.10, 'north': 0.02, 'south': -0.04, 'east': -0.02, 'west': -0.02, 'down': -0.2}


def strand(ctx, axis, seed, k=1.4, kl=0.17):
    """Fur grain around 0: strands run along `axis` (high noise frequency across, low along), lit from above."""
    p = ctx['p']
    f = [k, k, k]
    f[axis] = kl
    t = (fbm(p[0] * f[0], p[1] * f[1], p[2] * f[2], seed, 2) - 0.5) * 0.5 + SH[ctx['face']]
    t += (fbm(p[0] * 0.15, p[1] * 0.15, p[2] * 0.15, seed + 9, 2) - 0.5) * 0.2 - 0.12 * (1 - ctx['lp'][1])
    return t + (hrand(ctx['i'], ctx['j'], len(ctx['cube']['name'])) - 0.5) * 0.05


def coat(ctx, base, axis, seed):
    """`base` picks the place on the coat ramp: about 0.2 saddle, 0.5 flank, 0.8 cream."""
    return ramp(COAT, base + strand(ctx, axis, seed))


def sh_fur(ctx):
    """Trunk, neck and head: a darker saddle along the top of the back, a lighter neck and face, a mid-brown crown."""
    n, bone, y = ctx['cube']['name'], ctx['cube']['bone'], ctx['p'][1]
    if bone == 'head':
        if n.startswith('brow'):
            return coat(ctx, 0.3 if ctx['face'] == 'north' else 0.42, 2, 3)
        return coat(ctx, 0.84 if n.startswith('cheek') else 0.46 if ctx['face'] == 'up' else 0.72, 2, 3)
    if n == 'chest' and ctx['face'] == 'north':
        return coat(ctx, 0.8, 1, 7)                                                   # cream chest
    saddle = max(0.0, min(1.0, (y - 17.5) / 6.0))
    return coat(ctx, (0.68 - 0.2 * saddle) if bone == 'neck' else (0.62 - 0.32 * saddle), 2, 3)


def sh_ruff(ctx):
    """Long fur. The hackles and nape carry the saddle on; the neck ruff and cheek tufts are cream; the tail darkens to its tip."""
    n, y = ctx['cube']['name'], ctx['lp'][1]
    if n.startswith(('hackle', 'nape')):
        base = 0.16 + 0.1 * y
    elif n.startswith('tail'):
        base = 0.54 - 0.2 * ctx['lp'][2] - (0.12 if ctx['face'] == 'up' else 0)
    else:
        base = 0.64 + 0.14 * y
    return ramp(COAT, base + strand(ctx, 1, 11, 1.5, 0.12) + (0.12 if y > 0.7 and hrand(ctx['i'], ctx['j'], 4) > 0.85 else 0))


def sh_snout(ctx):
    """Pale muzzle sides and chin under a dark muzzle top."""
    n, f = ctx['cube']['name'], ctx['face']
    if n == 'chin' and f == 'up':
        return ramp(MOUTH, 0.2 + fbm(ctx['p'][0] * 0.9, 0, ctx['p'][2] * 0.9, 2) * 0.4)
    top = n == 'bridge' or (n == 'snout' and (f == 'up' or ctx['lp'][1] > 0.8))
    return ramp(COAT, (0.24 if top else 0.8) + strand(ctx, 2, 13, 1.6, 0.3))


def sh_lip(ctx):
    if ctx['face'] == 'down':
        return ramp(MOUTH, 0.2 + fbm(ctx['p'][0] * 0.9, 0, ctx['p'][2] * 0.9, 2) * 0.4)
    return ramp(LIP, 0.45 + fbm(ctx['p'][0] * 0.7, ctx['p'][1] * 0.7, ctx['p'][2] * 0.7, 3) * 0.4 + SH[ctx['face']])


def sh_nose(ctx):
    if ctx['face'] == 'north' and 0.15 < abs(ctx['fx'] - 0.5) < 0.38 and 0.35 < ctx['fy'] < 0.8:
        return (2, 2, 3)                                                                                   # nostrils
    return ramp(NOSE, 0.3 + fbm(ctx['p'][0], ctx['p'][1], ctx['p'][2], 2) * 0.5 + SH[ctx['face']])


def sh_tooth(ctx):
    y = ctx['lp'][1]
    return ramp(TOOTH, 0.3 + (y if ctx['cube']['bone'] == 'jaw' else 1 - y) * 0.55 + SH[ctx['face']] * 0.8)   # darker at the gum


def sh_ear(ctx):
    c = coat(ctx, 0.5 - 0.22 * ctx['lp'][1] - (0.14 if ctx['cube']['name'].startswith('ear_tip') else 0), 1, 3)   # dark tips
    if ctx['face'] == 'north' and 0.2 < ctx['fx'] < 0.8 and ctx['fy'] > 0.1:
        return tuple(int(c[k] * 0.5 + EAR_IN[k] * 0.4) for k in range(3))
    return c


def sh_eye(ctx):
    """Golden-yellow iris on a black sclera; only the iris goes to the glow mask."""
    if ctx['face'] != 'north':
        return (*EYE_HALO, 'glow')
    dx, dy = abs(ctx['fx'] - 0.5) * 2, abs(ctx['fy'] - 0.5) * 2
    d = dx * dx + dy * dy
    if d > 1.15:
        return (10, 8, 6)
    if dx < 0.3 and dy < 0.5:
        return (*EYE_HOT, 'glow')
    return (*(EYE if d < 0.7 else EYE_HALO), 'glow')


def sh_vein(ctx):
    """The black hybrid veins running down the face fur under each eye."""
    if ctx['face'] == 'north':
        for k, (cx, ln) in enumerate(((0.2, 0.75), (0.5, 1.0), (0.8, 0.6))):
            if ctx['fy'] < ln and abs(ctx['fx'] - cx - 0.1 * math.sin(ctx['fy'] * 6 + k * 2)) < 0.07:
                return VEIN
    return coat(ctx, 0.66, 2, 3)


SHADERS = {
    'fur': sh_fur, 'ruff': sh_ruff, 'snout': sh_snout, 'lip': sh_lip, 'nose': sh_nose, 'tooth': sh_tooth, 'ear': sh_ear,
    'eye': sh_eye, 'vein': sh_vein,
    'fur_leg': lambda ctx: coat(ctx, 0.64, 1, 5),
    'fur_dark': lambda ctx: coat(ctx, 0.1, 1, 6),
    'belly': lambda ctx: coat(ctx, 0.84, 1, 7),
    'claw': lambda ctx: ramp(CLAW, 0.3 + (1 - ctx['lp'][2]) * 0.5 + SH[ctx['face']]),
    'tongue': lambda ctx: ramp(MOUTH, 0.5 + fbm(ctx['p'][0] * 0.8, 0, ctx['p'][2] * 0.8, 2) * 0.5 + SH[ctx['face']]),
}


# ------------------------------------------------------------------ animations
REACH = [0.0]   # the furthest any paw target lay beyond its leg's reach, in px (printed; keep it near 0)
DROOP = 3.0     # degrees the toes hang per px a paw is lifted


def ik(name, dy=0.0, dz=0.0, bp=0.0, by=0.0, bz=0.0):
    """Two-bone IK in the y/z plane (every leg rotation is about X). Returns the bones of leg 'fl'/'fr'/'hl'/'hr' and the rotations
    that put its paw (dy up, dz back) from its rest spot, flat on the ground, under a body pitched bp and moved (by, bz).
    The elbow folds back and the knee forward, as they do at rest; the hind shin and metatarsus move as one piece."""
    fore = name[0] == 'f'
    ch = FORE if fore else HIND
    v1 = (ch[1][0] - ch[0][0], ch[1][1] - ch[0][1])
    v2 = (ch[-1][0] - ch[1][0], ch[-1][1] - ch[1][1])
    top = R((ch[0][0] - B[0], ch[0][1] - B[1]), bp)
    T = R((ch[-1][0] - B[0] + dy - by - top[0], ch[-1][1] - B[1] + dz - bz - top[1]), -bp)
    l1, l2, d = math.hypot(*v1), math.hypot(*v2), math.hypot(*T)
    REACH[0] = max(REACH[0], d - l1 - l2)
    th0 = (math.atan2(v2[1], v2[0]) - math.atan2(v1[1], v1[0]) + math.pi) % (2 * math.pi) - math.pi
    th = math.copysign(math.acos(max(-1.0, min(1.0, (d * d - l1 * l1 - l2 * l2) / (2 * l1 * l2)))), th0)
    a2 = math.degrees(th0 - th)
    c = R(v2, a2)
    a1 = (math.degrees(math.atan2(v1[1] + c[1], v1[0] + c[0]) - math.atan2(T[1], T[0])) + 180) % 360 - 180
    bones = ('foreleg', 'foreleg2', 'paw') if fore else ('hindleg', 'shin', 'foot')
    return ['%s_%s' % (b, name[1]) for b in bones], [a1, a2, -(bp + a1 + a2) + dy * DROOP]


def paws(a, t, legs=None, bp=0.0, by=0.0, bz=0.0, ease=None, yaw=0.0, roll=0.0):
    """Key the body at (by up, bz back), pitched bp, and all four legs so each paw sits at its (dy, dz) offset (default: planted)."""
    for name in ('fl', 'fr', 'hl', 'hr'):
        bones, rs = ik(name, *(legs or {}).get(name, (0.0, 0.0)), bp, by, bz)
        for b, r in zip(bones, rs):
            a.k(b, 'rotation', t, V(r), ease)
    a.k('body', 'position', t, V(0, by, bz), ease)
    a.k('body', 'rotation', t, V(bp, yaw, roll), ease)


def gait(a, n, phases, duty, stride, clear, height, pitch=lambda u: 0.0, sway=(0.0, 0.0)):
    """Sampled gait. Each paw stands for `duty` of the cycle, sliding back `stride` px under the body at a steady speed (so it
    does not skate), then swings forward, lifted up to `clear` px (most early in the swing, reaching low for the footfall)."""
    for s in range(n + 1):
        u0, legs = s / n, {}
        for name, ph in phases.items():
            u = (u0 + ph) % 1.0
            w = (u - duty) / (1 - duty)
            legs[name] = (0.0, stride * (u / duty - 0.5)) if u < duty else \
                (clear * math.sin(math.pi * w) * (1 - w) * 2, stride / 2 * math.cos(math.pi * w))
        w = math.sin(2 * math.pi * u0)
        paws(a, a.length * u0, legs, pitch(u0), height(u0), 0.0, None, sway[0] * w, sway[1] * w)


def build_anims():
    N = 'animation.%s.' % IDENT
    out = []
    S = lambda a, b, base, amp, ph=0.0, cyc=1, n=12: a.sine(b, 'rotation', base, amp, ph, n, cyc)
    K = lambda a, b, ks, chan='rotation': [a.k(b, chan, t, v, e) for t, v, e in ks]
    Z, ONE = V(0, 0, 0), V(1, 1, 1)

    def bristle(a, ks):
        """Hackles and ruff stand up by scaling from their roots: ks = (time, amount 0..1, easing)."""
        K(a, 'hackles', [(t, V(1, 1 + 0.45 * k, 1), e) for t, k, e in ks], 'scale')
        K(a, 'ruff', [(t, V(1 + 0.14 * k, 1 + 0.1 * k, 1), e) for t, k, e in ks], 'scale')

    # --- idle: watchful; slow look around, ears turning, tail and hackles stirring, a slow pant
    a = Anim(N + 'idle', 4.0, True)
    S(a, 'neck', Z, V(1.5, 5, 0), 0.1)
    S(a, 'head', Z, V(2, 7, 1.5), 0.25)
    S(a, 'jaw', V(1.5, 0, 0), V(1.5, 0, 0), 0.0, 2, 6)
    S(a, 'ear_l', Z, V(0, 0, 3), 0.4)
    S(a, 'ear_r', Z, V(3, 0, -3), 0.9)
    for b, amp, ph in (('tail1', 6, 0.0), ('tail2', 8, 0.15), ('tail3', 10, 0.3)):
        S(a, b, Z, V(1.5, amp, 0), ph)
    a.sine('hackles', 'scale', V(1, 1.06, 1), V(0, 0.06, 0), 0.0, 6, 2)        # breathing
    S(a, 'chest_ruff', Z, V(2, 0, 0), 0.1, 2, 6)
    out.append(a)

    # --- walk: an even diagonal prowl, head carried a little low
    a = Anim(N + 'walk', 1.0, True)
    gait(a, 12, {'fl': 0.0, 'hr': 0.06, 'fr': 0.5, 'hl': 0.56}, 0.55, 9, 3.0,
         height=lambda u: -1.1 + 0.25 * math.sin(4 * math.pi * u), sway=(2.5, 1.5))
    S(a, 'neck', V(5, 0, 0), V(1.5, -3, 0), 0.5)
    S(a, 'head', V(2, 0, 0), V(1.5, 2, -1), 0.0)
    S(a, 'jaw', V(1, 0, 0), V(1, 0, 0), 0.2, 2, 6)
    for b, amp, ph in (('tail1', 7, 0.2), ('tail2', 9, 0.35), ('tail3', 11, 0.5)):
        S(a, b, Z, V(1.5, amp, 0), ph)
    S(a, 'chest_ruff', Z, V(3, 0, 0), 0.2, 2, 6)
    out.append(a)

    # --- run: a low gallop; the fore pair lands, then the hind pair drives, with a short flight after each
    a = Anim(N + 'run', 0.5, True)
    gait(a, 16, {'fl': 0.0, 'fr': 0.08, 'hr': 0.5, 'hl': 0.58}, 0.32, 16, 5.0,
         height=lambda u: -3.3 + 1.6 * (0.5 + 0.5 * math.cos(4 * math.pi * (u - 0.37))) ** 4,
         pitch=lambda u: 5 * math.cos(2 * math.pi * (u - 0.15)))
    S(a, 'neck', V(6, 0, 0), V(-4, 0, 0), 0.1)
    S(a, 'head', V(-2, 0, 0), V(-3, 0, 0), 0.1)
    S(a, 'jaw', V(10, 0, 0), V(4, 0, 0), 0.0)
    S(a, 'ear_l', V(-22, 0, 0), V(4, 0, 0), 0.3)
    S(a, 'ear_r', V(-22, 0, 0), V(4, 0, 0), 0.3)
    S(a, 'tail1', V(24, 0, 0), V(5, 3, 0), 0.5)
    S(a, 'tail2', V(10, 0, 0), V(5, 3, 0), 0.6)
    S(a, 'tail3', V(6, 0, 0), V(5, 3, 0), 0.7)
    S(a, 'chest_ruff', V(8, 0, 0), V(5, 0, 0), 0.5)
    out.append(a)

    # --- attack_bite: crouch back on planted paws, lunge, snap the jaws shut, shake
    a = Anim(N + 'attack_bite', 1.2, False)
    paws(a, 0)
    paws(a, 0.32, None, -3, -1.6, 3, 'easeOutCubic')
    paws(a, 0.52, None, 5, -1.3, -5.5, 'easeInQuad')
    paws(a, 0.85, None, 3, -0.9, -3.5, 'easeOutCubic')
    paws(a, 1.2, None, 0, 0, 0, 'easeInOutSine')
    K(a, 'neck', ((0, Z, None), (0.32, V(-12, 0, 0), 'easeOutCubic'), (0.52, V(18, 0, 0), 'easeInQuad')))
    for t, yaw in ((0.60, -11), (0.67, 11), (0.74, -8), (0.81, 6), (0.88, 0)):
        a.k('neck', 'rotation', t, V(16, yaw, 0), 'easeInOutSine')
    a.k('neck', 'rotation', 1.2, Z, 'easeInOutSine')
    K(a, 'head', ((0, Z, None), (0.32, V(-10, 0, 0), 'easeOutCubic'), (0.52, V(4, 0, 0), 'easeInQuad'), (1.2, Z, 'easeInOutSine')))
    K(a, 'jaw', ((0, Z, None), (0.32, V(30, 0, 0), 'easeOutCubic'), (0.46, V(34, 0, 0), None), (0.54, V(-6, 0, 0), 'easeInQuart'),
                 (0.9, V(-6, 0, 0), None), (1.2, Z, 'easeInOutSine')))
    for s in 'lr':
        K(a, 'ear_' + s, ((0, Z, None), (0.32, V(-26, 0, 0), 'easeOutCubic'), (1.2, Z, 'easeInOutSine')))
    bristle(a, ((0, 0, None), (0.32, 1, 'easeOutBack'), (0.85, 0.6, 'easeInOutSine'), (1.2, 0, 'easeInOutSine')))
    K(a, 'tail1', ((0, Z, None), (0.32, V(-6, 0, 0), 'easeOutCubic'), (0.52, V(20, 0, 0), 'easeInQuad'), (1.2, Z, 'easeInOutSine')))
    out.append(a)

    # --- howl: dip, then sit back a little, throw the muzzle to the sky and hold the note
    a = Anim(N + 'howl', 3.2, False)
    up, end = 1.0, 2.5
    paws(a, 0)
    paws(a, 0.35, None, 3, -0.6, -0.5, 'easeInOutSine')
    paws(a, up, None, -5, -0.8, 1.5, 'easeOutCubic')
    paws(a, end, None, -5, -0.8, 1.5)
    paws(a, 3.2, None, 0, 0, 0, 'easeInOutSine')
    K(a, 'neck', ((0, Z, None), (0.35, V(8, 0, 0), 'easeInOutSine'), (up, V(-30, 0, 0), 'easeOutCubic'), (end, V(-32, 0, 0), 'easeInOutSine'), (3.2, Z, 'easeInOutSine')))
    K(a, 'head', ((0, Z, None), (0.35, V(8, 0, 0), 'easeInOutSine'), (up, V(-32, 0, 0), 'easeOutBack')))
    K(a, 'jaw', ((0, Z, None), (0.35, V(-4, 0, 0), 'easeInOutSine'), (up, V(15, 0, 0), 'easeOutCubic')))
    for i in range(1, 9):                                    # the held note: a slow tremble in the head and jaw
        t, sg = up + i * (end - up) / 9, (1 if i % 2 else -1)
        a.k('head', 'rotation', t, V(-34 + sg * 1.5, sg * 1.5, 0), 'easeInOutSine')
        a.k('jaw', 'rotation', t, V(17 + sg * 2.5, 0, 0), 'easeInOutSine')
    a.k('head', 'rotation', 3.2, Z, 'easeInOutSine')
    a.k('jaw', 'rotation', end, V(12, 0, 0), 'easeInOutSine')
    a.k('jaw', 'rotation', 3.2, Z, 'easeInOutSine')
    for s in 'lr':
        K(a, 'ear_' + s, ((0, Z, None), (up, V(-30, 0, 0), 'easeOutCubic'), (end, V(-30, 0, 0), None), (3.2, Z, 'easeInOutSine')))
    bristle(a, ((0, 0, None), (up, 1, 'easeOutBack'), (end, 0.8, 'easeInOutSine'), (3.2, 0, 'easeInOutSine')))
    for b, v in (('chest_ruff', -10), ('tail1', 14), ('tail2', 6)):
        K(a, b, ((0, Z, None), (up, V(v, 0, 0), 'easeOutBack'), (end, V(v * 0.8, 0, 0), 'easeInOutSine'), (3.2, Z, 'easeInOutSine')))
    out.append(a)

    # --- death: flinch, the legs give way, it rolls onto its side and goes still
    a = Anim(N + 'death', 2.4, 'hold_on_last_frame')
    paws(a, 0)
    paws(a, 0.25, None, -4, -0.6, 2, 'easeOutCubic')
    paws(a, 0.8, None, 3, -2.6, 0, 'easeInOutSine')
    paws(a, 1.6, {'fl': (4.5, -3), 'fr': (2.5, -1), 'hl': (4, 3), 'hr': (2, 1)}, 0, 0, 0, 'easeInOutSine')
    K(a, 'neck', ((0, Z, None), (0.25, V(-14, 8, 0), 'easeOutCubic'), (0.9, V(14, 0, 0), 'easeInOutSine'), (1.7, V(24, DEATH_SIDE * 22, 0), 'easeInOutSine')))
    K(a, 'head', ((0, Z, None), (0.25, V(-12, 8, 0), 'easeOutCubic'), (0.9, V(8, 0, 0), 'easeInOutSine'), (1.7, V(-18, DEATH_SIDE * 8, 0), 'easeInOutSine')))
    K(a, 'jaw', ((0, Z, None), (0.25, V(28, 0, 0), 'easeOutCubic'), (1.7, V(12, 0, 0), 'easeInOutSine')))
    K(a, 'root', ((0, Z, None), (0.7, Z, None), (1.5, V(0, 0, DEATH_SIDE * 88), 'easeInCubic'), (1.7, V(0, 0, DEATH_SIDE * 84), 'easeOutBounce'),
                  (2.4, V(0, 0, DEATH_SIDE * 86), 'easeInOutSine')))
    for t, y, e in ((0, 0, None), (0.7, 0, None), (1.1, DEATH_LIFT * 0.08, 'easeInQuad'), (1.5, DEATH_LIFT, None), (1.7, DEATH_LIFT + 0.4, 'easeOutBounce'), (2.4, DEATH_LIFT, 'easeInOutSine')):
        a.k('root', 'position', t, V(-DEATH_SIDE * 15.6 * (y / DEATH_LIFT), y, 0), e)
    bristle(a, ((0, 0, None), (0.25, 1, 'easeOutBack'), (1.7, 0, 'easeInOutSine')))
    for b, v in (('tail1', V(-10, DEATH_SIDE * 10, 0)), ('tail2', V(-8, 0, 0)), ('ear_l', V(-20, 0, 0)), ('ear_r', V(-20, 0, 0))):
        K(a, b, ((0, Z, None), (0.25, V(10, 0, 0), 'easeOutCubic'), (1.7, v, 'easeInOutSine')))
    out.append(a)

    for a in out:                                            # self-check: every loop ends on the pose it starts on
        if a.loop is True:
            for b, chans in a.ch.items():
                for c, ks in chans.items():
                    ks = sorted(ks, key=lambda k: k[0])
                    assert all(abs(x - y) < 1e-6 for x, y in zip(ks[0][1], ks[-1][1])), (a.name, b, c)
    return out


DEATH_SIDE = 1      # +1 rolls onto the file's +X (left) flank
DEATH_LIFT = 6.4    # root rise that puts that flank on the ground (checked with wolves/tools/bb_bounds.js)

if __name__ == '__main__':
    root = sys.argv[1]
    m = build()
    m.pack(256)
    img, glow = m.paint(SHADERS)
    res = os.path.join(root, 'src/main/resources/assets/supernaturalbloodlines')
    save_json(os.path.join(res, 'geo/entity/%s.geo.json' % IDENT), m.geo())
    tex = os.path.join(res, 'textures/entity/boss/%s_geo.png' % IDENT)
    os.makedirs(os.path.dirname(tex), exist_ok=True)
    img.save(tex)
    glow.save(tex.replace('_geo.png', '_geo_glowmask.png'))
    save_json(os.path.join(res, 'animations/entity/%s.animation.json' % IDENT), anim_file(build_anims()))
    print('cubes', len(m.cubes), 'bones', len(m.order), 'tex', m.tw, m.th, 'glow px', glow.getchannel('A').histogram()[255],
          'paw overreach px %.2f' % REACH[0])
