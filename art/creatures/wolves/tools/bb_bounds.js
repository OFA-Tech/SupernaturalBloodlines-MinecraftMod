// Run inside Blockbench: cfg = {project, frames:[[anim,time],...]} -> world-space min/max of the posed model (ground check).
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  proj.select();
  const res = [];
  for (const [an, tm] of cfg.frames) {
    Animator.showDefaultPose(true);
    if (an) {
      const a = Animation.all.find(x => x.name.endsWith('.' + an));
      a.select(); Timeline.setTime(tm); Animator.stackAnimations([a], true);
    }
    const box = new THREE.Box3();
    let low = null, lowy = 1e9;
    Cube.all.forEach(c => { if (c.mesh) { c.mesh.updateMatrixWorld(true); const b = new THREE.Box3().setFromObject(c.mesh); box.union(b); if (b.min.y < lowy) { lowy = b.min.y; low = c.name; } } });
    res.push([an, tm, +box.min.y.toFixed(1), +box.max.y.toFixed(1), +box.min.z.toFixed(1), +box.max.z.toFixed(1), +box.min.x.toFixed(1), +box.max.x.toFixed(1), low]);
  }
  Animator.showDefaultPose(true); Canvas.updateAll();
  return res;
})
