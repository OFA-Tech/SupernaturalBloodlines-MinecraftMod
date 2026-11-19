// risky_eval: (0,eval)(bb_tex.js)({fs, project, png, name})
// Puts the painted PNG into the project as its single texture (embedded as a data URL) and applies it to every cube.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  // the GeckoLib plugin's select handler can throw after the switch has already happened
  try { proj.select(); } catch (e) {}
  if (Project !== proj) throw new Error('could not select ' + cfg.project);
  const url = 'data:image/png;base64,' + cfg.fs.readFileSync(cfg.png).toString('base64');
  let tex = Texture.all[0];
  if (!tex) tex = new Texture({ name: cfg.name }).add(false);
  tex.fromDataURL(url);
  tex.name = cfg.name;
  for (const c of Cube.all) c.applyTexture(tex, true);
  Canvas.updateAll();
  // the image decodes asynchronously; resolve once it has, so the next render sees it
  return new Promise(resolve => setTimeout(() => { Canvas.updateAll(); resolve({ textures: Texture.all.length, size: [tex.width, tex.height] }); }, 500));
})
