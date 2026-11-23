"""Peter Hale's morph clips as a pose sheet: shift_in (1.0 s), the roar re-timed to 2.2 s and shift_out (1.0 s).

    python art/creatures/morph/tools/peter_keys.py <out.json>      (from the repo root; the old roar is read from origin/main)

Like klaus_keys.py it writes a file to import into the .bbmodel, never the game file. Peter's alpha is a wolf five blocks long, so the first frame is
it at half its size, reared in the man's outline. His rhythm is slow and sure: a breath in, then he lowers himself onto one forepaw and, a
tenth of a second later, the other, and the weight settles through him late. Where Klaus falls, Peter sets himself down.
"""
import json
import subprocess
import sys

import morph_rig as mr
from klaus_keys import tr

MODEL = 'peter_hale_alpha'
FORE_P, FORE_R = [(28.2, -9.5), (17.2, -9.4), (8.5, -9.6)], [0, -8, 8]
HIND_P, HIND_R = [(27.7, 14.0), (15.7, 14.2), (6.1, 14.4), (-0.3, 14.8)], [-26, 52, -30, -12]
BODY = (24.7, 0.0)
RIG = mr.Rig('body', BODY, [], {
    'fl': mr.Limb(('foreleg_l', 'foreleg2_l', 'paw_l'), FORE_P, FORE_R), 'fr': mr.Limb(('foreleg_r', 'foreleg2_r', 'paw_r'), FORE_P, FORE_R),
    'hl': mr.Limb(('hindleg_l', 'shin_l', 'foot_l'), HIND_P, HIND_R), 'hr': mr.Limb(('hindleg_r', 'shin_r', 'foot_r'), HIND_P, HIND_R)}, pivot_z=13.0)
LIMB_BONES = {b for L in RIG.limbs.values() for b in L.bones}
# The first frame of idle: where shift_in ends and where the roar starts and ends.
I = {'neck': (-1.618, 3.236, 0.0), 'head': (2.378, 5.706, 1.427), 'jaw': (0.927, 0.0, 0.0), 'tail1': (0.0, 0.0, 0.0), 'tail2': (1.618, 9.708, 0.0),
     'tail3': (1.902, 13.315, 0.0), 'mane_a': (2.853, 0.0, 1.427), 'mane_b': (2.853, 0.0, 1.427), 'mane_chest': (1.763, 0.0, 0.0),
     'ear_l': (0.0, 0.0, 1.763), 'ear_r': (0.0, 0.0, 1.763)}
PELVIS = HIND_P[0]

# The match pose: reared on folded haunches at half his size, the spine 72 degrees up, the neck curled over so the nape is the top, the left claw higher.
M_S, M_HP, M_PELVIS = 0.50, -72.0, (7.4, 3.0)
M_HOOK = {'fl': (-3.0, -4.6), 'fr': (-4.8, -3.0)}                    # wrist from the shoulder (down, forward), world px
M_HIND = {'hl': -10.0, 'hr': -6.0}                                   # world px forward of the rest spots: he stands where the man stood
M = {'neck': (36.0, 3.0, 0.0), 'head': (-14.0, 6.0, 3.0), 'jaw': (14.0, 0.0, 0.0), 'tail1': (100.0, -5.0, 0.0), 'tail2': (18.0, -8.0, 0.0), 'tail3': (12.0, -10.0, 0.0),
     'mane_a': (12.0, 0.0, 0.0), 'mane_b': (6.0, 0.0, 0.0), 'mane_chest': (-10.0, 0.0, 0.0), 'ear_l': (-14.0, 0.0, 0.0), 'ear_r': (-10.0, 0.0, -3.0)}


def mix(a, b, u):
    return tuple(x + (y - x) * u for x, y in zip(a, b))


def body_from_pelvis(pelvis, hp, s):
    """Where the body bone's pivot is (world px) when the pelvis (the hind legs' joint) is at `pelvis`, the body is pitched hp and the root is scaled s."""
    d = mr.R(mr.sub(PELVIS, BODY), hp)
    return (pelvis[0] - s * d[0], pelvis[1] - s * d[1])


def forepaw(st, name, hook, u):
    """A forepaw between a point held off the shoulder (u = 0) and its own spot on the ground (u = 1)."""
    sh = RIG.joint_world(st, name)
    grow = st['s'] / M_S
    return mix((sh[0] + hook[0] * grow, sh[1] + hook[1] * grow), RIG.rest_end_world(name), u)


def step(t, a, b, frm, to, lift, toes):
    """A hind foot stepping from z offset a to b between two times."""
    mid = (frm + to) / 2
    return {'plant': tr([(frm, a, 'lin'), (to, b, 'io')], t), 'lift': tr([(frm, 0.0, 'lin'), (mid, lift, 'out'), (to, 0.0, 'in')], t),
            'pitch': tr([(frm, 0.0, 'lin'), (mid, toes, 'out'), (to, 0.0, 'in')], t)}


def rot(out, tracks, t):
    for b, keys in tracks.items():
        out[b] = {'rotation': tr(keys, t)}


def shift_in(t):
    """0 the match pose. 0 to 0.14 a breath in: the chest lifts four degrees and the head turns. Then he grows while he lowers himself: the right hind
    foot steps back, then the left; the right forepaw is set down at 0.60 and the left at 0.70. 0.60 to 0.78 the weight comes down through the shoulders,
    the mane a beat behind it, the tail last. One slow rebound into idle."""
    hp = tr([(0, M_HP, 'lin'), (0.14, -75.0, 'io'), (0.60, 2.0, 'in15'), (0.78, 9.0, 'out'), (0.92, 1.2, 'io'), (1, 0.0, 'io')], t)
    pelvis = tr([(0, M_PELVIS, 'lin'), (0.14, (7.9, 3.3), 'io'), (0.60, (28.0, 14.0), 'in15'), (0.78, (25.6, 14.6), 'out'), (0.92, (27.85, 14.0), 'io'), (1, PELVIS, 'io')], t)
    s = tr([(0, M_S, 'lin'), (0.55, 1.0, 'io'), (1, 1.0, 'lin')], t)
    st = {'s': s, 'hp': hp, 'hip': body_from_pelvis(pelvis, hp, s)}
    open_claws = tr([(0, 0.0, 'lin'), (0.16, 1.0, 'io'), (0.34, 0.0, 'io')], t)
    st['limbs'] = {
        'fr': {'at': forepaw(st, 'fr', (M_HOOK['fr'][0] + 1.2 * open_claws, M_HOOK['fr'][1] - 1.6 * open_claws), tr([(0.18, 0.0, 'lin'), (0.60, 1.0, 'in')], t)),
               'pitch': tr([(0, 44.0, 'lin'), (0.20, 14.0, 'io'), (0.46, -12.0, 'io'), (0.60, 0.0, 'in')], t)},
        'fl': {'at': forepaw(st, 'fl', (M_HOOK['fl'][0] + 0.8 * open_claws, M_HOOK['fl'][1] - 1.2 * open_claws), tr([(0.26, 0.0, 'lin'), (0.70, 1.0, 'in')], t)),
               'pitch': tr([(0, 52.0, 'lin'), (0.26, 20.0, 'io'), (0.56, -12.0, 'io'), (0.70, 0.0, 'in')], t)},
        'hr': step(t, M_HIND['hr'], 0.0, 0.14, 0.36, 2.2, 9.0),
        'hl': step(t, M_HIND['hl'], 0.0, 0.36, 0.56, 2.6, 10.0),
    }
    out = RIG.solve(st)
    rot(out, {
        # the head stays up and level while the body comes down under it, and dips only after the second paw
        'neck': [(0, M['neck'], 'lin'), (0.16, (31.0, -4.0, 0.0), 'io'), (0.40, (26.0, -2.0, 0.0), 'io'), (0.62, (-9.0, 1.0, 0.0), 'io'), (0.82, (11.0, 4.0, 0.0), 'io'),
                 (0.94, (-3.0, 3.5, 0.0), 'io'), (1, I['neck'], 'io')],
        'head': [(0, M['head'], 'lin'), (0.18, (-22.0, -7.0, -3.0), 'io'), (0.44, (-18.0, -2.0, 0.0), 'io'), (0.64, (-7.0, 2.0, 0.0), 'io'), (0.84, (9.0, 7.0, 2.0), 'io'),
                 (0.95, (0.5, 6.0, 1.5), 'io'), (1, I['head'], 'io')],
        'jaw': [(0, M['jaw'], 'lin'), (0.16, (24.0, 0.0, 0.0), 'io'), (0.56, (9.0, 0.0, 0.0), 'io'), (0.72, (2.0, 0.0, 0.0), 'io'), (0.84, (9.0, 0.0, 0.0), 'out'),
                (1, I['jaw'], 'io')],
        'ear_l': [(0, M['ear_l'], 'lin'), (0.60, (-10.0, 0.0, 0.0), 'io'), (0.74, (-24.0, 0.0, 0.0), 'out'), (0.92, (3.0, 0.0, 2.0), 'io'), (1, I['ear_l'], 'io')],
        'ear_r': [(0, M['ear_r'], 'lin'), (0.56, (-8.0, 0.0, -2.0), 'io'), (0.68, (-22.0, 0.0, 0.0), 'out'), (0.88, (2.0, 0.0, 2.0), 'io'), (1, I['ear_r'], 'io')],
        # the tail is held up off the ground while the rump is tucked under, trails high as the hips rise, and comes down last
        'tail1': [(0, M['tail1'], 'lin'), (0.16, (102.0, -3.0, 0.0), 'io'), (0.42, (64.0, 3.0, 0.0), 'io'), (0.62, (34.0, 4.0, 0.0), 'io'), (0.86, (-7.0, -2.0, 0.0), 'io'),
                  (0.95, (2.5, 0.0, 0.0), 'io'), (1, I['tail1'], 'io')],
        'tail2': [(0, M['tail2'], 'lin'), (0.24, (22.0, -4.0, 0.0), 'io'), (0.58, (16.0, 6.0, 0.0), 'io'), (0.78, (20.0, 4.0, 0.0), 'io'), (0.92, (-8.0, 7.0, 0.0), 'io'),
                  (1, I['tail2'], 'io')],
        'tail3': [(0, M['tail3'], 'lin'), (0.30, (14.0, -6.0, 0.0), 'io'), (0.64, (12.0, 8.0, 0.0), 'io'), (0.84, (18.0, 8.0, 0.0), 'io'), (0.96, (-5.0, 12.0, 0.0), 'io'),
                  (1, I['tail3'], 'io')],
        # the mane lies back while he is upright and is thrown forward by each paw, the front of it first
        'mane_a': [(0, M['mane_a'], 'lin'), (0.16, (4.0, 0.0, 0.0), 'io'), (0.60, (-6.0, 0.0, 0.0), 'io'), (0.72, (24.0, 0.0, 3.0), 'out'), (0.90, (-4.0, 0.0, 0.0), 'io'),
                   (1, I['mane_a'], 'io')],
        'mane_b': [(0, M['mane_b'], 'lin'), (0.20, (0.0, 0.0, 0.0), 'io'), (0.64, (-5.0, 0.0, 0.0), 'io'), (0.78, (19.0, 0.0, 2.0), 'out'), (0.94, (-3.0, 0.0, 0.0), 'io'),
                   (1, I['mane_b'], 'io')],
        'mane_chest': [(0, M['mane_chest'], 'lin'), (0.40, (-16.0, 0.0, 0.0), 'io'), (0.62, (-12.0, 0.0, 0.0), 'io'), (0.74, (15.0, 0.0, 0.0), 'out'),
                       (0.90, (-4.0, 0.0, 0.0), 'io'), (1, I['mane_chest'], 'io')],
    }, t)
    return out


def shift_out(t):
    """Not shift_in backwards. 0 to 0.26 he gathers: the rump goes back and down and the chest lowers over the forepaws. He pushes off the left forepaw,
    then the right, and comes upright by 0.70 while he shrinks and walks his hind feet forward under himself. The head tucks last. The match pose."""
    # one long ease up from 0.26 to 0.74: with a fast start at 0.40 he stood 11 px taller in two ticks, which is Klaus's snap, not Peter
    hp = tr([(0, 0.0, 'lin'), (0.26, 6.0, 'io'), (0.74, -76.0, 'io'), (1, M_HP, 'io')], t)
    pelvis = tr([(0, PELVIS, 'lin'), (0.26, (24.0, 16.2), 'io'), (0.74, (9.6, 6.0), 'io'), (1, M_PELVIS, 'io')], t)
    s = tr([(0, 1.0, 'lin'), (0.26, 1.0, 'lin'), (0.82, 0.57, 'io'), (1, M_S, 'io')], t)
    st = {'s': s, 'hp': hp, 'hip': body_from_pelvis(pelvis, hp, s)}
    st['limbs'] = {
        'fl': {'at': forepaw(st, 'fl', M_HOOK['fl'], tr([(0.28, 1.0, 'lin'), (0.74, 0.0, 'out')], t)),
               'pitch': tr([(0.28, 0.0, 'lin'), (0.33, 3.0, 'in'), (0.46, 36.0, 'out'), (0.74, 30.0, 'io'), (1, 52.0, 'io')], t)},
        'fr': {'at': forepaw(st, 'fr', M_HOOK['fr'], tr([(0.36, 1.0, 'lin'), (0.84, 0.0, 'out3')], t)),
               'pitch': tr([(0.36, 0.0, 'lin'), (0.41, 3.0, 'in'), (0.54, 34.0, 'out'), (0.84, 26.0, 'io'), (1, 44.0, 'io')], t)},
        'hl': step(t, 0.0, M_HIND['hl'], 0.50, 0.72, 2.6, 10.0),
        'hr': step(t, 0.0, M_HIND['hr'], 0.72, 0.92, 2.2, 9.0),
    }
    out = RIG.solve(st)
    rot(out, {
        'neck': [(0, I['neck'], 'lin'), (0.26, (13.0, 1.0, 0.0), 'io'), (0.42, (2.0, 0.0, 0.0), 'io'), (0.72, (30.0, -3.0, 0.0), 'io'), (0.90, (44.0, 1.0, 0.0), 'io'),
                 (1, M['neck'], 'io')],
        'head': [(0, I['head'], 'lin'), (0.26, (8.0, 3.0, 0.0), 'io'), (0.46, (-8.0, 0.0, 0.0), 'io'), (0.76, (10.0, -4.0, -2.0), 'io'), (0.92, (-18.0, 3.0, 2.0), 'io'),
                 (1, M['head'], 'io')],
        'jaw': [(0, I['jaw'], 'lin'), (0.30, (0.0, 0.0, 0.0), 'io'), (0.60, (20.0, 0.0, 0.0), 'io'), (0.84, (8.0, 0.0, 0.0), 'io'), (1, M['jaw'], 'io')],
        'ear_l': [(0, I['ear_l'], 'lin'), (0.26, (-16.0, 0.0, 0.0), 'io'), (0.66, (-22.0, 0.0, 0.0), 'io'), (1, M['ear_l'], 'io')],
        'ear_r': [(0, I['ear_r'], 'lin'), (0.30, (-14.0, 0.0, 0.0), 'io'), (0.72, (-20.0, 0.0, -2.0), 'io'), (1, M['ear_r'], 'io')],
        'tail1': [(0, I['tail1'], 'lin'), (0.26, (-10.0, 0.0, 0.0), 'io'), (0.42, (56.0, -2.0, 0.0), 'io'), (0.56, (96.0, -4.0, 0.0), 'io'), (0.72, (108.0, -5.0, 0.0), 'io'),
                  (1, M['tail1'], 'io')],
        'tail2': [(0, I['tail2'], 'lin'), (0.30, (-7.0, 6.0, 0.0), 'io'), (0.56, (12.0, 0.0, 0.0), 'io'), (0.80, (22.0, -6.0, 0.0), 'io'), (1, M['tail2'], 'io')],
        'tail3': [(0, I['tail3'], 'lin'), (0.36, (-5.0, 9.0, 0.0), 'io'), (0.62, (14.0, 0.0, 0.0), 'io'), (0.86, (16.0, -8.0, 0.0), 'io'), (1, M['tail3'], 'io')],
        'mane_a': [(0, I['mane_a'], 'lin'), (0.26, (10.0, 0.0, 0.0), 'io'), (0.52, (-8.0, 0.0, 0.0), 'io'), (0.80, (20.0, 0.0, 0.0), 'io'), (1, M['mane_a'], 'io')],
        'mane_b': [(0, I['mane_b'], 'lin'), (0.30, (8.0, 0.0, 0.0), 'io'), (0.58, (-6.0, 0.0, 0.0), 'io'), (0.86, (13.0, 0.0, 0.0), 'io'), (1, M['mane_b'], 'io')],
        'mane_chest': [(0, I['mane_chest'], 'lin'), (0.26, (9.0, 0.0, 0.0), 'io'), (0.6, (-16.0, 0.0, 0.0), 'io'), (1, M['mane_chest'], 'io')],
    }, t)
    return out


def old_roar():
    path = 'src/main/resources/assets/supernaturalbloodlines/animations/entity/%s.animation.json' % MODEL
    text = subprocess.run(['git', 'show', 'origin/main:' + path], capture_output=True, check=True).stdout.decode('utf-8')
    clip = json.loads(text)['animations']['animation.%s.roar' % MODEL]
    assert abs(clip['animation_length'] - 2.6) < 1e-6, clip['animation_length']
    return clip


def roar():
    """peter.py's roar (2.6 s) at 2.2 s: the dip and the rear a little quicker, the shudder at its old beat but seven beats long instead of ten, the
    release as long as it was. It starts and ends on idle's first frame; the two tail bones it never keyed are held there, so they do not jump."""
    def tmap(t):
        if t <= 0.35:
            return t * 0.30 / 0.35
        if t <= 0.8:
            return 0.30 + (t - 0.35) * 0.42 / 0.45
        if t < 1.9:
            return 0.72 + (t - 0.8) if t - 0.8 <= 0.78 else None        # the shudder keys past the shorter hold are dropped
        return 1.52 if t < 2.6 else 2.2
    clip = mr.pin_ends(mr.retimed(old_roar(), tmap, 2.2), I, 2.2)
    for b in ('tail2', 'tail3'):
        assert b not in clip['bones'], b
        clip['bones'][b] = {'rotation': {'0.0': list(I[b]), '2.2': list(I[b])}}
    return clip


def build():
    clips = {'shift_in': mr.sampled(shift_in, LIMB_BONES), 'roar': roar(), 'shift_out': mr.sampled(shift_out, LIMB_BONES)}
    first, last, end = shift_in(0.0), shift_out(1.0), shift_in(1.0)
    for b in first:
        for c in first[b]:
            assert all(abs(x - y) < 1e-6 for x, y in zip(first[b][c], last[b][c])), ('shift_out does not end on the match pose', b, c, first[b][c], last[b][c])
    for b, chans in end.items():
        for c, v in chans.items():
            w = I.get(b, (0.0, 0.0, 0.0)) if c == 'rotation' else (1.0, 1.0, 1.0) if c == 'scale' else (0.0, 0.0, 0.0)
            assert all(abs(x - y) < 1e-3 for x, y in zip(v, w)), ('shift_in does not end on idle', b, c, v, w)
    assert RIG.reach < 0.6, 'a limb target is beyond reach:\n' + mr.reach_report(RIG, {'shift_in': shift_in, 'shift_out': shift_out})
    return clips


if __name__ == '__main__':
    mr.write(sys.argv[1], MODEL, build())
    print('wrote %s; the furthest a limb target lay beyond reach: %.2f px' % (sys.argv[1], max(RIG.reach, 0.0)))
