// Run inside Blockbench (risky_eval, API only, no focus). cfg = {name, geo, anim, tex}
// Builds a GeckoLib project from the generated geo/animation/texture files. Only touches the project named cfg.name
// (create it first with the create_project tool, format geckolib_model). Blockbench keeps x mirrored vs the file.
(async function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('create the project first with create_project: ' + cfg.name);
  proj.select();
  if (Format.id !== 'geckolib_model') throw new Error('project is ' + Format.id);
  Animation.all.slice().forEach(a => a.remove(false, false));
  Texture.all.slice().forEach(t => t.remove(true));
  Outliner.root.slice().forEach(n => n.remove && n.remove());
  const geo = JSON.parse(fs.readFileSync(cfg.geo, 'utf8'))['minecraft:geometry'][0];
  Project.texture_width = geo.description.texture_width;
  Project.texture_height = geo.description.texture_height;
  Project.box_uv = false;
  Project.geometry_name = geo.description.identifier.replace('geometry.', '');
  const groups = {};
  for (const b of geo.bones) {
    const g = new Group({
      name: b.name,
      origin: [-b.pivot[0], b.pivot[1], b.pivot[2]],
      rotation: b.rotation ? [-b.rotation[0], -b.rotation[1], b.rotation[2]] : [0, 0, 0],
    });
    if (b.parent && groups[b.parent]) g.addTo(groups[b.parent]);
    g.init();
    groups[b.name] = g;
    (b.cubes || []).forEach((c, i) => {
      const o = c.origin, s = c.size;
      const faces = {};
      for (const f of ['north', 'east', 'south', 'west', 'up', 'down']) {
        const [u, v] = c.uv[f].uv, [w, h] = c.uv[f].uv_size;
        const hh = Math.abs(h);
        faces[f] = { uv: f === 'up' ? [u + w, v + hh, u, v] : f === 'down' ? [u + w, v - hh, u, v] : [u, v, u + w, v + hh] };
      }
      const cube = new Cube({
        name: b.name + '_' + i,
        from: [-(o[0] + s[0]), o[1], o[2]],
        to: [-o[0], o[1] + s[1], o[2] + s[2]],
        inflate: c.inflate || 0,
        box_uv: false,
        faces,
      });
      if (c.rotation) {
        cube.origin = [-c.pivot[0], c.pivot[1], c.pivot[2]];
        cube.rotation = [-c.rotation[0], -c.rotation[1], c.rotation[2]];
      }
      cube.addTo(g).init();
    });
  }
  // data URL, not fromPath: the webview caches images by path and would show a stale texture
  const tex = new Texture({ name: cfg.name + '.png' }).fromDataURL('data:image/png;base64,' + fs.readFileSync(cfg.tex).toString('base64')).add(false);
  await new Promise(r => { if (tex.img && tex.img.complete && tex.img.naturalWidth) r(); else tex.img.addEventListener('load', () => setTimeout(r, 30)); });
  Cube.all.forEach(c => c.applyTexture(tex, true));
  if (cfg.anim) Animator.loadFile({ path: cfg.anim, content: fs.readFileSync(cfg.anim, 'utf8') });
  Canvas.updateAll();
  return { name: Project.name, cubes: Cube.all.length, groups: Group.all.length, anims: Animation.all.map(a => a.name), box_uv: Project.box_uv };
})
