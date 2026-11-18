// Loaded into Blockbench with risky_eval: eval(require('fs').readFileSync(<this file>,'utf8')).
// Builds item_<name> projects from art/items/spec3d/<name>.json, exports models/bbmodels, renders offscreen previews.
(function () {
  const fs = require('fs');
  const ROOT = 'D:/Development/OFA-TECH/sb-art-items/';
  const TEX = ROOT + 'src/main/resources/assets/supernaturalbloodlines/textures/item/';
  const MODELS = ROOT + 'src/main/resources/assets/supernaturalbloodlines/models/item/';
  const BBM = ROOT + 'art/blockbench/items/';
  const SPEC = ROOT + 'art/items/spec3d/';
  const TEMP = 'C:/Users/caiom/AppData/Local/Temp/opencode/item-art/v4/';
  const PREV = ROOT + 'art/previews/items/';
  const NS = 'supernaturalbloodlines:item/';
  const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];
  const pv = () => Preview.all.find(p => p.id === (window.SB && window.SB.viewId || 'mcp_offscreen_sbitems_gui'));
  const b64 = f => 'data:image/png;base64,' + fs.readFileSync(f).toString('base64');
  const proj = n => ModelProject.all.find(p => p.name === 'item_' + n);
  function use(n) { const p = proj(n); if (!p) throw new Error('no project ' + n); p.select(); if (Project !== p) throw new Error('select ' + n); return p; }
  function save(buf) { for (const d of [PREV, TEMP]) { fs.mkdirSync(d, { recursive: true }); } return buf; }
  function write(file, buf) { for (const d of [PREV, TEMP]) fs.mkdirSync(d, { recursive: true }); fs.writeFileSync(PREV + file, buf); fs.writeFileSync(TEMP + file, buf); }
  function render(file) {
    const v = pv();
    v.renderer.setClearColor(0x8a909c, 1);
    const grid = Canvas.scene.children.find(c => c.name === 'grid_group'); const gv = grid && grid.visible; if (grid) grid.visible = false;
    const tp = Transformer.parent; if (tp) tp.remove(Transformer);
    const pm = Canvas.pivot_marker.visible; Canvas.pivot_marker.visible = false;
    const og = Canvas.scene.children.find(c => c.name === 'outline_group'); const ov = og && og.visible; if (og) og.visible = false;
    v.render();
    const buf = Buffer.from(v.canvas.toDataURL('image/png').split(',')[1], 'base64');
    Canvas.pivot_marker.visible = pm; if (tp) tp.add(Transformer); if (grid) grid.visible = gv; if (og) og.visible = ov;
    write(file, buf);
  }
  const SB = window.SB = window.SB || {};

  SB.build = function (n) {
    use(n);
    Cube.all.slice().forEach(c => c.remove(false)); Texture.all.slice().forEach(t => t.remove(false));
    const spec = JSON.parse(fs.readFileSync(SPEC + n + '.json'));
    const pal = new Texture({ name: n + '.png' }).fromDataURL(b64(TEX + '3d/' + n + '.png')).add(false);
    const texs = { pal };
    for (const [k, v] of Object.entries(spec.extra_tex || {})) {
      const file = v.split('/').pop();
      texs[k] = new Texture({ name: file + '.png' }).fromDataURL(b64(TEX + file + '.png')).add(false);
    }
    for (const c of spec.cubes) {
      const cube = new Cube({ name: c.n, from: c.f, to: c.t, autouv: 0, box_uv: false });
      if (c.r) { cube.origin = c.o; const rot = [0, 0, 0]; rot['xyz'.indexOf(c.r[0])] = c.r[1]; cube.rotation = rot; }
      cube.init();
      for (const f of FACES) {
        const idx = c.c[f];
        if (c.tex) {
          if (f === 'north' || f === 'south') { cube.faces[f].uv = [0, 0, 16, 16]; cube.faces[f].texture = texs[c.tex].uuid; } else cube.faces[f].texture = false;
        } else if (idx === null || idx === undefined) cube.faces[f].texture = false;
        else { const px = idx % 16, py = Math.floor(idx / 16); cube.faces[f].uv = [px + .2, py + .2, px + .8, py + .8]; cube.faces[f].texture = pal.uuid; }
      }
    }
    for (const [slot, d] of Object.entries(spec.display)) Project.display_settings[slot] = new DisplaySlot(slot).extend(d);
    Canvas.updateAll(); unselectAllElements();
    return spec.cubes.length;
  };

  SB.setDisplay = function (n) {
    use(n);
    const spec = JSON.parse(fs.readFileSync(SPEC + n + '.json'));
    for (const [slot, d] of Object.entries(spec.display)) Project.display_settings[slot] = new DisplaySlot(slot).extend(d);
    return n;
  };

  SB.export = function (n, faceTex) {
    use(n); unselectAllElements();
    const j = JSON.parse(Codecs.java_block.compile({ raw: false }));
    delete j.format_version; delete j.credit;
    const t = NS + '3d/' + n;
    let spec = {}; try { spec = JSON.parse(fs.readFileSync(SPEC + n + '.json')); } catch (e) {}
    const tex = { '0': t, particle: t };
    let s = JSON.stringify(j.elements);
    if (spec.extra_tex && spec.extra_tex.face) { tex.face = spec.extra_tex.face; s = s.replace(/"#1"/g, '"#face"'); }
    j.elements = JSON.parse(s); j.textures = tex;
    j.gui_light = 'front';
    fs.writeFileSync(MODELS + n + '_3d.json', JSON.stringify(j, null, 2) + '\n');
    const bb = Codecs.project.compile({ compressed: false });
    fs.writeFileSync(BBM + 'item_' + n + '.bbmodel', typeof bb === 'string' ? bb : JSON.stringify(bb));
    return j.elements.length;
  };

  const CAMS = {
    tp: { load: 'loadThirdRight', bar: ['player'], cam: { position: [70, 20, 0], target: [0, 15, 0], projection: 'perspective', fov: 32, locked_angle: null } },
    ground: { load: 'loadGround', bar: ['block'], cam: { position: [-26, 14, -26], target: [0, 2, 0], projection: 'perspective', fov: 34, locked_angle: null } },
    fp: { load: 'loadFirstRight', bar: ['monitor'], cam: { position: [0, 24, 32.4], target: [0, 24, 0], projection: 'perspective', fov: 70, locked_angle: null } },
  };

  SB.shots = function (n, views) {
    use(n); unselectAllElements();
    const main = Preview.all.find(p => p.id === 'main');
    const saved = { pos: main.camera.position.toArray(), tgt: main.controls.target.toArray(), ortho: main.isOrtho, zoom: main.camera.zoom };
    const prev = Mode.selected.id;
    Modes.options.display.select();
    for (const v of views) {
      const c = CAMS[v];
      DisplayMode[c.load](); displayReferenceObjects.bar(c.bar);
      pv().loadAnglePreset(c.cam);
      render(n + '_' + v + '.png');
    }
    Modes.options[prev].select();
    main.loadAnglePreset({ position: saved.pos, target: saved.tgt, projection: saved.ortho ? 'orthographic' : 'perspective', zoom: saved.zoom, locked_angle: null });
    return views;
  };

  SB.bbOnly = function (n) {
    use(n); unselectAllElements();
    const bb = Codecs.project.compile({ compressed: false });
    fs.writeFileSync(BBM + 'item_' + n + '.bbmodel', typeof bb === 'string' ? bb : JSON.stringify(bb));
    return n;
  };

  // Right arms (classic + slim) wearing the exported models/curio/*.json, one arm per ring, spread along z.
  SB.armSetup = function () {
    const p = proj('ring_worn_preview'); p.select(); if (Project !== p) throw new Error('select arm');
    Cube.all.slice().forEach(c => c.remove(false)); Texture.all.slice().forEach(t => t.remove(false));
    const CUR = MODELS.replace('models/item/', 'models/curio/');
    const arm = new Texture({ name: 'arm_palette.png' }).fromDataURL(b64(ROOT + 'art/items/arm_palette.png')).add(false);
    const texCache = {};
    const rings = ['sun_ring', 'moon_ring', 'resurrection_ring'];
    let i = 0;
    for (const slim of [false, true]) for (const ring of rings) {
      const dz = (i - 2.5) * 6.2; i++;
      const hx = slim ? 1.5 : 2;
      const px = [[0.5, 0.5], [1.5, 0.5], [2.5, 0.5]];
      const add = (name, from, to, u) => {
        const c = new Cube({ name, from, to, autouv: 0, box_uv: false }).init();
        for (const f of FACES) { c.faces[f].uv = [u + .2, .2, u + .8, .8]; c.faces[f].texture = arm.uuid; }
      };
      add('arm_skin_' + i, [8 - hx, 6.5, 8 - 2 + dz], [8 + hx, 12, 8 + 2 + dz], 0);
      add('arm_sleeve_' + i, [8 - hx - 0.25, 12, 8 - 2.25 + dz], [8 + hx + 0.25, 14.5, 8 + 2.25 + dz], 1);
      const m = JSON.parse(fs.readFileSync(CUR + ring + (slim ? '_slim' : '') + '.json'));
      const tf = m.textures['0'].split(':')[1];
      if (!texCache[tf]) texCache[tf] = new Texture({ name: tf.split('/').pop() + '.png' }).fromDataURL(b64(TEX + tf.replace('item/', '') + '.png')).add(false);
      for (const e of m.elements) {
        const c = new Cube({ name: e.name + '_' + i, from: [e.from[0], e.from[1], e.from[2] + dz], to: [e.to[0], e.to[1], e.to[2] + dz], autouv: 0, box_uv: false }).init();
        for (const f of FACES) {
          if (e.faces[f]) { c.faces[f].uv = e.faces[f].uv; c.faces[f].texture = texCache[tf].uuid; } else c.faces[f].texture = false;
        }
      }
    }
    Canvas.updateAll(); unselectAllElements();
    return i;
  };

  // Skinned players wearing the exported models/curio/*.json. Skin PNGs are read from SKIN (vanilla steve/alex, kept out of the repo).
  // Entity-to-world: right arm pivot x = -5 (+5 left), arm bottom at world y = 12, ring centre 2.5 px above it (local y 9.5 of a 10 px arm).
  SB.playerSetup = function (SKIN, rings) {
    const p = proj('ring_worn_preview'); p.select(); if (Project !== p) throw new Error('select player');
    Cube.all.slice().forEach(c => c.remove(false)); Texture.all.slice().forEach(t => t.remove(false));
    const CUR = MODELS.replace('models/item/', 'models/curio/');
    const skins = {
      wide: new Texture({ name: 'steve.png' }).fromDataURL(b64(SKIN + 'wide_steve.png')).add(false),
      slim: new Texture({ name: 'alex.png' }).fromDataURL(b64(SKIN + 'slim_alex.png')).add(false),
    };
    const pals = {};
    const S = 0.25;
    const box = (name, tex, from, to, u, v, w, h, d) => {
      const c = new Cube({ name, from, to, autouv: 0, box_uv: false }).init();
      const f = c.faces, U = a => a * S;
      f.west.uv = [U(u), U(v + d), U(u + d), U(v + d + h)];
      f.north.uv = [U(u + d), U(v + d), U(u + d + w), U(v + d + h)];
      f.east.uv = [U(u + d + w), U(v + d), U(u + 2 * d + w), U(v + d + h)];
      f.south.uv = [U(u + 2 * d + w), U(v + d), U(u + 2 * d + 2 * w), U(v + d + h)];
      f.up.uv = [U(u + d), U(v + d), U(u + d + w), U(v)];
      f.down.uv = [U(u + d + w), U(v), U(u + d + 2 * w), U(v + d)];
      for (const k of FACES) f[k].texture = tex.uuid;
    };
    const inf = (a, e) => [a[0] - e, a[1] - e, a[2] - e];
    const sup = (a, e) => [a[0] + e, a[1] + e, a[2] + e];
    const out = [];
    rings.forEach((r, i) => {
      const slim = r.slim, tex = slim ? skins.slim : skins.wide, aw = slim ? 3 : 4;
      const z0 = (i - (rings.length - 1) / 2) * 22;
      const B = (n, f, t, u, v, w, h, d, e) => box(n + '_' + i, tex, e ? inf([f[0], f[1], f[2] + z0], e) : [f[0], f[1], f[2] + z0], e ? sup([t[0], t[1], t[2] + z0], e) : [t[0], t[1], t[2] + z0], u, v, w, h, d);
      B('head', [-4, 24, -4], [4, 32, 4], 0, 0, 8, 8, 8); B('hat', [-4, 24, -4], [4, 32, 4], 32, 0, 8, 8, 8, 0.5);
      B('body', [-4, 12, -2], [4, 24, 2], 16, 16, 8, 12, 4); B('jacket', [-4, 12, -2], [4, 24, 2], 16, 32, 8, 12, 4, 0.25);
      B('rleg', [-4, 0, -2], [0, 12, 2], 0, 16, 4, 12, 4); B('rpants', [-4, 0, -2], [0, 12, 2], 0, 32, 4, 12, 4, 0.25);
      B('lleg', [0, 0, -2], [4, 12, 2], 16, 48, 4, 12, 4); B('lpants', [0, 0, -2], [4, 12, 2], 0, 48, 4, 12, 4, 0.25);
      B('rarm', [-4 - aw, 12, -2], [-4, 24, 2], 40, 16, aw, 12, 4); B('rsleeve', [-4 - aw, 12, -2], [-4, 24, 2], 40, 32, aw, 12, 4, 0.25);
      B('larm', [4, 12, -2], [4 + aw, 24, 2], 32, 48, aw, 12, 4); B('lsleeve', [4, 12, -2], [4 + aw, 24, 2], 48, 48, aw, 12, 4, 0.25);
      const m = JSON.parse(fs.readFileSync(CUR + r.ring + (slim ? '_slim' : '') + '.json'));
      const tf = m.textures['0'].split(':')[1];
      if (!pals[tf]) pals[tf] = new Texture({ name: tf.split('/').pop() + '.png' }).fromDataURL(b64(TEX + tf.replace('item/', '') + '.png')).add(false);
      const cx = -5 + (slim ? -0.5 : -1);
      for (const e of m.elements) {
        // renderer chain: translate(cx, 9.5), XP180 (flips y and z), so z is mirrored and y maps to 12.5 + (my - 8)
        const c = new Cube({ name: e.name + '_' + i, from: [cx + e.from[0] - 8, 12.5 + e.from[1] - 8, -(e.to[2] - 8) + z0], to: [cx + e.to[0] - 8, 12.5 + e.to[1] - 8, -(e.from[2] - 8) + z0], autouv: 0, box_uv: false }).init();
        const fm = { north: 'south', south: 'north' };
        for (const k of FACES) {
          const src = e.faces[fm[k] || k];
          if (src) { c.faces[k].uv = src.uv; c.faces[k].texture = pals[tf].uuid; } else c.faces[k].texture = false;
        }
      }
      out.push(z0);
    });
    Canvas.updateAll(); unselectAllElements();
    return out;
  };

  SB.playerShot = function (file, cam) {
    const p = proj('ring_worn_preview'); p.select(); unselectAllElements();
    pv().loadAnglePreset({ position: cam.position, target: cam.target, projection: 'perspective', fov: cam.fov, locked_angle: null });
    render(file);
    return file;
  };

  SB.armShots = function () {
    const p = proj('ring_worn_preview'); p.select(); unselectAllElements();
    const v = pv();
    const views = { side: { position: [-52, 10, 0], target: [0, 10, 0], fov: 40 }, front34: { position: [-36, 15, -42], target: [0, 9.5, 0], fov: 40 } };
    for (const [k, c] of Object.entries(views)) {
      v.loadAnglePreset({ position: c.position, target: c.target, projection: 'perspective', fov: c.fov, locked_angle: null });
      render('ring_worn_' + k + '.png');
    }
    return Object.keys(views);
  };

  SB.guiSetup = function (names) {
    const p = proj('gui_icons'); p.select(); if (Project !== p) throw new Error('select gui');
    for (const n of names) {
      const tex = new Texture({ name: n + '.png' }).fromDataURL(b64(TEX + n + '.png')).add(false);
      const cube = new Cube({ name: n, from: [0, 0, 7.5], to: [16, 16, 8.5], autouv: 0, box_uv: false }).init();
      for (const f of FACES) {
        if (f === 'north' || f === 'south') { cube.faces[f].uv = [0, 0, 16, 16]; cube.faces[f].texture = tex.uuid; } else cube.faces[f].texture = false;
      }
      cube.visibility = false;
    }
    Canvas.updateAll();
    return names.length;
  };

  SB.gui = function (names) {
    const p = proj('gui_icons'); p.select(); if (Project !== p) throw new Error('select gui');
    unselectAllElements();
    for (const n of names) {
      Cube.all.forEach(c => { c.visibility = c.name === n; });
      Canvas.updateVisibility ? Canvas.updateVisibility() : Canvas.updateAll();
      pv().loadAnglePreset({ position: [0, 8, -60], target: [0, 8, 0], projection: 'orthographic', zoom: 0.7, locked_angle: null });
      const oc = pv().camera; oc.left = -6.4; oc.right = 6.4; oc.top = 6.4; oc.bottom = -6.4; oc.updateProjectionMatrix();
      render(n + '_gui.png');
    }
    return names;
  };
})();









