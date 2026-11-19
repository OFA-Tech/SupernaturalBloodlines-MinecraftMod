// risky_eval: (0,eval)(bb_anim.js)({fs, project, file})
// Replaces the project's animations with the ones in the animation.json and reports what Blockbench read back.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  // the GeckoLib plugin's select handler can throw after the switch has already happened
  try { proj.select(); } catch (e) {}
  if (Project !== proj) throw new Error('could not select ' + cfg.project);
  for (const a of Animation.all.slice()) a.remove(false);
  Animator.loadFile({ path: cfg.file, content: cfg.fs.readFileSync(cfg.file, 'utf8') });
  for (const a of Animation.all) { a.path = ''; a.saved = true; }
  let eased = 0, keys = 0;
  for (const a of Animation.all) for (const id in a.animators) for (const k of a.animators[id].keyframes) { keys++; if (k.easing && k.easing !== 'linear') eased++; }
  return { animations: Animation.all.map(a => a.name.split('.').pop() + ':' + a.length + ':' + a.loop), keys, eased };
})
