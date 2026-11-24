"""Deucalion's morph clips as a pose sheet: shift_in (1.0 s), the roar re-timed to 2.2 s and shift_out (1.0 s).

    python art/creatures/morph/tools/deucalion_keys.py <out.json>      (from the repo root; the old roar is read from origin/main)

Like klaus_keys.py it writes a file to import into the .bbmodel, never the game file. Deucalion's demon wolf walks upright, so his arrival is not a drop
onto four paws: he grows out of the man's size into a lunge, drives his left fist into the ground at 0.60 s (the LAND cue) and rises to his full height.
His rhythm is one heavy hit and a slow rise, where Klaus's is a snap.
"""
import json
import math
import subprocess
import sys

import morph_rig as mr
from klaus_keys import tr

MODEL = 'deucalion_demon_wolf'
ARM_P, ARM_R = [(34.88, -0.47), (25.11, -0.47), (16.65, -0.47)], [-30, -16, -8]
LEG_P, LEG_R = [(20.46, 0.47), (10.97, 0.19), (2.42, 0.37)], [-14, 26, -12]
RIG = mr.Rig('hips', (20.46, 0.0), [('spine', (22.32, 0.47), 15)], {
    'al': mr.Limb(('arm_l', 'forearm_l', 'hand_l'), ARM_P, ARM_R, on='torso'), 'ar': mr.Limb(('arm_r', 'forearm_r', 'hand_r'), ARM_P, ARM_R, on='torso'),
    'll': mr.Limb(('leg_l', 'shin_l', 'foot_l'), LEG_P, LEG_R), 'lr': mr.Limb(('leg_r', 'shin_r', 'foot_r'), LEG_P, LEG_R)})
LIMB_BONES = {b for L in RIG.limbs.values() for b in L.bones}
# The first frame of idle: where shift_in ends and where the roar starts and ends.
I = {'spine': (0.0, 0.0, 0.0), 'neck': (-0.47, 0.0, 0.0), 'head': (1.427, 6.657, 1.141), 'jaw': (0.773, 0.0, 0.0), 'hair': (2.023, 0.0, 0.647),
     'ear_l': (0.0, 0.0, 0.0), 'ear_r': (0.0, 0.0, 0.0), 'fingers_l': (-6.804, 0.0, 0.0), 'fingers_r': (0.804, 0.0, 0.0)}
I_ARM = (0.0, -1.214, 0.0)                                           # arm, forearm, hand keys of idle's first frame
REST = {'s': 1.0, 'hip': (20.46, 0.0), 'hp': 0.0, 'torso': [0.0]}

# The match pose: the demon wolf at 0.60 of its size, standing in the man's hunch with both claws hooked in front of the chest, the left one higher.
M_S, M_HIP, M_HP, M_SPINE = 0.66, (12.45, 0.6), 4.0, 12.0             # his feet are where the man's are; the muzzle and the claws reach forward of them
FIST = (7.8, -15.5)                                                  # where the left wrist is when the fist is on the ground, world px
M_HOOK = {'al': (-4.2, -5.6), 'ar': (-6.4, -4.0)}                    # wrist from the shoulder (down, forward), world px
M_IN = {'al': 14.0, 'ar': -14.0}                                     # the arms turned in over the chest (Z), so the outline is no wider than the man's
M = {'neck': (8.0, -3.0, 0.0), 'head': (-26.0, -5.0, -3.0), 'jaw': (14.0, 0.0, 0.0), 'hair': (-4.0, 0.0, 0.0), 'ear_l': (0.0, 0.0, -8.0), 'ear_r': (0.0, 0.0, 8.0),
     'fingers_l': (-30.0, 0.0, 0.0), 'fingers_r': (-24.0, 0.0, 0.0)}


def mix(a, b, u):
    return tuple(x + (y - x) * u for x, y in zip(a, b))


def arm(st, name, hook, ground, u, v, pitch=0.0, turn_in=0.0, arc=0.0):
    """An arm's target: from a point held off the shoulder (u = 0) to a spot in the world (u = 1), handed over to idle's keys (v = 1)."""
    sh = RIG.joint_world(st, name)
    grow = st['s'] / M_S
    at = mix((sh[0] + hook[0] * grow, sh[1] + hook[1] * grow), ground, u)
    end = RIG.as_target(st, name, *I_ARM)
    at = mix(at, end['at'], v)
    at = (at[0] + arc * math.sin(math.pi * v), at[1])                 # the hand is picked up off the ground on its way to idle, not dragged
    return {'at': at, 'pitch': pitch * (1 - v) + end['pitch'] * v, 'turn': turn_in * (1 - v)}


def rot(out, tracks, t):
    for b, keys in tracks.items():
        out[b] = {'rotation': tr(keys, t)}


def shift_in(t):
    """0 the match pose. 0 to 0.15 the chest opens and the head rolls across. To 0.60 he grows while he lunges: the right foot goes back, the body tips
    forward and the left fist comes down, landing at 0.60. 0.60 to 0.70 the hit goes through him. Then he pushes off the fist, the right foot steps up and
    he stands to his full height, on idle's first frame."""
    st = {
        's': tr([(0, M_S, 'lin'), (0.12, 0.70, 'io'), (0.55, 1.0, 'io'), (1, 1.0, 'lin')], t),
        'hip': tr([(0, M_HIP, 'lin'), (0.15, (13.1, 0.3), 'io'), (0.60, (15.8, 2.2), 'io'), (0.69, (14.3, 1.8), 'out3'), (0.76, (14.9, 1.6), 'io'), (1, (20.46, 0.0), 'io')], t),
        'hp': tr([(0, M_HP, 'lin'), (0.15, 0.0, 'io'), (0.60, 28.0, 'in15'), (0.69, 32.0, 'out3'), (1, 0.0, 'io')], t),
        'torso': [tr([(0, M_SPINE, 'lin'), (0.15, 3.0, 'out'), (0.60, 30.0, 'in15'), (0.69, 37.0, 'out3'), (0.76, 33.0, 'io'), (1, 0.0, 'io')], t)],
        # the chest twists, not the hips (a yaw of the hips drags both planted feet 0.7 px), and holds one angle while the fist is down
        'twist': tr([(0, 0.0, 'lin'), (0.10, -5.0, 'out'), (0.30, 6.0, 'io'), (0.56, 10.0, 'io'), (0.76, 10.0, 'lin'), (1, 0.0, 'io')], t),
    }
    hand_off = tr([(0.74, 0.0, 'lin'), (0.90, 1.0, 'io')], t)              # done by 0.90: any later, the rising chest pulled the arm 0.7 px past straight
    st['limbs'] = {
        'al': arm(st, 'al', mix(M_HOOK['al'], (-2.0, -9.0), tr([(0, 0.0, 'lin'), (0.18, 1.0, 'out')], t)), FIST, tr([(0.20, 0.0, 'lin'), (0.60, 1.0, 'in')], t), hand_off,
                  pitch=tr([(0, 0.0, 'lin'), (0.60, 24.0, 'io')], t), arc=4.5, turn_in=M_IN['al'] * tr([(0, 1.0, 'lin'), (0.22, 0.0, 'io')], t)),
        'ar': arm(st, 'ar', mix(M_HOOK['ar'], (-1.5, 9.5), tr([(0.08, 0.0, 'lin'), (0.56, 1.0, 'io')], t)), (0.0, 0.0), 0.0, tr([(0.66, 0.0, 'lin'), (0.96, 1.0, 'io')], t),
                  turn_in=M_IN['ar'] * tr([(0, 1.0, 'lin'), (0.26, 0.0, 'io')], t) - 18.0 * tr([(0.26, 0.0, 'lin'), (0.56, 1.0, 'io'), (0.90, 0.0, 'io')], t)),
        'll': {'plant': 0.0},
        'lr': {'plant': tr([(0.18, 0.0, 'lin'), (0.38, 9.0, 'io'), (0.74, 9.0, 'lin'), (0.92, 0.0, 'io')], t),
               'lift': tr([(0.18, 0.0, 'lin'), (0.27, 2.6, 'out'), (0.38, 0.3, 'in'), (0.74, 0.3, 'lin'), (0.82, 2.8, 'out'), (0.92, 0.0, 'in')], t),
               'pitch': tr([(0.18, 0.0, 'lin'), (0.27, 10.0, 'out'), (0.38, 0.0, 'in'), (0.74, 0.0, 'lin'), (0.82, 12.0, 'out'), (0.92, 0.0, 'in')], t)},
    }
    out = RIG.solve(st)
    rot(out, {
        # the head rolls across in the first two ticks, stays up while the body tips, and is driven down a beat after the fist
        'neck': [(0, M['neck'], 'lin'), (0.12, (-2.0, 4.0, 0.0), 'out'), (0.60, (-26.0, 0.0, 0.0), 'io'), (0.73, (-6.0, -3.0, 0.0), 'out'), (1, I['neck'], 'io')],
        'head': [(0, M['head'], 'lin'), (0.12, (-34.0, 9.0, 4.0), 'out'), (0.60, (-24.0, -2.0, 0.0), 'io'), (0.75, (10.0, -6.0, -2.0), 'out'), (0.90, (-3.0, 4.0, 1.0), 'io'),
                 (1, I['head'], 'io')],
        'jaw': [(0, M['jaw'], 'lin'), (0.15, (34.0, 0.0, 0.0), 'out'), (0.55, (26.0, 0.0, 0.0), 'io'), (0.63, (3.0, 0.0, 0.0), 'in'), (0.76, (14.0, 0.0, 0.0), 'out'),
                (1, I['jaw'], 'io')],
        'hair': [(0, M['hair'], 'lin'), (0.30, (-12.0, 0.0, 0.0), 'io'), (0.60, (-8.0, 0.0, 0.0), 'io'), (0.71, (24.0, 0.0, 0.0), 'out'), (0.86, (-7.0, 0.0, 0.0), 'io'),
                 (1, I['hair'], 'io')],
        'ear_l': [(0, M['ear_l'], 'lin'), (0.60, (0.0, 0.0, -4.0), 'io'), (0.68, (0.0, 0.0, -14.0), 'out'), (0.88, (0.0, 0.0, 3.0), 'io'), (1, I['ear_l'], 'io')],
        'ear_r': [(0, M['ear_r'], 'lin'), (0.62, (0.0, 0.0, 4.0), 'io'), (0.71, (0.0, 0.0, 13.0), 'out'), (0.90, (0.0, 0.0, -3.0), 'io'), (1, I['ear_r'], 'io')],
        # the left hand closes into the fist that lands, and opens as he pushes off it
        'fingers_l': [(0, M['fingers_l'], 'lin'), (0.18, (8.0, 0.0, 0.0), 'out'), (0.56, (-58.0, 0.0, 0.0), 'io'), (0.74, (-52.0, 0.0, 0.0), 'io'), (1, I['fingers_l'], 'io')],
        'fingers_r': [(0, M['fingers_r'], 'lin'), (0.22, (10.0, 0.0, 0.0), 'out'), (0.60, (14.0, 0.0, 0.0), 'io'), (0.80, (-16.0, 0.0, 0.0), 'io'), (1, I['fingers_r'], 'io')],
    }, t)
    return out


def shift_out(t):
    """Not shift_in backwards. 0 to 0.24 he heaves: the chest lifts and the claws open. To 0.66 he folds: the knees give, the spine curls over, the head
    tucks to the chest and the arms cross over it while he shrinks. Then the head comes back up and he is still, in the match pose."""
    st = {
        's': tr([(0, 1.0, 'lin'), (0.22, 1.0, 'lin'), (0.74, 0.70, 'io'), (1, M_S, 'io')], t),
        'hip': tr([(0, (20.46, 0.0), 'lin'), (0.24, (20.9, -0.4), 'io'), (0.66, (13.0, 1.2), 'io'), (0.84, (12.0, 0.8), 'io'), (1, M_HIP, 'io')], t),
        'hp': tr([(0, 0.0, 'lin'), (0.24, -4.0, 'io'), (0.66, 14.0, 'io'), (1, M_HP, 'io')], t),
        'torso': [tr([(0, 0.0, 'lin'), (0.24, -9.0, 'io'), (0.66, 30.0, 'io'), (0.86, 16.0, 'io'), (1, M_SPINE, 'io')], t)],
        'twist': tr([(0, 0.0, 'lin'), (0.40, 6.0, 'io'), (0.72, -5.0, 'io'), (0.88, 3.0, 'io'), (1, 0.0, 'io')], t),
    }
    st['limbs'] = {}
    for name, start, done in (('ar', 0.20, 0.66), ('al', 0.30, 0.80)):
        v = 1.0 - tr([(start, 0.0, 'lin'), (done, 1.0, 'io')], t)
        st['limbs'][name] = arm(st, name, M_HOOK[name], (0.0, 0.0), 0.0, v, turn_in=M_IN[name])
    st['limbs']['ll'] = {'plant': 0.0}
    st['limbs']['lr'] = {'plant': 0.0}
    out = RIG.solve(st)
    rot(out, {
        'neck': [(0, I['neck'], 'lin'), (0.24, (-10.0, 0.0, 0.0), 'io'), (0.62, (26.0, 3.0, 0.0), 'io'), (0.86, (14.0, -2.0, 0.0), 'io'), (1, M['neck'], 'io')],
        'head': [(0, I['head'], 'lin'), (0.24, (-12.0, 2.0, 0.0), 'io'), (0.64, (28.0, 6.0, 3.0), 'io'), (0.88, (-10.0, -8.0, -5.0), 'io'), (1, M['head'], 'io')],
        'jaw': [(0, I['jaw'], 'lin'), (0.24, (22.0, 0.0, 0.0), 'out'), (0.62, (4.0, 0.0, 0.0), 'io'), (1, M['jaw'], 'io')],
        'hair': [(0, I['hair'], 'lin'), (0.24, (-8.0, 0.0, 0.0), 'io'), (0.66, (20.0, 0.0, 0.0), 'io'), (0.90, (-10.0, 0.0, 0.0), 'io'), (1, M['hair'], 'io')],
        'ear_l': [(0, I['ear_l'], 'lin'), (0.6, (0.0, 0.0, -12.0), 'io'), (1, M['ear_l'], 'io')],
        'ear_r': [(0, I['ear_r'], 'lin'), (0.66, (0.0, 0.0, 12.0), 'io'), (1, M['ear_r'], 'io')],
        'fingers_l': [(0, I['fingers_l'], 'lin'), (0.24, (12.0, 0.0, 0.0), 'out'), (0.80, (-40.0, 0.0, 0.0), 'io'), (1, M['fingers_l'], 'io')],
        'fingers_r': [(0, I['fingers_r'], 'lin'), (0.20, (12.0, 0.0, 0.0), 'out'), (0.70, (-36.0, 0.0, 0.0), 'io'), (1, M['fingers_r'], 'io')],
    }, t)
    return out


def old_roar():
    path = 'src/main/resources/assets/supernaturalbloodlines/animations/entity/%s.animation.json' % MODEL
    text = subprocess.run(['git', 'show', 'origin/main:' + path], capture_output=True, check=True).stdout.decode('utf-8')
    clip = json.loads(text)['animations']['animation.%s.roar' % MODEL]
    assert abs(clip['animation_length'] - 2.6) < 1e-6, clip['animation_length']
    return clip


def roar():
    """deucalion.py's roar (2.6 s) at 2.2 s: the dip and the rise a little quicker, the shudder at its old beat but seven beats long instead of ten, the
    release as long as it was. It starts and ends on idle's first frame."""
    def tmap(t):
        if t <= 0.35:
            return t * 0.30 / 0.35
        if t <= 0.75:
            return 0.30 + (t - 0.35) * 0.38 / 0.40
        if t < 1.9:
            return 0.68 + (t - 0.75) if t - 0.75 <= 0.78 else None      # the shudder keys past the shorter hold are dropped
        return 1.50 if t < 2.6 else 2.2
    clip = mr.retimed(old_roar(), tmap, 2.2)
    return mr.pin_ends(clip, dict(I, forearm_l=(I_ARM[1], 0, 0), forearm_r=(I_ARM[1], 0, 0)), 2.2)


def build():
    clips = {'shift_in': mr.sampled(shift_in, LIMB_BONES), 'roar': roar(), 'shift_out': mr.sampled(shift_out, LIMB_BONES)}
    first, last, end = shift_in(0.0), shift_out(1.0), shift_in(1.0)
    for b in first:
        for c in first[b]:
            assert all(abs(x - y) < 1e-6 for x, y in zip(first[b][c], last[b][c])), ('shift_out does not end on the match pose', b, c, first[b][c], last[b][c])
    want = dict(I, arm_l=(0, 0, 0), arm_r=(0, 0, 0), forearm_l=(I_ARM[1], 0, 0), forearm_r=(I_ARM[1], 0, 0))
    for b, chans in end.items():
        for c, v in chans.items():
            w = want.get(b, (0.0, 0.0, 0.0)) if c == 'rotation' else (1.0, 1.0, 1.0) if c == 'scale' else (0.0, 0.0, 0.0)
            assert all(abs(x - y) < 1e-3 for x, y in zip(v, w)), ('shift_in does not end on idle', b, c, v, w)
    assert RIG.reach < 0.6, 'a limb target is beyond reach:\n' + mr.reach_report(RIG, {'shift_in': shift_in, 'shift_out': shift_out})
    return clips


if __name__ == '__main__':
    mr.write(sys.argv[1], MODEL, build())
    print('wrote %s; the furthest a limb target lay beyond reach: %.2f px' % (sys.argv[1], max(RIG.reach, 0.0)))
