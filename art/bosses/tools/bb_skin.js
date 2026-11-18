// Run inside Blockbench (risky_eval). cfg = {name, texPath, slim, cane}
// Builds the standard 64x64 player model (base + second layer) as a Modded Entity project.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('project not found: ' + cfg.name);
  proj.select();
  Project.texture_width = 64; Project.texture_height = 64;
  const aw = cfg.slim ? 3 : 4;
  // [bone, pivot, [name, from, to, uv, inflate]...]
  const bones = [
    ['head', [0, 24, 0], [['head', [-4, 24, -4], [4, 32, 4], [0, 0], 0], ['hat', [-4, 24, -4], [4, 32, 4], [32, 0], 0.5]]],
    ['body', [0, 24, 0], [['body', [-4, 12, -2], [4, 24, 2], [16, 16], 0], ['jacket', [-4, 12, -2], [4, 24, 2], [16, 32], 0.25]]],
    ['right_arm', [5, 22, 0], [['right_arm', [4, 12, -2], [4 + aw, 24, 2], [40, 16], 0], ['right_sleeve', [4, 12, -2], [4 + aw, 24, 2], [40, 32], 0.25]]],
    ['left_arm', [-5, 22, 0], [['left_arm', [-4 - aw, 12, -2], [-4, 24, 2], [32, 48], 0], ['left_sleeve', [-4 - aw, 12, -2], [-4, 24, 2], [48, 48], 0.25]]],
    ['right_leg', [1.9, 12, 0], [['right_leg', [0, 0, -2], [4, 12, 2], [0, 16], 0], ['right_pants', [0, 0, -2], [4, 12, 2], [0, 32], 0.25]]],
    ['left_leg', [-1.9, 12, 0], [['left_leg', [-4, 0, -2], [0, 12, 2], [16, 48], 0], ['left_pants', [-4, 0, -2], [0, 12, 2], [0, 48], 0.25]]],
  ];
  for (const [bone, pivot, cubes] of bones) {
    const g = new Group({ name: bone, origin: pivot }).init();
    for (const [name, from, to, uv, inflate] of cubes) {
      new Cube({ name, from, to, origin: pivot, uv_offset: uv, inflate, box_uv: true }).addTo(g).init();
    }
  }
  if (cfg.cane) {
    // held in front of the right hand; box UV nets sit in free skin space (56,16) and (56,32)
    const g = new Group({ name: 'cane_prop', origin: [6, 12, -3] }).init();
    new Cube({ name: 'cane_shaft', from: [5.5, 0, -3.5], to: [6.5, 13, -2.5], origin: [6, 12, -3], uv_offset: [56, 16], box_uv: true }).addTo(g).init();
    new Cube({ name: 'cane_knob', from: [5, 13, -4], to: [7, 15, -2], origin: [6, 12, -3], uv_offset: [56, 32], box_uv: true }).addTo(g).init();
  }
  const tex = new Texture({ name: cfg.name + '.png' }).fromPath(cfg.texPath).add(false);
  Cube.all.forEach(c => c.applyTexture(tex, true));
  Canvas.updateAll();
  return { cubes: Cube.all.length };
})
