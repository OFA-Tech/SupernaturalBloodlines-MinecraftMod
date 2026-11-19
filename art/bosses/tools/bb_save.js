// Run inside Blockbench (risky_eval). cfg = {project, out}
// Writes the project as a .bbmodel (textures embedded) without touching other projects.
(function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  proj.select();
  const data = Codecs.project.compile({ compressed: false });
  fs.writeFileSync(cfg.out, typeof data === 'string' ? data : JSON.stringify(data, null, 2));
  return fs.statSync(cfg.out).size;
})
