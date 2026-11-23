"""Klaus's morph clips as a pose sheet: shift_in (1.0 s), the howl re-timed to 2.2 s and shift_out (1.0 s).

    python art/creatures/morph/tools/klaus_keys.py <out.json>

It does NOT write the game file. It writes an animation file that is imported into art/blockbench/creatures/klaus_hybrid.bbmodel (the source of these
clips), looked at there, exported from there and merged with merge_clips.py. Why a script and not hand keys: the legs are two-bone IK against a body that
pitches 60 degrees and a root that swells from 0.75 to 1, and a planted paw must not skate, so every leg key is solved, one per tick.

Space: the animation file's (facing -Z, +X rotation tips the nose down, y up, z back), in model px. "World" below is that space after the root's scale.
"""
import json
import math
import sys

IDENT = 'klaus_hybrid'
B = (19.0, 0.0)                                                     # the body bone's pivot (y, z)
FORE = [(19.5, -8.5), (11.544, -6.516), (1.98, -7.353)]             # shoulder, elbow, wrist
HIND = [(20.0, 11.0), (11.778, 7.339), (1.456, 12.009)]             # hip, knee, ankle (shin and metatarsus move as one)
PIVOT_Z = 11.0                                                      # the root's pivot is on the ground under the hips
STEP = 0.05                                                         # one key per game tick
# The first frame of idle: where shift_in must end and where the howl starts and ends.
IDLE0 = {'neck': (0.882, 2.939, 0.0), 'head': (2.0, 7.0, 1.5), 'jaw': (1.5, 0.0, 0.0), 'ear_l': (0.0, 0.0, 1.763), 'ear_r': (-1.763, 0.0, 1.763),
         'tail1': (0.0, 0.0, 0.0), 'tail2': (1.214, 6.472, 0.0), 'tail3': (1.427, 9.511, 0.0), 'chest_ruff': (1.176, 0.0, 0.0)}
IDLE0_BRISTLE = 0.06 / 0.45                                         # idle's first hackles scale is (1, 1.06, 1)
REACH = [0.0]


def R(v, a):
    """Rotate a (y, z) offset by an X rotation: +a tips a nose down and swings a hanging limb back."""
    a = math.radians(a)
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c + v[1] * s, v[1] * c - v[0] * s)


def ik(name, dy, dz, bp, by, bz, paw=0.0):
    """Two-bone IK in the y/z plane (klaus.py's): the rotations that put the paw joint of leg fl/fr/hl/hr (dy up, dz back) from its rest spot under a
    body pitched bp and moved (by, bz). `paw` is the pitch of the paw itself against the ground (0 is flat, + lets the toes hang)."""
    fore = name[0] == 'f'
    ch = FORE if fore else HIND
    v1 = (ch[1][0] - ch[0][0], ch[1][1] - ch[0][1])
    v2 = (ch[2][0] - ch[1][0], ch[2][1] - ch[1][1])
    top = R((ch[0][0] - B[0], ch[0][1] - B[1]), bp)
    T = R((ch[2][0] - B[0] + dy - by - top[0], ch[2][1] - B[1] + dz - bz - top[1]), -bp)
    l1, l2, d = math.hypot(*v1), math.hypot(*v2), math.hypot(*T)
    REACH[0] = max(REACH[0], d - l1 - l2)
    th0 = (math.atan2(v2[1], v2[0]) - math.atan2(v1[1], v1[0]) + math.pi) % (2 * math.pi) - math.pi
    th = math.copysign(math.acos(max(-1.0, min(1.0, (d * d - l1 * l1 - l2 * l2) / (2 * l1 * l2)))), th0)
    a2 = math.degrees(th0 - th)
    c = R(v2, a2)
    a1 = (math.degrees(math.atan2(v1[1] + c[1], v1[0] + c[0]) - math.atan2(T[1], T[0])) + 180) % 360 - 180
    bones = ('foreleg', 'foreleg2', 'paw') if fore else ('hindleg', 'shin', 'foot')
    return ['%s_%s' % (b, name[1]) for b in bones], [a1, a2, -(bp + a1 + a2) + paw]


C1 = 1.70158
EASE = {
    'lin': lambda u: u,
    'in': lambda u: u * u,                                           # a fall: no slowing before the ground
    'in15': lambda u: u ** 1.5,                                      # the fall and the hind legs' push: moving early, fastest at the ground
    'in3': lambda u: u ** 3,
    'out': lambda u: 1 - (1 - u) ** 2,
    'out3': lambda u: 1 - (1 - u) ** 3,                              # an impact soaked up
    'io': lambda u: 0.5 - 0.5 * math.cos(math.pi * u),
    'back': lambda u: 1 + (C1 + 1) * (u - 1) ** 3 + C1 * (u - 1) ** 2,   # a snap that overshoots
}


def tr(keys, t):
    """A hand-keyed curve: keys are (time, value, ease into this key); value is a number or a tuple."""
    if t <= keys[0][0]:
        return keys[0][1]
    for (t0, v0, _), (t1, v1, e) in zip(keys, keys[1:]):
        if t <= t1:
            u = EASE[e]((t - t0) / (t1 - t0))
            if isinstance(v0, tuple):
                return tuple(a + (b - a) * u for a, b in zip(v0, v1))
            return v0 + (v1 - v0) * u
    return keys[-1][1]


# ------------------------------------------------------------------ the match pose (shift_in's first frame, shift_out's last)
# The wolf reared on folded haunches inside the man's outline: root 0.75, spine 60 degrees up, the nape the highest point, the muzzle thrown back.
# It stands where the man stood: the root is 4.6 px forward (0.3 blocks is the most the contract allows) and the stance carries the other 2.9, so the
# hips are over z = 3 and both hind feet are well forward of their rest spots. They step back to them, one after the other, while the body falls.
M_SCALE, M_PITCH, M_HIP, M_ROOT_Z = 0.75, -60.0, (6.0, 3.0), -4.6
M_HOOK = {'fl': (-7.6, -4.2), 'fr': (-4.6, -7.2)}                    # paw joint from the shoulder (down, forward) in world px: his right is the higher claw
M_HIND = {'hl': -12.5, 'hr': -7.5}                                   # world px from the rest spots; the left is a short step ahead, as the man's left leg is
M = {'neck': (57.0, -2.0, 0.0), 'head': (-26.0, -4.0, -3.0), 'jaw': (20.0, 0.0, 0.0), 'ear_l': (-30.0, 0.0, 0.0), 'ear_r': (-26.0, 0.0, -4.0),
     'tail1': (76.0, 6.0, 0.0), 'tail2': (34.0, 8.0, 0.0), 'tail3': (16.0, 10.0, 0.0), 'chest_ruff': (-14.0, 0.0, 0.0)}
M_RUFF = 0.5                                                         # the neck ruff half flat: fully bristled it made the first frame 11.64 px wide
# yaw, roll. None: a yaw on the body, the neck and the head adds up and pushed the muzzle 8.7 px off centre (0.96 blocks wide), and any roll
# sinks one hind foot. The claws and the hind step carry the asymmetry instead.
M_BODY_TWIST = (0.0, 0.0)


def to_model(s, rz, wz):
    """A world z as the model has it under a root scaled s about its pivot and moved rz."""
    return PIVOT_Z + (wz - rz - PIVOT_Z) / s


def shoulder_world(s, bp, hip, rz=0.0):
    """Where the shoulder is, in world px, for a root scale, a body pitch, a hip (world) and a root offset."""
    hm = (hip[0] / s, to_model(s, rz, hip[1]))
    d = R((FORE[0][0] - HIND[0][0], FORE[0][1] - HIND[0][1]), bp)
    return (s * (hm[0] + d[0]), PIVOT_Z + s * (hm[1] + d[1] - PIVOT_Z) + rz)


def solve(st):
    """One sampled state -> {bone: {channel: vector}} for root, body and the twelve leg bones."""
    s, bp, hip, rz = st['s'], st['bp'], st['hip'], st.get('root_z', 0.0)
    hm = (hip[0] / s, to_model(s, rz, hip[1]))
    top = R((HIND[0][0] - B[0], HIND[0][1] - B[1]), bp)
    by, bz = hm[0] - B[0] - top[0], hm[1] - B[1] - top[1]
    out = {'root': {'scale': (s, s, s), 'position': (0.0, 0.0, rz)}, 'body': {'rotation': (bp, st.get('yaw', 0.0), st.get('roll', 0.0)), 'position': (0.0, by, bz)}}
    sh = shoulder_world(s, bp, hip, rz)
    for name in ('fl', 'fr', 'hl', 'hr'):
        ch = FORE if name[0] == 'f' else HIND
        leg = st['legs'][name]
        if 'hook' in leg:                                            # a forepaw between the chest (u = 0) and its spot on the ground (u = 1)
            u, hook = leg['u'], leg['hook']
            wy = (sh[0] + hook[0]) * (1 - u) + ch[2][0] * u
            wz = (sh[1] + hook[1]) * (1 - u) + ch[2][1] * u
            my, mz = wy / s, to_model(s, rz, wz)
        else:                                                        # a planted or stepping paw: lift is world px above the ground, z a world offset
            my = ch[2][0] + leg.get('lift', 0.0) / s
            mz = to_model(s, rz, ch[2][1] + leg.get('z', 0.0))
        bones, rs = ik(name, my - ch[2][0], mz - ch[2][1], bp, by, bz, leg.get('paw', 0.0))
        for b, r in zip(bones, rs):
            out[b] = {'rotation': (r, 0.0, 0.0)}
    return out


def bristle(k):
    """Hackles and ruff stand up by scaling from their roots (klaus.py's)."""
    return {'hackles': {'scale': (1.0, 1 + 0.45 * k, 1.0)}, 'ruff': {'scale': (1 + 0.14 * k, 1 + 0.1 * k, 1.0)}}


# ------------------------------------------------------------------ shift_in: the wolf arrives
def shift_in(t):
    """0 the match pose; a two-degree rock back and a snap of the head in the first two ticks; then it grows out of the man's size while it falls onto its forepaws, the
    left at 0.60 and the right a tick later; 0.6 to 0.72 the chest soaks the landing up, the rump a beat after it; then one small rebound into idle."""
    st = {
        # the root grows while the body pitches forward, and the hind legs push late: grown upright first, it stood 3.5 blocks tall for three ticks
        's': tr([(0, M_SCALE, 'lin'), (0.18, 0.84, 'io'), (0.50, 1.0, 'io'), (1, 1.0, 'lin')], t),
        # the dip before the drop is a small rock back onto the haunches, not a rear-up (with the head thrown back it held 2.8 blocks for seven ticks)
        # and not a sink: the rump already rests on the ground in the match pose, and 0.6 px lower it went through it
        # (the fall starts at 0.06 and is 20 degrees along by 0.30: on a square from 0.14 it sat on its haunches until 0.3 and only grew, and on a
        # nearly straight u^1.2 from 0.10 it turned at one speed all the way down)
        'bp': tr([(0, M_PITCH, 'lin'), (0.06, -62.0, 'io'), (0.60, 1.0, 'in15'), (0.68, 8.5, 'out3'), (0.80, 2.0, 'io'), (0.90, 2.8, 'io'), (1, 0.0, 'io')], t),
        'root_z': tr([(0, M_ROOT_Z, 'lin'), (0.50, 0.0, 'io')], t),
        'hip': tr([(0, M_HIP, 'lin'), (0.12, (6.0, 3.4), 'out'), (0.60, (20.2, 11.0), 'in15'), (0.74, (18.7, 11.4), 'out'), (0.88, (20.15, 11.0), 'io'),
                   (1, (20.0, 11.0), 'io')], t),
        # ticks 20 to 24 were close to a held pose: a shudder through the shoulders, the hackles jumping, and the left hind foot already stepping
        'yaw': tr([(0, M_BODY_TWIST[0], 'lin'), (0.05, 4.0, 'out'), (0.12, -3.0, 'io'), (0.22, 1.5, 'io'), (0.50, 0.0, 'io')], t),
        'roll': 0.0,
    }
    grow = st['s'] / M_SCALE
    flare = tr([(0, 0.0, 'lin'), (0.16, 1.0, 'out'), (0.3, 0.0, 'io')], t)        # the claws go out as it rears
    st['legs'] = {
        'fl': {'u': tr([(0.16, 0.0, 'lin'), (0.60, 1.0, 'in')], t), 'hook': (M_HOOK['fl'][0] * grow + 1.5 * flare, M_HOOK['fl'][1] * grow - 3.0 * flare),
               'paw': tr([(0, 52.0, 'lin'), (0.16, 18.0, 'out'), (0.45, -14.0, 'io'), (0.60, 0.0, 'in')], t)},
        'fr': {'u': tr([(0.20, 0.0, 'lin'), (0.65, 1.0, 'in')], t), 'hook': (M_HOOK['fr'][0] * grow + 2.5 * flare, M_HOOK['fr'][1] * grow - 4.0 * flare),
               'paw': tr([(0, 40.0, 'lin'), (0.18, 6.0, 'out'), (0.5, -16.0, 'io'), (0.65, 0.0, 'in')], t)},
        'hl': {'z': tr([(0.10, M_HIND['hl'], 'lin'), (0.32, 0.0, 'io')], t), 'lift': tr([(0.10, 0.0, 'lin'), (0.20, 2.4, 'out'), (0.32, 0.0, 'in')], t),
               'paw': tr([(0.10, 0.0, 'lin'), (0.20, 11.0, 'out'), (0.32, 0.0, 'in')], t)},
        'hr': {'z': tr([(0.32, M_HIND['hr'], 'lin'), (0.50, 0.0, 'io')], t), 'lift': tr([(0.32, 0.0, 'lin'), (0.40, 1.8, 'out'), (0.50, 0.0, 'in')], t),
               'paw': tr([(0.32, 0.0, 'lin'), (0.40, 8.0, 'out'), (0.50, 0.0, 'in')], t)},
    }
    out = solve(st)
    I = IDLE0
    rot = {
        # the neck stays curled over the shoulders while it rears, holds the head up through the fall and whips down two ticks after the chest
        # (Klaus's snap: in the first two ticks the head whips across to the other side, the neck half as far)
        'neck': [(0, M['neck'], 'lin'), (0.12, (50.0, 4.0, 0.0), 'out'), (0.40, (30.0, -2.0, 0.0), 'io'), (0.60, (-7.0, 0.0, 0.0), 'io'),
                 (0.72, (17.0, 4.5, 0.0), 'out'), (0.86, (-3.5, 4.0, 0.0), 'io'), (1, I['neck'], 'io')],
        'head': [(0, M['head'], 'lin'), (0.10, (-34.0, 9.0, 4.0), 'out'), (0.42, (-20.0, -3.0, -2.0), 'io'), (0.60, (-9.0, 1.0, 0.0), 'io'),
                 (0.74, (11.0, 10.0, 4.0), 'out'), (0.88, (-2.5, 8.0, 2.0), 'io'), (1, I['head'], 'io')],
        'jaw': [(0, M['jaw'], 'lin'), (0.15, (33.0, 0.0, 0.0), 'out'), (0.52, (24.0, 0.0, 0.0), 'io'), (0.63, (2.0, 0.0, 0.0), 'in'),
                (0.74, (11.0, 0.0, 0.0), 'out'), (0.88, (3.5, 0.0, 0.0), 'io'), (1, I['jaw'], 'io')],
        'ear_l': [(0, M['ear_l'], 'lin'), (0.55, (-26.0, 0.0, 0.0), 'io'), (0.66, (-40.0, 0.0, 0.0), 'out'), (0.82, (-8.0, 0.0, 1.0), 'io'),
                  (0.91, (4.0, 0.0, 2.0), 'io'), (1, I['ear_l'], 'io')],
        'ear_r': [(0, M['ear_r'], 'lin'), (0.58, (-24.0, 0.0, -2.0), 'io'), (0.70, (-37.0, 0.0, 0.0), 'out'), (0.87, (-9.0, 0.0, 1.0), 'io'),
                  (0.95, (1.5, 0.0, 2.0), 'io'), (1, I['ear_r'], 'io')],
        # the tail unwinds and streams up behind the falling body, then comes down a beat after the paws and overshoots low
        # (it is lifted hard while the rump is tipped under: lowered there, its tip went 5.7 px through the ground)
        # and it is keyed at 0.12, where the hips are lowest: lifted only at the keys either side, it dipped half a px under between them)
        # (what touches first is the bottom corner at the base of tail2, 3 px under its pivot, so it is tail1 that lifts it: 0.12 px a degree, measured)
        'tail1': [(0, M['tail1'], 'lin'), (0.12, (92.0, 5.0, 0.0), 'out'), (0.20, (92.0, 4.0, 0.0), 'io'), (0.42, (58.0, -4.0, 0.0), 'io'), (0.56, (40.0, -5.0, 0.0), 'io'),
                  (0.66, (33.0, -3.0, 0.0), 'io'),
                  (0.80, (-9.0, 2.0, 0.0), 'io'), (0.91, (3.5, 1.0, 0.0), 'io'), (1, I['tail1'], 'io')],
        'tail2': [(0, M['tail2'], 'lin'), (0.12, (40.0, 7.0, 0.0), 'out'), (0.24, (34.0, 6.0, 0.0), 'io'), (0.54, (20.0, -7.0, 0.0), 'io'), (0.71, (24.0, -5.0, 0.0), 'io'),
                  (0.85, (-11.0, 3.0, 0.0), 'io'), (0.95, (3.0, 6.0, 0.0), 'io'), (1, I['tail2'], 'io')],
        'tail3': [(0, M['tail3'], 'lin'), (0.28, (14.0, 8.0, 0.0), 'io'), (0.58, (14.0, -9.0, 0.0), 'io'), (0.76, (20.0, -7.0, 0.0), 'io'),
                  (0.90, (-10.0, 5.0, 0.0), 'io'), (1, I['tail3'], 'io')],
        'chest_ruff': [(0, M['chest_ruff'], 'lin'), (0.40, (-21.0, 0.0, 0.0), 'io'), (0.60, (-17.0, 0.0, 0.0), 'io'), (0.69, (13.0, 0.0, 0.0), 'out'),
                       (0.82, (-5.0, 0.0, 0.0), 'io'), (0.92, (3.0, 0.0, 0.0), 'io'), (1, I['chest_ruff'], 'io')],
    }
    for b, keys in rot.items():
        out[b] = {'rotation': tr(keys, t)}
    out.update(bristle(tr([(0, 1.0, 'lin'), (0.07, 1.45, 'out'), (0.18, 1.1, 'io'), (0.56, 0.85, 'io'), (0.66, 1.3, 'out3'), (0.86, 0.45, 'io'), (1, IDLE0_BRISTLE, 'io')], t)))
    out['ruff']['scale'] = bristle(tr([(0, M_RUFF, 'lin'), (0.56, 0.6, 'io'), (0.66, 1.25, 'out3'), (1, 0.0, 'io')], t))['ruff']['scale']
    return out


# ------------------------------------------------------------------ shift_out: the wolf stands up into the man (ready art, wired to nothing)
def shift_out(t):
    """Not shift_in backwards. 0 to 0.22 it bows: the rump goes back over the hind feet and the chest sinks. 0.22 to 0.34 it shoves off the forepaws,
    the right first. Up to 0.66 the spine comes upright past the match and the head tucks to the chest. Then it shrinks and sinks into the match pose."""
    st = {
        # it shrinks while it rises and the haunches fold early: shrinking only from 0.5, it stood 3 blocks tall at 0.5
        's': tr([(0, 1.0, 'lin'), (0.28, 1.0, 'lin'), (0.80, 0.82, 'io'), (1, M_SCALE, 'io')], t),
        'bp': tr([(0, 0.0, 'lin'), (0.22, 5.0, 'io'), (0.34, -17.0, 'in'), (0.64, -63.0, 'out'), (1, M_PITCH, 'io')], t),
        # (the hips are down to 11 by 0.40: at 13.5 there the ear tips stood 41.8 px high, 3.7 px more than shift_in at the same pitch)
        'hip': tr([(0, (20.0, 11.0), 'lin'), (0.22, (17.0, 12.8), 'io'), (0.40, (11.0, 11.8), 'io'), (0.66, (8.0, 10.7), 'io'), (1, M_HIP, 'io')], t),
        # the last third was a held pose that shrank: now it walks its hind feet under itself to the man's spot, the right then the left, and shudders
        'root_z': tr([(0.50, 0.0, 'lin'), (1, M_ROOT_Z, 'io')], t),
        'yaw': tr([(0.40, 0.0, 'lin'), (0.70, 2.5, 'io'), (0.84, -3.5, 'io'), (0.93, 2.0, 'io'), (1, M_BODY_TWIST[0], 'io')], t),
        'roll': 0.0,
    }
    grow = st['s'] / M_SCALE
    st['legs'] = {
        'fr': {'u': tr([(0.25, 1.0, 'lin'), (0.72, 0.0, 'out')], t), 'hook': (M_HOOK['fr'][0] * grow, M_HOOK['fr'][1] * grow),
               # the toes hang only once the paw is clear of the ground: curled from the first tick, they dug half a px in
               'paw': tr([(0.25, 0.0, 'lin'), (0.30, 3.0, 'in'), (0.42, 34.0, 'out'), (0.72, 20.0, 'io'), (1, 40.0, 'io')], t)},
        'fl': {'u': tr([(0.30, 1.0, 'lin'), (0.80, 0.0, 'out3')], t), 'hook': (M_HOOK['fl'][0] * grow, M_HOOK['fl'][1] * grow),
               'paw': tr([(0.30, 0.0, 'lin'), (0.35, 3.0, 'in'), (0.47, 38.0, 'out'), (0.80, 30.0, 'io'), (1, 52.0, 'io')], t)},
        'hr': {'z': tr([(0.56, 0.0, 'lin'), (0.74, M_HIND['hr'], 'io')], t), 'lift': tr([(0.56, 0.0, 'lin'), (0.64, 1.8, 'out'), (0.74, 0.0, 'in')], t),
               'paw': tr([(0.56, 0.0, 'lin'), (0.64, 8.0, 'out'), (0.74, 0.0, 'in')], t)},
        'hl': {'z': tr([(0.72, 0.0, 'lin'), (0.92, M_HIND['hl'], 'io')], t), 'lift': tr([(0.72, 0.0, 'lin'), (0.81, 2.4, 'out'), (0.92, 0.0, 'in')], t),
               'paw': tr([(0.72, 0.0, 'lin'), (0.81, 11.0, 'out'), (0.92, 0.0, 'in')], t)},
    }
    out = solve(st)
    I = IDLE0
    rot = {
        # the head tucks while the spine is still coming up (by 0.54): tucked only at 0.64, the head stood 42 px high at 0.5
        'neck': [(0, I['neck'], 'lin'), (0.22, (15.0, 1.0, 0.0), 'io'), (0.36, (4.0, 0.0, 0.0), 'io'), (0.54, (66.0, -3.0, 0.0), 'io'), (0.84, (58.0, -6.0, 0.0), 'io'),
                 (1, M['neck'], 'io')],
        'head': [(0, I['head'], 'lin'), (0.22, (9.0, 3.0, 0.0), 'io'), (0.40, (-6.0, 0.0, 0.0), 'io'), (0.58, (24.0, -5.0, -3.0), 'io'), (0.86, (-4.0, -9.0, -6.0), 'io'),
                 (1, M['head'], 'io')],
        'jaw': [(0, I['jaw'], 'lin'), (0.30, (0.0, 0.0, 0.0), 'io'), (0.70, (6.0, 0.0, 0.0), 'io'), (1, M['jaw'], 'io')],
        'ear_l': [(0, I['ear_l'], 'lin'), (0.20, (-18.0, 0.0, 0.0), 'out'), (0.60, (-36.0, 0.0, 0.0), 'io'), (1, M['ear_l'], 'io')],
        'ear_r': [(0, I['ear_r'], 'lin'), (0.26, (-16.0, 0.0, 0.0), 'out'), (0.66, (-33.0, 0.0, -2.0), 'io'), (1, M['ear_r'], 'io')],
        # the tail is lifted from its base as fast as the rump tips under it (tail1 is what keeps it off the ground, as in shift_in)
        'tail1': [(0, I['tail1'], 'lin'), (0.22, (-12.0, 0.0, 0.0), 'io'), (0.40, (54.0, 3.0, 0.0), 'io'), (0.52, (80.0, 5.0, 0.0), 'io'), (0.66, (94.0, 6.0, 0.0), 'io'),
                  (1, M['tail1'], 'io')],
        'tail2': [(0, I['tail2'], 'lin'), (0.28, (-9.0, 4.0, 0.0), 'io'), (0.50, (20.0, 6.0, 0.0), 'io'), (0.70, (36.0, 8.0, 0.0), 'io'), (1, M['tail2'], 'io')],
        'tail3': [(0, I['tail3'], 'lin'), (0.34, (-7.0, 6.0, 0.0), 'io'), (0.56, (10.0, 8.0, 0.0), 'io'), (0.80, (16.0, 10.0, 0.0), 'io'), (1, M['tail3'], 'io')],
        'chest_ruff': [(0, I['chest_ruff'], 'lin'), (0.22, (8.0, 0.0, 0.0), 'io'), (0.5, (-8.0, 0.0, 0.0), 'io'), (1, M['chest_ruff'], 'io')],
    }
    for b, keys in rot.items():
        out[b] = {'rotation': tr(keys, t)}
    out.update(bristle(tr([(0, IDLE0_BRISTLE, 'lin'), (0.22, 0.0, 'io'), (0.60, 1.25, 'out'), (0.82, 0.85, 'io'), (0.92, 1.4, 'out'), (1, 1.0, 'io')], t)))
    out['ruff']['scale'] = bristle(tr([(0, 0.0, 'lin'), (0.30, 0.0, 'lin'), (0.66, 0.9, 'out'), (1, M_RUFF, 'io')], t))['ruff']['scale']
    return out


# ------------------------------------------------------------------ the file
def stamp(t):
    s = ('%.4f' % t).rstrip('0')
    return s + '0' if s.endswith('.') else s


def rnd(v):
    return [round(x, 3) + 0.0 for x in v]


LEGS = {'%s_%s' % (b, s) for b in ('foreleg', 'foreleg2', 'paw', 'hindleg', 'shin', 'foot') for s in 'lr'}


def sampled(fn, length=1.0):
    """A clip sampled once a tick: catmullrom on everything that is spine, neck, head or fur, linear on the solved legs."""
    bones = {}
    n = round(length / STEP)
    for i in range(n + 1):
        t = i * STEP
        for b, chans in fn(t).items():
            for c, v in chans.items():
                bones.setdefault(b, {}).setdefault(c, {})[stamp(t)] = rnd(v) if b in LEGS else {'post': rnd(v), 'lerp_mode': 'catmullrom'}
    return {'animation_length': length, 'bones': bones}


def howl():
    """klaus.py's howl (3.2 s) at 2.2 s: the same dip, rise and release at nearly the pace they had, and the held note cut from 1.5 s (eight trembles)
    to 0.8 s (four, at the old beat). It starts and ends on idle's first frame instead of the model's rest pose, so nothing jumps after shift_in."""
    up, beat, n = 0.85, 0.16, 4
    end, length, dip = up + (n + 1) * beat, 2.2, 0.30
    bones = {}

    def k(b, chan, t, v, e=None):
        bones.setdefault(b, {}).setdefault(chan, {})[stamp(t)] = rnd(v) if e is None else {'vector': rnd(v), 'easing': e}

    def paws(t, bp, by, bz, e=None):
        for name in ('fl', 'fr', 'hl', 'hr'):
            for b, r in zip(*ik(name, 0.0, 0.0, bp, by, bz)):
                k(b, 'rotation', t, (r, 0, 0), e)
        k('body', 'position', t, (0, by, bz), e)
        k('body', 'rotation', t, (bp, 0, 0), e)

    Z, I = (0, 0, 0), IDLE0
    paws(0, 0, 0, 0)
    paws(dip, 3, -0.6, -0.5, 'easeInOutSine')
    paws(up, -5, -0.8, 1.5, 'easeOutCubic')
    paws(end, -5, -0.8, 1.5)
    paws(length, 0, 0, 0, 'easeInOutSine')
    for b, ks in (('neck', ((0, I['neck'], None), (dip, (8, 0, 0), 'easeInOutSine'), (up, (-30, 0, 0), 'easeOutCubic'), (end, (-32, 0, 0), 'easeInOutSine'),
                            (length, I['neck'], 'easeInOutSine'))),
                  ('head', ((0, I['head'], None), (dip, (8, 0, 0), 'easeInOutSine'), (up, (-32, 0, 0), 'easeOutBack'))),
                  ('jaw', ((0, I['jaw'], None), (dip, (-4, 0, 0), 'easeInOutSine'), (up, (15, 0, 0), 'easeOutCubic')))):
        for t, v, e in ks:
            k(b, 'rotation', t, v, e)
    for i in range(1, n + 1):                                        # the held note: a slow tremble in the head and jaw
        t, sg = up + i * beat, (1 if i % 2 else -1)
        k('head', 'rotation', t, (-34 + sg * 1.5, sg * 1.5, 0), 'easeInOutSine')
        k('jaw', 'rotation', t, (17 + sg * 2.5, 0, 0), 'easeInOutSine')
    k('head', 'rotation', length, I['head'], 'easeInOutSine')
    k('jaw', 'rotation', end, (12, 0, 0), 'easeInOutSine')
    k('jaw', 'rotation', length, I['jaw'], 'easeInOutSine')
    for s in 'lr':
        for t, v, e in ((0, I['ear_' + s], None), (up, (-30, 0, 0), 'easeOutCubic'), (end, (-30, 0, 0), None), (length, I['ear_' + s], 'easeInOutSine')):
            k('ear_' + s, 'rotation', t, v, e)
    for t, a, e in ((0, IDLE0_BRISTLE, None), (up, 1, 'easeOutBack'), (end, 0.8, 'easeInOutSine'), (length, IDLE0_BRISTLE, 'easeInOutSine')):
        k('hackles', 'scale', t, (1, 1 + 0.45 * a, 1), e)
    for t, a, e in ((0, 0, None), (up, 1, 'easeOutBack'), (end, 0.8, 'easeInOutSine'), (length, 0, 'easeInOutSine')):
        k('ruff', 'scale', t, (1 + 0.14 * a, 1 + 0.1 * a, 1), e)
    for b, v in (('chest_ruff', -10), ('tail1', 14), ('tail2', 6)):
        for t, val, e in ((0, I[b], None), (up, (v, 0, 0), 'easeOutBack'), (end, (v * 0.8, 0, 0), 'easeInOutSine'), (length, I[b], 'easeInOutSine')):
            k(b, 'rotation', t, val, e)
    return {'animation_length': length, 'bones': bones}


def build():
    N = 'animation.%s.' % IDENT
    clips = {N + 'shift_in': sampled(shift_in), N + 'howl': howl(), N + 'shift_out': sampled(shift_out)}
    # self-check: shift_out ends where shift_in starts, shift_in ends on idle's first frame with the legs and body at rest
    first, last = shift_in(0.0), shift_out(1.0)
    for b in first:
        for c in first[b]:
            assert all(abs(x - y) < 1e-6 for x, y in zip(first[b][c], last[b][c])), ('shift_out does not end on the match pose', b, c)
    end = shift_in(1.0)
    for b, chans in end.items():
        for c, v in chans.items():
            want = IDLE0.get(b) if c == 'rotation' and b in IDLE0 else (1.0, 1.06, 1.0) if b == 'hackles' else (1.0, 1.0, 1.0) if c == 'scale' else (0.0, 0.0, 0.0)
            assert all(abs(x - y) < 1e-3 for x, y in zip(v, want)), ('shift_in does not end on idle', b, c, v, want)
    assert REACH[0] < 0.6, 'a paw target is %.2f px beyond its leg' % REACH[0]
    return {'format_version': '1.8.0', 'animations': clips}


if __name__ == '__main__':
    data = build()
    with open(sys.argv[1], 'w', encoding='utf-8', newline='') as f:
        json.dump(data, f, indent=1)
    print('wrote %s; the furthest a paw target lay beyond its leg: %.2f px' % (sys.argv[1], max(REACH[0], 0.0)))
