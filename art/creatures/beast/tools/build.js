// Run inside Blockbench via risky_eval: (0,eval)(fs.readFileSync(build.js))(cfg)
// cfg = {name, specPath, texW, texH}. Rebuilds the outliner of the named GeckoLib project from spec.js.
(function (cfg) {
  const fs = cfg.fs;
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('project not found: ' + cfg.name);
  proj.select();
  const spec = (0, eval)(fs.readFileSync(cfg.specPath, 'utf8'));

  const flip = s => s.replace(/_r(?=_|\d|$)/g, '_l');
  const bones = [];
  for (const bn of spec) {
    bones.push(bn);
    if (bn.mirror) {
      bones.push({
        name: flip(bn.name),
        parent: bn.parent && /_r(?=_|$)/.test(bn.parent) ? flip(bn.parent) : bn.parent,
        pivot: [-bn.pivot[0], bn.pivot[1], bn.pivot[2]],
        rot: [bn.rot[0], -bn.rot[1], -bn.rot[2]],
        mirrorOf: bn.name,
        cubes: bn.cubes.map(q => Object.assign({}, q, {
          name: flip(q.name),
          from: [-(q.from[0] + q.size[0]), q.from[1], q.from[2]],
          mirrorOf: q.name,
        })),
      });
    }
  }

  Project.box_uv = true;
  Project.texture_width = cfg.texW;
  Project.texture_height = cfg.texH;
  Project.geometry_name = 'beast_of_gevaudan';
  Outliner.root.slice().forEach(n => n.remove && n.remove());
  Texture.all.slice().forEach(t => t.remove && t.remove());

  const all = [];
  bones.forEach(bn => bn.cubes.forEach(q => all.push(q)));
  const units = {};
  all.forEach(q => {
    const key = q.mirrorOf || q.name;
    if (units[key]) return;
    const w = Math.ceil(q.size[0]) + 1, h = Math.ceil(q.size[1]) + 1, d = Math.ceil(q.size[2]) + 1;
    units[key] = { key, nw: 2 * (d + w), nh: d + h };
  });
  const list = Object.values(units).sort((a, b) => b.nh - a.nh || b.nw - a.nw);
  let x = 0, y = 0, rowH = 0;
  for (const u of list) {
    if (x + u.nw > cfg.texW) { x = 0; y += rowH + 1; rowH = 0; }
    u.u = x; u.v = y; x += u.nw + 1; rowH = Math.max(rowH, u.nh);
  }
  const used = y + rowH;
  if (used > cfg.texH) throw new Error('UV overflow: need ' + used + ' rows, have ' + cfg.texH);

  const groups = {};
  for (const bn of bones) {
    const g = new Group({ name: bn.name, origin: bn.pivot.slice(), rotation: bn.rot.slice() });
    const par = groups[bn.parent];
    if (par) g.addTo(par);
    g.init();
    groups[bn.name] = g;
    for (const q of bn.cubes) {
      const u = units[q.mirrorOf || q.name];
      const cube = new Cube({
        name: q.name,
        from: q.from.slice(),
        to: [q.from[0] + q.size[0], q.from[1] + q.size[1], q.from[2] + q.size[2]],
        origin: bn.pivot.slice(),
        box_uv: true,
        uv_offset: [u.u, u.v],
        mirror_uv: !!q.mirrorOf,
      });
      cube.addTo(g).init();
    }
  }
  Canvas.updateAll();
  return { bones: bones.length, cubes: all.length, texRowsUsed: used };
})
