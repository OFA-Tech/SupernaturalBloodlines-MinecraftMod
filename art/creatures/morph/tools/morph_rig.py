"""The solver behind the morph pose sheets of Deucalion, Peter and the Beast (Klaus's klaus_keys.py has its own, older one).

A rig here is a side view: a hip bone that moves and pitches, an optional torso chain on it, and limbs of two rigid pieces plus an end bone (hand, paw,
foot). Every rotation is about X. A pose sheet gives, per tick, the root's scale and offset, the hips in world px, the pitches, and for each limb where
its end joint is; solve() turns that into bone keys, so a planted paw stays where it is while the body pitches and the root grows.

Space: the animation file's (facing -Z, +X rotation tips a nose down and swings a hanging limb back, y up, z back), in model px. Bone pivots are the
geo's, which are given for the unrotated model: a bone's rest rotation is added to its key to get its real angle.
"""
import json
import math

STEP = 0.05


def R(v, a):
    """Rotate a (y, z) offset by an X rotation: +a tips a nose down and swings a hanging limb back."""
    a = math.radians(a)
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c + v[1] * s, v[1] * c - v[0] * s)


def add(a, b):
    return (a[0] + b[0], a[1] + b[1])


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1])


class Limb:
    """`bones`: (first, second, end). `pivots`: first, second, [a bone between that keeps its rest angle], end. `rests`: one rest X rotation per pivot.
    `on`: 'hips' or 'torso'. The middle joint folds to the side it is on at rest (`bend` says it for a limb that is straight at rest)."""

    def __init__(self, bones, pivots, rests, on='hips', bend=None):
        self.bones, self.on = bones, on
        self.p0, self.rest1 = pivots[0], rests[0]
        self.v1 = sub(pivots[1], pivots[0])
        self.rest2 = rests[1]
        v2, turn = (0.0, 0.0), 0.0
        for i in range(1, len(pivots) - 1):                       # the pieces after the second joint are one rigid piece, in the second bone's frame
            v2 = add(v2, R(sub(pivots[i + 1], pivots[i]), turn))
            turn += rests[i + 1] if i + 1 < len(pivots) - 1 else 0.0
        self.v2, self.between = v2, turn
        self.rest_end = rests[-1]
        self.flat = sum(rests)                                    # the end bone's real angle when the model stands at rest
        self.l1, self.l2 = math.hypot(*self.v1), math.hypot(*self.v2)
        self.bend = bend if bend is not None else (1.0 if self._side((0.0, 0.0), 0.0, self.rest1, self.rest_end_joint((0.0, 0.0))) >= 0 else -1.0)

    def _side(self, j0, a, a1, target):
        """Which side of the line from the first joint to the target the middle joint is on."""
        k = add(j0, R(self.v1, a + a1))
        return (k[1] - j0[1]) * (target[0] - j0[0]) - (k[0] - j0[0]) * (target[1] - j0[1])

    def end(self, j0, a, a1, a2):
        """The end joint for real angles a1 (first bone, from the parent frame at angle a) and a2 (second, from the first)."""
        return add(add(j0, R(self.v1, a + a1)), R(self.v2, a + a1 + a2))

    def solve(self, j0, a, target):
        """Real angles (a1, a2) that put the end joint on `target`, and how far the target lay beyond the limb's reach."""
        d = math.hypot(*sub(target, j0))
        over = d - self.l1 - self.l2
        best, hit = None, 1e9
        cosv = max(-1.0, min(1.0, (d * d - self.l1 ** 2 - self.l2 ** 2) / (2 * self.l1 * self.l2)))
        th = math.degrees(math.acos(cosv)) * self.bend           # the angle between the two pieces
        base = math.degrees(math.atan2(self.v2[1], self.v2[0]) - math.atan2(self.v1[1], self.v1[0]))
        for cand in (th - base, -th - base, base - th, base + th):   # whichever sign convention holds, the right answer hits the target with the right bend
            cand = (cand + 180) % 360 - 180
            for a1 in self._aim(j0, a, cand, target):
                e = self.end(j0, a, a1, cand)
                err = math.hypot(*sub(e, target)) if over <= 0 else abs(math.hypot(*sub(e, j0)) - (self.l1 + self.l2))
                hit = min(hit, err)
                score = err + (0.0 if self._side(j0, a, a1, target) * self.bend >= 0 else 1000.0)
                if best is None or score < best[0]:
                    best = (score, a1, cand)
        assert over > 0 or hit < 1e-6, 'the limb solver missed a target it can reach'
        return best[1], best[2], over

    def _aim(self, j0, a, a2, target):
        tip = add(self.v1, R(self.v2, a2))                        # the end joint in the first bone's frame
        want = sub(target, j0)
        turn = math.degrees(math.atan2(tip[1], tip[0]) - math.atan2(want[1], want[0]))
        return [((turn - a + 180) % 360) - 180]

    def rest_end_joint(self, j0):
        return self.end(j0, 0.0, self.rest1, self.rest2)


class Rig:
    def __init__(self, hips, hips_pivot, torso, limbs, pivot_z=0.0, root='root'):
        """`torso`: [(bone, pivot, rest rotation)] from the hips up, may be empty. `limbs`: {name: Limb}."""
        self.hips, self.bp, self.torso, self.limbs, self.pz, self.root = hips, hips_pivot, torso, limbs, pivot_z, root
        self.reach, self.last = 0.0, {}

    def to_model(self, s, rz, w):
        return (w[0] / s, self.pz + (w[1] - rz - self.pz) / s)

    def to_world(self, s, rz, m):
        return (m[0] * s, self.pz + (m[1] - self.pz) * s + rz)

    def frames(self, st):
        """Model-space joints the limbs hang from and the angle of each frame: {'hips': (fn, angle), 'torso': (fn, angle)}."""
        s, rz, hp = st['s'], st.get('root_z', 0.0), st.get('hp', 0.0)
        origin = self.to_model(s, rz, st['hip'])
        tips = st.get('torso', [0.0] * len(self.torso))
        frames = {'hips': (lambda p: add(origin, R(sub(p, self.bp), hp)), hp)}
        base, angle, last = origin, hp, self.bp
        for (bone, pivot, rest), delta in zip(self.torso, tips):
            base = add(base, R(sub(pivot, last), angle))
            angle += rest + delta
            last = pivot
        frames['torso'] = ((lambda p, base=base, angle=angle, last=last: add(base, R(sub(p, last), angle))), angle) if self.torso else frames['hips']
        return frames

    def joint_world(self, st, limb):
        """Where a limb's first joint (shoulder or hip) is, in world px."""
        fn, _ = self.frames(st)[self.limbs[limb].on]
        return self.to_world(st['s'], st.get('root_z', 0.0), fn(self.limbs[limb].p0))

    def as_target(self, st, limb, d1, d2, d3):
        """The limb target that gives back the keys (d1, d2, d3): how a limb is handed over to a pose that is known as keys (idle's first frame)."""
        L = self.limbs[limb]
        fn, a = self.frames(st)[L.on]
        e = L.end(fn(L.p0), a, L.rest1 + d1, L.rest2 + d2)
        pitch = d3 - (L.flat - a - (L.rest1 + d1) - (L.rest2 + d2) - L.between - L.rest_end)
        return {'at': self.to_world(st['s'], st.get('root_z', 0.0), e), 'pitch': pitch}

    def rest_end_world(self, limb):
        """Where a limb's end joint is when the model stands at rest (the spot a planted paw is measured from)."""
        L = self.limbs[limb]
        return L.rest_end_joint(L.p0)

    def solve(self, st):
        """One state -> {bone: {channel: vector}}. Limb targets: {'plant': z offset, 'lift': px} from the rest spot, or {'at': (y, z)} in world px;
        'pitch' turns the end bone (0 keeps it as it is at rest, + lets the toes hang); or {'fk': (d1, d2, d3)} for keys given directly."""
        s, rz, hp = st['s'], st.get('root_z', 0.0), st.get('hp', 0.0)
        hm = self.to_model(s, rz, st['hip'])
        out = {self.root: {'scale': (s, s, s), 'position': (0.0, 0.0, rz)},
               self.hips: {'rotation': (hp, st.get('yaw', 0.0), 0.0), 'position': (0.0, hm[0] - self.bp[0], hm[1] - self.bp[1])}}
        for (bone, _, _), delta in zip(self.torso, st.get('torso', [0.0] * len(self.torso))):
            out[bone] = {'rotation': (delta, st.get('twist', 0.0) if bone == self.torso[-1][0] else 0.0, 0.0)}   # a twist of the chest leaves the feet where they are
        frames = self.frames(st)
        for name, L in self.limbs.items():
            leg = st['limbs'][name]
            if 'fk' in leg:
                ds = leg['fk']
            else:
                fn, a = frames[L.on]
                j0 = fn(L.p0)
                if 'at' in leg:
                    target = self.to_model(s, rz, leg['at'])
                else:
                    r = self.rest_end_world(name)
                    target = (r[0] + leg.get('lift', 0.0) / s, self.to_model(s, rz, (0.0, r[1] + leg.get('plant', 0.0)))[1])
                a1, a2, over = L.solve(j0, a, target)
                self.reach = max(self.reach, over)
                self.last[name] = over
                ds = (a1 - L.rest1, a2 - L.rest2, L.flat + leg.get('pitch', 0.0) - a - a1 - a2 - L.between - L.rest_end)
            for b, d in zip(L.bones, ds):
                out[b] = {'rotation': (d, 0.0, leg.get('turn', 0.0) if b == L.bones[0] else 0.0)}
        return out


def reach_report(rig, clips):
    """For each clip (name -> pose function) and limb, the tick where its target lay furthest beyond reach (negative: inside)."""
    lines = []
    for name, fn in clips.items():
        worst = {}
        for i in range(21):
            rig.last = {}
            fn(i * STEP)
            for limb, over in rig.last.items():
                if limb not in worst or over > worst[limb][0]:
                    worst[limb] = (over, i * STEP)
        lines.append('%s: ' % name + ', '.join('%s %+.2f px at %.2f' % (limb, o, t) for limb, (o, t) in worst.items()))
    return '\n'.join(lines)


def stamp(t):
    s = ('%.4f' % t).rstrip('0')
    return s + '0' if s.endswith('.') else s


def rnd(v):
    return [round(x, 3) + 0.0 for x in v]


def sampled(fn, linear, length=1.0):
    """A clip sampled once a tick: catmullrom on the body, the head and the fur, linear on the solved limbs (`linear` is their bone names)."""
    bones = {}
    for i in range(round(length / STEP) + 1):
        t = i * STEP
        for b, chans in fn(t).items():
            for c, v in chans.items():
                bones.setdefault(b, {}).setdefault(c, {})[stamp(t)] = rnd(v) if b in linear else {'post': rnd(v), 'lerp_mode': 'catmullrom'}
    return {'animation_length': length, 'bones': bones}


def retimed(clip, tmap, length):
    """An eased clip with its key times moved by `tmap` (old time -> new time): the same poses and easings, holds made shorter by the map."""
    out = {'animation_length': length, 'bones': {}}
    for b, chans in clip['bones'].items():
        for c, keys in chans.items():
            if isinstance(keys, list) or 'vector' in keys or 'post' in keys:
                out['bones'].setdefault(b, {})[c] = keys
                continue
            new = {}
            for k, v in keys.items():
                nt = tmap(float(k))
                if nt is not None:
                    new[stamp(nt)] = v
            out['bones'].setdefault(b, {})[c] = new
    return out


def pin_ends(clip, pose, length):
    """Make an eased clip start and end on `pose` ({bone: rotation}): a key at 0 and at the end is set to it, or added where the bone had none there
    (a bone keyed only in the middle would otherwise hold its first key from 0, and jump)."""
    for b, v in pose.items():
        keys = clip['bones'].get(b, {}).get('rotation')
        if not isinstance(keys, dict) or 'vector' in keys:
            continue
        first, last = min(keys, key=float), max(keys, key=float)
        if float(first) > 1e-6:
            keys[stamp(0.0)] = list(v)
        else:
            keys[first] = list(v)
        if float(last) < length - 1e-6:
            keys[stamp(length)] = {'vector': list(v), 'easing': 'easeInOutSine'}
        else:
            keys[last] = {'vector': list(v), 'easing': keys[last]['easing']} if isinstance(keys[last], dict) and 'easing' in keys[last] else list(v)
        clip['bones'][b]['rotation'] = dict(sorted(keys.items(), key=lambda kv: float(kv[0])))
    return clip


def write(path, model, clips):
    with open(path, 'w', encoding='utf-8', newline='') as f:
        json.dump({'format_version': '1.8.0', 'animations': {'animation.%s.%s' % (model, n): c for n, c in clips.items()}}, f, indent=1)
