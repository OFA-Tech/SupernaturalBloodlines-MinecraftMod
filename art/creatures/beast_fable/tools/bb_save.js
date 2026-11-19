// risky_eval: (0,eval)(bb_save.js)({fs, project, bbmodel})
// Writes the project as an uncompressed .bbmodel (texture and animations embedded), without the machine's export paths.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  // the GeckoLib plugin's select handler can throw after the switch has already happened
  try { proj.select(); } catch (e) {}
  if (Project !== proj) throw new Error('could not select ' + cfg.project);
  const cleared = [];
  for (const k of Object.keys(Project)) {
    if (/filepath_cache/i.test(k) && Project[k] && typeof Project[k] === 'object') { Project[k] = {}; cleared.push(k); }
  }
  const pj = Codecs.project.compile({ compressed: false });
  cfg.fs.writeFileSync(cfg.bbmodel, typeof pj === 'string' ? pj : JSON.stringify(pj));
  return { bytes: cfg.fs.statSync(cfg.bbmodel).size, cleared, animations: Animation.all.length, textures: Texture.all.length };
})
