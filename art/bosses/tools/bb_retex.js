// Run inside Blockbench (risky_eval). cfg = {project, texPath}
// Reloads the project's first texture from disk after the PNG was repainted.
(function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  proj.select();
  const tex = Texture.all[0];
  const data = 'data:image/png;base64,' + fs.readFileSync(cfg.texPath).toString('base64');
  tex.fromDataURL(data);
  tex.updateSource && tex.updateSource(data);
  Canvas.updateAllFaces && Canvas.updateAllFaces();
  return Texture.all.length;
})
