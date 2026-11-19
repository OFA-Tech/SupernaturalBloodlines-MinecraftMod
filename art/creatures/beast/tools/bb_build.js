// risky_eval: (0,eval)(bb_build.js)({fs, project, spec, faces})
// Rebuilds the outline of the named project from spec.json and writes every cube's face UVs to faces.json for the painter.
(function (cfg) {
  const fs = cfg.fs;
  const spec = JSON.parse(fs.readFileSync(cfg.spec, 'utf8'));
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  // the GeckoLib plugin's select handler can throw after the switch has already happened
  try { proj.select(); } catch (e) {}
  if (Project !== proj) throw new Error('could not select ' + cfg.project);
  if (Modes.selected.id !== 'edit') Modes.options.edit.select();

  for (const e of Outliner.elements.slice()) e.remove();
  for (const g of Group.all.slice()) g.remove();
  Project.texture_width = spec.texture[0];
  Project.texture_height = spec.texture[1];
  Project.box_uv = true;
  Project.geometry_name = spec.name;
  Project.model_identifier = spec.name;

  const groups = {};
  const faces = [];
  for (const b of spec.bones) {
    const g = new Group({ name: b.name, origin: b.pivot, rotation: [0, 0, 0] }).init();
    if (b.parent) g.addTo(groups[b.parent]);
    groups[b.name] = g;
    for (const c of b.cubes) {
      const cube = new Cube({ name: c.name, from: c.from, to: c.to, origin: c.origin, rotation: c.rotation, uv_offset: c.uv, box_uv: true }).init();
      cube.addTo(g);
      const tex = Texture.all[0];
      if (tex) cube.applyTexture(tex, true);
      Canvas.updateUV(cube);
      const f = {};
      for (const dir in cube.faces) f[dir] = cube.faces[dir].uv.slice();
      faces.push({ name: c.name, bone: b.name, mat: c.mat, tone: c.tone, size: c.size, from: c.from, to: c.to, rotation: c.rotation, faces: f });
    }
  }
  Canvas.updateAll();
  fs.writeFileSync(cfg.faces, JSON.stringify({ texture: spec.texture, cubes: faces }));
  return { bones: Group.all.length, cubes: Cube.all.length, tex: [Project.texture_width, Project.texture_height] };
})
