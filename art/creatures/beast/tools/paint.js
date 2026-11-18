// risky_eval: (0,eval)(paint.js)(cfg). cfg = {fs, name, specPath, texW, texH, out?}
// Hand-paints every cube net of the named project (fur strands, silver belly, red veins, white-blue eyes, bone teeth and claws)
// from the spec's material tags, so the painting always matches the UV layout built by build.js.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('project not found: ' + cfg.name);
  proj.select();
  const spec = (0, eval)(cfg.fs.readFileSync(cfg.specPath, 'utf8'));
  const info = {};
  spec.forEach(b => b.cubes.forEach(q => { info[q.name] = q; }));

  const W = cfg.texW, H = cfg.texH;
  const cv = document.createElement('canvas'); cv.width = W; cv.height = H;
  const ctx = cv.getContext('2d');
  const img = ctx.createImageData(W, H);
  const px = img.data;

  const hex = s => [parseInt(s.slice(1, 3), 16), parseInt(s.slice(3, 5), 16), parseInt(s.slice(5, 7), 16)];
  const ramp = a => a.map(hex);
  const RAMP = {
    fur: ramp(['#08080b', '#121116', '#1b1a20', '#272630', '#383845', '#505466', '#6e748a']),
    belly: ramp(['#22242c', '#363944', '#4f5361', '#6b7080', '#8a90a1', '#a9aebd', '#c5c9d4']),
    skin: ramp(['#0d0d10', '#17171b', '#212126', '#2e2e35', '#404048', '#55555f']),
    bone: ramp(['#2f2c25', '#5b564a', '#8a8472', '#b5ae98', '#d8d2bb', '#efe9d4']),
    mouth: ramp(['#0e0307', '#1c0710', '#2e0d19', '#46142a', '#62203a']),
  };
  const lerp = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
  const sample = (r, i) => {
    i = Math.max(0, Math.min(r.length - 1, i));
    const lo = Math.floor(i), hi = Math.min(r.length - 1, lo + 1);
    return lerp(r[lo], r[hi], i - lo);
  };
  const smooth = t => { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); };

  let seed = 1337;
  const rnd = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
  const hash = (a, b, c) => {
    let h = (a * 374761393 + b * 668265263 + (c || 0) * 2246822519) >>> 0;
    h = ((h ^ (h >>> 13)) * 1274126177) >>> 0;
    return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
  };
  const put = (x, y, c, a) => {
    if (x < 0 || y < 0 || x >= W || y >= H) return;
    const o = (y * W + x) * 4;
    a = a === undefined ? 1 : a;
    if (a >= 1 || px[o + 3] === 0) { px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; return; }
    px[o] = px[o] + (c[0] - px[o]) * a; px[o + 1] = px[o + 1] + (c[1] - px[o + 1]) * a; px[o + 2] = px[o + 2] + (c[2] - px[o + 2]) * a;
  };

  const SHADE = { up: 1.6, north: 0.25, south: -0.2, east: -0.1, west: -0.1, down: -1.2 };
  const idxOf = (cube, dir, w, h, u, v) => {
    const f = cube.faces[dir];
    return f;
  };

  function furPixel(m, o, shade, u, v, fid, wy, ramps) {
    const col = hash(Math.floor(u / 2), fid, 7);
    const L = 3 + Math.floor(col * 5);
    const n = hash(Math.floor(u / 2), Math.floor((v + col * L * 3) / L), fid);
    let i = 2.2 + shade * 0.85 + (n - 0.5) * 2.6 + (hash(Math.floor(u / 3), Math.floor(v / 4), fid + 3) - 0.5) * 1.4;
    if (m === 'tuft') i += 0.8 + Math.min(1, v / 6) * 0.9 + (n > 0.7 ? 0.8 : 0);
    let c = sample(RAMP.fur, i);
    if (o.belly) {
      const t = (o.belly[0] - wy) / (o.belly[0] - o.belly[1]);
      const tt = smooth(t + (n - 0.5) * 0.6);
      if (tt > 0) {
        const bi = 2.2 + shade * 0.7 + (n - 0.5) * 2;
        c = lerp(c, sample(RAMP.belly, bi), tt);
      }
    }
    if (m === 'belly') c = sample(RAMP.belly, 2.4 + shade * 0.8 + (n - 0.5) * 2.2);
    const r = hash(u, v, fid + 91);
    if (r < 0.02) c = sample(RAMP.fur, 5.2 + hash(v, u, 3) * 1.2);
    else if (r > 0.985) c = sample(RAMP.fur, 0);
    return c;
  }

  function paintFace(cube, dir, q, rect) {
    const [x1, y1, x2, y2] = rect;
    const x0 = Math.floor(Math.min(x1, x2)), y0 = Math.floor(Math.min(y1, y2));
    const w = Math.max(1, Math.round(Math.abs(x2 - x1))), h = Math.max(1, Math.round(Math.abs(y2 - y1)));
    const m = q.mat, o = q;
    const fid = (dir.charCodeAt(0) * 31 + cube.name.length * 7 + cube.name.charCodeAt(0)) % 997;
    const from = cube.from, to = cube.to;
    for (let v = 0; v < h; v++) {
      for (let u = 0; u < w; u++) {
        let wy = (dir === 'up') ? to[1] : (dir === 'down') ? from[1] : to[1] - (v + 0.5) / h * (to[1] - from[1]);
        let shade = SHADE[dir];
        if (dir === 'up') shade += (v < 2 ? 0.8 : 0);
        if (dir !== 'up' && dir !== 'down' && v >= h - 1) shade -= 0.7;
        let c;
        if (m === 'fur' || m === 'tuft' || m === 'belly') {
          c = furPixel(m, o, shade, u, v, fid, wy);
        } else if (m === 'skinfur' || m === 'ear') {
          const n = hash(Math.floor(u / 2), Math.floor(v / 2), fid);
          if (n > 0.42) c = furPixel('fur', o, shade, u, v, fid, wy);
          else c = sample(RAMP.skin, 1.6 + shade * 0.7 + (hash(u, v, fid) - 0.5) * 1.8);
        } else if (m === 'skin') {
          const n = hash(u, v, fid);
          c = sample(RAMP.skin, 1.8 + shade * 0.7 + (n - 0.5) * 2.2);
          if (hash(Math.floor(u / 3), Math.floor(v / 2), fid + 5) > 0.88) c = sample(RAMP.skin, 0.4);
        } else if (m === 'mouth') {
          c = sample(RAMP.mouth, 1.2 + (hash(u, v, fid) - 0.5) * 2.6 + shade * 0.4);
        } else if (m === 'bone') {
          const down = /low/.test(cube.name) ? 'up' : /claw/.test(cube.name) ? 'front' : 'down';
          let tipT;
          if (down === 'front') {
            const fz = (dir === 'up' || dir === 'down') ? 1 - v / h : (dir === 'east' || dir === 'west') ? u / w : 0.5;
            tipT = 1 - fz;
            if (dir === 'east' || dir === 'west') tipT = 1 - u / w; else if (dir === 'up' || dir === 'down') tipT = v / h;
          } else if (down === 'up') tipT = 1 - v / h; else tipT = v / h;
          c = sample(RAMP.bone, 1.2 + tipT * 3.2 + (hash(u, v, fid) - 0.5) * 0.8 + shade * 0.3);
          if (down === 'front') c = sample(RAMP.bone, 0.8 + (1 - tipT) * 3.4 + shade * 0.3 + (hash(u, v, fid) - 0.5) * 0.7);
        } else c = [255, 0, 255];
        put(x0 + u, y0 + v, c);
      }
    }
    if (o.vein && m !== 'bone' && dir !== 'down') {
      const n = Math.ceil(o.vein * 0.4 * w * h / 42);
      for (let k = 0; k < n; k++) {
        let x = Math.floor(rnd() * w), y = Math.floor(rnd() * Math.max(1, h * 0.5));
        const len = 6 + Math.floor(rnd() * 10);
        for (let s = 0; s < len && y < h; s++) {
          if (x >= 0 && x < w) put(x0 + x, y0 + y, rnd() < 0.2 ? [150, 30, 28] : [96, 14, 16], 0.85);
          y += 1; if (rnd() < 0.45) x += rnd() < 0.5 ? -1 : 1;
          if (rnd() < 0.12) { const bx = x + (rnd() < 0.5 ? -1 : 1); if (bx >= 0 && bx < w && y < h) put(x0 + bx, y0 + y, [104, 16, 18], 0.8); }
        }
      }
    }
    return { x0, y0, w, h };
  }

  const EYE = [
    '##....',
    '.#####',
    '..###.',
  ];
  function eye(ox, oy, flipX) {
    const rows = EYE.map(r => flipX ? r.split('').reverse().join('') : r);
    for (let j = -1; j <= rows.length; j++) for (let i = -1; i <= rows[0].length; i++) put(ox + i, oy + j, [30, 70, 130], 0.22);
    rows.forEach((r, j) => r.split('').forEach((ch, i) => {
      if (ch !== '#') return;
      const core = j === 1 && i >= 1 && i <= 4;
      put(ox + i, oy + j, core ? [236, 250, 255] : [112, 188, 255]);
    }));
  }

  const glow = [];
  Cube.all.forEach(cube => {
    if (cube.mirror_uv) return;
    const q = info[cube.name];
    if (!q) return;
    const rects = {};
    for (const dir of ['north', 'east', 'south', 'west', 'up', 'down']) {
      if (!cube.faces[dir] || !cube.faces[dir].uv) continue;
      rects[dir] = paintFace(cube, dir, q, cube.faces[dir].uv);
    }
    if (cube.name === 'cranium' && rects.north) {
      const r = rects.north;
      eye(r.x0 + 2, r.y0 + 4, false);
      eye(r.x0 + r.w - 8, r.y0 + 4, true);
    }
    if (cube.name === 'nose' && rects.north) {
      put(rects.north.x0 + 1, rects.north.y0 + 1, [0, 0, 0]);
      put(rects.north.x0 + rects.north.w - 2, rects.north.y0 + 1, [0, 0, 0]);
    }
    if (cube.name === 'muzzle' && rects.north) {
      const r = rects.north;
      for (let u = 0; u < r.w; u++) put(r.x0 + u, r.y0 + r.h - 1, [20, 4, 10]);
    }
    if (q.mat === 'ear' && rects.north) {
      const r = rects.north;
      for (let v = 1; v < r.h; v++) for (let u = 1; u < r.w - 1; u++) put(r.x0 + u, r.y0 + v, sample(RAMP.mouth, 2.5 + hash(u, v, 5) * 1.2));
    }
  });

  ctx.putImageData(img, 0, 0);
  const url = cv.toDataURL('image/png');
  Texture.all.slice().forEach(t => t.remove && t.remove());
  const tex = new Texture({ name: 'beast_of_gevaudan_geo.png' }).fromDataURL(url).add(false);
  tex.width = W; tex.height = H;
  Cube.all.forEach(c => c.applyTexture(tex, true));
  Canvas.updateAll();
  if (cfg.out) cfg.fs.writeFileSync(cfg.out, Buffer.from(url.split(',')[1], 'base64'));
  return { size: [W, H], cubes: Cube.all.length };
})
