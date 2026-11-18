// risky_eval: (0,eval)(shot.js)(cfg). cfg = {fs, project, view, outDir, base, anim?, time?, views:[{name,pos,target,fov,anim?,time?}]}
// Renders the named project through an MCP offscreen view and writes PNGs. Never touches the user's viewport.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  proj.select();
  const pv = Preview.all.find(p => p.id === 'mcp_offscreen_' + cfg.view);
  if (!pv) throw new Error('view not found');
  const out = [];
  for (const v of cfg.views) {
    const an = v.anim !== undefined ? v.anim : cfg.anim;
    const tm = v.time !== undefined ? v.time : (cfg.time || 0);
    if (an) {
      const a = Animation.all.find(x => x.name === an || x.name.endsWith('.' + an));
      if (!a) throw new Error('animation not found: ' + an);
      Modes.options.animate.select();
      a.select();
      Timeline.setTime(tm);
      Animator.preview();
    } else {
      if (Modes.animate) Modes.options.edit.select();
      Canvas.updateAll();
    }
    pv.setProjectionMode(false);
    pv.camera.position.set(...v.pos);
    pv.controls.target.set(...v.target);
    pv.camera.fov = v.fov || 40;
    pv.camera.updateProjectionMatrix();
    pv.controls.update();
    let url;
    Canvas.withoutGizmos(() => { pv.render(); url = pv.canvas.toDataURL('image/png'); });
    const file = cfg.outDir + '/' + cfg.base + '_' + v.name + '.png';
    cfg.fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
    out.push(file);
  }
  return out;
})
