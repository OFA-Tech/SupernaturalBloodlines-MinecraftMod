// risky_eval: (0,eval)(bb_shot.js)({fs, project, view, shots:[{file, pos, target, fov?, anim?, time?}], sheet?:{file, cols, w, h}})
// Renders the named project through an MCP offscreen view and writes PNGs. The user's viewport and camera are never touched.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  // the GeckoLib plugin's select handler can throw after the switch has already happened
  try { proj.select(); } catch (e) {}
  if (Project !== proj) throw new Error('could not select ' + cfg.project);
  const pv = Preview.all.find(p => p.id === 'mcp_offscreen_' + cfg.view);
  if (!pv) throw new Error('view not found: ' + cfg.view);
  const out = [];
  for (const s of cfg.shots) {
    if (s.anim) {
      const a = Animation.all.find(x => x.name.endsWith('.' + s.anim));
      if (!a) throw new Error('animation not found: ' + s.anim);
      if (Modes.selected.id !== 'animate') Modes.options.animate.select();
      a.select();
      Timeline.setTime(s.time || 0);
      Animator.preview();
    } else {
      if (Modes.selected.id !== 'edit') Modes.options.edit.select();
      Canvas.updateAll();
    }
    pv.setProjectionMode(false);
    pv.camera.position.set(...s.pos);
    pv.controls.target.set(...s.target);
    pv.camera.fov = s.fov || 35;
    pv.camera.updateProjectionMatrix();
    pv.controls.update();
    let url;
    Canvas.withoutGizmos(() => { pv.render(); url = pv.canvas.toDataURL('image/png'); });
    cfg.fs.writeFileSync(s.file, Buffer.from(url.split(',')[1], 'base64'));
    out.push(s.file);
    if (cfg.sheet) {
      // quick work-in-progress contact sheet of this call's shots
      const sh = cfg.sheet, i = out.length - 1;
      if (!sh.canvas) {
        sh.canvas = document.createElement('canvas');
        sh.canvas.width = sh.cols * sh.w;
        sh.canvas.height = Math.ceil(cfg.shots.length / sh.cols) * sh.h;
        const c = sh.canvas.getContext('2d');
        c.fillStyle = '#3a3e46';
        c.fillRect(0, 0, sh.canvas.width, sh.canvas.height);
      }
      sh.canvas.getContext('2d').drawImage(pv.canvas, (i % sh.cols) * sh.w, Math.floor(i / sh.cols) * sh.h, sh.w, sh.h);
    }
  }
  if (cfg.sheet) cfg.fs.writeFileSync(cfg.sheet.file, Buffer.from(cfg.sheet.canvas.toDataURL('image/png').split(',')[1], 'base64'));
  if (Modes.selected.id !== 'edit') Modes.options.edit.select();
  return out;
})
