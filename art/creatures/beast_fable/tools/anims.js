// node anims.js <spec.json> <out animation.json>
// The Beast's ten animations, written straight to GeckoLib's animation format (Bedrock 1.8.0 + per-keyframe easing).
// Rotation signs, as GeckoLib and Blockbench play them:
//   +X: a part in front of its pivot dips, a hanging limb swings back.  +Y: turns to the creature's left.
//   +Z: rolls to the creature's right (a hanging left limb swings outwards).
// An easing belongs to the keyframe it is written on and shapes the move that arrives there.
const fs = require('fs');

const ID = 'animation.beast_gevaudan_fable.';
const spec = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const boneNames = new Set(spec.bones.map(b => b.name));
const out = { format_version: '1.8.0', geckolib_format_version: 2, animations: {} };
const num = n => Math.round(n * 1000) / 1000;
const SINE = 'easeInOutSine';

// tracks: { bone: { r|p|s: [[time, [x,y,z], easing?], ...] } }
function anim(name, length, loop, tracks) {
  const bones = {};
  for (const [bone, chans] of Object.entries(tracks)) {
    if (!boneNames.has(bone)) throw new Error(name + ': no bone ' + bone);
    const o = {};
    for (const [ch, keys] of Object.entries(chans)) {
      const t = {};
      for (const [time, vec, easing] of keys) {
        if (time < 0 || time > length + 1e-6) throw new Error(name + '/' + bone + ': key outside the animation at ' + time);
        const v = vec.map(num);
        t[num(time).toFixed(3).replace(/0+$/, '').replace(/\.$/, '.0')] = easing ? { vector: v, easing } : v;
      }
      o[{ r: 'rotation', p: 'position', s: 'scale' }[ch]] = t;
    }
    bones[bone] = o;
  }
  const a = { animation_length: length, bones };
  if (loop) a.loop = loop;
  out.animations[ID + name] = a;
}

// Four poses a quarter-cycle apart, played as a loop of length T starting at phase `shift` (0 or 0.5).
function cyc(T, poses, shift = 0, ease = SINE) {
  const k = shift ? 2 : 0;
  return [0, 1, 2, 3, 4].map(i => [T * i / 4, poses[(i + k) % 4], i ? ease : undefined]);
}
const xs = (...v) => v.map(x => [x, 0, 0]);
const ys = (...v) => v.map(y => [0, y, 0]);
const zs = (...v) => v.map(z => [0, 0, z]);

// ------------------------------------------------------------------ idle: slow breath, a watching head, bristling fur
{
  const T = 4;
  anim('idle', T, true, {
    body: { p: [[0, [0, 0, 0]], [2, [0, -0.5, 0], SINE], [4, [0, 0, 0], SINE]] },
    chest: { r: [[0, [0, 0, 0]], [2, [1.5, 0, 0], SINE], [4, [0, 0, 0], SINE]], s: [[0, [1, 1, 1]], [1.8, [1.025, 1.02, 1], SINE], [4, [1, 1, 1], SINE]] },
    arm_left: { r: [[0, [0, 0, 0]], [2, [-1.5, 0, 0], SINE], [4, [0, 0, 0], SINE]] },
    arm_right: { r: [[0, [0, 0, 0]], [2, [-1.5, 0, 0], SINE], [4, [0, 0, 0], SINE]] },
    neck: { r: [[0, [0, 0, 0]], [1.3, [-2, 5, 0], SINE], [2.9, [1, -6, 0], SINE], [4, [0, 0, 0], SINE]] },
    head: { r: [[0, [0, 0, 0]], [1.3, [-1, 6, 2], SINE], [2.9, [2, -7, -2], SINE], [4, [0, 0, 0], SINE]] },
    jaw: { r: [[0, [0, 0, 0]], [1.6, [0, 0, 0]], [2.0, [7, 0, 0], 'easeOutQuad'], [2.6, [5, 0, 0], SINE], [3.1, [0, 0, 0], 'easeInQuad'], [4, [0, 0, 0]]] },
    ear_left: { r: [[0, [0, 0, 0]], [2.5, [0, 0, 0]], [2.6, [14, 0, 10], 'easeOutQuad'], [2.8, [0, 0, 0], 'easeInOutQuad'], [4, [0, 0, 0]]] },
    ear_right: { r: [[0, [0, 0, 0]], [0.7, [0, 0, 0]], [0.8, [14, 0, -10], 'easeOutQuad'], [1.0, [0, 0, 0], 'easeInOutQuad'], [4, [0, 0, 0]]] },
    mane_front: { r: [[0, [0, 0, 0]], [1, [-8, 0, 0], SINE], [2, [3, 0, 0], SINE], [3, [-6, 0, 0], SINE], [4, [0, 0, 0], SINE]] },
    mane_crest: { r: [[0, [0, 0, 0]], [1.2, [0, 0, 5], SINE], [3.2, [0, 0, -5], SINE], [4, [0, 0, 0], SINE]] },
    mane_rear: { r: [[0, [0, 0, 0]], [1.4, [7, 0, 0], SINE], [2.6, [-4, 0, 0], SINE], [4, [0, 0, 0], SINE]] },
    wisp_tail: { r: [[0, [0, 0, 0]], [1, [5, 14, 0], SINE], [2, [-4, 0, 0], SINE], [3, [5, -14, 0], SINE], [4, [0, 0, 0], SINE]] },
    hand_right: { r: [[0, [0, 0, 0]], [3.0, [0, 0, 0]], [3.25, [-9, 0, 0], 'easeOutQuad'], [3.6, [0, 0, 0], 'easeInQuad'], [4, [0, 0, 0]]] },
  });
}

// ------------------------------------------------------------------ walk: knuckle-walk, diagonal pairs
{
  const T = 1.2;
  const arm = xs(-20, 0, 20, -4), fore = xs(6, 0, -6, -34), hand = xs(-12, 0, 14, 26);
  const leg = xs(-20, 0, 22, -6), shin = xs(6, 0, -8, 28), foot = xs(-6, 0, 18, -22);
  anim('walk', T, true, {
    body: { p: cyc(T, [[0, -1.2, 0], [0, 0, 0], [0, -1.2, 0], [0, 0, 0]]), r: cyc(T, zs(2, 0, -2, 0)) },
    waist: { r: cyc(T, ys(3, 0, -3, 0)) },
    chest: { r: cyc(T, ys(-7, 0, 7, 0)) },
    neck: { r: cyc(T, [[2, 2, 0], [-1, 0, 0], [2, -2, 0], [-1, 0, 0]]) },
    head: { r: cyc(T, [[1, 2, -2], [-1, 0, 0], [1, -2, 2], [-1, 0, 0]]) },
    jaw: { r: cyc(T, xs(3, 5, 3, 5)) },
    arm_left: { r: cyc(T, arm) }, forearm_left: { r: cyc(T, fore) }, hand_left: { r: cyc(T, hand) },
    arm_right: { r: cyc(T, arm, 0.5) }, forearm_right: { r: cyc(T, fore, 0.5) }, hand_right: { r: cyc(T, hand, 0.5) },
    leg_right: { r: cyc(T, leg) }, shin_right: { r: cyc(T, shin) }, foot_right: { r: cyc(T, foot) },
    leg_left: { r: cyc(T, leg, 0.5) }, shin_left: { r: cyc(T, shin, 0.5) }, foot_left: { r: cyc(T, foot, 0.5) },
    mane_front: { r: cyc(T, xs(4, -4, 4, -4)) },
    mane_crest: { r: cyc(T, zs(4, 0, -4, 0)) },
    mane_rear: { r: cyc(T, xs(-5, 5, -5, 5)) },
    wisp_tail: { r: cyc(T, [[4, 10, 0], [-4, 0, 0], [4, -10, 0], [-4, 0, 0]]) },
  });
}

// ------------------------------------------------------------------ run: bounding charge, arms together then legs
{
  const T = 0.7;
  const arm = xs(-28, 0, 22, -24), fore = xs(0, 0, -10, -28), hand = xs(-12, 5, 24, 0);
  const leg = xs(26, -15, -36, 10), shin = xs(-10, 45, 10, 0), foot = xs(25, -30, -8, 10);
  const out3 = (keys, z) => keys.map(([t, v, e]) => [t, [v[0], 0, z], e]);
  anim('run', T, true, {
    body: { p: cyc(T, [[0, -1.5, 0], [0, -2.5, 0], [0, -1, 0], [0, 3, 0]]), r: cyc(T, xs(6, 2, -4, -8)) },
    waist: { r: cyc(T, xs(-4, 6, 12, -8)) },
    chest: { r: cyc(T, xs(0, 3, 4, -4)) },
    neck: { r: cyc(T, xs(-4, -8, -10, 10)) },
    head: { r: cyc(T, xs(-2, -4, -6, 8)) },
    jaw: { r: cyc(T, xs(8, 14, 8, 14)) },
    ear_left: { r: cyc(T, [[25, 0, 6], [28, 0, 6], [25, 0, 6], [28, 0, 6]]) },
    ear_right: { r: cyc(T, [[25, 0, -6], [28, 0, -6], [25, 0, -6], [28, 0, -6]]) },
    arm_left: { r: out3(cyc(T, arm), 6) }, forearm_left: { r: cyc(T, fore) }, hand_left: { r: cyc(T, hand) },
    arm_right: { r: out3(cyc(T, arm), -6) }, forearm_right: { r: cyc(T, fore) }, hand_right: { r: cyc(T, hand) },
    leg_left: { r: cyc(T, leg) }, shin_left: { r: cyc(T, shin) }, foot_left: { r: cyc(T, foot) },
    leg_right: { r: cyc(T, leg) }, shin_right: { r: cyc(T, shin) }, foot_right: { r: cyc(T, foot) },
    mane_front: { r: cyc(T, xs(20, 14, 22, 12)) },
    mane_crest: { r: cyc(T, zs(5, -5, 5, -5)) },
    mane_rear: { r: cyc(T, xs(22, 14, 20, 12)) },
    wisp_tail: { r: cyc(T, [[-18, 6, 0], [-26, 0, 0], [-16, -6, 0], [-28, 0, 0]]) },
  });
}

// ------------------------------------------------------------------ attack_bite: coil back, lunge, snap, worry
anim('attack_bite', 1.0, false, {
  body: { p: [[0, [0, 0, 0]], [0.3, [0, -1, 2.5], 'easeOutQuad'], [0.42, [0, -1.5, -5], 'easeInExpo'], [0.7, [0, -1, -3], SINE], [1, [0, 0, 0], SINE]] },
  chest: { r: [[0, [0, 0, 0]], [0.3, [-5, 0, 0], 'easeOutQuad'], [0.42, [7, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  neck: { r: [[0, [0, 0, 0]], [0.3, [-16, 0, 0], 'easeOutQuad'], [0.42, [12, 0, 0], 'easeInExpo'], [0.58, [10, 8, 0], SINE], [0.7, [10, -8, 0], SINE], [1, [0, 0, 0], SINE]] },
  head: { r: [[0, [0, 0, 0]], [0.3, [-14, 0, 0], 'easeOutQuad'], [0.42, [6, 0, 0], 'easeInExpo'], [0.58, [4, 6, 12], SINE], [0.7, [4, -6, -12], SINE], [0.82, [2, 3, 6], SINE], [1, [0, 0, 0], SINE]] },
  jaw: { r: [[0, [0, 0, 0]], [0.3, [46, 0, 0], 'easeOutBack'], [0.4, [48, 0, 0]], [0.46, [0, 0, 0], 'easeInExpo'], [0.82, [0, 0, 0]], [0.9, [8, 0, 0], SINE], [1, [0, 0, 0], SINE]] },
  ear_left: { r: [[0, [0, 0, 0]], [0.3, [28, 0, 8], 'easeOutQuad'], [0.8, [22, 0, 6]], [1, [0, 0, 0], SINE]] },
  ear_right: { r: [[0, [0, 0, 0]], [0.3, [28, 0, -8], 'easeOutQuad'], [0.8, [22, 0, -6]], [1, [0, 0, 0], SINE]] },
  arm_left: { r: [[0, [0, 0, 0]], [0.3, [-6, 0, 0], 'easeOutQuad'], [0.42, [14, 0, 4], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  arm_right: { r: [[0, [0, 0, 0]], [0.3, [-6, 0, 0], 'easeOutQuad'], [0.42, [14, 0, -4], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  forearm_left: { r: [[0, [0, 0, 0]], [0.42, [-10, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  forearm_right: { r: [[0, [0, 0, 0]], [0.42, [-10, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  leg_left: { r: [[0, [0, 0, 0]], [0.3, [-6, 0, 0], 'easeOutQuad'], [0.42, [12, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  leg_right: { r: [[0, [0, 0, 0]], [0.3, [-6, 0, 0], 'easeOutQuad'], [0.42, [12, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  mane_front: { r: [[0, [0, 0, 0]], [0.3, [-18, 0, 0], 'easeOutQuad'], [0.46, [16, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  mane_rear: { r: [[0, [0, 0, 0]], [0.3, [-14, 0, 0], 'easeOutQuad'], [0.5, [14, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
  wisp_tail: { r: [[0, [0, 0, 0]], [0.3, [14, 0, 0], 'easeOutQuad'], [0.46, [-20, 0, 0], 'easeInExpo'], [1, [0, 0, 0], SINE]] },
});

// ------------------------------------------------------------------ attack_swipe: left arm rears up and rakes across
anim('attack_swipe', 1.1, false, {
  body: { p: [[0, [0, 0, 0]], [0.35, [-1.5, 0.5, 1], 'easeOutQuad'], [0.5, [1.5, -1.5, -3], 'easeInQuad'], [1.1, [0, 0, 0], SINE]], r: [[0, [0, 0, 0]], [0.35, [0, 0, 4], 'easeOutQuad'], [0.5, [0, 0, -5], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  waist: { r: [[0, [0, 0, 0]], [0.35, [-6, 6, 0], 'easeOutQuad'], [0.5, [6, -8, 0], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  chest: { r: [[0, [0, 0, 0]], [0.35, [-8, 14, 4], 'easeOutQuad'], [0.5, [8, -22, -6], 'easeInQuad'], [0.64, [8, -26, -6], 'easeOutQuad'], [1.1, [0, 0, 0], SINE]] },
  neck: { r: [[0, [0, 0, 0]], [0.35, [4, -10, 0], 'easeOutQuad'], [0.5, [0, 16, 0], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  head: { r: [[0, [0, 0, 0]], [0.35, [-4, -6, -6], 'easeOutQuad'], [0.5, [4, 10, 8], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  jaw: { r: [[0, [0, 0, 0]], [0.35, [22, 0, 0], 'easeOutQuad'], [0.64, [26, 0, 0]], [1.1, [0, 0, 0], SINE]] },
  arm_left: { r: [[0, [0, 0, 0]], [0.35, [-105, 0, 38], 'easeOutBack'], [0.5, [-38, 0, -30], 'easeInQuad'], [0.64, [-6, 0, -36], 'easeOutQuad'], [1.1, [0, 0, 0], SINE]] },
  forearm_left: { r: [[0, [0, 0, 0]], [0.35, [-50, 0, 0], 'easeOutQuad'], [0.5, [-6, 0, 0], 'easeInQuad'], [0.64, [-22, 0, 0], 'easeOutQuad'], [1.1, [0, 0, 0], SINE]] },
  hand_left: { r: [[0, [0, 0, 0]], [0.35, [-30, 0, 0], 'easeOutQuad'], [0.5, [28, 0, 0], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  arm_right: { r: [[0, [0, 0, 0]], [0.35, [6, 0, -4], 'easeOutQuad'], [0.5, [10, 0, -8], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  forearm_right: { r: [[0, [0, 0, 0]], [0.5, [-12, 0, 0], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  mane_front: { r: [[0, [0, 0, 0]], [0.35, [-14, 0, 0], 'easeOutQuad'], [0.5, [12, 0, 0], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  mane_crest: { r: [[0, [0, 0, 0]], [0.35, [0, 0, -10], 'easeOutQuad'], [0.5, [0, 0, 14], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
  wisp_tail: { r: [[0, [0, 0, 0]], [0.35, [0, -22, 0], 'easeOutQuad'], [0.55, [0, 28, 0], 'easeInQuad'], [1.1, [0, 0, 0], SINE]] },
});

// ------------------------------------------------------------------ roar: gathers low, then throws the head up and holds, shaking
{
  const shake = (t0, t1, base, amp) => {
    const keys = [];
    let s = 1;
    for (let t = t0; t <= t1 + 1e-6; t += 0.12) { keys.push([t, [base[0] + (s > 0 ? 1 : -1), base[1], base[2] + amp * s]]); s = -s; }
    return keys;
  };
  anim('roar', 2.6, false, {
    body: { p: [[0, [0, 0, 0]], [0.5, [0, -2, 1.5], 'easeOutQuad'], [0.8, [0, 0.5, -1], 'easeOutBack'], [2.0, [0, 0.5, -1]], [2.6, [0, 0, 0], SINE]] },
    waist: { r: [[0, [0, 0, 0]], [0.5, [4, 0, 0], 'easeOutQuad'], [0.8, [-5, 0, 0], 'easeOutBack'], [2.0, [-5, 0, 0]], [2.6, [0, 0, 0], SINE]] },
    chest: { r: [[0, [0, 0, 0]], [0.5, [5, 0, 0], 'easeOutQuad'], [0.8, [-9, 0, 0], 'easeOutBack'], [2.0, [-9, 0, 0]], [2.6, [0, 0, 0], SINE]], s: [[0, [1, 1, 1]], [0.5, [1.05, 1.04, 1], 'easeOutQuad'], [0.8, [1, 1, 1], 'easeOutQuad'], [2.6, [1, 1, 1]]] },
    arm_left: { r: [[0, [0, 0, 0]], [0.5, [-5, 0, 0], 'easeOutQuad'], [0.8, [14, 0, 5], 'easeOutBack'], [2.0, [14, 0, 5]], [2.6, [0, 0, 0], SINE]] },
    arm_right: { r: [[0, [0, 0, 0]], [0.5, [-5, 0, 0], 'easeOutQuad'], [0.8, [14, 0, -5], 'easeOutBack'], [2.0, [14, 0, -5]], [2.6, [0, 0, 0], SINE]] },
    neck: { r: [[0, [0, 0, 0]], [0.5, [14, 0, 0], 'easeOutQuad'], [0.8, [-16, 0, 0], 'easeOutBack'], [2.0, [-15, 0, 0]], [2.6, [0, 0, 0], SINE]] },
    head: { r: [[0, [0, 0, 0]], [0.5, [10, 0, 0], 'easeOutQuad'], [0.8, [-15, 0, 0], 'easeOutBack'], ...shake(0.92, 1.88, [-14, 0, 0], 3.5), [2.0, [-13, 0, 0]], [2.6, [0, 0, 0], SINE]] },
    jaw: { r: [[0, [0, 0, 0]], [0.5, [4, 0, 0]], [0.8, [58, 0, 0], 'easeOutBack'], ...shake(0.92, 1.88, [55, 0, 0], 0).map(([t, v], i) => [t, [v[0] + (i % 2 ? 3 : -3), 0, 0]]), [2.0, [50, 0, 0]], [2.45, [0, 0, 0], 'easeInQuad'], [2.6, [0, 0, 0]]] },
    ear_left: { r: [[0, [0, 0, 0]], [0.8, [34, 0, 10], 'easeOutQuad'], [2.0, [34, 0, 10]], [2.6, [0, 0, 0], SINE]] },
    ear_right: { r: [[0, [0, 0, 0]], [0.8, [34, 0, -10], 'easeOutQuad'], [2.0, [34, 0, -10]], [2.6, [0, 0, 0], SINE]] },
    mane_front: { r: [[0, [0, 0, 0]], [0.5, [14, 0, 0], 'easeOutQuad'], [0.8, [-24, 0, 0], 'easeOutBack'], [1.1, [-18, 0, 0], SINE], [1.4, [-26, 0, 0], SINE], [1.7, [-18, 0, 0], SINE], [2.0, [-24, 0, 0], SINE], [2.6, [0, 0, 0], SINE]], s: [[0, [1, 1, 1]], [0.8, [1.1, 1.35, 1], 'easeOutBack'], [2.0, [1.1, 1.3, 1]], [2.6, [1, 1, 1], SINE]] },
    mane_crest: { r: [[0, [0, 0, 0]], [1.1, [0, 0, 6], SINE], [1.4, [0, 0, -6], SINE], [1.7, [0, 0, 6], SINE], [2.0, [0, 0, -4], SINE], [2.6, [0, 0, 0], SINE]], s: [[0, [1, 1, 1]], [0.8, [1, 1.35, 1.05], 'easeOutBack'], [2.0, [1, 1.3, 1.05]], [2.6, [1, 1, 1], SINE]] },
    mane_rear: { r: [[0, [0, 0, 0]], [0.5, [10, 0, 0], 'easeOutQuad'], [0.8, [-20, 0, 0], 'easeOutBack'], [1.25, [-14, 0, 0], SINE], [1.55, [-22, 0, 0], SINE], [2.0, [-18, 0, 0], SINE], [2.6, [0, 0, 0], SINE]], s: [[0, [1, 1, 1]], [0.8, [1.1, 1.35, 1], 'easeOutBack'], [2.0, [1.1, 1.3, 1]], [2.6, [1, 1, 1], SINE]] },
    wisp_tail: { r: [[0, [0, 0, 0]], [0.8, [22, 0, 0], 'easeOutBack'], [1.2, [16, 10, 0], SINE], [1.6, [22, -10, 0], SINE], [2.0, [18, 0, 0], SINE], [2.6, [0, 0, 0], SINE]], s: [[0, [1, 1, 1]], [0.8, [1, 1.3, 1.3], 'easeOutBack'], [2.0, [1, 1.25, 1.25]], [2.6, [1, 1, 1], SINE]] },
  });
}

// ------------------------------------------------------------------ rear_up: crouch, stand on the hind legs with arms wide, slam down
anim('rear_up', 3.2, false, {
  body: { p: [[0, [0, 0, 0]], [0.5, [0, -3, 1], 'easeOutQuad'], [1.1, [0, 3, 3], 'easeOutBack'], [2.3, [0, 3, 3]], [2.7, [0, -2.5, 0], 'easeInQuad'], [3.2, [0, 0, 0], 'easeOutBack']] },
  waist: { r: [[0, [0, 0, 0]], [0.5, [6, 0, 0], 'easeOutQuad'], [1.1, [-52, 0, 0], 'easeOutBack'], [1.7, [-49, 0, 3], SINE], [2.3, [-53, 0, -3], SINE], [2.7, [5, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], 'easeOutBack']] },
  chest: { r: [[0, [0, 0, 0]], [0.5, [4, 0, 0], 'easeOutQuad'], [1.1, [-18, 0, 0], 'easeOutBack'], [2.3, [-20, 0, 0], SINE], [2.7, [8, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], 'easeOutBack']] },
  neck: { r: [[0, [0, 0, 0]], [0.5, [-8, 0, 0], 'easeOutQuad'], [1.1, [34, 0, 0], 'easeOutQuad'], [2.3, [30, 0, 0], SINE], [2.7, [-6, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  head: { r: [[0, [0, 0, 0]], [1.1, [22, 0, 0], 'easeOutQuad'], [1.5, [16, 8, 0], SINE], [1.9, [18, -8, 0], SINE], [2.3, [20, 0, 0], SINE], [2.7, [-4, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  jaw: { r: [[0, [0, 0, 0]], [0.9, [6, 0, 0]], [1.2, [44, 0, 0], 'easeOutBack'], [2.2, [40, 0, 0]], [2.6, [6, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  arm_left: { r: [[0, [0, 0, 0]], [0.5, [8, 0, 0], 'easeOutQuad'], [1.1, [-38, 0, 46], 'easeOutBack'], [1.7, [-44, 0, 50], SINE], [2.3, [-60, 0, 40], SINE], [2.7, [-4, 0, 4], 'easeInQuad'], [3.2, [0, 0, 0], 'easeOutBack']] },
  arm_right: { r: [[0, [0, 0, 0]], [0.5, [8, 0, 0], 'easeOutQuad'], [1.1, [-38, 0, -46], 'easeOutBack'], [1.7, [-44, 0, -50], SINE], [2.3, [-60, 0, -40], SINE], [2.7, [-4, 0, -4], 'easeInQuad'], [3.2, [0, 0, 0], 'easeOutBack']] },
  forearm_left: { r: [[0, [0, 0, 0]], [0.5, [-14, 0, 0], 'easeOutQuad'], [1.1, [-52, 0, 0], 'easeOutBack'], [2.3, [-40, 0, 0], SINE], [2.7, [-4, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  forearm_right: { r: [[0, [0, 0, 0]], [0.5, [-14, 0, 0], 'easeOutQuad'], [1.1, [-52, 0, 0], 'easeOutBack'], [2.3, [-40, 0, 0], SINE], [2.7, [-4, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  hand_left: { r: [[0, [0, 0, 0]], [1.1, [-28, 0, 0], 'easeOutBack'], [2.3, [-34, 0, 0], SINE], [2.7, [0, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0]]] },
  hand_right: { r: [[0, [0, 0, 0]], [1.1, [-28, 0, 0], 'easeOutBack'], [2.3, [-34, 0, 0], SINE], [2.7, [0, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0]]] },
  leg_left: { r: [[0, [0, 0, 0]], [0.5, [-12, 0, 0], 'easeOutQuad'], [1.1, [10, 0, 6], 'easeOutBack'], [2.3, [10, 0, 6]], [2.7, [-6, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  leg_right: { r: [[0, [0, 0, 0]], [0.5, [-12, 0, 0], 'easeOutQuad'], [1.1, [10, 0, -6], 'easeOutBack'], [2.3, [10, 0, -6]], [2.7, [-6, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  shin_left: { r: [[0, [0, 0, 0]], [0.5, [18, 0, 0], 'easeOutQuad'], [1.1, [-14, 0, 0], 'easeOutBack'], [2.3, [-14, 0, 0]], [2.7, [10, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  shin_right: { r: [[0, [0, 0, 0]], [0.5, [18, 0, 0], 'easeOutQuad'], [1.1, [-14, 0, 0], 'easeOutBack'], [2.3, [-14, 0, 0]], [2.7, [10, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  foot_left: { r: [[0, [0, 0, 0]], [0.5, [-8, 0, 0], 'easeOutQuad'], [1.1, [4, 0, 0], 'easeOutBack'], [2.3, [4, 0, 0]], [2.7, [-4, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  foot_right: { r: [[0, [0, 0, 0]], [0.5, [-8, 0, 0], 'easeOutQuad'], [1.1, [4, 0, 0], 'easeOutBack'], [2.3, [4, 0, 0]], [2.7, [-4, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  mane_front: { r: [[0, [0, 0, 0]], [1.1, [-20, 0, 0], 'easeOutBack'], [2.3, [-16, 0, 0], SINE], [2.7, [18, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  mane_rear: { r: [[0, [0, 0, 0]], [1.1, [-16, 0, 0], 'easeOutBack'], [2.3, [-12, 0, 0], SINE], [2.7, [16, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
  wisp_tail: { r: [[0, [0, 0, 0]], [1.1, [-30, 0, 0], 'easeOutQuad'], [1.7, [-26, 12, 0], SINE], [2.3, [-30, -12, 0], SINE], [2.7, [14, 0, 0], 'easeInQuad'], [3.2, [0, 0, 0], SINE]] },
});

// ------------------------------------------------------------------ hurt: a recoil to its right with a snarl
anim('hurt', 0.6, false, {
  body: { p: [[0, [0, 0, 0]], [0.12, [1.5, -1.5, 2.5], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]], r: [[0, [0, 0, 0]], [0.12, [-3, 0, 5], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  chest: { r: [[0, [0, 0, 0]], [0.12, [-4, -10, 4], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  neck: { r: [[0, [0, 0, 0]], [0.12, [-10, -14, 0], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  head: { r: [[0, [0, 0, 0]], [0.12, [-10, -18, 14], 'easeOutQuad'], [0.3, [-4, -8, 6], SINE], [0.6, [0, 0, 0], SINE]] },
  jaw: { r: [[0, [0, 0, 0]], [0.12, [30, 0, 0], 'easeOutQuad'], [0.4, [14, 0, 0], SINE], [0.6, [0, 0, 0], SINE]] },
  ear_left: { r: [[0, [0, 0, 0]], [0.12, [32, 0, 10], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  ear_right: { r: [[0, [0, 0, 0]], [0.12, [32, 0, -10], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  arm_left: { r: [[0, [0, 0, 0]], [0.12, [-14, 0, 10], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  forearm_left: { r: [[0, [0, 0, 0]], [0.12, [-22, 0, 0], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  arm_right: { r: [[0, [0, 0, 0]], [0.12, [-6, 0, -4], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  mane_front: { r: [[0, [0, 0, 0]], [0.12, [-22, 0, 0], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  mane_rear: { r: [[0, [0, 0, 0]], [0.12, [-18, 0, 0], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
  wisp_tail: { r: [[0, [0, 0, 0]], [0.12, [20, 16, 0], 'easeOutQuad'], [0.6, [0, 0, 0], SINE]] },
});

// ------------------------------------------------------------------ death: a last roar, the arms give way, it rolls onto its right side
{
  const limp = (s) => ({
    ['arm_' + s]: { r: [[0, [0, 0, 0]], [0.5, [-8, 0, 0], 'easeOutQuad'], [1.3, [26, 0, 0], 'easeInQuad'], [2.0, [-24, 0, s === 'left' ? -14 : 8], SINE], [3, [-28, 0, s === 'left' ? -18 : 10], SINE]] },
    ['forearm_' + s]: { r: [[0, [0, 0, 0]], [1.3, [-58, 0, 0], 'easeInQuad'], [2.0, [-30, 0, 0], SINE], [3, [-26, 0, 0], SINE]] },
    ['hand_' + s]: { r: [[0, [0, 0, 0]], [1.3, [30, 0, 0], 'easeInQuad'], [2.0, [38, 0, 0], SINE], [3, [42, 0, 0], SINE]] },
    ['leg_' + s]: { r: [[0, [0, 0, 0]], [1.3, [-18, 0, 0], 'easeInQuad'], [2.0, [14, 0, s === 'left' ? -12 : 6], SINE], [3, [18, 0, s === 'left' ? -14 : 6], SINE]] },
    ['shin_' + s]: { r: [[0, [0, 0, 0]], [1.3, [28, 0, 0], 'easeInQuad'], [2.0, [10, 0, 0], SINE], [3, [6, 0, 0], SINE]] },
    ['foot_' + s]: { r: [[0, [0, 0, 0]], [1.3, [-16, 0, 0], 'easeInQuad'], [2.0, [22, 0, 0], SINE], [3, [26, 0, 0], SINE]] },
  });
  anim('death', 3.0, 'hold_on_last_frame', {
    body: {
      p: [[0, [0, 0, 0]], [0.5, [0, 1, 2], 'easeOutQuad'], [1.3, [0, -8, -1], 'easeInQuad'], [2.0, [3, -8, 0], 'easeOutBounce'], [3, [3, -8, 0]]],
      r: [[0, [0, 0, 0]], [0.5, [-6, 0, 0], 'easeOutQuad'], [1.3, [8, 0, 10], 'easeInQuad'], [2.0, [0, 0, 84], 'easeOutBounce'], [3, [0, 0, 84]]],
    },
    waist: { r: [[0, [0, 0, 0]], [0.5, [-12, 0, 0], 'easeOutQuad'], [1.3, [8, 0, 0], 'easeInQuad'], [2.0, [6, 8, 0], SINE], [3, [8, 10, 0], SINE]] },
    chest: { r: [[0, [0, 0, 0]], [0.5, [-8, 0, 0], 'easeOutQuad'], [1.3, [8, 0, 0], 'easeInQuad'], [2.0, [4, 6, 0], SINE], [2.5, [3, 6, 0], SINE], [3, [5, 6, 0], SINE]], s: [[0, [1, 1, 1]], [2.0, [1, 1, 1]], [2.5, [1.03, 1.02, 1], SINE], [3, [0.98, 0.98, 1], SINE]] },
    neck: { r: [[0, [0, 0, 0]], [0.5, [-24, 0, 0], 'easeOutBack'], [1.3, [10, 0, 0], 'easeInQuad'], [2.0, [8, 16, 0], SINE], [3, [12, 22, 0], SINE]] },
    head: { r: [[0, [0, 0, 0]], [0.5, [-24, 0, 0], 'easeOutBack'], [0.8, [-20, 6, 0], SINE], [1.3, [8, 0, 0], 'easeInQuad'], [2.0, [0, 14, -10], SINE], [3, [4, 18, -14], SINE]] },
    jaw: { r: [[0, [0, 0, 0]], [0.5, [52, 0, 0], 'easeOutBack'], [1.1, [44, 0, 0]], [1.4, [10, 0, 0], 'easeInQuad'], [2.0, [24, 0, 0], 'easeOutQuad'], [3, [18, 0, 0], SINE]] },
    ear_left: { r: [[0, [0, 0, 0]], [0.5, [30, 0, 8], 'easeOutQuad'], [3, [36, 0, 12], SINE]] },
    ear_right: { r: [[0, [0, 0, 0]], [0.5, [30, 0, -8], 'easeOutQuad'], [3, [36, 0, -12], SINE]] },
    ...limp('left'),
    ...limp('right'),
    mane_front: { r: [[0, [0, 0, 0]], [0.5, [-22, 0, 0], 'easeOutBack'], [2.0, [16, 0, 0], SINE], [3, [26, 0, 0], SINE]], s: [[0, [1, 1, 1]], [2.0, [1, 1, 1]], [3, [1, 0.55, 1], SINE]] },
    mane_crest: { r: [[0, [0, 0, 0]], [2.0, [0, 0, 8], SINE], [3, [0, 0, 12], SINE]], s: [[0, [1, 1, 1]], [2.0, [1, 1, 1]], [3, [1, 0.55, 1], SINE]] },
    mane_rear: { r: [[0, [0, 0, 0]], [0.5, [-18, 0, 0], 'easeOutBack'], [2.0, [16, 0, 0], SINE], [3, [26, 0, 0], SINE]], s: [[0, [1, 1, 1]], [2.0, [1, 1, 1]], [3, [1, 0.55, 1], SINE]] },
    wisp_tail: { r: [[0, [0, 0, 0]], [0.5, [20, 0, 0], 'easeOutQuad'], [2.0, [-10, 18, 0], SINE], [3, [-16, 24, 0], SINE]], s: [[0, [1, 1, 1]], [2.0, [1, 1, 1]], [3, [1, 0.5, 0.6], SINE]] },
  });
}

// ------------------------------------------------------------------ collapse (phase 3): it shudders, sinks onto its belly, and the shadow leaves it
{
  const fold = (s) => {
    const z = s === 'left' ? 1 : -1;
    return {
      ['arm_' + s]: { r: [[0, [0, 0, 0]], [0.6, [-4, 0, 3 * z], SINE], [1.6, [6, 0, 10 * z], 'easeInQuad'], [3.4, [8, 0, 12 * z], SINE]] },
      ['forearm_' + s]: { r: [[0, [0, 0, 0]], [0.6, [-8, 0, 0], SINE], [1.6, [-56, 0, 0], 'easeInQuad'], [3.4, [-58, 0, 0], SINE]] },
      ['hand_' + s]: { r: [[0, [0, 0, 0]], [1.6, [48, 0, 0], 'easeInQuad'], [3.4, [50, 0, 0], SINE]] },
      ['leg_' + s]: { r: [[0, [0, 0, 0]], [0.6, [-6, 0, 0], SINE], [1.6, [-48, 0, 8 * z], 'easeInQuad'], [3.4, [-50, 0, 10 * z], SINE]] },
      ['shin_' + s]: { r: [[0, [0, 0, 0]], [0.6, [8, 0, 0], SINE], [1.6, [80, 0, 0], 'easeInQuad'], [3.4, [82, 0, 0], SINE]] },
      ['foot_' + s]: { r: [[0, [0, 0, 0]], [1.6, [-14, 0, 0], 'easeInQuad'], [3.4, [-14, 0, 0], SINE]] },
    };
  };
  const fade = (r, t0 = 1.6) => ({ r, s: [[0, [1, 1, 1]], [t0, [1.15, 1.4, 1.15], 'easeOutQuad'], [t0 + 0.8, [0.05, 0.05, 0.05], 'easeInQuad'], [3.4, [0.05, 0.05, 0.05]]] });
  anim('collapse', 3.4, 'hold_on_last_frame', {
    body: {
      p: [[0, [0, 0, 0]], [0.2, [0.8, 0, 0], SINE], [0.4, [-0.8, -0.5, 0], SINE], [0.6, [0, -1, 0], SINE], [1.6, [0, -16.5, -2], 'easeOutBounce'], [3.4, [0, -17, -2], SINE]],
      r: [[0, [0, 0, 0]], [0.2, [0, 0, 4], SINE], [0.4, [0, 0, -4], SINE], [0.6, [0, 0, 2], SINE], [1.6, [0, 0, 0], 'easeInQuad'], [2.4, [0, 0, 0]], [3.4, [0, 0, 5], SINE]],
    },
    waist: { r: [[0, [0, 0, 0]], [0.6, [-4, 0, 0], SINE], [1.6, [10, 0, 0], 'easeInQuad'], [3.4, [12, 0, 0], SINE]] },
    chest: { r: [[0, [0, 0, 0]], [0.6, [-3, 0, 0], SINE], [1.6, [6, 0, 0], 'easeInQuad'], [3.4, [7, 0, 0], SINE]], s: [[0, [1, 1, 1]], [2.2, [1.03, 1.03, 1], SINE], [3.4, [0.97, 0.97, 1], SINE]] },
    neck: { r: [[0, [0, 0, 0]], [0.2, [-6, 8, 0], SINE], [0.4, [-6, -8, 0], SINE], [0.6, [-10, 0, 0], SINE], [1.6, [4, 0, 0], 'easeInQuad'], [2.4, [6, 0, 0], SINE], [3.4, [8, 10, 0], SINE]] },
    head: { r: [[0, [0, 0, 0]], [0.2, [-4, 8, 8], SINE], [0.4, [-4, -8, -8], SINE], [0.6, [-10, 0, 0], SINE], [1.6, [-22, 0, 0], 'easeInQuad'], [2.4, [-24, 0, 0], SINE], [3.4, [-22, 6, -10], SINE]] },
    jaw: { r: [[0, [0, 0, 0]], [0.6, [34, 0, 0], 'easeOutQuad'], [1.5, [26, 0, 0]], [1.7, [2, 0, 0], 'easeInQuad'], [2.4, [12, 0, 0], SINE], [3.4, [6, 0, 0], SINE]] },
    ear_left: { r: [[0, [0, 0, 0]], [0.6, [26, 0, 8], SINE], [3.4, [38, 0, 14], SINE]] },
    ear_right: { r: [[0, [0, 0, 0]], [0.6, [26, 0, -8], SINE], [3.4, [38, 0, -14], SINE]] },
    ...fold('left'),
    ...fold('right'),
    mane_front: fade([[0, [0, 0, 0]], [1.6, [-26, 0, 0], 'easeOutQuad'], [3.4, [-30, 0, 0]]]),
    mane_crest: fade([[0, [0, 0, 0]], [1.6, [0, 0, 6], 'easeOutQuad'], [3.4, [0, 0, -6]]], 1.7),
    mane_rear: fade([[0, [0, 0, 0]], [1.6, [-22, 0, 0], 'easeOutQuad'], [3.4, [-26, 0, 0]]], 1.8),
    wisp_tail: fade([[0, [0, 0, 0]], [1.6, [30, 0, 0], 'easeOutQuad'], [3.4, [34, 0, 0]]], 1.9),
  });
}

fs.writeFileSync(process.argv[3], JSON.stringify(out, null, 2) + '\n');
console.log(Object.entries(out.animations).map(([k, a]) => `${k.slice(ID.length)} ${a.animation_length}s ${a.loop || 'once'} ${Object.keys(a.bones).length} bones`).join('\n'));
