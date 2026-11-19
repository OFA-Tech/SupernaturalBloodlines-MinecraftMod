// node model.js <out spec.json>
// The Beast of Gevaudan (Fable version): bones, cubes and box-UV packing, in Blockbench space.
// Front is -Z, up is +Y, the creature's left is +X. 1 unit = 1 px = 1/16 block, built at final size.
// Bones have no rest rotation: every tilt is a cube rotation, so animation axes stay world-aligned.
const fs = require('fs');

const TEX_W = 256;
const bones = [];
const deg = r => Math.round(r * 18000 / Math.PI) / 100;

function bone(name, parent, pivot) {
  const b = { name, parent, pivot, cubes: [] };
  bones.push(b);
  return b;
}

// Box by its two corners in the unrotated frame. o.rot rotates it about o.origin (default: its centre).
function box(b, name, from, to, o = {}) {
  const size = to.map((v, i) => v - from[i]);
  if (size.some(s => s < 0 || s !== Math.round(s))) throw new Error('bad size ' + name + ' ' + size);
  const centre = from.map((v, i) => (v + to[i]) / 2);
  const c = { name, from, to, rotation: o.rot || [0, 0, 0], origin: o.origin || centre, mat: o.mat || 'fur', tone: o.tone || 0 };
  b.cubes.push(c);
  return c;
}

// Box running from joint A to joint B (its local -Y axis points along A->B), w wide and d deep.
function seg(b, name, A, B, w, d, o = {}) {
  const v = B.map((x, i) => x - A[i]);
  const len = Math.hypot(...v);
  const n = v.map(x => x / len);
  const h = Math.round(len) + (o.extra === undefined ? 1 : o.extra);
  const mid = A.map((x, i) => (x + B[i]) / 2);
  const rx = -Math.asin(n[2]);
  const rz = Math.atan2(n[0], -n[1]);
  const from = [mid[0] - w / 2, mid[1] - h / 2, mid[2] - d / 2];
  const to = [mid[0] + w / 2, mid[1] + h / 2, mid[2] + d / 2];
  return box(b, name, from, to, { ...o, rot: [deg(rx), 0, deg(rz)], origin: mid });
}

const lerp = (A, B, t) => A.map((x, i) => x + (B[i] - x) * t);
// Rotate a point about an origin by an X rotation in degrees (same sense as Blockbench: +X lifts the front).
function rotX(p, origin, d) {
  const r = d * Math.PI / 180, y = p[1] - origin[1], z = p[2] - origin[2];
  return [p[0], origin[1] + y * Math.cos(r) - z * Math.sin(r), origin[2] + y * Math.sin(r) + z * Math.cos(r)];
}

// ---------------------------------------------------------------- trunk
const root = bone('root', null, [0, 0, 0]);
const body = bone('body', 'root', [0, 25, 14]);
box(body, 'pelvis', [-6.5, 18.5, 9.5], [6.5, 29.5, 19.5], { rot: [5, 0, 0] });

const waist = bone('waist', 'body', [0, 26, 10]);
box(waist, 'belly', [-6, 20, -2.5], [6, 31, 10.5], { rot: [12, 0, 0], tone: -1 });

const chest = bone('chest', 'waist', [0, 29, -1]);
const RIB = { origin: [0, 30, -9], rot: [22, 0, 0] };
box(chest, 'ribcage', [-10, 20.5, -17.5], [10, 39.5, -0.5], RIB);
box(chest, 'pecs', [-7, 21.5, -19.5], [7, 31.5, -17.5], { ...RIB, mat: 'chest' });
box(chest, 'hump', [-7, 39.5, -14], [7, 44.5, -2], { ...RIB, tone: 1 });
box(chest, 'hump_ridge', [-4, 44.5, -13], [4, 46.5, -6], { ...RIB, tone: 1 });

// ragged fur along the hump: one plane down the spine, two across it, each on its own bone so it can bristle
const ribP = p => rotX(p, RIB.origin, RIB.rot[0]);
const maneF = bone('mane_front', 'chest', ribP([0, 44.5, -11]));
box(maneF, 'mane_front', [-7, 44.5, -11], [7, 50.5, -11], { ...RIB, mat: 'mane' });
const maneC = bone('mane_crest', 'chest', ribP([0, 45, -6]));
box(maneC, 'mane_crest', [0, 44.5, -16], [0, 51.5, 3], { ...RIB, mat: 'mane' });
const maneR = bone('mane_rear', 'chest', ribP([0, 44.5, -4]));
box(maneR, 'mane_rear', [-6, 44.5, -4], [6, 49.5, -4], { ...RIB, mat: 'mane' });

// ---------------------------------------------------------------- neck and head
const neck = bone('neck', 'chest', [0, 33, -15]);
box(neck, 'neck', [-5.5, 26, -23.5], [5.5, 37, -15.5], { rot: [-20, 0, 0] });

const head = bone('head', 'neck', [0, 31, -22]);
box(head, 'skull', [-7, 25, -32], [7, 34, -22], { mat: 'skull' });
box(head, 'snout', [-6, 25, -35], [6, 29, -32], { mat: 'snout' });
box(head, 'nose', [-2, 27, -36], [2, 29, -35], { mat: 'nose' });
box(head, 'gum_upper', [-5, 23, -33], [5, 25, -23], { mat: 'gum' });
box(head, 'throat', [-5, 18, -24], [5, 25, -23], { mat: 'gum' });
box(head, 'teeth_upper_front', [-6, 21, -35], [6, 25, -35], { mat: 'teeth_upper' });

const jaw = bone('jaw', 'head', [0, 23, -23]);
box(jaw, 'jaw', [-6, 18, -34], [6, 21, -22], { mat: 'jaw' });
box(jaw, 'gum_lower', [-5, 21, -33], [5, 23, -23], { mat: 'gum' });
box(jaw, 'tongue', [-2, 23, -32], [2, 24, -24], { mat: 'tongue' });
box(jaw, 'teeth_lower_front', [-5.5, 21, -33.5], [5.5, 25, -33.5], { mat: 'teeth_lower' });

// ---------------------------------------------------------------- left side (mirrored below)
function side(s) {
  const L = s === 1 ? 'left' : 'right';
  const X = p => [p[0] * s, p[1], p[2]];
  const R = r => [r[0], r[1] * s, r[2] * s];
  const bx = (b, name, from, to, o = {}) => {
    const f = X(from), t = X(to);
    const lo = [Math.min(f[0], t[0]), f[1], f[2]], hi = [Math.max(f[0], t[0]), t[1], t[2]];
    return box(b, name + '_' + L, lo, hi, { ...o, rot: o.rot ? R(o.rot) : undefined, origin: o.origin ? X(o.origin) : undefined });
  };
  const sg = (b, name, A, B, w, d, o) => seg(b, name + '_' + L, X(A), X(B), w, d, o);

  // head parts
  bx(head, 'brow', [0, 30.5, -33], [7, 32.5, -32], { rot: [0, 0, 13], mat: 'brow' });
  bx(head, 'teeth_upper_side', [6, 21, -35], [6, 25, -26], { mat: 'teeth_upper' });
  bx(jaw, 'teeth_lower_side', [5.5, 21, -33.5], [5.5, 25, -24.5], { mat: 'teeth_lower' });
  const ear = bone('ear_' + L, 'head', X([6, 34, -25]));
  const EAR = { origin: [6, 34, -25], rot: [15, 0, -28], mat: 'ear' };
  bx(ear, 'ear_base', [4.5, 33.5, -26], [7.5, 36.5, -24], EAR);
  bx(ear, 'ear_mid', [5, 36.5, -26], [7, 38.5, -25], EAR);
  bx(ear, 'ear_tip', [6, 38.5, -26], [7, 40.5, -25], EAR);

  // arm: shoulder S, elbow E, wrist W
  const S = [11, 36, -10], E = [16.5, 20, -6.5], W = [15.5, 4, -17];
  const arm = bone('arm_' + L, 'chest', X(S));
  bx(arm, 'shoulder', [9.5, 29.5, -15.5], [17.5, 39.5, -4.5], { rot: [10, 0, -12], tone: 1 });
  bx(arm, 'shoulder_wisp', [14, 39, -15], [14, 43, -6], { rot: [10, 0, -12], origin: [13.5, 34.5, -10], mat: 'mane' });
  sg(arm, 'upper_arm', [13.5, 32, -9.5], E, 7, 8);
  const fore = bone('forearm_' + L, 'arm_' + L, X(E));
  sg(fore, 'forearm', E, W, 6, 7);
  sg(fore, 'forearm_cuff', lerp(E, W, 0.42), lerp(E, W, 0.92), 8, 9, { extra: 0, tone: 1 });
  bx(fore, 'elbow_tuft', [17, 17, -4], [17, 24, 1], { rot: [-25, 0, 0], mat: 'mane' });
  const hand = bone('hand_' + L, 'forearm_' + L, X(W));
  bx(hand, 'hand', [11.5, 0, -23], [19.5, 4, -15], { mat: 'paw' });
  for (const [i, x] of [12.5, 14.5, 16.5, 18.5].entries()) {
    bx(hand, 'claw_' + (i + 1), [x - 0.5, 0.5, -27], [x + 0.5, 2.5, -23], { rot: [-16, 0, 0], origin: [x, 2, -23], mat: 'claw' });
  }
  bx(hand, 'claw_thumb', [10.5, 0.5, -24], [11.5, 2.5, -21], { rot: [-16, 30, 0], origin: [11.5, 2, -21], mat: 'claw' });

  // hind leg: hip H, knee K, hock A, ball of the foot B
  const H = [6.5, 25, 15], K = [8.5, 13, 8.5], A = [8, 7, 17.5], B = [8, 1.5, 13.5];
  const leg = bone('leg_' + L, 'body', X(H));
  sg(leg, 'thigh', [7, 27, 16], K, 7, 10);
  const shin = bone('shin_' + L, 'leg_' + L, X(K));
  sg(shin, 'shin', K, A, 5, 6);
  const foot = bone('foot_' + L, 'shin_' + L, X(A));
  sg(foot, 'metatarsus', A, B, 4, 4);
  bx(foot, 'toes', [5.5, 0, 7], [10.5, 2, 14], { mat: 'paw' });
  for (const [i, x] of [6, 8, 10].entries()) {
    bx(foot, 'toe_claw_' + (i + 1), [x - 0.5, 0, 5], [x + 0.5, 1, 7], { mat: 'claw' });
  }
}
side(1);
side(-1);

// a short smoke wisp where the show's Beast has no visible tail
const tail = bone('wisp_tail', 'body', [0, 27, 19]);
box(tail, 'wisp_tail', [0, 22, 18], [0, 30, 27], { rot: [-18, 0, 0], origin: [0, 27, 19], mat: 'mane' });

// ---------------------------------------------------------------- box-UV packing (shelves, tallest first)
const all = bones.flatMap(b => b.cubes);
for (const c of all) {
  const [w, h, d] = c.to.map((v, i) => v - c.from[i]);
  c.size = [w, h, d];
  c.uvw = 2 * d + 2 * w;
  c.uvh = d + h;
}
let x = 0, y = 0, rowH = 0;
for (const c of [...all].sort((a, b) => b.uvh - a.uvh || b.uvw - a.uvw)) {
  if (x + c.uvw > TEX_W) { x = 0; y += rowH + 1; rowH = 0; }
  c.uv = [x, y];
  x += c.uvw + 1;
  rowH = Math.max(rowH, c.uvh);
}
const used = y + rowH;
const TEX_H = used <= 128 ? 128 : 256;
if (used > TEX_H) throw new Error('texture overflow: ' + used);

const out = { name: 'beast_gevaudan_fable', texture: [TEX_W, TEX_H], bones };
fs.writeFileSync(process.argv[2], JSON.stringify(out));
console.log(`bones ${bones.length}, cubes ${all.length}, uv rows used ${used}/${TEX_H}`);
