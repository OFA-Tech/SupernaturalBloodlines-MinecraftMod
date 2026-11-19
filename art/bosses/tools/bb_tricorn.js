// Run inside Blockbench (risky_eval). cfg = {project, tex, offset, parent, out}
// Builds Sebastien Valet's tricorn from cuboids in the named project, using its texture number cfg.tex.
// 1 unit = 1 skin pixel. The hat is centred on x/z = 8 with its base at y = 0 and its point to the north (front).
// cfg.offset moves it (use [-8, 31, -8] to sit it on a player model's head), cfg.parent is a group name.
// With cfg.out it also writes the Java item model JSON, with the display transforms, and checks it against the
// 1.21.1 model limits (one-axis rotations in 22.5 degree steps, coordinates within -16..32).
// Restores whichever project was selected before.
(function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  const prev = Project;
  proj.select();
  try {
    const tex = Texture.all[cfg.tex || 0];
    const off = cfg.offset || [0, 0, 0];
    const LACE = [0, 10, 1, 11], FELT = [10, 4, 16, 10], EDGE = [10, 9, 16, 10], CROWN = [0, 12, 10, 16], CAP = [2, 12, 8, 12.75];
    const wallX = n => ({ north: [0, 10, n, 14], south: [0, 10, n, 14], east: [0, 10, 1, 14], west: [0, 10, 1, 14], up: LACE, down: EDGE });
    const wallZ = n => ({ east: [0, 10, n, 14], west: [0, 10, n, 14], north: [0, 10, 1, 14], south: [0, 10, 1, 14], up: LACE, down: EDGE });
    const flat = { up: FELT, down: FELT, north: EDGE, south: EDGE, east: EDGE, west: EDGE };
    // right and left are the wearer's (east and west); the cockade goes on the left, as on French army hats
    const HAT = [
      { n: 'crown', f: [3, 0, 3], t: [13, 4, 13], uv: { north: CROWN, south: CROWN, east: CROWN, west: CROWN, up: [0, 0, 10, 10], down: FELT } },
      { n: 'crown_cap', f: [5, 4, 5], t: [11, 4.75, 11], uv: { north: CAP, south: CAP, east: CAP, west: CAP, up: [2, 2, 8, 8] } },
      { n: 'brim', f: [2, 0, 4], t: [14, 1, 14], uv: flat },
      { n: 'brim_point', f: [3.757, 0.05, -0.243], t: [12.243, 0.95, 8.243], r: 45, o: [8, 0.5, 4], uv: flat },
      { n: 'cock_back', f: [1, 0, 14], t: [15, 4, 15], uv: wallX(14) },
      { n: 'cock_right_side', f: [14, 0, 4], t: [15, 4, 14], uv: wallZ(10) },
      { n: 'cock_left_side', f: [1, 0, 4], t: [2, 4, 14], uv: wallZ(10) },
      { n: 'cock_right_front', f: [7, 0, -3], t: [17, 4, -2], r: -45, o: [8, 0, -2], uv: wallX(10) },
      { n: 'cock_left_front', f: [-1, 0, -3], t: [8, 4, -2], r: 45, o: [8, 0, -2], uv: wallX(9) },
      { n: 'cockade', f: [2, 0.5, -3.25], t: [5, 4.5, -3], r: 45, o: [8, 0, -2], uv: { north: [10, 0, 13, 4], south: [10, 0, 13, 4] } },
    ];
    const names = HAT.map(e => e.n);
    Cube.all.filter(c => names.includes(c.name)).forEach(c => c.remove());
    const parent = cfg.parent && Group.all.find(g => g.name === cfg.parent);
    const sh = v => [v[0] + off[0], v[1] + off[1], v[2] + off[2]];
    for (const e of HAT) {
      const faces = {};
      for (const d of ['north', 'south', 'east', 'west', 'up', 'down']) faces[d] = e.uv[d] ? { uv: e.uv[d], texture: tex.uuid } : { texture: null };
      const c = new Cube({ name: e.n, from: sh(e.f), to: sh(e.t), origin: sh(e.o || [8, 0, 8]), rotation: [0, e.r || 0, 0], box_uv: false, faces });
      if (parent) c.addTo(parent);
      c.init();
    }
    Canvas.updateAll();
    if (!cfg.out) return { cubes: names.length };
    const model = JSON.parse(Codecs.java_block.compile());
    model.textures = { '0': 'supernaturalbloodlines:item/sebastien_tricorn', particle: 'supernaturalbloodlines:item/sebastien_tricorn' };
    // head: CustomHeadLayer scales items by 0.625 about the head centre, so 1.6 makes 1 unit = 1 skin pixel and
    // 17.6 lifts the base to 1 pixel below the top of the head
    model.display = {
      head: { translation: [0, 17.6, 0], scale: [1.6, 1.6, 1.6] },
      gui: { rotation: [30, 225, 0], translation: [0, 3.75, 0], scale: [0.7, 0.7, 0.7] },
      ground: { translation: [0, 3, 0], scale: [0.5, 0.5, 0.5] },
      fixed: { translation: [0, 4, 0], scale: [0.9, 0.9, 0.9] },
      thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
      firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 3, 0], scale: [0.4, 0.4, 0.4] },
      firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 3, 0], scale: [0.4, 0.4, 0.4] },
    };
    delete model.format_version; // Blockbench stamps its newest Java format; the file is plain 1.21.1
    for (const e of model.elements) {
      if (e.rotation && e.rotation.angle === 0) delete e.rotation;
      if ([...e.from, ...e.to].some(v => v < -16 || v > 32)) throw new Error('out of range: ' + e.name);
      if (e.rotation && ![-45, -22.5, 0, 22.5, 45].includes(e.rotation.angle)) throw new Error('bad angle: ' + e.name);
      for (const f of Object.values(e.faces)) if (f.texture !== '#0' || f.uv.some(v => v < 0 || v > 16)) throw new Error('bad face: ' + e.name);
    }
    fs.writeFileSync(cfg.out, compileJSON(model) + '\n');
    return { elements: model.elements.length, bytes: fs.statSync(cfg.out).size };
  } finally {
    if (prev && prev !== Project) { try { prev.select(); } catch (e) { /* GeckoLib's select hook throws; the switch itself has happened */ } }
  }
})
