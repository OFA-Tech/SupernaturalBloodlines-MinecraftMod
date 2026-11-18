// risky_eval: (0,eval)(anims.js)(cfg). cfg = {name}. Rebuilds every animation of the named GeckoLib project.
// Rotations are degrees added to the bone's rest rotation (Blockbench convention: +x tips a bone's far end toward -z, i.e. forward/up
// for things hanging down, nose-up for things pointing forward); positions are model units. Keys are [time, [x,y,z], easing?, easingArg?].
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('project not found: ' + cfg.name);
  proj.select();
  Animation.all.slice().forEach(a => a.remove(false, false));

  const PI2 = Math.PI * 2, C = Math.cos, S = Math.sin, r2 = v => Math.round(v * 100) / 100;
  const ID = 'animation.beast_of_gevaudan.';
  const defs = [];
  const mk = (name, length, loop, fn) => defs.push({ name, length, loop, fn });
  const mir = v => [v[0], -v[1], -v[2]];

  const wave = (L, n, f) => {
    const out = [];
    for (let i = 0; i <= n; i++) out.push([r2(L * i / n), f(PI2 * i / n, i / n).map(r2)]);
    return out;
  };
  const pos = (v) => v;

  mk('idle', 4, 'loop', (T) => {
    const L = 4;
    T('chest', 'scale', wave(L, 8, p => [1 + 0.012 * S(p), 1 + 0.03 * S(p), 1 + 0.02 * S(p)]));
    T('body', 'rotation', wave(L, 8, p => [0.8 * S(p), 0, 0.5 * S(p + 1)]));
    T('body', 'position', wave(L, 8, p => [0, 0.35 * S(p), 0]));
    T('neck', 'rotation', wave(L, 8, p => [-1.6 * S(p) + 1, 3 * S(p * 1 + 0.5), 0]));
    T('head', 'rotation', wave(L, 8, p => [1.8 * S(p + 1.2), 5 * S(p + 2.2), 2 * S(p)]));
    T('jaw', 'rotation', wave(L, 8, p => [-(9 + 2.5 * S(p + 1.6)), 0, 0]));
    T('ear_l', 'rotation', [[0, [0, 0, 0]], [2.2, [0, 0, 0]], [2.4, [-8, 0, -6], 'easeOutQuad'], [2.6, [0, 0, 0], 'easeInOutSine'], [4, [0, 0, 0]]]);
    T('ear_r', 'rotation', [[0, [0, 0, 0]], [3.1, [0, 0, 0]], [3.3, [-8, 0, 6], 'easeOutQuad'], [3.5, [0, 0, 0], 'easeInOutSine'], [4, [0, 0, 0]]]);
    T('arm_l_upper', 'rotation', wave(L, 8, p => [1.5 * S(p), 0, 0]));
    T('arm_r_upper', 'rotation', wave(L, 8, p => [-1.5 * S(p), 0, 0]));
    T('tail_1', 'rotation', wave(L, 8, p => [0, 7 * S(p), 0]));
    T('tail_2', 'rotation', wave(L, 8, p => [0, 9 * S(p - 0.6), 0]));
    T('tail_3', 'rotation', wave(L, 8, p => [0, 11 * S(p - 1.2), 0]));
    ['mane_1', 'mane_2', 'mane_3', 'spike_shoulder_1', 'spike_shoulder_2', 'spike_shoulder_3', 'spike_back_1', 'spike_back_2', 'spike_back_3']
      .forEach((b, i) => T(b, 'rotation', wave(L, 8, p => [4 * S(p + i * 0.7), 0, 0])));
  });

  const gait = (L, A, mode) => (T) => {
    const bound = mode === 'run';
    const ph = (side) => bound ? 0 : (side === 'l' ? 0 : Math.PI);
    ['l', 'r'].forEach(side => {
      const f = side === 'l' ? 1 : 1;
      const pf = ph(side), pb = bound ? Math.PI * 0.85 : (side === 'l' ? Math.PI : 0);
      T('arm_' + side + '_upper', 'rotation', wave(L, 12, p => [A * 20 * C(p + pf), 0, 0]));
      T('arm_' + side + '_lower', 'rotation', wave(L, 12, p => [-(A * 4 + A * 30 * Math.max(0, -S(p + pf))), 0, 0]));
      T('hand_' + side, 'rotation', wave(L, 12, p => [A * 4 + A * 20 * Math.max(0, -S(p + pf)) - A * 8 * C(p + pf), 0, 0]));
      T('leg_' + side + '_upper', 'rotation', wave(L, 12, p => [A * 15 * C(p + pb), 0, 0]));
      T('leg_' + side + '_lower', 'rotation', wave(L, 12, p => [-A * 24 * Math.max(0, -S(p + pb)) + A * 6 * C(p + pb), 0, 0]));
      T('foot_' + side, 'rotation', wave(L, 12, p => [A * 14 * Math.max(0, -S(p + pb)) - A * 6 * C(p + pb), 0, 0]));
      T('paw_' + side, 'rotation', wave(L, 12, p => [-A * 6 * Math.max(0, -S(p + pb)) + A * 10 * Math.max(0, S(p + pb)), 0, 0]));
    });
    T('root', 'position', wave(L, 12, p => [0, bound ? 2.6 * (0.5 + 0.5 * S(p)) - 1 : 0.9 * C(2 * p), 0]));
    T('body', 'rotation', wave(L, 12, p => [bound ? 5 * S(p + 0.4) : 0.8 * S(2 * p), 0, bound ? 0 : 1.8 * C(p)]));
    T('chest', 'rotation', wave(L, 12, p => [bound ? -4 * S(p + 0.4) : 0, bound ? 0 : -4 * C(p), bound ? 0 : -1.5 * C(p)]));
    T('neck', 'rotation', wave(L, 12, p => [bound ? -6 + 3 * S(p + 1) : 0, bound ? 0 : 3 * C(p), 0]));
    T('head', 'rotation', wave(L, 12, p => [bound ? 2 * S(p + 2) : 1.2 * S(2 * p), 0, 0]));
    T('jaw', 'rotation', wave(L, 12, p => [bound ? -(14 + 4 * S(p)) : -(7 + S(2 * p)), 0, 0]));
    T('tail_1', 'rotation', wave(L, 12, p => [bound ? -14 : 0, (bound ? 4 : 11) * S(p - 0.3), 0]));
    T('tail_2', 'rotation', wave(L, 12, p => [bound ? -12 : 0, (bound ? 6 : 12) * S(p - 0.9), 0]));
    T('tail_3', 'rotation', wave(L, 12, p => [bound ? -8 : 0, (bound ? 8 : 14) * S(p - 1.5), 0]));
    ['mane_1', 'mane_2', 'mane_3', 'spike_shoulder_1', 'spike_shoulder_2', 'spike_shoulder_3', 'spike_back_1', 'spike_back_2', 'spike_back_3']
      .forEach((b, i) => T(b, 'rotation', wave(L, 12, p => [(bound ? 9 : 4) * S(2 * p + i * 0.6) + (bound ? 14 : 0), 0, 0])));
  };
  mk('walk', 1.2, 'loop', gait(1.2, 1, 'walk'));
  mk('run', 0.7, 'loop', gait(0.7, 1.7, 'run'));

  mk('attack_bite', 1, 'once', (T) => {
    const E = 'easeInOutSine';
    T('root', 'position', [[0, [0, 0, 0]], [0.32, [0, -3, 4], 'easeOutQuad'], [0.46, [0, -1, -12], 'easeInExpo'], [0.62, [0, -1, -12]], [1, [0, 0, 0], E]]);
    T('body', 'rotation', [[0, [0, 0, 0]], [0.32, [-4, 0, 0], 'easeOutQuad'], [0.46, [5, 0, 0], 'easeInQuad'], [0.62, [5, 0, 0]], [1, [0, 0, 0], E]]);
    T('chest', 'rotation', [[0, [0, 0, 0]], [0.32, [6, 0, 0], 'easeOutQuad'], [0.46, [-8, 0, 0], 'easeInQuad'], [0.62, [-8, 0, 0]], [1, [0, 0, 0], E]]);
    T('neck', 'rotation', [[0, [0, 0, 0]], [0.32, [22, 0, 0], 'easeOutQuad'], [0.46, [-30, 0, 0], 'easeInQuad'], [0.62, [-30, 0, 0]], [1, [0, 0, 0], E]]);
    T('head', 'rotation', [[0, [0, 0, 0]], [0.32, [16, 0, 0], 'easeOutQuad'], [0.46, [-10, 0, 0], 'easeInQuad'], [0.62, [-10, 4, 0]], [1, [0, 0, 0], E]]);
    T('jaw', 'rotation', [[0, [-3, 0, 0]], [0.32, [-52, 0, 0], 'easeOutBack', 0.6], [0.46, [-60, 0, 0], 'easeInQuad'], [0.52, [-2, 0, 0], 'easeInExpo'], [0.64, [-2, 0, 0]], [1, [-3, 0, 0], E]]);
    T('arm_l_upper', 'rotation', [[0, [0, 0, 0]], [0.32, [-24, 0, -6], 'easeOutQuad'], [0.46, [30, 0, 2], 'easeInQuad'], [0.62, [30, 0, 0]], [1, [0, 0, 0], E]]);
    T('arm_r_upper', 'rotation', [[0, [0, 0, 0]], [0.32, [-24, 0, 6], 'easeOutQuad'], [0.46, [30, 0, -2], 'easeInQuad'], [0.62, [30, 0, 0]], [1, [0, 0, 0], E]]);
    T('arm_l_lower', 'rotation', [[0, [0, 0, 0]], [0.32, [-18, 0, 0], 'easeOutQuad'], [0.46, [-6, 0, 0], 'easeInQuad'], [1, [0, 0, 0], E]]);
    T('arm_r_lower', 'rotation', [[0, [0, 0, 0]], [0.32, [-18, 0, 0], 'easeOutQuad'], [0.46, [-6, 0, 0], 'easeInQuad'], [1, [0, 0, 0], E]]);
    ['l', 'r'].forEach(s => {
      T('leg_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.32, [14, 0, 0], 'easeOutQuad'], [0.46, [-8, 0, 0], 'easeInQuad'], [1, [0, 0, 0], E]]);
      T('leg_' + s + '_lower', 'rotation', [[0, [0, 0, 0]], [0.32, [-12, 0, 0], 'easeOutQuad'], [0.46, [8, 0, 0], 'easeInQuad'], [1, [0, 0, 0], E]]);
    });
    T('tail_1', 'rotation', [[0, [0, 0, 0]], [0.32, [-6, 0, 0], 'easeOutQuad'], [0.46, [8, 0, 0]], [1, [0, 0, 0], E]]);
    T('ear_l', 'rotation', [[0, [0, 0, 0]], [0.32, [0, 0, -8], 'easeOutQuad'], [0.46, [18, 0, -8]], [1, [0, 0, 0], E]]);
    T('ear_r', 'rotation', [[0, [0, 0, 0]], [0.32, [0, 0, 8], 'easeOutQuad'], [0.46, [18, 0, 8]], [1, [0, 0, 0], E]]);
  });

  mk('attack_swipe', 1.1, 'once', (T) => {
    const E = 'easeInOutSine';
    T('root', 'position', [[0, [0, 0, 0]], [0.4, [3, -3, 3], 'easeOutQuad'], [0.58, [-4, -1, -8], 'easeInQuad'], [0.76, [-4, -1, -8]], [1.1, [0, 0, 0], E]]);
    T('chest', 'rotation', [[0, [0, 0, 0]], [0.4, [4, 16, 0], 'easeOutQuad'], [0.58, [-6, -26, 0], 'easeInCubic'], [0.76, [-6, -26, 0]], [1.1, [0, 0, 0], E]]);
    T('body', 'rotation', [[0, [0, 0, 0]], [0.4, [0, 6, -2], 'easeOutQuad'], [0.58, [0, -10, 2], 'easeInCubic'], [0.76, [0, -10, 2]], [1.1, [0, 0, 0], E]]);
    T('neck', 'rotation', [[0, [0, 0, 0]], [0.4, [10, 12, 0], 'easeOutQuad'], [0.58, [-14, -22, 0], 'easeInQuad'], [0.76, [-14, -22, 0]], [1.1, [0, 0, 0], E]]);
    T('head', 'rotation', [[0, [0, 0, 0]], [0.4, [8, 4, 0], 'easeOutQuad'], [0.58, [-6, -10, 0]], [1.1, [0, 0, 0], E]]);
    T('jaw', 'rotation', [[0, [-3, 0, 0]], [0.4, [-20, 0, 0], 'easeOutQuad'], [0.58, [-36, 0, 0], 'easeInQuad'], [0.8, [-30, 0, 0]], [1.1, [-3, 0, 0], E]]);
    T('arm_l_upper', 'rotation', [[0, [0, 0, 0]], [0.4, [-64, 0, -34], 'easeOutQuad'], [0.58, [52, 0, 34], 'easeInCubic'], [0.76, [52, 0, 34]], [1.1, [0, 0, 0], E]]);
    T('arm_l_lower', 'rotation', [[0, [0, 0, 0]], [0.4, [-52, 0, 0], 'easeOutQuad'], [0.58, [10, 0, 0], 'easeInCubic'], [1.1, [0, 0, 0], E]]);
    T('hand_l', 'rotation', [[0, [0, 0, 0]], [0.4, [20, 0, 0], 'easeOutQuad'], [0.58, [-18, 0, 0], 'easeInCubic'], [1.1, [0, 0, 0], E]]);
    T('arm_r_upper', 'rotation', [[0, [0, 0, 0]], [0.4, [10, 0, 6], 'easeOutQuad'], [0.58, [-8, 0, -4]], [1.1, [0, 0, 0], E]]);
    ['l', 'r'].forEach(s => {
      T('leg_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.4, [10, 0, 0], 'easeOutQuad'], [0.58, [-6, 0, 0]], [1.1, [0, 0, 0], E]]);
    });
    T('tail_1', 'rotation', [[0, [0, 0, 0]], [0.4, [0, -14, 0], 'easeOutQuad'], [0.58, [0, 22, 0]], [1.1, [0, 0, 0], E]]);
    T('tail_2', 'rotation', [[0, [0, 0, 0]], [0.45, [0, -10, 0], 'easeOutQuad'], [0.68, [0, 24, 0]], [1.1, [0, 0, 0], E]]);
  });

  mk('roar', 2.6, 'once', (T) => {
    const E = 'easeInOutSine';
    const shake = (amp, t0, t1, n) => {
      const out = [];
      for (let i = 0; i <= n; i++) { const t = t0 + (t1 - t0) * i / n; out.push([r2(t), [r2(amp * (i % 2 ? 1 : -1) * 0.5), r2(amp * (i % 2 ? -1 : 1)), 0]]); }
      return out;
    };
    T('root', 'position', [[0, [0, 0, 0]], [0.6, [0, -2, 2], 'easeOutQuad'], [0.9, [0, 3, 0], 'easeOutBack', 0.8], [1.9, [0, 3, 0]], [2.6, [0, 0, 0], E]]);
    T('body', 'rotation', [[0, [0, 0, 0]], [0.6, [-3, 0, 0], 'easeOutQuad'], [0.9, [12, 0, 0], 'easeOutBack', 0.7], [1.9, [12, 0, 0]], [2.6, [0, 0, 0], E]]);
    T('chest', 'rotation', [[0, [0, 0, 0]], [0.6, [-4, 0, 0], 'easeOutQuad'], [0.9, [16, 0, 0], 'easeOutBack', 0.7], [1.9, [16, 0, 0]], [2.6, [0, 0, 0], E]]);
    T('chest', 'scale', [[0, [1, 1, 1]], [0.6, [0.98, 0.97, 0.98], E], [0.95, [1.07, 1.08, 1.06], 'easeOutQuad'], [1.9, [1.07, 1.08, 1.06]], [2.6, [1, 1, 1], E]]);
    T('neck', 'rotation', [[0, [0, 0, 0]], [0.6, [-6, 0, 0], 'easeOutQuad'], [0.9, [34, 0, 0], 'easeOutBack', 0.7], [1.9, [34, 0, 0]], [2.6, [0, 0, 0], E]]);
    T('head', 'rotation', [[0, [0, 0, 0]], [0.6, [-6, 0, 0], 'easeOutQuad'], [0.9, [18, 0, 0], 'easeOutBack', 0.7], ...shake(2.2, 1.0, 1.8, 16).map(k => [k[0], [18 + k[1][0], k[1][1] * 0.3, k[1][1]]]), [1.9, [18, 0, 0]], [2.6, [0, 0, 0], E]]);
    T('jaw', 'rotation', [[0, [-3, 0, 0]], [0.6, [-6, 0, 0], 'easeOutQuad'], [0.9, [-58, 0, 0], 'easeOutBack', 0.6], ...shake(3, 1.0, 1.8, 16).map(k => [k[0], [-58 + k[1][0] * 2, 0, 0]]), [1.9, [-58, 0, 0]], [2.6, [-3, 0, 0], E]]);
    ['l', 'r'].forEach(s => {
      const k = s === 'l' ? -1 : 1;
      T('arm_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.6, [-10, 0, 0], 'easeOutQuad'], [0.9, [-24, 0, 18 * k], 'easeOutBack', 0.7], [1.9, [-24, 0, 18 * k]], [2.6, [0, 0, 0], E]]);
      T('arm_' + s + '_lower', 'rotation', [[0, [0, 0, 0]], [0.9, [-14, 0, 0], E], [1.9, [-14, 0, 0]], [2.6, [0, 0, 0], E]]);
      T('leg_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.6, [8, 0, 0], 'easeOutQuad'], [0.9, [-4, 0, 0], 'easeOutBack', 0.7], [1.9, [-4, 0, 0]], [2.6, [0, 0, 0], E]]);
      T('ear_' + s, 'rotation', [[0, [0, 0, 0]], [0.9, [-14, 0, -12 * k], 'easeOutQuad'], [1.9, [-14, 0, -12 * k]], [2.6, [0, 0, 0], E]]);
    });
    T('tail_1', 'rotation', [[0, [0, 0, 0]], [0.9, [-16, 0, 0], E], [1.9, [-16, 0, 0]], [2.6, [0, 0, 0], E]]);
    ['mane_1', 'mane_2', 'mane_3', 'spike_shoulder_1', 'spike_shoulder_2', 'spike_shoulder_3', 'spike_back_1', 'spike_back_2', 'spike_back_3']
      .forEach((b, i) => {
        T(b, 'rotation', [[0, [0, 0, 0]], [0.9, [-18, 0, 0], 'easeOutQuad'], [1.9, [-18, 0, 0]], [2.6, [0, 0, 0], E]]);
        T(b, 'scale', [[0, [1, 1, 1]], [0.9, [1.25, 1.45, 1.25], 'easeOutQuad'], [1.9, [1.25, 1.45, 1.25]], [2.6, [1, 1, 1], E]]);
      });
  });

  mk('rear_up', 3.2, 'once', (T) => {
    const E = 'easeInOutSine';
    const t = [0, 0.5, 1.3, 2.1, 2.8, 3.2];
    const seq = (a, b, c, d, e, f, ease) => [[t[0], a], [t[1], b, 'easeOutQuad'], [t[2], c, 'easeOutBack', 0.9], [t[3], d, E], [t[4], e, E], [t[5], f, E]];
    T('root', 'position', seq([0, 0, 0], [0, -3, 4], [0, 3, 8], [0, 4, 8], [0, 2, 3], [0, 0, 0]));
    T('body', 'rotation', seq([0, 0, 0], [-5, 0, 0], [46, 0, 0], [46, 0, 0], [20, 0, 0], [0, 0, 0]));
    T('chest', 'rotation', seq([0, 0, 0], [-6, 0, 0], [26, 0, 0], [26, 0, 0], [12, 0, 0], [0, 0, 0]));
    T('neck', 'rotation', seq([0, 0, 0], [-4, 0, 0], [-30, 0, 0], [-34, 0, 0], [-14, 0, 0], [0, 0, 0]));
    T('head', 'rotation', seq([0, 0, 0], [-6, 0, 0], [-14, 0, 0], [-6, 0, 0], [-4, 0, 0], [0, 0, 0]));
    T('jaw', 'rotation', seq([-3, 0, 0], [-8, 0, 0], [-30, 0, 0], [-8, 0, 0], [-6, 0, 0], [-3, 0, 0]));
    T('leg_l_upper', 'rotation', seq([0, 0, 0], [12, 0, 0], [-68, 0, 0], [-68, 0, 0], [-30, 0, 0], [0, 0, 0]));
    T('leg_r_upper', 'rotation', seq([0, 0, 0], [12, 0, 0], [-68, 0, 0], [-68, 0, 0], [-30, 0, 0], [0, 0, 0]));
    T('leg_l_lower', 'rotation', seq([0, 0, 0], [-10, 0, 0], [26, 0, 0], [26, 0, 0], [12, 0, 0], [0, 0, 0]));
    T('leg_r_lower', 'rotation', seq([0, 0, 0], [-10, 0, 0], [26, 0, 0], [26, 0, 0], [12, 0, 0], [0, 0, 0]));
    T('foot_l', 'rotation', seq([0, 0, 0], [0, 0, 0], [-18, 0, 0], [-18, 0, 0], [-8, 0, 0], [0, 0, 0]));
    T('foot_r', 'rotation', seq([0, 0, 0], [0, 0, 0], [-18, 0, 0], [-18, 0, 0], [-8, 0, 0], [0, 0, 0]));
    T('paw_l', 'rotation', seq([0, 0, 0], [0, 0, 0], [10, 0, 0], [10, 0, 0], [4, 0, 0], [0, 0, 0]));
    T('paw_r', 'rotation', seq([0, 0, 0], [0, 0, 0], [10, 0, 0], [10, 0, 0], [4, 0, 0], [0, 0, 0]));
    ['l', 'r'].forEach(s => {
      const k = s === 'l' ? -1 : 1;
      T('arm_' + s + '_upper', 'rotation', seq([0, 0, 0], [-14, 0, 0], [-66, 0, 24 * k], [-40, 0, 40 * k], [-30, 0, 8 * k], [0, 0, 0]));
      T('arm_' + s + '_lower', 'rotation', seq([0, 0, 0], [-16, 0, 0], [-30, 0, 0], [-22, 0, 0], [-14, 0, 0], [0, 0, 0]));
      T('hand_' + s, 'rotation', seq([0, 0, 0], [8, 0, 0], [26, 0, 0], [18, 0, 0], [10, 0, 0], [0, 0, 0]));
      T('ear_' + s, 'rotation', seq([0, 0, 0], [0, 0, 0], [-10, 0, -10 * k], [-10, 0, -10 * k], [0, 0, 0], [0, 0, 0]));
    });
    T('tail_1', 'rotation', seq([0, 0, 0], [-6, 0, 0], [-30, 0, 0], [-30, 0, 0], [-10, 0, 0], [0, 0, 0]));
    T('tail_2', 'rotation', seq([0, 0, 0], [0, 0, 0], [-14, 0, 0], [-14, 0, 0], [-6, 0, 0], [0, 0, 0]));
    ['mane_1', 'mane_2', 'mane_3', 'spike_shoulder_1', 'spike_shoulder_2', 'spike_shoulder_3'].forEach(b => {
      T(b, 'scale', seq([1, 1, 1], [1, 1, 1], [1.2, 1.35, 1.2], [1.2, 1.35, 1.2], [1.05, 1.1, 1.05], [1, 1, 1]));
    });
  });

  mk('hurt', 0.6, 'once', (T) => {
    const E = 'easeInOutSine';
    T('root', 'position', [[0, [0, 0, 0]], [0.1, [1.5, 0, 4], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
    T('body', 'rotation', [[0, [0, 0, 0]], [0.1, [4, 0, -3], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
    T('chest', 'rotation', [[0, [0, 0, 0]], [0.1, [8, -8, 3], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
    T('neck', 'rotation', [[0, [0, 0, 0]], [0.1, [18, -12, 0], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
    T('head', 'rotation', [[0, [0, 0, 0]], [0.1, [10, -10, 6], 'easeOutQuad'], [0.22, [4, 8, -5], E], [0.6, [0, 0, 0], E]]);
    T('jaw', 'rotation', [[0, [-3, 0, 0]], [0.1, [-34, 0, 0], 'easeOutQuad'], [0.6, [-3, 0, 0], E]]);
    ['l', 'r'].forEach(s => {
      T('arm_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.1, [-16, 0, s === 'l' ? -10 : 10], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
      T('leg_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.1, [-8, 0, 0], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
      T('ear_' + s, 'rotation', [[0, [0, 0, 0]], [0.1, [-16, 0, s === 'l' ? -14 : 14], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
    });
    T('tail_1', 'rotation', [[0, [0, 0, 0]], [0.1, [-12, 10, 0], 'easeOutQuad'], [0.6, [0, 0, 0], E]]);
  });

  mk('death', 3, 'hold', (T) => {
    const E = 'easeInOutSine';
    T('root', 'position', [[0, [0, 0, 0]], [0.5, [0, 1, 3], 'easeOutQuad'], [1.3, [0, -13, 4], 'easeInQuad'], [1.9, [14, -11, 6], 'easeInOutCubic'], [2.2, [16, -13, 6], 'easeOutBounce', 0.35], [3, [16, -13, 6]]]);
    T('root', 'rotation', [[0, [0, 0, 0]], [1.3, [0, 0, 0]], [1.9, [0, 0, -52], 'easeInOutCubic'], [2.2, [0, 0, -78], 'easeOutBounce', 0.35], [3, [0, 0, -78]]]);
    T('body', 'rotation', [[0, [0, 0, 0]], [0.5, [10, 0, 0], 'easeOutQuad'], [1.3, [-8, 0, 0], 'easeInQuad'], [2.2, [-6, 8, 0], E], [3, [-6, 8, 0]]]);
    T('chest', 'rotation', [[0, [0, 0, 0]], [0.5, [18, 0, 0], 'easeOutQuad'], [1.3, [-12, 0, 0], 'easeInQuad'], [2.2, [-14, 18, 0], E], [3, [-14, 18, 0]]]);
    T('neck', 'rotation', [[0, [0, 0, 0]], [0.5, [34, 0, 0], 'easeOutQuad'], [1.3, [-24, 0, 0], 'easeInQuad'], [2.2, [-18, 24, 0], E], [3, [-18, 24, 0]]]);
    T('head', 'rotation', [[0, [0, 0, 0]], [0.5, [14, 0, 6], 'easeOutQuad'], [1.3, [-10, 0, 0], 'easeInQuad'], [2.2, [-6, 14, 8], E], [3, [-6, 14, 8]]]);
    T('jaw', 'rotation', [[0, [-3, 0, 0]], [0.5, [-46, 0, 0], 'easeOutQuad'], [1.3, [-20, 0, 0], E], [2.2, [-14, 0, 0], E], [3, [-14, 0, 0]]]);
    ['l', 'r'].forEach(s => {
      const k = s === 'l' ? -1 : 1;
      T('arm_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.5, [-30, 0, 10 * k], 'easeOutQuad'], [1.3, [38, 0, 6 * k], 'easeInQuad'], [2.2, [30, 0, 22 * k], E], [3, [30, 0, 22 * k]]]);
      T('arm_' + s + '_lower', 'rotation', [[0, [0, 0, 0]], [0.5, [-20, 0, 0]], [1.3, [-74, 0, 0], 'easeInQuad'], [2.2, [-50, 0, 0], E], [3, [-50, 0, 0]]]);
      T('hand_' + s, 'rotation', [[0, [0, 0, 0]], [1.3, [34, 0, 0], 'easeInQuad'], [2.2, [20, 0, 0], E], [3, [20, 0, 0]]]);
      T('leg_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.5, [-10, 0, 0], 'easeOutQuad'], [1.3, [24, 0, 0], 'easeInQuad'], [2.2, [30, 0, 10 * k], E], [3, [30, 0, 10 * k]]]);
      T('leg_' + s + '_lower', 'rotation', [[0, [0, 0, 0]], [1.3, [-26, 0, 0], 'easeInQuad'], [2.2, [-30, 0, 0], E], [3, [-30, 0, 0]]]);
      T('ear_' + s, 'rotation', [[0, [0, 0, 0]], [0.5, [-12, 0, -14 * k], 'easeOutQuad'], [1.3, [20, 0, 18 * k], E], [3, [24, 0, 24 * k]]]);
    });
    T('tail_1', 'rotation', [[0, [0, 0, 0]], [1.3, [14, 0, 0]], [2.2, [10, 20, 0], E], [3, [10, 20, 0]]]);
    T('tail_2', 'rotation', [[0, [0, 0, 0]], [1.3, [8, 0, 0]], [2.2, [6, 24, 0], E], [3, [6, 24, 0]]]);
    ['mane_1', 'mane_2', 'mane_3', 'spike_shoulder_1', 'spike_shoulder_2', 'spike_shoulder_3', 'spike_back_1', 'spike_back_2', 'spike_back_3'].forEach(b => {
      T(b, 'rotation', [[0, [0, 0, 0]], [0.5, [-12, 0, 0], 'easeOutQuad'], [1.3, [20, 0, 0], E], [3, [30, 0, 0]]]);
    });
  });

  mk('collapse', 3.4, 'hold', (T) => {
    const E = 'easeInOutSine';
    T('root', 'position', [[0, [0, 0, 0]], [0.4, [0, 1, 2], 'easeOutQuad'], [1.2, [0, -11, 5], 'easeInOutCubic'], [1.5, [0, -12, 5], 'easeOutBounce', 0.3], [3.4, [0, -12, 5]]]);
    T('body', 'rotation', [[0, [0, 0, 0]], [0.4, [6, 0, 0], 'easeOutQuad'], [1.2, [-10, 0, 0], 'easeInOutCubic'], [3.4, [-10, 0, 0]]]);
    T('chest', 'rotation', [[0, [0, 0, 0]], [0.4, [12, 0, 0], 'easeOutQuad'], [1.2, [-16, 0, 0], 'easeInOutCubic'], [1.5, [-14, 0, 0]], [2.1, [-17, 0, 0], E], [2.7, [-14, 0, 0], E], [3.4, [-17, 0, 0], E]]);
    T('chest', 'scale', [[0, [1, 1, 1]], [1.5, [1, 1, 1]], [2.1, [1.02, 1.05, 1.03], E], [2.7, [1, 1, 1], E], [3.4, [1.02, 1.05, 1.03], E]]);
    T('neck', 'rotation', [[0, [0, 0, 0]], [0.4, [22, 0, 0], 'easeOutQuad'], [1.2, [-30, 6, 0], 'easeInOutCubic'], [3.4, [-34, 10, 0], E]]);
    T('head', 'rotation', [[0, [0, 0, 0]], [0.4, [10, 0, 0], 'easeOutQuad'], [1.2, [-12, 4, 4], 'easeInOutCubic'], [3.4, [-14, 8, 6], E]]);
    T('jaw', 'rotation', [[0, [-3, 0, 0]], [0.4, [-30, 0, 0], 'easeOutQuad'], [1.2, [-12, 0, 0], E], [1.8, [-20, 0, 0], E], [2.4, [-10, 0, 0], E], [3.0, [-20, 0, 0], E], [3.4, [-14, 0, 0], E]]);
    ['l', 'r'].forEach(s => {
      const k = s === 'l' ? -1 : 1;
      T('arm_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.4, [-24, 0, 6 * k], 'easeOutQuad'], [1.2, [34, 0, 12 * k], 'easeInOutCubic'], [3.4, [34, 0, 12 * k]]]);
      T('arm_' + s + '_lower', 'rotation', [[0, [0, 0, 0]], [0.4, [-14, 0, 0]], [1.2, [-66, 0, 0], 'easeInOutCubic'], [3.4, [-66, 0, 0]]]);
      T('hand_' + s, 'rotation', [[0, [0, 0, 0]], [1.2, [30, 0, 0], 'easeInOutCubic'], [3.4, [30, 0, 0]]]);
      T('leg_' + s + '_upper', 'rotation', [[0, [0, 0, 0]], [0.4, [-8, 0, 0], 'easeOutQuad'], [1.2, [22, 0, 0], 'easeInOutCubic'], [3.4, [22, 0, 0]]]);
      T('leg_' + s + '_lower', 'rotation', [[0, [0, 0, 0]], [1.2, [-22, 0, 0], 'easeInOutCubic'], [3.4, [-22, 0, 0]]]);
      T('ear_' + s, 'rotation', [[0, [0, 0, 0]], [0.4, [-10, 0, -10 * k]], [1.2, [14, 0, 16 * k], E], [3.4, [18, 0, 20 * k]]]);
    });
    T('tail_1', 'rotation', [[0, [0, 0, 0]], [1.2, [12, 0, 0], E], [3.4, [12, 0, 0]]]);
    ['mane_1', 'mane_2', 'mane_3', 'spike_shoulder_1', 'spike_shoulder_2', 'spike_shoulder_3'].forEach(b => {
      T(b, 'rotation', [[0, [0, 0, 0]], [1.2, [16, 0, 0], E], [3.4, [22, 0, 0]]]);
    });
  });

  const ch = { rotation: 'rotation', position: 'position', scale: 'scale' };
  const out = [];
  for (const d of defs) {
    const anim = new Animation({ name: ID + d.name, loop: d.loop, length: d.length, snapping: 24, anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '' }).add(false);
    let nkf = 0;
    const T = (bone, channel, keys) => {
      const g = Group.all.find(x => x.name === bone);
      if (!g) throw new Error('bone not found: ' + bone + ' (anim ' + d.name + ')');
      const ba = anim.getBoneAnimator(g);
      keys.forEach(k => {
        const [t, v, ease, arg] = k;
        const o = { channel: ch[channel], time: t, interpolation: 'linear', data_points: [{ x: v[0], y: v[1], z: v[2] }] };
        const kf = ba.addKeyframe(o);
        if (ease) { kf.easing = ease; if (arg !== undefined) kf.easingArgs = [arg]; }
        nkf++;
      });
    };
    d.fn(T);
    out.push(d.name + ':' + nkf);
  }
  return out;
})
