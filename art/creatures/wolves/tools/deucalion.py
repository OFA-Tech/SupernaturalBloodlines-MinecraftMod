"""Deucalion's Demon Wolf (Teen Wolf S3): hunched, hulking blue-grey biped. Run: python deucalion.py <repo_root>"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wolflib import *

FWD = -1  # verified in Blockbench: file +x rotation swings a hanging limb's lower end toward +Z (back), so forward = -x


def swing(d): return [FWD * d, 0, 0]          # lower end of a hanging limb goes forward
def pitch(d): return [-FWD * d, 0, 0]         # top of a bone tilts forward / nose down
def V(x=0, y=0, z=0): return [x, y, z]


# ------------------------------------------------------------------ model
def build():
    m = Model('deucalion_demon_wolf', scale=0.93)
    m.bone('root')
    m.bone('hips', 'root', (0, 22, 0))
    m.cube('hips', 'pelvis', (-6, 20.5, -3.5), (12, 4, 7.5), 'skin')
    m.cube('hips', 'waistband', (-6.5, 18.5, -4), (13, 5, 8.3), 'cloth')
    m.bone('spine', 'hips', (0, 24, 0.5), pitch(15))
    m.cube('spine', 'abdomen', (-5.5, 23, -3.6), (11, 8, 7.2), 'skin', den=2)
    m.cube('spine', 'chest', (-8, 30, -4.8), (16, 10, 10), 'skin', den=2)
    m.cube('spine', 'trap_hump', (-5.6, 38.6, -1.8), (11.2, 3.6, 7.4), 'skin')
    m.cube('spine', 'back_slab', (-7.4, 28.5, 4.8), (14.8, 10, 1.8), 'skin')
    m.cube('spine', 'spine_ridge', (-1.2, 30, 6.2), (2.4, 9, 1.4), 'skin_dark')
    m.bone('pec_l', 'spine', (0, 33, -5))
    m.cube('pec_l', 'pec_l', (0.4, 30.6, -6.4), (7.4, 5.4, 1.9), 'skin', den=2)
    m.bone('neck', 'spine', (0, 39, -2), pitch(10))
    m.cube('neck', 'neck', (-3.8, 38.6, -5.2), (7.6, 6.2, 6.8), 'skin')
    m.bone('head', 'neck', (0, 43, -2.5), pitch(-28))
    m.cube('head', 'skull', (-5.2, 42.2, -7.6), (10.4, 8.6, 9.6), 'skull', den=3)
    m.cube('head', 'brow', (-5.6, 47.0, -9.0), (11.2, 2.4, 3.2), 'brow', den=3)
    m.cube('head', 'cheek_l', (3.9, 42.8, -8.6), (2.0, 3.4, 4.0), 'skin', den=2)
    m.cube('head', 'snout', (-2.4, 42.2, -12.0), (4.8, 3.6, 4.6), 'snout', den=3)
    m.cube('head', 'nose_bridge', (-1.8, 45.8, -11.4), (3.6, 1.7, 4.0), 'snout', den=3)
    m.cube('head', 'nose_tip', (-1.6, 43.6, -12.7), (3.2, 1.5, 0.8), 'snout', den=3)
    m.cube('head', 'upper_lip', (-2.7, 41.5, -12.1), (5.4, 1.0, 4.9), 'gum', den=3)
    m.cube('head', 'fang_up_l', (1.4, 39.3, -11.9), (1.1, 2.4, 1.1), 'tooth', den=3)
    m.cube('head', 'incisors_up', (-1.6, 40.7, -12.1), (3.2, 0.8, 0.6), 'tooth', den=3)
    m.cube('head', 'teeth_up_l', (2.6, 40.6, -10.4), (0.7, 0.9, 3.5), 'tooth', den=3)
    m.bone('jaw', 'head', (0, 42.2, -3.4), V(8, 0, 0))
    m.cube('jaw', 'jaw', (-3.1, 38.8, -12.0), (6.2, 2.7, 9.0), 'jaw', den=2)
    m.cube('jaw', 'chin', (-2.6, 38.0, -12.5), (5.2, 1.4, 2.0), 'skin', den=3)
    m.cube('jaw', 'fang_lo_l', (1.5, 41.5, -11.8), (1.0, 2.3, 1.0), 'tooth', den=3)
    m.cube('jaw', 'incisors_lo', (-1.5, 41.5, -11.9), (3.0, 0.8, 0.6), 'tooth', den=3)
    m.cube('jaw', 'teeth_lo_l', (2.45, 41.5, -10.0), (0.7, 0.9, 3.3), 'tooth', den=3)
    m.cube('jaw', 'tongue', (-1.6, 41.2, -10.5), (3.2, 0.5, 7), 'tongue', den=3)
    m.bone('ear_l', 'head', (4.8, 46.8, -1.8), V(-14, 0, 34))
    m.cube('ear_l', 'ear_base_l', (4.4, 46.4, -3.1), (2.0, 4.4, 4.0), 'ear', den=3)
    m.cube('ear_l', 'ear_tip_l', (4.8, 50.6, -2.6), (1.4, 3.6, 3.0), 'ear', den=3)
    m.cube('ear_l', 'ear_point_l', (5.1, 54.0, -2.0), (0.7, 2.4, 1.6), 'ear', den=3)
    m.bone('hair', 'head', (0, 49, -1))
    m.cube('hair', 'hair_top', (-5.3, 50.4, -7.0), (10.6, 1.3, 9.2), 'hair', den=1)
    m.cube('hair', 'hair_front', (-4.6, 50.0, -8.4), (9.2, 1.0, 1.4), 'hair', den=1, rot=V(12, 0, 0), pivot=(0, 50.4, -7))
    m.cube('hair', 'hair_back', (-5.2, 45.4, 2.0), (10.4, 5.4, 2.6), 'hair', den=1)
    m.cube('hair', 'hair_swept', (-4.4, 44.6, 4.4), (8.8, 4.6, 2.6), 'hair', den=1, rot=V(18, 0, 0), pivot=(0, 49, 4.4))
    m.cube('hair', 'hair_nape', (-4.6, 41.4, 2.8), (9.2, 4.2, 2.2), 'hair', den=1)
    m.cube('hair', 'sideburn_l', (5.15, 46.2, -4.6), (0.7, 3.4, 3.4), 'hair', den=1)
    # arms
    m.bone('arm_l', 'spine', (9.6, 37.5, -0.5), V(-30, 0, -8))
    m.cube('arm_l', 'delt_l', (7.2, 33.6, -3.9), (6.9, 6.9, 7.8), 'skin', den=1)
    m.cube('arm_l', 'bicep_l', (7.9, 26.8, -3.2), (5.7, 9.4, 6.4), 'skin', den=2)
    m.cube('arm_l', 'bicep_bump_l', (8.2, 29.0, -4.3), (5.0, 5.0, 1.6), 'skin', den=2)
    m.bone('forearm_l', 'arm_l', (10.4, 27, -0.5), swing(16))
    m.cube('forearm_l', 'forearm_l', (8.1, 17.6, -3.1), (5.7, 10, 5.9), 'skin', den=2)
    m.cube('forearm_l', 'forearm_ridge_l', (7.8, 20.5, -3.5), (1.1, 6.5, 6.6), 'skin_dark', den=2)
    m.bone('hand_l', 'forearm_l', (10.4, 17.9, -0.5), swing(8))
    m.cube('hand_l', 'palm_l', (8.4, 13.0, -2.7), (4.9, 5.0, 5.4), 'skin_dark', den=2)
    m.cube('hand_l', 'thumb_l', (7.4, 13.4, -4.0), (1.4, 3.8, 1.4), 'skin_dark', den=2)
    m.cube('hand_l', 'thumb_claw_l', (7.5, 10.2, -4.0), (1.1, 3.4, 1.1), 'claw', den=2, rot=V(-12, 0, 0), pivot=(8, 13.2, -3.5))
    m.bone('fingers_l', 'hand_l', (10.4, 13.2, -0.5), swing(14))
    for k in range(4):
        z = -2.7 + k * 1.45
        m.cube('fingers_l', 'finger%d_l' % k, (9.0, 9.4, z), (1.35, 4.0, 1.35), 'skin_dark', den=2)
        m.cube('fingers_l', 'claw%d_l' % k, (9.1, 5.8, z + 0.05), (1.1, 3.8, 1.1), 'claw', den=2, rot=V(-10, 0, 0), pivot=(9.65, 9.6, z + 0.6))
    # legs
    m.bone('leg_l', 'hips', (4.2, 22, 0.5), swing(14))
    m.cube('leg_l', 'thigh_l', (0.8, 11.4, -3.6), (7.0, 11.4, 7.8), 'cloth', den=1, infl=0.1)
    m.bone('shin_l', 'leg_l', (4.4, 11.8, 0.2), swing(-26))
    m.cube('shin_l', 'calf_l', (1.9, 2.4, -3.1), (5.3, 9.6, 5.5), 'skin', den=2)
    m.cube('shin_l', 'shin_cloth_l', (1.5, 5.8, -3.5), (6.1, 6.2, 6.3), 'cloth_rag', den=2)
    m.bone('foot_l', 'shin_l', (4.4, 2.6, 0.4), swing(12))
    m.cube('foot_l', 'heel_l', (2.4, 0, -0.8), (4.3, 2.7, 3.4), 'skin_dark', den=2)
    m.cube('foot_l', 'foot_l', (2.2, 0, -4.4), (4.7, 2.3, 4.2), 'skin_dark', den=2)
    for k in range(3):
        x = 2.3 + k * 1.6
        m.cube('foot_l', 'toe%d_l' % k, (x, 0, -6.6), (1.45, 1.7, 2.4), 'skin_dark', den=2)
        m.cube('foot_l', 'toeclaw%d_l' % k, (x + 0.05, 0, -8.7), (1.3, 1.0, 2.2), 'claw', den=2)
    m.mirror()
    return m


# ------------------------------------------------------------------ shading
SKIN = [hexc(c) for c in ('1a2230', '232d41', '2d3a52', '38476a', '455781', '56698f', '6a7ea3', '8196b8')]
SKIND = [hexc(c) for c in ('10151f', '171e2d', '1f283b', '29344b', '344260', '435577')]
CLOTH = [hexc(c) for c in ('0e1015', '15181f', '1d212b', '262b38', '323847')]
HAIR = [hexc(c) for c in ('070605', '0c0a08', '120e0b', '1a1410', '241b15', '33281f')]
TOOTH = [hexc(c) for c in ('a89f86', 'cfc7ad', 'e6dfc8', 'f4f0e0')]
CLAW = [hexc(c) for c in ('07080b', '0f1116', '191c25', '272c39', '3a4152')]
GUM = [hexc(c) for c in ('2a0b11', '3d1118', '521a22', '6a2630')]
TONGUE = [hexc(c) for c in ('551b27', '6e2733', '8a3643', 'a24a58')]
NOSE = [hexc(c) for c in ('141922', '1d2430', '2a3342', '3a4558')]
FSH = {'up': 0.10, 'north': 0.02, 'south': -0.05, 'east': -0.03, 'west': -0.03, 'down': -0.22}


def nz(ctx, k, seed, oct=3):
    p = ctx['p']
    return fbm(p[0] * k, p[1] * k, p[2] * k, seed, oct)


def dith(ctx, a=0.07):
    return (hrand(ctx['i'], ctx['j'], hash(ctx['cube']['name']) % 997) - 0.5) * a


def skin_t(ctx, base=0.5, dark=False):
    p, lp, nm, f = ctx['p'], ctx['lp'], ctx['cube']['name'], ctx['face']
    n1 = nz(ctx, 0.14, 11, 2)
    t = base + (n1 - 0.5) * 0.34 + FSH[f] * 1.4
    t -= 0.16 * (1 - lp[1])                                                    # cheap ambient occlusion, darker low
    t += (fbm(p[0] * 1.1, p[1] * 0.11, p[2] * 1.1, 5, 2) - 0.5) * 0.2          # vertical muscle fibres
    if abs(math.sin(p[1] * 0.55 + n1 * 5.0)) < 0.07:                           # creases
        t -= 0.17
    if abs(nz(ctx, 0.45, 23) - 0.5) < 0.008:                                   # veins
        t -= 0.3
    if f == 'north':                                                           # painted muscle definition
        x, y = abs(lp[0] - 0.5), lp[1]
        if nm == 'chest' and (x < 0.03 or (0.30 < y < 0.36 and x < 0.45)):
            t -= 0.3                                                           # sternum + pec underline
        if nm == 'abdomen' and (x < 0.03 or min(abs(y - 0.33), abs(y - 0.66)) < 0.05):
            t -= 0.28                                                          # abs
        if nm.startswith('pec') and lp[1] < 0.3:
            t -= 0.25
    if hrand(ctx['i'], ctx['j'], len(nm)) > 0.965:
        t += 0.1                                                               # sparse highlight flecks
    return t + dith(ctx, 0.02)


def sh_skin(ctx):
    return ramp(SKIN, skin_t(ctx))


def sh_skin_dark(ctx):
    return ramp(SKIND, skin_t(ctx, 0.52))


def sh_hair(ctx):
    p = ctx['p']
    s = fbm(p[0] * 1.7, p[1] * 1.7, p[2] * 0.2, 31, 2)
    t = 0.12 + s * 0.55 + FSH[ctx['face']] * 1.2 + dith(ctx, 0.08)
    if hrand(ctx['i'] // 2, ctx['j'], 5) > 0.93:
        t += 0.25
    return ramp(HAIR, t)


def sh_cloth(ctx):
    p = ctx['p']
    t = 0.45 + (fbm(p[0] * 0.5, p[1] * 0.18, p[2] * 0.5, 41) - 0.5) * 0.9 + FSH[ctx['face']] + dith(ctx, 0.1)
    if abs(math.sin(p[1] * 0.9 + p[0] * 0.3)) < 0.06:
        t -= 0.25
    return ramp(CLOTH, t)


def sh_cloth_rag(ctx):
    c = sh_cloth(ctx)
    p = ctx['p']
    edge = 0.14 + 0.3 * fbm((p[0] + p[2]) * 1.4, 0, 3.3, 9, 2)
    if ctx['lp'][1] < edge and ctx['face'] not in ('up', 'down'):
        return (*c, 0)
    if ctx['face'] == 'down':
        return (*c, 0)
    return c


def sh_tooth(ctx):
    t = 0.35 + (1 - ctx['lp'][1]) * 0.55 + FSH[ctx['face']] * 0.8
    nm = ctx['cube']['name']
    if nm.startswith(('incisors', 'teeth')) and ctx['face'] in ('north', 'east', 'west'):
        u = ctx['lp'][0] if nm.startswith('incisors') else ctx['lp'][2]
        if (u * (4 if nm.startswith('incisors') else 3)) % 1.0 < 0.2:
            return (36, 22, 24)                                         # gap between teeth
        t -= 0.15 * hrand(int(u * 4), len(nm))
    return ramp(TOOTH, t)


def sh_claw(ctx):
    t = 0.3 + (1 - ctx['lp'][1]) * 0.5 + FSH[ctx['face']] + dith(ctx, 0.12)
    return ramp(CLAW, t)


def sh_gum(ctx):
    if ctx['face'] in ('north', 'east', 'west') and ctx['cube']['name'].startswith('upper_lip'):
        c = ramp(SKIND, 0.35 + nz(ctx, 0.9, 7) * 0.3)
        return tuple(int(k * 0.8) for k in c) if ctx['fy'] > 0.55 else ramp(GUM, 0.45)
    return ramp(GUM, 0.4 + nz(ctx, 0.9, 7) * 0.5 + FSH[ctx['face']] + dith(ctx))


def sh_tongue(ctx):
    return ramp(TONGUE, 0.4 + nz(ctx, 0.8, 8) * 0.5 + FSH[ctx['face']])


def sh_nose(ctx):
    t = 0.35 + nz(ctx, 1.2, 4) * 0.4 + FSH[ctx['face']]
    if ctx['face'] == 'north' and abs(ctx['fx'] - 0.5) > 0.2 and abs(ctx['fx'] - 0.5) < 0.32 and 0.3 < ctx['fy'] < 0.7:
        return (4, 5, 7)   # nostrils
    return ramp(NOSE, t)


def sh_ear(ctx):
    base = sh_skin(ctx)
    if ctx['face'] in ('north', 'south') and 0.2 < ctx['fx'] < 0.8 and ctx['fy'] > 0.05:   # inner ear is a darker plum
        return (int(base[0] * 0.8 + 26), int(base[1] * 0.55 + 4), int(base[2] * 0.62 + 10))
    return base


def sh_jaw(ctx):
    if ctx['face'] == 'up':
        return sh_gum(ctx)
    return sh_skin(ctx)


def sh_brow(ctx):
    p = ctx['p']
    c = sh_skin(ctx)
    if ctx['face'] == 'down':
        return tuple(int(k * 0.55) for k in c)
    if ctx['face'] in ('north', 'up') and abs(math.sin(p[0] * 1.9 + nz(ctx, 0.4, 3) * 3)) < 0.14:
        return tuple(int(k * 0.7) for k in c)   # heavy brow creases
    return c


def sh_snout(ctx):
    c = sh_skin(ctx)
    p = ctx['p']
    if ctx['face'] == 'down':
        return ramp(GUM, 0.55 + nz(ctx, 0.9, 7) * 0.4)
    if ctx['cube']['name'] == 'nose_tip' and ctx['face'] == 'north':
        dx = abs(ctx['fx'] - 0.5)
        if 0.16 < dx < 0.34 and 0.3 < ctx['fy'] < 0.75:
            return (8, 9, 14)                                           # flared nostril slits
    w = abs(math.sin(p[2] * 2.6 + p[1] * 0.8 + nz(ctx, 0.6, 14) * 2.2))   # wrinkled snout
    if w < 0.16 and ctx['face'] in ('up', 'east', 'west', 'north'):
        return tuple(int(k * 0.62) for k in c)
    if w > 0.93:
        return tuple(min(255, int(k * 1.18 + 6)) for k in c)
    return c


def eye_hit(lx, ly, side):
    """Angry slanted red eye; returns (strength 0..1 core, ring dist) or None. lx,ly are normalised skull face coords."""
    cx, cy, a, b = 0.30, 0.31, 0.16, 0.105
    dx = abs(lx - 0.5)
    cyy = cy + 0.55 * (dx - cx)
    q = math.hypot((dx - cx) / a, (ly - cyy) / b)
    return q


def sh_skull(ctx):
    c = sh_skin(ctx)
    if ctx['face'] == 'north':
        q = eye_hit(ctx['lp'][0], ctx['lp'][1], 0)
        if q < 1.0:
            if q < 0.16:
                return (255, 225, 190, 'glow')
            if q < 0.7:
                return (255, 56, 36, 'glow')
            return (196, 14, 16, 'glow')
        if q < 1.55:
            return tuple(int(k * 0.5) for k in c)       # deep socket shadow
    return c


SHADERS = {'skin': sh_skin, 'skin_dark': sh_skin_dark, 'hair': sh_hair, 'cloth': sh_cloth, 'cloth_rag': sh_cloth_rag,
           'tooth': sh_tooth, 'claw': sh_claw, 'gum': sh_gum, 'tongue': sh_tongue, 'nose': sh_nose, 'ear': sh_ear,
           'jaw': sh_jaw, 'brow': sh_brow, 'snout': sh_snout, 'skull': sh_skull}

# ------------------------------------------------------------------ animations (values are offsets from the rest pose)
# file-space rotation cheat sheet (verified in Blockbench): +x = top of bone tilts forward / hanging limb swings BACK,
# +y = turn toward the creature's right, +z = top tilts toward the creature's left. 'swing' and 'pitch' hide the x signs.
def mir(v): return [v[0], -v[1], -v[2]]


def Lk(a, base, t, v, ease=None, chan='rotation'):
    """key bone base_l with v and base_r with the mirrored value"""
    a.k(base + '_l', chan, t, v, ease)
    a.k(base + '_r', chan, t, mir(v) if chan == 'rotation' else [-v[0], v[1], v[2]], ease)


def Lsine(a, base, basev, ampv, phase=0.0, chan='rotation', n=12, cycles=1, opposite=True):
    a.sine(base + '_l', chan, basev, ampv, phase, n, cycles)
    b2 = mir(basev) if chan == 'rotation' else [-basev[0], basev[1], basev[2]]
    a2 = mir(ampv) if chan == 'rotation' else [-ampv[0], ampv[1], ampv[2]]
    a.sine(base + '_r', chan, b2, a2, phase + (0.5 if opposite else 0.0), n, cycles)


def build_anims():
    N = 'animation.deucalion_demon_wolf.'
    out = []

    # --- idle: heavy breathing, slow head sweep, fingers flexing, hair stirring
    a = Anim(N + 'idle', 4.0, True)
    a.sine('spine', 'rotation', V(0, 0, 0), V(1.6, 0.8, 0))
    a.sine('neck', 'rotation', V(0, 0, 0), V(-0.8, 0, 0), 0.1)
    a.sine('head', 'rotation', V(0, 0, 0), V(1.5, 7, 1.2), 0.2)
    a.sine('jaw', 'rotation', V(0, 0, 0), V(2.5, 0, 0), 0.05)
    a.sine('hips', 'position', V(0, 0, 0), V(0, 0.35, 0))
    Lsine(a, 'arm', V(0, 0, 0), V(-2.0, 0, -1.2), 0.0, opposite=False)
    Lsine(a, 'forearm', V(0, 0, 0), V(-1.5, 0, 0), 0.15, opposite=False)
    Lsine(a, 'fingers', V(-3, 0, 0), V(-4, 0, 0), 0.3, opposite=True)
    a.sine('hair', 'rotation', V(0, 0, 0), V(2.5, 0, 0.8), 0.35)
    Lsine(a, 'ear', V(0, 0, 0), V(0, 0, 3), 0.5, opposite=False)
    out.append(a)

    # --- walk: lumbering contralateral stride, torso twist, heavy bob
    a = Anim(N + 'walk', 1.0, True)
    Lsine(a, 'leg', V(0, 0, 0), swing(24))
    Lsine(a, 'shin', V(18, 0, 0), V(18, 0, 0), 0.25)
    Lsine(a, 'foot', V(0, 0, 0), V(7, 0, 0), 0.1)
    Lsine(a, 'arm', V(0, 0, 0), swing(15), 0.5)
    Lsine(a, 'forearm', V(0, 0, 0), swing(7), 0.75)
    a.sine('hips', 'position', V(0, 1.3, 0), V(0, 0.9, 0), 0.25, cycles=2, n=6)
    a.sine('hips', 'rotation', V(0, 0, 0), V(0, 4, 2), 0.0)
    a.sine('spine', 'rotation', V(3, 0, 0), V(1.5, -7, 2), 0.0)
    a.sine('head', 'rotation', V(-3, 0, 0), V(1.5, 4, -2), 0.5)
    a.sine('jaw', 'rotation', V(0, 0, 0), V(3, 0, 0), 0.3, cycles=2, n=6)
    a.sine('hair', 'rotation', V(2, 0, 0), V(3, 0, 0), 0.3, cycles=2, n=6)
    Lsine(a, 'fingers', V(-6, 0, 0), V(-3, 0, 0), 0.4)
    out.append(a)

    # --- run: long lunging gallop, torso thrown forward
    a = Anim(N + 'run', 0.62, True)
    Lsine(a, 'leg', V(0, 0, 0), swing(46))
    Lsine(a, 'shin', V(34, 0, 0), V(30, 0, 0), 0.25)
    Lsine(a, 'foot', V(0, 0, 0), V(12, 0, 0), 0.1)
    Lsine(a, 'arm', swing(10), swing(36), 0.5)
    Lsine(a, 'forearm', swing(36), swing(14), 0.75)
    a.sine('hips', 'position', V(0, 0, -1.5), V(0, 1.6, 0), 0.25, cycles=2, n=6)
    a.sine('hips', 'rotation', V(0, 0, 0), V(0, 6, 2.5), 0.0)
    a.sine('spine', 'rotation', V(16, 0, 0), V(2, -10, 3), 0.0)
    a.sine('neck', 'rotation', V(-4, 0, 0), V(0, 0, 0))
    a.sine('head', 'rotation', V(-14, 0, 0), V(2, 5, -3), 0.5)
    a.sine('jaw', 'rotation', V(14, 0, 0), V(4, 0, 0), 0.3, cycles=2, n=6)
    a.sine('hair', 'rotation', V(12, 0, 0), V(6, 0, 0), 0.3, cycles=2, n=6)
    Lsine(a, 'fingers', V(-14, 0, 0), V(-5, 0, 0), 0.4)
    out.append(a)

    # --- attack_swipe: right-hand claw rake across the body
    a = Anim(N + 'attack_swipe', 1.0, False)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(105, 0, 42), 'easeOutCubic'), (0.50, V(-85, 0, -32), 'easeInQuad'),
                    (0.68, V(-62, 0, -20), 'easeOutCubic'), (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('arm_r', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(34, 0, 0), 'easeOutCubic'), (0.50, V(-40, 0, 0), 'easeInQuad'),
                    (0.68, V(-14, 0, 0), 'easeOutCubic'), (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('forearm_r', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(-30, 0, 0), 'easeOutCubic'), (0.50, V(22, 0, 0), 'easeInQuad'),
                    (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('fingers_r', 'rotation', t, v, e)       # claws spread on the wind-up, hook in on the strike
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(-4, 26, 6), 'easeOutCubic'), (0.50, V(14, -30, -4), 'easeInQuad'),
                    (0.68, V(10, -22, -2), 'easeOutCubic'), (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('spine', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(-8, -16, 0), 'easeOutCubic'), (0.50, V(8, 22, 0), 'easeInQuad'),
                    (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('head', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(26, 0, 0), 'easeOutCubic'), (0.50, V(34, 0, 0), 'easeInQuad'),
                    (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('jaw', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, swing(-14), 'easeOutCubic'), (0.50, swing(20), 'easeInQuad'),
                    (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('arm_l', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, swing(-12), 'easeOutCubic'), (0.50, swing(22), 'easeInQuad'),
                    (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('leg_l', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(0, 0, 1.5), 'easeOutCubic'), (0.50, V(0, -1.2, -3.5), 'easeInQuad'),
                    (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('hips', 'position', t, v, e)
    out.append(a)

    # --- attack_bite: rear back, lunge, snap, shake the prey
    a = Anim(N + 'attack_bite', 1.3, False)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(-12, 0, 0), 'easeOutCubic'), (0.62, V(24, 0, 0), 'easeInQuad'),
                    (0.80, V(20, 0, 0), 'easeOutCubic'), (1.3, V(0, 0, 0), 'easeInOutSine')):
        a.k('spine', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(-14, 0, 0), 'easeOutCubic'), (0.62, V(30, 0, 0), 'easeInQuad'),
                    (0.80, V(26, 0, 0), 'easeOutCubic'), (1.3, V(0, 0, 0), 'easeInOutSine')):
        a.k('head', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(44, 0, 0), 'easeOutCubic'), (0.62, V(0, 0, 0), 'easeInQuart'),
                    (0.80, V(6, 0, 0), 'easeOutCubic'), (1.3, V(0, 0, 0), 'easeInOutSine')):
        a.k('jaw', 'rotation', t, v, e)
    for i, (t, v) in enumerate(((0.66, V(0, 0, -5)), (0.72, V(0, 0, 5)), (0.78, V(0, 0, -4)), (0.84, V(0, 0, 3)), (0.92, V(0, 0, 0)))):
        a.k('neck', 'rotation', t, v, 'easeInOutSine')   # head-shake while the jaws are clamped
    a.k('neck', 'rotation', 0, V(0, 0, 0)); a.k('neck', 'rotation', 1.3, V(0, 0, 0), 'easeInOutSine')
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(0, 0.4, 2.5), 'easeOutCubic'), (0.62, V(0, -1, -5), 'easeInQuad'),
                    (0.80, V(0, -0.8, -4), 'easeOutCubic'), (1.3, V(0, 0, 0), 'easeInOutSine')):
        a.k('hips', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(-6, 0, -22), 'easeOutCubic'), (0.62, swing(32) if False else V(-34, 0, -12), 'easeInQuad'),
                    (1.3, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'arm', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(-18, 0, 0), 'easeOutCubic'), (0.62, V(-26, 0, 0), 'easeInQuad'),
                    (1.3, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'fingers', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.62, swing(22), 'easeInQuad'), (1.3, V(0, 0, 0), 'easeInOutSine')):
        a.k('leg_l', 'rotation', t, v, e)
    out.append(a)

    # --- roar: chest out, head thrown back, arms flung wide, shuddering, then settle
    a = Anim(N + 'roar', 2.6, False)
    up = 0.75
    for t, v, e in ((0, V(0, 0, 0), None), (up, V(-16, 0, 0), 'easeOutCubic'), (1.9, V(-14, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('spine', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (up, V(-16, 0, 0), 'easeOutCubic'), (1.9, V(-14, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('neck', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, V(10, 0, 0), 'easeInOutSine'), (up, V(-20, 0, 0), 'easeOutBack'), (1.9, V(-18, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('head', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, V(4, 0, 0), 'easeInOutSine'), (up, V(52, 0, 0), 'easeOutCubic'), (1.9, V(50, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('jaw', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, V(14, 0, 8), 'easeInOutSine'), (up, V(8, 0, -34), 'easeOutBack'), (1.9, V(8, 0, -32), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'arm', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (up, V(-20, 0, 0), 'easeOutCubic'), (1.9, V(-20, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'forearm', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (up, V(-34, 0, 0), 'easeOutCubic'), (1.9, V(-34, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'fingers', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, V(0, -1.2, 0), 'easeInOutSine'), (up, V(0, 0.6, 1.5), 'easeOutCubic'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('hips', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, swing(-10), 'easeInOutSine'), (up, swing(8), 'easeOutCubic'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'leg', t, v, e)
    shake = 1.0
    ts = [up + 0.12 * i for i in range(10)]
    for i, t in enumerate(ts):                       # the shudder of a full-lung roar
        sgn = 1 if i % 2 == 0 else -1
        a.k('spine', 'rotation', t + 0.05, V(-16 + 0.9 * sgn, 1.6 * sgn, 1.1 * sgn))
    a.k('spine', 'rotation', up, V(-16, 0, 0))
    for i, t in enumerate(ts):
        a.k('hair', 'rotation', t, V(8 + 7 * (1 if i % 2 == 0 else -1), 0, 0), 'easeInOutSine')
    for i, t in enumerate(ts):
        a.k('ear_l', 'rotation', t, V(0, 0, -6 if i % 2 == 0 else 3), 'easeInOutSine')
        a.k('ear_r', 'rotation', t, V(0, 0, 6 if i % 2 == 0 else -3), 'easeInOutSine')
    out.append(a)

    # --- hurt: quick recoil
    a = Anim(N + 'hurt', 0.6, False)
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, V(-11, 5, 2), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('spine', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, V(-16, 16, 5), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('head', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, V(22, 0, 0), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('jaw', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, V(14, 0, -10), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'arm', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, V(0, 0, 2.2), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('hips', 'position', t, v, e)
    out.append(a)

    # --- death: flinch, knees buckle, topple onto the face
    a = Anim(N + 'death', 2.4, 'hold_on_last_frame')
    K = lambda b, t, v, e=None, c='rotation': a.k(b, c, t, v, e)
    for b, keys in {
        'spine': ((0, V(0, 0, 0)), (0.25, V(-14, 4, 0)), (0.80, V(24, 0, 0)), (1.5, V(34, 0, 0))),
        'head': ((0, V(0, 0, 0)), (0.25, V(-22, 14, 0)), (0.80, V(18, -8, 6)), (1.5, V(26, 0, 0))),
        'jaw': ((0, V(0, 0, 0)), (0.25, V(34, 0, 0)), (0.80, V(26, 0, 0)), (1.5, V(14, 0, 0))),
    }.items():
        for t, v in keys:
            K(b, t, v, 'easeOutCubic' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.25, swing(-4)), (0.80, swing(62)), (1.5, swing(70))):
        Lk(a, 'leg', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.25, V(4, 0, 0)), (0.80, V(88, 0, 0)), (1.5, V(96, 0, 0))):
        Lk(a, 'shin', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.80, swing(-18)), (1.5, swing(-26))):
        Lk(a, 'foot', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.25, V(14, 0, -12)), (0.80, V(-8, 0, -18)), (1.5, V(-26, 0, -34)), (1.9, V(-34, 0, -52)), (2.4, V(-34, 0, -52))):
        Lk(a, 'arm', t, v, 'easeOutCubic' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.80, V(-14, 0, 0)), (1.9, V(-22, 0, 0))):
        Lk(a, 'forearm', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.25, V(0, 0, 1.5)), (0.80, V(0, -6.0, 2.5)), (1.5, V(0, -6.5, 2.5))):
        K('hips', t, v, 'easeInOutSine' if t else None, 'position')
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, V(18, 0, 0), 'easeInCubic'), (1.48, V(88, 0, 0), 'easeInQuad'),
                    (1.64, V(83, 0, 0), 'easeOutQuad'), (1.8, V(86, 0, 0), 'easeInOutSine'), (2.4, V(86, 0, 0), None)):
        K('root', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, V(0, 5, 0), 'easeInCubic'), (1.48, V(0, 24.4, 0), 'easeInQuad'), (1.64, V(0, 25.6, 0), 'easeOutQuad'), (1.8, V(0, 24.8, 0), 'easeInOutSine'), (2.4, V(0, 24.8, 0), None)):
        K('root', t, v, e, 'position')
    out.append(a)

    # --- drop_to_fours: sink, throw the weight forward, hands slap down, settle into a quadruped crouch
    a = Anim(N + 'drop_to_fours', 1.5, 'hold_on_last_frame')
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(-8, 0, 0), 'easeOutCubic'), (1.0, V(48, 0, 0), 'easeInOutCubic'), (1.5, V(56, 0, 0), 'easeOutBack')):
        a.k('spine', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(-4, 0, 0), 'easeOutCubic'), (1.0, V(-8, 0, 0), 'easeInOutCubic'), (1.5, V(-12, 0, 0), 'easeOutCubic')):
        a.k('neck', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(-12, 0, 0), 'easeOutCubic'), (1.0, V(-30, 0, 0), 'easeInOutCubic'), (1.5, V(-38, 0, 0), 'easeOutCubic')):
        a.k('head', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(26, 0, 0), 'easeOutCubic'), (1.0, V(14, 0, 0), 'easeInOutCubic'), (1.5, V(6, 0, 0), 'easeOutCubic')):
        a.k('jaw', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(8, 0, 0), 'easeOutCubic'), (1.0, V(20, 0, 0), 'easeInOutCubic'), (1.5, V(24, 0, 0), 'easeOutCubic')):
        a.k('hips', 'rotation', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(0, -2.5, 1), 'easeOutCubic'), (1.0, V(0, -7, -1), 'easeInOutCubic'), (1.5, V(0, -9, -2), 'easeOutCubic')):
        a.k('hips', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(0, 0, 0), None), (1.0, V(0, 9, 0), 'easeInOutCubic'), (1.5, V(0, 13.6, 0), 'easeOutCubic')):
        a.k('root', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, swing(-16), 'easeOutCubic'), (1.0, swing(60), 'easeInOutCubic'), (1.5, swing(64), 'easeOutBack')):
        Lk(a, 'arm', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(10, 0, 0), 'easeOutCubic'), (1.0, swing(-12), 'easeInOutCubic'), (1.5, swing(-14), 'easeOutCubic')):
        Lk(a, 'forearm', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, swing(30), 'easeInOutCubic'), (1.5, swing(34), 'easeOutCubic')):
        Lk(a, 'hand', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, V(-14, 0, 0), 'easeInOutCubic'), (1.5, V(-22, 0, 0), 'easeOutCubic')):
        Lk(a, 'fingers', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.45, V(0, 0, 0), 'easeOutCubic'), (1.0, swing(34), 'easeInOutCubic'), (1.5, swing(42), 'easeOutCubic')):
        Lk(a, 'leg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, V(40, 0, 0), 'easeInOutCubic'), (1.5, V(48, 0, 0), 'easeOutCubic')):
        Lk(a, 'shin', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, swing(-22), 'easeInOutCubic'), (1.5, swing(-26), 'easeOutCubic')):
        Lk(a, 'foot', t, v, e)
    out.append(a)
    return out


if __name__ == '__main__':
    root = sys.argv[1]
    m = build()
    m.pack(256)
    img, glow = m.paint(SHADERS)
    ident = 'deucalion_demon_wolf'
    save_json(os.path.join(root, 'src/main/resources/assets/supernaturalbloodlines/geo/entity/%s.geo.json' % ident), m.geo())
    tex = os.path.join(root, 'src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/%s_geo.png' % ident)
    os.makedirs(os.path.dirname(tex), exist_ok=True)
    img.save(tex)
    glow.save(tex.replace('_geo.png', '_geo_glowmask.png'))
    save_json(os.path.join(root, 'src/main/resources/assets/supernaturalbloodlines/animations/entity/%s.animation.json' % ident), anim_file(build_anims()))
    print('cubes', len(m.cubes), 'bones', len(m.order), 'tex', m.tw, m.th)
