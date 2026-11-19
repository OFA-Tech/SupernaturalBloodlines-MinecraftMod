// Run inside Blockbench (risky_eval). cfg = {project, view, outDir, base, views:[{name,pos,target,fov}]}
// Renders the named project through an existing MCP offscreen view (create_offscreen_view) and writes PNGs.
// Never touches the user's viewport camera.
(function (cfg) {
  const fs = require('fs');
  const proj = ModelProject.all.find(p => p.name === cfg.project);
  if (!proj) throw new Error('project not found: ' + cfg.project);
  proj.select();
  const pv = Preview.all.find(p => p.id === 'mcp_offscreen_' + cfg.view);
  if (!pv) throw new Error('view not found');
  Canvas.updateAll();
  const out = [];
  for (const v of cfg.views) {
    pv.setProjectionMode(false);
    pv.camera.position.set(...v.pos);
    pv.controls.target.set(...v.target);
    pv.camera.fov = v.fov || 40;
    pv.camera.updateProjectionMatrix();
    pv.controls.update();
    let url;
    Canvas.withoutGizmos(() => { pv.render(); url = pv.canvas.toDataURL('image/png'); });
    const file = cfg.outDir + '/' + cfg.base + '_' + v.name + '.png';
    fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
    out.push(file);
  }
  return out;
})

