// risky_eval: (0,eval)(export.js)(cfg). cfg = {fs, name, geo, anim, bbmodel}
// Writes the GeckoLib geo.json (Blockbench's own Bedrock compile), the animation.json and the .bbmodel.
// The animation file is built straight from the keyframes: Blockbench-native values (GeckoLib format v2) with easing / easingArgs.
// The installed GeckoLib plugin's keyframe patches are not active in this Blockbench session, so its own compile drops easings.
(function (cfg) {
  const proj = ModelProject.all.find(p => p.name === cfg.name);
  if (!proj) throw new Error('project not found: ' + cfg.name);
  proj.select();
  const fs = cfg.fs;

  const geoSrc = Codecs.bedrock.compile({});
  fs.writeFileSync(cfg.geo, (typeof geoSrc === 'string' ? JSON.stringify(JSON.parse(geoSrc), null, 2) : JSON.stringify(geoSrc, null, 2)) + '\n');

  const num = v => { const n = Number(v); return Math.round(n * 10000) / 10000; };
  const out = { format_version: '1.8.0', geckolib_format_version: 2, animations: {} };
  for (const a of Animation.all) {
    const o = {};
    if (a.loop === 'loop') o.loop = true; else if (a.loop === 'hold') o.loop = 'hold_on_last_frame';
    o.animation_length = num(a.length);
    o.bones = {};
    for (const id in a.animators) {
      const an = a.animators[id];
      if (an.type !== 'bone') continue;
      const bone = {};
      for (const ch of ['rotation', 'position', 'scale']) {
        const kfs = (an[ch] || []).slice().sort((x, y) => x.time - y.time);
        if (!kfs.length) continue;
        const tr = {};
        for (const k of kfs) {
          const d = k.data_points[0];
          const vec = [num(d.x), num(d.y), num(d.z)];
          const key = num(k.time).toFixed(4).replace(/0+$/, '').replace(/\.$/, '.0');
          if (k.easing && k.easing !== 'linear') {
            const e = { vector: vec, easing: k.easing };
            if (Array.isArray(k.easingArgs) && k.easingArgs.length) e.easingArgs = k.easingArgs.slice();
            tr[key] = e;
          } else tr[key] = vec;
        }
        bone[ch] = tr;
      }
      if (Object.keys(bone).length) o.bones[an.name] = bone;
    }
    out.animations[a.name] = o;
  }
  fs.writeFileSync(cfg.anim, JSON.stringify(out, null, 2) + '\n');

  const pj = Codecs.project.compile({ compressed: false });
  fs.writeFileSync(cfg.bbmodel, typeof pj === 'string' ? pj : JSON.stringify(pj, null, 2));
  return { anims: Object.keys(out.animations).length, geoBytes: fs.statSync(cfg.geo).size, animBytes: fs.statSync(cfg.anim).size, bbBytes: fs.statSync(cfg.bbmodel).size };
})
