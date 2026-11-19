"""Peter Hale's Alpha (Teen Wolf S1): huge black/dark-blue quadruped beast with a shaggy mane.
Run: python peter.py <repo_root>"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wolflib import *

FWD = -1  # file +x tilts a bone's top forward / swings a hanging limb back (verified in Blockbench)


def swing(d): return [FWD * d, 0, 0]      # lower end of a hanging limb goes forward
def pitch(d): return [-FWD * d, 0, 0]     # top tilts forward / nose down
def V(x=0, y=0, z=0): return [x, y, z]


def build():
    m = Model('peter_hale_alpha', scale=1.0)
    m.bone('root', None, (0, 0, 13))
    m.bone('body', 'root', (0, 22, 0))
    m.cube('body', 'chest', (-8, 14, -16), (16, 14, 16), 'fur')
    m.cube('body', 'brisket', (-6.5, 11.5, -13), (13, 3, 11), 'fur_belly')
    m.cube('body', 'abdomen', (-6.5, 15, -1), (13, 11, 11), 'fur')
    m.cube('body', 'rump', (-7, 14.5, 9), (14, 12, 8.5), 'fur')
    m.cube('body', 'hump', (-6.5, 27.4, -14.5), (13, 3.8, 12), 'fur')
    m.cube('body', 'back_strip', (-4, 26, -2), (8, 1.8, 12), 'fur')
    # head / neck
    m.bone('neck', 'body', (0, 25, -14), pitch(6))
    m.cube('neck', 'neck', (-4.8, 21, -23), (9.6, 9.6, 10), 'fur')
    m.bone('head', 'neck', (0, 26, -22), pitch(6))
    m.cube('head', 'skull', (-4.7, 21.4, -31), (9.4, 8.8, 9.4), 'fur_head', den=2)
    m.cube('head', 'forehead', (-3.6, 29.8, -29.5), (7.2, 1.2, 6), 'fur_head')
    m.cube('head', 'snout', (-3.0, 21.8, -39.8), (6.0, 4.9, 9.4), 'snout', den=2)
    m.cube('head', 'bridge', (-2.3, 26.5, -39), (4.6, 1.5, 8.6), 'snout')
    m.cube('head', 'nose', (-1.9, 24.2, -40.7), (3.8, 2.5, 1.2), 'nose', den=2)
    m.cube('head', 'lip_up', (-3.4, 20.9, -39.6), (6.8, 1.3, 8.6), 'lip')
    m.cube('head', 'fang_up_l', (1.9, 18.3, -39.2), (1.15, 3.2, 1.15), 'tooth', den=2)
    m.cube('head', 'fang_side_l', (2.9, 19.4, -35.6), (0.9, 2.2, 1.1), 'tooth', den=2)
    m.cube('head', 'fang_side2_l', (2.9, 19.6, -33.2), (0.9, 1.9, 1.0), 'tooth', den=2)
    m.cube('head', 'incisors_up', (-1.4, 19.6, -39.4), (2.8, 1.4, 0.7), 'tooth', den=2)
    m.cube('head', 'brow_l', (0.4, 27.0, -32.2), (4.6, 1.7, 2.6), 'fur_head', den=2, rot=V(0, 0, -13), pivot=(2.7, 27.8, -31))
    m.cube('head', 'eye_l', (1.6, 25.1, -31.7), (3.0, 1.6, 0.9), 'eye', den=3, rot=V(0, 0, -16), pivot=(3.1, 25.9, -31.2))
    m.cube('head', 'cheek_l', (4.0, 21.4, -29.6), (1.8, 4.6, 6.0), 'fur_head')
    m.bone('jaw', 'head', (0, 22.4, -27.5), V(14, 0, 0))
    m.cube('jaw', 'jaw', (-2.7, 18.9, -38.6), (5.4, 2.9, 11), 'snout', den=2)
    m.cube('jaw', 'lip_lo', (-3.0, 18.4, -38.2), (6.0, 1.3, 9.2), 'lip')
    m.cube('jaw', 'fang_lo_l', (1.5, 21.8, -38.2), (1.05, 2.8, 1.05), 'tooth', den=2)
    m.cube('jaw', 'fang_lo_side_l', (2.4, 21.8, -35.0), (0.8, 1.6, 1.0), 'tooth', den=2)
    m.cube('jaw', 'incisors_lo', (-1.3, 21.8, -38.2), (2.6, 1.0, 0.6), 'tooth', den=2)
    m.cube('jaw', 'tongue', (-1.5, 21.2, -36.4), (3.0, 0.6, 8), 'tongue', den=2)
    m.bone('ear_l', 'head', (3.6, 29.6, -25.6), V(-16, 0, 14))
    m.cube('ear_l', 'ear_base_l', (2.2, 29.2, -27.2), (3.0, 4.2, 2.4), 'ear', den=2)
    m.cube('ear_l', 'ear_tip_l', (2.7, 33.2, -26.8), (2.0, 3.2, 1.6), 'ear', den=2)
    m.cube('ear_l', 'ear_point_l', (3.1, 36.2, -26.4), (1.0, 2.4, 1.0), 'ear', den=2)
    # forelegs
    m.bone('foreleg_l', 'body', (5.3, 25.5, -9.5))
    m.cube('foreleg_l', 'shoulder_l', (2.0, 13.5, -14), (6.6, 13, 9), 'fur')
    m.bone('foreleg2_l', 'foreleg_l', (5.5, 14.5, -9.4), pitch(-8))
    m.cube('foreleg2_l', 'forearm_l', (3.0, 5.2, -12.2), (4.6, 10, 5.8), 'fur_leg')
    m.cube('foreleg2_l', 'fur_tuft_l', (3.4, 7.0, -6.6), (3.8, 6.5, 1.8), 'fur_leg', rot=V(-18, 0, 0), pivot=(5.3, 13.5, -6.6))
    m.bone('paw_l', 'foreleg2_l', (5.3, 5.8, -9.6), pitch(8))
    m.cube('paw_l', 'wrist_l', (3.3, 2.6, -11.6), (4.0, 3.8, 4.8), 'fur_dark')
    m.cube('paw_l', 'paw_l', (2.6, 0, -15.2), (5.4, 3.0, 8.2), 'fur_dark')
    for k in range(4):
        x = 2.75 + k * 1.28
        m.cube('paw_l', 'toe%d_l' % k, (x, 0, -17.6), (1.15, 1.6, 2.6), 'fur_dark')
        m.cube('paw_l', 'claw%d_l' % k, (x + 0.06, 0, -20.4), (1.0, 1.1, 3.0), 'claw', rot=V(-10, 0, 0), pivot=(x + 0.6, 1.0, -17.6))
    # hind legs
    m.bone('hindleg_l', 'body', (5.8, 25, 14), swing(26))
    m.cube('hindleg_l', 'thigh_l', (2.0, 12.2, 9.2), (7.6, 13.3, 9.6), 'fur')
    m.cube('hindleg_l', 'thigh_tuft_l', (2.6, 12.0, 18.0), (5.2, 8.0, 2.0), 'fur_leg', rot=V(-14, 0, 0), pivot=(5.2, 20, 18))
    m.bone('shin_l', 'hindleg_l', (6.0, 13, 14.2), swing(-52))
    m.cube('shin_l', 'shin_l', (3.4, 2.4, 12.2), (5.0, 11, 5.4), 'fur_leg')
    m.bone('hock_l', 'shin_l', (6.0, 3.4, 14.4), swing(30))
    m.cube('hock_l', 'mt_l', (3.9, -3.4, 12.9), (4.0, 7.2, 3.8), 'fur_dark')
    m.bone('foot_l', 'hock_l', (5.9, -3.0, 14.8), swing(12))
    m.cube('foot_l', 'hindpaw_l', (3.2, -4.4, 8.4), (5.4, 2.7, 8.4), 'fur_dark')
    for k in range(4):
        x = 3.35 + k * 1.28
        m.cube('foot_l', 'htoe%d_l' % k, (x, -4.4, 6.2), (1.15, 1.6, 2.6), 'fur_dark')
        m.cube('foot_l', 'hclaw%d_l' % k, (x + 0.06, -4.4, 3.4), (1.0, 1.1, 3.0), 'claw', rot=V(-10, 0, 0), pivot=(x + 0.6, -3.4, 6.2))
    # tail
    m.bone('tail1', 'body', (0, 23, 17.5), V(-26, 0, 0))
    m.cube('tail1', 'tail1', (-2.8, 18.2, 16.5), (5.6, 5.8, 9.5), 'fur')
    m.bone('tail2', 'tail1', (0, 21, 26), V(-16, 0, 0))
    m.cube('tail2', 'tail2', (-3.3, 15.6, 25), (6.6, 6.6, 9.5), 'fur')
    m.bone('tail3', 'tail2', (0, 19, 34.5), V(-12, 0, 0))
    m.cube('tail3', 'tail3', (-2.8, 12.8, 33.5), (5.6, 7.4, 6.5), 'fur')
    m.mirror()
    # ---- the shaggy black mane: spiky tufts swept back over neck, shoulders and spine
    for name, pivot, rows in (
            ('mane_a', (0, 28, -15), [(-24, 4.4, 6.5, 6), (-21, 5.6, 7.5, 7), (-18, 6.4, 8.5, 7), (-15, 7.0, 8.8, 8), (-12, 7.2, 8.5, 8), (-9, 7.0, 8, 8), (-6, 6.4, 7, 7)]),
            ('mane_b', (0, 27.5, -3), [(-4, 5.8, 7, 7), (-1, 5.2, 6.4, 7), (2, 4.8, 6, 6), (5, 4.4, 5.6, 6), (8, 4.0, 5, 5), (11, 3.6, 4.6, 5), (14, 3.2, 4.2, 4)]),
    ):
        m.bone(name, 'body', pivot)
        for ri, (z, hw, h, n) in enumerate(rows):
            for i in range(n):
                fx = (i / (n - 1) - 0.5) * 2 if n > 1 else 0          # -1..1 across the back
                x = fx * hw * 1.25
                r1, r2, r3 = hrand(name, ri, i, 1), hrand(name, ri, i, 2), hrand(name, ri, i, 3)
                # tufts on the sides hang over the shoulders; top ones stand up and sweep back
                drop = abs(fx) ** 1.4
                y0 = 29.0 - drop * 9.5 - 0.4 * ri
                hh = h * (0.8 + r1 * 0.5) * (1 - 0.1 * drop)
                w, d = 3.2 + r2 * 1.6, 3.0 + r3 * 1.4
                m.cube(name, '%s_%d_%d' % (name, ri, i), (x - w / 2, y0, z - d / 2), (w, hh, d), 'mane', den=1,
                       rot=V(-(26 + r2 * 28), (r3 - 0.5) * 26, -fx * (20 + 26 * drop) + (r1 - 0.5) * 10),
                       pivot=(x, y0 + 0.4, z))
    m.cube('mane_a', 'mane_base_a', (-7.8, 25.6, -26), (15.6, 4.2, 21), 'mane')
    m.cube('mane_b', 'mane_base_b', (-6.6, 25.8, -5), (13.2, 3.4, 20), 'mane')
    m.cube('mane_a', 'mane_cape_l', (7.4, 17.5, -22), (2.6, 10.5, 15), 'mane', rot=V(0, 0, -10), pivot=(8.5, 27, -15))
    m.cube('mane_a', 'mane_cape_r', (-10.0, 17.5, -22), (2.6, 10.5, 15), 'mane', rot=V(0, 0, 10), pivot=(-8.5, 27, -15))
    m.cube('mane_b', 'mane_flank_l', (6.4, 18.5, -4), (2.2, 8.5, 12), 'mane', rot=V(0, 0, -8), pivot=(7.5, 26.5, 2))
    m.cube('mane_b', 'mane_flank_r', (-8.6, 18.5, -4), (2.2, 8.5, 12), 'mane', rot=V(0, 0, 8), pivot=(-7.5, 26.5, 2))
    m.bone('mane_chest', 'body', (0, 17, -14))
    for i in range(6):
        fx = i / 5 * 2 - 1
        r1, r2 = hrand('mc', i, 1), hrand('mc', i, 2)
        w = 3.0 + r1
        m.cube('mane_chest', 'ruff_%d' % i, (fx * 5.8 - w / 2, 7.6 + r2, -17.5 + r1), (w, 7.5 + r2 * 2, 2.6), 'mane', rot=V(-8 - r1 * 12, 0, -fx * 8), pivot=(fx * 5.8, 15.5, -17))
    m.shift_y(2.7)   # paws were modelled 2.7 px below the ground plane
    return m


FP = [hexc(c) for c in ('0a0d13', '121824', '1b2335', '263149', '344263', '46587f', '5d729b')]
FPD = [hexc(c) for c in ('07080c', '0d1118', '141a26', '1d2638', '2a364f')]
MANE = [hexc(c) for c in ('050608', '0a0c11', '11151e', '1a2130', '263148', '38476a', '51679a')]
BELLY = [hexc(c) for c in ('10151f', '19202f', '232d42', '2f3c57', '3f4f72')]
SNOUT = [hexc(c) for c in ('0e1219', '161c27', '202938', '2c374c', '3d4b66')]
LIP = [hexc(c) for c in ('070608', '0d0a0e', '171015', '241921')]
CLAW = [hexc(c) for c in ('9d957d', 'c4bda5', 'dfd9c4', 'f0ecdc', 'fbf9f0')]
TOOTH = [hexc(c) for c in ('a89f86', 'cfc7ad', 'e6dfc8', 'f4f0e0')]
TONGUE = [hexc(c) for c in ('3d0f17', '561823', '73242f', '8f3441')]
NOSE = [hexc(c) for c in ('050507', '0b0c10', '14161d', '23262f')]
FSH = {'up': 0.10, 'north': 0.02, 'south': -0.04, 'east': -0.02, 'west': -0.02, 'down': -0.2}


def dith(ctx, a=0.05):
    return (hrand(ctx['i'], ctx['j'], len(ctx['cube']['name'])) - 0.5) * a


def streak_t(ctx, base, axis, k=1.3, seed=3, kl=0.16):
    """fur: strands run along `axis` (0 x, 1 y, 2 z): high noise frequency across, low along."""
    p = ctx['p']
    f = [k, k, k]
    f[axis] = kl
    s = fbm(p[0] * f[0], p[1] * f[1], p[2] * f[2], seed, 2)
    t = base + (s - 0.5) * 0.62 + FSH[ctx['face']] * 1.5
    t -= 0.2 * (1 - ctx['lp'][1])
    n = fbm(p[0] * 0.15, p[1] * 0.15, p[2] * 0.15, seed + 9, 2)
    return t + (n - 0.5) * 0.28 + dith(ctx)


def sh_fur(ctx):
    n = ctx['cube']['name']
    ax = 2 if n in ('chest', 'abdomen', 'rump', 'back_strip', 'hump') or n.startswith('tail') else 1
    return ramp(FP, streak_t(ctx, 0.5, ax))


def sh_fur_leg(ctx): return ramp(FP, streak_t(ctx, 0.46, 1, 1.4, 5))
def sh_fur_dark(ctx): return ramp(FPD, streak_t(ctx, 0.5, 1, 1.4, 6))
def sh_fur_belly(ctx): return ramp(BELLY, streak_t(ctx, 0.45, 2, 1.2, 7))


def sh_fur_head(ctx):
    t = streak_t(ctx, 0.52, 2, 1.5, 8, 0.2)
    c = ramp(FP, t)
    if ctx['face'] == 'north' and ctx['cube']['name'].startswith('brow'):
        return tuple(int(k * 0.6) for k in c)
    return c


def sh_mane(ctx):
    lp = ctx['lp']
    t = streak_t(ctx, 0.3 + 0.3 * lp[1], 1, 1.5, 11, 0.12)
    if lp[1] > 0.7 and hrand(ctx['i'], ctx['j'], 4) > 0.82:
        t += 0.25                                                          # blue-grey strand tips catching the light
    return ramp(MANE, t)


def sh_snout(ctx):
    p = ctx['p']
    t = streak_t(ctx, 0.5, 2, 1.6, 13, 0.3)
    c = ramp(SNOUT, t)
    if ctx['face'] == 'down' and ctx['cube']['name'] == 'snout':
        return ramp(LIP, 0.4)
    w = abs(math.sin(p[2] * 2.3 + p[1] * 1.0 + fbm(p[0] * .5, p[1] * .5, p[2] * .5, 4) * 2.5))   # snarl wrinkles
    if ctx['cube']['name'] in ('snout', 'bridge') and w < 0.14 and ctx['face'] != 'down':
        return tuple(int(k * 0.55) for k in c)
    return c


def sh_lip(ctx):
    c = ramp(LIP, 0.45 + fbm(ctx['p'][0] * .7, ctx['p'][1] * .7, ctx['p'][2] * .7, 3) * 0.4 + FSH[ctx['face']])
    if ctx['face'] == 'up' and ctx['cube']['name'].startswith('lip_lo'):
        return ramp(TONGUE, 0.35 + fbm(ctx['p'][0] * .9, 0, ctx['p'][2] * .9, 2) * 0.4)
    if ctx['face'] == 'down' and ctx['cube']['name'].startswith('lip_up'):
        return ramp(TONGUE, 0.3 + fbm(ctx['p'][0] * .9, 0, ctx['p'][2] * .9, 2) * 0.4)
    return c


def sh_nose(ctx):
    t = 0.3 + fbm(ctx['p'][0], ctx['p'][1], ctx['p'][2], 2) * 0.5 + FSH[ctx['face']]
    if ctx['face'] == 'north' and 0.15 < abs(ctx['fx'] - 0.5) < 0.38 and 0.35 < ctx['fy'] < 0.8:
        return (2, 2, 4)
    if ctx['face'] == 'up' and hrand(ctx['i'], ctx['j'], 1) > 0.8:
        t += 0.4                                                           # wet gleam
    return ramp(NOSE, t)


def sh_tooth(ctx):
    t = 0.3 + (1 - ctx['lp'][1]) * 0.55 + FSH[ctx['face']] * 0.8
    return ramp(TOOTH, t)


def sh_claw(ctx):
    lp = ctx['lp']
    t = 0.25 + (1 - lp[2]) * 0.6 + FSH[ctx['face']] * 0.8 + dith(ctx, 0.12)    # darker at the root, bright white tip
    return ramp(CLAW, t)


def sh_tongue(ctx): return ramp(TONGUE, 0.4 + fbm(ctx['p'][0] * .8, 0, ctx['p'][2] * .8, 2) * 0.5 + FSH[ctx['face']])


def sh_ear(ctx):
    c = sh_fur_head(ctx)
    if ctx['face'] == 'north' and 0.2 < ctx['fx'] < 0.8 and ctx['fy'] > 0.1:
        return (int(c[0] * 0.6 + 16), int(c[1] * 0.5 + 8), int(c[2] * 0.6 + 14))
    return c


def sh_eye(ctx):
    f = ctx['face']
    if f == 'north':
        dx, dy = abs(ctx['fx'] - 0.5) * 2, abs(ctx['fy'] - 0.5) * 2
        if dx + dy < 0.55:
            return (255, 232, 205, 'glow')
        if dx * dx + dy * dy < 0.9:
            return (255, 48, 30, 'glow')
        return (170, 10, 14, 'glow')
    return (140, 8, 12, 'glow')


SHADERS = {'fur': sh_fur, 'fur_leg': sh_fur_leg, 'fur_dark': sh_fur_dark, 'fur_belly': sh_fur_belly, 'fur_head': sh_fur_head,
           'mane': sh_mane, 'snout': sh_snout, 'lip': sh_lip, 'nose': sh_nose, 'tooth': sh_tooth, 'claw': sh_claw,
           'tongue': sh_tongue, 'ear': sh_ear, 'eye': sh_eye}


# ------------------------------------------------------------------ animations
def mir(v): return [v[0], -v[1], -v[2]]


def Lk(a, base, t, v, ease=None, chan='rotation', r=None):
    a.k(base + '_l', chan, t, v, ease)
    a.k(base + '_r', chan, t, r if r is not None else (mir(v) if chan == 'rotation' else [-v[0], v[1], v[2]]), ease)


def build_anims():
    N = 'animation.peter_hale_alpha.'
    out = []
    SIN = lambda a, b, base, amp, ph=0.0, cyc=1, n=12: a.sine(b, 'rotation', base, amp, ph, n, cyc)

    # --- idle: heavy breathing, head swaying low, tail and mane stirring, a hungry snarl
    a = Anim(N + 'idle', 3.6, True)
    a.sine('body', 'rotation', V(0, 0, 0), V(1.2, 0, 0.6))
    a.sine('body', 'position', V(0, 0, 0), V(0, 0.35, 0), 0.0)
    SIN(a, 'neck', V(0, 0, 0), V(-2, 4, 0), 0.15)
    SIN(a, 'head', V(0, 0, 0), V(2.5, 6, 1.5), 0.3)
    SIN(a, 'jaw', V(0, 0, 0), V(3, 0, 0), 0.05)
    SIN(a, 'tail1', V(0, 0, 0), V(2, 9, 0), 0.0)
    SIN(a, 'tail2', V(0, 0, 0), V(2, 12, 0), 0.15)
    SIN(a, 'tail3', V(0, 0, 0), V(2, 14, 0), 0.3)
    SIN(a, 'mane_a', V(0, 0, 0), V(3, 0, 1.5), 0.2)
    SIN(a, 'mane_b', V(0, 0, 0), V(3, 0, 1.5), 0.3)
    SIN(a, 'mane_chest', V(0, 0, 0), V(3, 0, 0), 0.1)
    for s, ph in (('l', 0), ('r', 0.5)):
        SIN(a, 'ear_' + s, V(0, 0, 0), V(0, 0, 3 if s == 'l' else -3), 0.4 + ph)
    # weight shift from paw to paw
    SIN(a, 'foreleg_l', V(0, 0, 0), swing(2), 0.0)
    SIN(a, 'foreleg_r', V(0, 0, 0), swing(2), 0.5)
    out.append(a)

    def quad(a, amp_f, amp_h, flex_f, flex_h, ph_fl=0.0, ph_fr=0.5, ph_hl=0.5, ph_hr=0.0, hock=14):
        for s, pf, ph in (('l', ph_fl, ph_hl), ('r', ph_fr, ph_hr)):
            SIN(a, 'foreleg_' + s, V(0, 0, 0), swing(amp_f), pf)
            SIN(a, 'foreleg2_' + s, V(flex_f, 0, 0), V(flex_f, 0, 0), pf + 0.25)
            SIN(a, 'paw_' + s, V(0, 0, 0), V(8, 0, 0), pf + 0.05)
            SIN(a, 'hindleg_' + s, V(0, 0, 0), swing(amp_h), ph)
            SIN(a, 'shin_' + s, V(flex_h, 0, 0), V(flex_h, 0, 0), ph + 0.25)
            SIN(a, 'hock_' + s, V(0, 0, 0), V(hock, 0, 0), ph + 0.45)
            SIN(a, 'foot_' + s, V(0, 0, 0), V(10, 0, 0), ph + 0.1)

    # --- walk: prowling diagonal gait, head low, mane and tail swaying
    a = Anim(N + 'walk', 1.2, True)
    quad(a, 24, 20, 20, 16)
    a.sine('body', 'position', V(0, 0, 0), V(0, 0.8, 0), 0.25, 6, 2)
    a.sine('body', 'rotation', V(0, 0, 0), V(0, 3.5, 2.2), 0.0)
    SIN(a, 'neck', V(2, 0, 0), V(2, -5, 0), 0.5)
    SIN(a, 'head', V(2, 0, 0), V(2.5, 3, -1.5), 0.0)
    SIN(a, 'jaw', V(2, 0, 0), V(2, 0, 0), 0.2, 2, 6)
    SIN(a, 'tail1', V(4, 0, 0), V(2, 10, 0), 0.2)
    SIN(a, 'tail2', V(0, 0, 0), V(2, 12, 0), 0.35)
    SIN(a, 'tail3', V(0, 0, 0), V(2, 14, 0), 0.5)
    SIN(a, 'mane_a', V(0, 0, 0), V(4, 0, 1.5), 0.25, 2, 6)
    SIN(a, 'mane_b', V(0, 0, 0), V(4, 0, 1.5), 0.35, 2, 6)
    SIN(a, 'mane_chest', V(0, 0, 0), V(4, 0, 0), 0.2, 2, 6)
    out.append(a)

    # --- run: bounding gallop, fore pair and hind pair together, spine rocking
    a = Anim(N + 'run', 0.6, True)
    quad(a, 42, 38, 30, 26, 0.0, 0.06, 0.5, 0.56, 22)
    a.sine('body', 'position', V(0, 0, 0), V(0, 3.0, 0), 0.0)
    a.sine('body', 'rotation', V(0, 0, 0), V(7, 0, 0), 0.25)
    SIN(a, 'neck', V(-3, 0, 0), V(-5, 0, 0), 0.5)
    SIN(a, 'head', V(-5, 0, 0), V(-4, 0, 0), 0.5)
    SIN(a, 'jaw', V(14, 0, 0), V(5, 0, 0), 0.0)
    SIN(a, 'tail1', V(10, 0, 0), V(5, 4, 0), 0.5)
    SIN(a, 'tail2', V(6, 0, 0), V(5, 4, 0), 0.6)
    SIN(a, 'tail3', V(6, 0, 0), V(5, 4, 0), 0.7)
    SIN(a, 'mane_a', V(14, 0, 0), V(8, 0, 0), 0.5)
    SIN(a, 'mane_b', V(14, 0, 0), V(8, 0, 0), 0.6)
    SIN(a, 'mane_chest', V(10, 0, 0), V(6, 0, 0), 0.5)
    out.append(a)

    # --- attack_swipe: rear up on the hind legs a touch and rake with the right foreleg
    a = Anim(N + 'attack_swipe', 1.0, False)
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('foreleg_r', ((0, V(0, 0, 0), None), (0.30, V(-70, 0, 14), 'easeOutCubic'),
                    (0.50, V(52, 0, -26), 'easeInQuad'), (0.68, V(40, 0, -20), 'easeOutCubic'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('foreleg2_r', ((0, V(0, 0, 0), None), (0.30, V(30, 0, 0), 'easeOutCubic'), (0.50, V(-16, 0, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('paw_r', ((0, V(0, 0, 0), None), (0.30, V(-30, 0, 0), 'easeOutCubic'), (0.50, V(34, 0, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('foreleg_l', ((0, V(0, 0, 0), None), (0.30, V(18, 0, 0), 'easeOutCubic'), (0.50, V(-14, 0, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('body', ((0, V(0, 0, 0), None), (0.30, V(-9, -8, -3), 'easeOutCubic'), (0.50, V(8, 10, 2), 'easeInQuad'), (0.68, V(5, 8, 2), 'easeOutCubic'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('neck', ((0, V(0, 0, 0), None), (0.30, V(-8, 6, 0), 'easeOutCubic'), (0.50, V(8, -10, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('head', ((0, V(0, 0, 0), None), (0.30, V(-6, 8, 0), 'easeOutCubic'), (0.50, V(6, -10, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('jaw', ((0, V(0, 0, 0), None), (0.30, V(18, 0, 0), 'easeOutCubic'), (0.50, V(26, 0, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(0, -0.5, 2.2), 'easeOutCubic'), (0.50, V(0, 0, -3), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')):
        a.k('body', 'position', t, v, e)
    K('tail1', ((0, V(0, 0, 0), None), (0.30, V(-6, -14, 0), 'easeOutCubic'), (0.50, V(4, 18, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    K('mane_a', ((0, V(0, 0, 0), None), (0.30, V(14, 0, 0), 'easeOutCubic'), (0.50, V(-8, 0, 0), 'easeInQuad'), (1.0, V(0, 0, 0), 'easeInOutSine')))
    out.append(a)

    # --- attack_bite: coil back, lunge with the whole body, clamp, shake
    a = Anim(N + 'attack_bite', 1.2, False)
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('body', ((0, V(0, 0, 0), None), (0.35, V(-7, 0, 0), 'easeOutCubic'), (0.55, V(9, 0, 0), 'easeInQuad'), (0.8, V(5, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('neck', ((0, V(0, 0, 0), None), (0.35, V(-14, 0, 0), 'easeOutCubic'), (0.55, V(20, 0, 0), 'easeInQuad'), (0.8, V(12, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('head', ((0, V(0, 0, 0), None), (0.35, V(-12, 0, 0), 'easeOutCubic'), (0.55, V(14, 0, 0), 'easeInQuad'), (0.8, V(8, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('jaw', ((0, V(0, 0, 0), None), (0.35, V(30, 0, 0), 'easeOutCubic'), (0.55, V(-6, 0, 0), 'easeInQuart'), (0.8, V(0, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    for t, v in ((0.58, V(0, -10, 0)), (0.64, V(0, 10, 0)), (0.70, V(0, -8, 0)), (0.76, V(0, 6, 0)), (0.84, V(0, 0, 0))):
        a.k('neck', 'rotation', t, V(20 - (t - 0.55) * 20, v[1], 0), 'easeInOutSine')
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, V(0, -1.5, 3), 'easeOutCubic'), (0.55, V(0, 1.5, -9), 'easeInQuad'), (0.8, V(0, 0, -6), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        a.k('root', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, swing(-8), 'easeOutCubic'), (0.55, swing(30), 'easeInQuad'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'foreleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, swing(-16), 'easeOutCubic'), (0.55, swing(14), 'easeInQuad'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'hindleg', t, v, e)
    K('tail1', ((0, V(0, 0, 0), None), (0.55, V(14, 0, 0), 'easeInQuad'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('mane_a', ((0, V(0, 0, 0), None), (0.35, V(-6, 0, 0), 'easeOutCubic'), (0.58, V(18, 0, 0), 'easeInQuad'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    out.append(a)

    # --- roar: rear the chest up, head thrown back, mane flared, shuddering
    a = Anim(N + 'roar', 2.6, False)
    up = 0.8
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('body', ((0, V(0, 0, 0), None), (0.35, V(7, 0, 0), 'easeInOutSine'), (up, V(-18, 0, 0), 'easeOutCubic'), (1.9, V(-16, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    K('neck', ((0, V(0, 0, 0), None), (0.35, V(10, 0, 0), 'easeInOutSine'), (up, V(-26, 0, 0), 'easeOutCubic'), (1.9, V(-24, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    K('head', ((0, V(0, 0, 0), None), (0.35, V(8, 0, 0), 'easeInOutSine'), (up, V(-24, 0, 0), 'easeOutBack'), (1.9, V(-22, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    K('jaw', ((0, V(0, 0, 0), None), (0.35, V(0, 0, 0), 'easeInOutSine'), (up, V(34, 0, 0), 'easeOutCubic'), (1.9, V(32, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, V(0, -1.2, 0), 'easeInOutSine'), (up, V(0, 3.5, 5), 'easeOutCubic'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('body', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.35, swing(-8), 'easeInOutSine'), (up, swing(-14), 'easeOutCubic'), (1.9, swing(-14), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'foreleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (up, V(-6, 0, 0), 'easeOutCubic'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'hindleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (up, V(-8, 0, 0), 'easeOutCubic'), (2.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'shin', t, v, e)
    K('mane_a', ((0, V(0, 0, 0), None), (up, V(24, 0, 0), 'easeOutBack'), (1.9, V(20, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    K('mane_b', ((0, V(0, 0, 0), None), (up, V(18, 0, 0), 'easeOutBack'), (1.9, V(14, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    K('mane_chest', ((0, V(0, 0, 0), None), (up, V(22, 0, 0), 'easeOutBack'), (1.9, V(18, 0, 0), 'easeInOutSine'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    K('tail1', ((0, V(0, 0, 0), None), (up, V(20, 0, 0), 'easeOutCubic'), (2.6, V(0, 0, 0), 'easeInOutSine')))
    ts = [up + 0.12 * i for i in range(10)]
    for i, t in enumerate(ts):
        sg = 1 if i % 2 == 0 else -1
        a.k('head', 'rotation', t + 0.05, V(-22 + sg * 1.5, sg * 2.5, sg * 2.5), 'easeInOutSine')
        a.k('mane_a', 'rotation', t, V(22 + sg * 6, 0, 0), 'easeInOutSine')
        a.k('ear_l', 'rotation', t, V(0, 0, 4 * sg), 'easeInOutSine')
        a.k('ear_r', 'rotation', t, V(0, 0, -4 * sg), 'easeInOutSine')
    out.append(a)

    # --- hurt: flinch back and snap sideways
    a = Anim(N + 'hurt', 0.6, False)
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('body', ((0, V(0, 0, 0), None), (0.10, V(-7, 6, 3), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')))
    K('neck', ((0, V(0, 0, 0), None), (0.10, V(-10, 12, 0), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')))
    K('head', ((0, V(0, 0, 0), None), (0.10, V(-8, 14, 6), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')))
    K('jaw', ((0, V(0, 0, 0), None), (0.10, V(18, 0, 0), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')))
    K('tail1', ((0, V(0, 0, 0), None), (0.10, V(12, 20, 0), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')))
    K('mane_a', ((0, V(0, 0, 0), None), (0.10, V(16, 0, 0), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')))
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, V(0, 0, 2.4), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        a.k('root', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.10, swing(-10), 'easeOutCubic'), (0.6, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'foreleg', t, v, e)
    out.append(a)

    # --- death: forelegs buckle, chest drops, the beast rolls onto its side and goes still
    a = Anim(N + 'death', 2.4, 'hold_on_last_frame')
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('body', ((0, V(0, 0, 0), None), (0.25, V(-9, 0, 0), 'easeOutCubic'), (0.9, V(9, 0, 0), 'easeInOutSine'), (2.4, V(9, 0, 0), None)))
    K('neck', ((0, V(0, 0, 0), None), (0.25, V(-14, 8, 0), 'easeOutCubic'), (0.9, V(16, 0, 0), 'easeInOutSine'), (1.6, V(18, 0, 12), 'easeInOutSine')))
    K('head', ((0, V(0, 0, 0), None), (0.25, V(-12, 8, 0), 'easeOutCubic'), (0.9, V(14, 0, 0), 'easeInOutSine'), (1.6, V(16, 8, 10), 'easeInOutSine')))
    K('jaw', ((0, V(0, 0, 0), None), (0.25, V(30, 0, 0), 'easeOutCubic'), (1.6, V(18, 0, 0), 'easeInOutSine')))
    for t, v in ((0, V(0, 0, 0)), (0.25, swing(-6)), (0.9, swing(10)), (1.6, swing(20))):
        Lk(a, 'foreleg', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.9, V(-46, 0, 0)), (1.6, V(-60, 0, 0))):
        Lk(a, 'foreleg2', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.9, V(24, 0, 0)), (1.6, V(30, 0, 0))):
        Lk(a, 'paw', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.9, swing(24)), (1.6, swing(34))):
        Lk(a, 'hindleg', t, v, 'easeInOutSine' if t else None)
    for t, v in ((0, V(0, 0, 0)), (0.9, V(18, 0, 0)), (1.6, V(30, 0, 0))):
        Lk(a, 'shin', t, v, 'easeInOutSine' if t else None)
    for t, v, e in ((0, V(0, 0, 0), None), (0.25, V(0, 0, 1.5), 'easeOutCubic'), (0.9, V(0, -5.5, 0), 'easeInOutSine'), (1.5, V(0, -5.5, 0), None)):
        a.k('body', 'position', t, v, e)
    K('root', ((0, V(0, 0, 0), None), (1.0, V(0, 0, 0), None), (1.7, V(0, 0, 80), 'easeInCubic'), (1.9, V(0, 0, 76), 'easeOutBounce'), (2.4, V(0, 0, 78), 'easeInOutSine')))
    for t, v, e in ((0, V(0, 0, 0), None), (1.0, V(0, 0, 0), None), (1.7, V(-10, 7.5, 0), 'easeInCubic'), (1.9, V(-10, 7.9, 0), 'easeOutBounce'), (2.4, V(-10, 7.5, 0), 'easeInOutSine')):
        a.k('root', 'position', t, v, e)
    K('tail1', ((0, V(0, 0, 0), None), (0.3, V(10, 0, 0), 'easeOutCubic'), (1.6, V(-6, 14, 0), 'easeInOutSine')))
    K('mane_a', ((0, V(0, 0, 0), None), (0.25, V(14, 0, 0), 'easeOutCubic'), (1.6, V(-4, 0, 0), 'easeInOutSine')))
    out.append(a)

    # --- rise_up: from all fours, rear onto the hind legs and hold the stance
    a = Anim(N + 'rise_up', 1.6, 'hold_on_last_frame')
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('root', ((0, V(0, 0, 0), None), (0.40, V(6, 0, 0), 'easeOutCubic'), (1.2, V(-62, 0, 0), 'easeInOutCubic'), (1.6, V(-66, 0, 0), 'easeOutBack')))
    K('body', ((0, V(0, 0, 0), None), (1.2, V(-6, 0, 0), 'easeInOutCubic'), (1.6, V(-10, 0, 0), 'easeOutCubic')))
    K('neck', ((0, V(0, 0, 0), None), (1.2, V(16, 0, 0), 'easeInOutCubic'), (1.6, V(20, 0, 0), 'easeOutCubic')))
    K('head', ((0, V(0, 0, 0), None), (1.2, V(30, 0, 0), 'easeInOutCubic'), (1.6, V(34, 0, 0), 'easeOutCubic')))
    K('jaw', ((0, V(0, 0, 0), None), (1.2, V(14, 0, 0), 'easeInOutCubic'), (1.6, V(10, 0, 0), 'easeOutCubic')))
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(0, -2, 2), 'easeOutCubic'), (1.2, V(0, 11.8, 0), 'easeInOutCubic'), (1.6, V(0, 12.6, 0), 'easeOutCubic')):
        a.k('root', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, swing(10), 'easeOutCubic'), (1.2, swing(-56), 'easeInOutCubic'), (1.6, swing(-60), 'easeOutBack')):
        Lk(a, 'foreleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.2, V(-24, 0, 0), 'easeInOutCubic'), (1.6, V(-30, 0, 0), 'easeOutCubic')):
        Lk(a, 'foreleg2', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, swing(10), 'easeOutCubic'), (1.2, V(46, 0, 0), 'easeInOutCubic'), (1.6, V(52, 0, 0), 'easeOutCubic')):
        Lk(a, 'hindleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.40, V(10, 0, 0), 'easeOutCubic'), (1.2, V(-26, 0, 0), 'easeInOutCubic'), (1.6, V(-30, 0, 0), 'easeOutCubic')):
        Lk(a, 'shin', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.2, V(-6, 0, 0), 'easeInOutCubic'), (1.6, V(-8, 0, 0), 'easeOutCubic')):
        Lk(a, 'hock', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (1.2, V(-18, 0, 0), 'easeInOutCubic'), (1.6, V(-20, 0, 0), 'easeOutCubic')):
        Lk(a, 'foot', t, v, e)
    K('tail1', ((0, V(0, 0, 0), None), (1.2, V(92, 0, 0), 'easeInOutCubic'), (1.6, V(100, 0, 0), 'easeOutCubic')))
    K('tail2', ((0, V(0, 0, 0), None), (1.2, V(10, 0, 0), 'easeInOutCubic'), (1.6, V(14, 0, 0), 'easeOutCubic')))
    K('mane_a', ((0, V(0, 0, 0), None), (1.2, V(20, 0, 0), 'easeInOutCubic'), (1.6, V(10, 0, 0), 'easeOutBack')))
    out.append(a)

    # --- pounce: coil low, explode forward, land on the forepaws
    a = Anim(N + 'pounce', 1.2, False)
    K = lambda b, ks: [a.k(b, 'rotation', t, v, e) for t, v, e in ks]
    K('body', ((0, V(0, 0, 0), None), (0.30, V(10, 0, 0), 'easeOutCubic'), (0.50, V(-14, 0, 0), 'easeInQuad'), (0.78, V(8, 0, 0), 'easeInOutSine'), (0.95, V(2, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('neck', ((0, V(0, 0, 0), None), (0.30, V(-8, 0, 0), 'easeOutCubic'), (0.50, V(-10, 0, 0), 'easeInQuad'), (0.78, V(14, 0, 0), 'easeInOutSine'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('head', ((0, V(0, 0, 0), None), (0.30, V(-6, 0, 0), 'easeOutCubic'), (0.50, V(-6, 0, 0), 'easeInQuad'), (0.78, V(8, 0, 0), 'easeInOutSine'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('jaw', ((0, V(0, 0, 0), None), (0.30, V(10, 0, 0), 'easeOutCubic'), (0.50, V(34, 0, 0), 'easeInQuad'), (0.78, V(30, 0, 0), 'easeInOutSine'), (0.95, V(0, 0, 0), 'easeInQuart'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(0, -6, 3), 'easeOutCubic'), (0.50, V(0, 4, -4), 'easeInQuad'), (0.70, V(0, 11, -18), 'easeOutQuad'), (0.88, V(0, 0, -30), 'easeInQuad'), (1.0, V(0, -1, -30), 'easeOutCubic'), (1.2, V(0, 0, -30), 'easeInOutSine')):
        a.k('root', 'position', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, swing(-18), 'easeOutCubic'), (0.50, swing(48), 'easeInQuad'), (0.78, swing(40), 'easeInOutSine'), (0.95, swing(6), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'foreleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(30, 0, 0), 'easeOutCubic'), (0.50, V(-18, 0, 0), 'easeInQuad'), (0.95, V(12, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'foreleg2', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, swing(36), 'easeOutCubic'), (0.50, swing(-42), 'easeInQuad'), (0.78, swing(-24), 'easeInOutSine'), (1.0, swing(14), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'hindleg', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(-26, 0, 0), 'easeOutCubic'), (0.50, V(30, 0, 0), 'easeInQuad'), (0.78, V(14, 0, 0), 'easeInOutSine'), (1.0, V(-10, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'shin', t, v, e)
    for t, v, e in ((0, V(0, 0, 0), None), (0.30, V(16, 0, 0), 'easeOutCubic'), (0.50, V(-30, 0, 0), 'easeInQuad'), (1.2, V(0, 0, 0), 'easeInOutSine')):
        Lk(a, 'hock', t, v, e)
    K('tail1', ((0, V(0, 0, 0), None), (0.30, V(-10, 0, 0), 'easeOutCubic'), (0.55, V(24, 0, 0), 'easeInQuad'), (1.0, V(-6, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('mane_a', ((0, V(0, 0, 0), None), (0.50, V(22, 0, 0), 'easeInQuad'), (0.95, V(-10, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    K('mane_b', ((0, V(0, 0, 0), None), (0.50, V(20, 0, 0), 'easeInQuad'), (0.95, V(-8, 0, 0), 'easeOutCubic'), (1.2, V(0, 0, 0), 'easeInOutSine')))
    out.append(a)
    return out


if __name__ == '__main__':
    root = sys.argv[1]
    m = build()
    m.pack(256)
    img, glow = m.paint(SHADERS)
    ident = 'peter_hale_alpha'
    res = os.path.join(root, 'src/main/resources/assets/supernaturalbloodlines')
    save_json(os.path.join(res, 'geo/entity/%s.geo.json' % ident), m.geo())
    tex = os.path.join(res, 'textures/entity/boss/%s_geo.png' % ident)
    os.makedirs(os.path.dirname(tex), exist_ok=True)
    img.save(tex)
    glow.save(tex.replace('_geo.png', '_geo_glowmask.png'))
    save_json(os.path.join(res, 'animations/entity/%s.animation.json' % ident), anim_file(build_anims()))
    print('cubes', len(m.cubes), 'bones', len(m.order), 'tex', m.tw, m.th)
