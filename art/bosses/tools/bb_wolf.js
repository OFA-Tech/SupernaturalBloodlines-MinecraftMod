// Run inside Blockbench (risky_eval): cfg = {name, texPath, savePath, modelPath}
// Builds the Werewolves beast model (parsed by parse_model.py) as a Modded Entity project.
(function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('project not found: ' + cfg.name);
  proj.select();
  Project.texture_width = 128; Project.texture_height = 64;
  const model = JSON.parse(fs.readFileSync(cfg.modelPath, 'utf8'));
  const R = 180 / Math.PI;
  const abs = {}, groups = {};
  for (const name of model.order) {
    const p = model.parts[name];
    const par = abs[p.parent] || [0, 0, 0];
    const a = [par[0] + p.pose.o[0], par[1] + p.pose.o[1], par[2] + p.pose.o[2]];
    abs[name] = a;
    const g = new Group({
      name,
      origin: [-a[0], 24 - a[1], a[2]],
      rotation: [-p.pose.r[0] * R, -p.pose.r[1] * R, p.pose.r[2] * R],
    });
    const parent = groups[p.parent];
    if (parent) g.addTo(parent);
    g.init();
    groups[name] = g;
    p.cubes.forEach((c, i) => {
      const [bx, by, bz, w, h, d] = c.box;
      const cube = new Cube({
        name: p.cubes.length > 1 ? name + '_' + i : name,
        from: [-(a[0] + bx + w), 24 - (a[1] + by + h), a[2] + bz],
        to: [-(a[0] + bx), 24 - (a[1] + by), a[2] + bz + d],
        origin: [-a[0], 24 - a[1], a[2]],
        uv_offset: [c.tex[0], c.tex[1]],
        mirror_uv: !!c.mirror,
        box_uv: true,
      });
      cube.addTo(g).init();
    });
  }
  const tex = new Texture({ name: cfg.name + '.png' }).fromPath(cfg.texPath).add(false);
  Cube.all.forEach(c => c.applyTexture(tex, true));
  Canvas.updateAll();
  return { cubes: Cube.all.length, groups: Group.all.length };
})
