(function () {
  // Beast of Gevaudan model spec. Coordinates are Blockbench/Bedrock absolute units, y up, front = -z, left = +x.
  // Bones suffixed _r are authored on +x; the builder mirrors them to _l (shared, mirrored UVs).
  const bones = [];
  const b = (name, parent, pivot, rot, cubes, mirror) =>
    bones.push({ name, parent, pivot, rot: rot || [0, 0, 0], cubes: cubes || [], mirror: !!mirror });
  const c = (name, from, size, mat, o) => Object.assign({ name, from, size, mat }, o || {});

  b('root', null, [0, 0, 0]);

  b('body', 'root', [0, 30, 12], null, [
    c('rump', [-9, 20, 3], [18, 17, 17], 'fur', { belly: [34, 22], vein: 0.5 }),
    c('rump_ridge', [-6, 37, 5], [12, 2, 12], 'fur'),
  ]);
  b('spike_back_1', 'body', [0, 37, 8], [38, 0, 0], [c('spike_back_1', [-1.5, 37, 7], [3, 7, 3], 'tuft')]);
  b('spike_back_2', 'body', [0, 37, 13], [42, 0, 0], [c('spike_back_2', [-1.5, 37, 12], [3, 6, 3], 'tuft')]);
  b('spike_back_3', 'body', [0, 37, 18], [46, 0, 0], [c('spike_back_3', [-1.5, 36, 17], [3, 5, 3], 'tuft')]);

  b('tail_1', 'body', [0, 34, 19], [32, 0, 0], [c('tail_1', [-3.5, 28.5, 19], [7, 7, 8], 'fur')]);
  b('tail_2', 'tail_1', [0, 32, 27], [14, 0, 0], [c('tail_2', [-3, 29, 27], [6, 6, 7], 'fur')]);
  b('tail_3', 'tail_2', [0, 32, 35], [14, 0, 0], [
    c('tail_3', [-2.5, 29.5, 34], [5, 5, 6], 'fur'),
    c('tail_tuft', [-3, 27, 39], [6, 8, 5], 'tuft'),
  ]);

  const HY = 32, L1 = 16, L2 = 15, L3 = 5;
  const KY = HY - L1, AY = KY - L2, PY = AY - L3, PZ = 11.4;
  b('leg_r_upper', 'body', [9, HY, 14], [34, 0, 0], [
    c('thigh_r', [4, KY, 8], [10, L1 + 1, 12], 'fur', { vein: 0.4 }),
    c('thigh_fringe_r', [5, KY + 3, 19.5], [8, 9, 2.5], 'tuft'),
  ], true);
  b('leg_r_lower', 'leg_r_upper', [9, KY, 14], [-68, 0, 0], [c('shin_r', [5, AY, 11], [8, L2 + 1, 7], 'fur')], true);
  b('foot_r', 'leg_r_lower', [9, AY, 14], [58, 0, 0], [c('hock_r', [5.5, PY, 10.5], [7, L3 + 1, 7], 'fur')], true);
  b('paw_r', 'foot_r', [9, PY, PZ], [-24, 0, 0], [
    c('paw_r', [4.5, PY - 2, PZ - 9], [9, 3, 12], 'skin'),
    c('toe_claw_r_1', [4.7, PY - 2, PZ - 12.5], [1.8, 1.8, 3.5], 'bone'),
    c('toe_claw_r_2', [7.1, PY - 2, PZ - 12.5], [1.8, 1.8, 3.5], 'bone'),
    c('toe_claw_r_3', [9.5, PY - 2, PZ - 12.5], [1.8, 1.8, 3.5], 'bone'),
  ], true);

  b('chest', 'body', [0, 36, 3], null, [
    c('chest_main', [-13, 22, -22], [26, 22, 25], 'fur', { belly: [38, 24], vein: 0.8 }),
    c('shoulder_hump', [-12, 44, -20], [24, 8, 17], 'fur', { vein: 0.3 }),
    c('keel', [-9, 17, -18], [18, 6, 18], 'belly'),
    c('belly_fringe', [-7, 14.5, -14], [14, 3, 14], 'tuft'),
    c('flank_fur_r', [13, 27, -16], [2, 12, 14], 'tuft'),
    c('flank_fur_l', [-15, 27, -16], [2, 12, 14], 'tuft'),
  ]);
  b('spike_shoulder_1', 'chest', [0, 52, -16], [30, 0, 0], [c('spike_shoulder_1', [-2, 51, -17], [4, 7, 3], 'tuft')]);
  b('spike_shoulder_2', 'chest', [0, 52, -10], [34, 0, 0], [c('spike_shoulder_2', [-2, 51, -11], [4, 8, 3], 'tuft')]);
  b('spike_shoulder_3', 'chest', [0, 52, -4], [38, 0, 0], [c('spike_shoulder_3', [-2, 51, -5], [4, 7, 3], 'tuft')]);

  b('neck', 'chest', [0, 44, -18], [-19, 0, 0], [
    c('neck_main', [-8, 34, -30], [16, 16, 13], 'fur', { belly: [44, 34], vein: 0.6 }),
    c('throat', [-6, 29, -29], [12, 6, 10], 'belly'),
  ]);
  b('mane_1', 'neck', [0, 50, -27], [30, 0, 0], [c('mane_1', [-3, 49, -28], [6, 6, 4], 'tuft')]);
  b('mane_2', 'neck', [0, 50, -22], [32, 0, 0], [c('mane_2', [-3, 49, -23], [6, 7, 4], 'tuft')]);
  b('mane_3', 'neck', [0, 50, -17], [34, 0, 0], [c('mane_3', [-3.5, 49, -18], [7, 8, 4], 'tuft')]);

  const hu = [2.6, 1.8, 3.2, 2.2, 3.4, 2, 2.8, 1.8, 3, 2.2, 2.8, 1.8], hl = [2, 3, 2.2, 3.4, 2.4, 3.2, 2, 2.8, 2.2, 3, 2, 2.6];
  const teethUp = [], teethLow = [];
  for (let i = 0; i < 10; i++) {
    teethUp.push(c('teeth_up_' + (i + 1), [-6.4 + i * 1.4, 33.2 - hu[i], -54.4 + (i % 2) * 0.3], [1.1, hu[i], 1.4], 'bone'));
    teethLow.push(c('teeth_low_' + (i + 1), [-6 + i * 1.3, 33.6, -53.4 + (i % 2) * 0.3], [1.1, hl[i], 1.2], 'bone'));
  }
  b('head', 'neck', [0, 41, -31], [9, 0, 0], [
    c('cranium', [-9, 33, -46], [18, 15, 15], 'fur', { vein: 0.8 }),
    c('brow', [-9.5, 45, -47.5], [19, 3, 4], 'fur'),
    c('muzzle', [-7, 33, -55], [14, 6, 10], 'skinfur', { vein: 0.5 }),
    c('nose', [-2.5, 36.5, -56.5], [5, 3, 2], 'skin'),
    c('cheek_r', [9, 34, -45], [2, 9, 10], 'tuft'),
    c('cheek_l', [-11, 34, -45], [2, 9, 10], 'tuft'),
    ...teethUp,
    c('fang_up_r', [5.6, 29.6, -53.4], [1.8, 3.6, 1.8], 'bone'),
    c('fang_up_l', [-7.4, 29.6, -53.4], [1.8, 3.6, 1.8], 'bone'),
    c('teeth_side_r', [5.8, 31.4, -50], [1.4, 1.8, 3], 'bone'),
    c('teeth_side_l', [-7.2, 31.4, -50], [1.4, 1.8, 3], 'bone'),
    c('upper_gum', [-6.2, 32, -54], [12.4, 1.2, 10], 'mouth'),
  ]);
  b('ear_r', 'head', [6.5, 47, -37], [0, 0, -16], [c('ear_r', [4, 47, -38.8], [5.5, 4, 3.6], 'ear'), c('ear_tip_r', [4.8, 51, -38.4], [3.8, 4, 2.8], 'ear')], true);

  b('jaw', 'head', [0, 36, -44], null, [
    c('jaw_main', [-6.5, 29, -52.5], [13, 4, 10.5], 'skinfur'),
    c('chin_fur', [-4, 27, -51], [8, 2.5, 7], 'tuft'),
    c('jaw_floor', [-5.9, 32.6, -52], [11.8, 1, 9.5], 'mouth'),
    ...teethLow,
    c('fang_low_r', [4.4, 34, -52], [1.6, 4, 1.6], 'bone'),
    c('fang_low_l', [-6, 34, -52], [1.6, 4, 1.6], 'bone'),
    c('tongue', [-2.5, 34.4, -50], [5, 0.8, 7], 'mouth'),
  ]);

  b('arm_r_upper', 'chest', [14, 43, -11], null, [
    c('shoulder_r', [9, 25, -18], [11, 19, 13], 'fur', { vein: 1.0 }),
    c('shoulder_fringe_r', [10, 17, -6.5], [9, 14, 2.5], 'tuft'),
  ], true);
  b('arm_r_lower', 'arm_r_upper', [14, 27, -11], null, [c('forearm_r', [10.5, 10, -15], [7, 17, 8], 'fur', { vein: 1.0 }), c('forearm_fringe_r', [11, 12, -7.5], [6, 13, 2.5], 'tuft')], true);
  b('hand_r', 'arm_r_lower', [14, 11, -11], null, [
    c('hand_r', [10, 0, -19], [9, 10, 13], 'skinfur', { vein: 0.4 }),
    c('claw_r_1', [10.4, 0, -24], [1.8, 2.4, 5.5], 'bone'),
    c('claw_r_2', [12.6, 0, -25], [1.8, 2.4, 6.5], 'bone'),
    c('claw_r_3', [14.8, 0, -25], [1.8, 2.4, 6.5], 'bone'),
    c('claw_r_4', [17, 0, -24], [1.8, 2.4, 5.5], 'bone'),
  ], true);

  return bones;
})()