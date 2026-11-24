"""The Beast of Gevaudan's morph clips as a pose sheet: shift_in (1.0 s) and the roar re-timed to 2.2 s. It has no shift_out.

    python art/creatures/morph/tools/beast_keys.py <out.json>      (from the repo root; the old roar is read from origin/main)

Like klaus_keys.py it writes a file to import into the .bbmodel, never the game file. The Beast walks on its knuckles and is three blocks tall, so
its first frame is it at 0.52 of its size, upright in the Host's place. Its rhythm is to loom and then crash: for 0.3 s it only rises and opens,
taller than anything else in the fight, and then all of it comes down on its fists at once.

In this model `*_left` bones are on -X and `*_right` on +X.
"""
import json
import subprocess
import sys

import morph_rig as mr
from klaus_keys import tr

MODEL = 'beast_of_gevaudan'
ARM_P = [(36.0, -10.0), (20.0, -6.5), (4.0, -17.0)]
LEG_P = [(25.0, 15.0), (13.0, 8.5), (7.0, 17.5)]
BODY = (25.0, 14.0)
RIG = mr.Rig('body', BODY, [('waist', (26.0, 10.0), 0), ('chest', (29.0, -1.0), 0)], {
    'al': mr.Limb(('arm_left', 'forearm_left', 'hand_left'), ARM_P, [0, 0, 0], on='torso'),
    'ar': mr.Limb(('arm_right', 'forearm_right', 'hand_right'), ARM_P, [0, 0, 0], on='torso'),
    'll': mr.Limb(('leg_left', 'shin_left', 'foot_left'), LEG_P, [0, 0, 0]), 'lr': mr.Limb(('leg_right', 'shin_right', 'foot_right'), LEG_P, [0, 0, 0])})
LIMB_BONES = {b for L in RIG.limbs.values() for b in L.bones}
Z = (0.0, 0.0, 0.0)                                                  # idle's first frame is the model's rest pose, every bone

# The match pose: upright on folded legs at 0.52 of its size, hunched over hooked claws, standing where the Host stood.
M_S, M_HP, M_TORSO, M_HIP = 0.52, -76.0, (7.0, 11.0), (11.8, 4.5)
M_HOOK = {'al': (-5.5, -5.5), 'ar': (-8.5, -3.5)}                    # wrist from the shoulder (down, forward), world px: the -X claw is the higher one
M_LEG = {'ll': -14.0, 'lr': -10.0}                                   # world px forward of the rest spots
M = {'neck': (30.0, -4.0, 0.0), 'head': (-8.0, -6.0, -3.0), 'jaw': (10.0, 0.0, 0.0), 'ear_left': (-12.0, 0.0, 0.0), 'ear_right': (-8.0, 0.0, 0.0),
     'mane_front': (-8.0, 0.0, 0.0), 'mane_crest': (-6.0, 0.0, 0.0), 'mane_rear': (-4.0, 0.0, 0.0), 'wisp_tail': (60.0, 6.0, 0.0)}


def mix(a, b, u):
    return tuple(x + (y - x) * u for x, y in zip(a, b))


def fist(st, name, hook, u):
    """A fist between a point held off the shoulder (u = 0) and its own spot on the ground (u = 1)."""
    sh = RIG.joint_world(st, name)
    grow = st['s'] / M_S
    return mix((sh[0] + hook[0] * grow, sh[1] + hook[1] * grow), RIG.rest_end_world(name), u)


def step(t, a, frm, to, lift, toes):
    mid = (frm + to) / 2
    return {'plant': tr([(frm, a, 'lin'), (to, 0.0, 'io')], t), 'lift': tr([(frm, 0.0, 'lin'), (mid, lift, 'out'), (to, 0.0, 'in')], t),
            'pitch': tr([(frm, 0.0, 'lin'), (mid, toes, 'out'), (to, 0.0, 'in')], t)}


def shift_in(t):
    """0 the match pose. 0 to 0.30 it looms: it grows, straightens to its full height and lifts its claws, the head going back. 0.30 to 0.60 it comes
    down, the feet stepping back under it one after the other, and the -X fist hits at 0.60, the other a tick later. 0.60 to 0.70 the whole front of it
    sinks; the mane goes over front to back, the head last. A second, smaller sag at 0.92 and it is still, in idle's first frame."""
    st = {
        # it is not full size until it is nearly down: grown to 0.82 by 0.30 and falling on a square, it stood 4 blocks tall at 0.45
        's': tr([(0, M_S, 'lin'), (0.30, 0.70, 'io'), (0.60, 1.0, 'io'), (1, 1.0, 'lin')], t),
        'hp': tr([(0, M_HP, 'lin'), (0.30, -79.0, 'io'), (0.60, 0.0, 'in15'), (0.69, 7.0, 'out3'), (0.84, 0.6, 'io'), (0.92, 2.0, 'io'), (1, 0.0, 'io')], t),
        'torso': [tr([(0, M_TORSO[0], 'lin'), (0.28, 2.0, 'io'), (0.56, 0.0, 'io'), (0.70, 4.0, 'out'), (0.88, 0.0, 'io')], t),
                  tr([(0, M_TORSO[1], 'lin'), (0.28, -4.0, 'io'), (0.58, 0.0, 'io'), (0.71, 7.0, 'out3'), (0.86, -1.5, 'io'), (1, 0.0, 'io')], t)],
        'hip': tr([(0, M_HIP, 'lin'), (0.30, (15.5, 5.5), 'io'), (0.60, (25.2, 14.0), 'in'), (0.70, (22.4, 14.7), 'out3'), (0.85, (25.3, 14.0), 'io'),
                   (0.92, (24.6, 14.1), 'io'), (1, BODY, 'io')], t),
    }
    raised = tr([(0, 0.0, 'lin'), (0.30, 1.0, 'io')], t)                # the claws go up and out while it looms
    st['limbs'] = {
        'al': {'at': fist(st, 'al', mix(M_HOOK['al'], (2.0, -8.0), raised), tr([(0.34, 0.0, 'lin'), (0.60, 1.0, 'in3')], t)),
               'pitch': tr([(0, -30.0, 'lin'), (0.30, -60.0, 'io'), (0.60, 0.0, 'in')], t), 'turn': -14.0 * tr([(0, 0.0, 'lin'), (0.30, 1.0, 'io'), (0.56, 0.0, 'io')], t)},
        'ar': {'at': fist(st, 'ar', mix(M_HOOK['ar'], (-1.0, -9.0), raised), tr([(0.40, 0.0, 'lin'), (0.65, 1.0, 'in3')], t)),
               'pitch': tr([(0, -20.0, 'lin'), (0.34, -50.0, 'io'), (0.65, 0.0, 'in')], t), 'turn': 10.0 * tr([(0, 0.0, 'lin'), (0.32, 1.0, 'io'), (0.60, 0.0, 'io')], t)},
        'll': step(t, M_LEG['ll'], 0.30, 0.48, 3.0, 10.0),
        'lr': step(t, M_LEG['lr'], 0.44, 0.60, 2.6, 9.0),
    }
    out = RIG.solve(st)
    tracks = {
        'neck': [(0, M['neck'], 'lin'), (0.30, (-14.0, 5.0, 0.0), 'io'), (0.60, (-10.0, 0.0, 0.0), 'io'), (0.74, (5.0, -3.0, 0.0), 'out3'), (0.90, (-3.0, 0.0, 0.0), 'io'),
                 (1, Z, 'io')],
        'head': [(0, M['head'], 'lin'), (0.30, (-26.0, 8.0, 4.0), 'io'), (0.60, (-12.0, 0.0, 0.0), 'io'), (0.76, (5.0, -5.0, -3.0), 'out3'), (0.92, (-2.0, 1.0, 0.0), 'io'),
                 (1, Z, 'io')],
        'jaw': [(0, M['jaw'], 'lin'), (0.30, (30.0, 0.0, 0.0), 'io'), (0.58, (26.0, 0.0, 0.0), 'io'), (0.66, (0.0, 0.0, 0.0), 'in'), (0.78, (5.0, 0.0, 0.0), 'out'),
                (1, Z, 'io')],
        'ear_left': [(0, M['ear_left'], 'lin'), (0.60, (-6.0, 0.0, 0.0), 'io'), (0.68, (-22.0, 0.0, 0.0), 'out'), (0.88, (4.0, 0.0, 0.0), 'io'), (1, Z, 'io')],
        'ear_right': [(0, M['ear_right'], 'lin'), (0.62, (-5.0, 0.0, 0.0), 'io'), (0.71, (-20.0, 0.0, 0.0), 'out'), (0.92, (3.0, 0.0, 0.0), 'io'), (1, Z, 'io')],
        'mane_front': [(0, M['mane_front'], 'lin'), (0.30, (-16.0, 0.0, 0.0), 'io'), (0.60, (-12.0, 0.0, 0.0), 'io'), (0.69, (20.0, 0.0, 0.0), 'out3'), (0.86, (-5.0, 0.0, 0.0), 'io'),
                       (1, Z, 'io')],
        'mane_crest': [(0, M['mane_crest'], 'lin'), (0.32, (-13.0, 0.0, 0.0), 'io'), (0.63, (-10.0, 0.0, 0.0), 'io'), (0.74, (17.0, 0.0, 0.0), 'out3'), (0.90, (-4.0, 0.0, 0.0), 'io'),
                       (1, Z, 'io')],
        'mane_rear': [(0, M['mane_rear'], 'lin'), (0.34, (-10.0, 0.0, 0.0), 'io'), (0.66, (-8.0, 0.0, 0.0), 'io'), (0.79, (14.0, 0.0, 0.0), 'out3'), (0.94, (-3.0, 0.0, 0.0), 'io'),
                      (1, Z, 'io')],
        'wisp_tail': [(0, M['wisp_tail'], 'lin'), (0.30, (66.0, -4.0, 0.0), 'io'), (0.62, (30.0, 3.0, 0.0), 'io'), (0.82, (-12.0, -2.0, 0.0), 'io'), (0.94, (4.0, 0.0, 0.0), 'io'),
                      (1, Z, 'io')],
    }
    for b, keys in tracks.items():
        out[b] = {'rotation': tr(keys, t)}
    return out


def old_roar():
    path = 'src/main/resources/assets/supernaturalbloodlines/animations/entity/%s.animation.json' % MODEL
    text = subprocess.run(['git', 'show', 'origin/main:' + path], capture_output=True, check=True).stdout.decode('utf-8')
    clip = json.loads(text)['animations']['animation.%s.roar' % MODEL]
    assert abs(clip['animation_length'] - 2.6) < 1e-6, clip['animation_length']
    return clip


def roar():
    """The old roar (2.6 s) at 2.2 s: the crouch and the burst a little quicker (0.45 and 0.85 become 0.40 and 0.76), the held roar 0.79 s instead of
    1.1 s (six shudders at the old beat instead of nine), the recoil and the settle as long as they were. It already starts and ends on idle's first frame."""
    def tmap(t):
        if t <= 0.45:
            return t * 0.40 / 0.45
        if t <= 0.85:
            return 0.40 + (t - 0.45) * 0.36 / 0.40
        if t < 1.95 - 1e-6:
            return 0.76 + (t - 0.85) if t - 0.85 <= 0.705 else None      # the shudder keys past the shorter hold are dropped
        if t < 2.3 - 1e-6:
            return 1.55
        return 1.90 if t < 2.6 - 1e-6 else 2.2
    clip = mr.retimed(old_roar(), tmap, 2.2)
    clip['loop'] = False
    return clip


def build():
    clips = {'shift_in': mr.sampled(shift_in, LIMB_BONES), 'roar': roar()}
    for b, chans in shift_in(1.0).items():
        for c, v in chans.items():
            w = (1.0, 1.0, 1.0) if c == 'scale' else (0.0, 0.0, 0.0)
            assert all(abs(x - y) < 1e-3 for x, y in zip(v, w)), ('shift_in does not end on idle', b, c, v, w)
    assert RIG.reach < 0.6, 'a limb target is beyond reach:\n' + mr.reach_report(RIG, {'shift_in': shift_in})
    return clips


if __name__ == '__main__':
    mr.write(sys.argv[1], MODEL, build())
    print('wrote %s; the furthest a limb target lay beyond reach: %.2f px' % (sys.argv[1], max(RIG.reach, 0.0)))
