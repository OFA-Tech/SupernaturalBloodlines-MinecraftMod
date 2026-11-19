// Run inside Blockbench (risky_eval, API only, no focus).
// cfg = {project, view, outDir, base, anim?, time?, views:[{name,pos,target,fov,anim?,time?}]}
// Renders through an MCP offscreen view (create_offscreen_view); never touches the user's viewport.
(function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  proj.select();
  const pv = Preview.all.find(p => p.id === 'mcp_offscreen_' + cfg.view);
  if (!pv) throw new Error('view not found: ' + cfg.view);
  fs.mkdirSync(cfg.outDir, { recursive: true });
  const out = [];
  const saved = { pos: pv.camera.position.clone(), tgt: pv.controls.target.clone(), fov: pv.camera.fov };
  for (const v of cfg.views) {
    const an = v.anim !== undefined ? v.anim : cfg.anim;
    const tm = v.time !== undefined ? v.time : cfg.time;
    Animator.showDefaultPose && Animator.showDefaultPose(true);
    if (an) {
      const a = Animation.all.find(x => x.name === an || x.name.endsWith('.' + an));
      if (!a) throw new Error('anim not found: ' + an);
      a.select();
      Timeline.setTime(tm || 0);
      Animator.stackAnimations([a], true);
    }
    pv.setProjectionMode(false);
    let pos = v.pos, tgt = v.target;
    if (v.focus) {   // frame a bone: camera = bone world position + off, looking at it
      const g = Group.all.find(x => x.name === v.focus);
      const w = new THREE.Vector3();
      g.mesh.getWorldPosition(w);
      tgt = [w.x, w.y, w.z];
      pos = [w.x + v.off[0], w.y + v.off[1], w.z + v.off[2]];
    }
    pv.camera.position.set(...pos);
    pv.controls.target.set(...tgt);
    pv.camera.fov = v.fov || 40;
    pv.camera.updateProjectionMatrix();
    pv.controls.update();
    let url;
    Canvas.withoutGizmos(() => { pv.render(); url = pv.canvas.toDataURL('image/png'); });
    const file = cfg.outDir + '/' + cfg.base + '_' + v.name + '.png';
    fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
    out.push(file);
  }
  pv.camera.position.copy(saved.pos); pv.controls.target.copy(saved.tgt); pv.camera.fov = saved.fov;
  pv.camera.updateProjectionMatrix(); pv.controls.update();
  if (cfg.reset !== false) { Animator.showDefaultPose && Animator.showDefaultPose(true); Canvas.updateAll(); }
  return out;
})
